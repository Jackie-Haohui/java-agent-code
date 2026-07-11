package com.example.agent.rag;

import com.example.agent.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色定位：向量数据库驱动 (Vector DB Client / DAO)
 * 链路定位：处于系统最底层，负责与远端的 Milvus 服务进行物理通信。
 * 业务意图：管理集合(Collection/表)的生命周期，以及向量数据的插入与近似最近邻(ANN)检索。
 */
@Service
public class MilvusVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreService.class);

    private final MilvusProperties properties;
    private MilvusServiceClient client;

    public MilvusVectorStoreService(MilvusProperties properties) {
        this.properties = properties;
    }

    /**
     * 生命周期钩子：在 Spring 容器实例化该 Bean 后，立即初始化连接池并确保表结构存在。
     */
    @PostConstruct
    public void init() {
        this.client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(properties.getHost())
                        .withPort(properties.getPort())
                        .build()
        );

        createCollectionIfNeeded();
        createIndexIfNeeded();
        loadCollection();
    }

    /**
     * 内部初始化：定义 Milvus 集合(相当于关系型数据库的表)的 Schema。
     */
    private void createCollectionIfNeeded() {
        // 【核心修复】：在建表前，先去服务器查询这张表是否已经存在。如果存在则直接退出，消除报错噪音！
        R<Boolean> hasCollectionResp = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(properties.getCollectionName())
                        .build()
        );

        if (hasCollectionResp.getData() != null && hasCollectionResp.getData()) {
            return;
        }

        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .withPrimaryKey(true)
                .build();

        FieldType documentIdField = FieldType.newBuilder()
                .withName("document_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(256)
                .build();

        FieldType titleField = FieldType.newBuilder()
                .withName("title")
                .withDataType(DataType.VarChar)
                .withMaxLength(512)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(8192)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(properties.getEmbeddingDimension())
                .build();

        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withDescription("Java Agent documents")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(documentIdField)
                .addFieldType(titleField)
                .addFieldType(contentField)
                .addFieldType(vectorField)
                .build();

        try {
            client.createCollection(param);
        } catch (Exception e) {
            log.warn("Milvus 创建集合失败（可能已存在）: {}", e.getMessage());
        }
    }

    /**
     * 为 embedding 字段创建 IVF_FLAT 索引，启用向量相似度搜索能力。
     */
    private void createIndexIfNeeded() {
        try {
            client.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(properties.getCollectionName())
                            .withFieldName("embedding")
                            .withIndexType(IndexType.IVF_FLAT)
                            .withMetricType(MetricType.COSINE) // 使用余弦相似度
                            .withExtraParam("{\"nlist\":128}")
                            .withSyncMode(Boolean.TRUE)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Milvus 创建索引失败（可能已存在）: {}", e.getMessage());
        }
    }

    /**
     * 将集合加载到内存中，使其处于可搜索状态。
     * Milvus 必须先将数据 Load 进内存才能提供极速的检索服务。
     */
    private void loadCollection() {
        try {
            client.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(properties.getCollectionName())
                            .withSyncLoad(Boolean.TRUE)
                            .build()
            );
        } catch (Exception e) {
            log.error("Milvus 加载集合到内存失败，搜索将不可用: {}", e.getMessage());
        }
    }

    /**
     * 核心操作：插入一条带向量的文本切片。
     */
    public void insert(String id, String documentId, String title, String content, List<Double> embedding) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", List.of(id)));
        fields.add(new InsertParam.Field("document_id", List.of(documentId)));
        fields.add(new InsertParam.Field("title", List.of(title)));
        fields.add(new InsertParam.Field("content", List.of(content)));
        fields.add(new InsertParam.Field("embedding", List.of(toFloatList(embedding))));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withFields(fields)
                .build();

        client.insert(insertParam);
    }

    /**
     * 核心操作：向量相似度搜索。
     * 将查询文本向量化后，在 Milvus 中执行 ANN 检索，返回 TopK 个最相似的文档切片。
     */
    public List<RetrievedChunk> search(List<Double> embedding, int topK) {
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }

        List<List<Float>> vectors = List.of(toFloatList(embedding));

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withMetricType(MetricType.COSINE) // 必须与创建索引时配置的测量类型一致
                .withOutFields(List.of("document_id", "title", "content"))
                .withTopK(topK)
                .withVectors(vectors)
                .withVectorFieldName("embedding")
                .withParams("{\"nprobe\":16}")
                .build();

        try {
            SearchResults results = client.search(searchParam).getData();
            SearchResultsWrapper wrapper = new SearchResultsWrapper(results.getResults());

            List<RetrievedChunk> chunks = new ArrayList<>();
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
            for (int i = 0; i < scores.size(); i++) {
                Object documentId = wrapper.getFieldData("document_id", i);
                Object title = wrapper.getFieldData("title", i);
                Object content = wrapper.getFieldData("content", i);

                chunks.add(new RetrievedChunk(
                        documentId != null ? documentId.toString() : "",
                        title != null ? title.toString() : "",
                        content != null ? content.toString() : "",
                        scores.get(i).getScore()
                ));
            }
            return chunks;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 查询已入库的文档列表（按 document_id 去重，返回文档摘要）。
     */
    public List<Map<String, Object>> listDocuments() {
        try {
            io.milvus.param.dml.QueryParam queryParam = io.milvus.param.dml.QueryParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withExpr("id != \"\"")
                    .withOutFields(List.of("document_id", "title"))
                    .withLimit(10000L)
                    .build();

            io.milvus.grpc.QueryResults results = client.query(queryParam).getData();
            io.milvus.response.QueryResultsWrapper wrapper = new io.milvus.response.QueryResultsWrapper(results);

            Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
            for (var record : wrapper.getRowRecords()) {
                String docId = record.get("document_id") != null ? record.get("document_id").toString() : "unknown";
                String title = record.get("title") != null ? record.get("title").toString() : "Untitled";
                grouped.compute(docId, (k, v) -> {
                    if (v == null) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("documentId", docId);
                        m.put("title", title);
                        m.put("chunks", 1);
                        return m;
                    } else {
                        v.put("chunks", (int) v.get("chunks") + 1);
                        return v;
                    }
                });
            }
            return new ArrayList<>(grouped.values());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 按 document_id 删除文档的所有切片。
     */
    public long deleteByDocumentId(String documentId) {
        try {
            io.milvus.param.dml.DeleteParam param = io.milvus.param.dml.DeleteParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withExpr("document_id == \"" + documentId + "\"")
                    .build();
            return client.delete(param).getData().getDeleteCnt();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 辅助方法：将 Double 列表转为 SDK 要求的 Float 列表。
     */
    private List<Float> toFloatList(List<Double> doubles) {
        List<Float> floats = new ArrayList<>(doubles.size());
        for (Double d : doubles) {
            floats.add(d.floatValue());
        }
        return floats;
    }
}
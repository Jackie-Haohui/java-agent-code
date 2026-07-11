package com.example.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量数据库配置类 (Vector DB Config)
 * 负责将 application.yml 中的 milvus.* 配置映射为 Spring Bean。
 * 它是 RAG（检索增强生成）模块的基础，决定了 Agent 从哪里获取和写入知识。
 */
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    // 1. 向量数据库连接地址
    private String host = "localhost";

    // 2. 向量数据库连接端口
    private int port = 19530;

    // 3. 存储集合名称：指定 Agent 知识库所在的 "表" 名
    private String collectionName = "java_agent_docs";

    // 4. 向量维度：极度关键，必须与你使用的 Embedding 模型（如 text-embedding-3-small）输出维度严格匹配
    private int embeddingDimension = 1536;

    // ==========================================
    // Getters and Setters
    // ==========================================

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
}
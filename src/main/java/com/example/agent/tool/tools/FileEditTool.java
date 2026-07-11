package com.example.agent.tool.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import com.example.agent.tool.WorkspaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class FileEditTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(FileEditTool.class);

    private final WorkspaceContext workspaceContext;

    public FileEditTool(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "file_edit",
                "在文件中查找指定文本并替换为新文本。用于精确修改文件中的代码片段。如果搜索文本匹配到多处，工具会拒绝执行并要求缩小搜索范围。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "path", Map.of("type", "string", "description", "相对工作目录的文件路径"),
                                "search", Map.of("type", "string", "description", "要查找的精确文本"),
                                "replace", Map.of("type", "string", "description", "替换后的新文本")
                        ),
                        new String[]{"path", "search", "replace"}
                ),
                true
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        String rawPath = String.valueOf(arguments.get("path"));
        log.info("file_edit 请求: path={}", rawPath);
        try {
            String search = String.valueOf(arguments.get("search"));
            String replace = String.valueOf(arguments.get("replace"));
            Path target = workspaceContext.resolvePath(rawPath);

            String content = Files.readString(target);

            int count = 0;
            int idx = 0;
            while ((idx = content.indexOf(search, idx)) != -1) {
                count++;
                idx += search.length();
            }

            if (count == 0) {
                log.warn("file_edit 未找到搜索文本: path={}", rawPath);
                return ToolExecutionResult.failure(
                        definition().name(), "SEARCH_NOT_FOUND",
                        "未找到搜索文本，说明文件已被修改过，不是你记忆中的旧内容。请立即用 file_read 重新读取文件当前内容，再决定是否需要继续修改。搜索内容: " + search,
                        System.currentTimeMillis() - start);
            }

            if (count > 1) {
                log.warn("file_edit 多处匹配: path={} count={}", rawPath, count);
                return ToolExecutionResult.failure(
                        definition().name(), "MULTIPLE_MATCHES",
                        "搜索文本匹配到 " + count + " 处，请缩小搜索范围，提供更具体的上下文使搜索文本唯一。",
                        System.currentTimeMillis() - start);
            }

            String newContent = content.replace(search, replace);
            Files.writeString(target, newContent);
            log.info("file_edit 成功: path={}", rawPath);

            return ToolExecutionResult.success(
                    definition().name(),
                    "文件已修改: " + rawPath + " (替换了 1 处)",
                    System.currentTimeMillis() - start);
        } catch (SecurityException e) {
            log.warn("file_edit 越权拒绝: path={} error={}", rawPath, e.getMessage());
            return ToolExecutionResult.failure(
                    definition().name(), "PATH_NOT_ALLOWED", e.getMessage(), System.currentTimeMillis() - start);
        } catch (IllegalArgumentException e) {
            log.warn("file_edit 文件不存在: path={} error={}", rawPath, e.getMessage());
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_NOT_FOUND", e.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("file_edit 异常: path={}", rawPath, e);
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_EDIT_FAILED", e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}

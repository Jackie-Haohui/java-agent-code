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
public class FileReadTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(FileReadTool.class);

    private final WorkspaceContext workspaceContext;

    public FileReadTool(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "file_read",
                "读取当前工作目录内的文本文件。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "path", Map.of("type", "string", "description", "相对于工作目录的文件路径")
                        ),
                        new String[]{"path"}
                ),
                false
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        String rawPath = String.valueOf(arguments.get("path"));
        log.info("file_read 请求: path={}", rawPath);
        try {
            Path target = workspaceContext.resolvePath(rawPath);
            String content = Files.readString(target);
            log.info("file_read 成功: path={} size={} chars", rawPath, content.length());
            return ToolExecutionResult.success(
                    definition().name(), content, System.currentTimeMillis() - start);
        } catch (SecurityException e) {
            log.warn("file_read 越权拒绝: path={} error={}", rawPath, e.getMessage());
            return ToolExecutionResult.failure(
                    definition().name(), "PATH_NOT_ALLOWED", e.getMessage(), System.currentTimeMillis() - start);
        } catch (IllegalArgumentException e) {
            log.warn("file_read 文件不存在: path={} error={}", rawPath, e.getMessage());
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_NOT_FOUND", e.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("file_read 异常: path={}", rawPath, e);
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_READ_FAILED", e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}
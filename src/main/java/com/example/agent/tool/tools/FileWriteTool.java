package com.example.agent.tool.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import com.example.agent.tool.WorkspaceContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class FileWriteTool implements AgentTool {

    private final WorkspaceContext workspaceContext;

    public FileWriteTool(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "file_write",
                "创建新文件或覆盖写入文件到工作目录中。如果父目录不存在会自动创建。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "path", Map.of("type", "string", "description", "相对工作目录的文件路径"),
                                "content", Map.of("type", "string", "description", "要写入的文件内容")
                        ),
                        new String[]{"path", "content"}
                ),
                true
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        try {
            String rawPath = String.valueOf(arguments.get("path"));
            String content = String.valueOf(arguments.get("content"));
            Path safeRoot = workspaceContext.get();
            String normalized = rawPath.replace('\\', '/');
            Path target = Path.of(normalized);

            if (target.isAbsolute()) {
                target = target.normalize();
                if (!target.startsWith(safeRoot)) {
                    return ToolExecutionResult.failure(
                            definition().name(), "PATH_NOT_ALLOWED",
                            "路径越权", System.currentTimeMillis() - start);
                }
            } else {
                target = safeRoot.resolve(normalized).normalize();
                if (!target.startsWith(safeRoot)) {
                    return ToolExecutionResult.failure(
                            definition().name(), "PATH_NOT_ALLOWED",
                            "路径越权", System.currentTimeMillis() - start);
                }
            }

            Files.createDirectories(target.getParent());
            Files.writeString(target, content);

            return ToolExecutionResult.success(
                    definition().name(),
                    "文件已写入: " + rawPath + " (" + content.length() + " 字符)",
                    System.currentTimeMillis() - start);
        } catch (SecurityException e) {
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_WRITE_FAILED", e.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(), "FILE_WRITE_FAILED", e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}

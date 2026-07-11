package com.example.agent.tool.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ShellCommandTool implements AgentTool {

    // 强白名单：仅允许只读探查命令，拒绝任何修改操作
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "java -version",
            "mvn -version",
            "git status",
            "git log --oneline -10",
            "git branch",
            "git diff --stat",
            "ls",
            "dir",
            "tree",
            "find . -name *.java",
            "find . -name *.xml",
            "find . -name *.yml",
            "find . -name *.properties",
            "find . -name *.sql",
            "find . -name *.ts",
            "find . -name *.vue",
            "find . -type f"
    );

    private static final String WHITELIST_HINT =
            "命令不在白名单中。当前允许的命令: java -version, mvn -version, git status, git log --oneline -10, git branch, git diff --stat, ls, dir, tree, find . -name ...";

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "shell_command",
                "执行白名单中的只读 Shell 命令（ls/dir 列出文件, tree 查看目录树, find 搜索文件, git 查看版本信息）。注意：不允许 ls -la 等参数组合，只能用完整匹配的命令。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "command", Map.of("type", "string", "description", "需要执行的命令，必须是白名单中的完整命令字符串。列出文件用 ls 或 dir，目录树用 tree，搜索 Java 文件用 find . -name *.java")
                        ),
                        new String[]{"command"}
                ),
                true
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();

        try {
            String command = String.valueOf(arguments.get("command")).trim();

            if (!ALLOWED_COMMANDS.contains(command)) {
                return ToolExecutionResult.failure(
                        definition().name(),
                        "COMMAND_NOT_ALLOWED",
                        WHITELIST_HINT,
                        System.currentTimeMillis() - start
                );
            }

            ProcessBuilder builder = new ProcessBuilder(command.split(" "));
            Process process = builder.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ToolExecutionResult.failure(
                        definition().name(),
                        "COMMAND_TIMEOUT",
                        "命令执行超时",
                        System.currentTimeMillis() - start
                );
            }

            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            String combined = (output + error).trim();
            if (combined.isEmpty()) {
                combined = "命令执行成功，无输出。";
            }

            return ToolExecutionResult.success(
                    definition().name(),
                    combined,
                    System.currentTimeMillis() - start
            );
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(),
                    "SHELL_EXECUTION_FAILED",
                    e.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}

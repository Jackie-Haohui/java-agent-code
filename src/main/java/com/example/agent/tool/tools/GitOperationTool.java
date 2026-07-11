package com.example.agent.tool.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import com.example.agent.tool.WorkspaceContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class GitOperationTool implements AgentTool {

    private final WorkspaceContext workspaceContext;

    public GitOperationTool(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "git_operation",
                "在工作目录中执行 Git 操作。支持 status（查看状态）、diff（查看未暂存差异）、diff_staged（查看已暂存差异）、log（查看最近20条提交）、branch（查看当前分支）、add_and_commit（暂存并提交变更）。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "operation", Map.of("type", "string", "description", "操作类型: status, diff, diff_staged, log, branch, add_and_commit"),
                                "message", Map.of("type", "string", "description", "提交信息，仅 add_and_commit 时需要"),
                                "files", Map.of("type", "string", "description", "逗号分隔的要暂存的文件路径，仅 add_and_commit 时需要。留空则 git add -A")
                        ),
                        new String[]{"operation"}
                ),
                true
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        try {
            String operation = String.valueOf(arguments.get("operation"));
            Path workDir = workspaceContext.get();

            return switch (operation) {
                case "status" -> runGit(workDir, "status", "--porcelain");
                case "diff" -> runGit(workDir, "diff");
                case "diff_staged" -> runGit(workDir, "diff", "--staged");
                case "log" -> runGit(workDir, "log", "--oneline", "-20");
                case "branch" -> runGit(workDir, "branch");
                case "add_and_commit" -> {
                    String message = arguments.get("message") instanceof String m ? m : "Agent auto commit";
                    String files = arguments.get("files") instanceof String f ? f : null;
                    if (files != null && !files.isBlank()) {
                        String[] fileArray = files.split(",");
                        String[] addCmd = new String[fileArray.length + 1];
                        addCmd[0] = "add";
                        System.arraycopy(fileArray, 0, addCmd, 1, fileArray.length);
                        runGit(workDir, addCmd);
                    } else {
                        runGit(workDir, "add", "-A");
                    }
                    yield runGit(workDir, "commit", "-m", message);
                }
                default -> ToolExecutionResult.failure(
                        definition().name(), "UNKNOWN_OPERATION",
                        "未知操作: " + operation + "。支持: status, diff, diff_staged, log, branch, add_and_commit",
                        System.currentTimeMillis() - start);
            };
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(), "GIT_ERROR", e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private ToolExecutionResult runGit(Path workDir, String... args) {
        long start = System.currentTimeMillis();
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = "git";
            System.arraycopy(args, 0, cmd, 1, args.length);

            Process process = new ProcessBuilder(cmd)
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ToolExecutionResult.failure(
                        definition().name(), "GIT_TIMEOUT", "Git 操作超时", System.currentTimeMillis() - start);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ToolExecutionResult.failure(
                        definition().name(), "GIT_EXIT_ERROR",
                        "Git 退出码 " + exitCode + ": " + output,
                        System.currentTimeMillis() - start);
            }

            return ToolExecutionResult.success(
                    definition().name(),
                    output.isBlank() ? "操作完成，无输出" : output,
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(), "GIT_ERROR", e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}

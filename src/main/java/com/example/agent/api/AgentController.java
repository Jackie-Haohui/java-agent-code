package com.example.agent.api;

import com.example.agent.agent.AgentLoop;
import com.example.agent.api.dto.*;
import com.example.agent.conversation.Conversation;
import com.example.agent.conversation.ConversationMessage;
import com.example.agent.conversation.ConversationService;
import com.example.agent.eval.AgentEvaluationService;
import com.example.agent.eval.EvaluationStats;
import com.example.agent.hitl.HumanApproval;
import com.example.agent.hitl.HumanApprovalRepository;
import com.example.agent.hitl.HumanApprovalService;
import com.example.agent.state.AgentSessionRepository;
import com.example.agent.state.AgentStepLogRepository;
import com.example.agent.workspace.ProjectScanner;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class AgentController {

    private final AgentLoop agentLoop;
    private final HumanApprovalService humanApprovalService;
    private final HumanApprovalRepository approvalRepository;
    private final AgentSessionRepository sessionRepository;
    private final AgentStepLogRepository stepLogRepository;
    private final ConversationService conversationService;
    private final ProjectScanner projectScanner;
    private final AgentEvaluationService evaluationService;

    public AgentController(
            AgentLoop agentLoop,
            HumanApprovalService humanApprovalService,
            HumanApprovalRepository approvalRepository,
            AgentSessionRepository sessionRepository,
            AgentStepLogRepository stepLogRepository,
            ConversationService conversationService,
            ProjectScanner projectScanner,
            AgentEvaluationService evaluationService
    ) {
        this.agentLoop = agentLoop;
        this.humanApprovalService = humanApprovalService;
        this.approvalRepository = approvalRepository;
        this.sessionRepository = sessionRepository;
        this.stepLogRepository = stepLogRepository;
        this.conversationService = conversationService;
        this.projectScanner = projectScanner;
        this.evaluationService = evaluationService;
    }

    // ===================== Agent 运行 =====================

    @PostMapping("/api/agents/run")
    public AgentRunResponse run(@Valid @RequestBody AgentRunRequest request) {
        return agentLoop.run(request);
    }

    @PostMapping("/api/agents/approval")
    public AgentRunResponse approve(@Valid @RequestBody HumanApprovalRequest request) {
        humanApprovalService.resolveApproval(
                request.getApprovalId(),
                request.isApproved(),
                request.getReason()
        );

        HumanApproval approval = approvalRepository.findById(request.getApprovalId())
                .orElseThrow(() -> new IllegalArgumentException("Approval not found"));

        return agentLoop.resume(approval.getSessionId());
    }

    @GetMapping("/api/agents/sessions")
    public List<AgentSessionDto> sessions(@RequestParam(name = "userId", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return sessionRepository.findAll()
                    .stream()
                    .map(AgentSessionDto::new)
                    .toList();
        }

        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AgentSessionDto::new)
                .toList();
    }

    @GetMapping("/api/agents/sessions/{sessionId}")
    public AgentSessionDto session(@PathVariable("sessionId") UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .map(AgentSessionDto::new)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    @GetMapping("/api/agents/sessions/{sessionId}/steps")
    public List<AgentStepLogDto> steps(@PathVariable("sessionId") UUID sessionId) {
        return stepLogRepository.findBySessionIdOrderByStepIndexAsc(sessionId)
                .stream()
                .map(AgentStepLogDto::new)
                .toList();
    }

    // ===================== 对话管理 =====================

    @PostMapping("/api/conversations")
    public ConversationDto createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(
                request.getUserId(),
                request.getTitle(),
                request.getWorkspacePath()
        );
        return new ConversationDto(conversation);
    }

    @GetMapping("/api/conversations")
    public List<ConversationDto> listConversations(@RequestParam("userId") String userId) {
        return conversationService.listConversations(userId)
                .stream()
                .map(ConversationDto::new)
                .toList();
    }

    @GetMapping("/api/conversations/{conversationId}")
    public ConversationDto getConversation(@PathVariable("conversationId") UUID conversationId) {
        return new ConversationDto(conversationService.getConversation(conversationId));
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    public List<ConversationMessageDto> getMessages(@PathVariable("conversationId") UUID conversationId) {
        return conversationService.getMessages(conversationId)
                .stream()
                .map(ConversationMessageDto::new)
                .toList();
    }

    @PostMapping("/api/conversations/{conversationId}/chat")
    public AgentRunResponse sendChat(
            @PathVariable("conversationId") UUID conversationId,
            @Valid @RequestBody ChatRequest request) {

        Conversation conversation = conversationService.getConversation(conversationId);
        conversationService.addUserMessage(conversationId, request.getMessage());

        // 将项目扫描结果注入 Agent 上下文
        Map<String, Object> input = request.getInput() != null
                ? new java.util.HashMap<>(request.getInput())
                : new java.util.HashMap<>();
        conversationService.getMessages(conversationId).stream()
                .filter(m -> "SYSTEM".equals(m.getRole()))
                .reduce((first, second) -> second) // 取最后一条 SYSTEM 消息
                .ifPresent(sysMsg -> input.put("projectContext", sysMsg.getContent()));

        AgentRunRequest runRequest = new AgentRunRequest();
        runRequest.setUserId(request.getUserId());
        runRequest.setGoal(request.getMessage());
        runRequest.setInput(input);
        runRequest.setConversationId(conversationId);
        runRequest.setWorkspacePath(conversation.getWorkspacePath());

        return agentLoop.run(runRequest);
    }

    @PostMapping("/api/conversations/{conversationId}/resume")
    public AgentRunResponse resumeConversation(
            @PathVariable("conversationId") UUID conversationId,
            @Valid @RequestBody HumanApprovalRequest request) {

        humanApprovalService.resolveApproval(
                request.getApprovalId(),
                request.isApproved(),
                request.getReason()
        );

        HumanApproval approval = approvalRepository.findById(request.getApprovalId())
                .orElseThrow(() -> new IllegalArgumentException("Approval not found"));

        return agentLoop.resume(approval.getSessionId());
    }

    @PutMapping("/api/conversations/{conversationId}")
    public ConversationDto updateConversation(
            @PathVariable("conversationId") UUID conversationId,
            @RequestBody UpdateConversationRequest request) {
        Conversation conversation = conversationService.updateConversation(
                conversationId,
                request.getTitle(),
                request.getWorkspacePath()
        );
        return new ConversationDto(conversation);
    }

    @DeleteMapping("/api/conversations/{conversationId}")
    public void archiveConversation(@PathVariable("conversationId") UUID conversationId) {
        conversationService.archiveConversation(conversationId);
    }

    // ===================== 评估统计 =====================

    @GetMapping("/api/evaluations/stats")
    public EvaluationStats getEvaluationStats(@RequestParam("userId") String userId) {
        return evaluationService.getStats(userId);
    }

    // ===================== 工作空间 =====================

    @PostMapping("/api/workspace/validate")
    public Map<String, Object> validateWorkspace(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        if (path == null || path.isBlank()) {
            return Map.of("valid", false, "message", "路径不能为空");
        }

        Path p = Path.of(path).toAbsolutePath().normalize();
        if (!Files.exists(p)) {
            return Map.of("valid", false, "message", "路径不存在: " + p);
        }
        if (!Files.isDirectory(p)) {
            return Map.of("valid", false, "message", "路径不是目录: " + p);
        }

        // 检测项目类型
        String projectType = "Unknown";
        if (Files.exists(p.resolve("pom.xml"))) {
            projectType = "Maven (Java)";
        } else if (Files.exists(p.resolve("build.gradle")) || Files.exists(p.resolve("build.gradle.kts"))) {
            projectType = "Gradle";
        } else if (Files.exists(p.resolve("package.json"))) {
            projectType = "Node.js / npm";
        } else if (Files.exists(p.resolve("requirements.txt")) || Files.exists(p.resolve("setup.py"))) {
            projectType = "Python";
        } else if (Files.exists(p.resolve("Cargo.toml"))) {
            projectType = "Rust";
        } else if (Files.exists(p.resolve("go.mod"))) {
            projectType = "Go";
        }

        return Map.of("valid", true, "message", "路径有效", "path", p.toString(), "projectType", projectType);
    }

    @PostMapping("/api/workspace/scan")
    public Map<String, Object> scanWorkspace(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        if (path == null || path.isBlank()) {
            return Map.of("success", false, "message", "路径不能为空");
        }
        ProjectScanner.ProjectSummary summary = projectScanner.scan(path);
        return Map.of(
                "success", true,
                "projectType", summary.projectType(),
                "summary", summary.summary(),
                "tree", summary.tree(),
                "fileCount", summary.allFiles().size()
        );
    }

    @PostMapping("/api/workspace/upload")
    public Map<String, Object> uploadWorkspace(@RequestParam("file") MultipartFile file) {
        try {
            Path uploadDir = Path.of("workspace", "uploads", UUID.randomUUID().toString());
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            Path target = uploadDir.resolve(originalName != null ? originalName : "project.zip");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // 如果是 zip 文件，解压
            if (originalName != null && (originalName.endsWith(".zip") || originalName.endsWith(".jar"))) {
                unzipFile(target, uploadDir);
            }

            ProjectScanner.ProjectSummary summary = projectScanner.scan(uploadDir.toString());
            return Map.of(
                    "success", true,
                    "path", uploadDir.toAbsolutePath().normalize().toString(),
                    "projectType", summary.projectType(),
                    "summary", summary.summary(),
                    "fileCount", summary.allFiles().size()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/api/workspace/clone")
    public Map<String, Object> cloneRepository(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return Map.of("success", false, "message", "GitHub URL 不能为空");
        }

        try {
            Path cloneDir = Path.of("workspace", "clones", UUID.randomUUID().toString());
            Files.createDirectories(cloneDir);

            Process process = new ProcessBuilder("git", "clone", "--depth", "1", url, cloneDir.toString())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Map.of("success", false, "message", "克隆超时（60秒）");
            }

            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                return Map.of("success", false, "message", "克隆失败: " + output);
            }

            ProjectScanner.ProjectSummary summary = projectScanner.scan(cloneDir.toString());
            return Map.of(
                    "success", true,
                    "path", cloneDir.toAbsolutePath().normalize().toString(),
                    "projectType", summary.projectType(),
                    "summary", summary.summary(),
                    "fileCount", summary.allFiles().size()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "克隆异常: " + e.getMessage());
        }
    }

    private void unzipFile(Path zipPath, Path targetDir) throws Exception {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipPath))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) continue; // 防止 zip slip 攻击
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}

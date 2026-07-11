package com.example.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WorkspaceContext {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceContext.class);

    private final ThreadLocal<Path> workspacePath = new ThreadLocal<>();

    public void set(Path path) {
        workspacePath.set(path != null ? path.toAbsolutePath().normalize() : null);
    }

    public Path get() {
        Path path = workspacePath.get();
        if (path == null) {
            return Path.of("workspace").toAbsolutePath().normalize();
        }
        return path;
    }

    public void clear() {
        workspacePath.remove();
    }

    /**
     * 安全解析文件路径。支持绝对路径（必须在 workspace 内）和相对路径。
     * 自动将反斜杠转为正斜杠以兼容 Windows 路径。
     * @param rawPath 用户或 LLM 传入的原始路径
     * @return 解析并校验后的绝对路径
     */
    public Path resolvePath(String rawPath) {
        String normalized = rawPath.replace('\\', '/');
        Path safeRoot = get();
        Path target = Path.of(normalized);

        log.info("路径解析: raw={} normalized={} safeRoot={}", rawPath, normalized, safeRoot);

        if (target.isAbsolute()) {
            target = target.normalize();
            log.info("路径是绝对路径, normalized={}", target);
            if (!target.startsWith(safeRoot)) {
                log.warn("路径越权拒绝: target={} safeRoot={}", target, safeRoot);
                throw new SecurityException("路径越权: " + target + " 不在工作目录 " + safeRoot + " 内");
            }
        } else {
            target = safeRoot.resolve(normalized).normalize();
            log.info("路径是相对路径, resolved={}", target);
            if (!target.startsWith(safeRoot)) {
                log.warn("路径越权拒绝: target={}", target);
                throw new SecurityException("路径越权: " + target);
            }
        }

        if (!Files.exists(target)) {
            log.warn("文件不存在: target={}", target);
            throw new IllegalArgumentException("文件不存在: " + rawPath + " → " + target);
        }

        log.info("路径解析成功: {}", target);
        return target;
    }
}

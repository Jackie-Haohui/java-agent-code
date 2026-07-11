package com.example.agent.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProjectScanner {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_FILES_PER_DIR = 80;
    private static final int MAX_TREE_LINES = 200;
    private static final Set<String> KEY_FILES = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts", "package.json",
            "application.yml", "application.yaml", "application.properties",
            "Dockerfile", "docker-compose.yml", "Makefile", "README.md",
            "settings.gradle", "settings.gradle.kts", ".gitignore"
    );
    private static final Set<String> IGNORE_DIRS = Set.of(
            "node_modules", ".git", "target", "build", "dist", ".idea",
            ".vscode", "__pycache__", ".gradle", "coverage", ".nyc_output"
    );

    public ProjectSummary scan(String rootPath) {
        Path root = Path.of(rootPath).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return new ProjectSummary(root.toString(), "路径不存在或不是目录", "", "", List.of(), List.of());
        }

        String projectType = detectProjectType(root);
        String tree = buildTree(root);
        List<FileInfo> keyFileContents = readKeyFiles(root);
        List<String> allFiles = collectFileList(root, 0);

        String summary = buildSummary(projectType, tree, allFiles, keyFileContents, root);
        return new ProjectSummary(root.toString(), summary, projectType, tree, allFiles, keyFileContents);
    }

    private String detectProjectType(Path root) {
        if (Files.exists(root.resolve("pom.xml"))) {
            try {
                String content = Files.readString(root.resolve("pom.xml"));
                if (content.contains("<artifactId>spring-boot")) return "Spring Boot (Maven)";
                return "Maven (Java)";
            } catch (IOException e) {
                return "Maven (Java)";
            }
        }
        if (Files.exists(root.resolve("build.gradle.kts"))) return "Gradle Kotlin DSL";
        if (Files.exists(root.resolve("build.gradle"))) return "Gradle (Java)";
        if (Files.exists(root.resolve("package.json"))) {
            try {
                String content = Files.readString(root.resolve("package.json"));
                if (content.contains("\"vue\"")) return "Vue.js (npm)";
                if (content.contains("\"react\"")) return "React (npm)";
                if (content.contains("\"next\"")) return "Next.js (npm)";
                return "Node.js (npm)";
            } catch (IOException e) {
                return "Node.js (npm)";
            }
        }
        if (Files.exists(root.resolve("requirements.txt")) || Files.exists(root.resolve("setup.py"))) return "Python";
        if (Files.exists(root.resolve("Cargo.toml"))) return "Rust";
        if (Files.exists(root.resolve("go.mod"))) return "Go";
        if (Files.exists(root.resolve("CmakeLists.txt"))) return "C/C++ (CMake)";
        return "Unknown";
    }

    private String buildTree(Path root) {
        StringBuilder sb = new StringBuilder();
        sb.append(root.getFileName() != null ? root.getFileName().toString() : root.toString()).append("\n");
        try {
            buildTreeRecursive(root, "", sb, 0);
        } catch (Exception e) {
            sb.append("  [读取错误: ").append(e.getMessage()).append("]\n");
        }
        if (sb.length() > 8000) {
            return sb.substring(0, 8000) + "\n...(目录树截断)";
        }
        return sb.toString();
    }

    private void buildTreeRecursive(Path dir, String prefix, StringBuilder sb, int depth) {
        if (depth > MAX_DEPTH) return;
        try {
            List<Path> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) entries.add(p);
            }

            entries.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));

            int count = 0;
            for (int i = 0; i < entries.size() && count < MAX_FILES_PER_DIR; i++) {
                Path p = entries.get(i);
                String name = p.getFileName().toString();
                if (IGNORE_DIRS.contains(name)) continue;
                if (Files.isDirectory(p) && name.startsWith(".")) continue;
                count++;

                boolean isLast = (i == entries.size() - 1 || count >= MAX_FILES_PER_DIR);
                String connector = isLast ? "└── " : "├── ";
                String childPrefix = isLast ? "    " : "│   ";

                sb.append(prefix).append(connector).append(name);
                if (Files.isDirectory(p)) {
                    sb.append("/\n");
                    if (sb.length() < 8000) {
                        buildTreeRecursive(p, prefix + childPrefix, sb, depth + 1);
                    }
                } else {
                    try {
                        long size = Files.size(p);
                        if (size > 0) sb.append(" (").append(formatSize(size)).append(")");
                    } catch (IOException ignored) {}
                    sb.append("\n");
                }
            }
            if (entries.size() > MAX_FILES_PER_DIR) {
                sb.append(prefix).append("... (").append(entries.size() - MAX_FILES_PER_DIR).append(" more entries)\n");
            }
        } catch (IOException e) {
            sb.append(prefix).append("[读取错误]\n");
        }
    }

    private List<FileInfo> readKeyFiles(Path root) {
        List<FileInfo> files = new ArrayList<>();
        for (String name : KEY_FILES) {
            Path file = root.resolve(name);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                try {
                    String content = Files.readString(file);
                    if (content.length() > 3000) {
                        content = content.substring(0, 3000) + "\n...(文件截断)";
                    }
                    files.add(new FileInfo(name, content));
                } catch (IOException ignored) {}
            }
        }
        return files;
    }

    private List<String> collectFileList(Path root, int depth) {
        if (depth > MAX_DEPTH) return List.of();
        List<String> result = new ArrayList<>();
        try {
            List<Path> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path p : stream) entries.add(p);
            }
            entries.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));

            int count = 0;
            for (Path p : entries) {
                if (count >= MAX_TREE_LINES) {
                    result.add("...(截断)");
                    break;
                }
                String name = p.getFileName().toString();
                if (IGNORE_DIRS.contains(name)) continue;
                if (Files.isDirectory(p) && name.startsWith(".")) continue;

                String relPath = p.toString();
                if (Files.isDirectory(p)) {
                    result.add(relPath + "/");
                    result.addAll(collectFileList(p, depth + 1));
                } else {
                    result.add(relPath);
                }
                count++;
            }
        } catch (IOException ignored) {}
        return result;
    }

    private String buildSummary(String projectType, String tree, List<String> allFiles, List<FileInfo> keyFiles, Path root) {
        // 统计文件类型
        int javaFiles = 0, tsFiles = 0, vueFiles = 0, xmlFiles = 0, ymlFiles = 0, otherFiles = 0;
        for (String f : allFiles) {
            if (f.endsWith(".java")) javaFiles++;
            else if (f.endsWith(".ts")) tsFiles++;
            else if (f.endsWith(".vue")) vueFiles++;
            else if (f.endsWith(".xml")) xmlFiles++;
            else if (f.endsWith(".yml") || f.endsWith(".yaml")) ymlFiles++;
            else if (!f.endsWith("/")) otherFiles++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("项目类型: ").append(projectType).append("\n");
        sb.append("项目根目录: ").append(root).append("\n");
        sb.append("文件统计: ");
        List<String> parts = new ArrayList<>();
        if (javaFiles > 0) parts.add(javaFiles + " 个 .java");
        if (tsFiles > 0) parts.add(tsFiles + " 个 .ts");
        if (vueFiles > 0) parts.add(vueFiles + " 个 .vue");
        if (xmlFiles > 0) parts.add(xmlFiles + " 个 .xml");
        if (ymlFiles > 0) parts.add(ymlFiles + " 个 .yml/.yaml");
        if (otherFiles > 0) parts.add(otherFiles + " 个其他文件");
        sb.append(String.join(", ", parts)).append("\n\n");

        sb.append("=== 目录结构 ===\n");
        sb.append(tree).append("\n");

        if (!keyFiles.isEmpty()) {
            sb.append("=== 关键文件内容 ===\n");
            for (FileInfo f : keyFiles) {
                sb.append("\n--- ").append(f.name()).append(" ---\n");
                sb.append(f.content()).append("\n");
            }
        }

        if (allFiles.size() > MAX_TREE_LINES) {
            sb.append("\n(文件列表被截断，仅显示前 ").append(MAX_TREE_LINES).append(" 项)\n");
        }

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }

    public record ProjectSummary(
            String rootPath,
            String summary,
            String projectType,
            String tree,
            List<String> allFiles,
            List<FileInfo> keyFiles
    ) {}

    public record FileInfo(String name, String content) {}
}

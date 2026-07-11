package com.example.agent.tool.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 角色定位：搜索引擎集成工具 (Web Search Tool)
 * 业务意图：通过 DuckDuckGo HTML 搜索实现广域网信息检索，无需 API Key。
 */
@Component
public class WebSearchMockTool implements AgentTool {

    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "class=\"result__snippet[^\"]*\"[^>]*>(.*?)</(?:span|a|td)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "web_search",
                "通过 DuckDuckGo 搜索互联网信息，返回相关摘要。用于获取实时信息或补充知识库不足。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string", "description", "搜索关键词")
                        ),
                        new String[]{"query"}
                ),
                false
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        String query = String.valueOf(arguments.get("query"));

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JavaAgent/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            StringBuilder result = new StringBuilder();
            Matcher matcher = SNIPPET_PATTERN.matcher(html);
            int count = 0;
            while (matcher.find() && count < 5) {
                String snippet = TAG_PATTERN.matcher(matcher.group(1)).replaceAll("").trim();
                if (snippet.length() > 20) {
                    result.append(count + 1).append(". ").append(snippet).append("\n");
                    count++;
                }
            }

            if (result.isEmpty()) {
                return ToolExecutionResult.success(
                        definition().name(),
                        "未找到与 \"" + query + "\" 相关的搜索结果。",
                        System.currentTimeMillis() - start
                );
            }

            return ToolExecutionResult.success(
                    definition().name(),
                    "搜索 \"" + query + "\" 的结果：\n" + result.toString().trim(),
                    System.currentTimeMillis() - start
            );
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(),
                    "SEARCH_ERROR",
                    "搜索失败: " + e.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}
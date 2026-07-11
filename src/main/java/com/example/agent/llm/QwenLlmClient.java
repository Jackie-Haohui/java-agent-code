package com.example.agent.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 角色定位：千问（DashScope）具体实现适配器 (Qwen Adapter)
 * 链路定位：Spring 容器自动将其注入到 AgentLoop 中作为 LlmClient 的具体实现。
 * 业务意图：利用 Spring AI 框架，通过 OpenAI 兼容模式将系统内部的提示词请求转化为向阿里云 DashScope 发起的真实 HTTP 网络请求。
 */
@Component
public class QwenLlmClient implements LlmClient {

    // 注入 Spring AI 的聊天客户端（其底层配置来源于 application.yml 中的 spring.ai.openai.*，
    // 通过 base-url 指向 DashScope 的 OpenAI 兼容端点）
    private final ChatClient chatClient;

    /**
     * 构造函数注入 (Constructor Injection)
     * 在 Spring Boot 启动时，利用 ChatClient.Builder 构建出一个可用的客户端实例。
     */
    public QwenLlmClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 核心交互方法：执行同步的大模型调用（千问 qwen-plus）
     */
    @Override
    public LlmResponse complete(String prompt) {
        // 1. 发起网络调用并解析结果
        String content = chatClient
                .prompt()           // 开启一次新的对话构建
                .user(prompt)       // 将我们的 System Prompt 作为用户输入传递给大模型
                .call()             // 触发阻塞式的网络请求，等待大模型推理完毕
                .content();         // 从复杂的 HTTP 响应体中，仅仅提取出大模型回复的纯文本 (String)

        // 2. 包装并返回给上层 (AgentLoop)
        return new LlmResponse(content);
    }
}

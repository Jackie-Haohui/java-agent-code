package com.example.agent.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色定位：工具注册中心 (Tool Factory / Registry)
 * 链路定位：系统启动时装配 -> 在 AgentLoop 初始化 RuntimeContext 时提供全局可用工具清单。
 */
@Component
public class ToolRegistry {

    // 内存中的工具哈希表，Key 是工具名 (name)，Value 是具体的工具实现 Bean
    private final Map<String, AgentTool> tools;

    /**
     * 架构妙笔：利用 Spring 的特性，自动将所有实现了 AgentTool 接口的 Bean 注入到一个 List 中。
     * 然后在这里将其转换为 Map，实现工具的“即插即用自动发现”。
     */
    public ToolRegistry(List<AgentTool> tools) {
        this.tools = tools.stream()
                .collect(Collectors.toMap(tool -> tool.definition().name(), Function.identity()));
    }

    // 运行时根据大模型输出的 name 动态获取对应的工具实例
    public AgentTool get(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool;
    }

    // 获取系统中所有工具的说明书，用于给大模型拼装 Prompt
    public List<ToolDefinition> listDefinitions() {
        return tools.values()
                .stream()
                .map(AgentTool::definition)
                .toList();
    }

    // 检查工具是否存在，可用于前置校验
    public boolean exists(String name) {
        return tools.containsKey(name);
    }
}
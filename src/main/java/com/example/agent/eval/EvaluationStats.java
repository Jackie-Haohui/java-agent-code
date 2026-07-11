package com.example.agent.eval;

/**
 * 评估统计数据 DTO，用于 API 返回。
 */
public class EvaluationStats {

    private final long totalSessions;
    private final double successRate;
    private final double avgSteps;
    private final double avgLatencyMs;

    public EvaluationStats(long totalSessions, double successRate, double avgSteps, double avgLatencyMs) {
        this.totalSessions = totalSessions;
        this.successRate = successRate;
        this.avgSteps = avgSteps;
        this.avgLatencyMs = avgLatencyMs;
    }

    public long getTotalSessions() { return totalSessions; }
    public double getSuccessRate() { return successRate; }
    public double getAvgSteps() { return avgSteps; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
}
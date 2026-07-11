# Java Agent Code Frontend

这是 `java-agent-code` 的前端控制台，用于演示和操作 Agent 应用的核心能力：Agent 循环、RAG、工具调用、Workflow、状态管理、记忆、Human-in-the-Loop、评估与安全。

## 启动方式

确保后端已经启动在 `http://localhost:8080`，然后执行：

```powershell
cd D:\SpringAI项目\java-agent-code\frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 已对接的后端接口

- `POST /api/agents/run`：运行 Agent 主循环
- `POST /api/documents/ingest`：导入 RAG 文档
- `POST /api/agents/approval`：提交人工审批结果
- `/actuator`：后端健康入口

## 页面说明

- 总览：展示项目能力和要求映射
- Agent 运行台：输入用户目标，查看最终回答和工具调用审计
- RAG 知识库：上传文本进入 Milvus 向量库
- 人工审批：手动提交高危工具调用审批结果
- 学习地图：把课程要求映射到当前后端模块

## 开发代理

`vite.config.ts` 已配置代理：

```ts
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/actuator': 'http://localhost:8080',
  },
}
```

因此前端代码里直接请求 `/api/...` 即可。

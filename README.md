# Java Agent Code

基于 ReAct 范式的智能代码助手 Agent，具备自主工具调用、RAG 知识库检索、文件操作与代码编辑、Git 版本控制、长期记忆等能力。前后端分离架构，适用于 AI Agent 研究与工程实践。

---

## 核心特性

### ReAct 推理-行动循环

Agent 采用 **ReAct（Reasoning + Acting）** 范式，在 Think-Act-Observe 循环中自主决策。每一步 LLM 输出 JSON 结构化决策，系统解析后执行相应动作，观察结果反馈回下一轮推理。支持六种决策类型：

| 决策类型 | 说明 |
|----------|------|
| TOOL_CALL | 调用已注册的工具（文件读写、代码搜索、Git 操作等） |
| RAG_SEARCH | 检索 Milvus 知识库，Agent 自主判断是否需要检索 |
| FINAL_ANSWER | 任务完成，输出最终答案 |
| NEED_HUMAN_APPROVAL | 触发敏感操作时挂起，等待人工审批 |
| REPLAN | 执行受阻时重新规划策略 |
| ERROR | 不可恢复的错误，终止执行 |

### RAG 检索增强生成

集成 **Milvus 向量数据库**，支持 PDF、文本文档等知识入库。文档经固定窗口分块后通过 `text-embedding-v2` 模型向量化，存入 Milvus（IVF_FLAT 索引 + COSINE 度量）。Agent 根据问题语义自主判断是否需要检索知识库，避免不必要的检索开销。

### 代码修改能力

- `file_read` — 读取文件内容
- `file_write` — 创建或覆盖文件
- `file_edit` — 精确的查找替换编辑（单次匹配才执行，零次或多匹配均拒绝并提示）
- `git_operation` — Git 状态查看、差异对比、日志、分支管理、暂存提交

所有文件操作通过 **WorkspaceContext**（ThreadLocal）进行工作区隔离和路径遍历防护。

### 人在回路（Human-in-the-Loop）

敏感工具（写文件、编辑文件、Shell 命令、Git 操作）默认需人工审批。系统生成待审批工单，前端展示工具名称和参数，用户批准或拒绝后 Agent 继续执行。同会话内已批准的工具不会重复弹窗。

### 长期记忆

基于语义向量的混合排序记忆系统：70% 余弦相似度 + 30% 重要性加权。用户交互后自动提取关键信息并嵌入存储，支持跨会话回忆。

### 多轮对话管理

对话（Conversation）→ 会话（AgentSession）两级建模。每个对话包含多条消息，每条用户消息触发一次 Agent 会话。支持多对话并行，历史消息持久化到 PostgreSQL。

---

## 系统架构

```
┌──────────────────────────────────────────────────────┐
│                   Vue 3 Frontend                     │
│   ChatView  │  IngestView  │  HistoryView           │
│   Sidebar   │  MessageBubble  │  ApprovalCard       │
└─────────────────────┬────────────────────────────────┘
                      │ REST API (Axios)
┌─────────────────────▼────────────────────────────────┐
│              Spring Boot Controller                  │
│   AgentController  │  DocumentController             │
└─────────────────────┬────────────────────────────────┘
                      │
┌─────────────────────▼────────────────────────────────┐
│                  AgentLoop (ReAct)                    │
│  PromptBuilder → LlmClient → Decision → ToolExecutor │
│       ▲                           │                   │
│       │      ┌────────────────────┘                   │
│       │      ▼                                        │
│  ┌────┴─────────────┐                                │
│  │   观察结果反馈    │                                │
│  └──────────────────┘                                │
└──┬──────────┬──────────┬──────────┬──────────────────┘
   │          │          │          │
   ▼          ▼          ▼          ▼
┌──────┐ ┌──────┐ ┌────────┐ ┌──────────┐
│Tools │ │RAG   │ │Memory  │ │Security  │
│File  │ │Milvus│ │PostgreSQL│ │HITL     │
│Git   │ │Embed │ │        │ │Prompt    │
│Search│ │      │ │        │ │Injection │
└──────┘ └──────┘ └────────┘ └──────────┘
```

### 模块说明

| 模块 | 路径 | 职责 |
|------|------|------|
| agent | `agent/` | ReAct 循环引擎、提示词构建、运行时上下文、终止策略 |
| tool | `tool/` | 工具注册、工具执行沙箱、工作区上下文管理 |
| tools | `tool/tools/` | 具体工具实现（文件读写编辑、Git、代码搜索、Shell、网页搜索） |
| rag | `rag/` | Milvus 向量数据库驱动、文本嵌入、文档摄入管道、RAG 检索门面 |
| memory | `memory/` | 长期记忆存储、语义检索、混合排序、自动记忆提取 |
| conversation | `conversation/` | 对话与消息管理，多轮对话持久化 |
| security | `security/` | Prompt 注入检测、敏感操作策略、工具权限管理 |
| hitl | `hitl/` | 人工审批流程，审批工单生命周期管理 |
| eval | `eval/` | Agent 执行评估、统计聚合 |
| api | `api/` | REST Controller 与 DTO |
| state | `state/` | Agent 会话状态、步骤日志持久化 |
| workspace | `workspace/` | 项目结构扫描、类型检测 |

---

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.3.5 |
| AI 框架 | Spring AI | 1.0.0-M3 |
| LLM | 通义千问 Qwen-Plus（DashScope） | — |
| Embedding | text-embedding-v2 | — |
| ORM | JPA / Hibernate | — |
| 数据库迁移 | Flyway | — |
| 关系数据库 | PostgreSQL | 16 |
| 向量数据库 | Milvus | 2.4.6 |
| 前端框架 | Vue 3 + TypeScript | — |
| UI 组件 | Element Plus | — |
| 构建工具 | Vite / Maven | — |

---

## 快速启动

### 1. 环境要求

- JDK 21+
- Node.js 18+
- Docker（用于 Milvus 和 PostgreSQL）

### 2. 启动基础设施

```bash
# 一条命令启动 PostgreSQL + Milvus（含 etcd + MinIO）
docker compose up -d
```

### 3. 启动后端

```bash
# 设置 DashScope API Key 环境变量
# IDEA: Run → Edit Configurations → Environment variables 添加
#   SPRING_AI_OPENAI_API_KEY=你的API_Key
# 终端:
#   Windows: set SPRING_AI_OPENAI_API_KEY=你的API_Key
#   Mac/Linux: export SPRING_AI_OPENAI_API_KEY=你的API_Key

./mvnw spring-boot:run
# 服务运行在 http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

---

## 使用指南

1. 浏览器打开 `http://localhost:5173`
2. 在 **Chat** 页面创建新对话
3. （可选）导入项目目录 — Agent 将在该目录下执行文件操作
4. 输入自然语言指令，例如"_帮我重构这个类的错误处理逻辑_"、"_给最近3次提交写个变更总结_"
5. 当 Agent 需要写文件或执行 Git 操作时，前端弹出审批卡片，批准后继续执行
6. 在 **Ingest Knowledge** 页面上传技术文档，Agent 在后续对话中会自动检索相关知识
7. 在 **History** 页面查看所有历史会话和执行统计

---

## 核心配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `spring.ai.openai.api-key` | 环境变量 | `${SPRING_AI_OPENAI_API_KEY}` | DashScope API 密钥 |
| `spring.ai.openai.base-url` | URL | `dashscope.aliyuncs.com/compatible-mode` | API 端点 |
| `spring.ai.openai.chat.options.model` | 字符串 | `qwen-plus` | 对话模型 |
| `spring.ai.openai.embedding.options.model` | 字符串 | `text-embedding-v2` | 向量化模型 |
| `agent.max-steps` | 整数 | `12` | Agent 单次最大步数 |
| `agent.tool-timeout-ms` | 毫秒 | `15000` | 单次工具调用超时 |
| `agent.rag-top-k` | 整数 | `6` | RAG 检索返回条数 |
| `agent.require-approval-for-sensitive-tools` | 布尔 | `true` | 敏感工具是否需审批 |
| `agent.workspace-path` | 路径 | `workspace` | 默认文件操作根目录 |
| `agent.auto-commit` | 布尔 | `false` | Agent 是否自动 Git 提交 |
| `milvus.collection-name` | 字符串 | `java_agent_docs` | Milvus 集合名 |
| `milvus.embedding-dimension` | 整数 | `1536` | 向量维度 |

---

## 关键技术决策

### 安全防护

- **工作区隔离**：通过 `ThreadLocal<Path>` 实现请求级工作目录绑定，所有路径操作经 `resolvePath()` 验证，拒绝路径遍历攻击
- **工具敏感度分级**：文件写、编辑、Shell、Git 标记为敏感，默认需人工审批；文件读、代码搜索为非敏感，自动执行
- **Prompt 注入检测**：内置中英文危险模式黑名单，入站用户输入在进入 LLM 前进行前置扫描
- **硬熔断保护**：同一工具连续失败 ≥ 2 次或连续重复调用 ≥ 4 次时自动终止，防止死循环

### 可靠性设计

- 工具执行完全沙箱化，单个工具异常不影响 Agent 主循环
- LLM JSON 输出解析失败时尝试正则恢复，提高鲁棒性
- 工具输出截断至 5000 字符，防止上下文窗口溢出
- 数据库使用 Flyway 管理 Schema 版本，支持增量迁移与回滚

### 记忆系统设计

混合排序算法综合考虑语义相关性和记忆重要性：

```
score = 0.7 × cosineSimilarity(query_embedding, memory_embedding) + 0.3 × (importance / 10)
```

其中用户提问类记忆的重要性权重提高到 40%，确保关键交互不被遗忘。嵌入失败时自动降级为纯重要性排序。

---

## API 接口

### Agent 执行

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agents/run` | 运行 Agent 任务 |
| POST | `/api/agents/approval` | 提交 HITL 审批结果并恢复 |
| GET | `/api/agents/sessions` | 查询会话列表 |
| GET | `/api/agents/sessions/{id}` | 查询会话详情 |
| GET | `/api/agents/sessions/{id}/steps` | 查询会话步骤日志 |
| GET | `/api/evaluations/stats` | 查询执行统计 |

### 对话管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/conversations` | 创建对话 |
| GET | `/api/conversations` | 对话列表 |
| GET | `/api/conversations/{id}` | 对话详情 |
| GET | `/api/conversations/{id}/messages` | 对话消息列表 |
| POST | `/api/conversations/{id}/chat` | 发送消息并执行 Agent |
| POST | `/api/conversations/{id}/resume` | 恢复对话中的 Agent |
| PUT | `/api/conversations/{id}` | 更新对话 |
| DELETE | `/api/conversations/{id}` | 归档对话 |

### 工作区

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/workspace/validate` | 验证路径有效性 |
| POST | `/api/workspace/scan` | 扫描并分析项目结构 |
| POST | `/api/workspace/upload` | 上传 zip/jar 文件作为工作区 |
| POST | `/api/workspace/clone` | 克隆 Git 仓库作为工作区 |

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents/ingest/file` | 上传文件入库 |
| POST | `/api/documents/ingest` | JSON 文本入库 |
| GET | `/api/documents/list` | 已入库文档列表 |
| DELETE | `/api/documents/{id}` | 删除文档及向量 |

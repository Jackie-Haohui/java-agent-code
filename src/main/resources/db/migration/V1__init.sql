-- V1__init.sql: 初始化 Agent 系统全部核心数据表

CREATE TABLE IF NOT EXISTS agent_session (
    id           UUID PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    goal         TEXT         NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',
    current_step INT          NOT NULL DEFAULT 0,
    max_steps    INT          NOT NULL DEFAULT 12,
    summary      TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_step_log (
    id             UUID PRIMARY KEY,
    session_id     UUID         NOT NULL REFERENCES agent_session(id),
    step_index     INT          NOT NULL,
    decision_type  VARCHAR(50)  NOT NULL,
    model_output   TEXT,
    tool_name      VARCHAR(255),
    tool_arguments TEXT,
    tool_result    TEXT,
    error_message  TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memory_record (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    memory_type VARCHAR(50)  NOT NULL,
    content     TEXT         NOT NULL,
    importance  INT          NOT NULL DEFAULT 5,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS human_approval (
    id             UUID PRIMARY KEY,
    session_id     UUID         NOT NULL,
    tool_name      VARCHAR(255) NOT NULL,
    tool_arguments TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    reason         TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_record (
    id                 UUID PRIMARY KEY,
    session_id         UUID    NOT NULL,
    task_success       BOOLEAN,
    tool_call_accuracy DOUBLE PRECISION,
    step_reasonability DOUBLE PRECISION,
    latency_ms         BIGINT,
    total_steps        INT,
    failure_reason     TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_step_log_session ON agent_step_log(session_id);
CREATE INDEX IF NOT EXISTS idx_memory_user ON memory_record(user_id);
CREATE INDEX IF NOT EXISTS idx_approval_session ON human_approval(session_id);

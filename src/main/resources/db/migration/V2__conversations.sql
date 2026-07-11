-- V2__conversations.sql: 对话管理和项目工作空间支持

CREATE TABLE IF NOT EXISTS conversation (
    id             UUID PRIMARY KEY,
    user_id        VARCHAR(255) NOT NULL,
    title          VARCHAR(500),
    workspace_path TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_message (
    id               UUID PRIMARY KEY,
    conversation_id  UUID         NOT NULL,
    session_id       UUID,
    role             VARCHAR(50)  NOT NULL,
    content          TEXT         NOT NULL,
    tool_calls_json  TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conv_msg_conversation ON conversation_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conv_msg_session ON conversation_message(session_id);
CREATE INDEX IF NOT EXISTS idx_conv_user ON conversation(user_id);

ALTER TABLE agent_session ADD COLUMN IF NOT EXISTS conversation_id UUID;
ALTER TABLE agent_session ADD COLUMN IF NOT EXISTS workspace_path TEXT;
CREATE INDEX IF NOT EXISTS idx_session_conversation ON agent_session(conversation_id);

-- V3__memory_embedding.sql: 为记忆表添加向量嵌入字段，支持语义相似度检索

ALTER TABLE memory_record ADD COLUMN IF NOT EXISTS embedding TEXT;

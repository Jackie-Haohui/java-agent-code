<template>
  <aside class="conversation-sidebar">
    <div class="sidebar-header">
      <h3>Conversations</h3>
    </div>
    <el-button type="primary" class="new-btn" @click="$emit('new-conversation')">
      + New Conversation
    </el-button>
    <div class="conversation-list">
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conv-item"
        :class="{ active: conv.id === activeId }"
        @click="$emit('select', conv.id)"
      >
        <div class="conv-info">
          <span class="conv-title">{{ conv.title || 'Untitled' }}</span>
          <span class="conv-date">{{ formatDate(conv.updatedAt || conv.createdAt) }}</span>
        </div>
        <el-button
          class="conv-delete"
          :icon="Delete"
          text
          size="small"
          @click.stop="$emit('delete', conv.id)"
        />
      </div>
      <div v-if="conversations.length === 0" class="empty-hint">
        No conversations yet
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Conversation } from '@/types/agent'

defineProps<{
  conversations: Conversation[]
  activeId: string | null
}>()

defineEmits<{
  select: [id: string]
  'new-conversation': []
  delete: [id: string]
}>()

function formatDate(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}d ago`
  return d.toLocaleDateString()
}
</script>

<style scoped>
.conversation-sidebar {
  width: 260px;
  min-width: 260px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}
.new-btn {
  margin: 12px;
  width: calc(100% - 24px);
}
.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.15s;
}
.conv-item:hover {
  background: #e8eaed;
}
.conv-item.active {
  background: #d9ecff;
}
.conv-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}
.conv-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv-date {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.conv-delete {
  opacity: 0;
  transition: opacity 0.15s;
}
.conv-item:hover .conv-delete {
  opacity: 1;
}
.empty-hint {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 24px;
}
</style>

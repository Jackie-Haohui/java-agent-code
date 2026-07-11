<template>
  <div class="chat-input-area">
    <div class="workspace-bar" v-if="workspacePath">
      Workspace: <strong>{{ workspacePath }}</strong>
    </div>
    <div class="input-row">
      <el-input
        v-model="text"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 6 }"
        placeholder="Type a message... (Enter to send, Shift+Enter for newline)"
        :disabled="disabled"
        @keydown="handleKeydown"
        resize="none"
      />
      <el-button
        type="primary"
        :disabled="disabled || !text.trim()"
        @click="send"
      >
        Send
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  disabled: boolean
  workspacePath: string
}>()

const emit = defineEmits<{
  send: [text: string]
}>()

const text = ref('')

function handleKeydown(e: KeyboardEvent | any) {
  if (e instanceof KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }
}

function send() {
  const trimmed = text.value.trim()
  if (!trimmed || props.disabled) return
  emit('send', trimmed)
  text.value = ''
}
</script>

<style scoped>
.chat-input-area {
  border-top: 1px solid #e4e7ed;
  padding: 12px 20px;
  background: #fff;
}
.workspace-bar {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.input-row :deep(.el-textarea__inner) {
  font-size: 14px;
}
</style>

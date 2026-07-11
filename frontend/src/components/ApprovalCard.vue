<template>
  <div class="approval-card">
    <el-alert type="warning" :closable="false" show-icon>
      <template #title>
        Agent needs approval to use <strong>{{ toolName }}</strong>
      </template>
      <div class="approval-body">
        <div class="approval-args">
          <strong>Arguments:</strong>
          <pre>{{ formattedArgs }}</pre>
        </div>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="2"
          placeholder="Reason (optional)"
          class="reason-input"
        />
        <div class="approval-actions">
          <el-button type="success" @click="$emit('approve', approvalId, reason)">
            Approve
          </el-button>
          <el-button type="danger" @click="$emit('reject', approvalId, reason)">
            Reject
          </el-button>
        </div>
      </div>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  approvalId: string
  toolName: string
  toolArguments: string
}>()

defineEmits<{
  approve: [approvalId: string, reason: string]
  reject: [approvalId: string, reason: string]
}>()

const reason = ref('')

const formattedArgs = computed(() => {
  try {
    return JSON.stringify(JSON.parse(props.toolArguments), null, 2)
  } catch {
    return props.toolArguments
  }
})
</script>

<style scoped>
.approval-card {
  margin: 8px 0;
}
.approval-body {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.approval-args pre {
  background: #fff;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 150px;
  overflow: auto;
}
.reason-input {
  max-width: 400px;
}
.approval-actions {
  display: flex;
  gap: 8px;
}
</style>

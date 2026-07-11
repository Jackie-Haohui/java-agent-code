<template>
  <div class="history-page">
    <h2>Agent Session History</h2>

    <div class="filter-bar">
      <el-input
        v-model="filterUserId"
        placeholder="Filter by User ID"
        clearable
        style="width: 200px"
        @change="onUserIdChange"
      />
      <el-input
        v-model="filterGoal"
        placeholder="Search by goal text..."
        clearable
        style="width: 300px"
      />
    </div>

    <div v-if="stats" class="stats-panel">
      <div class="stat-card">
        <div class="stat-value">{{ stats.totalSessions }}</div>
        <div class="stat-label">Total Sessions</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" :class="stats.successRate >= 0.8 ? 'good' : stats.successRate >= 0.5 ? 'warn' : 'bad'">
          {{ (stats.successRate * 100).toFixed(0) }}%
        </div>
        <div class="stat-label">Success Rate</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.avgSteps.toFixed(1) }}</div>
        <div class="stat-label">Avg Steps</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ (stats.avgLatencyMs / 1000).toFixed(1) }}s</div>
        <div class="stat-label">Avg Latency</div>
      </div>
    </div>

    <el-table
      :data="filteredSessions"
      style="width: 100%"
      @row-click="selectSession"
      highlight-current-row
      v-loading="loading"
    >
      <el-table-column prop="userId" label="User" width="120" />
      <el-table-column prop="goal" label="Goal" min-width="250" show-overflow-tooltip />
      <el-table-column label="Status" width="140">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentStep" label="Steps" width="80" />
      <el-table-column prop="maxSteps" label="Max" width="60" />
      <el-table-column label="Created" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div v-if="selectedSession" class="step-detail">
      <h3>Steps for Session {{ selectedSession.id }}</h3>
      <el-table :data="steps" style="width: 100%" size="small">
        <el-table-column prop="stepIndex" label="#" width="50" />
        <el-table-column prop="decisionType" label="Decision" width="140" />
        <el-table-column prop="toolName" label="Tool" width="120" />
        <el-table-column prop="toolArguments" label="Arguments" min-width="180">
          <template #default="{ row }">
            <code v-if="row.toolArguments">{{ row.toolArguments }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="toolResult" label="Result" min-width="200">
          <template #default="{ row }">
            <span class="result-preview">{{ truncate(row.toolResult, 150) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="Error" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listSessions, getSessionSteps, getEvalStats } from '@/api/agent'
import type { AgentSession, AgentStepLog, EvaluationStats } from '@/types/agent'

const loading = ref(false)
const sessions = ref<AgentSession[]>([])
const filterUserId = ref('')
const filterGoal = ref('')
const selectedSession = ref<AgentSession | null>(null)
const steps = ref<AgentStepLog[]>([])
const stats = ref<EvaluationStats | null>(null)

const filteredSessions = computed(() => {
  return sessions.value.filter(s => {
    if (filterGoal.value && !s.goal.toLowerCase().includes(filterGoal.value.toLowerCase())) return false
    return true
  })
})

function statusTag(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'WAITING_APPROVAL') return 'warning'
  return 'info'
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

function truncate(text: string | null, max: number) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}

async function loadSessions() {
  loading.value = true
  try {
    const res = await listSessions(filterUserId.value || undefined)
    sessions.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  if (!filterUserId.value) {
    stats.value = null
    return
  }
  try {
    const res = await getEvalStats(filterUserId.value)
    stats.value = res.data
  } catch {
    stats.value = null
  }
}

async function onUserIdChange() {
  await Promise.all([loadSessions(), loadStats()])
}

async function selectSession(session: AgentSession) {
  selectedSession.value = session
  try {
    const res = await getSessionSteps(session.id)
    steps.value = res.data
  } catch {
    steps.value = []
  }
}

onMounted(() => loadSessions())
</script>

<style scoped>
.history-page {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
.history-page h2 {
  margin: 0 0 16px;
  font-size: 20px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.step-detail {
  margin-top: 24px;
}
.step-detail h3 {
  margin: 0 0 12px;
  font-size: 16px;
}
.result-preview {
  font-size: 12px;
  color: #606266;
}
code {
  font-size: 11px;
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
}

.stats-panel {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  background: #f8f9fb;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-value.good { color: #67c23a; }
.stat-value.warn { color: #e6a23c; }
.stat-value.bad { color: #f56c6c; }

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

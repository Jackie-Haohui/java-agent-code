<template>
  <div class="ingest-page">
    <h2>Ingest Knowledge</h2>
    <p class="ingest-desc">Upload files to the Milvus vector database for RAG retrieval.</p>

    <div
      class="drop-zone"
      @dragover.prevent
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInputRef"
        type="file"
        webkitdirectory
        multiple
        @change="handleFileSelect"
        style="display: none"
      />
      <p>Drag & drop files here, or</p>
      <el-button @click="selectFolder">Select Folder</el-button>
      <el-button @click="selectFiles">Select Files</el-button>
    </div>

    <div v-if="files.length > 0" class="file-list">
      <h3>Pending Files ({{ files.length }})</h3>
      <el-table :data="files" style="width: 100%" max-height="400">
        <el-table-column prop="name" label="Name" min-width="250" />
        <el-table-column prop="sizeFormatted" label="Size" width="100" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.uploaded ? 'success' : 'info'" size="small">
              {{ row.uploaded ? 'Done' : 'Pending' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="uploading || allDone"
        @click="uploadAll"
        style="margin-top: 12px"
      >
        {{ uploading ? `Uploading (${uploadedCount}/${files.length})...` : `Ingest ${files.length} Files` }}
      </el-button>
    </div>

    <div v-if="uploadResult" class="upload-result">
      <el-alert
        :type="uploadResult.success ? 'success' : 'warning'"
        :title="uploadResult.message"
        :closable="true"
        @close="uploadResult = null"
      />
    </div>

    <div v-if="ingestedDocs.length > 0" class="ingested-list">
      <h3>Ingested Documents ({{ ingestedDocs.length }})</h3>
      <el-table :data="ingestedDocs" style="width: 100%" max-height="300">
        <el-table-column prop="title" label="Document" min-width="250" />
        <el-table-column prop="chunks" label="Chunks" width="80" />
        <el-table-column label="Actions" width="130">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="confirmDelete(row)">
              Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="deleteDialogVisible" title="Confirm Delete" width="400px">
      <p>Delete <strong>{{ docToDelete?.title }}</strong> from knowledge base?</p>
      <p>This will remove all {{ docToDelete?.chunks }} chunks. This action cannot be undone.</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">Cancel</el-button>
        <el-button type="danger" :loading="deleting" @click="doDelete">Delete</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ingestFile, listDocuments, deleteDocument } from '@/api/document'
import type { IngestedDoc } from '@/types/document'

interface FileEntry {
  name: string
  size: number
  sizeFormatted: string
  file: File
  uploaded: boolean
}

const fileInputRef = ref<HTMLInputElement | null>(null)
const files = ref<FileEntry[]>([])
const uploading = ref(false)
const uploadResult = ref<{ success: boolean; message: string } | null>(null)
const ingestedDocs = ref<IngestedDoc[]>([])
const deleteDialogVisible = ref(false)
const docToDelete = ref<IngestedDoc | null>(null)
const deleting = ref(false)

const uploadedCount = computed(() => files.value.filter(f => f.uploaded).length)
const allDone = computed(() => files.value.length > 0 && uploadedCount.value === files.value.length)

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function selectFolder() {
  fileInputRef.value?.click()
}

function selectFiles() {
  const input = document.createElement('input')
  input.type = 'file'
  input.multiple = true
  input.onchange = (e) => {
    const target = e.target as HTMLInputElement
    if (target.files) addFiles(Array.from(target.files))
  }
  input.click()
}

function addFiles(fileList: File[]) {
  for (const file of fileList) {
    if (files.value.some(f => f.name === file.name && f.size === file.size)) continue
    files.value.push({
      name: file.name,
      size: file.size,
      sizeFormatted: formatSize(file.size),
      file,
      uploaded: false,
    })
  }
}

function handleDrop(e: DragEvent) {
  if (e.dataTransfer?.files) {
    addFiles(Array.from(e.dataTransfer.files))
  }
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) addFiles(Array.from(target.files))
}

async function uploadAll() {
  uploading.value = true
  let success = 0
  let failed = 0

  for (const entry of files.value) {
    if (entry.uploaded) continue
    try {
      const res = await ingestFile(entry.file)
      if (res.data?.success) {
        entry.uploaded = true
        success++
      } else {
        throw new Error(res.data?.message || 'Unknown error')
      }
    } catch {
      failed++
    }
  }

  uploading.value = false
  uploadResult.value = {
    success: failed === 0,
    message: `Uploaded ${success} files successfully${failed > 0 ? `, ${failed} failed` : ''}.`,
  }
  if (success > 0) loadIngestedDocuments()
}

async function loadIngestedDocuments() {
  try {
    const res = await listDocuments()
    ingestedDocs.value = res.data || []
  } catch {
    ingestedDocs.value = []
  }
}

function confirmDelete(doc: IngestedDoc) {
  docToDelete.value = doc
  deleteDialogVisible.value = true
}

async function doDelete() {
  if (!docToDelete.value) return
  deleting.value = true
  try {
    const res = await deleteDocument(docToDelete.value.documentId)
    if (res.data?.success) {
      ingestedDocs.value = ingestedDocs.value.filter(d => d.documentId !== docToDelete.value!.documentId)
      uploadResult.value = { success: true, message: `Deleted "${docToDelete.value.title}" (${res.data.deletedChunks} chunks removed).` }
    } else {
      uploadResult.value = { success: false, message: 'Delete failed.' }
    }
  } catch {
    uploadResult.value = { success: false, message: 'Delete request failed.' }
  }
  deleting.value = false
  deleteDialogVisible.value = false
  docToDelete.value = null
}

onMounted(loadIngestedDocuments)
</script>

<style scoped>
.ingest-page {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
.ingest-page h2 {
  margin: 0 0 8px;
  font-size: 20px;
}
.ingest-desc {
  color: #909399;
  margin-bottom: 20px;
}
.drop-zone {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  color: #909399;
  transition: border-color 0.2s;
}
.drop-zone:hover {
  border-color: #409eff;
}
.drop-zone p {
  margin-bottom: 12px;
}
.drop-zone .el-button {
  margin: 0 4px;
}
.file-list {
  margin-top: 20px;
}
.file-list h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.upload-result {
  margin-top: 16px;
}
.ingested-list {
  margin-top: 24px;
}
.ingested-list h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
</style>

<template>
  <el-dialog
    :model-value="visible"
    title="Import Project"
    width="560px"
    @update:model-value="$emit('cancel')"
  >
    <el-tabs v-model="mode">
      <!-- 模式 1: 本地路径 -->
      <el-tab-pane label="Local Path" name="local">
        <div class="import-form">
          <p class="mode-hint">Enter the absolute path to your project directory.</p>
          <el-input
            v-model="localPath"
            placeholder="e.g., D:/my-project or /home/user/project"
            clearable
            @keyup.enter="validateLocal"
          />
          <el-button class="action-btn" @click="validateLocal" :loading="validating">
            Validate & Scan
          </el-button>
          <div v-if="validationError" class="error-msg">{{ validationError }}</div>
          <div v-if="validationResult" class="validation-result" :class="{ valid: validationResult.valid }">
            <el-tag :type="validationResult.valid ? 'success' : 'danger'" size="small">
              {{ validationResult.valid ? 'Valid' : 'Invalid' }}
            </el-tag>
            <span>{{ validationResult.message }}</span>
            <span v-if="validationResult.projectType">
              | {{ validationResult.projectType }}
            </span>
          </div>
        </div>
      </el-tab-pane>

      <!-- 模式 2: GitHub URL -->
      <el-tab-pane label="GitHub" name="github">
        <div class="import-form">
          <p class="mode-hint">Enter a GitHub repository URL to clone and scan.</p>
          <el-input
            v-model="githubUrl"
            placeholder="e.g., https://github.com/user/repo.git"
            clearable
            @keyup.enter="cloneGitHub"
          />
          <el-button class="action-btn" @click="cloneGitHub" :loading="cloning">
            Clone & Scan
          </el-button>
          <div v-if="cloneError" class="error-msg">{{ cloneError }}</div>
          <div v-if="cloneResult" class="validation-result" :class="{ valid: cloneResult.valid }">
            <el-tag :type="cloneResult.valid ? 'success' : 'danger'" size="small">
              {{ cloneResult.valid ? 'Done' : 'Failed' }}
            </el-tag>
            <span>{{ cloneResult.message }}</span>
            <span v-if="cloneResult.projectType">
              | {{ cloneResult.projectType }}
            </span>
          </div>
        </div>
      </el-tab-pane>

      <!-- 模式 3: 上传文件 -->
      <el-tab-pane label="Upload Files" name="upload">
        <div class="import-form">
          <p class="mode-hint">Drag and drop a folder, .zip file, or individual files. You can also click to select.</p>
          <div
            class="drop-zone"
            @dragover.prevent="onDragOver"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop"
            :class="{ dragging }"
          >
            <p v-if="!uploadItems.length">Drop files or folders here, or click to select</p>
            <div v-else class="selected-files">
              <p class="selected-file" v-for="(f, i) in uploadItems.slice(0, 5)" :key="i">
                {{ f.name || f }} ({{ typeof f.size === 'number' ? formatSize(f.size) : 'folder' }})
              </p>
              <p v-if="uploadItems.length > 5" class="more-files">... and {{ uploadItems.length - 5 }} more</p>
            </div>
            <div class="drop-buttons">
              <input
                type="file"
                ref="fileInputRef"
                @change="onFileSelect"
                style="display: none"
                multiple
              />
              <input
                type="file"
                ref="folderInputRef"
                @change="onFileSelect"
                style="display: none"
                webkitdirectory
                multiple
              />
              <el-button @click="fileInputRef?.click()" size="small">Select Files</el-button>
              <el-button @click="folderInputRef?.click()" size="small">Select Folder</el-button>
            </div>
          </div>
          <el-button
            class="action-btn"
            @click="uploadAndScan"
            :loading="uploading"
            :disabled="!uploadItems.length"
          >
            Upload & Scan
          </el-button>
          <div v-if="uploadError" class="error-msg">{{ uploadError }}</div>
          <div v-if="uploadResult" class="validation-result" :class="{ valid: uploadResult.valid }">
            <el-tag :type="uploadResult.valid ? 'success' : 'danger'" size="small">
              {{ uploadResult.valid ? 'Done' : 'Failed' }}
            </el-tag>
            <span>{{ uploadResult.message }}</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="$emit('cancel')">Cancel</el-button>
      <el-button
        type="primary"
        @click="confirmImport"
        :disabled="!resolvedPath"
      >
        Import & Start Conversation
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import JSZip from 'jszip'
import { validateWorkspace, uploadWorkspace, cloneRepository } from '@/api/conversation'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  confirm: [path: string]
  cancel: []
}>()

const mode = ref('local')

// --- Local ---
const localPath = ref('')
const validating = ref(false)
const validationResult = ref<any>(null)
const validationError = ref('')

async function validateLocal() {
  if (!localPath.value.trim()) return
  validating.value = true
  validationError.value = ''
  validationResult.value = null
  try {
    const res = await validateWorkspace(localPath.value.trim())
    validationResult.value = { ...res.data }
    if (res.data.valid) {
      resolvedPath.value = res.data.path || localPath.value.trim()
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || 'Validation failed'
    validationError.value = 'Validation failed: ' + msg
    validationResult.value = { valid: false, message: msg }
  } finally {
    validating.value = false
  }
}

// --- GitHub ---
const githubUrl = ref('')
const cloning = ref(false)
const cloneResult = ref<any>(null)
const cloneError = ref('')

async function cloneGitHub() {
  if (!githubUrl.value.trim()) return
  cloning.value = true
  cloneError.value = ''
  cloneResult.value = null
  try {
    const res = await cloneRepository(githubUrl.value.trim())
    cloneResult.value = { valid: res.data.success, ...res.data }
    if (res.data.success) {
      resolvedPath.value = res.data.path
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || 'Clone failed'
    cloneError.value = 'Clone failed: ' + msg
    cloneResult.value = { valid: false, message: msg }
  } finally {
    cloning.value = false
  }
}

// --- Upload ---
const fileInputRef = ref<HTMLInputElement>()
const folderInputRef = ref<HTMLInputElement>()
const uploadItems = ref<File[]>([])
const uploading = ref(false)
const uploadResult = ref<any>(null)
const uploadError = ref('')
const dragging = ref(false)

function onDragOver(e: DragEvent) {
  dragging.value = true
}

function onDragLeave() {
  dragging.value = false
}

async function onDrop(e: DragEvent) {
  dragging.value = false
  const dt = e.dataTransfer
  if (!dt || !dt.items) return

  const files: File[] = []

  async function traverse(entry: FileSystemEntry, basePath: string) {
    if (entry.isFile) {
      const file = await new Promise<File>((resolve, reject) => {
        (entry as FileSystemFileEntry).file(resolve, reject)
      })
      const fullPath = basePath + file.name
      Object.defineProperty(file, 'webkitRelativePath', {
        value: fullPath,
        writable: false,
      })
      files.push(file)
    } else if (entry.isDirectory) {
      const reader = (entry as FileSystemDirectoryEntry).createReader()
      // readEntries may need multiple calls for large directories
      const allEntries: FileSystemEntry[] = []
      let batch: FileSystemEntry[]
      do {
        batch = await new Promise<FileSystemEntry[]>((resolve) => {
          reader.readEntries(resolve)
        })
        allEntries.push(...batch)
      } while (batch.length > 0)

      for (const child of allEntries) {
        await traverse(child, basePath + entry.name + '/')
      }
    }
  }

  // First try the Entry API (supports folders)
  const entries: FileSystemEntry[] = []
  for (let i = 0; i < dt.items.length; i++) {
    const entry = dt.items[i].webkitGetAsEntry()
    if (entry) entries.push(entry)
  }

  if (entries.length > 0) {
    for (const entry of entries) {
      await traverse(entry, '')
    }
  } else {
    // Fallback: plain file drop (no Entry API support)
    for (let i = 0; i < dt.files.length; i++) {
      files.push(dt.files[i])
    }
  }

  if (files.length > 0) {
    uploadItems.value = files
  }
}

function onFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    uploadItems.value = Array.from(target.files)
  }
}

async function uploadAndScan() {
  if (!uploadItems.value.length) return
  uploading.value = true
  uploadError.value = ''
  uploadResult.value = null

  try {
    const files = uploadItems.value
    let fileToUpload: File

    // If multiple files or folder content, zip them first
    if (files.length > 1 || files[0].webkitRelativePath) {
      const zip = new JSZip()
      for (const f of files) {
        const relPath = f.webkitRelativePath || f.name
        zip.file(relPath, f)
      }
      const zipBlob = await zip.generateAsync({ type: 'blob' })
      const name = files[0].webkitRelativePath
        ? files[0].webkitRelativePath.split('/')[0] + '.zip'
        : 'project.zip'
      fileToUpload = new File([zipBlob], name, { type: 'application/zip' })
    } else {
      fileToUpload = files[0]
    }

    const formData = new FormData()
    formData.append('file', fileToUpload)
    const res = await uploadWorkspace(formData)
    uploadResult.value = { valid: res.data.success, ...res.data }
    if (res.data.success) {
      resolvedPath.value = res.data.path
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || 'Upload failed'
    uploadError.value = 'Upload failed: ' + msg
    uploadResult.value = { valid: false, message: msg }
  } finally {
    uploading.value = false
  }
}

// --- Common ---
const resolvedPath = ref('')

function confirmImport() {
  if (resolvedPath.value) emit('confirm', resolvedPath.value)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.import-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 180px;
}
.mode-hint {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
.action-btn {
  align-self: flex-start;
}
.validation-result {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  flex-wrap: wrap;
}
.error-msg {
  color: #f56c6c;
  font-size: 13px;
  background: #fef0f0;
  border-radius: 4px;
  padding: 8px 12px;
}
.drop-zone {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  transition: border-color 0.2s, background 0.2s;
}
.drop-zone.dragging {
  border-color: #409eff;
  background: #ecf5ff;
}
.drop-zone p {
  margin: 0 0 10px;
  color: #909399;
}
.selected-file {
  color: #303133 !important;
  font-weight: 500;
}
.more-files {
  color: #909399;
  font-size: 12px;
}
.selected-files {
  margin-bottom: 12px;
  text-align: left;
}
.drop-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>

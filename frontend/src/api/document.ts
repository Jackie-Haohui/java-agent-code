import { http } from './http'
import type { DocumentUploadRequest, IngestFileResult, IngestedDoc, DeleteDocumentResult } from '@/types/document'

export function ingestDocument(data: DocumentUploadRequest) {
  return http.post<void>('/api/documents/ingest', data)
}

export function ingestFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<IngestFileResult>('/api/documents/ingest/file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function listDocuments() {
  return http.get<IngestedDoc[]>('/api/documents/list')
}

export function deleteDocument(documentId: string) {
  return http.delete<DeleteDocumentResult>(`/api/documents/${encodeURIComponent(documentId)}`)
}

export interface DocumentUploadRequest {
  documentId: string
  title: string
  content: string
}

export interface IngestFileResult {
  success: boolean
  documentId: string
  title: string
  chunks: number
  message: string
}

export interface IngestedDoc {
  documentId: string
  title: string
  chunks: number
}

export interface DeleteDocumentResult {
  success: boolean
  documentId: string
  deletedChunks: number
}

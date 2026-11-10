import { api, qs } from './client'
import type { PageResponse } from './types'

// UC-C01 — Kho tri thức RAG (khớp KnowledgeResponses ở backend)

export type DocumentStatus = 'DRAFT' | 'APPROVED' | 'DISABLED'

export interface DocumentSummary {
  id: number
  title: string
  sourceId: string
  url: string | null
  license: string
  language: 'vi' | 'en'
  accessedAt: string | null
  status: DocumentStatus
  chunkCount: number
  embeddedCount: number
  updatedAt: string
}

export interface ChunkPreview {
  id: number
  index: number
  heading: string | null
  pageNumber: number | null
  length: number
  content: string
}

export interface DocumentDetail {
  document: DocumentSummary
  chunks: ChunkPreview[]
}

export interface RetrievalHit {
  chunkId: number
  documentId: number
  title: string
  heading: string | null
  pageNumber: number | null
  score: number
  usedInPrompt: boolean
  content: string
}

export interface RetrievalTest {
  question: string
  threshold: number
  sufficient: boolean
  hits: RetrievalHit[]
}

export interface DocumentMetadata {
  title: string
  sourceId: string
  url: string | null
  license: string
  language: 'vi' | 'en'
  accessedAt: string | null
}

/** Nguồn mở trong USE_CASES.md §4.1, kèm giấy phép gợi ý. */
export const OPEN_SOURCES: { id: string; name: string; license: string; language: 'vi' | 'en' }[] = [
  { id: 'SRC-01', name: 'Ngân hàng Nhà nước Việt Nam (sbv.gov.vn)', license: 'Văn bản nhà nước công khai', language: 'vi' },
  { id: 'SRC-02', name: 'Ủy ban Chứng khoán Nhà nước (ssc.gov.vn)', license: 'Văn bản nhà nước công khai', language: 'vi' },
  { id: 'SRC-03', name: 'CSDL văn bản pháp luật quốc gia (vbpl.vn)', license: 'Văn bản quy phạm pháp luật công khai', language: 'vi' },
  { id: 'SRC-04', name: 'Bảo hiểm xã hội Việt Nam', license: 'Văn bản nhà nước công khai', language: 'vi' },
  { id: 'SRC-05', name: 'CFPB "Your Money, Your Goals"', license: 'Public domain (U.S. Government work)', language: 'en' },
  { id: 'SRC-06', name: 'OECD/INFE — giáo dục tài chính', license: 'Tài liệu miễn phí của OECD', language: 'en' },
  { id: 'SRC-07', name: 'World Bank Open Knowledge Repository', license: 'CC BY 4.0', language: 'en' },
]

export const knowledgeApi = {
  list: (status?: DocumentStatus | '', keyword?: string) =>
    api.get<PageResponse<DocumentSummary>>('/api/admin/knowledge/documents' + qs({ status, keyword, size: 100 })),
  get: (id: number) => api.get<DocumentDetail>(`/api/admin/knowledge/documents/${id}`),

  upload: (file: Blob, filename: string, metadata: DocumentMetadata) => {
    const fd = new FormData()
    fd.append('file', file, filename)
    fd.append('metadata', JSON.stringify(metadata))
    return api.post<DocumentDetail>('/api/admin/knowledge/documents', fd)
  },
  importUrl: (metadata: DocumentMetadata) => api.post<DocumentDetail>('/api/admin/knowledge/documents/from-url', { ...metadata }),
  updateMetadata: (id: number, metadata: DocumentMetadata) =>
    api.put<DocumentSummary>(`/api/admin/knowledge/documents/${id}`, { ...metadata }),
  replaceContent: (id: number, file: Blob, filename: string) => {
    const fd = new FormData()
    fd.append('file', file, filename)
    return api.post<DocumentDetail>(`/api/admin/knowledge/documents/${id}/content`, fd)
  },
  approve: (id: number) => api.post<DocumentSummary>(`/api/admin/knowledge/documents/${id}/approve`),
  disable: (id: number) => api.post<DocumentSummary>(`/api/admin/knowledge/documents/${id}/disable`),
  remove: (id: number) => api.delete(`/api/admin/knowledge/documents/${id}`),
  reindex: () => api.post<{ documents: number; chunks: number }>('/api/admin/knowledge/reindex'),
  testQuery: (q: string) => api.get<RetrievalTest>('/api/admin/knowledge/test-query' + qs({ q })),
}

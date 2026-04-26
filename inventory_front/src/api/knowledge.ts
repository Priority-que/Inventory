import { request } from '@/utils/request'

export interface RagKnowledgeImportRequest {
  docCode: string
  title: string
  docType?: string
  bizIntent?: string
  sourcePath?: string
  content: string
}

export interface RagImportResultVO {
  docCode?: string
  title?: string
  bizIntent?: string
  chunkCount?: number
  message?: string
}

export interface RagSearchRequest {
  query?: string
  bizIntent?: string
  topK?: number
}

export interface RagSearchResultVO {
  id?: string
  docCode?: string
  title?: string
  docType?: string
  bizIntent?: string
  sourcePath?: string
  chunkNo?: number
  content?: string
  score?: number
}

export function importRagKnowledgeApi(data: RagKnowledgeImportRequest) {
  return request<RagImportResultVO>({
    url: '/agent/rag/import',
    method: 'post',
    data,
  })
}

export function searchRagKnowledgeApi(data: RagSearchRequest) {
  return request<RagSearchResultVO[]>({
    url: '/agent/rag/search',
    method: 'post',
    data,
  })
}

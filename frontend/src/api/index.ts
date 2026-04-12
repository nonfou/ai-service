import request from '../utils/request'
import type { AxiosResponse } from 'axios'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  adminId: number
  username: string
  role: string
}

export interface AdminApiKey {
  id: string
  keyName: string
  apiKey: string
  relayBaseUrl: string
  upstreamApiKey: string
  description?: string
  status: number
  lastUsedAt?: string
  createdAt: string
}

export type AdminApiKeyUsage = Record<string, unknown>

export interface TokenStatsSummaryCard {
  key: string
  label: string
  value: number
  unit?: string | null
  detail?: string | null
}

export interface TokenTrendItem {
  date: string
  requestCount: number
  inputTokens: number
  outputTokens: number
  cacheReadTokens: number
  cacheWriteTokens: number
  totalTokens: number
}

export interface TokenDistributionItem {
  name: string
  requestCount: number
  totalTokens: number
}

export interface TokenUsageRecordItem {
  id: number
  apiKeyId: number
  apiKeyName: string
  model: string
  endpoint: string
  requestType: string
  success: boolean
  inputTokens: number
  outputTokens: number
  cacheReadTokens: number
  cacheWriteTokens: number
  totalTokens: number
  firstTokenLatencyMs?: number | null
  durationMs?: number | null
  errorMessage?: string | null
  userAgent?: string | null
  createdAt: string
}

export interface TokenUsageSummary {
  requestCount: number
  successCount: number
  failureCount: number
  inputTokens: number
  outputTokens: number
  cacheReadTokens: number
  cacheWriteTokens: number
  totalTokens: number
  averageDurationMs: number
}

export interface TokenFilterOption {
  label: string
  value: string
}

export interface TokenStatsFilterOptions {
  days: number
  apiKeys: TokenFilterOption[]
  endpoints: TokenFilterOption[]
}

export interface PageMeta {
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface AdminTokenDashboard {
  days: number
  summaryCards: TokenStatsSummaryCard[]
  trend: TokenTrendItem[]
  modelDistribution: TokenDistributionItem[]
  endpointDistribution: TokenDistributionItem[]
  recentRequests: TokenUsageRecordItem[]
}

export interface AdminTokenUsageRecordPage {
  summary: TokenUsageSummary
  filters: TokenStatsFilterOptions
  records: TokenUsageRecordItem[]
  pagination: PageMeta
}

export interface AdminTokenUsageQuery {
  page?: number
  pageSize?: number
  days?: number
  apiKeyId?: string
  model?: string
  endpoint?: string
  success?: boolean
}

const asApiResponse = <T>(promise: Promise<unknown>) => {
  return promise as Promise<ApiResponse<T>>
}

export const adminAPI = {
  login: (data: AdminLoginRequest) => {
    return asApiResponse<AdminLoginResponse>(request.post('/admin/login', data))
  },

  getAdminApiKeys: () => {
    return asApiResponse<AdminApiKey[]>(request.get('/admin/api-keys'))
  },

  createAdminApiKey: (data: {
    keyName: string
    relayBaseUrl: string
    upstreamApiKey?: string
    description?: string
  }) => {
    return asApiResponse<AdminApiKey>(request.post('/admin/api-keys', data))
  },

  updateAdminApiKey: (keyId: string, data: {
    keyName?: string
    relayBaseUrl?: string
    upstreamApiKey?: string
    description?: string
  }) => {
    return asApiResponse<null>(request.put(`/admin/api-keys/${keyId}`, data))
  },

  updateAdminApiKeyStatus: (keyId: string, status: number) => {
    return asApiResponse<null>(request.put(`/admin/api-keys/${keyId}/status`, { status }))
  },

  deleteAdminApiKey: (keyId: string) => {
    return asApiResponse<null>(request.delete(`/admin/api-keys/${keyId}`))
  },

  regenerateAdminApiKey: (keyId: string) => {
    return asApiResponse<AdminApiKey>(request.post(`/admin/api-keys/${keyId}/regenerate`))
  },

  getAdminApiKeyUsage: (keyId: string) => {
    return asApiResponse<AdminApiKeyUsage>(request.get(`/admin/api-keys/${keyId}/usage`))
  },

  getTokenDashboard: (days = 7) => {
    return asApiResponse<AdminTokenDashboard>(request.get('/admin/token-stats/dashboard', {
      params: { days }
    }))
  },

  getTokenUsageRecords: (params: AdminTokenUsageQuery) => {
    return asApiResponse<AdminTokenUsageRecordPage>(request.get('/admin/token-stats/records', {
      params
    }))
  },

  exportTokenUsageRecords: (params: AdminTokenUsageQuery) => {
    return request.get('/admin/token-stats/records/export', {
      params,
      responseType: 'blob'
    }) as Promise<AxiosResponse<Blob>>
  }
}

export default request

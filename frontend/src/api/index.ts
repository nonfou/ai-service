import request from '../utils/request'

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
  }
}

export default request

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

export const adminAPI = {
  login: (data: AdminLoginRequest) => {
    return request.post<ApiResponse<AdminLoginResponse>>('/admin/login', data)
  },

  getAdminApiKeys: () => {
    return request.get<ApiResponse<AdminApiKey[]>>('/admin/api-keys')
  },

  createAdminApiKey: (data: {
    keyName: string
    relayBaseUrl: string
    upstreamApiKey?: string
    description?: string
  }) => {
    return request.post<ApiResponse<AdminApiKey>>('/admin/api-keys', data)
  },

  updateAdminApiKey: (keyId: string, data: {
    keyName?: string
    relayBaseUrl?: string
    upstreamApiKey?: string
    description?: string
  }) => {
    return request.put<ApiResponse<null>>(`/admin/api-keys/${keyId}`, data)
  },

  updateAdminApiKeyStatus: (keyId: string, status: number) => {
    return request.put<ApiResponse<null>>(`/admin/api-keys/${keyId}/status`, { status })
  },

  deleteAdminApiKey: (keyId: string) => {
    return request.delete<ApiResponse<null>>(`/admin/api-keys/${keyId}`)
  },

  regenerateAdminApiKey: (keyId: string) => {
    return request.post<ApiResponse<AdminApiKey>>(`/admin/api-keys/${keyId}/regenerate`)
  },

  getAdminApiKeyUsage: (keyId: string) => {
    return request.get<ApiResponse<AdminApiKeyUsage>>(`/admin/api-keys/${keyId}/usage`)
  }
}

export default request

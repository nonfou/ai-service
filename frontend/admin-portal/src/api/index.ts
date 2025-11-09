import request from '../utils/request'

// ==================== 后端返回结构 ====================
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// Page 分页结构 (MyBatis Plus)
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ==================== 管理员相关类型 ====================
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

export interface AdminUser {
  id: number
  username?: string  // 用户名(可能不存在)
  email: string
  apiKey: string
  balance: number
  totalRecharge?: number  // 累计充值
  status: number
  lastLoginAt?: string  // 最后登录时间
  createdAt: string
  updatedAt: string
}

// 导出 User 作为 AdminUser 的别名
export type User = AdminUser

export interface AdminUserStats {
  totalUsers: number
  activeUsers: number
  todayNewUsers: number
  totalBalance: number
}

export interface RechargeOrder {
  id: number
  userId: number
  orderNo: string
  amount: number
  payMethod: string
  status: number  // 0-待支付, 1-已支付, 2-已取消
  tradeNo?: string
  createdAt: string
  paidAt?: string
  updatedAt: string
}

export interface AdminOrderStats {
  totalOrders: number
  pendingOrders: number
  paidOrders: number
  todayOrders: number
  todayAmount: number
}

export interface Ticket {
  id: number
  userId: number
  subject: string
  content: string
  priority: string  // low, normal, high, urgent
  status: string  // pending, processing, resolved, closed
  createdAt: string
  updatedAt: string
}

export interface TicketMessage {
  id: number
  ticketId: number
  userId: number
  isAdmin: boolean
  message: string
  createdAt: string
}

export interface TicketDetail {
  ticket: Ticket
  messages: TicketMessage[]
}

export interface AdminTicketStats {
  totalTickets: number
  pendingTickets: number
  processingTickets: number
  resolvedTickets?: number
  closedTickets: number
  todayTickets: number
}

export interface SubscriptionPlan {
  id: number
  planName: string
  displayName: string
  description: string
  originalPrice: number
  price: number
  quotaAmount: number
  features: string[]
  status: number  // 0-禁用, 1-启用
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface Model {
  id: number
  modelName: string
  displayName: string
  provider: string
  priceMultiplier: number
  status: number  // 0-禁用, 1-启用
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface AdjustBalanceRequest {
  amount: number
  remark: string
}

export interface UpdateOrderStatusRequest {
  status: number
}

export interface CompleteOrderRequest {
  tradeNo?: string
}

export interface RefundOrderRequest {
  reason?: string
}

export interface UpdateTicketStatusRequest {
  status: string
}

export interface UpdateTicketPriorityRequest {
  priority: string
}

export interface AdminReplyTicketRequest {
  message: string
}

export interface UpdatePlanStatusRequest {
  status: number
}

// ==================== 用户统计相关类型 ====================
export interface UserTokenStatsResponse {
  totalInputTokens: number
  totalOutputTokens: number
  totalTokens: number
  totalCost: number
  totalCalls: number
  todayInputTokens: number
  todayOutputTokens: number
  todayTokens: number
  todayCost: number
  todayCalls: number
}

export interface TokenTrendResponse {
  date: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  cost: number
  calls: number
}

export interface ModelStatsResponse {
  model: string
  displayName: string
  calls: number
  successCalls: number
  failedCalls: number
  successRate: number
  totalInputTokens: number
  totalOutputTokens: number
  totalTokens: number
  totalCost: number
  avgDuration: number
  lastUsedAt: string
}

export interface ApiCall {
  id: number
  userId: number
  apiKey: string
  model: string
  inputTokens: number
  outputTokens: number
  cost: number
  requestTime: string
  responseTime: string
  duration: number
  status: number
  errorMsg: string
  createdAt: string
}

export interface BalanceLog {
  id: number
  userId: number
  amount: number
  balanceAfter: number
  type: string
  relatedId: number
  remark: string
  createdAt: string
}

export interface RechargeOrder {
  id: number
  userId: number
  orderNo: string
  amount: number
  status: number
  payMethod: string
  tradeNo: string
  payTime: string
  createdAt: string
  updatedAt: string
}

// ==================== 后端账户管理相关类型 ====================
export interface BackendAccount {
  id: number
  accountName: string
  provider: 'copilot' | 'openrouter'
  accessToken: string  // 前端显示时会脱敏
  priority: number
  status: 'active' | 'disabled' | 'error'
  dailyLimit?: number
  monthlyLimit?: number
  currentDailyUsage?: number
  currentMonthlyUsage?: number
  lastUsedAt?: string
  healthStatus?: string
  errorCount: number
  lastErrorMsg?: string
  createdAt: string
  updatedAt: string
}

export interface CreateBackendAccountRequest {
  accountName: string
  provider: 'copilot' | 'openrouter'
  accessToken: string
  priority: number
  dailyLimit?: number
  monthlyLimit?: number
}

export interface UpdateBackendAccountRequest {
  accountName?: string
  accessToken?: string
  priority?: number
  dailyLimit?: number
  monthlyLimit?: number
}

export interface UserQuota {
  id: number
  userId: number
  dailyLimit: number
  monthlyLimit: number
  currentDailyUsage: number
  currentMonthlyUsage: number
  lastResetDate: string
  createdAt: string
  updatedAt: string
}

export interface UpdateUserQuotaRequest {
  dailyLimit: number
  monthlyLimit: number
}


// ====================管理员 API ====================
export const adminAPI = {
  // ========== 管理员认证 ==========
  // 管理员登录
  login: (data: AdminLoginRequest) => {
    return request.post<ApiResponse<AdminLoginResponse>>('/api/admin/login', data)
  },

  // ========== 用户管理 ==========
  // 获取用户列表
  getUsers: (pageNum: number = 1, pageSize: number = 10, status?: number, userId?: string, email?: string) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status !== undefined) params.append('status', String(status))
    if (userId) params.append('userId', userId)
    if (email) params.append('email', email)
    return request.get<ApiResponse<PageResult<AdminUser>>>(
      `/api/admin/users?${params}`
    )
  },

  // 获取用户详情
  getUserDetail: (userId: number) => {
    return request.get<ApiResponse<AdminUser>>(`/api/admin/users/${userId}`)
  },

  // 更新用户状态
  updateUserStatus: (userId: number, status: number) => {
    return request.put<ApiResponse<null>>(`/api/admin/users/${userId}/status`, { status })
  },

  // 调整用户余额
  adjustUserBalance: (userId: number, data: AdjustBalanceRequest) => {
    return request.post<ApiResponse<null>>(`/api/admin/users/${userId}/adjust-balance`, data)
  },

  // 调整余额(别名,支持前端的 type/amount/reason 格式)
  adjustBalance: (userId: number, data: any) => {
    return request.post<ApiResponse<null>>(`/api/admin/users/${userId}/adjust-balance`, {
      amount: data.type === 'add' ? data.amount : -data.amount,
      remark: data.reason
    })
  },

  // 切换用户状态(启用/禁用)
  toggleUserStatus: (userId: number) => {
    // 先获取当前状态,再切换
    return request.get<ApiResponse<AdminUser>>(`/api/admin/users/${userId}`)
      .then((res: any) => {
        const newStatus = res.data.status === 1 ? 0 : 1
        return request.put<ApiResponse<null>>(`/api/admin/users/${userId}/status`, { status: newStatus })
      })
  },

  // 获取用户统计
  getUserStatistics: () => {
    return request.get<ApiResponse<AdminUserStats>>('/api/admin/users/statistics')
  },

  // ========== 订单管理 ==========
  // 获取订单列表
  getOrders: (pageNum: number = 1, pageSize: number = 10, status?: number, orderNo?: string) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status !== undefined) params.append('status', String(status))
    if (orderNo) params.append('orderNo', orderNo)
    return request.get<ApiResponse<PageResult<RechargeOrder>>>(
      `/api/admin/orders?${params}`
    )
  },

  // 获取订单详情
  getOrderDetail: (orderId: number) => {
    return request.get<ApiResponse<RechargeOrder>>(`/api/admin/orders/${orderId}`)
  },

  // 更新订单状态
  updateOrderStatus: (orderId: number, data: UpdateOrderStatusRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/orders/${orderId}/status`, data)
  },

  // 手动完成订单
  completeOrder: (orderId: number, data: CompleteOrderRequest) => {
    return request.post<ApiResponse<null>>(`/api/admin/orders/${orderId}/complete`, data)
  },

  // 退款
  refundOrder: (orderId: number, data: RefundOrderRequest) => {
    return request.post<ApiResponse<null>>(`/api/admin/orders/${orderId}/refund`, data)
  },

  // 获取订单统计
  getOrderStatistics: () => {
    return request.get<ApiResponse<AdminOrderStats>>('/api/admin/orders/statistics')
  },

  // ========== 工单管理 ==========
  // 获取工单列表
  getTickets: (pageNum: number = 1, pageSize: number = 10, status?: string, priority?: string, ticketId?: string) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status) params.append('status', status)
    if (priority) params.append('priority', priority)
    if (ticketId) params.append('ticketId', ticketId)
    return request.get<ApiResponse<PageResult<Ticket>>>(
      `/api/admin/tickets?${params}`
    )
  },

  // 获取工单详情
  getTicketDetail: (ticketId: number) => {
    return request.get<ApiResponse<TicketDetail>>(
      `/api/admin/tickets/${ticketId}`
    )
  },

  // 管理员回复工单
  replyTicket: (ticketId: number, data: AdminReplyTicketRequest) => {
    return request.post<ApiResponse<TicketMessage>>(`/api/admin/tickets/${ticketId}/reply`, data)
  },

  // 关闭工单
  closeTicket: (ticketId: number) => {
    return request.put<ApiResponse<null>>(`/api/admin/tickets/${ticketId}/status`, { status: 'closed' })
  },

  // 更新工单状态
  updateTicketStatus: (ticketId: number, data: UpdateTicketStatusRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/tickets/${ticketId}/status`, data)
  },

  // 更新工单优先级
  updateTicketPriority: (ticketId: number, data: UpdateTicketPriorityRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/tickets/${ticketId}/priority`, data)
  },

  // 获取工单统计
  getTicketStatistics: () => {
    return request.get<ApiResponse<AdminTicketStats>>('/api/admin/tickets/statistics')
  },

  // ========== 套餐管理 ==========
  // 获取所有套餐(包括禁用的)
  getPlans: () => {
    return request.get<ApiResponse<SubscriptionPlan[]>>('/api/admin/plans')
  },

  // 获取套餐详情
  getPlanDetail: (planId: number) => {
    return request.get<ApiResponse<SubscriptionPlan>>(`/api/admin/plans/${planId}`)
  },

  // 创建套餐
  createPlan: (data: SubscriptionPlan) => {
    return request.post<ApiResponse<SubscriptionPlan>>('/api/admin/plans', data)
  },

  // 更新套餐
  updatePlan: (planId: number, data: Partial<SubscriptionPlan>) => {
    return request.put<ApiResponse<null>>(`/api/admin/plans/${planId}`, data)
  },

  // 删除套餐(软删除)
  deletePlan: (planId: number) => {
    return request.delete<ApiResponse<null>>(`/api/admin/plans/${planId}`)
  },

  // 更新套餐状态
  updatePlanStatus: (planId: number, data: UpdatePlanStatusRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/plans/${planId}/status`, data)
  },

  // ========== 模型管理 ==========
  // 获取所有模型(包括禁用的)
  getModels: () => {
    return request.get<ApiResponse<Model[]>>('/api/admin/models')
  },

  // 更新模型
  updateModel: (modelId: number, data: Partial<Model>) => {
    return request.put<ApiResponse<null>>(`/api/admin/models/${modelId}`, data)
  },

  // 更新模型状态
  updateModelStatus: (modelId: number, status: number) => {
    return request.put<ApiResponse<null>>(`/api/admin/models/${modelId}/status`, { status })
  },

  // 删除模型
  deleteModel: (modelId: number) => {
    return request.delete<ApiResponse<null>>(`/api/admin/models/${modelId}`)
  },

  // ========== 统计数据 ==========
  // 获取平台统计数据
  getPlatformStatistics: () => {
    return request.get<ApiResponse<any>>('/api/admin/statistics')
  },

  // ========== 用户统计 ==========
  // 获取用户Token统计
  getUserTokenStats: (userId: number) => {
    return request.get<ApiResponse<UserTokenStatsResponse>>(`/api/admin/users/${userId}/token-stats`)
  },

  // 获取用户Token趋势
  getUserTokenTrend: (userId: number, days: number = 7) => {
    return request.get<ApiResponse<TokenTrendResponse[]>>(`/api/admin/users/${userId}/token-trend?days=${days}`)
  },

  // 获取用户模型使用统计
  getUserModelStats: (userId: number) => {
    return request.get<ApiResponse<ModelStatsResponse[]>>(`/api/admin/users/${userId}/model-stats`)
  },

  // 获取用户订单列表
  getUserOrders: (userId: number, pageNum: number = 1, pageSize: number = 10) => {
    return request.get<ApiResponse<PageResult<RechargeOrder>>>(
      `/api/admin/users/${userId}/orders?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取用户API调用日志
  getUserApiCalls: (userId: number, pageNum: number = 1, pageSize: number = 10) => {
    return request.get<ApiResponse<PageResult<ApiCall>>>(
      `/api/admin/users/${userId}/api-calls?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取用户余额日志
  getUserBalanceLogs: (userId: number, pageNum: number = 1, pageSize: number = 10) => {
    return request.get<ApiResponse<PageResult<BalanceLog>>>(
      `/api/admin/users/${userId}/balance-logs?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // ========== 后端账户管理 ==========
  // 获取所有后端账户
  getBackendAccounts: () => {
    return request.get<ApiResponse<BackendAccount[]>>('/api/admin/backend-accounts')
  },

  // 创建后端账户
  createBackendAccount: (data: CreateBackendAccountRequest) => {
    return request.post<ApiResponse<BackendAccount>>('/api/admin/backend-accounts', data)
  },

  // 更新后端账户
  updateBackendAccount: (accountId: number, data: UpdateBackendAccountRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/backend-accounts/${accountId}`, data)
  },

  // 删除后端账户
  deleteBackendAccount: (accountId: number) => {
    return request.delete<ApiResponse<null>>(`/api/admin/backend-accounts/${accountId}`)
  },

  // 启用/禁用后端账户
  toggleBackendAccount: (accountId: number, enabled: boolean) => {
    return request.put<ApiResponse<null>>(`/api/admin/backend-accounts/${accountId}/enable`, { enabled })
  },

  // 健康检查后端账户
  healthCheckBackendAccount: (accountId: number) => {
    return request.post<ApiResponse<{ healthy: boolean }>>(`/api/admin/backend-accounts/${accountId}/health-check`)
  },

  // ========== 用户配额管理 ==========
  // 获取用户配额
  getUserQuota: (userId: number) => {
    return request.get<ApiResponse<UserQuota>>(`/api/admin/users/${userId}/quota`)
  },

  // 更新用户配额
  updateUserQuota: (userId: number, data: UpdateUserQuotaRequest) => {
    return request.put<ApiResponse<null>>(`/api/admin/users/${userId}/quota`, data)
  }
}

export default request

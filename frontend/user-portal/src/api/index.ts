import request from '../utils/request'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ==================== 认证相关类型 ====================
export interface SendCodeRequest {
  email: string
}

export interface LoginRequest {
  email: string
  code: string
}

export interface LoginResponse {
  token: string
  userId: number
  email: string
  // apiKey: string  // ❌ 已删除 - 不再在前端存储API密钥
  balance: number
  username?: string  // 可选字段,如果后端不返回则使用email
}

// ==================== AI聊天相关类型 ====================
export interface ChatRequest {
  model: string
  messages: Array<{
    role: string
    content: string
  }>
  stream?: boolean
  maxTokens?: number
  temperature?: number
}

export interface ChatResponse {
  id: string
  model: string
  choices: Array<{
    message: {
      role: string
      content: string
    }
    finishReason: string
  }>
  usage: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

// ==================== API密钥相关类型 ====================
export interface ApiKey {
  id: number
  keyName: string
  apiKey: string
  status: number  // 0-禁用, 1-启用
  lastUsed?: string
  createdAt: string
}

export interface CreateApiKeyRequest {
  keyName: string
}

export interface UpdateApiKeyRequest {
  status: number
}

// ==================== 充值订单相关类型 ====================
export interface RechargeOrder {
  id: number
  orderNo: string
  amount: number
  payMethod: string
  status: number  // 0-待支付, 1-已支付, 2-已取消
  createdAt: string
  paidAt?: string
}

export interface CreateRechargeRequest {
  amount: number
}

export interface PaymentResponse {
  orderId: number
  orderNo: string
  amount: number
  paymentType: string
  clientSecret?: string  // Stripe PaymentIntent clientSecret
}


// ==================== 余额相关类型 ====================
export interface BalanceInfo {
  balance: number
  totalRecharge: number
  totalSpent: number
}

export interface BalanceLog {
  id: number
  type: number  // 1-充值, 2-消费, 3-退款
  amount: number
  balance: number
  description: string
  createdAt: string
}

export interface BalanceStatistics {
  totalRecharge: number
  totalSpent: number
  monthlySpent: number
  lastRechargeTime?: string
}

// ==================== 统计相关类型 ====================
export interface TodayStats {
  requests: number
  models: number
  cost: number
}

export interface ApiCall {
  id: number
  model: string
  tokens: number
  cost: number
  createdAt: string
}

export interface UsageTrend {
  date: string
  requests: number
  cost: number
}

export interface ModelUsage {
  model: string
  count: number
  percentage: number
}

// ==================== 模型相关类型 ====================
export interface Model {
  id: number
  modelName: string
  displayName: string
  provider: string
  priceMultiplier: number
  status: number  // 0-禁用, 1-启用
  description?: string
  icon?: string  // 模型图标URL
  contextLength?: number  // 上下文长度
  speed?: string  // 速度等级：fast, medium, slow
  tags?: string[]  // 模型标签数组，如 ['推荐', '低价', '新品']
}

// ==================== 用户相关类型 ====================
export interface UserInfo {
  userId: number
  email: string
  username?: string
  // apiKey?: string  // ❌ 已删除 - 不再在前端存储API密钥
  balance: number
  status?: number
  createdAt?: string
}

export interface UserStats {
  totalRequests: number
  totalCost: number
  apiKeysCount: number
}

// ==================== 订阅相关类型 ====================
// 支付类型
export const PaymentTypeEnum = {
  MONTHLY: 1,
  PAY_AS_GO: 2
} as const

export type PaymentType = (typeof PaymentTypeEnum)[keyof typeof PaymentTypeEnum]

export interface SubscriptionPlan {
  id: number
  planName: string
  displayName: string
  description: string
  originalPrice: number
  price: number
  quotaAmount: number
  features?: string[]
  colorTheme?: string
  badgeText?: string
  status: number  // 0-禁用, 1-启用
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface Subscription {
  id: number
  userId: number
  planId: number
  planName: string
  amount: number
  quotaAmount: number
  startDate: string
  endDate: string
  status: string  // active/expired/cancelled
  createdAt: string
  updatedAt: string
}

export interface SubscribeRequest {
  planId: number
}

// 认证相关 API
export const authAPI = {
  // 发送验证码
  sendCode: (data: SendCodeRequest) => {
    return request.post<any, ApiResponse<null>>('/api/auth/send-code', data)
  },

  // 登录/注册
  login: (data: LoginRequest) => {
    return request.post<any, ApiResponse<LoginResponse>>('/api/auth/login', data)
  },

  // 测试接口
  test: () => {
    return request.get('/api/auth/test')
  }
}

// AI 聊天 API
export const chatAPI = {
  // 发送消息
  chat: (data: ChatRequest, apiKey: string) => {
    return request.post<any, { data: ChatResponse }>('/api-chat', data, {
      headers: {
        'Authorization': `Bearer ${apiKey}`
      }
    })
  },

  // 获取模型列表
  getModels: () => {
    return request.get<any, { data: string[] }>('/api/models')
  }
}

// 用户相关 API
export const userAPI = {
  // 获取用户信息
  getInfo: () => {
    return request.get<any, { data: UserInfo }>('/api/user/info')
  },

  // 获取余额
  getBalance: () => {
    return request.get<any, { data: { balance: number } }>('/api/user/balance')
  },

  // 获取用户统计
  getStats: () => {
    return request.get<any, { data: UserStats }>('/api/user/stats')
  }
}

// ==================== API密钥管理 ====================
export const apiKeyAPI = {
  // 获取所有API密钥
  getApiKeys: () => {
    return request.get<any, { data: ApiKey[] }>('/api/user/api-keys')
  },

  // 创建API密钥
  createApiKey: (data: CreateApiKeyRequest) => {
    return request.post<any, { data: ApiKey }>('/api/user/api-keys', data)
  },

  // 更新密钥状态
  updateApiKey: (keyId: number, data: UpdateApiKeyRequest) => {
    return request.put<any, { data: null }>(`/api/user/api-keys/${keyId}`, data)
  },

  // 删除密钥
  deleteApiKey: (keyId: number) => {
    return request.delete<any, { data: null }>(`/api/user/api-keys/${keyId}`)
  },

  // 重新生成密钥
  regenerateApiKey: (keyId: number) => {
    return request.post<any, { data: ApiKey }>(`/api/user/api-keys/${keyId}/regenerate`)
  }
}

// ==================== 充值与订单 ====================
export const rechargeAPI = {
  // 创建充值订单
  createRecharge: (data: CreateRechargeRequest) => {
    return request.post<any, { data: PaymentResponse }>('/api/recharge/create', data)
  },

  // 查询订单支付状态
  queryOrder: (orderId: number) => {
    return request.get<any, { data: RechargeOrder }>(`/api/recharge/query/${orderId}`)
  },

  // 获取充值订单列表
  getRechargeOrders: (pageNum: number = 1, pageSize: number = 10) => {
    return request.get<any, { data: { list: RechargeOrder[], total: number } }>(
      `/api/recharge/orders?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取订单详情
  getOrderDetail: (orderId: number) => {
    return request.get<any, { data: RechargeOrder }>(`/api/recharge/orders/${orderId}`)
  }
}

// ==================== 余额管理 ====================
export const balanceAPI = {
  // 获取余额
  getBalance: () => {
    return request.get<any, { data: BalanceInfo }>('/api/balance')
  },

  // 获取余额变动日志
  getBalanceLogs: (pageNum: number = 1, pageSize: number = 10) => {
    return request.get<any, { data: { list: BalanceLog[], total: number } }>(
      `/api/balance/logs?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取余额统计
  getBalanceStatistics: () => {
    return request.get<any, { data: BalanceStatistics }>('/api/balance/statistics')
  }
}

// ==================== API调用统计 ====================
export const statisticsAPI = {
  // 获取API调用日志
  getApiCalls: (pageNum: number = 1, pageSize: number = 10) => {
    return request.get<any, { data: { list: ApiCall[], total: number } }>(
      `/api/api-calls?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取今日统计
  getTodayStats: () => {
    return request.get<any, { data: TodayStats }>('/api/statistics/today')
  },

  // 获取使用趋势
  getUsageTrend: (days: number = 7) => {
    return request.get<any, { data: UsageTrend[] }>(`/api/statistics/usage?days=${days}`)
  },

  // 获取模型使用统计
  getModelUsage: () => {
    return request.get<any, { data: ModelUsage[] }>('/api/statistics/model-usage')
  }
}

// ==================== 模型管理 ====================
export const modelsAPI = {
  // 获取模型列表
  getModels: () => {
    return request.get<any, { data: Model[] }>('/api/models')
  }
}

// ==================== 订阅管理 ====================
export const subscriptionAPI = {
  // 获取套餐列表
  getPlans: (paymentType?: PaymentType) => {
    const url = paymentType
      ? `/api/subscriptions/plans?paymentType=${paymentType}`
      : '/api/subscriptions/plans'
    return request.get<any, ApiResponse<SubscriptionPlan[]>>(url)
  },

  // 订阅套餐
  subscribe: (data: SubscribeRequest) => {
    return request.post<any, ApiResponse<Subscription>>('/api/subscriptions/subscribe', data)
  },

  // 获取订阅历史
  getHistory: (pageNum: number = 1, pageSize: number = 10) => {
    return request.get<any, ApiResponse<PageResult<Subscription>>>(
      `/api/subscriptions/history?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 取消订阅
  cancel: (subscriptionId: number) => {
    return request.post<any, ApiResponse<null>>(`/api/subscriptions/${subscriptionId}/cancel`)
  }
}

// ==================== 工单相关类型 ====================
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

export interface CreateTicketRequest {
  subject: string
  content: string
  priority?: string
}

export interface ReplyTicketRequest {
  message: string
}

// 工单相关 API
export const ticketAPI = {
  // 获取工单列表
  getTickets: (pageNum: number = 1, pageSize: number = 10) => {
    return request.get<any, { data: { records: Ticket[], total: number } }>(
      `/api/tickets?pageNum=${pageNum}&pageSize=${pageSize}`
    )
  },

  // 获取工单详情
  getTicketDetail: (ticketId: number) => {
    return request.get<any, { data: { ticket: Ticket, messages: TicketMessage[] } }>(
      `/api/tickets/${ticketId}`
    )
  },

  // 创建工单
  createTicket: (data: CreateTicketRequest) => {
    return request.post<any, { data: Ticket }>('/api/tickets', data)
  },

  // 回复工单
  replyTicket: (ticketId: number, data: ReplyTicketRequest) => {
    return request.post<any, { data: TicketMessage }>(`/api/tickets/${ticketId}/reply`, data)
  },

  // 关闭工单
  closeTicket: (ticketId: number) => {
    return request.post<any, { data: null }>(`/api/tickets/${ticketId}/close`)
  }
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
  email: string
  apiKey: string
  balance: number
  status: number
  createdAt: string
  updatedAt: string
}

export interface AdminUserStats {
  totalUsers: number
  activeUsers: number
  todayNewUsers: number
  totalBalance: number
}

export interface AdminOrderStats {
  totalOrders: number
  pendingOrders: number
  paidOrders: number
  todayOrders: number
  todayAmount: number
}

export interface AdminTicketStats {
  totalTickets: number
  pendingTickets: number
  processingTickets: number
  closedTickets: number
  todayTickets: number
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

// 管理员 API
export const adminAPI = {
  // 管理员登录
  login: (data: AdminLoginRequest) => {
    return request.post<any, { data: AdminLoginResponse }>('/api/admin/login', data)
  },

  // ========== 用户管理 ==========
  // 获取用户列表
  getUsers: (pageNum: number = 1, pageSize: number = 10, status?: number) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status !== undefined) params.append('status', String(status))
    return request.get<any, { data: { records: AdminUser[], total: number } }>(
      `/api/admin/users?${params}`
    )
  },

  // 获取用户详情
  getUserDetail: (userId: number) => {
    return request.get<any, { data: AdminUser }>(`/api/admin/users/${userId}`)
  },

  // 更新用户状态
  updateUserStatus: (userId: number, status: number) => {
    return request.put<any, { data: null }>(`/api/admin/users/${userId}/status`, { status })
  },

  // 调整用户余额
  adjustUserBalance: (userId: number, data: AdjustBalanceRequest) => {
    return request.post<any, { data: null }>(`/api/admin/users/${userId}/adjust-balance`, data)
  },

  // 获取用户统计
  getUserStatistics: () => {
    return request.get<any, { data: AdminUserStats }>('/api/admin/users/statistics')
  },

  // ========== 订单管理 ==========
  // 获取订单列表
  getOrders: (pageNum: number = 1, pageSize: number = 10, status?: number, orderNo?: string) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status !== undefined) params.append('status', String(status))
    if (orderNo) params.append('orderNo', orderNo)
    return request.get<any, { data: { records: RechargeOrder[], total: number } }>(
      `/api/admin/orders?${params}`
    )
  },

  // 获取订单详情
  getOrderDetail: (orderId: number) => {
    return request.get<any, { data: RechargeOrder }>(`/api/admin/orders/${orderId}`)
  },

  // 更新订单状态
  updateOrderStatus: (orderId: number, data: UpdateOrderStatusRequest) => {
    return request.put<any, { data: null }>(`/api/admin/orders/${orderId}/status`, data)
  },

  // 手动完成订单
  completeOrder: (orderId: number, data: CompleteOrderRequest) => {
    return request.post<any, { data: null }>(`/api/admin/orders/${orderId}/complete`, data)
  },

  // 退款
  refundOrder: (orderId: number, data: RefundOrderRequest) => {
    return request.post<any, { data: null }>(`/api/admin/orders/${orderId}/refund`, data)
  },

  // 获取订单统计
  getOrderStatistics: () => {
    return request.get<any, { data: AdminOrderStats }>('/api/admin/orders/statistics')
  },

  // ========== 工单管理 ==========
  // 获取工单列表
  getTickets: (pageNum: number = 1, pageSize: number = 10, status?: string, priority?: string) => {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    if (status) params.append('status', status)
    if (priority) params.append('priority', priority)
    return request.get<any, { data: { records: Ticket[], total: number } }>(
      `/api/admin/tickets?${params}`
    )
  },

  // 获取工单详情
  getTicketDetail: (ticketId: number) => {
    return request.get<any, { data: { ticket: Ticket, messages: TicketMessage[] } }>(
      `/api/admin/tickets/${ticketId}`
    )
  },

  // 管理员回复工单
  replyTicket: (ticketId: number, data: AdminReplyTicketRequest) => {
    return request.post<any, { data: TicketMessage }>(`/api/admin/tickets/${ticketId}/reply`, data)
  },

  // 更新工单状态
  updateTicketStatus: (ticketId: number, data: UpdateTicketStatusRequest) => {
    return request.put<any, { data: null }>(`/api/admin/tickets/${ticketId}/status`, data)
  },

  // 更新工单优先级
  updateTicketPriority: (ticketId: number, data: UpdateTicketPriorityRequest) => {
    return request.put<any, { data: null }>(`/api/admin/tickets/${ticketId}/priority`, data)
  },

  // 获取工单统计
  getTicketStatistics: () => {
    return request.get<any, { data: AdminTicketStats }>('/api/admin/tickets/statistics')
  },

  // ========== 套餐管理 ==========
  // 获取所有套餐(包括禁用的)
  getPlans: () => {
    return request.get<any, { data: SubscriptionPlan[] }>('/api/admin/plans')
  },

  // 获取套餐详情
  getPlanDetail: (planId: number) => {
    return request.get<any, { data: SubscriptionPlan }>(`/api/admin/plans/${planId}`)
  },

  // 创建套餐
  createPlan: (data: SubscriptionPlan) => {
    return request.post<any, { data: SubscriptionPlan }>('/api/admin/plans', data)
  },

  // 更新套餐
  updatePlan: (planId: number, data: Partial<SubscriptionPlan>) => {
    return request.put<any, { data: null }>(`/api/admin/plans/${planId}`, data)
  },

  // 删除套餐(软删除)
  deletePlan: (planId: number) => {
    return request.delete<any, { data: null }>(`/api/admin/plans/${planId}`)
  },

  // 更新套餐状态
  updatePlanStatus: (planId: number, data: UpdatePlanStatusRequest) => {
    return request.put<any, { data: null }>(`/api/admin/plans/${planId}/status`, data)
  },

  // ========== 模型管理 ==========
  // 获取所有模型(包括禁用的)
  getModels: () => {
    return request.get<any, { data: Model[] }>('/api/models/admin/all')
  },

  // 更新模型
  updateModel: (modelId: number, data: Partial<Model>) => {
    return request.put<any, { data: null }>(`/api/models/admin/${modelId}`, data)
  },

  // 更新模型状态
  updateModelStatus: (modelId: number, status: number) => {
    return request.put<any, { data: null }>(`/api/models/admin/${modelId}/status`, { status })
  }
}

// ==================== 简化版 API(用于直接导出) ====================

// 认证相关
export const sendCodeAPI = (data: SendCodeRequest) => authAPI.sendCode(data)
export const loginAPI = (data: LoginRequest) => authAPI.login(data)
export const testAPI = () => authAPI.test()

// 用户相关
export const getUserInfoAPI = () => userAPI.getInfo()
export const getBalanceAPI = () => balanceAPI.getBalance()
export const getUserStatsAPI = () => userAPI.getStats()

// 今日统计
export const getTodayStatisticsAPI = () => statisticsAPI.getTodayStats()

// API Key 相关
export interface ApiKeyResponse {
  id: number
  name: string
  key: string
  status: number
  createdAt: string
  lastUsedAt?: string
}

export const getApiKeysAPI = async (): Promise<ApiKeyResponse[]> => {
  const res = await request.get('/api/apikeys')
  return res.data.data
}

export const createApiKeyAPI = async (data: { name: string }): Promise<{ key: string }> => {
  const res = await request.post('/api/apikeys', data)
  return res.data.data
}

export const updateApiKeyStatusAPI = async (id: number, data: { status: number }) => {
  const res = await request.put(`/api/apikeys/${id}/status`, data)
  return res.data
}

export const regenerateApiKeyAPI = async (id: number): Promise<{ key: string }> => {
  const res = await request.post(`/api/apikeys/${id}/regenerate`)
  return res.data.data
}

export const deleteApiKeyAPI = async (id: number) => {
  const res = await request.delete(`/api/apikeys/${id}`)
  return res.data
}

// 使用统计相关
export interface UsageTrendResponse {
  date: string
  callCount: number
  totalCost: string
}

export interface ModelUsageResponse {
  modelName: string
  callCount: number
  totalCost: string
  successRate: number
  avgResponseTime: number
  lastUsedAt: string
}

export const getUsageTrendAPI = async (params: { days: number }): Promise<UsageTrendResponse[]> => {
  const res = await request.get(`/api/statistics/usage-trend?days=${params.days}`)
  return res.data.data
}

export const getModelUsageStatisticsAPI = async (): Promise<ModelUsageResponse[]> => {
  const res = await request.get('/api/statistics/model-usage')
  return res.data.data
}

// API调用日志相关
export interface ApiCallResponse {
  id: number
  modelName: string
  status: string
  responseTime: number
  cost: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  createdAt: string
  apiKeyName?: string
  requestBody?: string
  responseBody?: string
  errorMessage?: string
  requestIp?: string
  userAgent?: string
}

export const getApiCallsAPI = async (params: {
  page: number
  pageSize: number
  modelId?: string
  status?: string
  startDate?: string
  endDate?: string
}): Promise<{ records: ApiCallResponse[], total: number }> => {
  const queryParams = new URLSearchParams()
  queryParams.append('page', String(params.page))
  queryParams.append('pageSize', String(params.pageSize))
  if (params.modelId) queryParams.append('modelId', params.modelId)
  if (params.status) queryParams.append('status', params.status)
  if (params.startDate) queryParams.append('startDate', params.startDate)
  if (params.endDate) queryParams.append('endDate', params.endDate)

  const res = await request.get(`/api/api-calls?${queryParams}`)
  return res.data.data
}

// 模型列表
export const getModelsAPI = async () => {
  const res = await request.get('/api/models')
  return res.data.data
}

// 订阅套餐相关
export const getPlansAPI = () => subscriptionAPI.getPlans()
export const subscribePlanAPI = (data: SubscribeRequest) => subscriptionAPI.subscribe(data)
export const getSubscriptionHistoryAPI = (pageNum?: number, pageSize?: number) =>
  subscriptionAPI.getHistory(pageNum, pageSize)

// 充值相关
export const createRechargeOrderAPI = async (data: { amount: number, paymentMethod: string }) => {
  const res = await request.post('/api/recharge/create', {
    amount: data.amount,
    payMethod: data.paymentMethod
  })
  return res.data.data
}

export const getRechargeHistoryAPI = async () => {
  const res = await request.get('/api/recharge/orders')
  return res.data.data
}

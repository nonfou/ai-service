<template>
  <div class="tickets-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
          <div class="header-left">
            <h1>工单</h1>
            <p>提交和管理您的技术支持工单</p>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              创建工单
            </el-button>
          </div>
        </div>

        <!-- 工单列表 -->
        <div class="tickets-section">
          <el-table :data="tickets" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="工单 ID" width="120" />
            <el-table-column prop="title" label="标题" min-width="300" />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag
                  :type="getStatusType(scope.row.status)"
                  size="small"
                >
                  {{ scope.row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="100">
              <template #default="scope">
                <el-tag
                  :type="getPriorityType(scope.row.priority)"
                  size="small"
                  effect="plain"
                >
                  {{ scope.row.priorityText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column prop="updatedAt" label="最后更新" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="viewTicket(scope.row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 创建工单对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建工单"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="createForm.title"
            placeholder="请简要描述您的问题"
          />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="createForm.priority">
            <el-radio label="low">低</el-radio>
            <el-radio label="normal">普通</el-radio>
            <el-radio label="high">高</el-radio>
            <el-radio label="urgent">紧急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="createForm.category" placeholder="请选择问题分类">
            <el-option label="账户问题" value="account" />
            <el-option label="订阅问题" value="subscription" />
            <el-option label="API 使用" value="api" />
            <el-option label="计费问题" value="billing" />
            <el-option label="技术支持" value="technical" />
            <el-option label="功能建议" value="feature" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述您遇到的问题..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">
          提交工单
        </el-button>
      </template>
    </el-dialog>

    <!-- 工单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="`工单详情 - ${currentTicket?.id}`"
      width="700px"
    >
      <div v-if="currentTicket" class="ticket-detail">
        <div class="detail-header">
          <h3>{{ currentTicket.title }}</h3>
          <div class="detail-meta">
            <el-tag :type="getStatusType(currentTicket.status)" size="small">
              {{ currentTicket.statusText }}
            </el-tag>
            <el-tag :type="getPriorityType(currentTicket.priority)" size="small" effect="plain">
              {{ currentTicket.priorityText }}
            </el-tag>
            <span class="meta-item">创建于 {{ currentTicket.createdAt }}</span>
          </div>
        </div>

        <div class="detail-content">
          <h4>问题描述</h4>
          <p>{{ currentTicket.description }}</p>
        </div>

        <div class="replies-section">
          <h4>回复记录</h4>
          <div v-if="currentTicket.replies && currentTicket.replies.length > 0" class="replies-list">
            <div
              v-for="(reply, idx) in currentTicket.replies"
              :key="idx"
              class="reply-item"
              :class="{ 'admin-reply': reply.isAdmin }"
            >
              <div class="reply-header">
                <span class="reply-author">
                  {{ reply.isAdmin ? '客服团队' : '我' }}
                </span>
                <span class="reply-time">{{ reply.time }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
            </div>
          </div>
          <div v-else class="no-replies">
            暂无回复
          </div>

          <div v-if="currentTicket.status !== 'closed'" class="reply-form">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="4"
              placeholder="输入您的回复..."
            />
            <el-button
              type="primary"
              @click="submitReply"
              style="margin-top: 12px;"
            >
              发送回复
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { ticketAPI, type Ticket, type TicketMessage } from '../api'

// 工单列表数据
interface DisplayTicket {
  id: string
  title: string
  status: string
  statusText: string
  priority: string
  priorityText: string
  category?: string
  description: string
  createdAt: string
  updatedAt: string
  replies?: Array<{
    isAdmin: boolean
    author: string
    time: string
    content: string
  }>
}

const tickets = ref<DisplayTicket[]>([])
const loading = ref(false)

// 创建工单对话框
const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref({
  title: '',
  priority: 'normal',
  category: '',
  description: ''
})

const createRules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择问题分类', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入详细描述', trigger: 'blur' },
    { min: 10, message: '请至少输入 10 个字符', trigger: 'blur' }
  ]
}

// 工单详情对话框
const detailDialogVisible = ref(false)
const currentTicket = ref<DisplayTicket | null>(null)
const replyContent = ref('')
const detailLoading = ref(false)

// 页面加载时获取工单列表
onMounted(() => {
  loadTickets()
})

// 加载工单列表
const loadTickets = async () => {
  try {
    loading.value = true
    const res = await ticketAPI.getTickets(1, 20)
    if (res.data && res.data.records) {
      tickets.value = res.data.records.map((ticket: Ticket) => ({
        id: `#${ticket.id}`,
        title: ticket.subject,
        status: ticket.status,
        statusText: getStatusText(ticket.status),
        priority: ticket.priority,
        priorityText: getPriorityText(ticket.priority),
        description: ticket.content,
        createdAt: formatDateTime(ticket.createdAt),
        updatedAt: formatDateTime(ticket.updatedAt),
        replies: []
      }))
    }
  } catch (error: any) {
    console.error('加载工单列表失败:', error)
    // ✅ 认证错误由响应拦截器统一处理,这里不显示通用错误
    // 避免与拦截器重复提示
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  createDialogVisible.value = true
}

const confirmCreate = async () => {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await ticketAPI.createTicket({
          subject: createForm.value.title,
          content: createForm.value.description,
          priority: createForm.value.priority
        })

        ElMessage.success('工单已提交')
        createDialogVisible.value = false
        createForm.value = {
          title: '',
          priority: 'normal',
          category: '',
          description: ''
        }

        // 刷新工单列表
        await loadTickets()
      } catch (error: any) {
        console.error('创建工单失败:', error)
        // ✅ 认证错误由响应拦截器统一处理
        // 其他业务错误可以显示
        if (error.response?.status && error.response.status >= 400 && error.response.status < 500 && error.response.status !== 401 && error.response.status !== 403) {
          ElMessage.error(error.response.data?.message || '创建工单失败')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const viewTicket = async (ticket: DisplayTicket) => {
  try {
    detailLoading.value = true
    const ticketId = parseInt(ticket.id.replace('#', ''))
    const res = await ticketAPI.getTicketDetail(ticketId)

    if (res.data) {
      currentTicket.value = {
        ...ticket,
        description: res.data.ticket.content,
        replies: res.data.messages.map((msg: TicketMessage) => ({
          isAdmin: msg.isAdmin,
          author: msg.isAdmin ? '客服团队' : '我',
          time: formatDateTime(msg.createdAt),
          content: msg.message
        }))
      }
      detailDialogVisible.value = true
    }
  } catch (error: any) {
    console.error('加载工单详情失败:', error)
    // ✅ 认证错误由响应拦截器统一处理(401/403),这里不再显示通用错误
    // 只在特定情况下显示错误(如404等业务错误)
    if (error.response?.status === 404) {
      ElMessage.error('工单不存在')
    }
    // 其他错误不显示,避免与拦截器重复提示
  } finally {
    detailLoading.value = false
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  if (currentTicket.value) {
    try {
      const ticketId = parseInt(currentTicket.value.id.replace('#', ''))
      await ticketAPI.replyTicket(ticketId, {
        message: replyContent.value
      })

      ElMessage.success('回复已发送')
      replyContent.value = ''

      // 重新加载工单详情
      await viewTicket(currentTicket.value)
    } catch (error: any) {
      console.error('回复工单失败:', error)
      // ✅ 认证错误由响应拦截器统一处理
      // 其他业务错误可以显示
      if (error.response?.status && error.response.status >= 400 && error.response.status < 500 && error.response.status !== 401 && error.response.status !== 403) {
        ElMessage.error(error.response.data?.message || '回复工单失败')
      }
    }
  }
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    pending: 'warning',
    processing: 'primary',
    resolved: 'success',
    closed: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    resolved: '已解决',
    closed: '已关闭'
  }
  return textMap[status] || status
}

const getPriorityType = (priority: string) => {
  const typeMap: Record<string, any> = {
    low: 'info',
    normal: '',
    high: 'warning',
    urgent: 'danger'
  }
  return typeMap[priority] || ''
}

const getPriorityText = (priority: string) => {
  const textMap: Record<string, string> = {
    low: '低',
    normal: '普通',
    high: '高',
    urgent: '紧急'
  }
  return textMap[priority] || '普通'
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\//g, '-')
}
</script>

<style scoped>
.tickets-page {
  min-height: calc(100vh - 64px);
  padding: 60px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9d5ff 100%);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.header-left h1 {
  font-size: 36px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.header-left p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.tickets-section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.ticket-detail {
  padding: 8px 0;
}

.detail-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.detail-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: #6b7280;
}

.detail-content {
  padding: 24px 0;
  border-bottom: 1px solid #e5e7eb;
}

.detail-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.detail-content p {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

.replies-section {
  padding: 24px 0;
}

.replies-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.reply-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}

.reply-item.admin-reply {
  background: #eff6ff;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reply-author {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.reply-time {
  font-size: 12px;
  color: #9ca3af;
}

.reply-content {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
}

.no-replies {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 14px;
}

.reply-form {
  border-top: 1px solid #e5e7eb;
  padding-top: 24px;
}

/* Responsive */
@media (max-width: 768px) {
  .tickets-page {
    padding: 40px 0;
  }

  .page-header {
    flex-direction: column;
    gap: 20px;
  }

  .header-left h1 {
    font-size: 28px;
  }

  .header-right {
    width: 100%;
  }

  .header-right .el-button {
    width: 100%;
  }

  .tickets-section {
    padding: 20px;
    overflow-x: auto;
  }
}
</style>

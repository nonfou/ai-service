<template>
  <div class="tickets-page">
    <!-- 页面头部 -->
    <PageHeader title="工单管理" description="处理用户反馈和技术支持请求" />

    <!-- 筛选条件 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">工单ID</label>
          <el-input
            v-model="ticketIdFilter"
            placeholder="请输入工单ID"
            clearable
            style="width: 150px"
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">状态</label>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">优先级</label>
          <el-select v-model="priorityFilter" placeholder="全部优先级" clearable style="width: 120px">
            <el-option label="低" value="low" />
            <el-option label="普通" value="normal" />
            <el-option label="高" value="high" />
            <el-option label="紧急" value="urgent" />
          </el-select>
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 工单列表 -->
    <div class="table-container">
      <el-table :data="tickets" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="subject" label="主题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="ticket-subject">{{ row.subject }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="优先级" width="100" align="center">
          <template #default="{ row }">
            <span class="priority-badge" :class="getPriorityClass(row.priority)">
              {{ getPriorityText(row.priority) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="getStatusClass(row.status)">
              {{ getStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="viewTicket(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button
                v-if="row.status !== 'closed'"
                size="small"
                type="danger"
                @click="closeTicket(row)"
              >
                关闭
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchTickets"
          @size-change="fetchTickets"
        />
      </div>
    </div>

    <!-- 工单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="工单详情" width="800px" class="ticket-dialog">
      <div v-if="ticketDetail" class="ticket-detail">
        <!-- 工单信息 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">工单ID</span>
            <span class="info-value">{{ ticketDetail.ticket.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">状态</span>
            <span class="status-badge" :class="getStatusClass(ticketDetail.ticket.status)">
              {{ getStatusText(ticketDetail.ticket.status) }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">用户ID</span>
            <span class="info-value">{{ ticketDetail.ticket.userId }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">优先级</span>
            <span class="priority-badge" :class="getPriorityClass(ticketDetail.ticket.priority)">
              {{ getPriorityText(ticketDetail.ticket.priority) }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ ticketDetail.ticket.createdAt }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间</span>
            <span class="info-value">{{ ticketDetail.ticket.updatedAt }}</span>
          </div>
          <div class="info-item full-width">
            <span class="info-label">主题</span>
            <span class="info-value ticket-subject">{{ ticketDetail.ticket.subject }}</span>
          </div>
        </div>

        <!-- 问题描述 -->
        <div class="section">
          <h4 class="section-title">问题描述</h4>
          <div class="content-box">{{ ticketDetail.ticket.content }}</div>
        </div>

        <!-- 回复列表 -->
        <div class="section">
          <h4 class="section-title">回复记录</h4>
          <div class="replies-list">
            <div
              v-for="msg in ticketDetail.messages"
              :key="msg.id"
              class="reply-item"
              :class="{ 'admin-reply': msg.isAdmin }"
            >
              <div class="reply-header">
                <span class="reply-author" :class="{ 'admin': msg.isAdmin }">
                  {{ msg.isAdmin ? '管理员' : '用户' }}
                </span>
                <span class="reply-time">{{ msg.createdAt }}</span>
              </div>
              <div class="reply-content">{{ msg.message }}</div>
            </div>
            <el-empty v-if="!ticketDetail.messages?.length" description="暂无回复" />
          </div>
        </div>

        <!-- 回复表单 -->
        <div v-if="ticketDetail.ticket.status !== 'closed'" class="section">
          <h4 class="section-title">添加回复</h4>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
          />
          <div class="reply-actions">
            <el-button @click="replyContent = ''">清空</el-button>
            <el-button type="primary" :loading="replying" @click="submitReply">
              提交回复
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { adminAPI, type Ticket, type TicketDetail } from '../api'
import message from '../utils/message'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const replying = ref(false)
const tickets = ref<Ticket[]>([])
const ticketDetail = ref<TicketDetail | null>(null)
const detailDialogVisible = ref(false)
const replyContent = ref('')

const ticketIdFilter = ref('')
const statusFilter = ref<string | undefined>(undefined)
const priorityFilter = ref<string | undefined>(undefined)

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const getStatusClass = (status: string) => {
  const classMap: Record<string, string> = {
    'pending': 'status-warning',
    'processing': 'status-primary',
    'resolved': 'status-active',
    'closed': 'status-muted'
  }
  return classMap[status] || 'status-muted'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    'pending': '待处理',
    'processing': '处理中',
    'resolved': '已解决',
    'closed': '已关闭'
  }
  return textMap[status] || '未知'
}

const getPriorityClass = (priority: string) => {
  const classMap: Record<string, string> = {
    'low': 'priority-low',
    'normal': 'priority-normal',
    'high': 'priority-high',
    'urgent': 'priority-urgent'
  }
  return classMap[priority] || 'priority-normal'
}

const getPriorityText = (priority: string) => {
  const textMap: Record<string, string> = {
    'low': '低',
    'normal': '普通',
    'high': '高',
    'urgent': '紧急'
  }
  return textMap[priority] || priority
}

const fetchTickets = async () => {
  try {
    loading.value = true
    const res = await adminAPI.getTickets(
      pagination.value.page,
      pagination.value.pageSize,
      statusFilter.value,
      priorityFilter.value,
      ticketIdFilter.value || undefined
    )

    tickets.value = res.data.records
    pagination.value.total = res.data.total
  } catch (error: any) {
    // 认证错误由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  fetchTickets()
}

const handleReset = () => {
  ticketIdFilter.value = ''
  statusFilter.value = undefined
  priorityFilter.value = undefined
  pagination.value.page = 1
  fetchTickets()
}

const viewTicket = async (row: Ticket) => {
  try {
    const res = await adminAPI.getTicketDetail(row.id)
    ticketDetail.value = res.data
    detailDialogVisible.value = true
    replyContent.value = ''
  } catch (error: any) {
    if (error.response?.status === 404) {
      message.error('工单不存在')
    }
  }
}

const closeTicket = async (row: Ticket) => {
  try {
    await ElMessageBox.confirm('确定要关闭该工单吗?', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await adminAPI.closeTicket(row.id)
    message.success('工单已关闭')
    await fetchTickets()
  } catch (error: any) {
    if (error !== 'cancel') {
      if (error.response?.status && error.response.status !== 401 && error.response.status !== 403) {
        message.error(error.response?.data?.message || '关闭失败')
      }
    }
  }
}

const submitReply = async () => {
  if (!ticketDetail.value || !replyContent.value.trim()) {
    message.warning('请输入回复内容')
    return
  }

  try {
    replying.value = true
    await adminAPI.replyTicket(ticketDetail.value.ticket.id, {
      message: replyContent.value
    })

    message.success('回复成功')
    replyContent.value = ''

    const res = await adminAPI.getTicketDetail(ticketDetail.value.ticket.id)
    ticketDetail.value = res.data
  } catch (error: any) {
    if (error.response?.status && error.response.status !== 401 && error.response.status !== 403) {
      message.error(error.response?.data?.message || '回复失败')
    }
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  fetchTickets()
})
</script>

<style scoped>
.tickets-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 筛选栏 */
.filter-bar {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  border: 1px solid var(--border-light);
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

/* 表格容器 */
.table-container {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.ticket-subject {
  font-weight: 500;
  color: var(--text-primary);
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: var(--success-bg);
  color: var(--success-color);
}

.status-warning {
  background: var(--warning-bg);
  color: var(--warning-color);
}

.status-primary {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.status-muted {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

/* 优先级徽章 */
.priority-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.priority-low {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

.priority-normal {
  background: var(--info-bg);
  color: var(--info-color);
}

.priority-high {
  background: var(--warning-bg);
  color: var(--warning-color);
}

.priority-urgent {
  background: var(--danger-bg);
  color: var(--danger-color);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* 工单详情 */
.ticket-detail {
  max-height: 70vh;
  overflow-y: auto;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.info-item.full-width {
  grid-column: span 2;
}

.info-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.info-value {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 区块 */
.section {
  margin-top: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.content-box {
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 回复列表 */
.replies-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
}

.reply-item {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-item.admin-reply {
  background: var(--primary-bg);
  border-left: 3px solid var(--primary-color);
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.reply-author {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 2px 8px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
}

.reply-author.admin {
  background: var(--primary-color);
  color: white;
}

.reply-time {
  font-size: 12px;
  color: var(--text-muted);
}

.reply-content {
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.reply-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
    margin-top: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.full-width {
    grid-column: span 1;
  }
}
</style>

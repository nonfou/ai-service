<template>
  <div class="tickets-page">
    <!-- 筛选条件 -->
    <el-card style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="工单ID">
          <el-input
            v-model="ticketIdFilter"
            placeholder="请输入工单ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-select v-model="priorityFilter" placeholder="全部优先级" clearable style="width: 120px">
            <el-option label="低" value="low" />
            <el-option label="普通" value="normal" />
            <el-option label="高" value="high" />
            <el-option label="紧急" value="urgent" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工单列表 -->
    <el-card>
      <el-table :data="tickets" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="subject" label="主题" min-width="200" show-overflow-tooltip />

        <el-table-column prop="priority" label="优先级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />

        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
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
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="fetchTickets"
        @size-change="fetchTickets"
      />
    </el-card>

    <!-- 工单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="工单详情" width="800px">
      <div v-if="ticketDetail" class="ticket-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单ID">{{ ticketDetail.ticket.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(ticketDetail.ticket.status)">
              {{ getStatusText(ticketDetail.ticket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ ticketDetail.ticket.userId }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(ticketDetail.ticket.priority)">
              {{ getPriorityText(ticketDetail.ticket.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ ticketDetail.ticket.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ ticketDetail.ticket.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="主题" :span="2">{{ ticketDetail.ticket.subject }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 10px">问题描述:</h4>
          <div class="content-box">{{ ticketDetail.ticket.content }}</div>
        </div>

        <!-- 回复列表 -->
        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 10px">回复记录:</h4>
          <div class="replies-list">
            <div
              v-for="message in ticketDetail.messages"
              :key="message.id"
              class="reply-item"
              :class="{ 'admin-reply': message.isAdmin }"
            >
              <div class="reply-header">
                <span class="reply-author">
                  {{ message.isAdmin ? '[管理员]' : '[用户]' }}
                </span>
                <span class="reply-time">{{ message.createdAt }}</span>
              </div>
              <div class="reply-content">{{ message.message }}</div>
            </div>
            <el-empty v-if="!ticketDetail.messages?.length" description="暂无回复" />
          </div>
        </div>

        <!-- 回复表单 -->
        <div v-if="ticketDetail.ticket.status !== 'closed'" style="margin-top: 20px">
          <h4 style="margin-bottom: 10px">添加回复:</h4>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
          />
          <div style="margin-top: 10px; text-align: right">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { adminAPI, type Ticket, type TicketDetail } from '../api'

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

const getStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    'pending': 'warning',
    'processing': 'primary',
    'resolved': 'success',
    'closed': 'info'
  }
  return typeMap[status] || 'info'
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

const getPriorityType = (priority: string) => {
  const typeMap: Record<string, any> = {
    'low': 'info',
    'normal': '',
    'high': 'warning',
    'urgent': 'danger'
  }
  return typeMap[priority] || ''
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
    ElMessage.error('获取工单列表失败')
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
    ElMessage.error('获取工单详情失败')
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
    ElMessage.success('工单已关闭')
    await fetchTickets()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '关闭失败')
    }
  }
}

const submitReply = async () => {
  if (!ticketDetail.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    replying.value = true
    await adminAPI.replyTicket(ticketDetail.value.ticket.id, {
      message: replyContent.value
    })

    ElMessage.success('回复成功')
    replyContent.value = ''

    // 重新获取工单详情
    const res = await adminAPI.getTicketDetail(ticketDetail.value.ticket.id)
    ticketDetail.value = res.data
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '回复失败')
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
  padding: 0;
}

:deep(.el-pagination) {
  display: flex;
}

.ticket-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.content-box {
  padding: 12px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.replies-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 12px;
}

.reply-item {
  margin-bottom: 16px;
  padding: 12px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.reply-item.admin-reply {
  background-color: #e8f4ff;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
}

.reply-author {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.reply-time {
  color: var(--el-text-color-secondary);
}

.reply-content {
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

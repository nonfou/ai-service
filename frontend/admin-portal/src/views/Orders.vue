<template>
  <div class="orders-page">
    <!-- 页面头部 -->
    <PageHeader title="订单管理" description="管理用户充值订单、退款和支付状态" />

    <!-- 筛选条件 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">订单号</label>
          <el-input
            v-model="orderNoFilter"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">状态</label>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
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

    <!-- 订单列表 -->
    <div class="table-container">
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount-value">¥{{ row.amount }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="payMethod" label="支付方式" width="120">
          <template #default="{ row }">
            <span class="pay-method-badge">{{ getPayMethodText(row.payMethod) }}</span>
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
        <el-table-column prop="paidAt" label="支付时间" width="180">
          <template #default="{ row }">
            {{ row.paidAt || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="viewOrder(row)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button
                v-if="row.status === 0"
                size="small"
                type="success"
                @click="completeOrder(row)"
              >
                完成订单
              </el-button>
              <el-button
                v-if="row.status === 1"
                size="small"
                type="danger"
                @click="refundOrder(row)"
              >
                退款
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
          @current-change="fetchOrders"
          @size-change="fetchOrders"
        />
      </div>
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="560px">
      <div class="info-grid" v-if="currentOrder">
        <div class="info-item">
          <span class="info-label">订单ID</span>
          <span class="info-value">{{ currentOrder.id }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">订单号</span>
          <span class="info-value order-no">{{ currentOrder.orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">金额</span>
          <span class="info-value amount-value">¥{{ currentOrder.amount }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">支付方式</span>
          <span class="info-value">{{ getPayMethodText(currentOrder.payMethod) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">状态</span>
          <span class="status-badge" :class="getStatusClass(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ currentOrder.createdAt }}</span>
        </div>
        <div class="info-item full-width">
          <span class="info-label">支付时间</span>
          <span class="info-value">{{ currentOrder.paidAt || '-' }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 完成订单对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成订单" width="480px">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="订单号">
          <span class="form-text order-no">{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="金额">
          <span class="amount-value">¥{{ currentOrder?.amount }}</span>
        </el-form-item>
        <el-form-item label="交易流水号">
          <el-input
            v-model="completeForm.tradeNo"
            placeholder="可选,输入第三方支付流水号"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="completing" @click="handleComplete">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 退款对话框 -->
    <el-dialog v-model="refundDialogVisible" title="订单退款" width="480px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="订单号">
          <span class="form-text order-no">{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span class="refund-amount">¥{{ currentOrder?.amount }}</span>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入退款原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="refunding" @click="handleRefund">
          确定退款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { adminAPI, type RechargeOrder } from '../api'
import message from '../utils/message'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const completing = ref(false)
const refunding = ref(false)
const orders = ref<RechargeOrder[]>([])
const currentOrder = ref<RechargeOrder | null>(null)
const detailDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const refundDialogVisible = ref(false)

const orderNoFilter = ref('')
const statusFilter = ref<number | undefined>(undefined)

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const completeForm = ref({
  tradeNo: ''
})

const refundForm = ref({
  reason: ''
})

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'status-warning',
    1: 'status-active',
    2: 'status-muted'
  }
  return classMap[status] || 'status-muted'
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '已取消'
  }
  return textMap[status] || '未知'
}

const getPayMethodText = (payMethod: string) => {
  const methodMap: Record<string, string> = {
    'stripe': 'Stripe',
    'pending': '待支付',
    'alipay': '支付宝',
    'wechat': '微信支付'
  }
  return methodMap[payMethod] || payMethod || '-'
}

const fetchOrders = async () => {
  try {
    loading.value = true
    const res = await adminAPI.getOrders(
      pagination.value.page,
      pagination.value.pageSize,
      statusFilter.value,
      orderNoFilter.value || undefined
    )

    orders.value = res.data.records
    pagination.value.total = res.data.total
  } catch (error: any) {
    message.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  fetchOrders()
}

const handleReset = () => {
  orderNoFilter.value = ''
  statusFilter.value = undefined
  pagination.value.page = 1
  fetchOrders()
}

const viewOrder = (row: RechargeOrder) => {
  currentOrder.value = row
  detailDialogVisible.value = true
}

const completeOrder = (row: RechargeOrder) => {
  currentOrder.value = row
  completeForm.value = { tradeNo: '' }
  completeDialogVisible.value = true
}

const handleComplete = async () => {
  if (!currentOrder.value) return

  try {
    await ElMessageBox.confirm('确定要手动完成该订单吗?', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    completing.value = true
    await adminAPI.completeOrder(currentOrder.value.id, completeForm.value)
    message.success('订单已完成')
    completeDialogVisible.value = false
    await fetchOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || '操作失败')
    }
  } finally {
    completing.value = false
  }
}

const refundOrder = (row: RechargeOrder) => {
  currentOrder.value = row
  refundForm.value = { reason: '' }
  refundDialogVisible.value = true
}

const handleRefund = async () => {
  if (!currentOrder.value) return

  try {
    await ElMessageBox.confirm('确定要退款吗?退款后将扣除用户余额', '确认退款', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    refunding.value = true
    await adminAPI.refundOrder(currentOrder.value.id, refundForm.value)
    message.success('退款成功')
    refundDialogVisible.value = false
    await fetchOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || '退款失败')
    }
  } finally {
    refunding.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-page {
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

.order-no {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  color: var(--text-primary);
}

.amount-value {
  color: var(--success-color);
  font-weight: 600;
}

.refund-amount {
  color: var(--danger-color);
  font-weight: 600;
}

.pay-method-badge {
  display: inline-flex;
  padding: 4px 10px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
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

.status-muted {
  background: var(--bg-tertiary);
  color: var(--text-muted);
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

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
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

.form-text {
  font-weight: 500;
  color: var(--text-primary);
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

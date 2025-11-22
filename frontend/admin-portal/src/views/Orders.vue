<template>
  <div class="orders-page">
    <!-- 筛选条件 -->
    <el-card style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="订单号">
          <el-input
            v-model="orderNoFilter"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
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

    <!-- 订单列表 -->
    <el-card>
      <el-table :data="orders" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="200" />

        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color: var(--success-color); font-weight: 600">
              ¥{{ row.amount }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="payMethod" label="支付方式" width="120">
          <template #default="{ row }">
            {{ row.payMethod === 'alipay' ? '支付宝' : '微信支付' }}
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
        <el-table-column prop="paidAt" label="支付时间" width="180">
          <template #default="{ row }">
            {{ row.paidAt || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
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
        @current-change="fetchOrders"
        @size-change="fetchOrders"
      />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentOrder.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ currentOrder.payMethod === 'alipay' ? '支付宝' : '微信支付' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.paidAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 完成订单对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成订单" width="500px">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="金额">
          <span style="color: var(--success-color); font-weight: 600">
            ¥{{ currentOrder?.amount }}
          </span>
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
    <el-dialog v-model="refundDialogVisible" title="订单退款" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span style="color: var(--danger-color); font-weight: 600">
            ¥{{ currentOrder?.amount }}
          </span>
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

const getStatusType = (status: number) => {
  const typeMap: Record<number, any> = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '已取消'
  }
  return textMap[status] || '未知'
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
  padding: 0;
}

:deep(.el-pagination) {
  display: flex;
}
</style>

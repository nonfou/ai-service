<template>
  <div class="wallet-page">
    <div class="container">
      <!-- 顶部统计卡片 -->
      <div class="balance-card">
        <div class="balance-content">
          <!-- 钱包余额 -->
          <div class="balance-main">
            <div class="balance-label">钱包余额</div>
            <div class="balance-amount">{{ balance.toFixed(2) }} <span class="currency">USD</span></div>
            <div class="exchange-rate">1 USDT = 1 USD</div>
          </div>

          <!-- 统计数据 -->
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-label">累计充值</div>
              <div class="stat-value">{{ statistics.totalRecharge.toFixed(2) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">累计消费</div>
              <div class="stat-value">{{ statistics.totalSpent.toFixed(2) }}</div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="showRechargeDialog">
            充值
          </el-button>
        </div>
      </div>

      <!-- 交易记录 -->
      <div class="transactions-section">
        <h2 class="section-title">交易记录</h2>

        <el-table
          :data="transactions"
          v-loading="loading"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa' }"
        >
          <el-table-column prop="createdAt" label="时间" width="180" />

          <el-table-column label="类型" width="120">
            <template #default="scope">
              <el-tag
                :type="getTransactionTypeTag(scope.row.type)"
                size="small"
              >
                {{ getTransactionTypeName(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="description" label="描述" min-width="300" />

          <el-table-column label="金额" width="150" align="right">
            <template #default="scope">
              <span :class="getAmountClass(scope.row.amount)">
                {{ formatAmount(scope.row.amount) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="balance" label="余额" width="150" align="right">
            <template #default="scope">
              {{ scope.row.balance.toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 充值对话框 -->
    <el-dialog
      v-model="rechargeDialogVisible"
      title="充值"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="rechargeForm" label-width="100px">
        <el-form-item label="充值金额">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="1"
            :max="10000"
            :step="10"
            style="width: 100%"
          />
          <div class="quick-amounts">
            <el-button size="small" @click="rechargeForm.amount = 10">10</el-button>
            <el-button size="small" @click="rechargeForm.amount = 50">50</el-button>
            <el-button size="small" @click="rechargeForm.amount = 100">100</el-button>
            <el-button size="small" @click="rechargeForm.amount = 500">500</el-button>
          </div>
        </el-form-item>

        <el-form-item label="支付方式">
          <el-radio-group v-model="rechargeForm.payMethod">
            <el-radio label="alipay">支付宝</el-radio>
            <el-radio label="wechat">微信支付</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge" :loading="rechargeLoading">
          确认充值
        </el-button>
      </template>
    </el-dialog>

    <!-- 支付信息对话框 -->
    <el-dialog
      v-model="paymentDialogVisible"
      title="支付信息"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="payment-info">
        <div v-if="paymentInfo.paymentType === 'alipay'" class="alipay-payment">
          <p>请点击下方按钮跳转到支付宝完成支付:</p>
          <el-button type="primary" @click="openPaymentUrl">
            前往支付宝支付
          </el-button>
        </div>
        <div v-else-if="paymentInfo.paymentType === 'wechat'" class="wechat-payment">
          <p>请使用微信扫描下方二维码完成支付:</p>
          <div class="qrcode">
            <img :src="paymentInfo.qrCode" alt="微信支付二维码" />
          </div>
        </div>
        <el-alert
          title="支付完成后,余额将自动到账"
          type="success"
          :closable="false"
          style="margin-top: 20px"
        />
      </div>

      <template #footer>
        <el-button @click="paymentDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="checkPaymentStatus">
          检查支付状态
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { balanceAPI, rechargeAPI, type BalanceLog } from '../api'

// 余额信息
const balance = ref(0)
const statistics = ref({
  totalRecharge: 0,
  totalSpent: 0,
  monthlySpent: 0
})

// 交易记录
interface Transaction extends BalanceLog {
  createdAt: string
}

const transactions = ref<Transaction[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 充值对话框
const rechargeDialogVisible = ref(false)
const rechargeLoading = ref(false)
const rechargeForm = ref({
  amount: 100,
  payMethod: 'alipay'
})

// 支付信息对话框
const paymentDialogVisible = ref(false)
const paymentInfo = ref({
  orderId: 0,
  orderNo: '',
  amount: 0,
  paymentType: '',
  paymentUrl: '',
  qrCode: ''
})

// 加载余额信息
const loadBalance = async () => {
  try {
    const res = await balanceAPI.getBalance()
    if (res.data) {
      balance.value = res.data.balance
      statistics.value.totalRecharge = res.data.totalRecharge || 0
      statistics.value.totalSpent = res.data.totalSpent || 0
    }
  } catch (error: any) {
    console.error('获取余额失败:', error)
    ElMessage.error(error.message || '获取余额失败')
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const res = await balanceAPI.getBalanceStatistics()
    if (res.data) {
      statistics.value = {
        totalRecharge: res.data.totalRecharge || 0,
        totalSpent: res.data.totalSpent || 0,
        monthlySpent: res.data.monthlySpent || 0
      }
    }
  } catch (error: any) {
    console.error('获取统计信息失败:', error)
  }
}

// 加载交易记录
const loadTransactions = async () => {
  try {
    loading.value = true
    const res = await balanceAPI.getBalanceLogs(currentPage.value, pageSize.value)
    if (res.data) {
      transactions.value = (res.data.list || []).map((log: BalanceLog) => ({
        ...log,
        createdAt: formatDateTime(log.createdAt)
      }))
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('获取交易记录失败:', error)
    ElMessage.error(error.message || '获取交易记录失败')
  } finally {
    loading.value = false
  }
}

// 显示充值对话框
const showRechargeDialog = () => {
  rechargeDialogVisible.value = true
}

// 处理充值
const handleRecharge = async () => {
  if (rechargeForm.value.amount < 1) {
    ElMessage.warning('充值金额不能小于 1 USD')
    return
  }

  try {
    rechargeLoading.value = true
    const res = await rechargeAPI.createRecharge({
      amount: rechargeForm.value.amount,
      payMethod: rechargeForm.value.payMethod
    })

    if (res.data) {
      paymentInfo.value = {
        orderId: res.data.orderId,
        orderNo: res.data.orderNo,
        amount: res.data.amount,
        paymentType: res.data.paymentType,
        paymentUrl: res.data.paymentUrl || '',
        qrCode: res.data.qrCode || ''
      }

      rechargeDialogVisible.value = false
      paymentDialogVisible.value = true
    }
  } catch (error: any) {
    console.error('创建充值订单失败:', error)
    ElMessage.error(error.message || '创建充值订单失败')
  } finally {
    rechargeLoading.value = false
  }
}

// 打开支付链接
const openPaymentUrl = () => {
  if (paymentInfo.value.paymentUrl) {
    window.open(paymentInfo.value.paymentUrl, '_blank')
  }
}

// 检查支付状态
const checkPaymentStatus = async () => {
  try {
    const res = await rechargeAPI.queryOrder(paymentInfo.value.orderId)
    if (res.data && res.data.status === 1) {
      ElMessage.success('支付成功,余额已到账!')
      paymentDialogVisible.value = false

      // 刷新数据
      await loadBalance()
      await loadStatistics()
      await loadTransactions()
    } else {
      ElMessage.warning('订单尚未支付')
    }
  } catch (error: any) {
    console.error('查询支付状态失败:', error)
    ElMessage.error(error.message || '查询支付状态失败')
  }
}

// 分页处理
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadTransactions()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadTransactions()
}

// 获取交易类型标签
const getTransactionTypeTag = (type: number) => {
  const typeMap: Record<number, any> = {
    1: 'success',  // 充值
    2: 'warning',  // 消费
    3: 'danger',   // 退款
    0: 'info'      // 支付中
  }
  return typeMap[type] || 'info'
}

// 获取交易类型名称
const getTransactionTypeName = (type: number) => {
  const nameMap: Record<number, string> = {
    1: '充值',
    2: '购买',
    3: '退款',
    0: '支付中'
  }
  return nameMap[type] || '未知'
}

// 获取金额样式类
const getAmountClass = (amount: number) => {
  return amount > 0 ? 'amount-positive' : 'amount-negative'
}

// 格式化金额
const formatAmount = (amount: number) => {
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(2)}`
}

// 格式化日期时间
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

// 页面加载
onMounted(() => {
  loadBalance()
  loadStatistics()
  loadTransactions()
})
</script>

<style scoped>
.wallet-page {
  min-height: calc(100vh - 64px);
  padding: 60px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9d5ff 100%);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 顶部余额卡片 */
.balance-card {
  background: linear-gradient(135deg, #7c3aed 0%, #2563eb 100%);
  border-radius: 24px;
  padding: 48px;
  margin-bottom: 32px;
  box-shadow: 0 20px 60px rgba(124, 58, 237, 0.3);
  color: white;
}

.balance-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 48px;
}

.balance-main {
  flex: 1;
}

.balance-label {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 12px;
}

.balance-amount {
  font-size: 56px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8px;
}

.currency {
  font-size: 24px;
  font-weight: 500;
  opacity: 0.9;
}

.exchange-rate {
  font-size: 14px;
  opacity: 0.7;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
  flex: 1;
}

.stat-item {
  text-align: left;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 16px;
}

.action-buttons .el-button {
  padding: 16px 32px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 500;
}

.action-buttons .el-button--primary {
  background: white;
  color: #7c3aed;
  border: none;
}

.action-buttons .el-button--primary:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.action-buttons .el-button:not(.el-button--primary) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
}

.action-buttons .el-button:not(.el-button--primary):hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* 交易记录 */
.transactions-section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 24px 0;
}

.amount-positive {
  color: #10b981;
  font-weight: 600;
}

.amount-negative {
  color: #ef4444;
  font-weight: 600;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 充值对话框 */
.quick-amounts {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

/* 支付信息 */
.payment-info {
  text-align: center;
  padding: 20px 0;
}

.payment-info p {
  margin-bottom: 20px;
  font-size: 14px;
  color: #6b7280;
}

.qrcode {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.qrcode img {
  width: 200px;
  height: 200px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .balance-content {
    flex-direction: column;
    gap: 32px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .wallet-page {
    padding: 40px 0;
  }

  .balance-card {
    padding: 32px 24px;
  }

  .balance-amount {
    font-size: 42px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .stat-value {
    font-size: 20px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }

  .transactions-section {
    padding: 20px;
    overflow-x: auto;
  }

  .section-title {
    font-size: 20px;
  }
}
</style>

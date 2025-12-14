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
          <el-button type="primary" size="large" @click="showRechargeDialog" :disabled="!stripeEnabled">
            {{ stripeEnabled ? '充值' : '充值 (即将开放)' }}
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

          <el-table-column prop="remark" label="描述" min-width="300" />

          <el-table-column label="金额" width="150" align="right">
            <template #default="scope">
              <span :class="getAmountClass(scope.row.amount)">
                {{ formatAmount(scope.row.amount) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="余额" width="150" align="right">
            <template #default="scope">
              {{ scope.row.balanceAfter?.toFixed(2) || '-' }}
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
      @closed="resetRechargeDialog"
    >
      <!-- 步骤1: 选择金额 -->
      <div v-if="rechargeStep === 1">
        <div class="amount-section">
          <div class="section-label">选择充值金额 (USD)</div>

          <!-- 预设金额按钮 -->
          <div class="preset-amounts">
            <el-button
              v-for="amt in presetAmounts"
              :key="amt"
              :type="rechargeForm.amount === amt ? 'primary' : 'default'"
              @click="rechargeForm.amount = amt"
            >
              ${{ amt }}
            </el-button>
          </div>

          <!-- 自定义金额 -->
          <div class="custom-amount">
            <span class="custom-label">自定义金额:</span>
            <el-input-number
              v-model="rechargeForm.amount"
              :min="stripeConfig.minAmount"
              :max="stripeConfig.maxAmount"
              :step="1"
              :precision="0"
              style="width: 150px"
            />
            <span class="amount-hint">USD (${{ stripeConfig.minAmount }} - ${{ stripeConfig.maxAmount }})</span>
          </div>
        </div>
      </div>

      <!-- 步骤2: Stripe 支付 -->
      <div v-else-if="rechargeStep === 2">
        <div class="payment-section">
          <div class="payment-amount">
            支付金额: <strong>${{ rechargeForm.amount.toFixed(2) }}</strong>
          </div>

          <!-- Stripe Elements 容器 -->
          <div id="payment-element" class="stripe-element"></div>

          <!-- 错误信息 -->
          <div v-if="paymentError" class="payment-error">
            {{ paymentError }}
          </div>
        </div>
      </div>

      <!-- 步骤3: 支付结果 -->
      <div v-else-if="rechargeStep === 3">
        <div class="payment-result">
          <el-result
            :icon="paymentSuccess ? 'success' : 'error'"
            :title="paymentSuccess ? '支付成功' : '支付失败'"
            :sub-title="paymentSuccess ? `已成功充值 $${rechargeForm.amount.toFixed(2)}` : paymentError"
          >
            <template #extra>
              <el-button type="primary" @click="closeAndRefresh">
                {{ paymentSuccess ? '完成' : '关闭' }}
              </el-button>
            </template>
          </el-result>
        </div>
      </div>

      <!-- Dialog Footer -->
      <template #footer>
        <div v-if="rechargeStep === 1">
          <el-button @click="rechargeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="proceedToPayment" :loading="creatingOrder">
            继续支付 ${{ rechargeForm.amount }}
          </el-button>
        </div>
        <div v-else-if="rechargeStep === 2">
          <el-button @click="rechargeStep = 1" :disabled="processing">返回</el-button>
          <el-button type="primary" @click="confirmPayment" :loading="processing">
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { balanceAPI, type BalanceLog, getStripeConfigAPI, createRechargeOrderAPI, type StripeConfig } from '../api'
import { getStripe } from '../utils/stripe'
import type { Stripe, StripeElements } from '@stripe/stripe-js'

// Stripe 实例
let stripe: Stripe | null = null
let elements: StripeElements | null = null

// Stripe 配置
const stripeEnabled = ref(false)
const stripeConfig = ref<StripeConfig>({
  publishableKey: '',
  currency: 'usd',
  minAmount: 1,
  maxAmount: 1000,
  presetAmounts: [5, 10, 20, 50, 100]
})
const presetAmounts = ref([5, 10, 20, 50, 100])

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
const rechargeStep = ref(1) // 1: 选择金额, 2: 支付, 3: 结果
const rechargeForm = ref({
  amount: 10
})
const creatingOrder = ref(false)
const processing = ref(false)
const paymentError = ref('')
const paymentSuccess = ref(false)
const currentOrderId = ref<number | null>(null)
const clientSecret = ref('')

// 加载 Stripe 配置
const loadStripeConfig = async () => {
  try {
    const config = await getStripeConfigAPI()
    stripeConfig.value = config
    presetAmounts.value = config.presetAmounts || [5, 10, 20, 50, 100]
    stripeEnabled.value = true

    // 初始化 Stripe
    stripe = await getStripe()
  } catch (error: any) {
    console.log('Stripe 未配置:', error.message)
    stripeEnabled.value = false
  }
}

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
      transactions.value = (res.data.records || []).map((log: BalanceLog) => ({
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
  rechargeStep.value = 1
  rechargeForm.value.amount = 10
  paymentError.value = ''
  paymentSuccess.value = false
}

// 重置充值对话框
const resetRechargeDialog = () => {
  rechargeStep.value = 1
  rechargeForm.value.amount = 10
  paymentError.value = ''
  paymentSuccess.value = false
  creatingOrder.value = false
  processing.value = false
  currentOrderId.value = null
  clientSecret.value = ''
  elements = null
}

// 进入支付步骤
const proceedToPayment = async () => {
  if (!stripe) {
    ElMessage.error('支付服务初始化失败，请刷新页面重试')
    return
  }

  creatingOrder.value = true
  paymentError.value = ''

  try {
    // 创建订单
    const response = await createRechargeOrderAPI({ amount: rechargeForm.value.amount })
    currentOrderId.value = response.orderId
    clientSecret.value = response.clientSecret

    // 进入支付步骤
    rechargeStep.value = 2

    // 等待 DOM 更新后初始化 Stripe Elements
    await nextTick()
    await initStripeElements()

  } catch (error: any) {
    console.error('创建订单失败:', error)
    paymentError.value = error.message || '创建订单失败'
    ElMessage.error(paymentError.value)
  } finally {
    creatingOrder.value = false
  }
}

// 初始化 Stripe Elements
const initStripeElements = async () => {
  if (!stripe || !clientSecret.value) return

  const appearance = {
    theme: 'stripe' as const,
    variables: {
      colorPrimary: '#7c3aed',
      borderRadius: '8px'
    }
  }

  elements = stripe.elements({
    clientSecret: clientSecret.value,
    appearance,
    locale: 'zh' // 中文界面
  })

  const paymentElement = elements.create('payment', {
    layout: {
      type: 'tabs',
      defaultCollapsed: false
    },
    // 支付宝和微信支付需要的业务名称
    business: {
      name: 'AI Service'
    }
  })
  paymentElement.mount('#payment-element')
}

// 确认支付
const confirmPayment = async () => {
  if (!stripe || !elements) {
    paymentError.value = '支付服务未就绪'
    return
  }

  processing.value = true
  paymentError.value = ''

  try {
    const { error, paymentIntent } = await stripe.confirmPayment({
      elements,
      confirmParams: {
        // 支付宝等重定向支付需要 return_url，带上订单ID
        return_url: `${window.location.origin}/payment/success?order_id=${currentOrderId.value}`
      },
      redirect: 'if_required'
    })

    if (error) {
      paymentError.value = error.message || '支付失败'
      ElMessage.error(paymentError.value)
    } else if (paymentIntent && paymentIntent.status === 'succeeded') {
      // 支付成功（卡支付等不需要重定向的情况）
      paymentSuccess.value = true
      rechargeStep.value = 3
      ElMessage.success('支付成功！')
    } else if (paymentIntent && paymentIntent.status === 'processing') {
      // 支付处理中（某些支付方式需要更长时间确认）
      paymentError.value = '支付处理中，请稍候查看订单状态...'
    } else {
      // 可能需要额外验证或重定向
      paymentError.value = '请按照页面提示完成支付...'
    }
  } catch (error: any) {
    console.error('支付失败:', error)
    paymentError.value = error.message || '支付失败'
    ElMessage.error(paymentError.value)
  } finally {
    processing.value = false
  }
}

// 关闭并刷新
const closeAndRefresh = () => {
  rechargeDialogVisible.value = false
  if (paymentSuccess.value) {
    loadBalance()
    loadStatistics()
    loadTransactions()
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
const getTransactionTypeTag = (type: string) => {
  const typeMap: Record<string, any> = {
    'recharge': 'success',      // 充值
    'refund': 'warning',        // 退款
    'subscription': 'primary',  // 订阅购买
    'gift': 'success',          // 赠送
    'adjustment': 'info',       // 人工调整
  }
  return typeMap[type] || 'info'
}

// 获取交易类型名称
const getTransactionTypeName = (type: string) => {
  const nameMap: Record<string, string> = {
    'recharge': '充值',
    'refund': '退款',
    'subscription': '订阅购买',
    'gift': '赠送',
    'adjustment': '余额调整',
  }
  return nameMap[type] || type
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
  loadStripeConfig()
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

.action-buttons .el-button--primary:hover:not(:disabled) {
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

/* 充值对话框 - 金额选择 */
.amount-section {
  padding: 16px 0;
}

.section-label {
  font-size: 16px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 16px;
}

.preset-amounts {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.preset-amounts .el-button {
  min-width: 80px;
}

.custom-amount {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.custom-label {
  font-size: 14px;
  color: #6b7280;
}

.amount-hint {
  font-size: 12px;
  color: #9ca3af;
}

/* 支付区域 */
.payment-section {
  padding: 16px 0;
}

.payment-amount {
  font-size: 18px;
  color: #374151;
  margin-bottom: 24px;
  padding: 16px;
  background: #f0fdf4;
  border-radius: 8px;
  text-align: center;
}

.payment-amount strong {
  color: #10b981;
  font-size: 24px;
}

.stripe-element {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  min-height: 200px;
}

.payment-error {
  margin-top: 16px;
  padding: 12px;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 8px;
  font-size: 14px;
}

/* 支付结果 */
.payment-result {
  padding: 32px 0;
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

  .preset-amounts {
    justify-content: center;
  }

  .custom-amount {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }
}
</style>

<template>
  <div class="subscriptions-page">
    <!-- 页面标题 -->
    <div class="page-header-section">
      <span class="hero-badge">订阅方案</span>
      <h1 class="page-title">灵活的订阅方案</h1>
      <p class="page-description">按需选择适合你的套餐,专注于构建应用,我们负责 AI 能力。</p>
    </div>

    <!-- 订阅套餐卡片 -->
    <div class="section-heading">
      <h2>订阅套餐</h2>
      <p>根据开发需求选择合适的套餐,随时可升级扩容</p>
    </div>

    <!-- 按月支付 - 卡片网格 -->
    <div
      class="plans-grid"
      v-loading="plansLoading"
      element-loading-text="加载套餐中..."
    >
      <div
        v-for="plan in monthlyPlans"
        :key="plan.id"
        class="plan-card"
        :class="{ 'plan-card-featured': plan.featured }"
      >
        <!-- 标签 -->
        <span v-if="plan.badge" class="plan-badge" :class="`badge-${getBadgeColor(plan.badge)}`">
          {{ plan.badge }}
        </span>

        <!-- 套餐标题 -->
        <div class="plan-header">
          <h3 class="plan-name">{{ plan.displayName }}</h3>
          <p class="plan-subtitle">{{ plan.description }}</p>
        </div>

        <!-- 价格 -->
        <div class="plan-price">
          <span class="price-symbol">&yen;</span>
          <span class="price-value">{{ formatCurrency(plan.price) }}</span>
          <span class="price-period">{{ getPricePeriod(plan) }}</span>
        </div>

        <!-- 配额信息 -->
        <div class="plan-quota" v-if="plan.quotaAmount > 0">
          {{ getQuotaText(plan.paymentType, plan.quotaAmount) }}
        </div>

        <!-- 功能列表 -->
        <ul class="plan-features">
          <li v-for="(feature, idx) in plan.features" :key="idx">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>{{ feature }}</span>
          </li>
        </ul>

        <!-- 订阅按钮 -->
        <button
          :class="['subscribe-btn', { 'subscribe-btn-gradient': plan.featured }]"
          :loading="subscribingPlanId === plan.id"
          :disabled="subscribingPlanId === plan.id"
          @click="handleSubscribe(plan)"
        >
          {{ getButtonText(plan.paymentType) }}
        </button>
      </div>
      <div
        v-if="!plansLoading && monthlyPlans.length === 0"
        class="plans-empty-tip"
      >
        暂无可用套餐,请稍后再试
      </div>
    </div>


    <!-- 订阅历史 -->
    <div class="section-heading">
      <h2>订阅历史</h2>
      <p>查看您的订阅记录和使用情况</p>
    </div>

    <div class="history-table-card">
      <el-table
        :data="subscriptionHistory"
        style="width: 100%"
        v-loading="historyLoading"
        element-loading-text="加载订阅记录..."
      >
        <el-table-column prop="planName" label="套餐" min-width="140" />
        <el-table-column label="开启日期" width="140">
          <template #default="{ row }">
            {{ formatDate(row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column label="结束日期" width="140">
          <template #default="{ row }">
            {{ formatDate(row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'active'"
              text
              type="danger"
              size="small"
              :loading="cancellingId === row.id"
              @click="handleCancelSubscription(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="table-empty">暂无订阅记录</div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { subscriptionAPI, PaymentTypeEnum, type PaymentType, type SubscriptionPlan, type Subscription } from '../api'
import { useUserStore } from '../stores/user'

type DisplayPlan = SubscriptionPlan & {
  paymentType: PaymentType
  features: string[]
  badge?: string
  featured?: boolean
}

const router = useRouter()
const userStore = useUserStore()
const plansLoading = ref(false)
const historyLoading = ref(false)
const subscribingPlanId = ref<number | null>(null)
const cancellingId = ref<number | null>(null)

const plans = ref<DisplayPlan[]>([])
const subscriptionHistory = ref<Subscription[]>([])

const monthlyPlans = computed(() =>
  plans.value.filter(plan => plan.paymentType === PaymentTypeEnum.MONTHLY)
)

const CURRENCY_SYMBOL = '\u00A5'

const formatCurrency = (value?: number) => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '0.00'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const DAILY_PLAN_CODES = ['trial_card', 'trial', 'daily_card']

const isDailyPlan = (plan: DisplayPlan) => {
  const code = (plan.planName || '').toLowerCase()
  if (DAILY_PLAN_CODES.some(item => code.includes(item))) {
    return true
  }
  return plan.displayName?.includes('体验卡') ?? false
}

const getPricePeriod = (plan: DisplayPlan) => {
  if (plan.paymentType === PaymentTypeEnum.MONTHLY) {
    return isDailyPlan(plan) ? '/天' : '/月'
  }
  return ''
}

const getQuotaText = (paymentType: PaymentType, quotaAmount: number) => {
  const formatted = formatCurrency(quotaAmount)
  return paymentType === PaymentTypeEnum.MONTHLY
    ? `含 ${formatted} 元额度`
    : `${formatted} 元`
}

const getButtonText = (paymentType: PaymentType) => {
  return paymentType === PaymentTypeEnum.MONTHLY ? '立即订阅' : '立即充值'
}

const getBadgeColor = (badge: string) => {
  const colorMap: Record<string, string> = {
    '新用户专享': 'gray',
    '最受欢迎': 'green',
    '推荐套餐': 'green',
    '企业推荐': 'orange',
    '高级套餐': 'orange',
    '旗舰': 'purple',
    '企业': 'purple'
  }
  return colorMap[badge] || 'gray'
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    active: 'success',
    expired: 'info',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    active: '已激活',
    expired: '已过期',
    cancelled: '已取消'
  }
  return map[status] || status
}

const formatDate = (value?: string) => {
  if (!value) {
    return '--'
  }
  return value
}

const isPayAsGoPlan = (planName?: string) => {
  if (!planName) return false
  const normalized = planName.toLowerCase()
  return normalized.includes('pay_as_go') || normalized.includes('pay-as-go') || normalized.includes('paygo')
}

const transformPlan = (plan: SubscriptionPlan): DisplayPlan => {
  const paymentType = isPayAsGoPlan(plan.planName) ? PaymentTypeEnum.PAY_AS_GO : PaymentTypeEnum.MONTHLY
  const badge = plan.badgeText || ''
  const featured = Boolean(badge && ['荐', '牌', '企'].some(flag => badge.includes(flag)))

  return {
    ...plan,
    paymentType,
    badge,
    featured,
    features: Array.isArray(plan.features) ? plan.features : []
  }
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'string') {
    return error
  }
  if (error && typeof error === 'object' && 'message' in error && typeof (error as any).message === 'string') {
    return (error as any).message || fallback
  }
  if ((error as any)?.response?.data?.message) {
    return (error as any).response.data.message
  }
  return fallback
}

const loadPlans = async () => {
  plansLoading.value = true
  try {
    const res = await subscriptionAPI.getPlans()
    if (res.code !== 200) {
      throw new Error(res.message || '加载套餐列表失败')
    }
    plans.value = (res.data || []).map(transformPlan)
  } catch (error) {
    console.error('加载套餐列表失败', error)
    ElMessage.error(resolveErrorMessage(error, '加载套餐列表失败'))
  } finally {
    plansLoading.value = false
  }
}

const loadSubscriptionHistory = async () => {
  historyLoading.value = true
  try {
    const res = await subscriptionAPI.getHistory(1, 20)
    if (res.code !== 200) {
      throw new Error(res.message || '加载订阅记录失败')
    }
    subscriptionHistory.value = res.data?.records || []
  } catch (error) {
    console.error('加载订阅记录失败', error)
    ElMessage.error(resolveErrorMessage(error, '加载订阅记录失败'))
  } finally {
    historyLoading.value = false
  }
}

const ensureLoggedIn = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

const handleSubscribe = async (plan: DisplayPlan) => {
  if (!ensureLoggedIn()) {
    return
  }

  const actionText = plan.paymentType === PaymentTypeEnum.MONTHLY ? '订阅' : '充值'
  const priceText = `${CURRENCY_SYMBOL}${formatCurrency(plan.price)}`

  try {
    await ElMessageBox.confirm(
      `确定${actionText} ${plan.displayName} 吗? 价格: ${priceText}`,
      `确认${actionText}`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
  } catch {
    return
  }

  subscribingPlanId.value = plan.id
  try {
    const res = await subscriptionAPI.subscribe({ planId: plan.id })
    if (res.code !== 200) {
      throw new Error(res.message || `${actionText}失败`)
    }
    ElMessage.success(`${actionText}成功`)
    await loadSubscriptionHistory()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, `${actionText}失败`))
  } finally {
    subscribingPlanId.value = null
  }
}

const handleCancelSubscription = async (subscription: Subscription) => {
  if (!ensureLoggedIn()) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要取消此订阅吗?',
      '取消订阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  cancellingId.value = subscription.id
  try {
    const res = await subscriptionAPI.cancel(subscription.id)
    if (res.code !== 200) {
      throw new Error(res.message || '取消失败')
    }
    ElMessage.success('已取消订阅')
    await loadSubscriptionHistory()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '取消失败'))
  } finally {
    cancellingId.value = null
  }
}

onMounted(() => {
  loadPlans()
  loadSubscriptionHistory()
})
</script>

<style scoped>
.subscriptions-page {
  background: linear-gradient(180deg, #f8f8ff 0%, #ffffff 100%);
  color: #0f172a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  min-height: 100vh;
  padding: 0;
}

/* 页面头部 */
.page-header-section {
  text-align: center;
  padding: 4rem 0 3rem;
  background: linear-gradient(135deg, #f7f9ff 0%, #f1f0ff 100%);
  position: relative;
  overflow: hidden;
}

.page-header-section::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.65;
  pointer-events: none;
  background: radial-gradient(600px at 50% -20%, rgba(99, 102, 241, 0.3), transparent 60%);
}

.hero-badge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  font-size: 0.875rem;
  font-weight: 600;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  border: 1px solid rgba(99, 102, 241, 0.25);
  margin-bottom: 1.5rem;
}

.page-title {
  position: relative;
  z-index: 1;
  font-size: 3rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 1rem 0;
  letter-spacing: -0.04em;
}

.page-description {
  position: relative;
  z-index: 1;
  font-size: 1.05rem;
  color: #475569;
  margin: 0 0 2.5rem 0;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.75;
}

/* Section Heading */
.section-heading {
  max-width: 1200px;
  margin: 0 auto 3rem;
  padding: 0 1.75rem;
  text-align: center;
}

.section-heading h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.section-heading p {
  font-size: 1.05rem;
  color: #64748b;
  max-width: 640px;
  margin: 0 auto;
}

/* 套餐网格 */
.plans-grid {
  max-width: 1200px;
  margin: 0 auto 6rem;
  padding: 0 1.75rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  justify-items: center;
}

.plan-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 2rem 1.75rem;
  border-radius: 1.25rem;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 12px 35px rgba(15, 23, 42, 0.06);
  transition: all 0.3s ease;
  width: 100%;
  max-width: 420px;
}

.plan-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.1);
  border-color: rgba(124, 58, 237, 0.3);
}

.plan-card-featured {
  border: 2px solid rgba(124, 58, 237, 0.5);
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.04), rgba(37, 99, 235, 0.04));
  box-shadow: 0 20px 60px rgba(76, 29, 149, 0.12);
}

.plan-card-featured:hover {
  box-shadow: 0 28px 70px rgba(76, 29, 149, 0.18);
}

.plans-empty-tip {
  grid-column: 1 / -1;
  text-align: center;
  color: #94a3b8;
  padding: 2rem 0;
  font-size: 0.95rem;
}

.plan-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 0.375rem 1rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
  color: #ffffff;
  letter-spacing: 0.02em;
}

.badge-gray {
  background: linear-gradient(135deg, #64748b, #475569);
  box-shadow: 0 6px 16px rgba(71, 85, 105, 0.3);
}

.badge-green {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 6px 16px rgba(34, 197, 94, 0.35);
}

.badge-orange {
  background: linear-gradient(135deg, #fb923c, #f97316);
  box-shadow: 0 6px 16px rgba(249, 115, 22, 0.35);
}

.badge-purple {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  box-shadow: 0 6px 16px rgba(124, 58, 237, 0.35);
}

.plan-header {
  text-align: center;
  margin-bottom: 1.25rem;
}

.plan-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.5rem 0;
  letter-spacing: -0.02em;
}

.plan-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.plan-price {
  text-align: center;
  margin-bottom: 0.75rem;
}

.price-symbol {
  font-size: 2.75rem;
  font-weight: 800;
  color: #475569;
  vertical-align: baseline;
  margin-right: 0.25rem;
}

.price-value {
  font-size: 2.75rem;
  font-weight: 800;
  color: #1f2937;
  letter-spacing: -0.03em;
}

.custom-amount {
  font-size: 1.75rem;
  font-weight: 700;
  color: #7c3aed;
}

.price-period {
  font-size: 0.95rem;
  color: #64748b;
  margin-left: 0.25rem;
  font-weight: 500;
}

.plan-quota {
  text-align: center;
  font-size: 0.875rem;
  font-weight: 600;
  color: #7c3aed;
  margin-bottom: 1.75rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 1.75rem 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.plan-features li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
  color: #475569;
  line-height: 1.5;
}

.feature-icon {
  color: #22c55e;
  flex-shrink: 0;
  font-size: 1.125rem;
}

.subscribe-btn {
  width: 100%;
  padding: 0.875rem 1.5rem;
  border-radius: 0.75rem;
  border: 1.5px solid rgba(148, 163, 184, 0.4);
  font-weight: 600;
  font-size: 0.9375rem;
  color: #1f2937;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.25s ease;
  letter-spacing: 0.01em;
}

.subscribe-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
  border-color: rgba(124, 58, 237, 0.4);
  background: #fafbfc;
}

.subscribe-btn-gradient {
  border: none;
  background: linear-gradient(135deg, #7c3aed 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 16px 40px rgba(79, 70, 229, 0.3);
}

.subscribe-btn-gradient:hover {
  box-shadow: 0 20px 50px rgba(79, 70, 229, 0.4);
  transform: translateY(-2px) scale(1.01);
}

/* 历史表格 */
.history-table-card {
  max-width: 1200px;
  margin: 0 auto 4rem;
  padding: 0 1.75rem;
}

/* 历史表格 */
.history-table-card {
  max-width: 1200px;
  margin: 0 auto 4rem;
  padding: 0 1.75rem;
}

.history-table-card :deep(.el-table) {
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.7);
}

.history-table-card :deep(.el-table th) {
  background: #f8fafc;
  color: #1f2937;
  font-weight: 600;
}

.table-empty {
  padding: 2rem 0;
  color: #94a3b8;
  font-size: 0.95rem;
}

/* 响应式 */
@media (max-width: 1024px) {
  .plans-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1.5rem;
  }
}

@media (max-width: 768px) {
  .page-header-section {
    padding: 3rem 1.25rem 2.5rem;
  }

  .page-title {
    font-size: 2.25rem;
  }

  .page-description {
    font-size: 0.95rem;
  }

  .plans-grid,
  .history-table-card {
    padding: 0 1.25rem;
  }

  .plans-grid {
    grid-template-columns: 1fr;
    gap: 1.75rem;
  }

  .plan-card {
    padding: 1.75rem 1.5rem;
  }

  .section-heading {
    padding: 0 1.25rem;
  }

  .section-heading h2 {
    font-size: 2rem;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 1.875rem;
  }

  .section-heading h2 {
    font-size: 1.75rem;
  }

  .plan-name {
    font-size: 1.35rem;
  }

  .price-value {
    font-size: 2.5rem;
  }
}
</style>

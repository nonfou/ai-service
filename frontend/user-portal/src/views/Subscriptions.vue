<template>
  <div class="subscriptions-page">
    <!-- 页面标题 -->
    <div class="page-header-section">
      <span class="hero-badge">订阅方案</span>
      <h1 class="page-title">灵活的订阅方案</h1>
      <p class="page-description">按需选择适合你的套餐,专注于构建应用,我们负责 AI 能力。</p>
      <div class="tab-switches">
        <button
          :class="['tab-btn', { active: activeTab === 'monthly' }]"
          @click="switchPaymentType(1)"
        >
          按月支付
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'payAsGo' }]"
          @click="switchPaymentType(2)"
        >
          按量支付
        </button>
      </div>
    </div>

    <!-- 订阅套餐卡片 -->
    <div class="section-heading">
      <h2>订阅套餐</h2>
      <p>根据开发需求选择合适的套餐,随时可升级扩容</p>
    </div>

    <!-- 按月支付 - 卡片网格 -->
    <div v-if="activeTab === 'monthly'" class="plans-grid">
      <div
        v-for="plan in displayPlans"
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
          <span class="price-symbol">$</span>
          <span class="price-value">{{ plan.price }}</span>
          <span class="price-period">{{ getPricePeriod(plan.paymentType) }}</span>
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
          @click="handleSubscribe(plan)"
        >
          {{ getButtonText(plan.paymentType) }}
        </button>
      </div>
    </div>

    <!-- 按量支付 - 新的 UI 布局 -->
    <div v-else class="pay-as-go-container">
      <div class="pay-as-go-card">
        <div class="pay-as-go-header">
          <div class="header-icon">
            <el-icon :size="48"><Coin /></el-icon>
          </div>
          <h3 class="pay-as-go-title">按量付费</h3>
          <p class="pay-as-go-desc">灵活充值,按实际使用量计费,永久有效</p>
        </div>

        <div class="pricing-rules">
          <h4 class="rules-title">计费规则</h4>
          <div class="rules-grid">
            <div class="rule-item">
              <div class="rule-label">输入 Token</div>
              <div class="rule-price">$2 <span class="rule-unit">/ 1M tokens</span></div>
            </div>
            <div class="rule-item">
              <div class="rule-label">输出 Token</div>
              <div class="rule-price">$10 <span class="rule-unit">/ 1M tokens</span></div>
            </div>
          </div>
        </div>

        <div class="pay-as-go-features">
          <div class="feature-item" v-for="(feature, idx) in payAsGoPlans[0].features.slice(2)" :key="idx">
            <el-icon class="feature-check"><Check /></el-icon>
            <span>{{ feature }}</span>
          </div>
        </div>

        <button class="recharge-btn" @click="handleRecharge">
          立即充值
        </button>
      </div>
    </div>

    <!-- 订阅历史 -->
    <div class="section-heading">
      <h2>订阅历史</h2>
      <p>查看您的订阅记录和使用情况</p>
    </div>

    <div class="history-table-card">
      <el-table :data="subscriptionHistory" style="width: 100%">
        <el-table-column prop="planName" label="套餐" min-width="120" />
        <el-table-column prop="startDate" label="开启日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
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
              @click="handleCancelSubscription(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Coin } from '@element-plus/icons-vue'
import { subscriptionAPI, PaymentType, type SubscriptionPlan } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref<'monthly' | 'payAsGo'>('monthly')
const rechargeAmount = ref<number>()

// Mock 数据 - 按月支付套餐
const monthlyPlans = ref<SubscriptionPlan[]>([
  {
    id: 1,
    planName: 'trial',
    displayName: '体验卡',
    description: '快速体验 AI 开发平台',
    originalPrice: 1,
    price: 1,
    quotaAmount: 5,  // $5 额度/天
    paymentType: PaymentType.MONTHLY,
    badge: '新用户专享',
    featured: false,
    features: [
      '1天有效期',
      'Claude & Gemini 模型',
      '24/7 技术支持'
    ],
    status: 1,
    sortOrder: 1,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z'
  },
  {
    id: 2,
    planName: 'max_100',
    displayName: 'Max 100',
    description: '适合专业开发者',
    originalPrice: 60,
    price: 50,
    quotaAmount: 50,  // $50 额度/天
    paymentType: PaymentType.MONTHLY,
    badge: '最受欢迎',
    featured: true,
    features: [
      '30天有效期',
      '全模型访问权限',
      'IDE 与 CLI 集成',
      '优先技术支持'
    ],
    status: 1,
    sortOrder: 2,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z'
  },
  {
    id: 3,
    planName: 'max_200',
    displayName: 'Max 200',
    description: '适合团队协作',
    originalPrice: 120,
    price: 100,
    quotaAmount: 100,  // $100 额度/天
    paymentType: PaymentType.MONTHLY,
    badge: '企业推荐',
    featured: false,
    features: [
      '30天有效期',
      '全模型访问权限',
      '多账号管理',
      '专属技术支持'
    ],
    status: 1,
    sortOrder: 3,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z'
  }
])

// Mock 数据 - 按量支付套餐
const payAsGoPlans = ref<SubscriptionPlan[]>([
  {
    id: 11,
    planName: 'pay_as_go',
    displayName: '按量付费',
    description: '灵活充值,按实际使用量计费',
    originalPrice: 0,
    price: 0,  // 自定义金额
    quotaAmount: 0,
    paymentType: PaymentType.PAY_AS_GO,
    featured: true,
    features: [
      '输入: $2 / 1M tokens',
      '输出: $10 / 1M tokens',
      '按实际用量扣费',
      '支持所有模型',
      '实时消费明细'
    ],
    status: 1,
    sortOrder: 1,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z'
  }
])

// 根据当前 Tab 显示对应的套餐列表
const displayPlans = computed(() => {
  return activeTab.value === 'monthly' ? monthlyPlans.value : payAsGoPlans.value
})

// 订阅历史
const subscriptionHistory = ref([
  { id: 1, planName: 'Max 100', startDate: '2025-10-09', endDate: '2025-10-13', status: 'expired' },
  { id: 2, planName: '体验卡', startDate: '2025-09-30', endDate: '2025-10-01', status: 'expired' },
  { id: 3, planName: '体验卡', startDate: '2025-09-25', endDate: '2025-09-26', status: 'expired' },
  { id: 4, planName: 'Max 100', startDate: '2025-09-04', endDate: '2025-09-25', status: 'expired' }
])

// 切换支付类型
const switchPaymentType = (type: number) => {
  activeTab.value = type === 1 ? 'monthly' : 'payAsGo'
}

// 获取价格周期文本
const getPricePeriod = (paymentType: PaymentType) => {
  return paymentType === PaymentType.MONTHLY ? '/月' : ''
}

// 获取配额文本
const getQuotaText = (paymentType: PaymentType, quotaAmount: number) => {
  if (paymentType === PaymentType.MONTHLY) {
    return `每日 ${quotaAmount.toLocaleString()} 美元`
  } else {
    return `${quotaAmount.toLocaleString()} 美元`
  }
}

// 获取按钮文本
const getButtonText = (paymentType: PaymentType) => {
  return paymentType === PaymentType.MONTHLY ? '立即订阅' : '立即充值'
}

// 获取标签颜色
const getBadgeColor = (badge: string) => {
  const colorMap: Record<string, string> = {
    '新用户专享': 'gray',
    '最受欢迎': 'green',
    '企业推荐': 'orange',
    '推荐': 'green',
    '超值': 'orange',
    '企业': 'purple'
  }
  return colorMap[badge] || 'gray'
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
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

const handleSubscribe = async (plan: SubscriptionPlan) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  const actionText = plan.paymentType === PaymentType.MONTHLY ? '订阅' : '充值'
  const priceText = plan.paymentType === PaymentType.MONTHLY ? `$${plan.price}/月` : `$${plan.price}`

  ElMessageBox.confirm(
    `确定${actionText} ${plan.displayName} 吗? 价格: ${priceText}`,
    `确认${actionText}`,
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      // await subscriptionAPI.subscribe({ planId: plan.id })
      ElMessage.success(`${actionText}成功!`)
      loadSubscriptionHistory()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || `${actionText}失败`)
    }
  }).catch(() => {})
}

const handleRecharge = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 直接跳转到充值页面或打开充值弹窗
  ElMessage.info('跳转到充值页面...')
  // 这里可以调用充值API或跳转到支付页面
  // router.push('/recharge')
}

const handleCancelSubscription = async (subscription: any) => {
  ElMessageBox.confirm(
    '确定要取消此订阅吗?',
    '取消订阅',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // await subscriptionAPI.cancel(subscription.id)
      ElMessage.success('已取消订阅')
      loadSubscriptionHistory()
    } catch (error: any) {
      ElMessage.error('取消失败')
    }
  }).catch(() => {})
}

const loadSubscriptionHistory = async () => {
  try {
    // const res = await subscriptionAPI.getHistory()
    // subscriptionHistory.value = res.data.list
  } catch (error) {
    console.error('Failed to load subscription history')
  }
}

onMounted(() => {
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

.tab-switches {
  position: relative;
  z-index: 1;
  display: inline-flex;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.75);
  padding: 0.375rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  backdrop-filter: blur(6px);
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 2rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.95rem;
  border: none;
  background: transparent;
  color: #475569;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-btn.active {
  background: linear-gradient(135deg, #7c3aed 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.25);
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

/* 按量支付 - 新布局样式 */
.pay-as-go-container {
  max-width: 700px;
  margin: 0 auto 6rem;
  padding: 0 1.75rem;
}

.pay-as-go-card {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05), rgba(37, 99, 235, 0.05));
  border: 2px solid rgba(124, 58, 237, 0.2);
  border-radius: 1.5rem;
  padding: 3rem 2.5rem;
  box-shadow: 0 20px 60px rgba(76, 29, 149, 0.15);
}

.pay-as-go-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  border-radius: 50%;
  margin-bottom: 1.5rem;
  color: white;
  box-shadow: 0 12px 30px rgba(124, 58, 237, 0.3);
}

.pay-as-go-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.75rem 0;
}

.pay-as-go-desc {
  font-size: 1rem;
  color: #64748b;
  margin: 0;
}

.pricing-rules {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.rules-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1.5rem 0;
  text-align: center;
}

.rules-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.rule-item {
  text-align: center;
  padding: 1.25rem;
  background: linear-gradient(135deg, #f8f9ff, #f1f0ff);
  border-radius: 0.75rem;
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.rule-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.rule-price {
  font-size: 1.5rem;
  font-weight: 800;
  color: #7c3aed;
}

.rule-unit {
  font-size: 0.875rem;
  font-weight: 500;
  color: #64748b;
}

.pay-as-go-features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2.5rem;
}

.feature-item {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #475569;
  background: white;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.15);
  border-color: rgba(124, 58, 237, 0.3);
}

.feature-check {
  color: #22c55e;
  font-size: 1rem;
  flex-shrink: 0;
}

.recharge-btn {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  display: block;
  padding: 1rem 2rem;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.3);
}

.recharge-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 36px rgba(79, 70, 229, 0.4);
}

.recharge-btn:active {
  transform: translateY(0);
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
  .history-table-card,
  .pay-as-go-container {
    padding: 0 1.25rem;
  }

  .plans-grid {
    grid-template-columns: 1fr;
    gap: 1.75rem;
  }

  .plan-card {
    padding: 1.75rem 1.5rem;
  }

  .tab-switches {
    flex-direction: column;
    width: 100%;
    max-width: 300px;
  }

  .tab-btn {
    width: 100%;
    justify-content: center;
  }

  .section-heading {
    padding: 0 1.25rem;
  }

  .section-heading h2 {
    font-size: 2rem;
  }

  /* 按量付费响应式 */
  .pay-as-go-card {
    padding: 2rem 1.5rem;
  }

  .rules-grid {
    grid-template-columns: 1fr;
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

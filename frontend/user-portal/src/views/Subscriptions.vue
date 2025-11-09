<template>
  <div class="subscriptions-page">
    <!-- 页面标题 -->
    <div class="page-header-section">
      <h1 class="page-title">订阅套餐</h1>
      <p class="page-description">优选订阅一次性套餐,包含套餐点数和按次计费。</p>
      <div class="tab-switches">
        <el-button
          :type="activeTab === 'monthly' ? 'primary' : ''"
          @click="activeTab = 'monthly'"
        >
          月付
        </el-button>
        <el-button
          :type="activeTab === 'yearly' ? 'success' : ''"
          @click="activeTab = 'yearly'"
        >
          年付 <el-tag size="small" type="success">省20%</el-tag>
        </el-button>
      </div>
    </div>

    <!-- 新的免费试用卡片 -->
    <div class="trial-banner v2board-card">
      <el-icon class="banner-icon"><Gift /></el-icon>
      <div class="banner-content">
        <h3>新用户福利</h3>
        <p>首次订阅享受额外优惠,立即开始体验!</p>
      </div>
    </div>

    <!-- 订阅套餐卡片 -->
    <div class="section-title-row">
      <h2 class="section-title">订阅套餐</h2>
    </div>

    <div class="plans-grid">
      <div
        v-for="plan in subscriptionPlans"
        :key="plan.id"
        class="plan-card v2board-card"
        :class="{ featured: plan.featured }"
      >
        <!-- 标签 -->
        <div v-if="plan.badge" class="plan-badge" :class="`badge-${plan.badge.type}`">
          {{ plan.badge.text }}
        </div>

        <!-- 套餐标题 -->
        <div class="plan-header">
          <h3 class="plan-name">{{ plan.name }}</h3>
          <p class="plan-subtitle">{{ plan.subtitle }}</p>
        </div>

        <!-- 价格 -->
        <div class="plan-price">
          <span class="price-symbol">$</span>
          <span class="price-value">{{ plan.price }}</span>
          <span class="price-period">/月</span>
        </div>

        <!-- 功能列表 */
        <ul class="plan-features">
          <li v-for="(feature, idx) in plan.features" :key="idx">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>{{ feature }}</span>
          </li>
        </ul>

        <!-- 订阅按钮 -->
        <el-button
          :type="plan.featured ? 'primary' : 'default'"
          size="large"
          class="subscribe-btn"
          @click="handleSubscribe(plan)"
        >
          {{ plan.buttonText || '立即订阅' }}
        </el-button>
      </div>
    </div>

    <!-- 积分包 -->
    <div class="section-title-row">
      <h2 class="section-title">积分包</h2>
    </div>
    <p class="section-desc">积分次日自动，赠送积分按月扣费，终及达订阅时计费。</p>

    <div class="credits-grid">
      <div
        v-for="credit in creditPackages"
        :key="credit.id"
        class="credit-card v2board-card"
      >
        <div v-if="credit.hot" class="hot-badge">热销</div>

        <div class="credit-header">
          <h3 class="credit-amount">{{ credit.amount }}积分加油包</h3>
        </div>

        <div class="credit-price">
          <span class="price-symbol">$</span>
          <span class="price-value">{{ credit.price }}</span>
        </div>

        <div class="credit-details">
          <p>获得 {{ credit.amount }} 积分</p>
          <p class="credit-bonus">{{ credit.bonus }}</p>
        </div>

        <ul class="credit-features">
          <li><el-icon><Check /></el-icon>即时到账</li>
          <li><el-icon><Check /></el-icon>终及达订阅时计费</li>
        </ul>

        <el-button size="large" class="purchase-btn" @click="handlePurchaseCredit(credit)">
          立即购买
        </el-button>
      </div>
    </div>

    <!-- 订阅历史 -->
    <div class="section-title-row">
      <h2 class="section-title">订阅历史</h2>
    </div>

    <div class="history-table-card v2board-card">
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
import { Check, Gift } from '@element-plus/icons-vue'
import { subscriptionAPI } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('monthly')

// 订阅套餐数据
const subscriptionPlans = ref([
  {
    id: 1,
    name: '体验卡',
    subtitle: '轻分不听,可以遗缺试',
    price: 1,
    badge: { text: '新用户专享', type: 'gray' },
    features: [
      '1天有效期',
      '一次性购买600积分',
      'Gemini/Code = API 通0元/次',
      'Claude Code = API $1.5/元',
      '仅白日程运适用指令CLI + B',
      '交持Claude 4 Opus & Sonnet',
      '24/7 技术支持'
    ],
    buttonText: '立即试用'
  },
  {
    id: 2,
    name: 'Pro 20',
    subtitle: '适合轻度使用',
    price: 10,
    badge: { text: '适合初次订阅200次/月超过', type: 'green' },
    features: [
      '30天有效期',
      '每日3,000积分',
      'Gemini/Code = API 通0元/次',
      'Claude Code = API $1.5/元',
      '仅白日程运适用指令CLI(TC + B)',
      '交持Claude 4 Opus & Sonnet',
      '24/7 技术支持'
    ],
    buttonText: '选择此套餐',
    featured: false
  },
  {
    id: 3,
    name: 'Max 100',
    subtitle: '适合专业用户',
    price: 50,
    badge: null,
    features: [
      '30天有效期',
      '每日50,000积分',
      'Gemini/Code = API 通0元/次',
      'Claude Code = API $1/元',
      '交持所有通道运适用(TC + B)',
      '仅白日程运适用指令CLI',
      '24/7 优先支持'
    ],
    buttonText: '选择此套餐',
    featured: true
  },
  {
    id: 4,
    name: 'Max 200',
    subtitle: '适合高级用户',
    price: 100,
    badge: { text: '接适过3100次/月需求—套餐', type: 'orange' },
    features: [
      '30天有效期',
      '每日100,000积分',
      'Gemini/Code = API 通0元/次',
      'Claude Code = API $34/元',
      '交持所有通道运适用(TC + B)',
      '仅白日程运适用指令CLI',
      '24/7 技术支持'
    ],
    buttonText: '选择此套餐',
    featured: false
  }
])

// 积分包数据
const creditPackages = ref([
  {
    id: 1,
    amount: 8000,
    price: 1,
    bonus: 'Gemini/Code = API $0元',
    hot: false
  },
  {
    id: 2,
    amount: 25000,
    price: 3,
    bonus: 'Gemini/Code = API $0元',
    hot: true
  },
  {
    id: 3,
    amount: 43000,
    price: 5,
    bonus: 'Gemini/Code = API $1元',
    hot: false
  },
  {
    id: 4,
    amount: 88000,
    price: 10,
    bonus: 'Gemini/Code = API $0元',
    hot: false
  }
])

// 订阅历史
const subscriptionHistory = ref([
  { id: 1, planName: 'Max 100', startDate: '2025-10-09', endDate: '2025-10-13', status: 'expired' },
  { id: 2, planName: '体验卡', startDate: '2025-09-30', endDate: '2025-10-01', status: 'expired' },
  { id: 3, planName: '体验卡', startDate: '2025-09-25', endDate: '2025-09-26', status: 'expired' },
  { id: 4, planName: 'Max 100', startDate: '2025-09-04', endDate: '2025-09-25', status: 'expired' }
])

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

const handleSubscribe = async (plan: any) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  ElMessageBox.confirm(
    `确定订阅 ${plan.name} 套餐吗? 价格: $${plan.price}/月`,
    '确认订阅',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      // await subscriptionAPI.subscribe({ planId: plan.id })
      ElMessage.success('订阅成功!')
      loadSubscriptionHistory()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '订阅失败')
    }
  }).catch(() => {})
}

const handlePurchaseCredit = async (credit: any) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  ElMessageBox.confirm(
    `确定购买 ${credit.amount} 积分吗? 价格: $${credit.price}`,
    '确认购买',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      // await subscriptionAPI.purchaseCredits({ packageId: credit.id })
      ElMessage.success('购买成功!')
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '购买失败')
    }
  }).catch(() => {})
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
      // await subscriptionAPI.cancelSubscription(subscription.id)
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
    // subscriptionHistory.value = res.data
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
  padding: 0;
}

/* 页面头部 */
.page-header-section {
  text-align: center;
  margin-bottom: var(--spacing-8);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-3) 0;
}

.page-description {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-6) 0;
}

.tab-switches {
  display: flex;
  gap: var(--spacing-3);
  justify-content: center;
}

/* 试用横幅 */
.trial-banner {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  padding: var(--spacing-6);
  margin-bottom: var(--spacing-8);
  background: linear-gradient(135deg, var(--color-primary-50) 0%, var(--color-primary-100) 100%);
  border: 1px solid var(--color-primary-200);
}

.banner-icon {
  font-size: var(--font-size-4xl);
  color: var(--color-primary);
}

.banner-content h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-1) 0;
}

.banner-content p {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 区块标题 */
.section-title-row {
  margin-bottom: var(--spacing-4);
}

.section-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.section-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-6) 0;
}

/* 套餐网格 */
.plans-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-12);
}

.plan-card {
  position: relative;
  display: flex;
  flex-direction: column;
  transition: all var(--transition-base);
}

.plan-card.featured {
  border: 2px solid var(--color-primary);
  transform: scale(1.02);
}

.plan-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.badge-gray {
  background: var(--color-gray-600);
  color: white;
}

.badge-green {
  background: var(--color-success-500);
  color: white;
}

.badge-orange {
  background: var(--color-warning-500);
  color: white;
}

.plan-header {
  text-align: center;
  margin-bottom: var(--spacing-4);
}

.plan-name {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.plan-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.plan-price {
  text-align: center;
  margin-bottom: var(--spacing-6);
}

.price-symbol {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  vertical-align: top;
}

.price-value {
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.price-period {
  font-size: var(--font-size-base);
  color: var(--color-text-tertiary);
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--spacing-6) 0;
  flex: 1;
}

.plan-features li {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-2);
  padding: var(--spacing-2) 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
}

.feature-icon {
  color: var(--color-success-500);
  flex-shrink: 0;
  margin-top: 2px;
}

.subscribe-btn {
  width: 100%;
}

/* 积分包网格 */
.credits-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-12);
}

.credit-card {
  position: relative;
  text-align: center;
  display: flex;
  flex-direction: column;
}

.hot-badge {
  position: absolute;
  top: -8px;
  right: var(--spacing-4);
  background: var(--color-danger-500);
  color: white;
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-base);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.credit-header h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-4) 0;
}

.credit-price {
  margin-bottom: var(--spacing-4);
}

.credit-details {
  margin-bottom: var(--spacing-4);
}

.credit-details p {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: var(--spacing-1) 0;
}

.credit-bonus {
  color: var(--color-primary) !important;
  font-weight: var(--font-weight-medium);
}

.credit-features {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--spacing-6) 0;
  flex: 1;
}

.credit-features li {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  padding: var(--spacing-1) 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.purchase-btn {
  width: 100%;
}

/* 历史表格 */
.history-table-card {
  overflow: hidden;
}

/* 响应式 */
@media (max-width: 1280px) {
  .plans-grid,
  .credits-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .plans-grid,
  .credits-grid {
    grid-template-columns: 1fr;
  }

  .trial-banner {
    flex-direction: column;
    text-align: center;
  }

  .tab-switches {
    flex-direction: column;
  }
}
</style>

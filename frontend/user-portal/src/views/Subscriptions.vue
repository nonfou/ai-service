<template>
  <div class="subscriptions-page">
    <!-- 页面标题 -->
    <div class="page-header-section">
      <span class="hero-badge">订阅方案</span>
      <h1 class="page-title">选择适合您的套餐</h1>
      <p class="page-description">根据使用需求灵活选择,按月付费随时升级,透明计费无隐藏费用。</p>
      <div class="tab-switches">
        <button
          :class="['tab-btn', { active: activeTab === 'monthly' }]"
          @click="activeTab = 'monthly'"
        >
          月付
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'yearly' }]"
          @click="activeTab = 'yearly'"
        >
          年付
          <span class="save-badge">省20%</span>
        </button>
      </div>
    </div>

    <!-- 订阅套餐卡片 -->
    <div class="section-heading">
      <h2>订阅套餐</h2>
      <p>选择适合您团队规模的套餐方案,随时升级无忧</p>
    </div>

    <div class="plans-grid">
      <div
        v-for="plan in subscriptionPlans"
        :key="plan.id"
        class="plan-card"
        :class="{ 'plan-card-featured': plan.featured }"
      >
        <!-- 标签 -->
        <span v-if="plan.badge" class="plan-badge" :class="`badge-${plan.badge.type}`">
          {{ plan.badge.text }}
        </span>

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
          {{ plan.buttonText || '立即订阅' }}
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
import { Check } from '@element-plus/icons-vue'
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
    subtitle: '快速体验 AI 开发平台',
    price: 1,
    badge: { text: '新用户专享', type: 'gray' },
    features: [
      '1天有效期',
      '600 积分额度',
      'Claude & Gemini 模型',
      '24/7 技术支持'
    ],
    buttonText: '立即试用'
  },
  {
    id: 3,
    name: 'Max 100',
    subtitle: '适合专业开发者',
    price: 50,
    badge: { text: '最受欢迎', type: 'green' },
    features: [
      '30天有效期',
      '每日 50,000 积分',
      '全模型访问权限',
      'IDE 与 CLI 集成',
      '优先技术支持'
    ],
    buttonText: '选择此套餐',
    featured: true
  },
  {
    id: 4,
    name: 'Max 200',
    subtitle: '适合团队协作',
    price: 100,
    badge: { text: '企业推荐', type: 'orange' },
    features: [
      '30天有效期',
      '每日 100,000 积分',
      '全模型访问权限',
      '多账号管理',
      '专属技术支持'
    ],
    buttonText: '选择此套餐',
    featured: false
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

.save-badge {
  padding: 0.25rem 0.625rem;
  border-radius: 999px;
  font-size: 0.75rem;
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
  font-weight: 700;
}

.tab-btn.active .save-badge {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
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
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
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
  transform: scale(1.05);
}

.plan-card-featured:hover {
  transform: scale(1.05) translateY(-6px);
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
  margin-bottom: 1.75rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.price-symbol {
  font-size: 1.125rem;
  font-weight: 600;
  color: #475569;
  vertical-align: top;
  margin-right: 0.125rem;
}

.price-value {
  font-size: 2.75rem;
  font-weight: 800;
  color: #1f2937;
  letter-spacing: -0.03em;
}

.price-period {
  font-size: 0.95rem;
  color: #64748b;
  margin-left: 0.25rem;
  font-weight: 500;
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

  .plan-card-featured {
    transform: scale(1);
  }

  .plan-card-featured:hover {
    transform: translateY(-6px);
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

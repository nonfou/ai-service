<template>
  <div class="overview-page">
    <!-- 欢迎标题 -->
    <div class="welcome-section">
      <h1 class="welcome-title">欢迎回来,{{ username }}</h1>
      <p class="welcome-subtitle">这是您的配额使用概览</p>
    </div>

    <!-- 今日使用统计 -->
    <div class="today-usage-card v2board-card">
      <div class="card-header-row">
        <div class="card-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="card-header-content">
          <h3 class="card-title">我的今日使用</h3>
          <p class="card-date">{{ formatToday() }}</p>
        </div>
      </div>

      <div class="usage-stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ stats.todayRequests || 0 }}</div>
          <div class="stat-label">请求次数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.modelsUsed || 0 }}</div>
          <div class="stat-label">使用模型</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.creditsUsed || 0 }}</div>
          <div class="stat-label">消耗积分</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">${{ formatNumber(stats.todayCost) }}</div>
          <div class="stat-label">总消费</div>
        </div>
      </div>

      <div class="model-usage-section">
        <h4 class="section-title">模型使用详情</h4>
        <div v-if="stats.modelUsage && stats.modelUsage.length > 0" class="model-list">
          <div v-for="model in stats.modelUsage" :key="model.name" class="model-item">
            <span class="model-name">{{ model.name }}</span>
            <span class="model-count">{{ model.count }} 次</span>
          </div>
        </div>
        <div v-else class="empty-data">
          <el-icon class="empty-icon"><DataLine /></el-icon>
          <p>暂无使用数据</p>
        </div>
      </div>

      <div class="last-update">最后更新: {{ formatTime(new Date()) }}</div>
    </div>

    <!-- 第二行: 配额、套餐、订阅 -->
    <div class="info-cards-grid">
      <!-- 当前可用配额 -->
      <div class="quota-card v2board-card">
        <div class="card-icon-header">
          <el-icon class="header-icon"><Coin /></el-icon>
        </div>
        <dl class="info-list">
          <dt>当前可用</dt>
          <dd>{{ stats.currentQuota || 0 }} / {{ stats.totalQuota || 0 }}</dd>
        </dl>
        <div class="progress-section">
          <el-progress
            :percentage="quotaPercentage"
            :color="quotaColor"
            :stroke-width="8"
          />
          <p class="progress-text">{{ quotaPercentage }}% 剩余</p>
          <p class="reset-time">下次重置: {{ formatResetTime() }}</p>
        </div>
      </div>

      <!-- 套餐到期时间 -->
      <div class="plan-card v2board-card">
        <div class="card-icon-header">
          <el-icon class="header-icon"><Calendar /></el-icon>
        </div>
        <dl class="info-list">
          <dt>套餐到期时间</dt>
          <dd>{{ stats.planExpiry || '暂无订阅' }}</dd>
        </dl>
      </div>

      <!-- 活跃订阅 -->
      <div class="subscription-card v2board-card">
        <div class="card-icon-header">
          <el-icon class="header-icon"><ShoppingCart /></el-icon>
        </div>
        <dt class="info-label">活跃订阅</dt>
        <dd class="info-value">
          <span class="sub-count">{{ stats.activeSubscriptions || 0 }}</span>
          <span class="sub-unit">个套餐</span>
        </dd>
      </div>
    </div>

    <!-- 系统公告与快速开始已移除，根据需求保留右侧主要内容 -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  TrendCharts,
  DataLine,
  Coin,
  Calendar,
  ShoppingCart,
  Bell
} from '@element-plus/icons-vue'
import { userAPI } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const username = computed(() => userStore.user?.username || '用户')

const stats = ref({
  todayRequests: 0,
  modelsUsed: 0,
  creditsUsed: 0,
  todayCost: 0,
  currentQuota: 3607,
  totalQuota: 1000,
  planExpiry: '',
  activeSubscriptions: 0,
  modelUsage: [] as Array<{ name: string; count: number }>
})

const announcements = ref([
  // { id: 1, title: '系统维护通知' }
])

const quotaPercentage = computed(() => {
  if (!stats.value.totalQuota) return 100
  return Math.round((stats.value.currentQuota / stats.value.totalQuota) * 100)
})

const quotaColor = computed(() => {
  const percentage = quotaPercentage.value
  if (percentage > 50) return '#67c23a'
  if (percentage > 20) return '#e6a23c'
  return '#f56c6c'
})

const formatNumber = (num: number) => {
  return (num || 0).toFixed(3)
}

const formatToday = () => {
  const today = new Date()
  return today.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatResetTime = () => {
  // 示例: 下一天的 8:00
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(8, 0, 0, 0)

  const now = new Date()
  const diff = tomorrow.getTime() - now.getTime()
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)

  const dateStr = `${tomorrow.getMonth() + 1}-${tomorrow.getDate()} 08:00`
  const countdownStr = `(${hours}小时${minutes}分钟${seconds}秒后)`

  return `${dateStr} ${countdownStr}`
}

const loadStats = async () => {
  try {
    const res = await userAPI.getUserStats()
    if (res.data) {
      stats.value = { ...stats.value, ...res.data }
    }
  } catch (error) {
    console.error('Failed to load stats')
  }
}

onMounted(() => {
  loadStats()
  // 每5秒更新一次时间
  setInterval(() => {
    // 触发响应式更新
  }, 5000)
})
</script>

<style scoped>
.overview-page {
  padding: 0;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: var(--spacing-8);
}

.welcome-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.welcome-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 今日使用卡片 */
.today-usage-card {
  margin-bottom: var(--spacing-6);
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--font-size-2xl);
}

.card-header-content {
  flex: 1;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-1) 0;
}

.card-date {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin: 0;
}

/* 使用统计网格 */
.usage-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-6);
  padding-bottom: var(--spacing-6);
  border-bottom: 1px solid var(--color-border-light);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-2);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 模型使用详情 */
.model-usage-section {
  margin-bottom: var(--spacing-4);
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-4) 0;
}

.model-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.model-item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-2);
  background: var(--color-gray-50);
  border-radius: var(--radius-base);
}

.model-name {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.model-count {
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
}

.empty-data {
  text-align: center;
  padding: var(--spacing-8) var(--spacing-4);
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: var(--font-size-4xl);
  color: var(--color-text-disabled);
  margin-bottom: var(--spacing-2);
}

.empty-data p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.last-update {
  text-align: right;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

/* 信息卡片网格 */
.info-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-6);
}

.card-icon-header {
  margin-bottom: var(--spacing-4);
}

.header-icon {
  width: 40px;
  height: 40px;
  padding: var(--spacing-2);
  border-radius: var(--radius-lg);
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-size: var(--font-size-xl);
}

.info-list {
  margin: 0 0 var(--spacing-4) 0;
}

.info-list dt {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-2);
}

.info-list dd {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

/* 进度条部分 */
.progress-section {
  margin-top: var(--spacing-4);
}

.progress-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: var(--spacing-2) 0;
}

.reset-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin: var(--spacing-1) 0 0 0;
}

/* 活跃订阅卡片 */
.info-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-3);
}

.info-value {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-2);
}

.sub-count {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.sub-unit {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

/* 底部卡片网格 */
.bottom-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-6);
}

.card-header-with-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
}

/* 系统公告 */
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.announcement-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-base);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.announcement-icon {
  color: var(--color-warning-500);
  flex-shrink: 0;
}

/* 快速开始 */
.quick-start-steps {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
}

.step-item {
  display: flex;
  gap: var(--spacing-4);
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-semibold);
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.step-code {
  display: block;
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  background: var(--color-gray-100);
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-base);
  color: var(--color-text-primary);
  overflow-x: auto;
}

.step-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.quick-start-actions {
  display: flex;
  gap: var(--spacing-3);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-border-light);
}

.action-link {
  flex: 1;
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  text-align: center;
  text-decoration: none;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast);
  border: 1px solid var(--color-border-light);
  color: var(--color-text-primary);
}

.action-link:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-link.primary {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.action-link.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: white;
}

/* 响应式 */
@media (max-width: 1024px) {
  .usage-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .info-cards-grid {
    grid-template-columns: 1fr;
  }

  .bottom-cards-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .usage-stats-grid {
    grid-template-columns: 1fr;
  }

  .quick-start-actions {
    flex-direction: column;
  }
}
</style>

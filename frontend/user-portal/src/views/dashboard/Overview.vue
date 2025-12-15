<template>
  <div class="overview-page">
    <!-- 顶部欢迎区域 -->
    <div class="header-section">
      <div class="welcome-text">
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">{{ username }}, 这是您的使用概览</p>
      </div>
      <div class="time-selector">
        <button
          v-for="option in timeOptions"
          :key="option.value"
          :class="['time-btn', { active: selectedDays === option.value }]"
          @click="selectTimeRange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <!-- 账户概览卡片 -->
    <div class="account-card" v-loading="balanceLoading">
      <div class="account-left">
        <div class="balance-section">
          <span class="balance-label">账户余额</span>
          <div class="balance-amount">
            <span class="currency">$</span>
            <span class="amount">{{ formatBalance(balance) }}</span>
            <span class="unit">USD</span>
          </div>
        </div>
        <button class="recharge-btn" @click="goToWallet">
          <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
          充值
        </button>
      </div>
      <div class="account-right">
        <div class="stat-item">
          <span class="stat-label">累计充值</span>
          <span class="stat-value">${{ formatCost(statistics.totalRecharge) }}</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-label">累计消费</span>
          <span class="stat-value">${{ formatCost(statistics.totalSpent) }}</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-label">本月消费</span>
          <span class="stat-value">${{ formatCost(statistics.monthlySpent) }}</span>
        </div>
      </div>
    </div>

    <!-- 配额区域 -->
    <div class="quota-section" v-loading="quotaLoading">
      <div class="quota-card">
        <div class="quota-header">
          <span class="quota-title">每日配额</span>
          <span :class="['quota-status', { exceeded: quota.daily.isExceeded }]">
            {{ quota.daily.isExceeded ? '已超限' : '正常' }}
          </span>
        </div>
        <div class="quota-ring-container">
          <svg class="quota-ring" viewBox="0 0 120 120">
            <circle class="ring-bg" cx="60" cy="60" r="52"/>
            <circle
              class="ring-progress"
              cx="60" cy="60" r="52"
              :style="{
                strokeDasharray: `${quota.daily.usagePercentage * 3.267} 326.7`,
                stroke: getQuotaColor(quota.daily.usagePercentage)
              }"
            />
          </svg>
          <div class="quota-center">
            <span class="quota-percent">{{ Math.round(quota.daily.usagePercentage) }}%</span>
            <span class="quota-label">已使用</span>
          </div>
        </div>
        <div class="quota-details">
          <div class="quota-row">
            <span>已用 / 总额</span>
            <span class="quota-value">${{ formatCost(quota.daily.usedAmount) }} / ${{ formatCost(quota.daily.quotaAmount) }}</span>
          </div>
          <div class="quota-row">
            <span>重置时间</span>
            <span class="quota-value">{{ formatResetTime(quota.daily.resetAt) }}</span>
          </div>
        </div>
      </div>

      <div class="quota-card">
        <div class="quota-header">
          <span class="quota-title">每月配额</span>
          <span :class="['quota-status', { exceeded: quota.monthly.isExceeded }]">
            {{ quota.monthly.isExceeded ? '已超限' : '正常' }}
          </span>
        </div>
        <div class="quota-ring-container">
          <svg class="quota-ring" viewBox="0 0 120 120">
            <circle class="ring-bg" cx="60" cy="60" r="52"/>
            <circle
              class="ring-progress"
              cx="60" cy="60" r="52"
              :style="{
                strokeDasharray: `${quota.monthly.usagePercentage * 3.267} 326.7`,
                stroke: getQuotaColor(quota.monthly.usagePercentage)
              }"
            />
          </svg>
          <div class="quota-center">
            <span class="quota-percent">{{ Math.round(quota.monthly.usagePercentage) }}%</span>
            <span class="quota-label">已使用</span>
          </div>
        </div>
        <div class="quota-details">
          <div class="quota-row">
            <span>已用 / 总额</span>
            <span class="quota-value">${{ formatCost(quota.monthly.usedAmount) }} / ${{ formatCost(quota.monthly.quotaAmount) }}</span>
          </div>
          <div class="quota-row">
            <span>重置时间</span>
            <span class="quota-value">{{ formatResetTime(quota.monthly.resetAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心统计区 -->
    <div class="stats-grid" v-loading="summaryLoading">
      <div class="stats-card today">
        <div class="stats-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ formatNumber(summary.today.calls) }}</span>
          <span class="stats-label">今日请求</span>
        </div>
      </div>
      <div class="stats-card today">
        <div class="stats-icon cost">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v12M8 10h8M8 14h8"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">${{ formatCost(summary.today.cost) }}</span>
          <span class="stats-label">今日费用</span>
        </div>
      </div>
      <div class="stats-card today">
        <div class="stats-icon tokens">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ formatTokens(summary.today.inputTokens + summary.today.outputTokens) }}</span>
          <span class="stats-label">今日Token</span>
        </div>
      </div>
      <div class="stats-card total">
        <div class="stats-icon total-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ formatNumber(summary.total.calls) }}</span>
          <span class="stats-label">总请求数</span>
        </div>
      </div>
      <div class="stats-card total">
        <div class="stats-icon total-icon cost">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v12M8 10h8M8 14h8"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">${{ formatCost(summary.total.cost) }}</span>
          <span class="stats-label">总费用</span>
        </div>
      </div>
      <div class="stats-card total">
        <div class="stats-icon total-icon tokens">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
          </svg>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ formatTokens(summary.total.inputTokens + summary.total.outputTokens) }}</span>
          <span class="stats-label">总Token消耗</span>
        </div>
      </div>
    </div>

    <!-- Token分析区 -->
    <div class="chart-section token-section" v-loading="summaryLoading">
      <div class="section-header">
        <h3 class="section-title">Token 使用分析</h3>
        <span class="section-subtitle">输入、输出与缓存Token分布</span>
      </div>
      <div class="token-bars">
        <div class="token-bar-group">
          <div class="bar-label">今日</div>
          <div class="stacked-bar">
            <div
              class="bar-segment input"
              :style="{ width: getTokenBarWidth(summary.today, 'input') + '%' }"
              :title="`输入: ${formatTokens(summary.today.inputTokens)}`"
            ></div>
            <div
              class="bar-segment output"
              :style="{ width: getTokenBarWidth(summary.today, 'output') + '%' }"
              :title="`输出: ${formatTokens(summary.today.outputTokens)}`"
            ></div>
            <div
              class="bar-segment cache"
              :style="{ width: getTokenBarWidth(summary.today, 'cache') + '%' }"
              :title="`缓存: ${formatTokens(summary.today.cacheReadTokens + summary.today.cacheWriteTokens)}`"
            ></div>
          </div>
          <div class="bar-value">{{ formatTokens(summary.today.inputTokens + summary.today.outputTokens + summary.today.cacheReadTokens + summary.today.cacheWriteTokens) }}</div>
        </div>
        <div class="token-bar-group">
          <div class="bar-label">总计</div>
          <div class="stacked-bar">
            <div
              class="bar-segment input"
              :style="{ width: getTokenBarWidth(summary.total, 'input') + '%' }"
              :title="`输入: ${formatTokens(summary.total.inputTokens)}`"
            ></div>
            <div
              class="bar-segment output"
              :style="{ width: getTokenBarWidth(summary.total, 'output') + '%' }"
              :title="`输出: ${formatTokens(summary.total.outputTokens)}`"
            ></div>
            <div
              class="bar-segment cache"
              :style="{ width: getTokenBarWidth(summary.total, 'cache') + '%' }"
              :title="`缓存: ${formatTokens(summary.total.cacheReadTokens + summary.total.cacheWriteTokens)}`"
            ></div>
          </div>
          <div class="bar-value">{{ formatTokens(summary.total.inputTokens + summary.total.outputTokens + summary.total.cacheReadTokens + summary.total.cacheWriteTokens) }}</div>
        </div>
      </div>
      <div class="token-legend">
        <div class="legend-item"><span class="legend-dot input"></span>输入Token</div>
        <div class="legend-item"><span class="legend-dot output"></span>输出Token</div>
        <div class="legend-item"><span class="legend-dot cache"></span>缓存Token</div>
      </div>
    </div>

    <!-- 使用趋势图表 -->
    <div class="chart-section" v-loading="trendLoading">
      <div class="section-header">
        <h3 class="section-title">使用趋势</h3>
        <span class="section-subtitle">请求次数与费用变化</span>
      </div>
      <div class="chart-container">
        <canvas ref="trendChartRef" v-show="trendData.length > 0"></canvas>
        <div v-if="trendData.length === 0 && !trendLoading" class="empty-chart">
          <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
          <p>暂无趋势数据</p>
        </div>
      </div>
    </div>

    <!-- 模型消耗分析 -->
    <div class="model-analysis" v-loading="modelLoading">
      <div class="chart-section model-chart-section">
        <div class="section-header">
          <h3 class="section-title">模型消耗分布</h3>
          <span class="section-subtitle">TOP 10 模型费用占比</span>
        </div>
        <div class="chart-container pie-container">
          <canvas ref="modelChartRef" v-show="modelStats.length > 0"></canvas>
          <div v-if="modelStats.length === 0 && !modelLoading" class="empty-chart">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 2a10 10 0 0 1 10 10"/>
            </svg>
            <p>暂无模型数据</p>
          </div>
        </div>
      </div>

      <div class="chart-section model-table-section">
        <div class="section-header">
          <h3 class="section-title">模型详情</h3>
          <span class="section-subtitle">各模型使用统计</span>
        </div>
        <div class="model-table-container">
          <el-table :data="modelStats" style="width: 100%" size="small" :show-header="true">
            <el-table-column prop="model" label="模型" min-width="160">
              <template #default="scope">
                <span class="model-name">{{ scope.row.model }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="calls" label="调用次数" width="90" align="right">
              <template #default="scope">
                <span class="table-value">{{ formatNumber(scope.row.calls) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Token" width="100" align="right">
              <template #default="scope">
                <span class="table-value">{{ formatTokens(scope.row.totalInputTokens + scope.row.totalOutputTokens) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="费用" width="90" align="right">
              <template #default="scope">
                <span class="table-value cost">${{ formatCost(scope.row.totalCost) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="占比" width="70" align="right">
              <template #default="scope">
                <span class="table-percent">{{ calculatePercentage(scope.row.totalCost) }}%</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="modelStats.length === 0 && !modelLoading" class="empty-table">
            <p>暂无模型使用数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 24小时分布 -->
    <div class="chart-section hourly-section" v-loading="hourlyLoading">
      <div class="section-header">
        <h3 class="section-title">今日时段分析</h3>
        <span class="section-subtitle">24小时请求分布</span>
      </div>
      <div class="chart-container">
        <canvas ref="hourlyChartRef" v-show="hourlyData.length > 0"></canvas>
        <div v-if="hourlyData.length === 0 && !hourlyLoading" class="empty-chart">
          <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          <p>暂无时段数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { statisticsAPI, balanceAPI, quotaAPI, type StatsSummary, type HourlyStats, type UserQuota } from '../../api'
import { useUserStore } from '../../stores/user'

let Chart: any = null

const router = useRouter()
const userStore = useUserStore()
const username = computed(() => userStore.user?.username || userStore.user?.email?.split('@')[0] || '用户')

// 时间维度选项
const timeOptions = [
  { value: 1, label: '今天' },
  { value: 7, label: '7天' },
  { value: 30, label: '30天' }
]
const selectedDays = ref(7)

// 加载状态
const balanceLoading = ref(false)
const quotaLoading = ref(false)
const summaryLoading = ref(false)
const trendLoading = ref(false)
const modelLoading = ref(false)
const hourlyLoading = ref(false)

// 数据
const balance = ref(0)
const statistics = ref({
  totalRecharge: 0,
  totalSpent: 0,
  monthlySpent: 0
})

const quota = ref<UserQuota>({
  daily: { quotaAmount: 0, usedAmount: 0, remainingAmount: 0, usagePercentage: 0, resetAt: '', isExceeded: false, isEnabled: false },
  monthly: { quotaAmount: 0, usedAmount: 0, remainingAmount: 0, usagePercentage: 0, resetAt: '', isExceeded: false, isEnabled: false }
})

const summary = ref<StatsSummary>({
  today: { calls: 0, cost: 0, inputTokens: 0, outputTokens: 0, cacheReadTokens: 0, cacheWriteTokens: 0 },
  total: { calls: 0, cost: 0, inputTokens: 0, outputTokens: 0, cacheReadTokens: 0, cacheWriteTokens: 0 }
})

const trendData = ref<Array<{ date: string; calls: number; cost: number }>>([])
const modelStats = ref<Array<{ model: string; calls: number; totalCost: number; totalInputTokens: number; totalOutputTokens: number }>>([])
const hourlyData = ref<HourlyStats[]>([])

// Chart 实例
let trendChart: any = null
let modelChart: any = null
let hourlyChart: any = null
const trendChartRef = ref<HTMLCanvasElement | null>(null)
const modelChartRef = ref<HTMLCanvasElement | null>(null)
const hourlyChartRef = ref<HTMLCanvasElement | null>(null)

// 选择时间范围
const selectTimeRange = (days: number) => {
  selectedDays.value = days
}

// 跳转到钱包页面
const goToWallet = () => {
  router.push('/wallet')
}

// 加载余额数据
const loadBalance = async () => {
  balanceLoading.value = true
  try {
    const [balanceRes, statsRes] = await Promise.all([
      balanceAPI.getBalance(),
      balanceAPI.getBalanceStatistics()
    ])
    if (balanceRes.data) {
      balance.value = balanceRes.data.balance || 0
    }
    if (statsRes.data) {
      statistics.value = {
        totalRecharge: statsRes.data.totalRecharge || 0,
        totalSpent: statsRes.data.totalSpent || 0,
        monthlySpent: statsRes.data.monthlySpent || 0
      }
    }
  } catch (error) {
    console.error('加载余额失败:', error)
  } finally {
    balanceLoading.value = false
  }
}

// 加载配额数据
const loadQuota = async () => {
  quotaLoading.value = true
  try {
    const res = await quotaAPI.getQuota()
    if (res.data) {
      quota.value = res.data
    }
  } catch (error) {
    console.error('加载配额失败:', error)
  } finally {
    quotaLoading.value = false
  }
}

// 加载综合统计
const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await statisticsAPI.getSummary()
    if (res.data) {
      summary.value = res.data
    }
  } catch (error) {
    console.error('加载综合统计失败:', error)
  } finally {
    summaryLoading.value = false
  }
}

// 加载趋势数据
const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await statisticsAPI.getUsageTrend(selectedDays.value)
    if (res.data) {
      trendData.value = res.data.map((item: any) => ({
        date: item.date,
        calls: item.calls || 0,
        cost: parseFloat(item.cost) || 0
      })).sort((a: any, b: any) => new Date(a.date).getTime() - new Date(b.date).getTime())
    }
    await nextTick()
    updateTrendChart()
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  } finally {
    trendLoading.value = false
  }
}

// 加载模型统计
const loadModelStats = async () => {
  modelLoading.value = true
  try {
    const res = await statisticsAPI.getModelUsage()
    if (res.data) {
      modelStats.value = res.data.map((item: any) => ({
        model: item.model,
        calls: item.calls || 0,
        totalCost: parseFloat(item.totalCost) || 0,
        totalInputTokens: item.totalInputTokens || 0,
        totalOutputTokens: item.totalOutputTokens || 0
      })).sort((a: any, b: any) => b.totalCost - a.totalCost)
    }
    await nextTick()
    updateModelChart()
  } catch (error) {
    console.error('加载模型统计失败:', error)
  } finally {
    modelLoading.value = false
  }
}

// 加载小时统计
const loadHourlyStats = async () => {
  hourlyLoading.value = true
  try {
    const res = await statisticsAPI.getHourlyStats()
    if (res.data) {
      hourlyData.value = res.data
    }
    await nextTick()
    updateHourlyChart()
  } catch (error) {
    console.error('加载时段统计失败:', error)
  } finally {
    hourlyLoading.value = false
  }
}

// 更新趋势图表
const updateTrendChart = () => {
  if (!Chart || !trendChartRef.value || trendData.value.length === 0) return

  const ctx = trendChartRef.value.getContext('2d')
  if (!ctx) return

  if (trendChart) trendChart.destroy()

  const labels = trendData.value.map(item => {
    const date = new Date(item.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  })

  trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '请求数',
          data: trendData.value.map(item => item.calls),
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          yAxisID: 'y',
          pointRadius: 3,
          pointHoverRadius: 5
        },
        {
          label: '费用 (USD)',
          data: trendData.value.map(item => item.cost),
          borderColor: '#ec4899',
          backgroundColor: 'rgba(236, 72, 153, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          yAxisID: 'y1',
          pointRadius: 3,
          pointHoverRadius: 5
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: {
          position: 'top',
          labels: { usePointStyle: true, padding: 20, font: { size: 12 } }
        }
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          title: { display: true, text: '请求数', font: { size: 11 } },
          grid: { color: 'rgba(0, 0, 0, 0.05)' },
          ticks: { font: { size: 11 } }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          title: { display: true, text: '费用 (USD)', font: { size: 11 } },
          grid: { drawOnChartArea: false },
          ticks: { font: { size: 11 } }
        }
      }
    }
  })
}

// 更新模型饼图
const updateModelChart = () => {
  if (!Chart || !modelChartRef.value || modelStats.value.length === 0) return

  const ctx = modelChartRef.value.getContext('2d')
  if (!ctx) return

  if (modelChart) modelChart.destroy()

  const colors = ['#8b5cf6', '#ec4899', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#84cc16', '#f97316', '#14b8a6']
  const topModels = modelStats.value.slice(0, 10)

  modelChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: topModels.map(item => item.model),
      datasets: [{
        data: topModels.map(item => item.totalCost),
        backgroundColor: colors.slice(0, topModels.length),
        borderWidth: 0,
        hoverOffset: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '65%',
      plugins: {
        legend: {
          position: 'right',
          labels: {
            usePointStyle: true,
            padding: 12,
            font: { size: 11 },
            generateLabels: function(chart: any) {
              const data = chart.data
              if (data.labels && data.datasets.length) {
                return data.labels.map((label: string, i: number) => {
                  const value = data.datasets[0].data[i] as number
                  const shortLabel = label.length > 18 ? label.substring(0, 18) + '...' : label
                  return {
                    text: `${shortLabel}: $${value.toFixed(4)}`,
                    fillStyle: colors[i],
                    strokeStyle: colors[i],
                    lineWidth: 0,
                    pointStyle: 'circle',
                    hidden: false,
                    index: i
                  }
                })
              }
              return []
            }
          }
        }
      }
    }
  })
}

// 更新小时柱状图
const updateHourlyChart = () => {
  if (!Chart || !hourlyChartRef.value || hourlyData.value.length === 0) return

  const ctx = hourlyChartRef.value.getContext('2d')
  if (!ctx) return

  if (hourlyChart) hourlyChart.destroy()

  const labels = hourlyData.value.map(item => `${item.hour}:00`)

  hourlyChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: '请求数',
        data: hourlyData.value.map(item => item.calls),
        backgroundColor: 'rgba(139, 92, 246, 0.7)',
        borderRadius: 4,
        barPercentage: 0.7
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 10 }, maxRotation: 0 } },
        y: {
          grid: { color: 'rgba(0, 0, 0, 0.05)' },
          ticks: { font: { size: 11 } },
          beginAtZero: true
        }
      }
    }
  })
}

// 格式化函数
const formatBalance = (num: number) => {
  return num.toFixed(2)
}

const formatNumber = (num: number) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

const formatCost = (cost: number) => {
  if (cost === undefined || cost === null) return '0.00'
  return cost.toFixed(4)
}

const formatTokens = (tokens: number) => {
  if (tokens >= 1000000) return (tokens / 1000000).toFixed(2) + 'M'
  if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'K'
  return tokens.toString()
}

const formatResetTime = (resetAt: string) => {
  if (!resetAt) return '-'
  const reset = new Date(resetAt)
  const now = new Date()
  const diff = reset.getTime() - now.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天后`
  if (hours > 0) return `${hours}小时后`
  if (diff > 0) return '即将重置'
  return '已重置'
}

const getQuotaColor = (percentage: number) => {
  if (percentage >= 80) return '#ef4444'
  if (percentage >= 60) return '#f59e0b'
  return '#10b981'
}

const calculatePercentage = (cost: number) => {
  const total = modelStats.value.reduce((sum, item) => sum + item.totalCost, 0)
  if (total === 0) return '0.0'
  return ((cost / total) * 100).toFixed(1)
}

const getTokenBarWidth = (data: any, type: string) => {
  const total = (data.inputTokens || 0) + (data.outputTokens || 0) + (data.cacheReadTokens || 0) + (data.cacheWriteTokens || 0)
  if (total === 0) return 0
  if (type === 'input') return ((data.inputTokens || 0) / total) * 100
  if (type === 'output') return ((data.outputTokens || 0) / total) * 100
  return (((data.cacheReadTokens || 0) + (data.cacheWriteTokens || 0)) / total) * 100
}

// 监听时间范围变化
watch(selectedDays, () => {
  if (Chart) {
    loadTrend()
  }
})

onMounted(async () => {
  try {
    const chartModule = await import('chart.js/auto')
    Chart = chartModule.Chart || chartModule.default

    // 并行加载数据
    await Promise.all([
      loadBalance(),
      loadQuota(),
      loadSummary(),
      loadTrend(),
      loadModelStats(),
      loadHourlyStats()
    ])
  } catch (error) {
    console.error('初始化失败:', error)
  }
})

onUnmounted(() => {
  if (trendChart) trendChart.destroy()
  if (modelChart) modelChart.destroy()
  if (hourlyChart) hourlyChart.destroy()
})
</script>

<style scoped>
.overview-page {
  padding: 0;
  min-height: 100%;
}

/* 头部区域 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e1b4b;
  margin: 0 0 4px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.time-selector {
  display: flex;
  gap: 6px;
  background: #f3f4f6;
  padding: 4px;
  border-radius: 10px;
}

.time-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.time-btn:hover {
  color: #1e1b4b;
}

.time-btn.active {
  background: white;
  color: #8b5cf6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 账户卡片 */
.account-card {
  background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 50%, #2563eb 100%);
  border-radius: 20px;
  padding: 32px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  color: white;
  box-shadow: 0 10px 40px rgba(124, 58, 237, 0.3);
  position: relative;
  overflow: hidden;
}

.account-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 60%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.account-left {
  display: flex;
  align-items: center;
  gap: 32px;
  position: relative;
  z-index: 1;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
  display: block;
  margin-bottom: 8px;
}

.balance-amount {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.balance-amount .currency {
  font-size: 24px;
  font-weight: 500;
  opacity: 0.9;
}

.balance-amount .amount {
  font-size: 48px;
  font-weight: 700;
  letter-spacing: -1px;
}

.balance-amount .unit {
  font-size: 16px;
  font-weight: 500;
  opacity: 0.8;
  margin-left: 4px;
}

.recharge-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #7c3aed;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recharge-btn:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.account-right {
  display: flex;
  align-items: center;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  opacity: 0.85;
  display: block;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
}

/* 配额区域 */
.quota-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.quota-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

.quota-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.quota-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e1b4b;
}

.quota-status {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
  background: #ecfdf5;
  color: #10b981;
}

.quota-status.exceeded {
  background: #fef2f2;
  color: #ef4444;
}

.quota-ring-container {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
}

.quota-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #f3f4f6;
  stroke-width: 10;
}

.ring-progress {
  fill: none;
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease, stroke 0.3s ease;
}

.quota-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.quota-percent {
  font-size: 24px;
  font-weight: 700;
  color: #1e1b4b;
  display: block;
}

.quota-label {
  font-size: 12px;
  color: #6b7280;
}

.quota-details {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
}

.quota-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.quota-row:last-child {
  margin-bottom: 0;
}

.quota-value {
  font-weight: 500;
  color: #1e1b4b;
}

/* 核心统计区 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stats-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stats-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
  flex-shrink: 0;
}

.stats-icon svg {
  width: 22px;
  height: 22px;
}

.stats-icon.cost {
  background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
}

.stats-icon.tokens {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stats-card.total .stats-icon.total-icon {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}

.stats-card.total .stats-icon.total-icon.cost {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.stats-card.total .stats-icon.total-icon.tokens {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
}

.stats-info {
  flex: 1;
  min-width: 0;
}

.stats-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e1b4b;
  display: block;
  margin-bottom: 2px;
}

.stats-label {
  font-size: 13px;
  color: #6b7280;
}

/* 图表区域通用样式 */
.chart-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e1b4b;
  margin: 0 0 4px 0;
}

.section-subtitle {
  font-size: 13px;
  color: #9ca3af;
}

.chart-container {
  height: 280px;
  position: relative;
}

.pie-container {
  height: 260px;
}

.empty-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-chart p {
  margin: 0;
  font-size: 14px;
}

/* Token分析区 */
.token-section {
  margin-bottom: 24px;
}

.token-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.token-bar-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bar-label {
  width: 40px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}

.stacked-bar {
  flex: 1;
  height: 24px;
  background: #f3f4f6;
  border-radius: 12px;
  display: flex;
  overflow: hidden;
}

.bar-segment {
  height: 100%;
  transition: width 0.5s ease;
}

.bar-segment.input {
  background: linear-gradient(90deg, #8b5cf6, #a78bfa);
}

.bar-segment.output {
  background: linear-gradient(90deg, #ec4899, #f472b6);
}

.bar-segment.cache {
  background: linear-gradient(90deg, #06b6d4, #22d3ee);
}

.bar-value {
  width: 80px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #1e1b4b;
}

.token-legend {
  display: flex;
  gap: 24px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.input {
  background: #8b5cf6;
}

.legend-dot.output {
  background: #ec4899;
}

.legend-dot.cache {
  background: #06b6d4;
}

/* 模型分析区 */
.model-analysis {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 20px;
  margin-bottom: 24px;
}

.model-chart-section,
.model-table-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

.model-table-container {
  max-height: 300px;
  overflow-y: auto;
}

.model-name {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #8b5cf6;
}

.table-value {
  font-weight: 500;
  color: #1e1b4b;
}

.table-value.cost {
  color: #ec4899;
}

.table-percent {
  font-size: 12px;
  color: #6b7280;
}

.empty-table {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

/* 24小时分布 */
.hourly-section .chart-container {
  height: 220px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .model-analysis {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
  }

  .account-card {
    flex-direction: column;
    text-align: center;
    padding: 24px;
  }

  .account-left {
    flex-direction: column;
    gap: 20px;
  }

  .balance-amount .amount {
    font-size: 36px;
  }

  .account-right {
    flex-wrap: wrap;
    justify-content: center;
  }

  .quota-section {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stats-card {
    padding: 16px;
  }

  .stats-value {
    font-size: 18px;
  }

  .chart-container {
    height: 220px;
  }

  .token-bar-group {
    flex-wrap: wrap;
  }

  .bar-label {
    width: 100%;
    margin-bottom: 4px;
  }

  .bar-value {
    width: 100%;
    text-align: left;
    margin-top: 4px;
  }
}
</style>

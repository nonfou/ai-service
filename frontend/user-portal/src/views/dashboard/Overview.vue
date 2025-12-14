<template>
  <div class="overview-page">
    <!-- 欢迎标题 -->
    <div class="welcome-section">
      <h1 class="welcome-title">欢迎回来,{{ username }}</h1>
      <p class="welcome-subtitle">这是您的使用情况分析</p>
    </div>

    <!-- 时间维度选择器 -->
    <div class="time-selector">
      <div class="time-buttons">
        <button
          v-for="option in timeOptions"
          :key="option.value"
          :class="['time-btn', { active: selectedDays === option.value }]"
          @click="selectTimeRange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <div class="last-update">最后更新: {{ formatTime(new Date()) }}</div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards" v-loading="loading">
      <div class="stat-card">
        <div class="stat-icon requests">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stats.totalCalls) }}</div>
          <div class="stat-label">请求次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon models">
          <el-icon><Box /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.uniqueModels || 0 }}</div>
          <div class="stat-label">使用模型</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cost">
          <el-icon><Wallet /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">${{ formatCost(stats.totalCost) }}</div>
          <div class="stat-label">消耗金额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon tokens">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ formatTokens(stats.totalTokens) }}</div>
          <div class="stat-label">Token 消耗</div>
        </div>
      </div>
    </div>

    <!-- 使用趋势图表 -->
    <div class="chart-section">
      <div class="section-header">
        <h3 class="section-title">使用趋势</h3>
      </div>
      <div class="chart-container">
        <canvas ref="trendChartRef" v-show="trendData.length > 0"></canvas>
        <div v-if="trendData.length === 0 && !loading" class="empty-chart">
          <el-icon class="empty-icon"><DataLine /></el-icon>
          <p>暂无趋势数据</p>
        </div>
      </div>
    </div>

    <!-- 模型消耗分析 -->
    <div class="model-analysis">
      <div class="model-chart-section">
        <div class="section-header">
          <h3 class="section-title">模型消耗分布</h3>
        </div>
        <div class="chart-container pie-container">
          <canvas ref="modelChartRef" v-show="modelStats.length > 0"></canvas>
          <div v-if="modelStats.length === 0 && !loading" class="empty-chart">
            <el-icon class="empty-icon"><PieChart /></el-icon>
            <p>暂无模型数据</p>
          </div>
        </div>
      </div>

      <div class="model-table-section">
        <div class="section-header">
          <h3 class="section-title">模型详情</h3>
        </div>
        <div class="model-table-container">
          <el-table :data="modelStats" v-loading="loading" style="width: 100%" size="small">
            <el-table-column prop="model" label="模型" min-width="180">
              <template #default="scope">
                <span class="model-name-cell">{{ scope.row.model }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="calls" label="调用次数" width="100" align="right">
              <template #default="scope">
                {{ formatNumber(scope.row.calls) }}
              </template>
            </el-table-column>
            <el-table-column label="Token" width="120" align="right">
              <template #default="scope">
                {{ formatTokens(scope.row.totalInputTokens + scope.row.totalOutputTokens) }}
              </template>
            </el-table-column>
            <el-table-column label="费用" width="100" align="right">
              <template #default="scope">
                ${{ formatCost(scope.row.totalCost) }}
              </template>
            </el-table-column>
            <el-table-column label="占比" width="80" align="right">
              <template #default="scope">
                {{ calculatePercentage(scope.row.totalCost) }}%
              </template>
            </el-table-column>
          </el-table>
          <div v-if="modelStats.length === 0 && !loading" class="empty-table">
            <p>暂无模型使用数据</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import {
  TrendCharts,
  Box,
  Wallet,
  Coin,
  DataLine,
  PieChart
} from '@element-plus/icons-vue'
import { statisticsAPI } from '../../api'
import { useUserStore } from '../../stores/user'

// 动态导入 Chart.js 避免 SSR/编译问题
let Chart: any = null

const userStore = useUserStore()
const username = computed(() => userStore.user?.username || '用户')

// 时间维度选项
const timeOptions = [
  { value: 1, label: '当天' },
  { value: 7, label: '7天' },
  { value: 30, label: '30天' }
]

const selectedDays = ref(7)
const loading = ref(false)

// 统计数据
const stats = ref({
  totalCalls: 0,
  uniqueModels: 0,
  totalCost: 0,
  totalTokens: 0
})

// 趋势数据
const trendData = ref<Array<{ date: string; calls: number; cost: number }>>([])

// 模型统计
const modelStats = ref<Array<{
  model: string
  calls: number
  totalCost: number
  totalInputTokens: number
  totalOutputTokens: number
}>>([])

// Chart 实例
let trendChart: any = null
let modelChart: any = null
const trendChartRef = ref<HTMLCanvasElement | null>(null)
const modelChartRef = ref<HTMLCanvasElement | null>(null)

// 选择时间范围
const selectTimeRange = (days: number) => {
  selectedDays.value = days
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 加载趋势数据
    const trendRes = await statisticsAPI.getUsageTrend(selectedDays.value)
    if (trendRes.data) {
      trendData.value = trendRes.data.map((item: any) => ({
        date: item.date,
        calls: item.calls || 0,
        cost: parseFloat(item.cost) || 0
      })).sort((a: any, b: any) => new Date(a.date).getTime() - new Date(b.date).getTime())

      // 计算汇总统计
      stats.value.totalCalls = trendData.value.reduce((sum, item) => sum + item.calls, 0)
      stats.value.totalCost = trendData.value.reduce((sum, item) => sum + item.cost, 0)
    }

    // 加载模型统计
    const modelRes = await statisticsAPI.getModelUsage()
    if (modelRes.data) {
      modelStats.value = modelRes.data.map((item: any) => ({
        model: item.model,
        calls: item.calls || 0,
        totalCost: parseFloat(item.totalCost) || 0,
        totalInputTokens: item.totalInputTokens || 0,
        totalOutputTokens: item.totalOutputTokens || 0
      })).sort((a: any, b: any) => b.totalCost - a.totalCost)

      stats.value.uniqueModels = modelStats.value.length
      stats.value.totalTokens = modelStats.value.reduce(
        (sum, item) => sum + item.totalInputTokens + item.totalOutputTokens, 0
      )
    }

    // 更新图表
    await nextTick()
    updateTrendChart()
    updateModelChart()
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 更新趋势图表
const updateTrendChart = () => {
  if (!Chart || !trendChartRef.value || trendData.value.length === 0) return

  const ctx = trendChartRef.value.getContext('2d')
  if (!ctx) return

  // 销毁旧图表
  if (trendChart) {
    trendChart.destroy()
  }

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
          borderColor: '#667eea',
          backgroundColor: 'rgba(102, 126, 234, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          yAxisID: 'y'
        },
        {
          label: '费用 (USD)',
          data: trendData.value.map(item => item.cost),
          borderColor: '#f093fb',
          backgroundColor: 'rgba(240, 147, 251, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        legend: {
          position: 'top',
          labels: {
            usePointStyle: true,
            padding: 20
          }
        },
        tooltip: {
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          padding: 12,
          titleFont: { size: 14 },
          bodyFont: { size: 13 },
          callbacks: {
            label: function(context) {
              const label = context.dataset.label || ''
              const value = context.parsed.y
              if (label.includes('费用')) {
                return `${label}: $${value.toFixed(4)}`
              }
              return `${label}: ${value}`
            }
          }
        }
      },
      scales: {
        x: {
          grid: {
            display: false
          }
        },
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          title: {
            display: true,
            text: '请求数'
          },
          grid: {
            color: 'rgba(0, 0, 0, 0.05)'
          }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          title: {
            display: true,
            text: '费用 (USD)'
          },
          grid: {
            drawOnChartArea: false
          }
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

  // 销毁旧图表
  if (modelChart) {
    modelChart.destroy()
  }

  const colors = [
    '#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe',
    '#43e97b', '#38f9d7', '#fa709a', '#fee140', '#ff6b6b'
  ]

  // 取前10个模型
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
      plugins: {
        legend: {
          position: 'right',
          labels: {
            usePointStyle: true,
            padding: 15,
            font: { size: 11 },
            generateLabels: function(chart) {
              const data = chart.data
              if (data.labels && data.datasets.length) {
                return data.labels.map((label, i) => {
                  const value = data.datasets[0].data[i] as number
                  const shortLabel = (label as string).length > 20
                    ? (label as string).substring(0, 20) + '...'
                    : label
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
        },
        tooltip: {
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          padding: 12,
          callbacks: {
            label: function(context) {
              const value = context.parsed
              const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0)
              const percentage = ((value / total) * 100).toFixed(1)
              return `$${value.toFixed(4)} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

// 格式化函数
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

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const calculatePercentage = (cost: number) => {
  const total = modelStats.value.reduce((sum, item) => sum + item.totalCost, 0)
  if (total === 0) return '0.0'
  return ((cost / total) * 100).toFixed(1)
}

// 监听时间范围变化
watch(selectedDays, () => {
  if (Chart) {
    loadData()
  }
})

onMounted(async () => {
  // 动态导入 Chart.js
  try {
    const chartModule = await import('chart.js/auto')
    Chart = chartModule.Chart || chartModule.default
    loadData()
  } catch (error) {
    console.error('Failed to load Chart.js:', error)
  }
})

onUnmounted(() => {
  if (trendChart) trendChart.destroy()
  if (modelChart) modelChart.destroy()
})
</script>

<style scoped>
.overview-page {
  padding: 0;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: var(--spacing-6);
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

/* 时间选择器 */
.time-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-6);
}

.time-buttons {
  display: flex;
  gap: var(--spacing-2);
  background: var(--color-gray-100);
  padding: 4px;
  border-radius: var(--radius-lg);
}

.time-btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.time-btn:hover {
  color: var(--color-text-primary);
}

.time-btn.active {
  background: white;
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.last-update {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
}

.stat-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-fast);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl);
}

.stat-icon.requests {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon.models {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stat-icon.cost {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-icon.tokens {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 图表区域 */
.chart-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  margin-bottom: var(--spacing-6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--color-border-light);
}

.section-header {
  margin-bottom: var(--spacing-4);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.chart-container {
  height: 300px;
  position: relative;
}

.pie-container {
  height: 280px;
}

.empty-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-2);
}

/* 模型分析 */
.model-analysis {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: var(--spacing-6);
}

.model-chart-section,
.model-table-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--color-border-light);
}

.model-table-container {
  max-height: 320px;
  overflow-y: auto;
}

.model-name-cell {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  color: var(--color-primary);
}

.empty-table {
  text-align: center;
  padding: var(--spacing-8);
  color: var(--color-text-tertiary);
}

/* 响应式 */
@media (max-width: 1280px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .model-analysis {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .time-selector {
    flex-direction: column;
    gap: var(--spacing-3);
    align-items: flex-start;
  }

  .stat-card {
    padding: var(--spacing-4);
  }

  .chart-container {
    height: 250px;
  }
}
</style>

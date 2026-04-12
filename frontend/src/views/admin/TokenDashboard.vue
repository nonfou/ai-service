<template>
  <div class="token-dashboard-page">
    <PageHeader
      title="Token 仪表盘"
      description="查看最近窗口内的请求总览、Token 趋势和热点模型分布。"
    >
      <template #actions>
        <el-radio-group v-model="days" @change="handleDaysChange">
          <el-radio-button
            v-for="option in TOKEN_STATS_DAY_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="refreshDashboard">
          <RefreshRight />
          刷新
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="loadError"
      class="dashboard-alert"
      type="error"
      show-icon
      :closable="false"
      title="统计数据加载失败"
      :description="loadError"
    />

    <section class="summary-grid" v-loading="loading">
      <StatCard
        v-for="card in summaryCards"
        :key="card.key"
        :value="formatTokenStatValue(card.key, card.value)"
        :label="card.label"
        :description="card.detail || ''"
        :type="resolveCardMeta(card.key).type"
      >
        <template #icon>
          <component :is="resolveCardMeta(card.key).icon" />
        </template>
      </StatCard>
    </section>

    <section class="chart-stack">
      <EChartCard
        title="Token 使用趋势"
        description="总 Token 为主线，辅助展示输入与输出 Token 变化。"
        :loading="loading"
        :option="trendChartOption"
        empty-description="当前窗口暂无趋势数据"
        height="360px"
      />

      <div class="distribution-grid">
        <EChartCard
          title="模型分布"
          description="按总 Token 倒序展示最近窗口内最活跃的模型。"
          :loading="loading"
          :option="modelDistributionOption"
          empty-description="暂无模型分布数据"
          height="360px"
        />
        <EChartCard
          title="端点分布"
          description="帮助快速识别当前请求主要集中在哪些上游端点。"
          :loading="loading"
          :option="endpointDistributionOption"
          empty-description="暂无端点分布数据"
          height="360px"
        />
      </div>
    </section>

    <el-card class="recent-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="recent-card-header">
          <div>
            <h3 class="recent-card-title">最近请求</h3>
            <p class="recent-card-description">展示近 {{ days }} 天内最近 10 条调用记录。</p>
          </div>
          <el-button type="primary" link @click="goToUsage">
            查看全部
            <ArrowRight />
          </el-button>
        </div>
      </template>

      <el-table :data="recentRequests" stripe empty-text="暂无请求记录">
        <el-table-column label="时间" min-width="168">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="apiKeyName" label="API Key" min-width="120" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" min-width="150" show-overflow-tooltip />
        <el-table-column label="端点" min-width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.endpoint }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            {{ formatRequestType(row.requestType) }}
          </template>
        </el-table-column>
        <el-table-column label="总 Token" min-width="120" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" min-width="110">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ formatSuccessLabel(row.success) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import {
  ArrowRight,
  Coin,
  Clock,
  DataAnalysis,
  Download,
  RefreshRight,
  Upload,
  WarningFilled
} from '@element-plus/icons-vue'
import type { AdminTokenDashboard, TokenDistributionItem, TokenStatsSummaryCard, TokenTrendItem } from '../../api'
import { adminAPI } from '../../api'
import EChartCard from '../../components/EChartCard.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import message from '../../utils/message'
import {
  TOKEN_STATS_DAY_OPTIONS,
  formatDateTime,
  formatDuration,
  formatNumber,
  formatRequestType,
  formatSuccessLabel,
  formatTokenStatValue,
  normalizeTokenStatsDays
} from '../../utils/tokenStats'

type CardType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const route = useRoute()
const router = useRouter()

const days = ref<7 | 30>(normalizeTokenStatsDays(route.query.days))
const loading = ref(false)
const loadError = ref('')
const dashboard = ref<AdminTokenDashboard | null>(null)

const CARD_META: Record<string, { icon: Component; type: CardType }> = {
  requestCount: { icon: DataAnalysis, type: 'primary' },
  totalTokens: { icon: Coin, type: 'warning' },
  inputTokens: { icon: Upload, type: 'info' },
  outputTokens: { icon: Download, type: 'success' },
  averageDurationMs: { icon: Clock, type: 'info' },
  failureCount: { icon: WarningFilled, type: 'danger' }
}

const summaryCards = computed<TokenStatsSummaryCard[]>(() => dashboard.value?.summaryCards ?? [])
const recentRequests = computed(() => dashboard.value?.recentRequests ?? [])

const resolveCardMeta = (key: string) => CARD_META[key] || { icon: Coin, type: 'primary' as CardType }

const buildTrendChartOption = (items: TokenTrendItem[]): EChartsOption | null => {
  if (!items.length) {
    return null
  }

  return {
    color: ['#6366f1', '#3b82f6', '#10b981'],
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      bottom: 0
    },
    grid: {
      left: 20,
      right: 20,
      top: 24,
      bottom: 48,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: items.map(item => item.date.slice(5)),
      axisLine: {
        lineStyle: {
          color: '#cbd5e1'
        }
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    series: [
      {
        name: '总 Token',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: 'rgba(99, 102, 241, 0.12)'
        },
        data: items.map(item => item.totalTokens)
      },
      {
        name: '输入 Token',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: items.map(item => item.inputTokens)
      },
      {
        name: '输出 Token',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: items.map(item => item.outputTokens)
      }
    ]
  }
}

const buildDistributionOption = (items: TokenDistributionItem[], seriesName: string): EChartsOption | null => {
  const topItems = items.slice(0, 8)
  if (!topItems.length) {
    return null
  }

  const reversedItems = [...topItems].reverse()
  return {
    color: ['#4f46e5'],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: 16,
      right: 16,
      top: 20,
      bottom: 8,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: reversedItems.map(item => item.name),
      axisTick: {
        show: false
      }
    },
    series: [
      {
        name: seriesName,
        type: 'bar',
        barWidth: 18,
        itemStyle: {
          borderRadius: [0, 8, 8, 0]
        },
        data: reversedItems.map(item => item.totalTokens)
      }
    ]
  }
}

const trendChartOption = computed(() => buildTrendChartOption(dashboard.value?.trend ?? []))
const modelDistributionOption = computed(() =>
  buildDistributionOption(dashboard.value?.modelDistribution ?? [], '模型 Token')
)
const endpointDistributionOption = computed(() =>
  buildDistributionOption(dashboard.value?.endpointDistribution ?? [], '端点 Token')
)

const loadDashboard = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const res = await adminAPI.getTokenDashboard(days.value)
    dashboard.value = res.data
  } catch (error) {
    const fallbackMessage = error instanceof Error ? error.message : '请稍后重试'
    loadError.value = fallbackMessage || '请稍后重试'
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

const handleDaysChange = () => {
  void loadDashboard()
}

const refreshDashboard = async () => {
  await loadDashboard()
  if (!loadError.value) {
    message.success('统计数据已刷新')
  }
}

const goToUsage = () => {
  void router.push({
    name: 'TokenUsage',
    query: { days: String(days.value) }
  })
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped>
.token-dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-alert {
  margin-top: -8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.chart-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.distribution-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.recent-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.recent-card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.recent-card-description {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .distribution-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .recent-card-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

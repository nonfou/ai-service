<template>
  <div class="token-usage-page">
    <PageHeader
      title="Token 使用记录"
      description="按 API Key、模型、端点和状态筛选历史调用记录，并支持导出 CSV。"
    >
      <template #actions>
        <el-button :loading="loading" @click="refreshRecords">
          <RefreshRight />
          刷新
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="loadError"
      class="usage-alert"
      type="error"
      show-icon
      :closable="false"
      title="统计数据加载失败"
      :description="loadError"
    />

    <el-card class="filter-card" shadow="never">
      <div class="filter-grid">
        <div class="filter-item">
          <label class="filter-label">时间范围</label>
          <el-select v-model="filters.days" @change="handleSearch">
            <el-option
              v-for="option in TOKEN_STATS_DAY_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">API Key</label>
          <el-select v-model="filters.apiKeyId" clearable placeholder="全部密钥" @change="handleSearch">
            <el-option
              v-for="option in filterOptions.apiKeys"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">端点</label>
          <el-select v-model="filters.endpoint" clearable placeholder="全部端点" @change="handleSearch">
            <el-option
              v-for="option in filterOptions.endpoints"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">模型关键字</label>
          <el-input
            v-model="filters.model"
            clearable
            placeholder="支持模糊搜索"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">状态</label>
          <el-select v-model="filters.success" @change="handleSearch">
            <el-option label="全部状态" value="all" />
            <el-option label="成功" value="true" />
            <el-option label="失败" value="false" />
          </el-select>
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :loading="exporting" @click="handleExport">
            <Download />
            导出 CSV
          </el-button>
        </div>
      </div>
    </el-card>

    <section class="summary-grid" v-loading="loading">
      <StatCard
        v-for="card in summaryCards"
        :key="card.key"
        :value="card.displayValue"
        :label="card.label"
        :description="card.description"
        :type="card.type"
      >
        <template #icon>
          <component :is="card.icon" />
        </template>
      </StatCard>
    </section>

    <el-card class="table-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="table-card-header">
          <div>
            <h3 class="table-card-title">请求明细</h3>
            <p class="table-card-description">
              当前筛选共 {{ formatNumber(pagination.total) }} 条记录，按时间倒序展示。
            </p>
          </div>
        </div>
      </template>

      <el-table
        :data="records"
        stripe
        class="usage-table"
        empty-text="当前筛选条件下暂无请求记录"
      >
        <el-table-column label="时间" min-width="172" fixed="left">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="apiKeyName" label="API Key" min-width="120" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" min-width="160" show-overflow-tooltip />

        <el-table-column label="端点" min-width="132">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.endpoint }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.requestType === 'stream' ? 'primary' : 'info'">
              {{ formatRequestType(row.requestType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ formatSuccessLabel(row.success) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="输入 Token" min-width="118" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.inputTokens) }}
          </template>
        </el-table-column>

        <el-table-column label="输出 Token" min-width="118" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.outputTokens) }}
          </template>
        </el-table-column>

        <el-table-column label="缓存读" min-width="108" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.cacheReadTokens) }}
          </template>
        </el-table-column>

        <el-table-column label="缓存写" min-width="108" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.cacheWriteTokens) }}
          </template>
        </el-table-column>

        <el-table-column label="总 Token" min-width="118" align="right">
          <template #default="{ row }">
            <strong class="total-token-value">{{ formatNumber(row.totalTokens) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="首 Token 耗时" min-width="126">
          <template #default="{ row }">
            {{ formatDuration(row.firstTokenLatencyMs) }}
          </template>
        </el-table-column>

        <el-table-column label="总耗时" min-width="110">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>

        <el-table-column label="User-Agent" min-width="240">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.userAgent"
              :content="row.userAgent"
              placement="top"
              effect="dark"
            >
              <span class="truncate-text">{{ row.userAgent }}</span>
            </el-tooltip>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="错误信息" min-width="220">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.errorMessage"
              :content="row.errorMessage"
              placement="top"
              effect="dark"
            >
              <span class="truncate-text error-text">{{ row.errorMessage }}</span>
            </el-tooltip>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRoute } from 'vue-router'
import {
  Clock,
  Coin,
  DataAnalysis,
  Download,
  RefreshRight,
  Upload,
  WarningFilled
} from '@element-plus/icons-vue'
import type { AdminTokenUsageQuery, AdminTokenUsageRecordPage, TokenStatsFilterOptions } from '../../api'
import { adminAPI } from '../../api'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import message from '../../utils/message'
import {
  TOKEN_STATS_DAY_OPTIONS,
  downloadBlobFile,
  extractDownloadFileName,
  formatDateTime,
  formatDuration,
  formatNumber,
  formatRequestType,
  formatSuccessLabel,
  formatTokenStatValue,
  normalizeTokenStatsDays
} from '../../utils/tokenStats'

type CardType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
type SuccessFilterValue = 'all' | 'true' | 'false'

interface SummaryCardViewModel {
  key: string
  label: string
  displayValue: string
  description: string
  icon: Component
  type: CardType
}

const route = useRoute()

const loading = ref(false)
const exporting = ref(false)
const loadError = ref('')
const pageData = ref<AdminTokenUsageRecordPage | null>(null)

const filters = reactive({
  days: normalizeTokenStatsDays(route.query.days),
  apiKeyId: '',
  endpoint: '',
  model: '',
  success: 'all' as SuccessFilterValue
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

const CARD_META: Record<string, { icon: Component; type: CardType }> = {
  requestCount: { icon: DataAnalysis, type: 'primary' },
  totalTokens: { icon: Coin, type: 'warning' },
  inputTokens: { icon: Upload, type: 'info' },
  outputTokens: { icon: Download, type: 'success' },
  averageDurationMs: { icon: Clock, type: 'info' },
  failureCount: { icon: WarningFilled, type: 'danger' }
}

const summaryCards = computed<SummaryCardViewModel[]>(() => {
  const summary = pageData.value?.summary

  const items = [
    {
      key: 'requestCount',
      label: '总请求数',
      value: summary?.requestCount ?? 0,
      description: `近 ${filters.days} 天命中的记录数`
    },
    {
      key: 'totalTokens',
      label: '总 Token',
      value: summary?.totalTokens ?? 0,
      description: '输入、输出与缓存 Token 合计'
    },
    {
      key: 'inputTokens',
      label: '输入 Token',
      value: summary?.inputTokens ?? 0,
      description: '筛选结果内累计输入 Token'
    },
    {
      key: 'outputTokens',
      label: '输出 Token',
      value: summary?.outputTokens ?? 0,
      description: '筛选结果内累计输出 Token'
    },
    {
      key: 'averageDurationMs',
      label: '平均耗时',
      value: summary?.averageDurationMs ?? 0,
      description: '筛选结果中每次请求的平均总耗时'
    },
    {
      key: 'failureCount',
      label: '失败请求数',
      value: summary?.failureCount ?? 0,
      description: `成功 ${formatNumber(summary?.successCount ?? 0)} 次 / 失败 ${formatNumber(summary?.failureCount ?? 0)} 次`
    }
  ]

  return items.map(item => ({
    key: item.key,
    label: item.label,
    displayValue: formatTokenStatValue(item.key, item.value),
    description: item.description,
    icon: CARD_META[item.key]?.icon || Coin,
    type: CARD_META[item.key]?.type || 'primary'
  }))
})

const records = computed(() => pageData.value?.records ?? [])
const filterOptions = computed<TokenStatsFilterOptions>(() => pageData.value?.filters ?? {
  days: filters.days,
  apiKeys: [],
  endpoints: []
})

const buildQuery = (includePaging: boolean): AdminTokenUsageQuery => {
  const query: AdminTokenUsageQuery = {
    days: filters.days,
    apiKeyId: filters.apiKeyId || undefined,
    endpoint: filters.endpoint || undefined,
    model: filters.model.trim() || undefined,
    success: filters.success === 'all' ? undefined : filters.success === 'true'
  }

  if (includePaging) {
    query.page = pagination.page
    query.pageSize = pagination.pageSize
  }

  return query
}

const loadRecords = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const res = await adminAPI.getTokenUsageRecords(buildQuery(true))
    pageData.value = res.data
    pagination.total = res.data.pagination.total
    pagination.totalPages = res.data.pagination.totalPages
    pagination.page = res.data.pagination.page
    pagination.pageSize = res.data.pagination.pageSize
  } catch (error) {
    const fallbackMessage = error instanceof Error ? error.message : '请稍后重试'
    loadError.value = fallbackMessage || '请稍后重试'
    pageData.value = null
    pagination.total = 0
    pagination.totalPages = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pagination.page = 1
  await loadRecords()
}

const resetFilters = async () => {
  filters.days = normalizeTokenStatsDays(route.query.days)
  filters.apiKeyId = ''
  filters.endpoint = ''
  filters.model = ''
  filters.success = 'all'
  pagination.page = 1
  pagination.pageSize = 20
  await loadRecords()
}

const handlePageChange = async (page: number) => {
  pagination.page = page
  await loadRecords()
}

const handlePageSizeChange = async (pageSize: number) => {
  pagination.page = 1
  pagination.pageSize = pageSize
  await loadRecords()
}

const refreshRecords = async () => {
  await loadRecords()
  if (!loadError.value) {
    message.success('使用记录已刷新')
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const response = await adminAPI.exportTokenUsageRecords(buildQuery(false))
    const fallbackName = `token-usage-records-${filters.days}d.csv`
    const fileName = extractDownloadFileName(response.headers as unknown as Record<string, unknown>, fallbackName)
    downloadBlobFile(response.data, fileName)
    message.success('CSV 导出成功')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  void loadRecords()
})
</script>

<style scoped>
.token-usage-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.usage-alert {
  margin-top: -8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.filter-card,
.table-card {
  border: 1px solid var(--border-light);
}

.filter-card {
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.table-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.table-card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.table-card-description {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.usage-table :deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

.total-token-value {
  font-weight: 700;
  color: var(--primary-color);
}

.truncate-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.error-text {
  color: var(--danger-color);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 1400px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-card {
    position: static;
  }

  .filter-actions {
    flex-wrap: wrap;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}
</style>

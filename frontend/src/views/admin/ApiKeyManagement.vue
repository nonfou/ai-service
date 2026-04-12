<template>
  <div class="api-key-management-page">
    <PageHeader title="Copilot API Key 管理" description="每个 API Key 独立绑定一个 Copilot 服务地址和上游凭证">
      <template #actions>
        <el-button @click="loadApiKeys">刷新列表</el-button>
        <el-button :loading="usageSyncing" @click="syncUsagePreviews">同步使用进度</el-button>
        <el-button type="primary" @click="openCreateDialog">新增 API Key</el-button>
      </template>
    </PageHeader>

    <el-alert type="info" :closable="false" show-icon>
      <template #title>
        每个 API Key 都需要单独填写对应的 Copilot 服务地址。客户端调用时请在
        <code>Authorization: Bearer sk-xxx</code> 中传入下方创建的 API Key。
      </template>
    </el-alert>

    <section class="panel">
      <el-table :data="apiKeys" v-loading="loading" style="width: 100%">
        <el-table-column prop="keyName" label="名称" min-width="180">
          <template #default="{ row }">
            <div class="key-name-cell">
              <span>{{ row.keyName }}</span>
              <span v-if="row.description" class="key-name-desc">{{ row.description }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="apiKey" label="客户端 API Key" min-width="220">
          <template #default="{ row }">
            <code class="code-text">{{ row.apiKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="relayBaseUrl" label="Copilot 地址" min-width="260" />
        <el-table-column label="套餐" width="150">
          <template #default="{ row }">
            {{ usagePreviewMap[row.id]?.plan || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="重置日期" width="120">
          <template #default="{ row }">
            {{ usagePreviewMap[row.id]?.resetDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="高级交互进度" min-width="220">
          <template #default="{ row }">
            <div v-if="usagePreviewMap[row.id]?.loading" class="usage-inline-muted">同步中</div>
            <div v-else-if="usagePreviewMap[row.id]?.premiumUsedPercent !== null" class="usage-inline-progress">
              <el-progress
                :percentage="usagePreviewMap[row.id]?.premiumUsedPercent || 0"
                :status="usageProgressStatus(usagePreviewMap[row.id]?.premiumUsedPercent || null)"
                :stroke-width="10"
              />
              <span class="usage-inline-meta">
                {{ usagePreviewMap[row.id]?.premiumUsed || '-' }} / {{ usagePreviewMap[row.id]?.premiumEntitlement || '-' }}
              </span>
            </div>
            <div v-else class="usage-inline-muted">-</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="primary" @click="showUsage(row)">使用进度</el-button>
              <el-button
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button text type="primary" @click="regenerateKey(row)">重置</el-button>
              <el-button text type="danger" @click="deleteKey(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑 API Key 路由' : '新增 API Key 路由'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="form.keyName" placeholder="例如：生产环境 Copilot" />
        </el-form-item>
        <el-form-item label="Copilot 转发地址">
          <el-input v-model="form.relayBaseUrl" placeholder="例如：http://127.0.0.1:4141/v1" />
        </el-form-item>
        <el-form-item :label="isEdit ? '上游 API Key（留空则不修改）' : '上游 API Key'">
          <el-input
            v-model="form.upstreamApiKey"
            type="password"
            show-password
            :placeholder="isEdit ? '不修改可留空' : '输入对应 Copilot 服务凭证'"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="可选，用于区分环境或业务线"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="newKeyDialogVisible"
      :title="newKeyDialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>完整客户端 API Key 只展示这一次，请立即保存。</template>
      </el-alert>
      <div class="new-key-box">
        <code>{{ latestFullKey }}</code>
      </div>
      <div class="new-key-actions">
        <el-button type="primary" @click="copyLatestKey">复制密钥</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeNewKeyDialog">我已保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="usageDialogVisible"
      title="使用进度"
      width="680px"
      :close-on-click-modal="false"
    >
      <div class="usage-dialog">
        <div class="usage-title">{{ usageDialogTitle }}</div>
        <section class="usage-summary-grid">
          <div class="usage-summary-card">
            <span class="usage-label">账号</span>
            <strong>{{ usageSummary.login }}</strong>
          </div>
          <div class="usage-summary-card">
            <span class="usage-label">套餐</span>
            <strong>{{ usageSummary.plan }}</strong>
          </div>
          <div class="usage-summary-card">
            <span class="usage-label">重置日期</span>
            <strong>{{ usageSummary.resetDate }}</strong>
          </div>
          <div class="usage-summary-card">
            <span class="usage-label">API 端点</span>
            <strong class="usage-endpoint">{{ usageSummary.apiEndpoint }}</strong>
          </div>
        </section>
        <section v-if="usageQuotaCards.length" class="usage-quotas">
          <div
            v-for="quota in usageQuotaCards"
            :key="quota.key"
            class="quota-card"
          >
            <div class="quota-header">
              <div>
                <div class="quota-title">{{ quota.label }}</div>
                <div class="quota-meta">
                  已使用 {{ quota.used }} / 总量 {{ quota.entitlement }}
                </div>
              </div>
              <el-tag :type="quota.unlimited ? 'success' : 'info'" size="small">
                {{ quota.unlimited ? '不限量' : '限量' }}
              </el-tag>
            </div>
            <el-progress
              :percentage="quota.percentUsed ?? 0"
              :status="usageProgressStatus(quota.percentUsed)"
              :stroke-width="12"
            />
            <div class="quota-footnote">
              {{ quota.overagePermitted ? '支持超额' : '不支持超额' }}
            </div>
          </div>
        </section>
        <pre class="usage-json">{{ usageJson }}</pre>
      </div>
      <template #footer>
        <el-button type="primary" @click="usageDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { adminAPI, type AdminApiKey, type AdminApiKeyUsage } from '../../api'
import message from '../../utils/message'
import PageHeader from '../../components/PageHeader.vue'

const loading = ref(false)
const usageSyncing = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const newKeyDialogVisible = ref(false)
const newKeyDialogTitle = ref('API Key 创建成功')
const latestFullKey = ref('')
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const usageDialogVisible = ref(false)
const usageDialogTitle = ref('')
const usageJson = ref('')
const usageSummary = ref({
  login: '-',
  plan: '-',
  resetDate: '-',
  apiEndpoint: '-'
})
const usageQuotaCards = ref<Array<{
  key: string
  label: string
  percentUsed: number | null
  used: string
  entitlement: string
  unlimited: boolean
  overagePermitted: boolean
}>>([])
const usagePreviewMap = ref<Record<string, {
  loading: boolean
  plan: string
  resetDate: string
  premiumUsedPercent: number | null
  premiumUsed: string
  premiumEntitlement: string
}>>({})

const apiKeys = ref<AdminApiKey[]>([])

const form = reactive({
  keyName: '',
  relayBaseUrl: '',
  upstreamApiKey: '',
  description: ''
})

const resetForm = () => {
  editingId.value = null
  form.keyName = ''
  form.relayBaseUrl = ''
  form.upstreamApiKey = ''
  form.description = ''
}

const loadApiKeys = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getAdminApiKeys()
    apiKeys.value = res.data
    syncUsagePreviewState(res.data)
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载 API Key 列表失败')
  } finally {
    loading.value = false
  }
}

const syncUsagePreviewState = (rows: AdminApiKey[]) => {
  const nextMap: Record<string, {
    loading: boolean
    plan: string
    resetDate: string
    premiumUsedPercent: number | null
    premiumUsed: string
    premiumEntitlement: string
  }> = {}

  for (const row of rows) {
    nextMap[row.id] = usagePreviewMap.value[row.id] || {
      loading: false,
      plan: '-',
      resetDate: '-',
      premiumUsedPercent: null,
      premiumUsed: '-',
      premiumEntitlement: '-'
    }
  }

  usagePreviewMap.value = nextMap
}

const openCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: AdminApiKey) => {
  isEdit.value = true
  editingId.value = row.id
  form.keyName = row.keyName
  form.relayBaseUrl = row.relayBaseUrl || ''
  form.upstreamApiKey = ''
  form.description = row.description || ''
  dialogVisible.value = true
}

const validateForm = () => {
  if (!form.keyName.trim()) {
    message.warning('请输入名称')
    return false
  }
  if (!form.relayBaseUrl.trim()) {
    message.warning('请输入 Copilot 转发地址')
    return false
  }
  return true
}

const submitForm = async () => {
  if (!validateForm()) {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      const payload: Record<string, string> = {
        keyName: form.keyName.trim(),
        relayBaseUrl: form.relayBaseUrl.trim(),
        description: form.description.trim()
      }
      if (form.upstreamApiKey.trim()) {
        payload.upstreamApiKey = form.upstreamApiKey.trim()
      }
      await adminAPI.updateAdminApiKey(editingId.value, payload)
      message.success('保存成功')
    } else {
      const res = await adminAPI.createAdminApiKey({
        keyName: form.keyName.trim(),
        relayBaseUrl: form.relayBaseUrl.trim(),
        upstreamApiKey: form.upstreamApiKey.trim(),
        description: form.description.trim()
      })
      latestFullKey.value = res.data.apiKey
      newKeyDialogTitle.value = 'API Key 创建成功'
      newKeyDialogVisible.value = true
      message.success('创建成功')
    }

    dialogVisible.value = false
    resetForm()
    await loadApiKeys()
  } catch (error: any) {
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row: AdminApiKey) => {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await adminAPI.updateAdminApiKeyStatus(row.id, nextStatus)
    message.success(nextStatus === 1 ? 'API Key 已启用' : 'API Key 已禁用')
    await loadApiKeys()
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新状态失败')
  }
}

const regenerateKey = async (row: AdminApiKey) => {
  try {
    await ElMessageBox.confirm(
      '重置后旧密钥将立即失效，是否继续？',
      '重置 API Key',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await adminAPI.regenerateAdminApiKey(row.id)
    latestFullKey.value = res.data.apiKey
    newKeyDialogTitle.value = 'API Key 重置成功'
    newKeyDialogVisible.value = true
    await loadApiKeys()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || '重置 API Key 失败')
    }
  }
}

const deleteKey = async (row: AdminApiKey) => {
  try {
    await ElMessageBox.confirm(
      '删除后该 API Key 将无法再用于 Copilot 转发，是否继续？',
      '删除 API Key',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await adminAPI.deleteAdminApiKey(row.id)
    message.success('API Key 已删除')
    await loadApiKeys()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || '删除 API Key 失败')
    }
  }
}

const showUsage = async (row: AdminApiKey) => {
  try {
    const res = await adminAPI.getAdminApiKeyUsage(row.id)
    const data = res.data as AdminApiKeyUsage
    applyUsagePreview(row.id, data)
    usageDialogTitle.value = `${row.keyName} 使用进度`
    usageSummary.value = {
      login: readText(data, 'login'),
      plan: readText(data, 'copilot_plan'),
      resetDate: readText(data, 'quota_reset_date'),
      apiEndpoint: readNestedText(data, ['endpoints', 'api'])
    }
    usageQuotaCards.value = buildQuotaCards(data)
    usageJson.value = JSON.stringify(data, null, 2)
    usageDialogVisible.value = true
  } catch (error: any) {
    message.error(error.response?.data?.message || '获取使用进度失败')
  }
}

const loadUsagePreviews = async (rows: AdminApiKey[]) => {
  const tasks = rows.map(async (row) => {
    usagePreviewMap.value[row.id] = {
      loading: true,
      plan: usagePreviewMap.value[row.id]?.plan || '-',
      resetDate: usagePreviewMap.value[row.id]?.resetDate || '-',
      premiumUsedPercent: usagePreviewMap.value[row.id]?.premiumUsedPercent ?? null,
      premiumUsed: usagePreviewMap.value[row.id]?.premiumUsed || '-',
      premiumEntitlement: usagePreviewMap.value[row.id]?.premiumEntitlement || '-'
    }

    try {
      const res = await adminAPI.getAdminApiKeyUsage(row.id)
      applyUsagePreview(row.id, res.data as AdminApiKeyUsage)
    } catch {
      usagePreviewMap.value[row.id] = {
        loading: false,
        plan: '-',
        resetDate: '-',
        premiumUsedPercent: null,
        premiumUsed: '-',
        premiumEntitlement: '-'
      }
    }
  })

  await Promise.allSettled(tasks)
}

const syncUsagePreviews = async (showSuccess = true) => {
  if (!apiKeys.value.length) {
    return
  }

  usageSyncing.value = true
  try {
    await loadUsagePreviews(apiKeys.value)
    if (showSuccess) {
      message.success('使用进度已同步')
    }
  } finally {
    usageSyncing.value = false
  }
}

const applyUsagePreview = (keyId: string, data: AdminApiKeyUsage) => {
  const premium = readQuotaSnapshot(data, 'premium_interactions')
  usagePreviewMap.value[keyId] = {
    loading: false,
    plan: readText(data, 'copilot_plan'),
    resetDate: readText(data, 'quota_reset_date'),
    premiumUsedPercent: premium.percentUsed,
    premiumUsed: premium.used,
    premiumEntitlement: premium.entitlement
  }
}

const fallbackCopyText = (value: string) => {
  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  try {
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
}

const copyLatestKey = async () => {
  if (!latestFullKey.value) return

  try {
    if (navigator.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(latestFullKey.value)
    } else if (!fallbackCopyText(latestFullKey.value)) {
      throw new Error('fallback copy failed')
    }

    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动选中文本复制')
  }
}

const closeNewKeyDialog = () => {
  newKeyDialogVisible.value = false
  latestFullKey.value = ''
}

const readText = (value: Record<string, unknown>, key: string) => {
  const raw = value[key]
  return raw === null || raw === undefined || raw === '' ? '-' : String(raw)
}

const readNestedText = (value: Record<string, unknown>, path: string[]) => {
  let current: unknown = value
  for (const key of path) {
    if (!current || typeof current !== 'object' || !(key in current)) {
      return '-'
    }
    current = (current as Record<string, unknown>)[key]
  }
  return current === null || current === undefined || current === '' ? '-' : String(current)
}

const formatQuotaValue = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : value.toFixed(1)
  }
  return String(value)
}

const formatQuotaLabel = (key: string) => {
  const mapping: Record<string, string> = {
    premium_interactions: '高级交互',
    completions: '补全',
    chat: '对话'
  }
  return mapping[key] || key
}

const buildQuotaCards = (data: AdminApiKeyUsage) => {
  const snapshots = data.quota_snapshots
  if (!snapshots || typeof snapshots !== 'object') {
    return []
  }

  return Object.entries(snapshots as Record<string, unknown>).map(([key, raw]) => {
    const snapshot = (raw && typeof raw === 'object' ? raw : {}) as Record<string, unknown>
    const quota = parseQuotaSnapshot(snapshot)

    return {
      key,
      label: formatQuotaLabel(key),
      percentUsed: quota.percentUsed,
      used: quota.used,
      entitlement: quota.entitlement,
      unlimited: Boolean(snapshot.unlimited),
      overagePermitted: Boolean(snapshot.overage_permitted)
    }
  })
}

const parseQuotaNumber = (value: unknown) => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

const clampPercent = (value: number) => Math.max(0, Math.min(100, value))

const parseQuotaSnapshot = (snapshot: Record<string, unknown>) => {
  const entitlementValue = parseQuotaNumber(snapshot.entitlement)
  const remainingValue = parseQuotaNumber(snapshot.quota_remaining ?? snapshot.remaining)
  const percentRemainingValue = parseQuotaNumber(snapshot.percent_remaining)

  let usedValue: number | null = null
  if (entitlementValue !== null && remainingValue !== null) {
    usedValue = Math.max(0, entitlementValue - remainingValue)
  }

  let percentUsed: number | null = null
  if (entitlementValue !== null && entitlementValue > 0 && usedValue !== null) {
    percentUsed = clampPercent((usedValue / entitlementValue) * 100)
  } else if (percentRemainingValue !== null) {
    percentUsed = clampPercent(100 - percentRemainingValue)
  }

  return {
    percentUsed,
    used: formatQuotaValue(usedValue ?? (snapshot.used ?? '-')),
    entitlement: formatQuotaValue(snapshot.entitlement)
  }
}

const readQuotaSnapshot = (data: AdminApiKeyUsage, key: string) => {
  const snapshots = data.quota_snapshots
  const snapshot = snapshots && typeof snapshots === 'object'
    ? (snapshots as Record<string, unknown>)[key]
    : null
  const safeSnapshot = (snapshot && typeof snapshot === 'object' ? snapshot : {}) as Record<string, unknown>
  return parseQuotaSnapshot(safeSnapshot)
}

const usageProgressStatus = (percent: number | null) => {
  if (percent === null) return undefined
  if (percent >= 80) return 'exception'
  if (percent >= 50) return 'warning'
  return 'success'
}

onMounted(() => {
  loadApiKeys().then(() => syncUsagePreviews(false))
})
</script>

<style scoped>
.api-key-management-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.code-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: var(--text-primary);
}

.key-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.key-name-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.new-key-box {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  word-break: break-all;
}

.new-key-box code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  color: var(--text-primary);
}

.new-key-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.usage-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.usage-title {
  font-weight: 600;
  color: var(--text-primary);
}

.usage-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.usage-summary-card,
.quota-card {
  padding: 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.usage-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.usage-summary-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.5;
}

.usage-endpoint {
  word-break: break-all;
}

.usage-quotas {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.usage-inline-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.usage-inline-meta,
.usage-inline-muted {
  font-size: 12px;
  color: var(--text-secondary);
}

.quota-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.quota-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.quota-meta,
.quota-footnote {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.usage-json {
  margin: 0;
  padding: 16px;
  max-height: 420px;
  overflow: auto;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .action-buttons {
    flex-wrap: wrap;
  }

  .usage-summary-grid,
  .usage-quotas {
    grid-template-columns: 1fr;
  }
}
</style>

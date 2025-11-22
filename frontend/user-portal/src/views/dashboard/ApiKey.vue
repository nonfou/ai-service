<template>
  <div class="api-key-page">
    <!-- 页面头部 -->
    <div class="page-header-section">
      <div class="header-text">
        <h2 class="page-title">API 密钥</h2>
        <p class="page-subtitle">管理您的 API 密钥</p>
        <p class="page-description">创建和管理用于程序访问您账户的 API 密钥。</p>
      </div>
      <router-link to="/api-keys/create" class="create-btn">
        <el-icon><Plus /></el-icon>
        创建 API 密钥
      </router-link>
    </div>

    <!-- 安全提醒 -->
    <div class="security-alert">
      <el-icon class="alert-icon"><Warning /></el-icon>
      <div class="alert-content">
        <h3 class="alert-title">安全提醒</h3>
        <p class="alert-text">
          请妥善保管您的 API 密钥,不要在公开场所分享。如果密钥泄露,请立即删除并重新创建。
        </p>
      </div>
    </div>

    <!-- API Keys 表格 -->
    <div class="table-card">
      <el-table
        :data="apiKeys"
        style="width: 100%"
        :header-cell-style="headerCellStyle"
        :cell-style="cellStyle"
      >
        <el-table-column prop="keyName" label="名称" min-width="150">
          <template #default="{ row }">
            <div class="key-name-cell">
              {{ row.keyName }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="apiKey" label="密钥" min-width="300">
          <template #default="{ row }">
            <div class="key-value-cell">
              <code class="api-key-code">{{ maskApiKey(row.apiKey) }}</code>
              <el-button
                text
                class="copy-btn"
                @click="copyToClipboard(row.apiKey)"
              >
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'info'"
              size="small"
            >
              {{ row.status === 1 ? '活跃' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="lastUsed" label="最后使用" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.lastUsed) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.status === 1"
                text
                type="primary"
                size="small"
                @click="handleToggleStatus(row)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                text
                type="success"
                size="small"
                @click="handleToggleStatus(row)"
              >
                启用
              </el-button>
              <el-button
                text
                type="danger"
                size="small"
                @click="handleDelete(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 使用提示 -->
    <div class="usage-tip-card">
      <h4 class="tip-title">如何使用 API 密钥</h4>
      <p class="tip-text">完整教程与安装指引已迁移至"快速开始"。</p>
      <router-link to="/getting-started" class="tip-link">
        前往快速开始
      </router-link>
    </div>

    <!-- 创建密钥对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="创建 API 密钥"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="密钥名称">
          <el-input
            v-model="form.keyName"
            placeholder="例如: 生产环境密钥"
            size="large"
          />
          <div class="form-hint">为您的密钥起一个容易识别的名称</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleCreate" size="large">创建密钥</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新密钥成功对话框 -->
    <el-dialog
      v-model="newKeyDialogVisible"
      title="API 密钥创建成功"
      width="600px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="new-key-content">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
        >
          <template #title>
            <strong>请妥善保管您的密钥</strong>
          </template>
          <p>出于安全考虑,我们不会再次显示完整的密钥。请立即复制并保存到安全的地方。</p>
        </el-alert>

        <div class="new-key-display">
          <div class="new-key-label">您的新 API 密钥</div>
          <div class="new-key-value">
            <code>{{ newlyCreatedKey }}</code>
            <el-button
              type="primary"
              @click="copyToClipboard(newlyCreatedKey)"
              class="copy-new-key"
            >
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeNewKeyDialog" size="large">
          我已保存密钥
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  CopyDocument,
  Warning
} from '@element-plus/icons-vue'
import { apiKeyAPI, type ApiKey } from '../../api'

const apiKeys = ref<ApiKey[]>([])
const dialogVisible = ref(false)
const newKeyDialogVisible = ref(false)
const newlyCreatedKey = ref('')

const form = reactive({
  keyName: ''
})

// 表格样式
const headerCellStyle = {
  background: '#f9fafb',
  color: '#374151',
  fontWeight: '600',
  fontSize: '14px'
}

const cellStyle = {
  padding: '16px'
}

const loadApiKeys = async () => {
  try {
    const res = await apiKeyAPI.getApiKeys()
    apiKeys.value = res.data
  } catch (error) {
    ElMessage.error('加载API密钥失败')
  }
}

const handleCreate = async () => {
  if (!form.keyName.trim()) {
    ElMessage.warning('请输入密钥名称')
    return
  }

  try {
    const res = await apiKeyAPI.createApiKey({ keyName: form.keyName })
    dialogVisible.value = false

    // 展示新生成的密钥
    newlyCreatedKey.value = res.data?.apiKey ?? ''
    newKeyDialogVisible.value = true

    form.keyName = ''
    loadApiKeys()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const closeNewKeyDialog = () => {
  newKeyDialogVisible.value = false
  newlyCreatedKey.value = ''
}

const handleToggleStatus = async (key: ApiKey) => {
  const action = key.status === 1 ? '禁用' : '启用'
  try {
    // 调用禁用/启用API (根据您的后端API调整)
    // await apiKeyAPI.toggleStatus(key.id)
    ElMessage.success(`${action}成功`)
    loadApiKeys()
  } catch (error) {
    ElMessage.error(`${action}失败`)
  }
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm(
    '删除后将无法恢复,确定要删除这个 API 密钥吗?',
    '删除密钥',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await apiKeyAPI.deleteApiKey(id)
      ElMessage.success('密钥已删除')
      loadApiKeys()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // User cancelled
  })
}

const maskApiKey = (key: string) => {
  if (!key || key.length < 8) return key
  const visibleStart = key.substring(0, 8)
  const visibleEnd = key.substring(key.length - 8)
  return `${visibleStart}...${visibleEnd}`
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    // Fallback for browsers that don't support clipboard API
    const textArea = document.createElement('textarea')
    textArea.value = text
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      ElMessage.success('已复制到剪贴板')
    } catch (err) {
      ElMessage.error('复制失败')
    }
    document.body.removeChild(textArea)
  }
}

const formatDate = (dateString: string | Date) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadApiKeys()
})
</script>

<style scoped>
.api-key-page {
  padding: 0;
}

/* 页面头部 */
.page-header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-6);
  gap: var(--spacing-4);
}

.header-text {
  flex: 1;
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-1) 0;
}

.page-description {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin: 0;
}

.create-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-5);
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.create-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  color: white;
}

/* 安全提醒 */
.security-alert {
  display: flex;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  background: var(--color-warning-50);
  border: 1px solid var(--color-warning-500);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-6);
}

.alert-icon {
  flex-shrink: 0;
  font-size: var(--font-size-2xl);
  color: var(--color-warning-600);
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.alert-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
  line-height: var(--line-height-relaxed);
}

/* 表格卡片 */
.table-card {
  background: var(--color-bg-overlay);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  margin-bottom: var(--spacing-6);
}

/* 表格单元格样式 */
.key-name-cell {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.key-value-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.api-key-code {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  background: transparent;
  padding: 0;
  flex: 1;
}

.copy-btn {
  color: var(--color-text-tertiary);
  padding: var(--spacing-1);
  transition: color var(--transition-fast);
}

.copy-btn:hover {
  color: var(--color-primary);
}

.date-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-1);
}

/* 使用提示卡片 */
.usage-tip-card {
  background: var(--color-bg-overlay);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-2) 0;
}

.tip-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.tip-link {
  padding: var(--spacing-2) var(--spacing-4);
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.tip-link:hover {
  background: var(--color-primary-hover);
  color: white;
}

/* Dialog样式 */
.dialog-footer {
  display: flex;
  gap: var(--spacing-3);
  justify-content: flex-end;
}

.form-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin-top: var(--spacing-2);
}

/* 新密钥对话框 */
.new-key-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.new-key-display {
  background: var(--color-gray-50);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
}

.new-key-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-3);
  font-weight: var(--font-weight-medium);
}

.new-key-value {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.new-key-value code {
  flex: 1;
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  background: white;
  border: 1px solid var(--color-border-light);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-base);
  word-break: break-all;
  color: var(--color-text-primary);
}

.copy-new-key {
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-section {
    flex-direction: column;
    align-items: stretch;
  }

  .create-btn {
    width: 100%;
    justify-content: center;
  }

  .usage-tip-card {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-4);
  }

  .tip-link {
    width: 100%;
    text-align: center;
  }
}
</style>

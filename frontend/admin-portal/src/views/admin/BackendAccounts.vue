<template>
  <div class="backend-accounts-page">
    <!-- 页面头部 -->
    <PageHeader title="后端账户管理" description="管理 API 提供商账户和访问凭证">
      <template #actions>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          添加后端账户
        </el-button>
        <el-button @click="loadAccounts">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </PageHeader>

    <!-- 账户列表 -->
    <div class="table-container">
      <el-table :data="accounts" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountName" label="账户名称" min-width="150">
          <template #default="{ row }">
            <span class="account-name">{{ row.accountName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" width="120">
          <template #default="{ row }">
            <span class="provider-badge" :class="row.provider === 'copilot' ? 'provider-copilot' : 'provider-openrouter'">
              {{ row.provider === 'copilot' ? 'Copilot' : 'OpenRouter' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="accessToken" label="访问令牌" width="200">
          <template #default="{ row }">
            <span class="token-masked">{{ maskToken(row.accessToken) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" align="center" sortable />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="getStatusClass(row.status)">
              {{ statusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="errorCount" label="错误次数" width="100" align="center">
          <template #default="{ row }">
            <span class="error-count" :class="{ 'has-errors': row.errorCount > 0 }">
              {{ row.errorCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastUsedAt" label="最后使用" width="160">
          <template #default="{ row }">
            {{ formatDate(row.lastUsedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                :type="row.status === 'active' ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-button
                size="small"
                @click="handleHealthCheck(row)"
                :loading="healthCheckLoading[row.id]"
              >
                健康检查
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑后端账户' : '创建后端账户'"
      width="560px"
      class="account-dialog"
    >
      <el-form :model="accountForm" :rules="formRules" ref="formRef" label-width="110px">
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="accountForm.accountName" placeholder="例如: copilot-account-1" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="accountForm.provider" :disabled="isEdit" style="width: 100%">
            <el-option label="GitHub Copilot" value="copilot" />
            <el-option label="OpenRouter" value="openrouter" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问令牌" prop="accessToken">
          <el-input
            v-model="accountForm.accessToken"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改' : '输入 API Token'"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="accountForm.priority" :min="1" :max="100" />
          <div class="form-tip">数字越小优先级越高，建议1-10</div>
        </el-form-item>

        <el-divider content-position="left">
          <span class="divider-text">使用限额</span>
        </el-divider>

        <div class="limit-grid">
          <el-form-item label="每日限额">
            <el-input-number
              v-model="accountForm.dailyLimit"
              :precision="2"
              :step="100"
              :min="0"
              placeholder="不限制"
              style="width: 100%"
            />
            <div class="form-tip">0表示不限制</div>
          </el-form-item>
          <el-form-item label="每月限额">
            <el-input-number
              v-model="accountForm.monthlyLimit"
              :precision="2"
              :step="1000"
              :min="0"
              placeholder="不限制"
              style="width: 100%"
            />
            <div class="form-tip">0表示不限制</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { adminAPI, type BackendAccount, type CreateBackendAccountRequest, type UpdateBackendAccountRequest } from '../../api'
import message from '../../utils/message'
import PageHeader from '../../components/PageHeader.vue'

const loading = ref(false)
const accounts = ref<BackendAccount[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentAccountId = ref<number>(0)
const submitting = ref(false)
const healthCheckLoading = ref<Record<number, boolean>>({})

const formRef = ref<FormInstance>()
const accountForm = reactive<Partial<CreateBackendAccountRequest>>({
  accountName: '',
  provider: 'copilot',
  accessToken: '',
  priority: 1,
  dailyLimit: 0,
  monthlyLimit: 0
})

const formRules: FormRules = {
  accountName: [
    { required: true, message: '请输入账户名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '请选择提供商', trigger: 'change' }
  ],
  accessToken: [
    {
      validator: (rule, value, callback) => {
        if (!isEdit.value && !value) {
          callback(new Error('请输入访问令牌'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'blur' }
  ]
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getBackendAccounts()
    accounts.value = res.data
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载账户列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(accountForm, {
    accountName: '',
    provider: 'copilot',
    accessToken: '',
    priority: 1,
    dailyLimit: 0,
    monthlyLimit: 0
  })
  formRef.value?.clearValidate()
}

const handleEdit = (row: BackendAccount) => {
  isEdit.value = true
  currentAccountId.value = row.id
  Object.assign(accountForm, {
    accountName: row.accountName,
    provider: row.provider,
    accessToken: '',
    priority: row.priority,
    dailyLimit: row.dailyLimit || 0,
    monthlyLimit: row.monthlyLimit || 0
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        const updateData: UpdateBackendAccountRequest = {
          accountName: accountForm.accountName,
          priority: accountForm.priority,
          dailyLimit: accountForm.dailyLimit,
          monthlyLimit: accountForm.monthlyLimit
        }
        if (accountForm.accessToken) {
          updateData.accessToken = accountForm.accessToken
        }
        await adminAPI.updateBackendAccount(currentAccountId.value, updateData)
        message.success('更新成功')
      } else {
        await adminAPI.createBackendAccount(accountForm as CreateBackendAccountRequest)
        message.success('创建成功')
      }
      dialogVisible.value = false
      loadAccounts()
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleToggleStatus = async (row: BackendAccount) => {
  const enabled = row.status !== 'active'
  try {
    await adminAPI.toggleBackendAccount(row.id, enabled)
    message.success(enabled ? '已启用' : '已禁用')
    loadAccounts()
  } catch (error: any) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

const handleHealthCheck = async (row: BackendAccount) => {
  healthCheckLoading.value[row.id] = true
  try {
    const res = await adminAPI.healthCheckBackendAccount(row.id)
    if (res.data.healthy) {
      message.success('健康检查通过')
    } else {
      message.warning('健康检查失败')
    }
    loadAccounts()
  } catch (error: any) {
    message.error(error.response?.data?.message || '健康检查失败')
  } finally {
    healthCheckLoading.value[row.id] = false
  }
}

const handleDelete = async (row: BackendAccount) => {
  await ElMessageBox.confirm(
    `确定要删除账户 "${row.accountName}" 吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    await adminAPI.deleteBackendAccount(row.id)
    message.success('删除成功')
    loadAccounts()
  } catch (error: any) {
    message.error(error.response?.data?.message || '删除失败')
  }
}

const maskToken = (token: string) => {
  if (!token || token.length < 12) return token
  return token.substring(0, 8) + '***' + token.substring(token.length - 4)
}

const getStatusClass = (status: string) => {
  const classMap: Record<string, string> = {
    active: 'status-active',
    disabled: 'status-muted',
    error: 'status-inactive'
  }
  return classMap[status] || 'status-muted'
}

const statusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '正常',
    disabled: '已禁用',
    error: '异常'
  }
  return statusMap[status] || status
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.backend-accounts-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 表格容器 */
.table-container {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.account-name {
  font-weight: 600;
  color: var(--text-primary);
}

.token-masked {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: var(--text-muted);
}

/* 提供商徽章 */
.provider-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.provider-copilot {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.provider-openrouter {
  background: var(--success-bg);
  color: var(--success-color);
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: var(--success-bg);
  color: var(--success-color);
}

.status-inactive {
  background: var(--danger-bg);
  color: var(--danger-color);
}

.status-muted {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

.error-count {
  font-weight: 600;
  color: var(--text-secondary);
}

.error-count.has-errors {
  color: var(--danger-color);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

/* 对话框样式 */
.divider-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.limit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 24px;
}

.form-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

@media (max-width: 600px) {
  .limit-grid {
    grid-template-columns: 1fr;
  }
}
</style>

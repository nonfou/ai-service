<template>
  <div class="backend-accounts-container">
    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        添加后端账户
      </el-button>
      <el-button @click="loadAccounts">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </el-card>

    <!-- 账户列表 -->
    <el-card class="table-card">
      <el-table :data="accounts" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountName" label="账户名称" min-width="150" />
        <el-table-column prop="provider" label="提供商" width="120">
          <template #default="{ row }">
            <el-tag :type="row.provider === 'copilot' ? 'primary' : 'success'">
              {{ row.provider === 'copilot' ? 'Copilot' : 'OpenRouter' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessToken" label="访问令牌" width="200">
          <template #default="{ row }">
            <span style="font-family: monospace; font-size: 12px;">
              {{ maskToken(row.accessToken) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'active' ? 'success' : row.status === 'error' ? 'danger' : 'info'"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorCount" label="错误次数" width="100">
          <template #default="{ row }">
            <el-badge :value="row.errorCount" :type="row.errorCount > 0 ? 'danger' : 'success'">
              <span>{{ row.errorCount }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column prop="lastUsedAt" label="最后使用" width="160">
          <template #default="{ row }">
            {{ formatDate(row.lastUsedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 'active' ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="handleHealthCheck(row)"
              :loading="healthCheckLoading[row.id]"
            >
              健康检查
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑后端账户' : '创建后端账户'"
      width="600px"
    >
      <el-form :model="accountForm" :rules="formRules" ref="formRef" label-width="120px">
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
        <el-form-item label="每日限额">
          <el-input-number
            v-model="accountForm.dailyLimit"
            :precision="2"
            :step="100"
            :min="0"
            placeholder="不限制"
          />
          <div class="form-tip">每日最大使用金额（元），0表示不限制</div>
        </el-form-item>
        <el-form-item label="每月限额">
          <el-input-number
            v-model="accountForm.monthlyLimit"
            :precision="2"
            :step="1000"
            :min="0"
            placeholder="不限制"
          />
          <div class="form-tip">每月最大使用金额（元），0表示不限制</div>
        </el-form-item>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { adminAPI, type BackendAccount, type CreateBackendAccountRequest, type UpdateBackendAccountRequest } from '../../api'

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

// 加载账户列表
const loadAccounts = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getBackendAccounts()
    accounts.value = res.data
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载账户列表失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 重置表单
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

// 编辑账户
const handleEdit = (row: BackendAccount) => {
  isEdit.value = true
  currentAccountId.value = row.id
  Object.assign(accountForm, {
    accountName: row.accountName,
    provider: row.provider,
    accessToken: '', // 不显示原密码
    priority: row.priority,
    dailyLimit: row.dailyLimit || 0,
    monthlyLimit: row.monthlyLimit || 0
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        // 更新账户
        const updateData: UpdateBackendAccountRequest = {
          accountName: accountForm.accountName,
          priority: accountForm.priority,
          dailyLimit: accountForm.dailyLimit,
          monthlyLimit: accountForm.monthlyLimit
        }
        // 只有填写了新密码才更新
        if (accountForm.accessToken) {
          updateData.accessToken = accountForm.accessToken
        }
        await adminAPI.updateBackendAccount(currentAccountId.value, updateData)
        ElMessage.success('更新成功')
      } else {
        // 创建账户
        await adminAPI.createBackendAccount(accountForm as CreateBackendAccountRequest)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadAccounts()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 启用/禁用账户
const handleToggleStatus = async (row: BackendAccount) => {
  const enabled = row.status !== 'active'
  try {
    await adminAPI.toggleBackendAccount(row.id, enabled)
    ElMessage.success(enabled ? '已启用' : '已禁用')
    loadAccounts()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 健康检查
const handleHealthCheck = async (row: BackendAccount) => {
  healthCheckLoading.value[row.id] = true
  try {
    const res = await adminAPI.healthCheckBackendAccount(row.id)
    if (res.data.healthy) {
      ElMessage.success('健康检查通过')
    } else {
      ElMessage.warning('健康检查失败')
    }
    loadAccounts()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '健康检查失败')
  } finally {
    healthCheckLoading.value[row.id] = false
  }
}

// 删除账户
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
    ElMessage.success('删除成功')
    loadAccounts()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

// 脱敏显示令牌
const maskToken = (token: string) => {
  if (!token || token.length < 12) return token
  return token.substring(0, 8) + '***' + token.substring(token.length - 4)
}

// 状态文本
const statusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '正常',
    disabled: '已禁用',
    error: '异常'
  }
  return statusMap[status] || status
}

// 格式化日期
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
.backend-accounts-container {
  padding: 0;
}

.action-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>

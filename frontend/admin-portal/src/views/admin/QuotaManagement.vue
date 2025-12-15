<template>
  <div class="page-container">
    <PageHeader title="配额管理" description="管理用户的API调用配额限制" />

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="searchForm.userId"
        placeholder="输入用户ID"
        clearable
        @keyup.enter="handleSearch"
        style="width: 200px"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 用户列表（带配额信息） -->
    <div class="table-container">
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="每日配额" width="220">
          <template #default="{ row }">
            <div v-if="row.quota" class="quota-cell">
              <div class="quota-progress">
                <div class="quota-progress-bar" :style="{
                  width: getUsagePercentage(row.quota.currentDailyUsage, row.quota.dailyLimit) + '%',
                  backgroundColor: getProgressColor(row.quota.currentDailyUsage, row.quota.dailyLimit)
                }"></div>
              </div>
              <div class="quota-text">
                {{ row.quota.currentDailyUsage.toFixed(2) }} / {{ row.quota.dailyLimit.toFixed(2) }} 元
              </div>
            </div>
            <span v-else class="quota-not-set">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="每月配额" width="220">
          <template #default="{ row }">
            <div v-if="row.quota" class="quota-cell">
              <div class="quota-progress">
                <div class="quota-progress-bar" :style="{
                  width: getUsagePercentage(row.quota.currentMonthlyUsage, row.quota.monthlyLimit) + '%',
                  backgroundColor: getProgressColor(row.quota.currentMonthlyUsage, row.quota.monthlyLimit)
                }"></div>
              </div>
              <div class="quota-text">
                {{ row.quota.currentMonthlyUsage.toFixed(2) }} / {{ row.quota.monthlyLimit.toFixed(2) }} 元
              </div>
            </div>
            <span v-else class="quota-not-set">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span v-if="row.quota" :class="['status-badge', isQuotaExceeded(row.quota) ? 'status-danger' : 'status-active']">
              {{ isQuotaExceeded(row.quota) ? '超额' : '正常' }}
            </span>
            <span v-else class="status-badge status-muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="quota.lastResetDate" label="最后重置时间" width="140">
          <template #default="{ row }">
            <span class="date-text">{{ row.quota ? formatDate(row.quota.lastResetDate) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <button class="btn-action btn-primary" @click="handleEdit(row)">
                {{ row.quota ? '编辑' : '设置' }}
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 编辑配额对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="配额设置"
      width="480px"
      class="quota-dialog"
    >
      <div v-if="currentUser" class="user-info-card">
        <div class="user-info-row">
          <span class="info-label">用户ID</span>
          <span class="info-value">{{ currentUser.id }}</span>
        </div>
        <div class="user-info-row">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ currentUser.email }}</span>
        </div>
      </div>

      <el-form :model="quotaForm" :rules="formRules" ref="formRef" label-width="100px" class="quota-form">
        <el-form-item label="每日限额" prop="dailyLimit">
          <el-input-number
            v-model="quotaForm.dailyLimit"
            :precision="2"
            :step="10"
            :min="0"
            style="width: 100%"
          />
          <div class="form-tip">每日最大使用金额（元），0表示不限制</div>
        </el-form-item>
        <el-form-item label="每月限额" prop="monthlyLimit">
          <el-input-number
            v-model="quotaForm.monthlyLimit"
            :precision="2"
            :step="100"
            :min="0"
            style="width: 100%"
          />
          <div class="form-tip">每月最大使用金额（元），0表示不限制</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { adminAPI, type AdminUser, type UserQuota, type UpdateUserQuotaRequest } from '../../api'
import message from '../../utils/message'
import PageHeader from '../../components/PageHeader.vue'

interface UserWithQuota extends AdminUser {
  quota?: UserQuota
}

const loading = ref(false)
const users = ref<UserWithQuota[]>([])
const dialogVisible = ref(false)
const currentUser = ref<UserWithQuota | null>(null)
const submitting = ref(false)

const searchForm = reactive({
  userId: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formRef = ref<FormInstance>()
const quotaForm = reactive<UpdateUserQuotaRequest>({
  dailyLimit: 100,
  monthlyLimit: 2000
})

const formRules: FormRules = {
  dailyLimit: [
    { required: true, message: '请输入每日限额', trigger: 'blur' }
  ],
  monthlyLimit: [
    { required: true, message: '请输入每月限额', trigger: 'blur' }
  ]
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const userId = searchForm.userId || undefined
    const res = await adminAPI.getUsers(
      pagination.current,
      pagination.size,
      undefined,
      userId
    )

    const usersData = res.data.records
    pagination.total = res.data.total

    // 为每个用户加载配额信息
    const usersWithQuota = await Promise.all(
      usersData.map(async (user) => {
        try {
          const quotaRes = await adminAPI.getUserQuota(user.id)
          return {
            ...user,
            quota: quotaRes.data
          }
        } catch (error) {
          // 如果用户没有配额记录，返回没有配额的用户
          return {
            ...user,
            quota: undefined
          }
        }
      })
    )

    users.value = usersWithQuota
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadUsers()
}

// 重置
const handleReset = () => {
  searchForm.userId = ''
  pagination.current = 1
  loadUsers()
}

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadUsers()
}

// 当前页改变
const handleCurrentChange = (page: number) => {
  pagination.current = page
  loadUsers()
}

// 编辑配额
const handleEdit = (row: UserWithQuota) => {
  currentUser.value = row
  if (row.quota) {
    quotaForm.dailyLimit = row.quota.dailyLimit
    quotaForm.monthlyLimit = row.quota.monthlyLimit
  } else {
    quotaForm.dailyLimit = 100
    quotaForm.monthlyLimit = 2000
  }
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value || !currentUser.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await adminAPI.updateUserQuota(currentUser.value!.id, quotaForm)
      message.success('配额设置成功')
      dialogVisible.value = false
      loadUsers()
    } catch (error: any) {
      message.error(error.response?.data?.message || '设置失败')
    } finally {
      submitting.value = false
    }
  })
}

// 计算使用百分比
const getUsagePercentage = (usage: number, limit: number): number => {
  if (limit === 0) return 0
  const percentage = (usage / limit) * 100
  return Math.min(percentage, 100)
}

// 获取进度条颜色
const getProgressColor = (usage: number, limit: number): string => {
  const percentage = getUsagePercentage(usage, limit)
  if (percentage >= 100) return 'var(--color-error)'
  if (percentage >= 80) return 'var(--color-warning)'
  return 'var(--color-success)'
}

// 判断是否超额
const isQuotaExceeded = (quota: UserQuota): boolean => {
  return quota.currentDailyUsage >= quota.dailyLimit ||
         quota.currentMonthlyUsage >= quota.monthlyLimit
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.table-container {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-container :deep(.el-table) {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: var(--bg-secondary);
  --el-table-row-hover-bg-color: var(--bg-tertiary);
}

.table-container :deep(.el-table th.el-table__cell) {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid var(--border-light);
}

.table-container :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid var(--border-lighter);
}

/* 配额单元格样式 */
.quota-cell {
  padding: 4px 0;
}

.quota-progress {
  height: 6px;
  background: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.quota-progress-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.quota-text {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.quota-not-set {
  color: var(--text-muted);
  font-size: 13px;
}

.date-text {
  color: var(--text-muted);
  font-size: 13px;
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
}

.status-danger {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error);
}

.status-muted {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-action {
  padding: 5px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover {
  background: var(--color-primary-dark);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--border-light);
}

/* 对话框样式 */
.quota-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-light);
  padding: 16px 20px;
  margin: 0;
}

.quota-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.quota-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.user-info-card {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.user-info-row {
  display: flex;
  align-items: center;
  padding: 6px 0;
}

.user-info-row:first-child {
  padding-top: 0;
}

.user-info-row:last-child {
  padding-bottom: 0;
}

.info-label {
  width: 60px;
  color: var(--text-muted);
  font-size: 13px;
}

.info-value {
  color: var(--text-primary);
  font-weight: 500;
}

.quota-form {
  padding-top: 4px;
}

.form-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

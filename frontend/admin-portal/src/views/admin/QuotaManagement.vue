<template>
  <div class="quota-management-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input
            v-model="searchForm.userId"
            placeholder="输入用户ID"
            clearable
            @keyup.enter="handleSearch"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表（带配额信息） -->
    <el-card class="table-card">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="每日配额" width="200">
          <template #default="{ row }">
            <div v-if="row.quota">
              <el-progress
                :percentage="getUsagePercentage(row.quota.currentDailyUsage, row.quota.dailyLimit)"
                :color="getProgressColor(row.quota.currentDailyUsage, row.quota.dailyLimit)"
              />
              <div class="quota-text">
                {{ row.quota.currentDailyUsage.toFixed(2) }} / {{ row.quota.dailyLimit.toFixed(2) }} 元
              </div>
            </div>
            <span v-else class="no-quota">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="每月配额" width="200">
          <template #default="{ row }">
            <div v-if="row.quota">
              <el-progress
                :percentage="getUsagePercentage(row.quota.currentMonthlyUsage, row.quota.monthlyLimit)"
                :color="getProgressColor(row.quota.currentMonthlyUsage, row.quota.monthlyLimit)"
              />
              <div class="quota-text">
                {{ row.quota.currentMonthlyUsage.toFixed(2) }} / {{ row.quota.monthlyLimit.toFixed(2) }} 元
              </div>
            </div>
            <span v-else class="no-quota">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.quota" :type="isQuotaExceeded(row.quota) ? 'danger' : 'success'">
              {{ isQuotaExceeded(row.quota) ? '超额' : '正常' }}
            </el-tag>
            <el-tag v-else type="info">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quota.lastResetDate" label="最后重置时间" width="160">
          <template #default="{ row }">
            {{ row.quota ? formatDate(row.quota.lastResetDate) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              {{ row.quota ? '编辑配额' : '设置配额' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
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
    </el-card>

    <!-- 编辑配额对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="配额设置"
      width="500px"
    >
      <div v-if="currentUser" class="user-info">
        <p><strong>用户ID:</strong> {{ currentUser.id }}</p>
        <p><strong>邮箱:</strong> {{ currentUser.email }}</p>
      </div>

      <el-form :model="quotaForm" :rules="formRules" ref="formRef" label-width="120px">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { adminAPI, type AdminUser, type UserQuota, type UpdateUserQuotaRequest } from '../../api'
import message from '../../utils/message'

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
  if (percentage >= 100) return '#f56c6c'
  if (percentage >= 80) return '#e6a23c'
  return '#67c23a'
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
.quota-management-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.quota-text {
  font-size: 12px;
  color: #606266;
  margin-top: 5px;
  text-align: center;
}

.no-quota {
  color: #909399;
  font-size: 14px;
}

.user-info {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.user-info p {
  margin: 5px 0;
  color: #606266;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

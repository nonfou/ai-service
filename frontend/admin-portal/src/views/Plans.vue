<template>
  <div class="plans-page">
    <!-- 页面头部 -->
    <PageHeader title="套餐管理" description="管理用户订阅套餐和定价方案">
      <template #actions>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建套餐
        </el-button>
      </template>
    </PageHeader>

    <!-- 套餐列表 -->
    <div class="table-container">
      <el-table :data="plans" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="planName" label="套餐标识" width="150">
          <template #default="{ row }">
            <span class="plan-id">{{ row.planName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="displayName" label="显示名称" width="150">
          <template #default="{ row }">
            <span class="plan-name">{{ row.displayName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100" align="right">
          <template #default="{ row }">
            <span class="price-value">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quotaAmount" label="额度" width="100" align="right">
          <template #default="{ row }">
            <span class="quota-value">¥{{ row.quotaAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-inactive'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
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
      :title="editMode ? '编辑套餐' : '新建套餐'"
      width="560px"
      class="plan-dialog"
    >
      <el-form :model="planForm" label-width="100px">
        <el-form-item label="套餐标识">
          <el-input v-model="planForm.planName" placeholder="如: trial_card" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="planForm.displayName" placeholder="如: 体验卡" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="planForm.description" type="textarea" :rows="2" placeholder="套餐描述" />
        </el-form-item>

        <el-divider content-position="left">
          <span class="divider-text">价格设置</span>
        </el-divider>

        <div class="price-grid">
          <el-form-item label="原价">
            <el-input-number v-model="planForm.originalPrice" :precision="2" :step="10" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="现价">
            <el-input-number v-model="planForm.price" :precision="2" :step="10" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="额度">
            <el-input-number v-model="planForm.quotaAmount" :precision="2" :step="10" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="planForm.sortOrder" :min="0" style="width: 100%" />
          </el-form-item>
        </div>

        <el-divider content-position="left">
          <span class="divider-text">功能特性</span>
        </el-divider>

        <div class="features-editor">
          <div v-for="(_feature, index) in planForm.features" :key="index" class="feature-item">
            <el-input
              v-model="planForm.features![index]"
              placeholder="如: 30天有效期"
            />
            <el-button type="danger" size="small" @click="removeFeature(index)">
              删除
            </el-button>
          </div>
          <el-button type="primary" size="small" @click="addFeature" class="add-feature-btn">
            + 添加特性
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminAPI, type SubscriptionPlan } from '../api'
import message from '../utils/message'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const plans = ref<SubscriptionPlan[]>([])
const dialogVisible = ref(false)
const editMode = ref(false)
const currentPlanId = ref<number>(0)

const planForm = reactive<Partial<SubscriptionPlan>>({
  planName: '',
  displayName: '',
  description: '',
  originalPrice: 0,
  price: 0,
  quotaAmount: 0,
  sortOrder: 0,
  features: [],
  status: 1
})

const loadPlans = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getPlans()
    plans.value = res.data
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载套餐列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  editMode.value = false
  Object.assign(planForm, {
    planName: '',
    displayName: '',
    description: '',
    originalPrice: 0,
    price: 0,
    quotaAmount: 0,
    sortOrder: 0,
    features: [],
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: SubscriptionPlan) => {
  editMode.value = true
  currentPlanId.value = row.id
  Object.assign(planForm, {
    ...row,
    features: [...(row.features || [])]
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    const submitData = {
      ...planForm,
      features: planForm.features?.filter(f => f.trim() !== '') || []
    }
    if (editMode.value) {
      await adminAPI.updatePlan(currentPlanId.value, submitData)
      message.success('更新成功')
    } else {
      await adminAPI.createPlan(submitData as SubscriptionPlan)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadPlans()
  } catch (error: any) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

const addFeature = () => {
  if (!planForm.features) {
    planForm.features = []
  }
  planForm.features.push('')
}

const removeFeature = (index: number) => {
  planForm.features?.splice(index, 1)
}

const handleToggleStatus = async (row: SubscriptionPlan) => {
  try {
    await adminAPI.updatePlanStatus(row.id, { status: row.status === 1 ? 0 : 1 })
    message.success('状态更新成功')
    loadPlans()
  } catch (error: any) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

const handleDelete = (row: SubscriptionPlan) => {
  ElMessageBox.confirm('确定要删除该套餐吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await adminAPI.deletePlan(row.id)
      message.success('删除成功')
      loadPlans()
    } catch (error: any) {
      message.error(error.response?.data?.message || '删除失败')
    }
  })
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.plans-page {
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

.plan-id {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  color: var(--text-secondary);
}

.plan-name {
  font-weight: 600;
  color: var(--text-primary);
}

.price-value {
  color: var(--success-color);
  font-weight: 600;
}

.quota-value {
  color: var(--primary-color);
  font-weight: 500;
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

.price-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 24px;
}

/* 功能特性编辑器 */
.features-editor {
  width: 100%;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.feature-item .el-input {
  flex: 1;
}

.add-feature-btn {
  margin-top: 8px;
}

@media (max-width: 600px) {
  .price-grid {
    grid-template-columns: 1fr;
  }
}
</style>

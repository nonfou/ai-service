<template>
  <div class="plans-container">
    <el-card class="action-card">
      <el-button type="primary" @click="handleCreate">新建套餐</el-button>
    </el-card>

    <el-card class="table-card">
      <el-table :data="plans" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="planName" label="套餐标识" width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quotaAmount" label="额度" width="100">
          <template #default="{ row }">¥{{ row.quotaAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editMode ? '编辑套餐' : '新建套餐'"
      width="600px"
    >
      <el-form :model="planForm" label-width="100px">
        <el-form-item label="套餐标识">
          <el-input v-model="planForm.planName" placeholder="如: trial_card" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="planForm.displayName" placeholder="如: 体验卡" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="planForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="planForm.originalPrice" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="现价">
          <el-input-number v-model="planForm.price" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="额度">
          <el-input-number v-model="planForm.quotaAmount" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="planForm.sortOrder" :min="0" />
        </el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI, type SubscriptionPlan } from '../../api'

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
    ElMessage.error(error.response?.data?.message || '加载套餐列表失败')
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
  Object.assign(planForm, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (editMode.value) {
      await adminAPI.updatePlan(currentPlanId.value, planForm)
      ElMessage.success('更新成功')
    } else {
      await adminAPI.createPlan(planForm as SubscriptionPlan)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadPlans()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const handleToggleStatus = async (row: SubscriptionPlan) => {
  try {
    await adminAPI.updatePlanStatus(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态更新成功')
    loadPlans()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
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
      ElMessage.success('删除成功')
      loadPlans()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  })
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.plans-container {
  padding: 0;
}

.action-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}
</style>

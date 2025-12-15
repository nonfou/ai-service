<template>
  <div class="models-container">
    <el-card class="table-card">
      <el-table :data="models" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="modelName" label="模型名称" min-width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="provider" label="提供商" width="100" />
        <el-table-column prop="priceMultiplier" label="价格倍率" width="100">
          <template #default="{ row }">{{ row.priceMultiplier }}x</template>
        </el-table-column>
        <el-table-column label="输入价格" width="100">
          <template #default="{ row }">
            <span v-if="row.inputTokenPrice != null">${{ row.inputTokenPrice }}</span>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="输出价格" width="100">
          <template #default="{ row }">
            <span v-if="row.outputTokenPrice != null">${{ row.outputTokenPrice }}</span>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="缓存读取" width="100">
          <template #default="{ row }">
            <span v-if="row.cacheReadTokenPrice != null">${{ row.cacheReadTokenPrice }}</span>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="缓存写入" width="100">
          <template #default="{ row }">
            <span v-if="row.cacheWriteTokenPrice != null">${{ row.cacheWriteTokenPrice }}</span>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑模型"
      width="600px"
    >
      <el-form :model="modelForm" label-width="120px">
        <el-form-item label="模型名称">
          <el-input v-model="modelForm.modelName" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="modelForm.displayName" />
        </el-form-item>
        <el-form-item label="提供商">
          <el-input v-model="modelForm.provider" />
        </el-form-item>
        <el-form-item label="价格倍率">
          <el-input-number v-model="modelForm.priceMultiplier" :precision="2" :step="0.1" :min="0" />
        </el-form-item>
        <el-divider content-position="left">Token 价格配置（每百万 Token / USD）</el-divider>
        <el-form-item label="输入价格">
          <el-input-number
            v-model="modelForm.inputTokenPrice"
            :precision="4"
            :step="0.1"
            :min="0"
            placeholder="输入token价格"
          />
        </el-form-item>
        <el-form-item label="输出价格">
          <el-input-number
            v-model="modelForm.outputTokenPrice"
            :precision="4"
            :step="0.1"
            :min="0"
            placeholder="输出token价格"
          />
        </el-form-item>
        <el-form-item label="缓存读取价格">
          <el-input-number
            v-model="modelForm.cacheReadTokenPrice"
            :precision="4"
            :step="0.01"
            :min="0"
            placeholder="缓存读取价格（可选）"
          />
        </el-form-item>
        <el-form-item label="缓存写入价格">
          <el-input-number
            v-model="modelForm.cacheWriteTokenPrice"
            :precision="4"
            :step="0.01"
            :min="0"
            placeholder="缓存写入价格（可选）"
          />
        </el-form-item>
        <el-divider />
        <el-form-item label="描述">
          <el-input v-model="modelForm.description" type="textarea" :rows="3" />
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
import { adminAPI, type Model } from '../api'
import message from '../utils/message'

const loading = ref(false)
const models = ref<Model[]>([])
const dialogVisible = ref(false)
const currentModelId = ref<number>(0)

const modelForm = reactive<Partial<Model>>({
  modelName: '',
  displayName: '',
  provider: '',
  priceMultiplier: 1.0,
  inputTokenPrice: undefined,
  outputTokenPrice: undefined,
  cacheReadTokenPrice: undefined,
  cacheWriteTokenPrice: undefined,
  description: ''
})

const loadModels = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getModels()
    models.value = res.data
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载模型列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row: Model) => {
  currentModelId.value = row.id
  Object.assign(modelForm, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await adminAPI.updateModel(currentModelId.value, modelForm)
    message.success('更新成功')
    dialogVisible.value = false
    loadModels()
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败')
  }
}

const handleToggleStatus = async (row: Model) => {
  try {
    await adminAPI.updateModelStatus(row.id, row.status === 1 ? 0 : 1)
    message.success('状态更新成功')
    loadModels()
  } catch (error: any) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.models-container {
  padding: 0;
}

.table-card {
  margin-bottom: 20px;
}

.text-gray {
  color: #909399;
}
</style>

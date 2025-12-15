<template>
  <div class="models-page">
    <!-- 页面头部 -->
    <PageHeader title="模型管理" description="管理 AI 模型配置、定价和状态" />

    <!-- 模型列表 -->
    <div class="table-container">
      <el-table :data="models" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="modelName" label="模型名称" min-width="180">
          <template #default="{ row }">
            <span class="model-name">{{ row.modelName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="provider" label="提供商" width="100">
          <template #default="{ row }">
            <span class="provider-badge">{{ row.provider }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="priceMultiplier" label="价格倍率" width="100" align="center">
          <template #default="{ row }">
            <span class="multiplier-badge">{{ row.priceMultiplier }}x</span>
          </template>
        </el-table-column>
        <el-table-column label="输入价格" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.inputTokenPrice != null" class="price-value">${{ row.inputTokenPrice }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="输出价格" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.outputTokenPrice != null" class="price-value">${{ row.outputTokenPrice }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="缓存读取" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.cacheReadTokenPrice != null" class="price-value">${{ row.cacheReadTokenPrice }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="缓存写入" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.cacheWriteTokenPrice != null" class="price-value">${{ row.cacheWriteTokenPrice }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-inactive'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
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
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑模型"
      width="560px"
      class="model-dialog"
    >
      <el-form :model="modelForm" label-width="110px">
        <el-form-item label="模型名称">
          <el-input v-model="modelForm.modelName" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="modelForm.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="提供商">
          <el-input v-model="modelForm.provider" placeholder="请输入提供商" />
        </el-form-item>
        <el-form-item label="价格倍率">
          <el-input-number v-model="modelForm.priceMultiplier" :precision="2" :step="0.1" :min="0" />
        </el-form-item>

        <el-divider content-position="left">
          <span class="divider-text">Token 价格配置（每百万 Token / USD）</span>
        </el-divider>

        <div class="price-grid">
          <el-form-item label="输入价格">
            <el-input-number
              v-model="modelForm.inputTokenPrice"
              :precision="4"
              :step="0.1"
              :min="0"
              placeholder="输入token价格"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="输出价格">
            <el-input-number
              v-model="modelForm.outputTokenPrice"
              :precision="4"
              :step="0.1"
              :min="0"
              placeholder="输出token价格"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="缓存读取价格">
            <el-input-number
              v-model="modelForm.cacheReadTokenPrice"
              :precision="4"
              :step="0.01"
              :min="0"
              placeholder="可选"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="缓存写入价格">
            <el-input-number
              v-model="modelForm.cacheWriteTokenPrice"
              :precision="4"
              :step="0.01"
              :min="0"
              placeholder="可选"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-divider />

        <el-form-item label="描述">
          <el-input v-model="modelForm.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
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
import { adminAPI, type Model } from '../../api'
import message from '../../utils/message'
import PageHeader from '../../components/PageHeader.vue'

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
.models-page {
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

.model-name {
  font-weight: 600;
  color: var(--text-primary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.provider-badge {
  display: inline-flex;
  padding: 4px 10px;
  background: var(--primary-bg);
  color: var(--primary-color);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.multiplier-badge {
  display: inline-flex;
  padding: 4px 10px;
  background: var(--info-bg);
  color: var(--info-color);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.price-value {
  font-weight: 500;
  color: var(--text-primary);
}

.text-muted {
  color: var(--text-muted);
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

@media (max-width: 600px) {
  .price-grid {
    grid-template-columns: 1fr;
  }
}
</style>

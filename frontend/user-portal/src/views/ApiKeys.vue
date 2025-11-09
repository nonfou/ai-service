<template>
  <div class="api-keys-page">
    <div class="page-header">
      <h2>API密钥管理</h2>
      <el-button type="primary" @click="dialogVisible = true">创建密钥</el-button>
    </div>

    <el-table :data="apiKeys" stripe v-loading="loading">
      <el-table-column prop="keyName" label="名称" width="200" />
      <el-table-column prop="apiKey" label="密钥" width="400">
        <template #default="{ row }">
          <code>{{ row.apiKey }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建API密钥" width="500px">
      <el-form :model="form">
        <el-form-item label="密钥名称">
          <el-input v-model="form.keyName" placeholder="请输入密钥名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiKeyAPI, type ApiKey } from '../api'

const apiKeys = ref<ApiKey[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  keyName: ''
})

const loadApiKeys = async () => {
  loading.value = true
  try {
    const res = await apiKeyAPI.getApiKeys()
    apiKeys.value = res.data
  } catch (error) {
    ElMessage.error('加载API密钥失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!form.keyName.trim()) {
    ElMessage.warning('请输入密钥名称')
    return
  }

  try {
    await apiKeyAPI.createApiKey({ keyName: form.keyName })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    form.keyName = ''
    loadApiKeys()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该密钥吗?', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await apiKeyAPI.deleteApiKey(id)
      ElMessage.success('删除成功')
      loadApiKeys()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

onMounted(() => {
  loadApiKeys()
})
</script>

<style scoped>
.api-keys-page {
  padding: 2rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

code {
  font-family: 'Courier New', monospace;
  background: #f5f7fa;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 13px;
}
</style>

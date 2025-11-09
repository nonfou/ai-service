<template>
  <div class="models-page">
    <div class="page-header">
      <h1>AI模型列表</h1>
      <p>选择合适的AI模型,开始您的应用开发</p>
    </div>

    <div class="models-grid">
      <el-card v-for="model in models" :key="model.id" class="model-card" shadow="hover">
        <div class="model-header">
          <h3>{{ model.displayName }}</h3>
          <el-tag :type="model.status === 1 ? 'success' : 'danger'">
            {{ model.status === 1 ? '可用' : '不可用' }}
          </el-tag>
        </div>
        <div class="model-info">
          <p class="model-name">{{ model.modelName }}</p>
          <p class="provider">提供商: {{ model.provider }}</p>
          <p class="price">计费倍率: {{ model.priceMultiplier }}x</p>
          <p class="description">{{ model.description }}</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { modelsAPI, type Model } from '../api'

const models = ref<Model[]>([])

const loadModels = async () => {
  try {
    const res = await modelsAPI.getModels()
    models.value = res.data
  } catch (error: any) {
    ElMessage.error('加载模型列表失败')
  }
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.models-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
}

.page-header h1 {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.page-header p {
  color: #666;
  font-size: 1.1rem;
}

.models-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
}

.model-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.model-card:hover {
  transform: translateY(-4px);
}

.model-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.model-header h3 {
  margin: 0;
  font-size: 1.3rem;
}

.model-info p {
  margin: 0.5rem 0;
  color: #666;
}

.model-name {
  font-family: monospace;
  color: #409EFF !important;
}

.description {
  margin-top: 1rem !important;
  line-height: 1.6;
}
</style>

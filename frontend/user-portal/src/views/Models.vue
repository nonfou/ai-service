<template>
  <div class="models-page">
    <!-- 所有模型区域 -->
    <section class="all-models-section">
      <div class="section-header">
        <h2 class="section-title">所有模型</h2>
        <p class="section-subtitle">按提供商分组展示</p>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索模型名称、提供商或描述..."
          :prefix-icon="Search"
          clearable
          size="large"
          class="search-input"
        />
      </div>

      <!-- 按提供商分组展示 -->
      <div
        v-for="group in filteredGroupedModels"
        :key="group.provider"
        class="provider-group"
      >
        <div class="provider-header">
          <div class="provider-title-wrapper">
            <div class="provider-icon-large">
              <span>{{ group.provider.charAt(0) }}</span>
            </div>
            <div>
              <h3 class="provider-title">{{ group.provider }}</h3>
              <p class="provider-count">{{ group.models.length }} 个模型</p>
            </div>
          </div>
        </div>

        <!-- 该提供商的模型网格 -->
        <div class="models-grid">
          <div
            v-for="model in group.models"
            :key="model.id"
            class="model-card"
          >
            <div class="model-card-header">
              <div class="model-icon">
                <img v-if="model.icon" :src="model.icon" :alt="model.provider" />
                <div v-else class="icon-placeholder-small">{{ model.provider.charAt(0) }}</div>
              </div>
              <div class="model-title-group">
                <h3 class="model-title">{{ model.displayName }}</h3>
                <p class="model-name">{{ model.modelName }}</p>
              </div>
              <div class="model-tags-header" v-if="model.tags && model.tags.length > 0">
                <el-tag
                  v-for="tag in model.tags.slice(0, 2)"
                  :key="tag"
                  size="small"
                  class="tag-item"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>

            <p class="model-description">
              {{ model.description || '优质AI模型，提供强大的AI能力' }}
            </p>

            <div class="model-specs" v-if="model.contextLength || model.speed">
              <div class="spec-row" v-if="model.contextLength">
                <span class="spec-label">上下文长度：</span>
                <span class="spec-value">{{ formatContextLength(model.contextLength) }}</span>
              </div>
              <div class="spec-row" v-if="model.speed">
                <span class="spec-label">响应速度：</span>
                <span class="spec-value">{{ formatSpeed(model.speed) }}</span>
              </div>
            </div>

            <div class="model-pricing">
              <div class="pricing-title">Token 价格 (每百万)</div>
              <div class="pricing-grid">
                <div class="price-item">
                  <span class="price-label">输入</span>
                  <span class="price-value">{{ formatTokenPrice(model.inputTokenPrice) }}</span>
                </div>
                <div class="price-item">
                  <span class="price-label">输出</span>
                  <span class="price-value">{{ formatTokenPrice(model.outputTokenPrice) }}</span>
                </div>
                <div class="price-item">
                  <span class="price-label">缓存输入</span>
                  <span class="price-value">{{ formatTokenPrice(model.cacheReadTokenPrice) }}</span>
                </div>
                <div class="price-item">
                  <span class="price-label">缓存输出</span>
                  <span class="price-value">{{ formatTokenPrice(model.cacheWriteTokenPrice) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredGroupedModels.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p class="empty-text" v-if="searchQuery">未找到匹配的模型</p>
        <p class="empty-text" v-else>暂无可用模型</p>
        <p class="empty-hint" v-if="searchQuery">尝试使用其他关键词搜索</p>
        <p class="empty-hint" v-else>请稍后再试</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { modelsAPI, type Model } from '../api'

const models = ref<Model[]>([])
const searchQuery = ref('')

// 加载模型列表
const loadModels = async () => {
  try {
    const res = await modelsAPI.getModels()
    models.value = res.data
  } catch (error: any) {
    console.error('加载模型列表失败', error)
    ElMessage.error('加载模型列表失败')
    models.value = []
  }
}

// 需要隐藏的提供商列表
const hiddenProviders = ['xai', 'google']

// 按提供商分组的模型
const groupedModels = computed(() => {
  // 获取所有可用模型，排除隐藏的提供商
  const availableModels = models.value.filter(m =>
    m.status === 1 && !hiddenProviders.includes(m.provider.toLowerCase())
  )

  // 按提供商分组
  const groups = availableModels.reduce((acc, model) => {
    if (!acc[model.provider]) {
      acc[model.provider] = []
    }
    acc[model.provider]!.push(model)
    return acc
  }, {} as Record<string, Model[]>)

  // 转换为数组并排序
  return Object.entries(groups)
    .map(([provider, models]) => ({
      provider,
      models: models.sort((a, b) => a.id - b.id)
    }))
    .sort((a, b) => a.provider.localeCompare(b.provider))
})

// 根据搜索过滤后的分组模型
const filteredGroupedModels = computed(() => {
  if (!searchQuery.value.trim()) {
    return groupedModels.value
  }

  const query = searchQuery.value.toLowerCase().trim()

  return groupedModels.value
    .map(group => ({
      provider: group.provider,
      models: group.models.filter(model =>
        model.modelName.toLowerCase().includes(query) ||
        model.displayName.toLowerCase().includes(query) ||
        model.provider.toLowerCase().includes(query) ||
        (model.description && model.description.toLowerCase().includes(query))
      )
    }))
    .filter(group => group.models.length > 0)
})

// 格式化上下文长度
const formatContextLength = (length: number) => {
  if (length >= 1000000) {
    return `${(length / 1000000).toFixed(1)}M tokens`
  } else if (length >= 1000) {
    return `${(length / 1000).toFixed(0)}K tokens`
  }
  return `${length} tokens`
}

// 格式化速度
const formatSpeed = (speed: string) => {
  const speedMap: Record<string, string> = {
    'fast': '快速',
    'medium': '中等',
    'slow': '较慢'
  }
  return speedMap[speed] || speed
}

// 格式化 Token 价格（每百万 token 的美元价格）
const formatTokenPrice = (price: number | undefined | null) => {
  if (price === undefined || price === null) {
    return '-'
  }
  // 根据价格大小决定显示精度
  if (price >= 1) {
    return `$${price.toFixed(2)}`
  } else if (price >= 0.01) {
    return `$${price.toFixed(3)}`
  } else {
    return `$${price.toFixed(4)}`
  }
}


onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.models-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f8ff 0%, #ffffff 100%);
}

/* ==================== Section 通用样式 ==================== */
.all-models-section {
  padding: 5rem 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 3rem;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 1rem 0;
}

.section-subtitle {
  font-size: 1.1rem;
  color: #64748b;
  margin: 0;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  max-width: 500px;
  margin: 0 auto 3rem;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 1rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.8);
  padding: 0.25rem 0.5rem;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(124, 58, 237, 0.3);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.tag-item {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
  border: none !important;
  font-weight: 500;
}

/* ==================== 提供商分组 ==================== */
.provider-group {
  margin-bottom: 2.5rem;
}

.provider-header {
  margin-bottom: 1.25rem;
}

.provider-title-wrapper {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.provider-icon-large {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  border-radius: 0.75rem;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2);
}

.provider-icon-large span {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
}

.provider-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.15rem 0;
}

.provider-count {
  font-size: 0.85rem;
  color: #64748b;
  margin: 0;
}

/* ==================== 所有模型网格 ==================== */
.models-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.model-card {
  background: white;
  border-radius: 0.875rem;
  padding: 1rem;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  transition: all 0.2s ease;
  cursor: pointer;
}

.model-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  border-color: rgba(124, 58, 237, 0.2);
}

.model-card-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.model-tags-header {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
  margin-left: auto;
}

.model-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f7f9ff, #f1f0ff);
  border-radius: 0.5rem;
  overflow: hidden;
}

.model-icon img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.icon-placeholder-small {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  font-weight: 700;
  color: #7c3aed;
}

.model-title-group {
  flex: 1;
  min-width: 0;
}

.model-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.15rem 0;
}

.model-name {
  font-family: 'Courier New', monospace;
  font-size: 0.7rem;
  color: #7c3aed;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-description {
  color: #64748b;
  font-size: 0.8rem;
  line-height: 1.5;
  margin: 0 0 0.75rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.model-specs {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
  padding: 0.6rem;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 0.5rem;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
}

.spec-label {
  color: #64748b;
  font-weight: 500;
}

.spec-value {
  color: #1e293b;
  font-weight: 600;
}

/* ==================== 模型价格区域 ==================== */
.model-pricing {
  padding: 0.6rem;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05), rgba(37, 99, 235, 0.05));
  border-radius: 0.5rem;
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.pricing-title {
  font-size: 0.65rem;
  color: #7c3aed;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
}

.pricing-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.25rem 0.5rem;
}

.price-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.2rem 0;
}

.price-label {
  font-size: 0.7rem;
  color: #64748b;
}

.price-value {
  font-size: 0.75rem;
  font-weight: 600;
  color: #1e293b;
  font-family: 'Courier New', monospace;
}

/* ==================== 空状态 ==================== */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: #475569;
  margin: 0 0 0.5rem 0;
}

.empty-hint {
  font-size: 0.95rem;
  color: #64748b;
  margin: 0;
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 1280px) {
  .models-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .models-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .section-title {
    font-size: 2rem;
  }

  .models-grid {
    grid-template-columns: 1fr;
  }

  .pricing-grid {
    grid-template-columns: 1fr 1fr;
  }

  .search-bar {
    max-width: 100%;
  }
}

@media (max-width: 480px) {
  .all-models-section {
    padding: 3rem 1rem;
  }
}
</style>

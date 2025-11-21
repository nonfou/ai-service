<template>
  <div class="models-page">
    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-icon">✨</span>
          <span>AI 模型市场</span>
        </div>
        <h1 class="hero-title">丰富的 AI 模型库</h1>
        <p class="hero-description">
          集成 OpenAI、Anthropic、Google 等主流 AI 厂商的最新模型<br/>
          为开发者提供统一 API 接口，支持文本、视觉、代码等多种能力，一键调用即可集成到你的应用中
        </p>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-number">{{ models.length }}+</div>
            <div class="stat-label">可用模型</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ groupedModels.length }}</div>
            <div class="stat-label">顶级厂商</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">99.9%</div>
            <div class="stat-label">服务可用性</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 精选模型区域 -->
    <section class="featured-section" v-if="featuredModels.length > 0">
      <div class="section-header">
        <h2 class="section-title">精选模型</h2>
        <p class="section-subtitle">开发者最常用的高性能 AI 模型</p>
      </div>

      <div class="featured-grid">
        <div
          v-for="model in featuredModels"
          :key="model.id"
          class="featured-card"
        >
          <div class="featured-tags-top" v-if="model.tags && model.tags.length > 0">
            <el-tag
              v-for="tag in model.tags.slice(0, 2)"
              :key="tag"
              size="small"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div class="featured-header">
            <span class="provider-badge">{{ model.provider }}</span>
          </div>
          <h3 class="featured-title">{{ model.displayName }}</h3>
          <p class="featured-model-name">{{ model.modelName }}</p>
          <p class="featured-description">{{ model.description || '强大的AI模型，适用于各种应用场景' }}</p>

          <div class="featured-specs">
            <div class="spec-item" v-if="model.contextLength">
              <span class="spec-icon">📊</span>
              <span class="spec-text">{{ formatContextLength(model.contextLength) }}</span>
            </div>
            <div class="spec-item" v-if="model.speed">
              <span class="spec-icon">⚡</span>
              <span class="spec-text">{{ formatSpeed(model.speed) }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-icon">💰</span>
              <span class="spec-text">{{ model.priceMultiplier }}x 倍率</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 所有模型区域 -->
    <section class="all-models-section">
      <div class="section-header">
        <h2 class="section-title">所有模型</h2>
        <p class="section-subtitle">按提供商分组展示</p>
      </div>

      <!-- 按提供商分组展示 -->
      <div
        v-for="group in groupedModels"
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

            <div class="model-specs">
              <div class="spec-row" v-if="model.contextLength">
                <span class="spec-label">上下文长度：</span>
                <span class="spec-value">{{ formatContextLength(model.contextLength) }}</span>
              </div>
              <div class="spec-row" v-if="model.speed">
                <span class="spec-label">响应速度：</span>
                <span class="spec-value">{{ formatSpeed(model.speed) }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">计费倍率：</span>
                <span class="spec-value">{{ model.priceMultiplier }}x</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="groupedModels.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p class="empty-text">暂无可用模型</p>
        <p class="empty-hint">请稍后再试</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { modelsAPI, type Model } from '../api'

const models = ref<Model[]>([])

// Mock 数据（用于演示效果）
const mockModels: Model[] = [
  {
    id: 1,
    modelName: 'gpt-4-turbo',
    displayName: 'GPT-4 Turbo',
    provider: 'OpenAI',
    priceMultiplier: 1.5,
    status: 1,
    description: '最新的GPT-4模型，更快的响应速度，128K上下文窗口，支持视觉理解和函数调用',
    contextLength: 128000,
    speed: 'fast',
    tags: ['推荐', '视觉理解', '函数调用']
  },
  {
    id: 2,
    modelName: 'gpt-4',
    displayName: 'GPT-4',
    provider: 'OpenAI',
    priceMultiplier: 2.0,
    status: 1,
    description: 'OpenAI最强大的模型，适合复杂推理和创作任务',
    contextLength: 8192,
    speed: 'medium',
    tags: ['强大', '复杂推理']
  },
  {
    id: 3,
    modelName: 'gpt-3.5-turbo',
    displayName: 'GPT-3.5 Turbo',
    provider: 'OpenAI',
    priceMultiplier: 0.5,
    status: 1,
    description: '性价比极高的模型，适合大多数日常应用场景',
    contextLength: 16385,
    speed: 'fast',
    tags: ['低价', '高性价比']
  },
  {
    id: 4,
    modelName: 'claude-3-opus-20240229',
    displayName: 'Claude 3 Opus',
    provider: 'Anthropic',
    priceMultiplier: 2.5,
    status: 1,
    description: 'Anthropic最强大的模型，擅长复杂分析和创意写作，200K上下文',
    contextLength: 200000,
    speed: 'medium',
    tags: ['长上下文', '视觉理解', '多语言']
  },
  {
    id: 5,
    modelName: 'claude-3-sonnet-20240229',
    displayName: 'Claude 3 Sonnet',
    provider: 'Anthropic',
    priceMultiplier: 1.2,
    status: 1,
    description: '平衡性能和成本的优秀选择，适合企业级应用',
    contextLength: 200000,
    speed: 'fast',
    tags: ['长上下文', '视觉理解', '多语言']
  },
  {
    id: 6,
    modelName: 'claude-3-haiku-20240307',
    displayName: 'Claude 3 Haiku',
    provider: 'Anthropic',
    priceMultiplier: 0.4,
    status: 1,
    description: '最快速的Claude模型，适合高并发场景',
    contextLength: 200000,
    speed: 'fast',
    tags: ['低价', '快速', '高并发']
  },
  {
    id: 7,
    modelName: 'gemini-pro',
    displayName: 'Gemini Pro',
    provider: 'Google',
    priceMultiplier: 1.0,
    status: 1,
    description: 'Google先进的多模态AI模型，支持文本和图像理解',
    contextLength: 32768,
    speed: 'fast',
    tags: ['视觉理解', '代码生成', '多语言']
  },
  {
    id: 8,
    modelName: 'gemini-pro-vision',
    displayName: 'Gemini Pro Vision',
    provider: 'Google',
    priceMultiplier: 1.3,
    status: 1,
    description: '专为视觉理解优化的Gemini模型',
    contextLength: 16384,
    speed: 'medium',
    tags: ['视觉理解', '图像分析']
  },
  {
    id: 9,
    modelName: 'llama-3-70b',
    displayName: 'Llama 3 70B',
    provider: 'Meta',
    priceMultiplier: 0.8,
    status: 1,
    description: 'Meta开源的大语言模型，性能强劲且成本低廉',
    contextLength: 8192,
    speed: 'fast',
    tags: ['开源', '代码生成', '多语言']
  },
  {
    id: 10,
    modelName: 'llama-3-8b',
    displayName: 'Llama 3 8B',
    provider: 'Meta',
    priceMultiplier: 0.3,
    status: 1,
    description: '轻量级的Llama模型，适合资源受限场景',
    contextLength: 8192,
    speed: 'fast',
    tags: ['低价', '轻量级']
  },
  {
    id: 11,
    modelName: 'mistral-large',
    displayName: 'Mistral Large',
    provider: 'Mistral AI',
    priceMultiplier: 1.5,
    status: 1,
    description: 'Mistral AI的旗舰模型，多语言能力出色',
    contextLength: 32768,
    speed: 'fast',
    tags: ['多语言', '代码生成', '函数调用']
  },
  {
    id: 12,
    modelName: 'mistral-medium',
    displayName: 'Mistral Medium',
    provider: 'Mistral AI',
    priceMultiplier: 0.9,
    status: 1,
    description: '性价比优秀的中等规模模型',
    contextLength: 32768,
    speed: 'fast',
    tags: ['性价比', '多语言']
  }
]

// 加载模型列表
const loadModels = async () => {
  try {
    const res = await modelsAPI.getModels()
    models.value = res.data
  } catch (error: any) {
    console.error('加载模型列表失败', error)
    ElMessage.error('加载模型列表失败')
  }
}

// 精选模型（按 ID 排序，取前3个可用模型）
const featuredModels = computed(() => {
  return models.value
    .filter(m => m.status === 1)
    .sort((a, b) => a.id - b.id)
    .slice(0, 3)
})

// 按提供商分组的模型
const groupedModels = computed(() => {
  // 获取所有可用模型
  const availableModels = models.value.filter(m => m.status === 1)

  // 按提供商分组
  const groups = availableModels.reduce((acc, model) => {
    if (!acc[model.provider]) {
      acc[model.provider] = []
    }
    acc[model.provider].push(model)
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


onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.models-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f8ff 0%, #ffffff 100%);
}

/* ==================== Hero 区域 ==================== */
.hero-section {
  background: linear-gradient(135deg, #f7f9ff 0%, #f1f0ff 100%);
  padding: 5rem 2rem;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 40%;
  height: 200%;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.hero-content {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 2rem;
  font-size: 0.9rem;
  color: #7c3aed;
  font-weight: 500;
  margin-bottom: 2rem;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.1);
}

.badge-icon {
  font-size: 1.1rem;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin: 0 0 1.5rem 0;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.hero-description {
  font-size: 1.2rem;
  color: #64748b;
  line-height: 1.8;
  margin-bottom: 3rem;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 4rem;
  margin-top: 3rem;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.95rem;
  color: #64748b;
  font-weight: 500;
}

/* ==================== Section 通用样式 ==================== */
.featured-section,
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

/* ==================== 精选模型卡片 ==================== */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
  margin-bottom: 2rem;
}

.featured-card {
  background: #ffffff;
  border-radius: 1.5rem;
  padding: 2.5rem;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.featured-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #7c3aed, #2563eb);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.featured-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.15);
  border-color: rgba(124, 58, 237, 0.3);
}

.featured-card:hover::before {
  opacity: 1;
}

.featured-tags-top {
  position: absolute;
  top: 2rem;
  right: 2rem;
  display: flex;
  gap: 0.5rem;
  z-index: 2;
}

.featured-header {
  margin-bottom: 1.5rem;
}

.provider-badge {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1), rgba(37, 99, 235, 0.1));
  color: #7c3aed;
  border-radius: 0.75rem;
  font-size: 0.9rem;
  font-weight: 600;
  border: 1px solid rgba(124, 58, 237, 0.2);
}

.featured-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

.featured-model-name {
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  color: #7c3aed;
  margin: 0 0 1rem 0;
  font-weight: 500;
}

.featured-description {
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 1.5rem 0;
  min-height: 3rem;
}

.featured-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  background: rgba(124, 58, 237, 0.05);
  border-radius: 0.75rem;
  font-size: 0.85rem;
  color: #475569;
}

.spec-icon {
  font-size: 1rem;
}

.tag-item {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
  border: none !important;
  font-weight: 500;
}

/* ==================== 提供商分组 ==================== */
.provider-group {
  margin-bottom: 4rem;
}

.provider-header {
  margin-bottom: 2rem;
}

.provider-title-wrapper {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.provider-icon-large {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  border-radius: 1.25rem;
  box-shadow: 0 8px 24px rgba(124, 58, 237, 0.25);
}

.provider-icon-large span {
  font-size: 2.5rem;
  font-weight: 700;
  color: white;
}

.provider-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.25rem 0;
}

.provider-count {
  font-size: 1rem;
  color: #64748b;
  margin: 0;
}

/* ==================== 所有模型网格 ==================== */
.models-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.model-card {
  background: white;
  border-radius: 1.25rem;
  padding: 1.75rem;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  transition: all 0.25s ease;
  cursor: pointer;
}

.model-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
  border-color: rgba(124, 58, 237, 0.2);
}

.model-card-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.model-tags-header {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-left: auto;
}

.model-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f7f9ff, #f1f0ff);
  border-radius: 0.75rem;
  overflow: hidden;
}

.model-icon img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.icon-placeholder-small {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  color: #7c3aed;
}

.model-title-group {
  flex: 1;
  min-width: 0;
}

.model-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.25rem 0;
}

.model-name {
  font-family: 'Courier New', monospace;
  font-size: 0.8rem;
  color: #7c3aed;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-description {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.model-specs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 1rem;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 0.75rem;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.spec-label {
  color: #64748b;
  font-weight: 500;
}

.spec-value {
  color: #1e293b;
  font-weight: 600;
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
@media (max-width: 1024px) {
  .hero-title {
    font-size: 2.75rem;
  }

  .hero-stats {
    gap: 2rem;
  }

  .featured-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .models-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 3rem 1.5rem;
  }

  .hero-title {
    font-size: 2rem;
  }

  .hero-description {
    font-size: 1rem;
  }

  .hero-stats {
    flex-direction: column;
    gap: 1.5rem;
  }

  .section-title {
    font-size: 2rem;
  }

  .featured-grid {
    grid-template-columns: 1fr;
  }

  .models-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    gap: 1rem;
  }

  .filter-item {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
  }

  .provider-select {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .featured-section,
  .all-models-section {
    padding: 3rem 1rem;
  }

  .hero-title {
    font-size: 1.75rem;
  }

  .stat-number {
    font-size: 2rem;
  }
}
</style>

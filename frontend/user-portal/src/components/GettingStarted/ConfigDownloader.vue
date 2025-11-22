<template>
  <div class="config-downloader">
    <div class="config-downloader__grid">
      <div
        v-for="config in configs"
        :key="config.type"
        class="config-card"
        :class="`config-card--${config.type}`"
      >
        <div class="config-card__header">
          <div class="config-card__logo" :class="`config-card__logo--${config.type}`">
            {{ config.logoText }}
          </div>
          <h5 class="config-card__title">{{ config.name }}</h5>
        </div>

        <p class="config-card__description">{{ config.description }}</p>

        <div class="config-card__path">
          <el-icon><FolderOpened /></el-icon>
          <code>{{ config.path }}</code>
        </div>

        <el-button
          type="primary"
          size="default"
          class="config-card__button"
          @click="handleDownload(config.type)"
        >
          <el-icon><Download /></el-icon>
          下载配置
        </el-button>

        <div class="config-card__shimmer"></div>
      </div>
    </div>

    <el-alert
      v-if="!apiKey"
      type="warning"
      :closable="false"
      class="config-downloader__alert"
    >
      <template #title>
        <div class="config-downloader__alert-content">
          <span>登录后可自动填入您的 API Key</span>
          <el-button
            type="warning"
            size="small"
            @click="$router.push('/login')"
          >
            立即登录
          </el-button>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Download, FolderOpened } from '@element-plus/icons-vue'

interface ConfigType {
  type: string
  name: string
  logoText: string
  description: string
  path: string
  generateConfig: (apiKey: string) => string
}

interface Props {
  apiKey?: string
  baseURL?: string
}

const props = withDefaults(defineProps<Props>(), {
  apiKey: '',
  baseURL: 'https://api.xcoder.plus'
})

const configs: ConfigType[] = [
  {
    type: 'claude',
    name: 'Claude Desktop',
    logoText: 'C',
    description: '使用 Claude 官方 API 的配置文件',
    path: '~/.config/claude/config.json',
    generateConfig: (apiKey: string) => JSON.stringify({
      baseURL: props.baseURL,
      apiKey: apiKey || 'your-api-key-here',
      model: 'claude-3-5-sonnet-20241022'
    }, null, 2)
  },
  {
    type: 'continue',
    name: 'Continue',
    logoText: 'Co',
    description: '适用于 Continue 插件的配置文件',
    path: '~/.continue/config.json',
    generateConfig: (apiKey: string) => JSON.stringify({
      models: [{
        title: 'Claude 3.5 Sonnet',
        provider: 'anthropic',
        model: 'claude-3-5-sonnet-20241022',
        apiKey: apiKey || 'your-api-key-here',
        apiBase: props.baseURL
      }]
    }, null, 2)
  },
  {
    type: 'cursor',
    name: 'Cursor',
    logoText: 'Cu',
    description: '适用于 Cursor 编辑器的配置文件',
    path: '~/.cursor/config.json',
    generateConfig: (apiKey: string) => JSON.stringify({
      anthropic: {
        apiKey: apiKey || 'your-api-key-here',
        baseURL: props.baseURL
      }
    }, null, 2)
  }
]

const handleDownload = (type: string) => {
  const config = configs.find(c => c.type === type)
  if (!config) return

  const configContent = config.generateConfig(props.apiKey)
  const blob = new Blob([configContent], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${type}-config.json`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success(`${config.name} 配置文件已下载`)
}
</script>

<style scoped>
.config-downloader {
  margin: 2rem 0;
}

.config-downloader__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.config-card {
  position: relative;
  padding: 1.75rem;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 16px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  overflow: hidden;
}

.config-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
}

.config-card--claude:hover {
  border-color: #667eea;
  box-shadow: 0 12px 28px rgba(102, 126, 234, 0.25);
}

.config-card--continue:hover {
  border-color: #f39c12;
  box-shadow: 0 12px 28px rgba(243, 156, 18, 0.25);
}

.config-card--cursor:hover {
  border-color: #3498db;
  box-shadow: 0 12px 28px rgba(52, 152, 219, 0.25);
}

.config-card__shimmer {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transform: rotate(45deg);
  transition: all 0.6s ease;
  opacity: 0;
}

.config-card:hover .config-card__shimmer {
  opacity: 1;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%) rotate(45deg);
  }
  100% {
    transform: translateX(100%) rotate(45deg);
  }
}

.config-card__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.config-card__logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 1rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.config-card__logo--claude {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.config-card__logo--continue {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
}

.config-card__logo--cursor {
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
}

.config-card__title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.config-card__description {
  color: #666;
  font-size: 0.9rem;
  line-height: 1.5;
  margin: 0 0 1.25rem 0;
}

.config-card__path {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px;
  margin-bottom: 1.25rem;
  font-size: 0.85rem;
}

.config-card__path .el-icon {
  color: #667eea;
  font-size: 1rem;
  flex-shrink: 0;
}

.config-card__path code {
  color: #e74c3c;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  word-break: break-all;
}

.config-card__button {
  width: 100%;
  font-weight: 600;
  transition: all 0.3s ease;
}

.config-card__button:hover {
  transform: translateY(-2px);
}

.config-downloader__alert {
  margin-top: 1.5rem;
}

.config-downloader__alert-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .config-downloader__grid {
    grid-template-columns: 1fr;
  }

  .config-downloader__alert-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .config-downloader__alert-content .el-button {
    width: 100%;
  }
}
</style>

<template>
  <div class="platform-selector">
    <div class="platform-selector__header">
      <h4 class="platform-selector__title">
        <el-icon><Monitor /></el-icon>
        <span>{{ title }}</span>
      </h4>
      <el-tag v-if="detectedPlatform" type="success" size="small">
        已检测: {{ platformLabels[detectedPlatform] }}
      </el-tag>
    </div>

    <div class="platform-selector__tabs">
      <div
        v-for="platform in platforms"
        :key="platform"
        class="platform-selector__tab"
        :class="{
          'platform-selector__tab--active': modelValue === platform,
          'platform-selector__tab--recommended': platform === detectedPlatform
        }"
        @click="handleSelect(platform)"
      >
        <div class="platform-selector__tab-icon">
          <el-icon>
            <component :is="platformIcons[platform]" />
          </el-icon>
        </div>
        <div class="platform-selector__tab-content">
          <div class="platform-selector__tab-label">
            {{ platformLabels[platform] }}
          </div>
          <div v-if="platform === detectedPlatform" class="platform-selector__tab-badge">
            推荐
          </div>
        </div>
        <div v-if="modelValue === platform" class="platform-selector__tab-indicator"></div>
      </div>
    </div>

    <transition name="slide-fade" mode="out-in">
      <div :key="modelValue" class="platform-selector__content">
        <slot :name="modelValue" :platform="modelValue"></slot>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { usePlatformDetect, type Platform } from '@/composables/usePlatformDetect'

// 平台图标 (使用 Unicode 字符代替，因为 Element Plus 可能没有所有图标)
const platformIcons: Record<Platform, any> = {
  macos: Monitor,
  linux: Monitor,
  windows: Monitor
}

const platformLabels: Record<Platform, string> = {
  macos: 'macOS',
  linux: 'Linux',
  windows: 'Windows'
}

interface Props {
  modelValue: Platform
  title?: string
  platforms?: Platform[]
  autoDetect?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '选择您的操作系统',
  platforms: () => ['macos', 'linux', 'windows'],
  autoDetect: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: Platform): void
}>()

const { detectedPlatform } = usePlatformDetect()

// 如果启用自动检测且检测到平台，则自动选择
const currentPlatform = computed(() => {
  if (props.autoDetect && detectedPlatform.value && !props.modelValue) {
    return detectedPlatform.value
  }
  return props.modelValue
})

const handleSelect = (platform: Platform) => {
  emit('update:modelValue', platform)
}
</script>

<style scoped>
.platform-selector {
  margin: 1.5rem 0;
}

.platform-selector__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
  gap: 1rem;
}

.platform-selector__title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.platform-selector__title .el-icon {
  font-size: 1.3rem;
  color: #667eea;
}

.platform-selector__tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.platform-selector__tab {
  position: relative;
  padding: 1.25rem;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.platform-selector__tab:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.platform-selector__tab--active {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.platform-selector__tab--recommended::before {
  content: '★';
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  color: #ffc107;
  font-size: 1.2rem;
  animation: sparkle 2s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

.platform-selector__tab-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.75rem;
  transition: all 0.3s ease;
}

.platform-selector__tab:hover .platform-selector__tab-icon,
.platform-selector__tab--active .platform-selector__tab-icon {
  transform: scale(1.1) rotate(5deg);
}

.platform-selector__tab-icon .el-icon {
  font-size: 1.5rem;
  color: white;
}

.platform-selector__tab-content {
  position: relative;
}

.platform-selector__tab-label {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.25rem;
}

.platform-selector__tab-badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  font-size: 0.75rem;
  border-radius: 4px;
  font-weight: 600;
  margin-top: 0.25rem;
}

.platform-selector__tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px 2px 0 0;
}

.platform-selector__content {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  border-left: 3px solid #667eea;
}

/* 过渡动画 */
.slide-fade-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(10px);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .platform-selector__tabs {
    grid-template-columns: 1fr;
  }

  .platform-selector__tab {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
  }

  .platform-selector__tab-icon {
    margin-bottom: 0;
    width: 40px;
    height: 40px;
  }

  .platform-selector__tab-icon .el-icon {
    font-size: 1.2rem;
  }

  .platform-selector__content {
    padding: 1rem;
  }
}
</style>

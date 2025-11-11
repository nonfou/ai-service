<template>
  <div
    class="gradient-card"
    :class="[
      `gradient-card--${variant}`,
      {
        'gradient-card--hoverable': hoverable,
        'gradient-card--active': active
      }
    ]"
  >
    <!-- 背景装饰 -->
    <div class="gradient-card__background"></div>
    <div class="gradient-card__glow"></div>

    <!-- 头部 -->
    <div v-if="$slots.header || title" class="gradient-card__header">
      <slot name="header">
        <div class="gradient-card__title-section">
          <div v-if="icon || logo" class="gradient-card__icon">
            <slot name="icon">
              <component v-if="icon" :is="icon" />
              <div v-else-if="logo" class="gradient-card__logo">
                {{ logo }}
              </div>
            </slot>
          </div>
          <div class="gradient-card__title-group">
            <h3 class="gradient-card__title">{{ title }}</h3>
            <p v-if="subtitle" class="gradient-card__subtitle">{{ subtitle }}</p>
          </div>
        </div>
        <div v-if="badge" class="gradient-card__badge">
          <el-tag :type="badgeType" size="large" effect="dark">
            <el-icon v-if="badgeIcon"><component :is="badgeIcon" /></el-icon>
            {{ badge }}
          </el-tag>
        </div>
      </slot>
    </div>

    <!-- 主体内容 -->
    <div class="gradient-card__body">
      <slot></slot>
    </div>

    <!-- 底部操作 -->
    <div v-if="$slots.footer || $slots.actions" class="gradient-card__footer">
      <slot name="footer">
        <div class="gradient-card__actions">
          <slot name="actions"></slot>
        </div>
      </slot>
    </div>

    <!-- 顶部装饰条 -->
    <div class="gradient-card__top-bar"></div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  title?: string
  subtitle?: string
  icon?: Component
  logo?: string
  badge?: string
  badgeType?: 'success' | 'info' | 'warning' | 'danger' | 'primary'
  badgeIcon?: Component
  variant?: 'primary' | 'success' | 'warning' | 'info'
  hoverable?: boolean
  active?: boolean
}

withDefaults(defineProps<Props>(), {
  title: '',
  subtitle: '',
  icon: undefined,
  logo: '',
  badge: '',
  badgeType: 'primary',
  badgeIcon: undefined,
  variant: 'primary',
  hoverable: true,
  active: false
})
</script>

<style scoped>
.gradient-card {
  position: relative;
  background: white;
  border-radius: 20px;
  padding: 2rem;
  border: 2px solid transparent;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.gradient-card--hoverable:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
}

.gradient-card__background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
  z-index: 0;
}

.gradient-card--primary .gradient-card__background {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.gradient-card--success .gradient-card__background {
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.05) 0%, rgba(56, 239, 125, 0.05) 100%);
}

.gradient-card--warning .gradient-card__background {
  background: linear-gradient(135deg, rgba(243, 156, 18, 0.05) 0%, rgba(230, 126, 34, 0.05) 100%);
}

.gradient-card--info .gradient-card__background {
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.05) 0%, rgba(41, 128, 185, 0.05) 100%);
}

.gradient-card:hover .gradient-card__background,
.gradient-card--active .gradient-card__background {
  opacity: 1;
}

.gradient-card__glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  opacity: 0;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
  transition: opacity 0.6s ease;
  pointer-events: none;
  z-index: 0;
}

.gradient-card:hover .gradient-card__glow {
  opacity: 1;
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.gradient-card__top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.gradient-card--primary .gradient-card__top-bar {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.gradient-card--success .gradient-card__top-bar {
  background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%);
}

.gradient-card--warning .gradient-card__top-bar {
  background: linear-gradient(90deg, #f39c12 0%, #e67e22 100%);
}

.gradient-card--info .gradient-card__top-bar {
  background: linear-gradient(90deg, #3498db 0%, #2980b9 100%);
}

.gradient-card:hover .gradient-card__top-bar,
.gradient-card--active .gradient-card__top-bar {
  opacity: 1;
}

.gradient-card__header,
.gradient-card__body,
.gradient-card__footer {
  position: relative;
  z-index: 1;
}

.gradient-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  gap: 1.5rem;
}

.gradient-card__title-section {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  flex: 1;
}

.gradient-card__icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.8rem;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.gradient-card--success .gradient-card__icon {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 4px 16px rgba(17, 153, 142, 0.3);
}

.gradient-card:hover .gradient-card__icon {
  transform: scale(1.1) rotate(-5deg);
}

.gradient-card__logo {
  font-size: 1.5rem;
  font-weight: 700;
}

.gradient-card__title-group {
  flex: 1;
}

.gradient-card__title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  color: #333;
  line-height: 1.3;
}

.gradient-card__subtitle {
  color: #666;
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
}

.gradient-card__badge {
  flex-shrink: 0;
}

.gradient-card__body {
  margin: 1.5rem 0;
}

.gradient-card__footer {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.gradient-card__actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

/* 响应式 */
@media (max-width: 768px) {
  .gradient-card {
    padding: 1.5rem;
    border-radius: 16px;
  }

  .gradient-card__header {
    flex-direction: column;
    gap: 1rem;
  }

  .gradient-card__title-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .gradient-card__title {
    font-size: 1.5rem;
  }

  .gradient-card__icon {
    width: 48px;
    height: 48px;
    font-size: 1.5rem;
  }

  .gradient-card__actions {
    flex-direction: column;
  }

  .gradient-card__actions :deep(.el-button) {
    width: 100%;
  }
}
</style>

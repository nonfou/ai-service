<template>
  <div
    class="installation-step"
    :class="{
      'installation-step--completed': completed,
      'installation-step--active': active
    }"
  >
    <div class="installation-step__indicator">
      <div class="installation-step__number">
        <el-icon v-if="completed" class="installation-step__check">
          <CircleCheck />
        </el-icon>
        <el-icon v-else-if="icon" class="installation-step__icon">
          <component :is="icon" />
        </el-icon>
        <span v-else class="installation-step__step-num">{{ stepNumber }}</span>
      </div>
      <div v-if="!isLast" class="installation-step__line"></div>
    </div>

    <div class="installation-step__content">
      <div class="installation-step__header">
        <h5 class="installation-step__title">{{ title }}</h5>
        <el-tag v-if="optional" size="small" type="info">可选</el-tag>
      </div>

      <p v-if="description" class="installation-step__description">
        {{ description }}
      </p>

      <div class="installation-step__body">
        <slot></slot>
      </div>

      <div v-if="!hideCheckbox" class="installation-step__checkbox">
        <el-checkbox
          :model-value="completed"
          @update:model-value="$emit('update:completed', $event)"
        >
          {{ completed ? '已完成此步骤' : '标记为已完成' }}
        </el-checkbox>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck } from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface Props {
  stepNumber?: number
  title: string
  description?: string
  icon?: Component
  completed?: boolean
  active?: boolean
  optional?: boolean
  isLast?: boolean
  hideCheckbox?: boolean
}

withDefaults(defineProps<Props>(), {
  stepNumber: 1,
  description: '',
  icon: undefined,
  completed: false,
  active: false,
  optional: false,
  isLast: false,
  hideCheckbox: false
})

defineEmits<{
  (e: 'update:completed', value: boolean): void
}>()
</script>

<style scoped>
.installation-step {
  display: flex;
  gap: 1.5rem;
  position: relative;
  padding-bottom: 2rem;
}

.installation-step--active .installation-step__content {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  border-left-color: #667eea;
}

.installation-step--completed .installation-step__number {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.installation-step__indicator {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.installation-step__number {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 2;
}

.installation-step--active .installation-step__number {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }
  50% {
    box-shadow: 0 4px 24px rgba(102, 126, 234, 0.6);
  }
}

.installation-step__check,
.installation-step__icon {
  font-size: 1.5rem;
}

.installation-step__step-num {
  font-size: 1.2rem;
}

.installation-step__line {
  flex: 1;
  width: 2px;
  background: linear-gradient(180deg, #667eea 0%, rgba(102, 126, 234, 0.2) 100%);
  margin-top: 0.5rem;
  min-height: 40px;
  position: relative;
}

.installation-step--completed .installation-step__line {
  background: linear-gradient(180deg, #67c23a 0%, rgba(103, 194, 58, 0.2) 100%);
}

.installation-step__content {
  flex: 1;
  padding: 1rem 1.5rem;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  border-left: 3px solid transparent;
  transition: all 0.3s ease;
}

.installation-step__content:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.installation-step__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
  gap: 1rem;
}

.installation-step__title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.installation-step__description {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.6;
  margin: 0 0 1rem 0;
}

.installation-step__body {
  margin: 1rem 0;
}

.installation-step__checkbox {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

/* 响应式 */
@media (max-width: 768px) {
  .installation-step {
    gap: 1rem;
  }

  .installation-step__number {
    width: 40px;
    height: 40px;
    font-size: 0.95rem;
  }

  .installation-step__check,
  .installation-step__icon {
    font-size: 1.2rem;
  }

  .installation-step__content {
    padding: 0.75rem 1rem;
  }

  .installation-step__title {
    font-size: 1.05rem;
  }
}
</style>

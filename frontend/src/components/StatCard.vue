<template>
  <div class="stat-card" :class="{ clickable: to }" @click="handleClick">
    <div class="stat-card-header">
      <div class="stat-icon" :class="`stat-icon-${type}`">
        <slot name="icon" />
      </div>
      <div v-if="trend" class="stat-trend" :class="trend > 0 ? 'trend-up' : 'trend-down'">
        <component :is="trend > 0 ? TrendCharts : TrendCharts" />
        <span>{{ trend > 0 ? '+' : '' }}{{ trend }}</span>
      </div>
      <div v-if="badge" class="stat-badge">
        {{ badge }}
      </div>
    </div>
    <div class="stat-card-body">
      <div class="stat-value">{{ formattedValue }}</div>
      <div class="stat-label">{{ label }}</div>
      <div v-if="description" class="stat-description">{{ description }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts } from '@element-plus/icons-vue'

const props = defineProps<{
  value: number | string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  trend?: number
  badge?: number | string
  prefix?: string
  to?: string
  description?: string
}>()

const router = useRouter()

const formattedValue = computed(() => {
  const val = typeof props.value === 'number' ? props.value.toLocaleString() : props.value
  return props.prefix ? `${props.prefix}${val}` : val
})

const handleClick = () => {
  if (props.to) {
    router.push(props.to)
  }
}
</script>

<style scoped>
.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-light);
  transition: all var(--transition-normal);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon-primary {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.stat-icon-success {
  background: var(--success-bg);
  color: var(--success-color);
}

.stat-icon-warning {
  background: var(--warning-bg);
  color: var(--warning-color);
}

.stat-icon-danger {
  background: var(--danger-bg);
  color: var(--danger-color);
}

.stat-icon-info {
  background: var(--info-bg);
  color: var(--info-color);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

.trend-up {
  background: var(--success-bg);
  color: var(--success-color);
}

.trend-down {
  background: var(--danger-bg);
  color: var(--danger-color);
}

.stat-badge {
  background: var(--danger-color);
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  min-width: 24px;
  text-align: center;
}

.stat-card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.stat-description {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>

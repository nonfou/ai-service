<template>
  <div class="page-header">
    <div class="page-header-content">
      <div class="page-header-info">
        <h1 class="page-header-title">{{ title }}</h1>
        <p v-if="description" class="page-header-description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-header-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header-info {
  flex: 1;
}

.page-header-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
  line-height: 1.3;
}

.page-header-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .page-header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header-actions {
    margin-top: 16px;
  }

  .page-header-title {
    font-size: 20px;
  }
}
</style>

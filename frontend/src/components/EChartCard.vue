<template>
  <el-card class="chart-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="chart-card-header">
        <div>
          <h3 class="chart-card-title">{{ title }}</h3>
          <p v-if="description" class="chart-card-description">{{ description }}</p>
        </div>
        <slot name="extra" />
      </div>
    </template>

    <div v-if="hasOption" ref="chartRef" class="chart-canvas" :style="{ height }" />
    <el-empty v-else :description="emptyDescription" :image-size="96" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

const props = withDefaults(defineProps<{
  title: string
  description?: string
  option?: EChartsCoreOption | null
  loading?: boolean
  height?: string
  emptyDescription?: string
}>(), {
  description: '',
  option: null,
  loading: false,
  height: '320px',
  emptyDescription: '暂无数据'
})

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: ECharts | null = null

const hasOption = computed(() => Boolean(props.option))

const resizeChart = () => {
  chartInstance?.resize()
}

const renderChart = async () => {
  if (!chartRef.value || !props.option) {
    return
  }

  await nextTick()

  if (!chartRef.value) {
    return
  }

  if (!chartInstance) {
    chartInstance = init(chartRef.value)
  }

  chartInstance.setOption(props.option, true)
  resizeChart()
}

watch(
  () => props.option,
  async (option) => {
    if (!option) {
      chartInstance?.dispose()
      chartInstance = null
      return
    }
    await renderChart()
  },
  { deep: true }
)

onMounted(async () => {
  window.addEventListener('resize', resizeChart)
  if (props.option) {
    await renderChart()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.chart-card {
  height: 100%;
}

.chart-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.chart-card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.chart-card-description {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.chart-canvas {
  width: 100%;
}
</style>

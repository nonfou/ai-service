<template>
  <div class="logs-page">
    <h2>调用日志</h2>
    <el-table :data="logs" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="modelName" label="模型" />
      <el-table-column prop="inputTokens" label="输入Token" />
      <el-table-column prop="outputTokens" label="输出Token" />
      <el-table-column prop="totalCost" label="消费" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const logs = ref([])
</script>

<style scoped>
.logs-page {
  padding: 2rem;
}

h2 {
  margin-bottom: 2rem;
}
</style>

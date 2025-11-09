<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  msg: string
}>()
</script>

<style scoped>
.hello {
  padding: 2rem;
}

h1 {
  font-size: 2rem;
  color: #42b983;
}
</style>

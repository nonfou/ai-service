<template>
  <div
    class="code-block"
    :class="{
      'code-block--inline': inline,
      'code-block--copied': showCopied
    }"
  >
    <div v-if="language" class="code-block__header">
      <span class="code-block__language">{{ language }}</span>
    </div>

    <div class="code-block__content">
      <pre><code>{{ code }}</code></pre>
    </div>

    <el-button
      class="code-block__copy-btn"
      :class="{ 'code-block__copy-btn--copied': showCopied }"
      size="small"
      :type="showCopied ? 'success' : 'primary'"
      @click="handleCopy"
    >
      <el-icon>
        <component :is="showCopied ? SuccessFilled : CopyDocument" />
      </el-icon>
      <span>{{ showCopied ? '已复制' : copyText }}</span>
    </el-button>

    <!-- 复制成功动画 -->
    <transition name="ripple">
      <div v-if="showCopied" class="code-block__ripple"></div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CopyDocument, SuccessFilled } from '@element-plus/icons-vue'
import { useClipboard } from '@/composables/useClipboard'

interface Props {
  code: string
  language?: string
  inline?: boolean
  copyText?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: '',
  inline: false,
  copyText: '复制'
})

const { copy } = useClipboard()
const showCopied = ref(false)

const handleCopy = async () => {
  const success = await copy(props.code)

  if (success) {
    showCopied.value = true
    setTimeout(() => {
      showCopied.value = false
    }, 2000)
  }
}
</script>

<style scoped>
.code-block {
  position: relative;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 12px;
  padding: 1.25rem;
  margin: 1rem 0;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.code-block:hover {
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.code-block--copied {
  box-shadow:
    0 0 0 2px rgba(103, 194, 58, 0.5),
    0 8px 24px rgba(103, 194, 58, 0.2);
}

.code-block__header {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  z-index: 1;
}

.code-block__language {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  color: #8892b0;
  font-size: 0.75rem;
  border-radius: 6px;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.code-block__content {
  margin-top: 1.5rem;
}

.code-block__content pre {
  margin: 0;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
}

.code-block__content pre::-webkit-scrollbar {
  height: 6px;
}

.code-block__content pre::-webkit-scrollbar-track {
  background: transparent;
}

.code-block__content pre::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.code-block__content pre::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.code-block__content code {
  color: #e6f1ff;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.code-block__copy-btn {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  z-index: 2;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.code-block__copy-btn--copied {
  transform: scale(1.05);
}

.code-block__ripple {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.2) 0%, transparent 70%);
  pointer-events: none;
}

/* 内联模式 */
.code-block--inline {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  margin: 0.5rem 0;
  background: linear-gradient(135deg, #252540 0%, #1f2937 100%);
}

.code-block--inline .code-block__content {
  margin-top: 0;
  flex: 1;
}

.code-block--inline .code-block__content pre {
  margin: 0;
}

.code-block--inline .code-block__copy-btn {
  position: static;
}

/* 动画 */
.ripple-enter-active {
  animation: ripple-expand 0.6s ease-out;
}

.ripple-leave-active {
  animation: ripple-fade 0.4s ease-out;
}

@keyframes ripple-expand {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes ripple-fade {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .code-block {
    padding: 1rem;
  }

  .code-block__copy-btn {
    position: static;
    margin-top: 1rem;
    width: 100%;
  }

  .code-block__content {
    margin-top: 0.5rem;
  }

  .code-block__language {
    position: static;
    display: block;
    margin-bottom: 0.75rem;
  }
}
</style>

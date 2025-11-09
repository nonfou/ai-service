<template>
  <div class="dashboard-layout">
    <el-container>
      <!-- 当路由为 /dashboard/overview 时隐藏左侧导航 -->
      <el-aside v-if="$route.path !== '/dashboard/overview'" width="200px">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/dashboard/overview">
            <el-icon><DataLine /></el-icon>
            <span>概览</span>
          </el-menu-item>
          <el-menu-item index="/dashboard/api-key">
            <el-icon><Key /></el-icon>
            <span>API密钥</span>
          </el-menu-item>
          <el-menu-item index="/dashboard/usage">
            <el-icon><TrendCharts /></el-icon>
            <span>使用统计</span>
          </el-menu-item>
          <el-menu-item index="/dashboard/logs">
            <el-icon><Document /></el-icon>
            <span>调用日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { DataLine, Key, TrendCharts, Document } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

/* container 使用 flex 布局以便更好地控制左右占比 */
.el-container {
  display: flex;
  gap: 0;
  align-items: stretch;
}

/* 侧栏默认宽度并增加内边距与垂直滚动支持 */
.el-aside {
  background: white;
  border-right: 1px solid #eee;
  width: 220px; /* 略增宽度，提升可点击区域 */
  padding-top: 1rem;
  box-sizing: border-box;
  overflow-y: auto;
}

/* 当侧栏被隐藏（例如在 overview 页面），让主区域占满全部宽度 */
.el-aside[style*="display: none"], .el-aside[hidden] {
  display: none !important;
}

/* 主区域在有侧栏时占剩余空间；无侧栏时自然充满容器 */
.el-main {
  padding: 2rem;
  flex: 1 1 auto;
  box-sizing: border-box;
  min-width: 0; /* 避免子元素超出容器 */
}

/* 提升概览页视觉比例：使卡片宽度更合理，间距更舒展 */
.overview-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 1.5rem;
}

/* 调整网格间距与卡片最小高度，改善视觉密度 */
.usage-stats-grid {
  gap: 1.25rem;
}
.info-cards-grid {
  gap: 1.25rem;
}
.bottom-cards-grid {
  gap: 1.25rem;
}

/* 卡片内边距与最小高度优化 */
.v2board-card {
  padding: 1.25rem;
  min-height: 120px;
}

/* 欢迎标题调整，给右侧留更多空间显得更平衡 */
.welcome-title {
  font-size: 1.5rem;
  margin-bottom: 0.25rem;
}
.welcome-subtitle {
  margin-bottom: 0.75rem;
}

/* 响应式优化：在窄屏隐藏侧栏并让主内容无边距 */
@media (max-width: 1024px) {
  .el-aside {
    display: none !important;
  }
  .el-main {
    padding: 1rem;
  }
  .overview-page {
    max-width: 100%;
    padding-top: 1rem;
  }
}

/* 小屏幕进一步压缩间距 */
@media (max-width: 768px) {
  .el-main {
    padding: 0.75rem;
  }
  .v2board-card {
    padding: 0.75rem;
  }
}
</style>

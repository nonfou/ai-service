<template>
  <div class="overview-page">
    <!-- 数据概览 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon size="32"><User /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-label">总用户数</p>
              <p class="stat-value">{{ userStats.totalUsers }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon size="32"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-label">今日订单</p>
              <p class="stat-value">{{ orderStats.todayOrders }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-label">今日金额</p>
              <p class="stat-value">¥{{ orderStats.todayAmount }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon size="32"><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-label">待处理工单</p>
              <p class="stat-value">{{ ticketStats.pendingTickets }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细统计 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户统计</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总用户数">
              {{ userStats.totalUsers }}
            </el-descriptions-item>
            <el-descriptions-item label="活跃用户">
              {{ userStats.activeUsers }}
            </el-descriptions-item>
            <el-descriptions-item label="今日新增">
              <el-tag type="success">+{{ userStats.todayNewUsers }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总余额">
              ¥{{ userStats.totalBalance }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>订单统计</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总订单数">
              {{ orderStats.totalOrders }}
            </el-descriptions-item>
            <el-descriptions-item label="待支付">
              <el-tag type="warning">{{ orderStats.pendingOrders }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已支付">
              <el-tag type="success">{{ orderStats.paidOrders }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="今日订单">
              {{ orderStats.todayOrders }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>工单统计</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总工单数">
              {{ ticketStats.totalTickets }}
            </el-descriptions-item>
            <el-descriptions-item label="待处理">
              <el-tag type="danger">{{ ticketStats.pendingTickets }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理中">
              <el-tag type="warning">{{ ticketStats.processingTickets }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已关闭">
              <el-tag type="info">{{ ticketStats.closedTickets }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  User,
  Money,
  TrendCharts,
  Bell,
  UserFilled,
  Tickets,
  ChatDotRound,
  Cpu
} from '@element-plus/icons-vue'
import { adminAPI } from '../api'

const userStats = ref({
  totalUsers: 0,
  activeUsers: 0,
  todayNewUsers: 0,
  totalBalance: 0
})

const orderStats = ref({
  totalOrders: 0,
  pendingOrders: 0,
  paidOrders: 0,
  todayOrders: 0,
  todayAmount: 0
})

const ticketStats = ref({
  totalTickets: 0,
  pendingTickets: 0,
  processingTickets: 0,
  closedTickets: 0,
  todayTickets: 0
})

const fetchUserStats = async () => {
  try {
    const res = await adminAPI.getUserStatistics()
    userStats.value = res.data
  } catch (error) {
    console.error('获取用户统计失败:', error)
  }
}

const fetchOrderStats = async () => {
  try {
    const res = await adminAPI.getOrderStatistics()
    orderStats.value = res.data
  } catch (error) {
    console.error('获取订单统计失败:', error)
  }
}

const fetchTicketStats = async () => {
  try {
    const res = await adminAPI.getTicketStatistics()
    ticketStats.value = res.data
  } catch (error) {
    console.error('获取工单统计失败:', error)
  }
}

onMounted(() => {
  fetchUserStats()
  fetchOrderStats()
  fetchTicketStats()
})
</script>

<style scoped>
.overview-page {
  padding: 0;
}

.stat-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 8px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-actions .el-button {
  width: 100%;
  height: 48px;
}

@media (max-width: 768px) {
  .stat-value {
    font-size: 24px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>

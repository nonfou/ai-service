<template>
  <div class="overview-page">
    <!-- 统计卡片区域 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-card-header">
          <div class="stat-icon stat-icon-primary">
            <User />
          </div>
          <div class="stat-trend trend-up">
            <TrendCharts />
            <span>+{{ userStats.todayNewUsers }}</span>
          </div>
        </div>
        <div class="stat-card-body">
          <div class="stat-value">{{ userStats.totalUsers }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <div class="stat-icon stat-icon-success">
            <Tickets />
          </div>
        </div>
        <div class="stat-card-body">
          <div class="stat-value">{{ orderStats.todayOrders }}</div>
          <div class="stat-label">今日订单</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <div class="stat-icon stat-icon-warning">
            <Money />
          </div>
        </div>
        <div class="stat-card-body">
          <div class="stat-value">¥{{ formatNumber(orderStats.todayAmount) }}</div>
          <div class="stat-label">今日金额</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <div class="stat-icon stat-icon-danger">
            <Bell />
          </div>
          <div v-if="ticketStats.pendingTickets > 0" class="stat-badge">
            {{ ticketStats.pendingTickets }}
          </div>
        </div>
        <div class="stat-card-body">
          <div class="stat-value">{{ ticketStats.totalTickets }}</div>
          <div class="stat-label">待处理工单</div>
        </div>
      </div>
    </div>

    <!-- 详细统计区域 -->
    <div class="details-grid">
      <!-- 用户统计 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3>用户统计</h3>
          <router-link to="/users" class="view-all-link">
            查看全部
            <ArrowRight />
          </router-link>
        </div>
        <div class="detail-card-body">
          <div class="detail-item">
            <span class="detail-label">总用户数</span>
            <span class="detail-value">{{ userStats.totalUsers }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">活跃用户</span>
            <span class="detail-value">{{ userStats.activeUsers }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">今日新增</span>
            <span class="detail-value">
              <span class="badge badge-success">+{{ userStats.todayNewUsers }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">总余额</span>
            <span class="detail-value">¥{{ formatNumber(userStats.totalBalance) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单统计 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3>订单统计</h3>
          <router-link to="/orders" class="view-all-link">
            查看全部
            <ArrowRight />
          </router-link>
        </div>
        <div class="detail-card-body">
          <div class="detail-item">
            <span class="detail-label">总订单数</span>
            <span class="detail-value">{{ orderStats.totalOrders }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">待支付</span>
            <span class="detail-value">
              <span class="badge badge-warning">{{ orderStats.pendingOrders }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">已支付</span>
            <span class="detail-value">
              <span class="badge badge-success">{{ orderStats.paidOrders }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">今日订单</span>
            <span class="detail-value">{{ orderStats.todayOrders }}</span>
          </div>
        </div>
      </div>

      <!-- 工单统计 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3>工单统计</h3>
          <router-link to="/tickets" class="view-all-link">
            查看全部
            <ArrowRight />
          </router-link>
        </div>
        <div class="detail-card-body">
          <div class="detail-item">
            <span class="detail-label">总工单数</span>
            <span class="detail-value">{{ ticketStats.totalTickets }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">待处理</span>
            <span class="detail-value">
              <span class="badge badge-danger">{{ ticketStats.pendingTickets }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">处理中</span>
            <span class="detail-value">
              <span class="badge badge-warning">{{ ticketStats.processingTickets }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">已关闭</span>
            <span class="detail-value">
              <span class="badge badge-muted">{{ ticketStats.closedTickets }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3>快捷操作</h3>
        </div>
        <div class="detail-card-body">
          <div class="quick-actions">
            <router-link to="/users" class="quick-action-btn">
              <User />
              <span>用户管理</span>
            </router-link>
            <router-link to="/models" class="quick-action-btn">
              <Cpu />
              <span>模型管理</span>
            </router-link>
            <router-link to="/orders" class="quick-action-btn">
              <Tickets />
              <span>订单管理</span>
            </router-link>
            <router-link to="/tickets" class="quick-action-btn">
              <ChatDotRound />
              <span>工单管理</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  User,
  Money,
  TrendCharts,
  Bell,
  Tickets,
  ChatDotRound,
  Cpu,
  ArrowRight
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

const formatNumber = (num: number) => {
  return num?.toLocaleString() || '0'
}

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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ==================== 统计卡片网格 ==================== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

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

/* ==================== 详细统计网格 ==================== */
.details-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.detail-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.detail-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
}

.detail-card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.view-all-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--primary-color);
  text-decoration: none;
}

.view-all-link:hover {
  color: var(--primary-light);
}

.detail-card-body {
  padding: 16px 24px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 徽章样式 */
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
}

.badge-success {
  background: var(--success-bg);
  color: var(--success-color);
}

.badge-warning {
  background: var(--warning-bg);
  color: var(--warning-color);
}

.badge-danger {
  background: var(--danger-bg);
  color: var(--danger-color);
}

.badge-muted {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

/* ==================== 快捷操作 ==================== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--text-secondary);
  transition: all var(--transition-normal);
  font-size: 14px;
}

.quick-action-btn:hover {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.quick-action-btn svg {
  width: 24px;
  height: 24px;
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 28px;
  }
}
</style>

<template>
  <div class="users-page">
    <!-- 筛选条件 -->
    <el-card style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="用户ID">
          <el-input
            v-model="userIdFilter"
            placeholder="请输入用户ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="用户名">
          <el-input
            v-model="usernameFilter"
            placeholder="请输入用户名"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card>
      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />

        <el-table-column prop="balance" label="余额" width="120" align="right">
          <template #default="{ row }">
            <span style="color: var(--success-color); font-weight: 600">
              ¥{{ row.balance }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="totalRecharge" label="累计充值" width="120" align="right">
          <template #default="{ row }">
            ¥{{ row.totalRecharge || 0 }}
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column prop="lastLoginAt" label="最后登录" width="180">
          <template #default="{ row }">
            {{ row.lastLoginAt || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="viewUser(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button size="small" type="primary" @click="adjustBalance(row)">
              调整余额
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="fetchUsers"
        @size-change="fetchUsers"
      />
    </el-card>

    <!-- 用户详情对话框 - 多Tab结构 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详细信息"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" v-if="currentUser">
        <!-- Tab 1: 基础信息 -->
        <el-tab-pane label="基础信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
                {{ currentUser.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="余额">
              <span style="color: var(--el-color-success); font-weight: 600">
                ¥{{ currentUser.balance }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="累计充值">¥{{ currentUser.totalRecharge || 0 }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ currentUser.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ currentUser.lastLoginAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 订单记录 -->
        <el-tab-pane label="订单记录" name="orders">
          <el-table :data="userOrders" v-loading="ordersLoading" border>
            <el-table-column prop="orderNo" label="订单号" width="200" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'">
                  {{ row.status === 1 ? '已支付' : row.status === 0 ? '待支付' : '已取消' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="payMethod" label="支付方式" width="120" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column prop="payTime" label="支付时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="ordersPagination.page"
            v-model:page-size="ordersPagination.pageSize"
            :total="ordersPagination.total"
            layout="prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadUserOrders"
          />
        </el-tab-pane>

        <!-- Tab 3: Token消耗 -->
        <el-tab-pane label="Token消耗" name="tokens">
          <el-row :gutter="20" style="margin-bottom: 20px">
            <el-col :span="6">
              <el-statistic title="总消耗Token" :value="tokenStats.totalTokens" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="今日消耗" :value="tokenStats.todayTokens" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="总费用" :value="tokenStats.totalCost" prefix="¥" :precision="2" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="今日费用" :value="tokenStats.todayCost" prefix="¥" :precision="2" />
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-bottom: 20px">
            <el-col :span="6">
              <el-statistic title="总调用次数" :value="tokenStats.totalCalls" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="今日调用" :value="tokenStats.todayCalls" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="输入Token" :value="tokenStats.totalInputTokens" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="输出Token" :value="tokenStats.totalOutputTokens" />
            </el-col>
          </el-row>

          <div ref="tokenTrendChartRef" style="height: 400px; margin-top: 20px"></div>
        </el-tab-pane>

        <!-- Tab 4: 模型使用 -->
        <el-tab-pane label="模型使用" name="models">
          <el-table :data="modelStats" v-loading="modelStatsLoading" border>
            <el-table-column prop="displayName" label="模型" width="200" />
            <el-table-column prop="calls" label="调用次数" width="120" align="right" />
            <el-table-column prop="successRate" label="成功率" width="100" align="right">
              <template #default="{ row }">{{ row.successRate }}%</template>
            </el-table-column>
            <el-table-column prop="totalTokens" label="总Token" width="150" align="right" />
            <el-table-column prop="totalCost" label="总费用" width="120" align="right">
              <template #default="{ row }">¥{{ row.totalCost }}</template>
            </el-table-column>
            <el-table-column prop="avgDuration" label="平均耗时(ms)" width="150" align="right" />
            <el-table-column prop="lastUsedAt" label="最后使用" width="180" />
          </el-table>

          <div ref="modelPieChartRef" style="height: 400px; margin-top: 20px"></div>
        </el-tab-pane>

        <!-- Tab 5: API调用日志 -->
        <el-tab-pane label="调用日志" name="logs">
          <el-table :data="apiCallLogs" v-loading="logsLoading" border>
            <el-table-column prop="model" label="模型" width="180" />
            <el-table-column prop="inputTokens" label="输入Token" width="120" align="right" />
            <el-table-column prop="outputTokens" label="输出Token" width="120" align="right" />
            <el-table-column prop="cost" label="费用" width="100" align="right">
              <template #default="{ row }">¥{{ row.cost }}</template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时(ms)" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="logsPagination.page"
            v-model:page-size="logsPagination.pageSize"
            :total="logsPagination.total"
            layout="prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadApiCallLogs"
          />
        </el-tab-pane>

        <!-- Tab 6: 余额日志 -->
        <el-tab-pane label="余额日志" name="balance">
          <el-table :data="balanceLogs" v-loading="balanceLogsLoading" border>
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="row.amount > 0 ? 'success' : 'warning'">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="变动金额" width="150" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.amount > 0 ? 'var(--el-color-success)' : 'var(--el-color-warning)' }">
                  {{ row.amount > 0 ? '+' : '' }}¥{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变动后余额" width="150" align="right">
              <template #default="{ row }">¥{{ row.balanceAfter }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="balanceLogsPagination.page"
            v-model:page-size="balanceLogsPagination.pageSize"
            :total="balanceLogsPagination.total"
            layout="prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadBalanceLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 调整余额对话框 -->
    <el-dialog v-model="balanceDialogVisible" title="调整用户余额" width="500px">
      <el-form :model="balanceForm" label-width="100px" v-if="currentUser">
        <el-form-item label="用户名">
          <span>{{ currentUser.username }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span style="color: var(--success-color); font-weight: 600">
            ¥{{ currentUser.balance }}
          </span>
        </el-form-item>
        <el-form-item label="调整类型">
          <el-radio-group v-model="balanceForm.type">
            <el-radio value="add">增加</el-radio>
            <el-radio value="subtract">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整金额">
          <el-input-number
            v-model="balanceForm.amount"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="balanceForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
          />
        </el-form-item>
        <el-form-item label="调整后余额">
          <span style="color: var(--warning-color); font-weight: 600">
            ¥{{ calculatedBalance }}
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="balanceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="handleAdjustBalance">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { adminAPI, type User, type UserTokenStatsResponse, type ModelStatsResponse, type RechargeOrder, type ApiCall, type BalanceLog } from '../api'
import * as echarts from 'echarts'
import message from '../utils/message'

const loading = ref(false)
const adjusting = ref(false)
const users = ref<User[]>([])
const currentUser = ref<User | null>(null)
const detailDialogVisible = ref(false)
const balanceDialogVisible = ref(false)
const activeTab = ref('basic')

// 筛选条件
const userIdFilter = ref('')
const usernameFilter = ref('')
const statusFilter = ref<number | undefined>(undefined)

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const balanceForm = ref({
  type: 'add',
  amount: 0,
  reason: ''
})

// 用户统计数据
const tokenStats = ref<UserTokenStatsResponse>({
  totalInputTokens: 0,
  totalOutputTokens: 0,
  totalTokens: 0,
  totalCost: 0,
  totalCalls: 0,
  todayInputTokens: 0,
  todayOutputTokens: 0,
  todayTokens: 0,
  todayCost: 0,
  todayCalls: 0
})

const modelStats = ref<ModelStatsResponse[]>([])
const userOrders = ref<RechargeOrder[]>([])
const apiCallLogs = ref<ApiCall[]>([])
const balanceLogs = ref<BalanceLog[]>([])

// Loading状态
const ordersLoading = ref(false)
const modelStatsLoading = ref(false)
const logsLoading = ref(false)
const balanceLogsLoading = ref(false)

// 分页
const ordersPagination = ref({ page: 1, pageSize: 10, total: 0 })
const logsPagination = ref({ page: 1, pageSize: 10, total: 0 })
const balanceLogsPagination = ref({ page: 1, pageSize: 10, total: 0 })

// 图表引用
const tokenTrendChartRef = ref<HTMLElement>()
const modelPieChartRef = ref<HTMLElement>()
let tokenTrendChart: echarts.ECharts | null = null
let modelPieChart: echarts.ECharts | null = null

const calculatedBalance = computed(() => {
  if (!currentUser.value) return 0
  const current = currentUser.value.balance
  const amount = balanceForm.value.amount || 0
  return balanceForm.value.type === 'add' ? current + amount : current - amount
})

const fetchUsers = async () => {
  try {
    loading.value = true
    const res = await adminAPI.getUsers(
      pagination.value.page,
      pagination.value.pageSize,
      statusFilter.value,
      userIdFilter.value || undefined,
      usernameFilter.value || undefined
    )

    users.value = res.data.records
    pagination.value.total = res.data.total
  } catch (error: any) {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  fetchUsers()
}

const handleReset = () => {
  userIdFilter.value = ''
  usernameFilter.value = ''
  statusFilter.value = undefined
  pagination.value.page = 1
  fetchUsers()
}

const viewUser = async (row: User) => {
  currentUser.value = row
  activeTab.value = 'basic'
  detailDialogVisible.value = true
}

const handleTabChange = async (tabName: string) => {
  if (!currentUser.value) return

  switch (tabName) {
    case 'orders':
      await loadUserOrders()
      break
    case 'tokens':
      await loadTokenStats()
      break
    case 'models':
      await loadModelStats()
      break
    case 'logs':
      await loadApiCallLogs()
      break
    case 'balance':
      await loadBalanceLogs()
      break
  }
}

const loadUserOrders = async () => {
  if (!currentUser.value) return
  try {
    ordersLoading.value = true
    const res = await adminAPI.getUserOrders(
      currentUser.value.id,
      ordersPagination.value.page,
      ordersPagination.value.pageSize
    )
    userOrders.value = res.data.records
    ordersPagination.value.total = res.data.total
  } catch (error) {
    message.error('获取订单记录失败')
  } finally {
    ordersLoading.value = false
  }
}

const loadTokenStats = async () => {
  if (!currentUser.value) return
  try {
    // 获取统计数据
    const statsRes = await adminAPI.getUserTokenStats(currentUser.value.id)
    tokenStats.value = statsRes.data

    // 获取趋势数据
    const trendRes = await adminAPI.getUserTokenTrend(currentUser.value.id, 7)

    // 渲染图表
    await nextTick()
    renderTokenTrendChart(trendRes.data)
  } catch (error) {
    message.error('获取Token统计失败')
  }
}

const loadModelStats = async () => {
  if (!currentUser.value) return
  try {
    modelStatsLoading.value = true
    const res = await adminAPI.getUserModelStats(currentUser.value.id)
    modelStats.value = res.data

    // 渲染饼图
    await nextTick()
    renderModelPieChart(res.data)
  } catch (error) {
    message.error('获取模型统计失败')
  } finally {
    modelStatsLoading.value = false
  }
}

const loadApiCallLogs = async () => {
  if (!currentUser.value) return
  try {
    logsLoading.value = true
    const res = await adminAPI.getUserApiCalls(
      currentUser.value.id,
      logsPagination.value.page,
      logsPagination.value.pageSize
    )
    apiCallLogs.value = res.data.records
    logsPagination.value.total = res.data.total
  } catch (error) {
    message.error('获取API调用日志失败')
  } finally {
    logsLoading.value = false
  }
}

const loadBalanceLogs = async () => {
  if (!currentUser.value) return
  try {
    balanceLogsLoading.value = true
    const res = await adminAPI.getUserBalanceLogs(
      currentUser.value.id,
      balanceLogsPagination.value.page,
      balanceLogsPagination.value.pageSize
    )
    balanceLogs.value = res.data.records
    balanceLogsPagination.value.total = res.data.total
  } catch (error) {
    message.error('获取余额日志失败')
  } finally {
    balanceLogsLoading.value = false
  }
}

const renderTokenTrendChart = (data: any[]) => {
  if (!tokenTrendChartRef.value) return

  if (tokenTrendChart) {
    tokenTrendChart.dispose()
  }

  tokenTrendChart = echarts.init(tokenTrendChartRef.value)

  const option = {
    title: {
      text: 'Token消耗趋势 (最近7天)',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['输入Token', '输出Token', '费用'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(item => item.date)
    },
    yAxis: [
      {
        type: 'value',
        name: 'Token数量',
        position: 'left'
      },
      {
        type: 'value',
        name: '费用(¥)',
        position: 'right'
      }
    ],
    series: [
      {
        name: '输入Token',
        type: 'line',
        data: data.map(item => item.inputTokens),
        smooth: true,
        itemStyle: { color: '#5470c6' }
      },
      {
        name: '输出Token',
        type: 'line',
        data: data.map(item => item.outputTokens),
        smooth: true,
        itemStyle: { color: '#91cc75' }
      },
      {
        name: '费用',
        type: 'bar',
        yAxisIndex: 1,
        data: data.map(item => item.cost),
        itemStyle: { color: '#fac858' }
      }
    ]
  }

  tokenTrendChart.setOption(option)
}

const renderModelPieChart = (data: ModelStatsResponse[]) => {
  if (!modelPieChartRef.value) return

  if (modelPieChart) {
    modelPieChart.dispose()
  }

  modelPieChart = echarts.init(modelPieChartRef.value)

  const option = {
    title: {
      text: '模型使用占比',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [
      {
        name: '调用次数',
        type: 'pie',
        radius: '50%',
        data: data.map(item => ({
          name: item.displayName,
          value: item.calls
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  modelPieChart.setOption(option)
}

const adjustBalance = (row: User) => {
  currentUser.value = row
  balanceForm.value = {
    type: 'add',
    amount: 0,
    reason: ''
  }
  balanceDialogVisible.value = true
}

const handleAdjustBalance = async () => {
  if (!currentUser.value) return

  if (!balanceForm.value.amount || balanceForm.value.amount <= 0) {
    message.warning('请输入有效的调整金额')
    return
  }

  if (!balanceForm.value.reason.trim()) {
    message.warning('请输入调整原因')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要${balanceForm.value.type === 'add' ? '增加' : '减少'}用户余额 ¥${balanceForm.value.amount} 吗?`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    adjusting.value = true
    const finalAmount = balanceForm.value.type === 'add' ? balanceForm.value.amount : -balanceForm.value.amount
    await adminAPI.adjustUserBalance(currentUser.value.id, {
      amount: finalAmount,
      remark: balanceForm.value.reason
    })
    message.success('余额调整成功')
    balanceDialogVisible.value = false
    await fetchUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || '调整失败')
    }
  } finally {
    adjusting.value = false
  }
}

const toggleUserStatus = async (row: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}该用户吗?`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await adminAPI.updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    message.success(`${row.status === 1 ? '禁用' : '启用'}成功`)
    await fetchUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error.response?.data?.message || `${row.status === 1 ? '禁用' : '启用'}失败`)
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.users-page {
  padding: 0;
}

:deep(.el-pagination) {
  display: flex;
}
</style>

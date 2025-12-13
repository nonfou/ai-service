<template>
  <div class="payment-success-page">
    <div class="container">
      <div class="result-card">
        <el-result
          v-if="loading"
          icon="info"
          title="正在确认支付..."
          sub-title="请稍候，正在验证您的支付状态"
        >
          <template #extra>
            <el-icon class="loading-icon"><Loading /></el-icon>
          </template>
        </el-result>

        <el-result
          v-else-if="success"
          icon="success"
          title="支付成功"
          :sub-title="`您已成功充值 $${amount.toFixed(2)}`"
        >
          <template #extra>
            <el-button type="primary" @click="goToWallet">返回钱包</el-button>
          </template>
        </el-result>

        <el-result
          v-else
          icon="error"
          title="支付失败"
          :sub-title="errorMessage"
        >
          <template #extra>
            <el-button type="primary" @click="goToWallet">返回钱包</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { queryRechargeOrderAPI } from '../api'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const success = ref(false)
const amount = ref(0)
const errorMessage = ref('支付未完成或已取消')

// 查询支付状态
const checkPaymentStatus = async () => {
  const paymentIntent = route.query.payment_intent as string
  const redirectStatus = route.query.redirect_status as string

  if (redirectStatus === 'succeeded') {
    // 从 URL 参数判断支付成功
    success.value = true
    loading.value = false
    return
  }

  // 如果有订单 ID，查询订单状态
  const orderId = route.query.order_id as string
  if (orderId) {
    try {
      const result = await queryRechargeOrderAPI(parseInt(orderId))
      if (result.order && result.order.status === 1) {
        success.value = true
        amount.value = result.order.amount
      } else {
        success.value = false
        errorMessage.value = '订单状态异常，请联系客服'
      }
    } catch (error: any) {
      success.value = false
      errorMessage.value = error.message || '查询订单失败'
    }
  } else if (paymentIntent) {
    // 有 payment_intent 但没有 succeeded 状态
    success.value = false
    errorMessage.value = '支付未完成，请重试'
  }

  loading.value = false
}

const goToWallet = () => {
  router.push('/wallet')
}

onMounted(() => {
  checkPaymentStatus()
})
</script>

<style scoped>
.payment-success-page {
  min-height: calc(100vh - 64px);
  padding: 80px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9d5ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 24px;
}

.result-card {
  background: white;
  border-radius: 24px;
  padding: 48px;
  box-shadow: 0 20px 60px rgba(124, 58, 237, 0.15);
  text-align: center;
}

.loading-icon {
  font-size: 48px;
  color: #7c3aed;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .result-card {
    padding: 32px 24px;
  }
}
</style>

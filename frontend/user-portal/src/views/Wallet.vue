<template>
  <div class="wallet-page">
    <h1>我的钱包</h1>

    <el-card class="balance-card">
      <div class="balance-info">
        <div class="label">当前余额</div>
        <div class="amount">¥{{ balance.toFixed(2) }}</div>
      </div>
      <el-button type="primary" @click="rechargeVisible = true">充值</el-button>
    </el-card>

    <el-dialog v-model="rechargeVisible" title="充值" width="500px">
      <el-form :model="rechargeForm">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeForm.amount" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-radio-group v-model="rechargeForm.payMethod">
            <el-radio label="alipay">支付宝</el-radio>
            <el-radio label="wechat">微信支付</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userAPI, rechargeAPI } from '../api'

const balance = ref(0)
const rechargeVisible = ref(false)
const rechargeForm = reactive({
  amount: 100,
  payMethod: 'alipay'
})

const loadBalance = async () => {
  try {
    const res = await userAPI.getBalance()
    balance.value = res.data.balance
  } catch (error) {
    ElMessage.error('获取余额失败')
  }
}

const handleRecharge = async () => {
  try {
    await rechargeAPI.createRecharge({
      amount: rechargeForm.amount,
      payMethod: rechargeForm.payMethod
    })
    ElMessage.success('充值订单创建成功')
    rechargeVisible.value = false
    loadBalance()
  } catch (error: any) {
    ElMessage.error('创建充值订单失败')
  }
}

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.wallet-page {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 2rem;
}

.balance-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2rem;
}

.balance-info .label {
  color: #666;
  margin-bottom: 0.5rem;
}

.balance-info .amount {
  font-size: 2.5rem;
  font-weight: bold;
  color: #409EFF;
}
</style>

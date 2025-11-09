<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>AI API Management Platform</h1>
        <p>Administrator Login</p>
      </div>

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="Username"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="Password"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            Login
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>&copy; 2024 AI API Platform. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { adminAPI } from '../api'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: 'Please enter username', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const res = await adminAPI.login({
        username: loginForm.username,
        password: loginForm.password
      })

      const data = res.data  // res 是 ApiResponse, res.data 是实际数据

      adminStore.setToken(data.token)
      adminStore.setAdminInfo({
        adminId: data.adminId,
        username: data.username,
        role: data.role
      })

      ElMessage.success('Login successful')
      router.push('/')
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || 'Login failed')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 28px;
  margin: 0 0 10px;
  color: var(--text-primary);
}

.login-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.login-form {
  margin-top: 30px;
}

.login-button {
  width: 100%;
}

.login-footer {
  margin-top: 30px;
  text-align: center;
}

.login-footer p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}
</style>

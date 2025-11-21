<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="background-animation">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="login-container">
      <!-- Logo 和品牌区域 -->
      <div class="brand-section">
        <div class="logo-container">
          <div class="logo-icon">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="40" height="40" rx="8" fill="url(#gradient-logo)"/>
              <path d="M20 10L28 16V24L20 30L12 24V16L20 10Z" fill="white" opacity="0.9"/>
              <defs>
                <linearGradient id="gradient-logo" x1="0" y1="0" x2="40" y2="40">
                  <stop offset="0%" stop-color="#3b82f6"/>
                  <stop offset="100%" stop-color="#2563eb"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="brand-title">X Coder</h1>
        </div>
        <p class="brand-subtitle">智能 API 服务平台，为开发者赋能</p>
      </div>

      <!-- 登录卡片 -->
      <div class="login-card">
        <div class="card-header">
          <h2 class="card-title">欢迎登录</h2>
          <p class="card-subtitle">使用邮箱验证码快速登录</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
          <el-form-item prop="email">
            <div class="form-group">
              <div class="form-label-row">
                <label class="form-label">邮箱地址</label>
              </div>
              <el-input
                v-model="form.email"
                placeholder="your@email.com"
                size="large"
                :prefix-icon="Message"
                class="modern-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="code">
            <div class="form-group">
              <div class="form-label-row">
                <label class="form-label">验证码</label>
                <el-button
                  :disabled="countdown > 0"
                  @click="handleSendCode"
                  size="small"
                  class="send-code-link"
                  type="primary"
                  link
                >
                  <span v-if="countdown > 0" class="countdown-text">{{ countdown }}s 后重新发送</span>
                  <span v-else>获取验证码</span>
                </el-button>
              </div>
              <el-input
                v-model="form.code"
                placeholder="输入 6 位验证码"
                size="large"
                :prefix-icon="Lock"
                class="modern-input"
                maxlength="6"
              />
            </div>
          </el-form-item>

          <el-form-item class="submit-section">
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              <span v-if="!loading">立即登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <div class="tip-item">
            <svg class="tip-icon" viewBox="0 0 16 16" fill="currentColor">
              <path fill-rule="evenodd" d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"/>
            </svg>
            <span>首次登录将自动创建账号</span>
          </div>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="page-footer">
        <p class="footer-text">
          登录即表示您同意我们的
          <a href="#" class="footer-link">服务条款</a>
          和
          <a href="#" class="footer-link">隐私政策</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import { authAPI } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 获取重定向目标
const redirectPath = ref(route.query.redirect as string || '/dashboard')
const formRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  code: ''
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.error('请先输入邮箱')
    return
  }

  try {
    await authAPI.sendCode({ email: form.email })
    ElMessage.success('验证码已发送,请查收邮件')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '发送验证码失败')
  }
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const res = await authAPI.login({
        email: form.email,
        code: form.code
      })

      // res 已经被响应拦截器处理过,格式为 { code, message, data }
      // 检查业务状态码
      if (!res || res.code !== 200) {
        throw new Error(res?.message || '登录失败')
      }

      // 检查数据是否存在
      if (!res.data) {
        throw new Error('登录响应数据为空')
      }

      const data = res.data

      // 验证必需字段
      // ❌ 删除: if (!data.token || ...) - token不再在响应中返回
      if (!data.userId || !data.email) {
        console.error('登录响应数据:', data)
        throw new Error('登录响应数据不完整')
      }

      // ❌ 删除: userStore.setToken(data.token)
      // ✅ 后端通过 Set-Cookie 设置 HttpOnly Cookie

      // 设置用户信息,包含所有后端返回的字段
      userStore.setUserInfo({
        userId: data.userId,
        email: data.email,
        username: data.username || data.email,
        // apiKey: data.apiKey,  // ❌ 已删除 - 不再在前端存储API密钥
        balance: data.balance
      })

      // ✅ 更新登录状态
      userStore.isLoggedIn = true

      ElMessage.success('登录成功')
      // 跳转到重定向目标或默认控制台
      router.push(redirectPath.value)
    } catch (error: any) {
      // 提取后端错误信息
      // error.response.data 是后端返回的 Result 对象: { code, message, data }
      let errorMessage = '登录失败,请检查验证码是否正确'

      if (error?.response?.data) {
        const result = error.response.data
        // 后端返回的 Result 对象中的 message 字段
        errorMessage = result.message || errorMessage
      } else if (error?.message) {
        // 其他类型的错误(如网络错误)
        errorMessage = error.message
      }

      // 在控制台打印完整错误以便调试
      console.error('Login failed:', error)
      console.error('Error response:', error?.response?.data)

      ElMessage.error(errorMessage)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-attachment: fixed;
}

/* ========== 动态背景动画 ========== */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 20s infinite ease-in-out;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  top: -10%;
  left: -10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  bottom: -5%;
  right: -5%;
  animation-delay: 7s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 14s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -50px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

/* ========== 主容器 ========== */
.login-container {
  width: 100%;
  max-width: 480px;
  padding: 0 var(--spacing-6);
  position: relative;
  z-index: 1;
}

/* ========== 品牌区域 ========== */
.brand-section {
  text-align: center;
  margin-bottom: var(--spacing-8);
  animation: fadeInDown 0.8s ease-out;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.logo-icon {
  width: 48px;
  height: 48px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.brand-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: white;
  margin: 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.brand-subtitle {
  font-size: var(--font-size-base);
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  font-weight: var(--font-weight-medium);
}

/* ========== 登录卡片 ========== */
.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-2xl);
  padding: var(--spacing-12);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  animation: fadeInUp 0.8s ease-out;
  transition: all var(--transition-slow);
}

.login-card:hover {
  box-shadow:
    0 25px 70px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 卡片头部 ========== */
.card-header {
  text-align: center;
  margin-bottom: var(--spacing-10);
}

.card-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-3);
  letter-spacing: -0.02em;
}

.card-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
  font-weight: var(--font-weight-normal);
}

/* ========== 表单样式 ========== */
.login-form {
  margin: 0;
}

.form-group {
  width: 100%;
}

.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-3);
}

.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: 0.01em;
}

.send-code-link {
  padding: 0;
  height: auto;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.send-code-link.is-disabled {
  color: var(--color-text-disabled);
}

.countdown-text {
  font-variant-numeric: tabular-nums;
}

/* 自定义 Element Plus 输入框样式 */
.login-form :deep(.el-input__wrapper) {
  background-color: var(--color-gray-50);
  border: 2px solid transparent;
  box-shadow: none;
  border-radius: var(--radius-lg);
  padding: var(--spacing-3) var(--spacing-4);
  transition: all var(--transition-base);
}

.login-form :deep(.el-input__wrapper:hover) {
  background-color: white;
  border-color: var(--color-primary-200);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background-color: white;
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.login-form :deep(.el-input__inner) {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: var(--color-text-disabled);
}

.login-form :deep(.el-input__prefix) {
  color: var(--color-text-tertiary);
}

.login-form :deep(.el-input__wrapper.is-focus .el-input__prefix) {
  color: var(--color-primary-500);
}

/* ========== 登录按钮 ========== */
.submit-section {
  margin-top: var(--spacing-8);
}

.login-button {
  width: 100%;
  height: 56px;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  border: none;
  border-radius: var(--radius-lg);
  color: white;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  letter-spacing: 0.02em;
}

.login-button:not(.is-loading):hover {
  background: linear-gradient(135deg, var(--color-primary-600) 0%, var(--color-primary-700) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.4);
}

.login-button:not(.is-loading):active {
  transform: translateY(0);
}

/* ========== 登录底部提示 ========== */
.login-footer {
  margin-top: var(--spacing-8);
  padding-top: var(--spacing-8);
  border-top: 1px solid var(--color-border-light);
}

.tip-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-normal);
  background: var(--color-primary-50);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
}

.tip-icon {
  width: 16px;
  height: 16px;
  color: var(--color-primary-600);
  flex-shrink: 0;
}

/* ========== 页面底部 ========== */
.page-footer {
  text-align: center;
  margin-top: var(--spacing-8);
  animation: fadeInUp 1s ease-out;
  animation-delay: 0.2s;
  animation-fill-mode: both;
}

.footer-text {
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.footer-link {
  color: white;
  text-decoration: underline;
  font-weight: var(--font-weight-semibold);
  transition: all var(--transition-fast);
}

.footer-link:hover {
  color: rgba(255, 255, 255, 1);
  text-decoration: none;
}

/* ========== 响应式设计 ========== */
@media (max-width: 640px) {
  .login-container {
    padding: 0 var(--spacing-4);
  }

  .login-card {
    padding: var(--spacing-10) var(--spacing-6);
  }

  .brand-title {
    font-size: var(--font-size-2xl);
  }

  .card-header {
    margin-bottom: var(--spacing-8);
  }

  .card-title {
    font-size: var(--font-size-2xl);
  }

  .login-form :deep(.el-form-item) {
    margin-bottom: var(--spacing-6);
  }

  .send-code-link {
    font-size: var(--font-size-xs);
  }

  .gradient-orb {
    filter: blur(60px);
  }

  .orb-1,
  .orb-2,
  .orb-3 {
    width: 300px;
    height: 300px;
  }
}

/* ========== 表单项间距调整 ========== */
.login-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-8);
}

.login-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.login-form :deep(.el-form-item__error) {
  margin-top: var(--spacing-2);
  font-size: var(--font-size-xs);
}

/* ========== 加载状态 ========== */
.login-button.is-loading {
  cursor: not-allowed;
  opacity: 0.8;
}
</style>

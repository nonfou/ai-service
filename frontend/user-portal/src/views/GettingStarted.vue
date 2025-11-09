<template>
  <div class="getting-started-page">
    <h1>快速开始</h1>
    <el-steps :active="1" align-center>
      <el-step title="注册账号" description="使用邮箱注册" />
      <el-step title="获取API密钥" description="在控制台创建" />
      <el-step title="开始调用" description="参考文档接入" />
    </el-steps>

    <el-card class="guide-card">
      <h2>1. 注册账号</h2>
      <p>使用您的邮箱进行注册,我们会发送验证码到您的邮箱。</p>
      <el-button type="primary" @click="$router.push('/login')">立即注册</el-button>
    </el-card>

    <el-card class="guide-card">
      <h2>2. 获取API密钥</h2>
      <p>登录后,在控制台创建您的API密钥。</p>
      <el-button @click="$router.push('/dashboard/api-key')">前往控制台</el-button>
    </el-card>

    <el-card class="guide-card">
      <h2>3. 开始调用</h2>
      <p>参考API文档,在您的应用中集成我们的API。</p>
      <el-button @click="$router.push('/documentation')">查看文档</el-button>
    </el-card>
  </div>
</template>

<style scoped>
.getting-started-page {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  margin-bottom: 3rem;
}

.el-steps {
  margin-bottom: 3rem;
}

.guide-card {
  margin-bottom: 2rem;
}

.guide-card h2 {
  margin-bottom: 1rem;
}

.guide-card p {
  margin-bottom: 1.5rem;
  color: #666;
  line-height: 1.6;
}
</style>

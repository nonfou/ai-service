<template>
  <div class="documentation-page">
    <h1>API文档</h1>

    <el-card class="doc-section">
      <h2>认证</h2>
      <p>所有API请求都需要在Header中携带API密钥:</p>
      <pre><code>Authorization: Bearer YOUR_API_KEY</code></pre>
    </el-card>

    <el-card class="doc-section">
      <h2>聊天接口</h2>
      <p>POST /api/chat</p>
      <pre><code>{
  "model": "gpt-3.5-turbo",
  "messages": [
    {"role": "user", "content": "Hello"}
  ]
}</code></pre>
    </el-card>

    <el-card class="doc-section">
      <h2>错误码</h2>
      <ul>
        <li>401 - 未授权</li>
        <li>403 - 余额不足</li>
        <li>429 - 请求过于频繁</li>
        <li>500 - 服务器错误</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped>
.documentation-page {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 2rem;
}

.doc-section {
  margin-bottom: 2rem;
}

.doc-section h2 {
  margin-bottom: 1rem;
  color: #409EFF;
}

.doc-section p {
  margin-bottom: 1rem;
  line-height: 1.6;
}

.doc-section pre {
  background: #f5f7fa;
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
}

.doc-section code {
  font-family: 'Courier New', monospace;
}

.doc-section ul {
  padding-left: 2rem;
}

.doc-section li {
  margin-bottom: 0.5rem;
  line-height: 1.6;
}
</style>

<template>
  <div class="getting-started-page">
    <!-- 动态网格背景 -->
    <div class="grid-background">
      <div class="grid-lines"></div>
      <div class="glow-effect glow-1"></div>
      <div class="glow-effect glow-2"></div>
    </div>

    <!-- 主容器 -->
    <div class="main-container">
      <!-- 左侧导航栏 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="logo-section">
            <div class="logo-icon">
              <el-icon><Promotion /></el-icon>
            </div>
            <h2>快速开始</h2>
          </div>
          <p class="sidebar-subtitle">选择您的接入方式</p>
        </div>

        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <div
            class="nav-item"
            :class="{ 'nav-item--active': activeSection === 'claude-code' }"
            @click="setActiveSection('claude-code')"
          >
            <div class="nav-item__icon nav-item__icon--claude">
              <span>C</span>
            </div>
            <div class="nav-item__content">
              <h3>Claude Code</h3>
              <p>官方 CLI 工具</p>
            </div>
            <el-tag v-if="activeSection === 'claude-code'" type="primary" size="small" effect="dark">
              <el-icon><Star /></el-icon>
              推荐
            </el-tag>
          </div>

          <div
            class="nav-item"
            :class="{ 'nav-item--active': activeSection === 'codex' }"
            @click="setActiveSection('codex')"
          >
            <div class="nav-item__icon nav-item__icon--codex">
              <span>Co</span>
            </div>
            <div class="nav-item__content">
              <h3>Codex 配置</h3>
              <p>轻量级方案</p>
            </div>
          </div>
        </nav>

        <!-- 侧边栏底部信息 -->
        <div class="sidebar-footer">
          <div class="quick-info">
            <el-icon><InfoFilled /></el-icon>
            <span>需要帮助? <a href="#" @click.prevent>查看文档</a></span>
          </div>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <main class="content-area">
        <!-- Claude Code 内容 -->
        <transition name="fade-slide" mode="out-in">
          <div v-if="activeSection === 'claude-code'" key="claude" class="content-section">
            <!-- 头部横幅 -->
            <div class="section-banner section-banner--claude">
              <div class="banner-content">
                <div class="banner-icon">
                  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                      <linearGradient id="banner-claude-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#667eea"/>
                        <stop offset="100%" stop-color="#764ba2"/>
                      </linearGradient>
                    </defs>
                    <rect x="20" y="30" width="80" height="60" rx="6" fill="url(#banner-claude-grad)" opacity="0.2"/>
                    <rect x="20" y="30" width="80" height="60" rx="6" fill="none" stroke="url(#banner-claude-grad)" stroke-width="2"/>
                    <circle cx="30" cy="40" r="2" fill="url(#banner-claude-grad)"/>
                    <circle cx="38" cy="40" r="2" fill="url(#banner-claude-grad)"/>
                    <circle cx="46" cy="40" r="2" fill="url(#banner-claude-grad)"/>
                    <text x="30" y="60" fill="url(#banner-claude-grad)" font-family="monospace" font-size="10">$ claude</text>
                    <path d="M 55 65 L 65 75 L 55 85" stroke="url(#banner-claude-grad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                    <path d="M 75 65 L 85 75 L 75 85" stroke="url(#banner-claude-grad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                  </svg>
                </div>
                <div class="banner-text">
                  <h1>Claude Code CLI</h1>
                  <p>最强大的 AI 编程助手,为开发者打造的官方命令行工具</p>
                  <div class="banner-features">
                    <span><el-icon><CircleCheck /></el-icon>交互式界面</span>
                    <span><el-icon><CircleCheck /></el-icon>代码生成</span>
                    <span><el-icon><CircleCheck /></el-icon>项目分析</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 配置步骤时间轴 -->
            <div class="steps-section">
              <h2 class="section-title">
                <el-icon><List /></el-icon>
                配置步骤
              </h2>

              <div class="steps-timeline">
                <!-- 步骤 1: 安装 Claude Code -->
                <div class="timeline-item">
                  <div class="timeline-marker">1</div>
                  <div class="timeline-content">
                    <h3>安装 Claude Code</h3>
                    <p>选择适合您操作系统的安装方式</p>

                    <el-tabs v-model="installTab" class="tech-tabs">
                      <el-tab-pane label="macOS" name="macos">
                        <div class="install-method">
                          <CodeBlock
                            :code="INSTALLATION_COMMANDS.homebrew.claudeCode"
                            language="bash"
                          />
                          <el-alert type="info" :closable="false" class="method-tip">
                            如果尚未安装 Homebrew,请先执行: <code>{{ INSTALLATION_COMMANDS.homebrew.install }}</code>
                          </el-alert>
                        </div>
                      </el-tab-pane>

                      <el-tab-pane label="Linux" name="linux">
                        <div class="install-method">
                          <CodeBlock
                            :code="INSTALLATION_COMMANDS.oneClick.unix"
                            language="bash"
                          />
                          <el-alert type="success" :closable="false" class="method-tip">
                            该命令会自动检测系统环境并完成安装配置
                          </el-alert>
                        </div>
                      </el-tab-pane>

                      <el-tab-pane label="Windows" name="windows">
                        <div class="install-method">
                          <CodeBlock
                            :code="INSTALLATION_COMMANDS.oneClick.windows"
                            language="powershell"
                          />
                          <el-alert type="warning" :closable="false" class="method-tip">
                            请在 PowerShell 中以管理员身份运行此命令
                          </el-alert>
                        </div>
                      </el-tab-pane>
                    </el-tabs>
                  </div>
                </div>

                <!-- 步骤 2: 配置 API -->
                <div class="timeline-item">
                  <div class="timeline-marker">2</div>
                  <div class="timeline-content">
                    <h3>配置 API</h3>
                    <p>在 Claude Code 配置文件中设置 API Base URL 和 API Key</p>

                    <div class="config-file-section">
                      <div class="file-path-display">
                        <el-icon><Setting /></el-icon>
                        <span>配置文件位置: <code>~/.claude/config.json</code></span>
                      </div>

                      <CodeBlock
                        :code="getClaudeCodeConfig()"
                        language="json"
                      />

                      <div v-if="!apiKey" class="api-key-reminder">
                        <el-icon><Warning /></el-icon>
                        <span>登录后可自动获取 API Key</span>
                        <el-button type="primary" size="small" @click="$router.push('/login')">
                          立即登录
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 步骤 3: 开始使用 -->
                <div class="timeline-item">
                  <div class="timeline-marker">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <div class="timeline-content">
                    <h3>启动 Claude Code</h3>
                    <p>在终端中执行以下命令</p>
                    <CodeBlock
                      :code="INSTALLATION_COMMANDS.start"
                      language="bash"
                    />
                    <div class="success-message">
                      <el-icon><SuccessFilled /></el-icon>
                      <span>现在您可以开始使用 Claude Code 了!</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Codex 内容 -->
          <div v-else-if="activeSection === 'codex'" key="codex" class="content-section">
            <!-- 头部横幅 -->
            <div class="section-banner section-banner--codex">
              <div class="banner-content">
                <div class="banner-icon">
                  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                      <linearGradient id="banner-codex-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#11998e"/>
                        <stop offset="100%" stop-color="#38ef7d"/>
                      </linearGradient>
                    </defs>
                    <rect x="35" y="20" width="50" height="80" rx="4" fill="url(#banner-codex-grad)" opacity="0.2"/>
                    <rect x="35" y="20" width="50" height="80" rx="4" fill="none" stroke="url(#banner-codex-grad)" stroke-width="2"/>
                    <line x1="45" y1="35" x2="75" y2="35" stroke="url(#banner-codex-grad)" stroke-width="2"/>
                    <line x1="45" y1="45" x2="68" y2="45" stroke="url(#banner-codex-grad)" stroke-width="2"/>
                    <line x1="45" y1="55" x2="75" y2="55" stroke="url(#banner-codex-grad)" stroke-width="2"/>
                    <line x1="45" y1="65" x2="65" y2="65" stroke="url(#banner-codex-grad)" stroke-width="2"/>
                    <path d="M 48 75 Q 45 80 48 85" stroke="url(#banner-codex-grad)" stroke-width="2" fill="none"/>
                    <path d="M 72 75 Q 75 80 72 85" stroke="url(#banner-codex-grad)" stroke-width="2" fill="none"/>
                    <circle cx="60" cy="80" r="2" fill="url(#banner-codex-grad)"/>
                  </svg>
                </div>
                <div class="banner-text">
                  <h1>OpenAI Codex CLI</h1>
                  <p>轻量级编码代理,可在终端中读取、修改和运行代码</p>
                  <div class="banner-features">
                    <span><el-icon><CircleCheck /></el-icon>命令行工具</span>
                    <span><el-icon><CircleCheck /></el-icon>支持多模型</span>
                    <span><el-icon><CircleCheck /></el-icon>轻量高效</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 配置步骤 -->
            <div class="steps-section">
              <h2 class="section-title">
                <el-icon><List /></el-icon>
                配置步骤
              </h2>

              <div class="steps-timeline">
                <!-- 步骤 1: 安装 Node.js 和 npm -->
                <div class="timeline-item">
                  <div class="timeline-marker">1</div>
                  <div class="timeline-content">
                    <h3>安装 Node.js 和 npm</h3>
                    <p>Codex CLI 通过 npm 安装,首先需要安装 Node.js 环境</p>

                    <el-tabs v-model="codexInstallTab" class="tech-tabs">
                      <el-tab-pane label="Ubuntu/Debian" name="ubuntu">
                        <div class="install-method">
                          <h4 class="method-title">更新系统包</h4>
                          <CodeBlock
                            code="sudo apt update && sudo apt upgrade -y"
                            language="bash"
                          />

                          <h4 class="method-title">添加 NodeSource 仓库</h4>
                          <CodeBlock
                            code="curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -"
                            language="bash"
                          />

                          <h4 class="method-title">安装 Node.js 和 npm</h4>
                          <CodeBlock
                            code="sudo apt install nodejs -y"
                            language="bash"
                          />

                          <h4 class="method-title">验证安装</h4>
                          <CodeBlock
                            code="node --version  # 应显示 v22.x.x\nnpm --version   # 应显示 10.x.x 或更高"
                            language="bash"
                          />
                        </div>
                      </el-tab-pane>

                      <el-tab-pane label="macOS" name="macos">
                        <div class="install-method">
                          <h4 class="method-title">使用 Homebrew 安装</h4>
                          <CodeBlock
                            code="brew install node"
                            language="bash"
                          />

                          <h4 class="method-title">验证安装</h4>
                          <CodeBlock
                            code="node --version\nnpm --version"
                            language="bash"
                          />
                        </div>
                      </el-tab-pane>

                      <el-tab-pane label="Windows (WSL)" name="windows">
                        <div class="install-method">
                          <el-alert type="warning" :closable="false" class="method-tip">
                            Windows 用户强烈推荐使用 WSL (Windows Subsystem for Linux),然后按照 Ubuntu 的步骤安装
                          </el-alert>
                        </div>
                      </el-tab-pane>
                    </el-tabs>
                  </div>
                </div>

                <!-- 步骤 2: 安装 Codex CLI -->
                <div class="timeline-item">
                  <div class="timeline-marker">2</div>
                  <div class="timeline-content">
                    <h3>安装 Codex CLI</h3>
                    <p>通过 npm 全局安装 Codex CLI</p>

                    <CodeBlock
                      code="sudo npm install -g @openai/codex@latest"
                      language="bash"
                    />

                    <h4 class="method-title">验证安装</h4>
                    <CodeBlock
                      code="codex --version  # 应显示版本号,如 0.40.0"
                      language="bash"
                    />
                  </div>
                </div>

                <!-- 步骤 3: 配置 API Key -->
                <div class="timeline-item">
                  <div class="timeline-marker">3</div>
                  <div class="timeline-content">
                    <h3>配置 API Key</h3>
                    <p>将 API Key 写入环境变量</p>

                    <h4 class="method-title">临时保存 (当前会话)</h4>
                    <CodeBlock
                      :code="getCodexEnvCommand(false)"
                      language="bash"
                    />

                    <h4 class="method-title">永久保存 (推荐)</h4>
                    <p class="config-hint">编辑 <code>~/.bashrc</code> 或 <code>~/.zshrc</code> 文件,添加:</p>
                    <CodeBlock
                      :code="getCodexEnvCommand(false)"
                      language="bash"
                    />
                    <p class="config-hint">然后运行 <code>source ~/.bashrc</code> 或 <code>source ~/.zshrc</code> 使其生效</p>

                    <div v-if="!apiKey" class="api-key-reminder">
                      <el-icon><Warning /></el-icon>
                      <span>登录后可自动获取 API Key</span>
                      <el-button type="primary" size="small" @click="$router.push('/login')">
                        立即登录
                      </el-button>
                    </div>
                  </div>
                </div>

                <!-- 步骤 4: 配置 config.toml -->
                <div class="timeline-item">
                  <div class="timeline-marker">4</div>
                  <div class="timeline-content">
                    <h3>配置 config.toml</h3>
                    <p>在 Codex 配置文件中设置模型和 API 提供商</p>

                    <div class="config-file-section">
                      <div class="file-path-display">
                        <el-icon><Setting /></el-icon>
                        <span>配置文件: <code>~/.codex/config.toml</code></span>
                      </div>

                      <CodeBlock
                        :code="getCodexConfigToml()"
                        language="toml"
                      />

                      <el-alert type="info" :closable="false" class="method-tip">
                        你可以将 model 改成其他支持的模型,如 <code>gpt-5-codex</code>、<code>claude-3-5-sonnet-20241022</code> 等
                      </el-alert>
                    </div>
                  </div>
                </div>

                <!-- 步骤 5: 开始使用 -->
                <div class="timeline-item">
                  <div class="timeline-marker">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <div class="timeline-content">
                    <h3>开始使用 Codex</h3>
                    <p>导航到你的代码目录并启动 Codex</p>

                    <h4 class="method-title">启动 Codex</h4>
                    <CodeBlock
                      code="cd /path/to/your/project\ncodex"
                      language="bash"
                    />

                    <h4 class="method-title">示例命令</h4>
                    <ul class="command-list">
                      <li>基本提示: <code>codex "修复这个 bug:在 main.py 的第 42 行添加错误处理"</code></li>
                      <li>指定模型: <code>codex -m gpt-5-codex "构建一个简单 Web 服务器"</code></li>
                      <li>模式指定: <code>codex --mode full-auto "部署 ML 模型到 Vercel"</code></li>
                      <li>退出: 输入 <code>/exit</code> 或按 <code>Ctrl+C</code></li>
                    </ul>

                    <div class="success-message">
                      <el-icon><SuccessFilled /></el-icon>
                      <span>现在您可以开始使用 Codex CLI 了!</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  Star, Download, Setting, Promotion, Key, Lightning,
  CircleCheck, InfoFilled, Warning, SuccessFilled, List
} from '@element-plus/icons-vue'

// 导入组件
import CodeBlock from '@/components/GettingStarted/CodeBlock.vue'

// 导入常量
import {
  API_BASE_URL,
  INSTALLATION_COMMANDS,
  ENV_COMMANDS
} from '@/constants/installation'

const router = useRouter()

// 状态管理
const activeSection = ref<'claude-code' | 'codex'>('claude-code')
const installTab = ref('macos')
const envTab = ref('unix')
const codexInstallTab = ref('ubuntu')
const apiKey = ref('')

// 切换激活部分
const setActiveSection = (section: 'claude-code' | 'codex') => {
  activeSection.value = section
}

// 获取环境变量命令
const getEnvCommand = (platform: 'unix' | 'windows'): string => {
  const key = apiKey.value || 'your-api-key-here'
  return ENV_COMMANDS[platform](key)
}

// 获取 Claude Code 配置
const getClaudeCodeConfig = (): string => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    "api": {
      "baseURL": API_BASE_URL,
      "key": key
    }
  }, null, 2)
}

// 获取 Continue 配置
const getContinueConfig = (): string => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    "title": "Claude (自定义)",
    "provider": "anthropic",
    "model": "claude-3-5-sonnet-20241022",
    "apiKey": key,
    "apiBase": API_BASE_URL
  }, null, 2)
}

// 获取 Cursor 配置
const getCursorConfig = (): string => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    "model": "claude-3-5-sonnet-20241022",
    "apiKey": key,
    "baseURL": API_BASE_URL
  }, null, 2)
}

// 获取 Claude Desktop 配置
const getClaudeDesktopConfig = (): string => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    "apiKey": key,
    "apiUrl": API_BASE_URL
  }, null, 2)
}

// 获取 Codex 环境变量命令
const getCodexEnvCommand = (permanent: boolean): string => {
  const key = apiKey.value || 'sk-your-api-key-here'
  return `export OPENAI_API_KEY="${key}"`
}

// 获取 Codex config.toml 配置
const getCodexConfigToml = (): string => {
  return `model = "gpt-4o"
model_provider = "bianxieai"

[model_providers.bianxieai]
name = "bianxieai"
base_url = "${API_BASE_URL}/v1"
env_key = "OPENAI_API_KEY"
wire_api = "chat"`
}

// 获取 Codex auth.json 配置 (备选方案)
const getCodexAuthJson = (): string => {
  const key = apiKey.value || 'sk-your-api-key-here'
  return JSON.stringify({
    "OPENAI_API_KEY": key
  }, null, 2)
}

// 获取 API Key
const fetchApiKey = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const response = await axios.get('/api/user/api-key', {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data && response.data.apiKey) {
      apiKey.value = response.data.apiKey
    }
  } catch (error) {
    console.error('Failed to fetch API key:', error)
  }
}

onMounted(() => {
  fetchApiKey()
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.getting-started-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  color: #2d3748;
  overflow: hidden;
  animation: page-fade-in 0.6s ease-out;
}

@keyframes page-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ========== 动态网格背景 ========== */
.grid-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: grid-move 20s linear infinite;
}

@keyframes grid-move {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(50px, 50px);
  }
}

.glow-effect {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
  animation: glow-pulse 8s ease-in-out infinite;
}

.glow-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 70%);
  top: -200px;
  right: -100px;
  animation-delay: 0s;
}

.glow-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(17, 153, 142, 0.15) 0%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation-delay: 4s;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.1);
  }
}

/* ========== 主容器 ========== */
.main-container {
  position: relative;
  display: flex;
  max-width: 1800px;
  margin: 0 auto;
  min-height: 100vh;
  z-index: 1;
}

/* ========== 左侧边栏 ========== */
.sidebar {
  width: 320px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(99, 102, 241, 0.1);
  display: flex;
  flex-direction: column;
  padding: 2rem 0;
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  padding: 0 2rem 2rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  animation: logo-rotate 10s linear infinite;
}

@keyframes logo-rotate {
  0%, 90%, 100% {
    transform: rotate(0deg) scale(1);
  }
  95% {
    transform: rotate(360deg) scale(1.1);
  }
}

.sidebar-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-subtitle {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0;
}

/* ========== 导航菜单 ========== */
.nav-menu {
  flex: 1;
  padding: 2rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.nav-item::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.05);
  transform: translate(-50%, -50%);
  transition: width 0.6s ease, height 0.6s ease;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-item:hover {
  background: white;
  border-color: rgba(99, 102, 241, 0.2);
  transform: translateX(4px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
}

.nav-item:hover::after {
  width: 300px;
  height: 300px;
}

.nav-item--active {
  background: rgba(99, 102, 241, 0.05);
  border-color: rgba(99, 102, 241, 0.3);
}

.nav-item--active::before {
  opacity: 1;
}

.nav-item__icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.2rem;
  color: white;
  flex-shrink: 0;
}

.nav-item__icon--claude {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.nav-item:hover .nav-item__icon--claude {
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.6);
  transform: scale(1.05) rotate(-3deg);
}

.nav-item__icon--codex {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.4);
  transition: all 0.3s ease;
}

.nav-item:hover .nav-item__icon--codex {
  box-shadow: 0 8px 24px rgba(17, 153, 142, 0.6);
  transform: scale(1.05) rotate(-3deg);
}

.nav-item__content {
  flex: 1;
}

.nav-item__content h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 0.25rem 0;
  color: #1e293b;
}

.nav-item__content p {
  font-size: 0.85rem;
  color: #64748b;
  margin: 0;
}

/* ========== 侧边栏底部 ========== */
.sidebar-footer {
  padding: 0 2rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 2rem;
}

.quick-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #64748b;
}

.quick-info a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.quick-info a:hover {
  text-decoration: underline;
}

/* ========== 右侧内容区 ========== */
.content-area {
  flex: 1;
  padding: 3rem;
  overflow-y: auto;
}

.content-section {
  max-width: 900px;
  margin: 0 auto;
}

/* ========== 横幅区域 ========== */
.section-banner {
  padding: 3rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 20px;
  margin-bottom: 3rem;
  position: relative;
  overflow: hidden;
  animation: banner-slide-in 0.6s ease-out;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
}

@keyframes banner-slide-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.section-banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
}

.section-banner--claude::before {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.section-banner--codex::before {
  background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%);
}

.banner-content {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.banner-icon {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  animation: icon-float 3s ease-in-out infinite;
}

@keyframes icon-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.banner-icon svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 16px rgba(99, 102, 241, 0.3));
}

.banner-text h1 {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 0.5rem 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: title-shimmer 3s ease-in-out infinite;
  background-size: 200% auto;
}

@keyframes title-shimmer {
  0%, 100% {
    background-position: 0% center;
  }
  50% {
    background-position: 100% center;
  }
}

.section-banner--codex .banner-text h1 {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.banner-text p {
  font-size: 1.1rem;
  color: #64748b;
  margin: 0 0 1rem 0;
}

.banner-features {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.banner-features span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #475569;
  padding: 0.5rem 1rem;
  background: rgba(99, 102, 241, 0.08);
  border-radius: 20px;
  transition: all 0.3s ease;
}

.banner-features span:hover {
  background: rgba(99, 102, 241, 0.15);
  transform: translateY(-2px);
}

.banner-features .el-icon {
  color: #667eea;
  font-size: 1.1rem;
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* ========== 区块标题 ========== */
.section-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 1.5rem 0;
  color: #1e293b;
}

.section-title .el-icon {
  font-size: 1.6rem;
  color: #667eea;
}

/* ========== 各个内容区块 ========== */
.quick-install,
.config-section,
.start-section,
.download-section,
.steps-section {
  margin-bottom: 3rem;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  transition: all 0.3s ease;
  animation: section-fade-in 0.6s ease-out backwards;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
}

.quick-install {
  animation-delay: 0.1s;
}

.config-section {
  animation-delay: 0.2s;
}

.start-section,
.download-section {
  animation-delay: 0.3s;
}

.steps-section {
  animation-delay: 0.4s;
}

@keyframes section-fade-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.quick-install:hover,
.config-section:hover,
.start-section:hover,
.download-section:hover,
.steps-section:hover {
  border-color: rgba(99, 102, 241, 0.2);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.12);
}

.install-content,
.config-content {
  margin-top: 1rem;
}

.platform-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid rgba(99, 102, 241, 0.1);
}

.install-method {
  margin-top: 0.5rem;
}

.method-tip {
  margin-top: 1rem;
}

.method-tip code {
  background: rgba(99, 102, 241, 0.1);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: #667eea;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9rem;
}

.config-file-section {
  margin-top: 1rem;
}

.file-path-display {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: rgba(99, 102, 241, 0.05);
  border-left: 3px solid rgba(99, 102, 241, 0.4);
  border-radius: 8px;
  margin-bottom: 1rem;
  font-size: 0.95rem;
  color: #475569;
}

.file-path-display .el-icon {
  color: #667eea;
  font-size: 1.2rem;
}

.file-path-display code {
  background: rgba(99, 102, 241, 0.1);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  color: #667eea;
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 600;
}

.tool-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.tool-card {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid rgba(99, 102, 241, 0.1);
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.tool-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
}

.tool-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.tool-card-header h4 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.tool-desc {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0;
}

.install-tips {
  margin-top: 1rem;
}

.config-hint {
  margin-top: 1rem;
  color: #64748b;
  font-size: 0.95rem;
}

.config-hint code {
  background: rgba(99, 102, 241, 0.08);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: #667eea;
  font-family: 'Consolas', 'Monaco', monospace;
}

.method-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 1rem 0 0.5rem 0;
}

.command-list {
  list-style: none;
  padding: 0;
  margin: 1rem 0;
}

.command-list li {
  padding: 0.75rem;
  background: rgba(99, 102, 241, 0.05);
  border-left: 3px solid rgba(99, 102, 241, 0.4);
  border-radius: 4px;
  margin-bottom: 0.5rem;
  color: #475569;
  font-size: 0.95rem;
  transition: all 0.3s ease;
  line-height: 1.6;
}

.command-list li:hover {
  background: rgba(99, 102, 241, 0.1);
  border-left-color: rgba(99, 102, 241, 0.8);
  transform: translateX(4px);
}

.command-list code {
  background: rgba(99, 102, 241, 0.1);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  color: #667eea;
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 600;
}

.api-key-reminder {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1rem;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  border-radius: 8px;
  color: #fbbf24;
  animation: reminder-pulse 2s ease-in-out infinite;
}

@keyframes reminder-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(251, 191, 36, 0);
  }
}

.success-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
  padding: 1rem;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 8px;
  color: #22c55e;
  font-weight: 500;
  animation: success-slide-in 0.5s ease-out;
}

@keyframes success-slide-in {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* ========== 时间轴 ========== */
.steps-timeline {
  margin-top: 2rem;
}

.timeline-item {
  display: flex;
  gap: 1.5rem;
  padding-bottom: 2rem;
  position: relative;
}

.timeline-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 20px;
  top: 44px;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.5) 0%, transparent 100%);
}

.timeline-marker {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 1.1rem;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  animation: marker-pop-in 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55) backwards;
}

.timeline-item:nth-child(1) .timeline-marker {
  animation-delay: 0.1s;
}

.timeline-item:nth-child(2) .timeline-marker {
  animation-delay: 0.2s;
}

.timeline-item:nth-child(3) .timeline-marker {
  animation-delay: 0.3s;
}

.timeline-item:nth-child(4) .timeline-marker {
  animation-delay: 0.4s;
}

@keyframes marker-pop-in {
  from {
    transform: scale(0) rotate(-180deg);
    opacity: 0;
  }
  to {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
}

.timeline-item:hover .timeline-marker {
  transform: scale(1.15) rotate(5deg);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.6);
}

.timeline-content {
  flex: 1;
  padding-top: 0.5rem;
}

.timeline-content h3 {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  color: #1e293b;
}

.timeline-content p {
  color: #64748b;
  margin: 0 0 1rem 0;
}

.path-list {
  list-style: none;
  padding: 0;
  margin: 1rem 0 0 0;
}

.path-list li {
  padding: 0.75rem;
  background: rgba(99, 102, 241, 0.05);
  border-left: 3px solid rgba(99, 102, 241, 0.4);
  border-radius: 4px;
  margin-bottom: 0.5rem;
  color: #475569;
  font-size: 0.95rem;
  transition: all 0.3s ease;
  cursor: pointer;
}

.path-list li:hover {
  background: rgba(99, 102, 241, 0.1);
  border-left-color: rgba(99, 102, 241, 0.8);
  transform: translateX(8px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.path-list code {
  background: rgba(99, 102, 241, 0.08);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  color: #667eea;
  font-family: 'Consolas', 'Monaco', monospace;
  margin-left: 0.5rem;
}

/* ========== 过渡动画 ========== */
.fade-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(30px) scale(0.95);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.95);
}

/* ========== Tech Tabs 样式 ========== */
.tech-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.tech-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  height: 3px;
}

.tech-tabs :deep(.el-tabs__item) {
  color: #64748b;
  font-weight: 600;
}

.tech-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
}

.tech-tabs :deep(.el-tabs__item:hover) {
  color: #5a67d8;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .sidebar {
    width: 280px;
  }

  .content-area {
    padding: 2rem;
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
  }

  .banner-text h1 {
    font-size: 2rem;
  }
}

@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(99, 102, 241, 0.2);
  }

  .content-area {
    padding: 1.5rem;
  }

  .section-banner {
    padding: 2rem;
  }

  .banner-text h1 {
    font-size: 1.75rem;
  }

  .timeline-item {
    gap: 1rem;
  }

  .timeline-marker {
    width: 36px;
    height: 36px;
    font-size: 0.95rem;
  }
}

/* ========== 工具类 ========== */
.mt-2 {
  margin-top: 0.5rem;
}
</style>

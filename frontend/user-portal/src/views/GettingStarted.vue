<template>
  <div class="getting-started-page">
    <!-- 页面头部装饰 -->
    <div class="page-header-decoration"></div>

    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="header-content">
        <el-icon class="header-icon-large"><Rocket /></el-icon>
        <h1>快速开始</h1>
        <p class="subtitle">选择你喜欢的AI工具开始使用</p>
      </div>
    </div>

    <!-- Claude Code 卡片及安装指南 -->
    <div class="ai-card-wrapper">
      <el-card class="ai-card claude-card" shadow="hover">
        <div class="card-header">
          <div class="card-title-section">
            <div class="tool-logo claude-logo">
              <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="40" height="40" rx="8" fill="url(#claude-gradient)"/>
                <path d="M20 10L28 18L20 26L12 18L20 10Z" fill="white" opacity="0.9"/>
                <defs>
                  <linearGradient id="claude-gradient" x1="0" y1="0" x2="40" y2="40">
                    <stop offset="0%" stop-color="#667eea"/>
                    <stop offset="100%" stop-color="#764ba2"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <div>
              <h2>Claude Code</h2>
              <p class="description">官方命令行界面工具,为开发者提供强大的AI辅助编程能力</p>
            </div>
          </div>
          <el-tag type="primary" size="large" effect="dark">
            <el-icon><Star /></el-icon>
            推荐
          </el-tag>
        </div>
        <div class="card-actions">
          <el-button
            type="primary"
            size="large"
            @click="toggleSection('claude')"
            :icon="expandedSection === 'claude' ? ArrowUp : Download"
          >
            {{ expandedSection === 'claude' ? '收起指南' : '开始安装' }}
          </el-button>
        </div>
      </el-card>

      <!-- Claude Code 安装指南 -->
      <el-collapse-transition>
        <el-card v-show="expandedSection === 'claude'" class="installation-guide claude-guide" shadow="never">
          <div class="guide-header">
            <el-icon class="guide-icon"><Guide /></el-icon>
            <h3>安装 Claude Code</h3>
          </div>

          <!-- 一键安装 -->
          <div class="section">
            <div class="section-title">
              <el-icon><Download /></el-icon>
              <h4>一键安装命令</h4>
            </div>
            <p class="section-desc">复制以下命令到终端执行,即可自动完成安装:</p>

            <el-tabs v-model="claudeInstallTab" class="install-tabs">
              <el-tab-pane name="unix">
                <template #label>
                  <span class="tab-label">
                    <el-icon><Monitor /></el-icon>
                    macOS / Linux
                  </span>
                </template>
                <div class="code-block">
                  <div class="code-header">
                    <span class="code-lang">bash</span>
                  </div>
                  <pre><code>curl -fsSL https://cli.anthropic.com/install.sh | sh</code></pre>
                  <el-button size="small" type="primary" @click="copyToClipboard('curl -fsSL https://cli.anthropic.com/install.sh | sh')">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
                <div class="tip success-tip">
                  <el-icon><SuccessFilled /></el-icon>
                  <span>该命令会自动检测系统环境并完成安装配置</span>
                </div>
              </el-tab-pane>

              <el-tab-pane name="windows">
                <template #label>
                  <span class="tab-label">
                    <el-icon><Platform /></el-icon>
                    Windows
                  </span>
                </template>
                <div class="code-block">
                  <div class="code-header">
                    <span class="code-lang">powershell</span>
                  </div>
                  <pre><code>irm https://cli.anthropic.com/install.ps1 | iex</code></pre>
                  <el-button size="small" type="primary" @click="copyToClipboard('irm https://cli.anthropic.com/install.ps1 | iex')">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
                <div class="tip info-tip">
                  <el-icon><InfoFilled /></el-icon>
                  <span>请在 PowerShell 中运行此命令</span>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 手动安装步骤 -->
          <div class="section">
            <div class="section-title">
              <el-icon><Tools /></el-icon>
              <h4>手动安装步骤</h4>
            </div>
            <el-tabs v-model="claudeManualTab" class="platform-tabs">
              <el-tab-pane label="macOS" name="macos">
                <div class="steps">
                  <div class="step">
                    <div class="step-number">
                      <el-icon><Download /></el-icon>
                      <span>1</span>
                    </div>
                    <div class="step-content">
                      <h5>通过 Homebrew 安装</h5>
                      <p>如果尚未安装 Homebrew,先执行:</p>
                      <div class="code-block">
                        <pre><code>{{ getHomebrewInstallCommand() }}</code></pre>
                        <el-button size="small" @click="copyToClipboard(getHomebrewInstallCommand())">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                      <p>然后安装 Claude Code:</p>
                      <div class="code-block">
                        <pre><code>brew install anthropics/claude/claude</code></pre>
                        <el-button size="small" @click="copyToClipboard('brew install anthropics/claude/claude')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <div class="step">
                    <div class="step-number">
                      <el-icon><CircleCheck /></el-icon>
                      <span>2</span>
                    </div>
                    <div class="step-content">
                      <h5>验证安装</h5>
                      <div class="code-block">
                        <pre><code>claude --version</code></pre>
                        <el-button size="small" @click="copyToClipboard('claude --version')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="Linux" name="linux">
                <div class="steps">
                  <div class="step">
                    <div class="step-number">
                      <el-icon><Download /></el-icon>
                      <span>1</span>
                    </div>
                    <div class="step-content">
                      <h5>下载并安装</h5>
                      <div class="code-block">
                        <pre><code>curl -fsSL https://cli.anthropic.com/install.sh | sh</code></pre>
                        <el-button size="small" @click="copyToClipboard('curl -fsSL https://cli.anthropic.com/install.sh | sh')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <div class="step">
                    <div class="step-number">
                      <el-icon><FolderOpened /></el-icon>
                      <span>2</span>
                    </div>
                    <div class="step-content">
                      <h5>添加到 PATH (如果需要)</h5>
                      <div class="code-block">
                        <pre><code>{{ getPathCommand() }}</code></pre>
                        <el-button size="small" @click="copyToClipboard(getPathCommand())">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <div class="step">
                    <div class="step-number">
                      <el-icon><CircleCheck /></el-icon>
                      <span>3</span>
                    </div>
                    <div class="step-content">
                      <h5>验证安装</h5>
                      <div class="code-block">
                        <pre><code>claude --version</code></pre>
                        <el-button size="small" @click="copyToClipboard('claude --version')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="Windows" name="windows">
                <div class="steps">
                  <div class="step">
                    <div class="step-number">
                      <el-icon><Download /></el-icon>
                      <span>1</span>
                    </div>
                    <div class="step-content">
                      <h5>使用 PowerShell 安装</h5>
                      <p>以管理员身份运行 PowerShell,然后执行:</p>
                      <div class="code-block">
                        <pre><code>irm https://cli.anthropic.com/install.ps1 | iex</code></pre>
                        <el-button size="small" @click="copyToClipboard('irm https://cli.anthropic.com/install.ps1 | iex')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <div class="step">
                    <div class="step-number">
                      <el-icon><CircleCheck /></el-icon>
                      <span>2</span>
                    </div>
                    <div class="step-content">
                      <h5>验证安装</h5>
                      <div class="code-block">
                        <pre><code>claude --version</code></pre>
                        <el-button size="small" @click="copyToClipboard('claude --version')">
                          <el-icon><CopyDocument /></el-icon>
                          复制
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 环境变量配置 -->
          <div class="section">
            <div class="section-title">
              <el-icon><Key /></el-icon>
              <h4>配置环境变量</h4>
            </div>
            <p class="section-desc">设置 API Base URL 和 API Key:</p>

            <el-tabs v-model="claudeEnvTab" class="platform-tabs">
              <el-tab-pane label="macOS / Linux" name="unix">
                <h5>临时配置 (当前终端会话)</h5>
                <div class="code-block">
                  <pre><code>{{ getUnixEnvCommand() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getUnixEnvCommand())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>

                <h5>永久配置</h5>
                <p>将以下内容添加到 <code>~/.bashrc</code> 或 <code>~/.zshrc</code>:</p>
                <div class="code-block">
                  <pre><code>{{ getUnixEnvCommand() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getUnixEnvCommand())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
                <div class="tip info-tip">
                  <el-icon><InfoFilled /></el-icon>
                  <span>修改后执行 <code>source ~/.bashrc</code> 或 <code>source ~/.zshrc</code> 使配置生效</span>
                </div>
              </el-tab-pane>

              <el-tab-pane label="Windows" name="windows">
                <h5>临时配置 (当前 PowerShell 会话)</h5>
                <div class="code-block">
                  <pre><code>{{ getWindowsEnvCommand() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getWindowsEnvCommand())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>

                <h5>永久配置</h5>
                <p>使用以下命令设置系统环境变量:</p>
                <div class="code-block">
                  <pre><code>{{ getWindowsPermanentEnvCommand() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getWindowsPermanentEnvCommand())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
            </el-tabs>

            <div v-if="!apiKey" class="api-key-hint">
              <el-icon><Warning /></el-icon>
              <div>
                <p>登录后可自动获取您的 API Key</p>
                <el-button type="warning" size="small" @click="$router.push('/login')">
                  立即登录
                </el-button>
              </div>
            </div>
          </div>

          <!-- 开始使用 -->
          <div class="section">
            <div class="section-title">
              <el-icon><Promotion /></el-icon>
              <h4>开始使用</h4>
            </div>
            <div class="code-block">
              <pre><code>claude</code></pre>
              <el-button size="small" @click="copyToClipboard('claude')">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
            <div class="tip success-tip">
              <el-icon><SuccessFilled /></el-icon>
              <span>执行此命令即可启动 Claude Code 交互式界面</span>
            </div>
          </div>
        </el-card>
      </el-collapse-transition>
    </div>

    <!-- Codex 卡片及安装指南 -->
    <div class="ai-card-wrapper">
      <el-card class="ai-card codex-card" shadow="hover">
        <div class="card-header">
          <div class="card-title-section">
            <div class="tool-logo codex-logo">
              <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="40" height="40" rx="8" fill="url(#codex-gradient)"/>
                <path d="M12 12H28V16H12V12Z M12 18H28V22H12V18Z M12 24H22V28H12V24Z" fill="white" opacity="0.9"/>
                <defs>
                  <linearGradient id="codex-gradient" x1="0" y1="0" x2="40" y2="40">
                    <stop offset="0%" stop-color="#11998e"/>
                    <stop offset="100%" stop-color="#38ef7d"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <div>
              <h2>Codex</h2>
              <p class="description">轻量级配置方案,通过配置文件快速接入各类AI工具</p>
            </div>
          </div>
        </div>
        <div class="card-actions">
          <el-button
            type="success"
            size="large"
            @click="toggleSection('codex')"
            :icon="expandedSection === 'codex' ? ArrowUp : Setting"
          >
            {{ expandedSection === 'codex' ? '收起指南' : '开始配置' }}
          </el-button>
        </div>
      </el-card>

      <!-- Codex 安装指南 -->
      <el-collapse-transition>
        <el-card v-show="expandedSection === 'codex'" class="installation-guide codex-guide" shadow="never">
          <div class="guide-header">
            <el-icon class="guide-icon"><Setting /></el-icon>
            <h3>配置 Codex</h3>
          </div>

          <!-- 下载配置文件 -->
          <div class="section">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <h4>下载配置文件</h4>
            </div>
            <p class="section-desc">选择适合您的 AI 工具的配置文件:</p>

            <div class="config-files">
              <div class="config-file-card claude-config">
                <div class="config-card-header">
                  <div class="config-logo claude-mini-logo">C</div>
                  <h5>Claude Desktop</h5>
                </div>
                <p>使用 Claude 官方 API 的配置文件</p>
                <el-button type="primary" size="small" @click="downloadConfig('claude')">
                  <el-icon><Download /></el-icon>
                  下载配置
                </el-button>
              </div>

              <div class="config-file-card continue-config">
                <div class="config-card-header">
                  <div class="config-logo continue-mini-logo">Co</div>
                  <h5>Continue</h5>
                </div>
                <p>适用于 Continue 插件的配置文件</p>
                <el-button type="primary" size="small" @click="downloadConfig('continue')">
                  <el-icon><Download /></el-icon>
                  下载配置
                </el-button>
              </div>

              <div class="config-file-card cursor-config">
                <div class="config-card-header">
                  <div class="config-logo cursor-mini-logo">Cu</div>
                  <h5>Cursor</h5>
                </div>
                <p>适用于 Cursor 编辑器的配置文件</p>
                <el-button type="primary" size="small" @click="downloadConfig('cursor')">
                  <el-icon><Download /></el-icon>
                  下载配置
                </el-button>
              </div>
            </div>
          </div>

          <!-- 安装步骤 -->
          <div class="section">
            <div class="section-title">
              <el-icon><List /></el-icon>
              <h4>安装步骤</h4>
            </div>
            <div class="steps">
              <div class="step">
                <div class="step-number codex-step">
                  <el-icon><Download /></el-icon>
                  <span>1</span>
                </div>
                <div class="step-content">
                  <h5>下载配置文件</h5>
                  <p>根据您使用的工具,点击上方对应的下载按钮获取配置文件</p>
                </div>
              </div>

              <div class="step">
                <div class="step-number codex-step">
                  <el-icon><Edit /></el-icon>
                  <span>2</span>
                </div>
                <div class="step-content">
                  <h5>修改配置</h5>
                  <p>在配置文件中填入您的 API Key:</p>
                  <div class="code-block">
                    <pre><code>{
  "baseURL": "https://api.xcoder.plus",
  "apiKey": "your-api-key-here"
}</code></pre>
                  </div>
                  <div v-if="apiKey" class="api-key-display">
                    <p>您的 API Key:</p>
                    <div class="code-block inline">
                      <code>{{ apiKey }}</code>
                      <el-button size="small" @click="copyToClipboard(apiKey)">
                        <el-icon><CopyDocument /></el-icon>
                        复制
                      </el-button>
                    </div>
                  </div>
                  <div v-else class="api-key-hint">
                    <el-icon><Warning /></el-icon>
                    <div>
                      <p>登录后可查看您的 API Key</p>
                      <el-button type="warning" size="small" @click="$router.push('/login')">
                        立即登录
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="step">
                <div class="step-number codex-step">
                  <el-icon><FolderOpened /></el-icon>
                  <span>3</span>
                </div>
                <div class="step-content">
                  <h5>放置配置文件</h5>
                  <p>将配置文件放到对应工具的配置目录:</p>
                  <ul class="path-list">
                    <li>
                      <el-icon><Document /></el-icon>
                      <strong>Claude Desktop:</strong> <code>~/.config/claude/config.json</code>
                    </li>
                    <li>
                      <el-icon><Document /></el-icon>
                      <strong>Continue:</strong> <code>~/.continue/config.json</code>
                    </li>
                    <li>
                      <el-icon><Document /></el-icon>
                      <strong>Cursor:</strong> <code>~/.cursor/config.json</code>
                    </li>
                  </ul>
                </div>
              </div>

              <div class="step">
                <div class="step-number codex-step">
                  <el-icon><RefreshRight /></el-icon>
                  <span>4</span>
                </div>
                <div class="step-content">
                  <h5>重启应用</h5>
                  <p>重启对应的应用程序使配置生效</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 配置示例 -->
          <div class="section">
            <div class="section-title">
              <el-icon><View /></el-icon>
              <h4>完整配置示例</h4>
            </div>
            <el-tabs v-model="codexConfigTab" class="config-tabs">
              <el-tab-pane name="claude">
                <template #label>
                  <span class="tab-label">
                    <span class="config-logo-mini claude-mini-logo">C</span>
                    Claude Desktop
                  </span>
                </template>
                <div class="code-block">
                  <div class="code-header">
                    <span class="code-lang">json</span>
                  </div>
                  <pre><code>{{ getClaudeConfig() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getClaudeConfig())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>

              <el-tab-pane name="continue">
                <template #label>
                  <span class="tab-label">
                    <span class="config-logo-mini continue-mini-logo">Co</span>
                    Continue
                  </span>
                </template>
                <div class="code-block">
                  <div class="code-header">
                    <span class="code-lang">json</span>
                  </div>
                  <pre><code>{{ getContinueConfig() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getContinueConfig())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>

              <el-tab-pane name="cursor">
                <template #label>
                  <span class="tab-label">
                    <span class="config-logo-mini cursor-mini-logo">Cu</span>
                    Cursor
                  </span>
                </template>
                <div class="code-block">
                  <div class="code-header">
                    <span class="code-lang">json</span>
                  </div>
                  <pre><code>{{ getCursorConfig() }}</code></pre>
                  <el-button size="small" @click="copyToClipboard(getCursorConfig())">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-collapse-transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Rocket, Star, Download, ArrowUp, Setting, Guide, Monitor, Platform,
  CopyDocument, SuccessFilled, InfoFilled, Warning, Tools, CircleCheck,
  FolderOpened, Key, Promotion, Document, List, Edit, RefreshRight, View
} from '@element-plus/icons-vue'
import axios from 'axios'

// 展开状态
const expandedSection = ref<string>('')

// Tab 状态
const claudeInstallTab = ref('unix')
const claudeManualTab = ref('macos')
const claudeEnvTab = ref('unix')
const codexConfigTab = ref('claude')

// API Key
const apiKey = ref('')

// 切换展开/收起
const toggleSection = (section: string) => {
  expandedSection.value = expandedSection.value === section ? '' : section
}

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

// 获取 Homebrew 安装命令
const getHomebrewInstallCommand = () => {
  return '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
}

// 获取 PATH 命令
const getPathCommand = () => {
  return 'export PATH="$HOME/.claude/bin:$PATH"'
}

// 获取 Unix 环境变量命令
const getUnixEnvCommand = () => {
  const key = apiKey.value || 'your-api-key-here'
  return `export ANTHROPIC_BASE_URL="https://api.xcoder.plus"\nexport ANTHROPIC_API_KEY="${key}"`
}

// 获取 Windows 环境变量命令
const getWindowsEnvCommand = () => {
  const key = apiKey.value || 'your-api-key-here'
  return `$env:ANTHROPIC_BASE_URL="https://api.xcoder.plus"\n$env:ANTHROPIC_API_KEY="${key}"`
}

// 获取 Windows 永久环境变量命令
const getWindowsPermanentEnvCommand = () => {
  const key = apiKey.value || 'your-api-key-here'
  return `[System.Environment]::SetEnvironmentVariable("ANTHROPIC_BASE_URL", "https://api.xcoder.plus", "User")\n[System.Environment]::SetEnvironmentVariable("ANTHROPIC_API_KEY", "${key}", "User")`
}

// 获取 Claude 配置
const getClaudeConfig = () => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    baseURL: 'https://api.xcoder.plus',
    apiKey: key,
    model: 'claude-3-5-sonnet-20241022'
  }, null, 2)
}

// 获取 Continue 配置
const getContinueConfig = () => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    models: [{
      title: 'Claude 3.5 Sonnet',
      provider: 'anthropic',
      model: 'claude-3-5-sonnet-20241022',
      apiKey: key,
      apiBase: 'https://api.xcoder.plus'
    }]
  }, null, 2)
}

// 获取 Cursor 配置
const getCursorConfig = () => {
  const key = apiKey.value || 'your-api-key-here'
  return JSON.stringify({
    anthropic: {
      apiKey: key,
      baseURL: 'https://api.xcoder.plus'
    }
  }, null, 2)
}

// 下载配置文件
const downloadConfig = (type: string) => {
  let config = ''
  let filename = ''

  switch (type) {
    case 'claude':
      config = getClaudeConfig()
      filename = 'claude-config.json'
      break
    case 'continue':
      config = getContinueConfig()
      filename = 'continue-config.json'
      break
    case 'cursor':
      config = getCursorConfig()
      filename = 'cursor-config.json'
      break
  }

  const blob = new Blob([config], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('配置文件已下载')
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
/* 基础布局 */
.getting-started-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 3rem 2rem;
  position: relative;
}

/* 页面头部装饰 */
.page-header-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 50%, #11998e 100%);
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 3rem;
  padding: 2rem 0;
  position: relative;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
  pointer-events: none;
  z-index: -1;
}

.header-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.header-icon-large {
  font-size: 3.5rem;
  color: #667eea;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #666;
  font-size: 1.1rem;
  margin: 0;
}

/* AI 卡片容器 */
.ai-card-wrapper {
  margin-bottom: 2rem;
}

/* 卡片样式 */
.ai-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.ai-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.claude-card::before {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.codex-card::before {
  background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%);
}

.ai-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.ai-card:hover::before {
  opacity: 1;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.card-title-section {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.tool-logo {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
}

.tool-logo svg {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header h2 {
  font-size: 1.8rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  color: #333;
}

.description {
  color: #666;
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
}

.card-header .el-tag {
  flex-shrink: 0;
}

.card-actions {
  display: flex;
  gap: 1rem;
}

/* 安装指南 */
.installation-guide {
  margin-top: 1rem;
  border: none;
  position: relative;
  overflow: hidden;
}

.claude-guide {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.03) 100%);
  border-left: 4px solid #667eea;
}

.codex-guide {
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.03) 0%, rgba(56, 239, 125, 0.03) 100%);
  border-left: 4px solid #11998e;
}

.guide-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.guide-icon {
  font-size: 1.8rem;
  color: #667eea;
}

.codex-guide .guide-icon {
  color: #11998e;
}

.installation-guide h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

/* 区块 */
.section {
  margin-bottom: 2.5rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.section:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.section-title .el-icon {
  font-size: 1.3rem;
  color: #667eea;
}

.codex-guide .section-title .el-icon {
  color: #11998e;
}

.section h4 {
  font-size: 1.3rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.section h5 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 1.5rem 0 0.75rem 0;
  color: #444;
}

.section-desc {
  color: #666;
  margin-bottom: 1rem;
  line-height: 1.6;
}

/* 代码块 */
.code-block {
  position: relative;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 1rem;
  margin: 1rem 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.code-block:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.code-header {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
}

.code-lang {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  color: #888;
  font-size: 0.75rem;
  border-radius: 4px;
  text-transform: uppercase;
  font-weight: 600;
}

.code-block pre {
  margin: 0;
  overflow-x: auto;
  padding-top: 1.5rem;
}

.code-block code {
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.code-block .el-button {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}

.code-block.inline {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: #2d2d2d;
}

.code-block.inline pre {
  padding-top: 0;
}

.code-block.inline code {
  flex: 1;
}

/* 步骤 */
.steps {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.step {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: rgba(102, 126, 234, 0.02);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.codex-guide .step {
  background: rgba(17, 153, 142, 0.02);
}

.step:hover {
  background: rgba(102, 126, 234, 0.05);
  transform: translateX(4px);
}

.codex-guide .step:hover {
  background: rgba(17, 153, 142, 0.05);
}

.step-number {
  position: relative;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1.1rem;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.step-number.codex-step {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.3);
}

.step-number .el-icon {
  position: absolute;
  top: -4px;
  right: -4px;
  font-size: 1rem;
  background: white;
  border-radius: 50%;
  padding: 2px;
}

.step-content {
  flex: 1;
}

/* 提示框 */
.tip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 0.9rem;
}

.success-tip {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(103, 194, 58, 0.05) 100%);
  border-left: 3px solid #67c23a;
  color: #529b2e;
}

.info-tip {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(64, 158, 255, 0.05) 100%);
  border-left: 3px solid #409eff;
  color: #337ecc;
}

.tip .el-icon {
  font-size: 1.1rem;
}

/* API Key 提示 */
.api-key-hint {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding: 1rem;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.1) 0%, rgba(255, 193, 7, 0.05) 100%);
  border-left: 3px solid #ffc107;
  border-radius: 8px;
}

.api-key-hint .el-icon {
  font-size: 1.5rem;
  color: #f39c12;
  flex-shrink: 0;
}

.api-key-hint p {
  margin: 0 0 0.5rem 0;
  color: #856404;
  font-weight: 500;
}

.api-key-display {
  margin-top: 1rem;
}

.api-key-display p {
  margin-bottom: 0.5rem;
  font-weight: 600;
}

/* 配置文件卡片 */
.config-files {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin: 1rem 0;
}

.config-file-card {
  padding: 1.5rem;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.config-file-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.claude-config:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
}

.continue-config:hover {
  border-color: #f39c12;
  box-shadow: 0 8px 24px rgba(243, 156, 18, 0.2);
}

.cursor-config:hover {
  border-color: #3498db;
  box-shadow: 0 8px 24px rgba(52, 152, 219, 0.2);
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.config-logo {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 0.9rem;
}

.claude-mini-logo {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.continue-mini-logo {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
}

.cursor-mini-logo {
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
}

.config-file-card h5 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0;
}

.config-file-card p {
  color: #666;
  font-size: 0.9rem;
  margin: 0.5rem 0 1rem 0;
  line-height: 1.5;
}

/* 路径列表 */
.path-list {
  list-style: none;
  padding: 0;
  margin: 1rem 0;
}

.path-list li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: rgba(102, 126, 234, 0.02);
  border-radius: 6px;
  margin-bottom: 0.5rem;
  transition: all 0.3s ease;
}

.codex-guide .path-list li {
  background: rgba(17, 153, 142, 0.02);
}

.path-list li:hover {
  background: rgba(102, 126, 234, 0.05);
  transform: translateX(4px);
}

.codex-guide .path-list li:hover {
  background: rgba(17, 153, 142, 0.05);
}

.path-list li .el-icon {
  color: #667eea;
  font-size: 1.1rem;
}

.codex-guide .path-list li .el-icon {
  color: #11998e;
}

.path-list li code {
  background: #f5f5f5;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.85rem;
  color: #e74c3c;
}

/* Tabs */
.install-tabs,
.platform-tabs,
.config-tabs {
  margin: 1rem 0;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.config-logo-mini {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 0.7rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .getting-started-page {
    padding: 2rem 1rem;
  }

  .header-icon-large {
    font-size: 2.5rem;
  }

  h1 {
    font-size: 2rem;
  }

  .subtitle {
    font-size: 1rem;
  }

  .card-header {
    flex-direction: column;
    gap: 1rem;
  }

  .card-title-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .config-files {
    grid-template-columns: 1fr;
  }

  .step {
    flex-direction: column;
  }

  .code-block .el-button {
    position: static;
    margin-top: 0.5rem;
    width: 100%;
  }

  .code-block pre {
    padding-top: 0;
  }

  .code-header {
    position: static;
    margin-bottom: 0.5rem;
  }
}

@media (max-width: 480px) {
  .card-actions {
    width: 100%;
  }

  .card-actions .el-button {
    flex: 1;
  }
}
</style>

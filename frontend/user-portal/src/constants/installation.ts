import type { Platform } from '@/composables/usePlatformDetect'

/**
 * 安装相关的常量配置
 */

// API 基础 URL
export const API_BASE_URL = 'https://api.xcoder.plus'

// 安装命令
export const INSTALLATION_COMMANDS = {
  // 一键安装命令
  oneClick: {
    unix: 'curl -fsSL https://cli.anthropic.com/install.sh | sh',
    windows: 'irm https://cli.anthropic.com/install.ps1 | iex'
  },

  // Homebrew 安装
  homebrew: {
    install: '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"',
    claudeCode: 'brew install anthropics/claude/claude'
  },

  // 验证安装
  verify: 'claude --version',

  // 启动 Claude Code
  start: 'claude'
}

// 环境变量命令
export const ENV_COMMANDS = {
  unix: (apiKey: string) => `export ANTHROPIC_BASE_URL="${API_BASE_URL}"
export ANTHROPIC_API_KEY="${apiKey}"`,

  windows: (apiKey: string) => `$env:ANTHROPIC_BASE_URL="${API_BASE_URL}"
$env:ANTHROPIC_API_KEY="${apiKey}"`,

  windowsPermanent: (apiKey: string) => `[System.Environment]::SetEnvironmentVariable("ANTHROPIC_BASE_URL", "${API_BASE_URL}", "User")
[System.Environment]::SetEnvironmentVariable("ANTHROPIC_API_KEY", "${apiKey}", "User")`
}

// PATH 命令
export const PATH_COMMANDS = {
  unix: 'export PATH="$HOME/.claude/bin:$PATH"',
  bashrc: 'echo \'export PATH="$HOME/.claude/bin:$PATH"\' >> ~/.bashrc && source ~/.bashrc',
  zshrc: 'echo \'export PATH="$HOME/.claude/bin:$PATH"\' >> ~/.zshrc && source ~/.zshrc'
}

// 安装步骤定义
export interface InstallationStep {
  id: string
  title: string
  description: string
  commands?: Record<Platform, string | string[]>
  notes?: Record<Platform, string>
}

export const CLAUDE_CODE_STEPS: InstallationStep[] = [
  {
    id: 'install',
    title: '安装 Claude Code',
    description: '选择适合您操作系统的安装方式',
    commands: {
      macos: INSTALLATION_COMMANDS.homebrew.claudeCode,
      linux: INSTALLATION_COMMANDS.oneClick.unix,
      windows: INSTALLATION_COMMANDS.oneClick.windows
    },
    notes: {
      macos: '推荐使用 Homebrew 安装,确保已安装 Homebrew',
      linux: '该命令会自动检测系统环境并完成安装配置',
      windows: '请在 PowerShell 中运行此命令'
    }
  },
  {
    id: 'verify',
    title: '验证安装',
    description: '检查 Claude Code 是否正确安装',
    commands: {
      macos: INSTALLATION_COMMANDS.verify,
      linux: INSTALLATION_COMMANDS.verify,
      windows: INSTALLATION_COMMANDS.verify
    }
  },
  {
    id: 'configure',
    title: '配置环境变量',
    description: '设置 API Base URL 和 API Key',
    commands: {
      macos: ENV_COMMANDS.unix('your-api-key'),
      linux: ENV_COMMANDS.unix('your-api-key'),
      windows: ENV_COMMANDS.windows('your-api-key')
    },
    notes: {
      macos: '添加到 ~/.zshrc 或 ~/.bashrc 以永久保存',
      linux: '添加到 ~/.bashrc 以永久保存',
      windows: '使用 SetEnvironmentVariable 可永久保存'
    }
  },
  {
    id: 'start',
    title: '开始使用',
    description: '启动 Claude Code 交互式界面',
    commands: {
      macos: INSTALLATION_COMMANDS.start,
      linux: INSTALLATION_COMMANDS.start,
      windows: INSTALLATION_COMMANDS.start
    }
  }
]

// 配置文件路径
export const CONFIG_PATHS = {
  claude: '~/.config/claude/config.json',
  continue: '~/.continue/config.json',
  cursor: '~/.cursor/config.json'
}

// AI 工具配置
export interface AIToolConfig {
  id: string
  name: string
  description: string
  icon: string
  color: string
  gradient: string
  recommended?: boolean
}

export const AI_TOOLS: AIToolConfig[] = [
  {
    id: 'claude-code',
    name: 'Claude Code',
    description: '官方命令行工具,为开发者提供强大的 AI 编程助手能力',
    icon: 'C',
    color: '#667eea',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    recommended: true
  },
  {
    id: 'codex',
    name: 'Codex',
    description: '轻量级配置方案,快速接入各类 AI 开发工具',
    icon: 'Co',
    color: '#11998e',
    gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)'
  }
]

// 常见问题
export interface FAQ {
  question: string
  answer: string
  platform?: Platform[]
}

export const FAQS: FAQ[] = [
  {
    question: '如何获取 API Key?',
    answer: '登录后在个人中心即可查看和管理您的 API Key。'
  },
  {
    question: '支持哪些模型?',
    answer: '目前支持 Claude 3.5 Sonnet、Claude 3 Opus 等多个模型,详见模型页面。'
  },
  {
    question: '安装失败怎么办?',
    answer: '请检查网络连接,确保有足够的权限。如果问题持续,请联系技术支持。',
    platform: ['windows']
  },
  {
    question: 'Homebrew 安装很慢怎么办?',
    answer: '可以尝试更换 Homebrew 镜像源,或使用一键安装脚本。',
    platform: ['macos']
  }
]

// 提示信息
export const TIPS = {
  apiKeyRequired: '登录后可自动获取您的 API Key',
  envPermanent: '记得将环境变量添加到配置文件以永久保存',
  pathSetup: '如果命令未找到,请检查 PATH 环境变量配置',
  windowsAdmin: 'Windows 用户建议以管理员身份运行 PowerShell'
}

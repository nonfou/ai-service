import { ref, onMounted } from 'vue'

export type Platform = 'macos' | 'linux' | 'windows'

export interface PlatformInfo {
  platform: Platform
  userAgent: string
  confidence: number // 0-1, 检测置信度
}

/**
 * 平台检测 Composable
 * 自动检测用户的操作系统
 */
export function usePlatformDetect() {
  const detectedPlatform = ref<Platform | null>(null)
  const platformInfo = ref<PlatformInfo | null>(null)

  /**
   * 检测操作系统平台
   */
  const detectPlatform = (): PlatformInfo | null => {
    if (typeof window === 'undefined' || !window.navigator) {
      return null
    }

    const userAgent = window.navigator.userAgent.toLowerCase()
    const platform = (window.navigator as any).userAgentData?.platform?.toLowerCase() ||
                     window.navigator.platform?.toLowerCase() || ''

    // macOS 检测
    if (
      userAgent.includes('mac') ||
      platform.includes('mac') ||
      userAgent.includes('darwin')
    ) {
      return {
        platform: 'macos',
        userAgent,
        confidence: 0.95
      }
    }

    // Windows 检测
    if (
      userAgent.includes('win') ||
      platform.includes('win') ||
      userAgent.includes('windows')
    ) {
      return {
        platform: 'windows',
        userAgent,
        confidence: 0.95
      }
    }

    // Linux 检测
    if (
      userAgent.includes('linux') ||
      platform.includes('linux') ||
      userAgent.includes('x11') ||
      userAgent.includes('ubuntu') ||
      userAgent.includes('debian') ||
      userAgent.includes('fedora') ||
      userAgent.includes('arch')
    ) {
      return {
        platform: 'linux',
        userAgent,
        confidence: 0.9
      }
    }

    // Android/iOS 等移动设备,返回 null
    if (userAgent.includes('android') || userAgent.includes('iphone') || userAgent.includes('ipad')) {
      return null
    }

    // 默认返回 null(无法确定)
    return null
  }

  /**
   * 获取平台友好名称
   */
  const getPlatformName = (platform: Platform): string => {
    const names: Record<Platform, string> = {
      macos: 'macOS',
      linux: 'Linux',
      windows: 'Windows'
    }
    return names[platform] || platform
  }

  /**
   * 检查是否为特定平台
   */
  const isPlatform = (platform: Platform): boolean => {
    return detectedPlatform.value === platform
  }

  /**
   * 获取推荐的 shell
   */
  const getRecommendedShell = (): string => {
    switch (detectedPlatform.value) {
      case 'macos':
        return 'zsh'
      case 'linux':
        return 'bash'
      case 'windows':
        return 'powershell'
      default:
        return 'bash'
    }
  }

  /**
   * 获取推荐的包管理器
   */
  const getRecommendedPackageManager = (): string => {
    switch (detectedPlatform.value) {
      case 'macos':
        return 'homebrew'
      case 'linux':
        return 'apt/yum'
      case 'windows':
        return 'winget/choco'
      default:
        return 'unknown'
    }
  }

  // 在组件挂载时自动检测
  onMounted(() => {
    const info = detectPlatform()
    if (info) {
      platformInfo.value = info
      detectedPlatform.value = info.platform
    }
  })

  return {
    detectedPlatform,
    platformInfo,
    detectPlatform,
    getPlatformName,
    isPlatform,
    getRecommendedShell,
    getRecommendedPackageManager
  }
}

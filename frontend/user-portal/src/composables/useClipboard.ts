import { ElMessage } from 'element-plus'

/**
 * 剪贴板复制功能的 Composable
 */
export function useClipboard() {
  /**
   * 复制文本到剪贴板
   * @param text 要复制的文本
   * @param successMessage 成功提示消息
   * @param errorMessage 失败提示消息
   * @returns Promise<boolean> 是否成功
   */
  const copy = async (
    text: string,
    successMessage: string = '已复制到剪贴板',
    errorMessage: string = '复制失败,请手动复制'
  ): Promise<boolean> => {
    try {
      // 尝试使用现代 Clipboard API
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text)
        if (successMessage) {
          ElMessage.success(successMessage)
        }
        return true
      }

      // 降级方案: 使用 execCommand (已弃用但兼容性更好)
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed'
      textArea.style.left = '-999999px'
      textArea.style.top = '-999999px'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()

      const successful = document.execCommand('copy')
      textArea.remove()

      if (successful) {
        if (successMessage) {
          ElMessage.success(successMessage)
        }
        return true
      } else {
        throw new Error('execCommand failed')
      }
    } catch (err) {
      console.error('复制失败:', err)
      if (errorMessage) {
        ElMessage.error(errorMessage)
      }
      return false
    }
  }

  /**
   * 批量复制多个文本(用换行符连接)
   * @param texts 要复制的文本数组
   * @param separator 分隔符,默认为换行符
   * @param successMessage 成功提示消息
   * @returns Promise<boolean> 是否成功
   */
  const copyMultiple = async (
    texts: string[],
    separator: string = '\n',
    successMessage: string = '已复制所有命令到剪贴板'
  ): Promise<boolean> => {
    const combinedText = texts.join(separator)
    return copy(combinedText, successMessage)
  }

  /**
   * 读取剪贴板内容
   * @returns Promise<string | null> 剪贴板内容,失败返回 null
   */
  const read = async (): Promise<string | null> => {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        const text = await navigator.clipboard.readText()
        return text
      }
      return null
    } catch (err) {
      console.error('读取剪贴板失败:', err)
      return null
    }
  }

  /**
   * 检查是否支持剪贴板 API
   * @returns boolean 是否支持
   */
  const isSupported = (): boolean => {
    return !!(navigator.clipboard && window.isSecureContext)
  }

  return {
    copy,
    copyMultiple,
    read,
    isSupported
  }
}

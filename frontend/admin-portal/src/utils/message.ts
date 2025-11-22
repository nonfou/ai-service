import { ElMessage, type MessageHandler, type MessageOptions } from 'element-plus'

type MessageType = 'success' | 'warning' | 'info' | 'error'

const DEFAULT_COOLDOWN = 2000
const recentMessages = new Map<string, number>()

interface OnceMessageOptions extends MessageOptions {
  key?: string
  cooldown?: number
  force?: boolean
}

type MessageInput = string | (OnceMessageOptions & { message: string })

const buildKey = (type: MessageType, message: string, customKey?: string) =>
  customKey || `${type}-${message}`

const shouldSkip = (key: string, cooldown: number, force?: boolean) => {
  if (force) return false
  const lastShown = recentMessages.get(key)
  if (!lastShown) return false
  return Date.now() - lastShown < cooldown
}

const parseInput = (
  input: MessageInput,
  extraOptions?: OnceMessageOptions
): { message: string; options: OnceMessageOptions } => {
  if (typeof input === 'string') {
    return { message: input, options: extraOptions ?? {} }
  }
  const { message, ...rest } = input
  return {
    message,
    options: {
      ...rest,
      ...(extraOptions ?? {})
    }
  }
}

const showMessage = (
  type: MessageType,
  input: MessageInput,
  extraOptions?: OnceMessageOptions
): MessageHandler | null => {
  const { message, options } = parseInput(input, extraOptions)
  if (!message) return null

  const {
    key,
    cooldown = DEFAULT_COOLDOWN,
    force,
    onClose,
    ...elMessageOptions
  } = options

  const dedupeKey = buildKey(type, message, key)

  if (shouldSkip(dedupeKey, cooldown, force)) {
    return null
  }

  recentMessages.set(dedupeKey, Date.now())

  return ElMessage({
    ...elMessageOptions,
    message,
    type,
    onClose: () => {
      recentMessages.delete(dedupeKey)
      onClose?.()
    }
  })
}

export const resetMessage = (key?: string) => {
  if (key) {
    recentMessages.delete(key)
  } else {
    recentMessages.clear()
  }
}

const message = {
  success: (input: MessageInput, options?: OnceMessageOptions) =>
    showMessage('success', input, options),
  error: (input: MessageInput, options?: OnceMessageOptions) =>
    showMessage('error', input, options),
  warning: (input: MessageInput, options?: OnceMessageOptions) =>
    showMessage('warning', input, options),
  info: (input: MessageInput, options?: OnceMessageOptions) =>
    showMessage('info', input, options),
  reset: resetMessage
}

export default message

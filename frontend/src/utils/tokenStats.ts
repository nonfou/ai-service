const integerFormatter = new Intl.NumberFormat('zh-CN')
const compactFormatter = new Intl.NumberFormat('zh-CN', {
  notation: 'compact',
  maximumFractionDigits: 1
})

const toSafeNumber = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return 0
  }
  return value
}

const pad = (value: number) => String(value).padStart(2, '0')

export const TOKEN_STATS_DAY_OPTIONS = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 }
] as const

export const normalizeTokenStatsDays = (value: unknown): 7 | 30 => {
  const raw = Array.isArray(value) ? value[0] : value
  const parsed = Number(raw)
  return parsed === 30 ? 30 : 7
}

export const formatNumber = (value?: number | null) => integerFormatter.format(toSafeNumber(value))

export const formatCompactNumber = (value?: number | null) => compactFormatter.format(toSafeNumber(value))

export const formatTokenStatValue = (key: string, value?: number | null) => {
  if (key === 'averageDurationMs') {
    return formatDuration(value)
  }
  if (key.includes('Tokens')) {
    return formatCompactNumber(value)
  }
  return formatNumber(value)
}

export const formatDuration = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '-'
  }
  if (value < 1000) {
    return `${Math.round(value)}ms`
  }

  const totalSeconds = value / 1000
  if (totalSeconds < 60) {
    return `${totalSeconds.toFixed(totalSeconds < 10 ? 2 : 1)}s`
  }

  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes < 60) {
    const fractionDigits = seconds >= 10 ? 0 : 1
    return `${minutes}m ${seconds.toFixed(fractionDigits)}s`
  }

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return `${hours}h ${remainingMinutes}m`
}

export const formatRequestType = (value?: string | null) => {
  if (!value) {
    return '-'
  }

  const normalized = value.trim().toLowerCase()
  if (normalized === 'stream') {
    return '流式'
  }
  if (normalized === 'non_stream') {
    return '非流式'
  }
  return value
}

export const formatSuccessLabel = (value?: boolean | null) => {
  if (value == null) {
    return '-'
  }
  return value ? '成功' : '失败'
}

export const formatDateTime = (value?: string | Date | null) => {
  if (!value) {
    return '-'
  }

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value : '-'
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export const extractDownloadFileName = (
  headers: Record<string, unknown>,
  fallback: string
) => {
  const contentDisposition = headers['content-disposition']
  if (typeof contentDisposition !== 'string') {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1] || fallback
}

export const downloadBlobFile = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

import { message } from '@hmfw/ant-design'
import type { LoginResponse, R } from '@/types/api'

const BASE_URL = '/api'
const TOKEN_KEY = 'admin_token'
const TOKEN_EXP_KEY = 'admin_token_exp'

/** 距过期小于该阈值时，发请求前先滑动续期（提前 5 分钟） */
const REFRESH_AHEAD_MS = 5 * 60 * 1000

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 记录 token 与其过期时间（expiresIn 为秒） */
export function setTokenWithExpiry(token: string, expiresInSeconds: number): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(TOKEN_EXP_KEY, String(Date.now() + expiresInSeconds * 1000))
}

function getTokenExpiry(): number | null {
  const raw = localStorage.getItem(TOKEN_EXP_KEY)
  return raw ? Number(raw) : null
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXP_KEY)
}

/** 业务错误：HTTP 可能是 200，但 body.code 不是 200 */
export class BizError extends Error {
  code: number
  constructor(code: number, msg: string) {
    super(msg)
    this.name = 'BizError'
    this.code = code
  }
}

interface RequestOptions {
  method?: string
  /** 请求体，自动 JSON 序列化 */
  body?: unknown
  /** query 参数，自动拼到 URL */
  params?: Record<string, unknown>
  /** 关闭失败时的自动 message 提示 */
  silent?: boolean
}

function buildUrl(path: string, params?: Record<string, unknown>): string {
  const url = BASE_URL + path
  if (!params) return url
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, String(value))
    }
  }
  const qs = search.toString()
  return qs ? `${url}?${qs}` : url
}

function onUnauthorized(): void {
  clearToken()
  // 避免在登录页重复跳转
  if (!location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    location.assign(`/login?redirect=${redirect}`)
  }
}

/** 这些路径自身不触发续期检查，避免递归 */
function isAuthFlowPath(path: string): boolean {
  return path === '/auth/login' || path === '/auth/refresh' || path === '/auth/captcha'
}

let refreshing: Promise<void> | null = null

/** 活跃请求前的滑动续期：token 临近过期（且未过期）就先换新 */
async function ensureFreshToken(): Promise<void> {
  const token = getToken()
  const exp = getTokenExpiry()
  if (!token || !exp) return
  const now = Date.now()
  if (now < exp - REFRESH_AHEAD_MS || now >= exp) return // 还早 / 已过期（交给 401）
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await request<LoginResponse>('/auth/refresh', { method: 'POST', silent: true })
        setTokenWithExpiry(res.token, res.expiresIn)
      } catch {
        // 续期失败就维持原 token，后续请求若 401 再走登录
      }
    })().finally(() => {
      refreshing = null
    })
  }
  await refreshing
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, silent = false } = options

  if (!isAuthFlowPath(path)) {
    await ensureFreshToken()
  }

  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let payload: string | undefined
  if (body !== undefined) {
    if (body instanceof FormData) {
      // 文件上传：直接传 FormData，让浏览器自动设置 multipart/form-data 和 boundary
      payload = undefined
    } else {
      headers['Content-Type'] = 'application/json'
      payload = JSON.stringify(body)
    }
  }

  // FormData 不设置 Content-Type，浏览器会自动带上 multipart boundary
  const fetchBody = body instanceof FormData ? body : payload

  let response: Response
  try {
    response = await fetch(buildUrl(path, params), { method, headers, body: fetchBody })
  } catch {
    if (!silent) message.error('网络异常，请检查后端服务是否已启动')
    throw new BizError(-1, '网络异常')
  }

  // 后端对所有响应都返回 R 结构（含 403 / 400 / 500）
  let result: R<T>
  try {
    result = (await response.json()) as R<T>
  } catch {
    if (response.status === 401 && path !== '/auth/login') onUnauthorized()
    if (!silent) message.error(`请求失败（HTTP ${response.status}）`)
    throw new BizError(response.status, `HTTP ${response.status}`)
  }

  if (response.status === 401) {
    // 登录失败是表单校验结果，不是当前会话过期
    if (path !== '/auth/login') onUnauthorized()
    const msg = result.msg || '登录已过期'
    if (!silent) message.error(msg)
    throw new BizError(result.code || 401, msg)
  }

  if (result.code !== 200) {
    if (!silent) message.error(result.msg || '请求失败')
    throw new BizError(result.code, result.msg || '请求失败')
  }

  return result.data
}

export const http = {
  get: <T>(path: string, params?: Record<string, unknown>, silent?: boolean) =>
    request<T>(path, { method: 'GET', params, silent }),
  post: <T>(path: string, body?: unknown, silent?: boolean) =>
    request<T>(path, { method: 'POST', body, silent }),
  delete: <T>(path: string, silent?: boolean) =>
    request<T>(path, { method: 'DELETE', silent }),
  getBlob: async (path: string, params?: Record<string, unknown>): Promise<Blob> => {
    const url = buildUrl(path, params)
    const headers: Record<string, string> = {}
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(url, { headers })
    if (response.status === 401) {
      onUnauthorized()
      throw new BizError(401, '登录已过期')
    }
    if (!response.ok) {
      message.error(`请求失败（HTTP ${response.status}）`)
      throw new BizError(response.status, `HTTP ${response.status}`)
    }
    return response.blob()
  },
}

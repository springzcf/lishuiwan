const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function call(path, { method = 'GET', data, auth = true } = {}) {
  const token = localStorage.getItem('h5_token')
  let response
  try {
    response = await fetch(`${API_BASE}/api${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: data === undefined ? undefined : JSON.stringify(data)
    })
  } catch (_) {
    throw new ApiError('网络连接失败，请稍后重试', 0)
  }
  const body = await response.json().catch(() => null)
  if (response.ok && body?.code === 0) return body.data
  if (response.status === 401) {
    localStorage.removeItem('h5_token')
    window.dispatchEvent(new CustomEvent('h5-unauthorized'))
  }
  throw new ApiError(body?.msg || '请求失败，请稍后重试', response.status, body?.code)
}

export const api = {
  get: path => call(path),
  post: (path, data) => call(path, { method: 'POST', data }),
  put: (path, data) => call(path, { method: 'PUT', data }),
  publicPost: (path, data) => call(path, { method: 'POST', data, auth: false }),
  publicUrl: path => `${API_BASE}/api${path}`,
  asset: url => url?.startsWith('/') ? `${API_BASE}${url}` : url,
  uuid: () => globalThis.crypto?.randomUUID?.() || `${Date.now()}_${Math.random().toString(36).slice(2, 12)}`,
  isLocal: import.meta.env.VITE_DEV_LOGIN === 'true',
  mockPayment: import.meta.env.VITE_MOCK_PAYMENT === 'true'
}

export const isWechatBrowser = () => /MicroMessenger/i.test(navigator.userAgent)

export function saveLogin(result) {
  localStorage.setItem('h5_token', result.token)
  localStorage.setItem('h5_member', JSON.stringify(result.member || {}))
}

export function getMember() {
  try { return JSON.parse(localStorage.getItem('h5_member') || 'null') } catch (_) { return null }
}

export function logout() {
  localStorage.removeItem('h5_token')
  localStorage.removeItem('h5_member')
}

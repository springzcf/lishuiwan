export function toast(message) {
  window.dispatchEvent(new CustomEvent('h5-toast', { detail: message }))
}

export function errorMessage(error) {
  toast(error?.message || '操作失败，请稍后重试')
}

export function money(value) {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number.toFixed(2).replace(/\.00$/, '') : '0'
}

export function statusText(status) {
  return ({ expired: '已过期', used_up: '已用完', unused: '可使用', using: '可使用' })[status] || status
}

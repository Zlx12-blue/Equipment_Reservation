import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const request = axios.create({ baseURL: '/api', timeout: 10000 })

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) config.headers.Authorization = `Bearer ${auth.token}`
  return config
})

request.interceptors.response.use(
  (res) => {
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '请求失败')
      return Promise.reject(new Error(res.data.message))
    }
    return res.data.data
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const registerAdmin = (data) => request.post('/auth/register-admin', data)

export const listDevices = () => request.get('/devices')
export const createDevice = (data) => request.post('/devices', data)
export const updateDevice = (id, data) => request.put(`/devices/${id}`, data)
export const deleteDevice = (id) => request.delete(`/devices/${id}`)

export const submitReservation = (data) => request.post('/reservations', data)
export const myReservations = (params) => request.get('/reservations/my', { params })
export const updateReservation = (id, data) => request.put(`/reservations/${id}`, data)
export const cancelReservation = (id) => request.delete(`/reservations/${id}`)
export const allReservations = (params) => request.get('/reservations/all', { params })
export const deviceBusy = (deviceId, date, excludeId) =>
  request.get(`/reservations/device/${deviceId}/busy`, { params: { date, excludeId } })

export const pendingApprovals = () => request.get('/approvals/pending')
export const approvalHistory = () => request.get('/approvals/history')
export const completeApproval = (data) => request.post('/approvals/complete', data)

export const myNotifications = () => request.get('/notifications/my')
export const markNotificationRead = (id) => request.post(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.post('/notifications/read-all')
export const unreadNotificationCount = () => request.get('/notifications/unread-count')

export const adminStats = () => request.get('/admin/stats')
export const listUsers = () => request.get('/admin/users')
export const setUserEnabled = (id, enabled) => request.put(`/admin/users/${id}/enabled`, { enabled })
export const resetUserPassword = (id, password) => request.post(`/admin/users/${id}/reset-password`, { password })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)

/** 导出预约记录:文件流不经过统一拦截器,直接用 fetch 下载 */
export async function exportReservations(params = {}) {
  const auth = useAuthStore()
  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  ).toString()
  const res = await fetch(`/api/reservations/export${query ? '?' + query : ''}`, {
    headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (!res.ok) throw new Error('导出失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '预约记录.csv'
  a.click()
  URL.revokeObjectURL(url)
}

/**
 * 建立实时通知通道;收到消息时回调(多端实时互动)。
 * 生产环境通过 nginx 将 /ws 代理到后端。
 */
export function connectNotify(userId, onMessage) {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const ws = new WebSocket(`${protocol}://${location.host}/ws/notify?userId=${userId}`)
  ws.onmessage = (event) => onMessage(JSON.parse(event.data))
  return ws
}

export default request

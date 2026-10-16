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

export const listDevices = () => request.get('/devices')
export const createDevice = (data) => request.post('/devices', data)
export const updateDevice = (id, data) => request.put(`/devices/${id}`, data)
export const deleteDevice = (id) => request.delete(`/devices/${id}`)

export const submitReservation = (data) => request.post('/reservations', data)
export const myReservations = () => request.get('/reservations/my')
export const cancelReservation = (id) => request.delete(`/reservations/${id}`)
export const allReservations = () => request.get('/reservations/all')

export const pendingApprovals = () => request.get('/approvals/pending')
export const completeApproval = (data) => request.post('/approvals/complete', data)

export const myNotifications = () => request.get('/notifications/my')
export const markNotificationRead = (id) => request.post(`/notifications/${id}/read`)

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

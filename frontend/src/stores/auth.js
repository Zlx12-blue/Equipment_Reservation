import { defineStore } from 'pinia'

// 使用 sessionStorage:每个标签页独立的登录态,
// 便于同一浏览器开两个标签页分别扮演学生/教师演示多端互动
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    user: JSON.parse(sessionStorage.getItem('user') || 'null')
  }),
  getters: {
    homePath: (state) =>
      state.user?.role === 'STUDENT' ? '/student' : '/admin'
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
    }
  }
})

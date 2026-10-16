import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/student',
    component: () => import('../views/student/StudentHome.vue'),
    meta: { roles: ['STUDENT'] }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminHome.vue'),
    meta: { roles: ['TEACHER', 'ADMIN'] }
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/login') return true
  if (!auth.token) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(auth.user?.role)) {
    return auth.homePath
  }
  return true
})

export default router

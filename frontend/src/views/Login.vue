<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>实验室设备预约审批系统</h2>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <p class="tip">测试账号:student1 / teacher1 / admin,密码均为 123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useAuthStore } from '../stores/auth'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()

async function onLogin() {
  loading.value = true
  try {
    const data = await login(form)
    auth.setLogin(data.token, data.user)
    ElMessage.success('登录成功')
    router.push(auth.homePath)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3c88, #409eff);
}
.login-card {
  width: 400px;
  text-align: center;
}
.tip {
  color: #909399;
  font-size: 12px;
}
</style>

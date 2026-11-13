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
      <div class="register-entry">
        <span>还没有账号?</span>
        <el-button link type="primary" @click="onRegisterClick">注册账号</el-button>
      </div>
      <p class="tip">测试账号:student1 / teacher1 / admin,密码均为 123456</p>
    </el-card>

    <!-- 注册第一步:提示并选择账号类型 -->
    <el-dialog v-model="roleVisible" title="注册账号" width="420px">
      <p class="role-tip">请选择要注册的账号类型,注册后不可修改:</p>
      <div class="role-buttons">
        <el-button type="primary" plain size="large" @click="chooseRole('STUDENT')">
          <div class="role-item"><b>学生</b><span>预约实验室设备</span></div>
        </el-button>
        <el-button type="success" plain size="large" @click="chooseRole('TEACHER')">
          <div class="role-item"><b>教师</b><span>审批设备预约</span></div>
        </el-button>
      </div>
      <p class="role-note">管理员账号无法自助注册,需由已有管理员在管理端添加。</p>
    </el-dialog>

    <!-- 注册第二步:填写资料 -->
    <el-dialog v-model="regVisible" :title="`注册${regForm.role === 'TEACHER' ? '教师' : '学生'}账号`" width="420px">
      <el-form :model="regForm" label-width="70px">
        <el-form-item label="用户名"><el-input v-model="regForm.username" placeholder="3-50 位" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="regForm.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="regForm.password" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="regForm.confirm" type="password" show-password placeholder="再次输入密码" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="onRegister">确认注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'
import { useAuthStore } from '../stores/auth'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()

const roleVisible = ref(false)
const regVisible = ref(false)
const regLoading = ref(false)
const regForm = reactive({ role: 'STUDENT', username: '', realName: '', password: '', confirm: '' })

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

function onRegisterClick() {
  roleVisible.value = true
}

function chooseRole(role) {
  regForm.role = role
  regForm.username = ''
  regForm.realName = ''
  regForm.password = ''
  regForm.confirm = ''
  roleVisible.value = false
  regVisible.value = true
}

async function onRegister() {
  if (!regForm.username.trim() || !regForm.realName.trim()) {
    ElMessage.warning('请填写用户名和姓名')
    return
  }
  if (regForm.username.trim().length < 3) {
    ElMessage.warning('用户名至少 3 位')
    return
  }
  if (regForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (regForm.password !== regForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  regLoading.value = true
  try {
    await register({
      username: regForm.username.trim(),
      realName: regForm.realName.trim(),
      password: regForm.password,
      role: regForm.role
    })
    ElMessage.success('注册成功,请使用新账号登录')
    regVisible.value = false
    form.username = regForm.username.trim()
    form.password = ''
  } finally {
    regLoading.value = false
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
.register-entry {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}
.tip {
  color: #909399;
  font-size: 12px;
}
.role-tip {
  margin: 0 0 12px;
  color: #606266;
  font-size: 14px;
}
.role-buttons {
  display: flex;
  gap: 12px;
}
.role-buttons .el-button {
  flex: 1;
  height: auto;
  padding: 14px 10px;
}
.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.4;
}
.role-item span {
  font-size: 12px;
  opacity: 0.75;
}
.role-note {
  margin: 14px 0 0;
  color: #e6a23c;
  font-size: 12px;
}
</style>

<template>
  <el-container class="page">
    <el-header class="header">
      <span class="title">学生端 · 设备预约</span>
      <div>
        <el-badge :value="unread" :hidden="unread === 0" class="badge">
          <el-button @click="drawer = true">通知</el-button>
        </el-badge>
        <span class="user">{{ auth.user?.realName }}</span>
        <el-button link @click="onLogout">退出</el-button>
      </div>
    </el-header>

    <el-main>
      <el-tabs v-model="tab">
        <!-- 设备列表 + 预约 -->
        <el-tab-pane label="设备预约" name="devices">
          <el-table :data="devices" v-loading="loading">
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="model" label="型号" />
            <el-table-column prop="location" label="位置" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" :disabled="row.status !== 'AVAILABLE'"
                           @click="openReserve(row)">预约</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 我的预约 -->
        <el-tab-pane label="我的预约" name="mine">
          <el-table :data="mine">
            <el-table-column prop="device.name" label="设备" />
            <el-table-column prop="startTime" label="开始时间" />
            <el-table-column prop="endTime" label="结束时间" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="reserveTag(row.status)">{{ reserveText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approvalComment" label="审批意见" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 'PENDING'" type="danger" size="small"
                           @click="onCancel(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 预约弹窗 -->
    <el-dialog v-model="dialogVisible" :title="`预约 ${currentDevice?.name}`" width="480px">
      <el-form :model="reserveForm" label-width="80px">
        <el-form-item label="预约时间">
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
                          start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="reserveForm.purpose" type="textarea" placeholder="请说明预约用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 通知抽屉 -->
    <el-drawer v-model="drawer" title="我的通知">
      <el-timeline>
        <el-timeline-item v-for="n in notifications" :key="n.id" :timestamp="n.createdAt">
          {{ n.content }}
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  listDevices, submitReservation, myReservations, cancelReservation,
  myNotifications, connectNotify
} from '../../api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const tab = ref('devices')
const devices = ref([])
const mine = ref([])
const notifications = ref([])
const unread = ref(0)
const drawer = ref(false)
const loading = ref(false)
const dialogVisible = ref(false)
const currentDevice = ref(null)
const timeRange = ref([])
const reserveForm = reactive({ purpose: '' })
let ws = null

function statusText(s) {
  return { AVAILABLE: '可预约', MAINTENANCE: '维护中', DISABLED: '已停用' }[s] || s
}
function reserveText(s) {
  return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }[s] || s
}
function reserveTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s]
}

async function refresh() {
  loading.value = true
  try {
    devices.value = await listDevices()
    mine.value = await myReservations()
    notifications.value = await myNotifications()
  } finally {
    loading.value = false
  }
}

function openReserve(device) {
  currentDevice.value = device
  timeRange.value = []
  reserveForm.purpose = ''
  dialogVisible.value = true
}

async function onSubmit() {
  if (!timeRange.value?.length) return ElMessage.warning('请选择预约时间')
  await submitReservation({
    deviceId: currentDevice.value.id,
    startTime: timeRange.value[0].toISOString().slice(0, 19),
    endTime: timeRange.value[1].toISOString().slice(0, 19),
    purpose: reserveForm.purpose
  })
  ElMessage.success('已提交,等待教师审批')
  dialogVisible.value = false
  refresh()
}

async function onCancel(row) {
  await cancelReservation(row.id)
  ElMessage.success('已取消')
  refresh()
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  refresh()
  // 实时通知:审批结果即时推送到学生端
  ws = connectNotify(auth.user.id, (msg) => {
    unread.value++
    ElNotification({ title: '新通知', message: msg.content, type: 'info' })
    refresh()
  })
})
onUnmounted(() => ws?.close())
</script>

<style scoped>
.page { height: 100vh; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: #1f3c88; color: #fff;
}
.title { font-size: 18px; font-weight: bold; }
.user { margin: 0 12px; }
.badge { margin-right: 8px; }
</style>

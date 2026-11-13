<template>
  <el-container class="page">
    <el-header class="header">
      <span class="title">学生端 · 设备预约</span>
      <div>
        <el-badge :value="unread" :hidden="unread === 0" class="badge">
          <el-button @click="openDrawer">通知</el-button>
        </el-badge>
        <span class="user">{{ auth.user?.realName }}</span>
        <el-button link @click="onLogout">退出</el-button>
      </div>
    </el-header>

    <el-main>
      <el-tabs v-model="tab">
        <!-- 设备列表 -->
        <el-tab-pane label="设备预约" name="devices">
          <div class="filters">
            <el-input v-model="deviceKeyword" placeholder="搜索设备名称 / 位置" clearable style="width: 240px" />
            <el-select v-model="deviceStatus" placeholder="设备状态" clearable style="width: 140px">
              <el-option label="可预约" value="AVAILABLE" />
              <el-option label="维护中" value="MAINTENANCE" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
          </div>
          <el-table :data="filteredDevices" v-loading="loading">
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="model" label="型号" />
            <el-table-column prop="location" label="位置" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button type="primary" size="small" :disabled="row.status !== 'AVAILABLE'"
                           @click="openReserve(row)">预约</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 我的预约 -->
        <el-tab-pane label="我的预约" name="mine">
          <div class="filters">
            <el-select v-model="mineFilter.status" placeholder="预约状态" clearable style="width: 140px">
              <el-option label="待审批" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-input v-model="mineFilter.keyword" placeholder="搜索设备 / 用途" clearable style="width: 240px"
                      @keyup.enter="applyMineFilter" />
            <el-button type="primary" @click="applyMineFilter">查询</el-button>
            <el-button @click="resetMineFilter">重置</el-button>
          </div>
          <el-table :data="pagedMine" v-loading="loading">
            <el-table-column prop="device.name" label="设备" min-width="120" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column prop="endTime" label="结束时间" width="170" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reserveTag(row.status)">{{ reserveText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批老师" width="100">
              <template #default="{ row }">{{ row.approverName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="approvalComment" label="审批意见" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button size="small" @click="openModify(row)">修改</el-button>
                  <el-popconfirm title="确认取消该预约?" @confirm="onCancel(row)">
                    <template #reference>
                      <el-button type="danger" size="small">取消</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" layout="total, prev, pager, next" :total="mine.length"
                         :page-size="pageSize" v-model:current-page="page" />
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 预约 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form label-width="80px">
        <el-form-item label="设备">
          <span class="device-name">{{ currentDevice?.name }}</span>
          <span class="device-meta">{{ currentDevice?.location }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="pickDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择日期" :disabled-date="disabledDate" @change="loadBusy" />
        </el-form-item>
        <el-form-item label="时间段">
          <div class="slot-wrap">
            <div class="timeline">
              <div v-for="slot in slots" :key="slot.hour" class="slot"
                   :class="{ busy: slot.busy, past: slot.past,
                             selected: isSelected(slot.hour), inRange: inRange(slot.hour) }"
                   @click="pickHour(slot)">
                {{ pad(slot.hour) }}
              </div>
            </div>
            <div class="slot-legend">
              <span><i class="dot free"></i>空闲</span>
              <span><i class="dot busy"></i>已占用</span>
              <span><i class="dot selected"></i>已选</span>
              <span class="picked">{{ pickedText }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="reserveForm.purpose" type="textarea" :rows="3" placeholder="请说明预约用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ editingId ? '保存修改' : '提交申请' }}</el-button>
      </template>
    </el-dialog>

    <!-- 通知抽屉 -->
    <el-drawer v-model="drawer" title="消息中心" size="380px">
      <div class="drawer-actions">
        <span class="unread-tip">未读 {{ unread }} 条</span>
        <el-button link type="primary" :disabled="unread === 0" @click="onMarkAll">全部已读</el-button>
      </div>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <div v-for="n in notifications" :key="n.id" class="notice" :class="{ unread: !n.readFlag }"
           @click="onRead(n)">
        <div class="notice-content">{{ n.content }}</div>
        <div class="notice-time">{{ n.createdAt }}</div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  listDevices, submitReservation, myReservations, updateReservation, cancelReservation,
  deviceBusy, myNotifications, markNotificationRead, markAllNotificationsRead,
  unreadNotificationCount, connectNotify
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
const submitting = ref(false)
const dialogVisible = ref(false)
const currentDevice = ref(null)
const editingId = ref(null)
const deviceKeyword = ref('')
const deviceStatus = ref('')
const mineFilter = reactive({ status: '', keyword: '' })
const page = ref(1)
const pageSize = 8
let ws = null
let unreadTimer = null

const pickDate = ref(todayStr())
const pickStart = ref(null)
const pickEnd = ref(null)
const busyList = ref([])
const reserveForm = reactive({ purpose: '' })

const HOURS = Array.from({ length: 14 }, (_, i) => i + 8) // 08:00 - 22:00

const filteredDevices = computed(() => {
  const k = deviceKeyword.value.trim().toLowerCase()
  return devices.value.filter(d => {
    const matchKeyword = !k || d.name.toLowerCase().includes(k) || (d.location || '').toLowerCase().includes(k)
    const matchStatus = !deviceStatus.value || d.status === deviceStatus.value
    return matchKeyword && matchStatus
  })
})

const pagedMine = computed(() => {
  const start = (page.value - 1) * pageSize
  return mine.value.slice(start, start + pageSize)
})

const slots = computed(() => HOURS.map(hour => ({
  hour,
  busy: isBusy(hour),
  past: isPast(hour)
})))

const pickedText = computed(() => {
  if (pickStart.value == null) return '请选择开始时段'
  if (pickEnd.value == null) return `${pad(pickStart.value)}:00 - 请选择结束时段`
  return `${pad(pickStart.value)}:00 - ${pad(pickEnd.value)}:00`
})

const dialogTitle = computed(() =>
  `${editingId.value ? '修改预约' : '预约'} · ${currentDevice.value?.name || ''}`)

function pad(n) {
  return String(n).padStart(2, '0')
}

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function statusText(s) {
  return { AVAILABLE: '可预约', MAINTENANCE: '维护中', DISABLED: '已停用' }[s] || s
}
function reserveText(s) {
  return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }[s] || s
}
function reserveTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s]
}

function slotStart(hour) {
  return new Date(`${pickDate.value}T${pad(hour)}:00:00`)
}
function slotEnd(hour) {
  return new Date(`${pickDate.value}T${pad(hour + 1)}:00:00`)
}

function isBusy(hour) {
  const s = slotStart(hour).getTime()
  const e = slotEnd(hour).getTime()
  return busyList.value.some(b => {
    const bs = new Date(b.startTime).getTime()
    const be = new Date(b.endTime).getTime()
    return bs < e && be > s
  })
}

function isPast(hour) {
  return slotStart(hour).getTime() <= Date.now()
}

function isSelected(hour) {
  return hour === pickStart.value || (pickEnd.value != null && hour === pickEnd.value - 1)
}
function inRange(hour) {
  return pickEnd.value != null && hour > pickStart.value && hour < pickEnd.value - 1
}

function pickHour(slot) {
  if (slot.busy || slot.past) {
    ElMessage.warning(slot.busy ? '该时段已被占用' : '该时段已过去')
    return
  }
  if (pickStart.value == null || pickEnd.value != null) {
    pickStart.value = slot.hour
    pickEnd.value = null
    return
  }
  if (slot.hour <= pickStart.value) {
    pickStart.value = slot.hour
    return
  }
  for (let h = pickStart.value; h < slot.hour; h++) {
    if (isBusy(h) || isPast(h)) {
      ElMessage.warning('所选区间内包含不可用时段,请重新选择')
      pickStart.value = slot.hour
      pickEnd.value = null
      return
    }
  }
  pickEnd.value = slot.hour
}

function toLocalDateTime(hour) {
  return `${pickDate.value}T${pad(hour)}:00:00`
}

async function loadDevices() {
  devices.value = await listDevices()
}

async function loadMine() {
  mine.value = await myReservations({
    status: mineFilter.status || undefined,
    keyword: mineFilter.keyword || undefined
  })
  page.value = 1
}

async function loadNotifications() {
  notifications.value = await myNotifications()
  const res = await unreadNotificationCount()
  unread.value = res.count
}

async function refresh() {
  loading.value = true
  try {
    await Promise.all([loadDevices(), loadMine(), loadNotifications()])
  } finally {
    loading.value = false
  }
}

function applyMineFilter() {
  loadMine()
}

function resetMineFilter() {
  mineFilter.status = ''
  mineFilter.keyword = ''
  loadMine()
}

async function loadBusy() {
  if (!currentDevice.value) return
  busyList.value = await deviceBusy(currentDevice.value.id, pickDate.value, editingId.value || undefined)
}

async function openReserve(device) {
  currentDevice.value = device
  editingId.value = null
  pickDate.value = todayStr()
  pickStart.value = null
  pickEnd.value = null
  reserveForm.purpose = ''
  dialogVisible.value = true
  await loadBusy()
}

async function openModify(row) {
  currentDevice.value = row.device
  editingId.value = row.id
  pickDate.value = row.startTime.slice(0, 10)
  pickStart.value = Number(row.startTime.slice(11, 13))
  pickEnd.value = Number(row.endTime.slice(11, 13))
  if (Number(row.endTime.slice(14, 16)) > 0) {
    pickEnd.value = Math.min(22, pickEnd.value + 1)
  }
  reserveForm.purpose = row.purpose || ''
  dialogVisible.value = true
  await loadBusy()
}

async function onSubmit() {
  if (pickStart.value == null || pickEnd.value == null) {
    ElMessage.warning('请在时间轴上选择完整的开始与结束时段')
    return
  }
  if (pickEnd.value <= pickStart.value) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  const payload = {
    deviceId: currentDevice.value.id,
    startTime: toLocalDateTime(pickStart.value),
    endTime: toLocalDateTime(pickEnd.value),
    purpose: reserveForm.purpose
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateReservation(editingId.value, payload)
      ElMessage.success('修改已保存,等待教师重新确认')
    } else {
      await submitReservation(payload)
      ElMessage.success('已提交,等待教师审批')
      tab.value = 'mine'
    }
    dialogVisible.value = false
    refresh()
  } finally {
    submitting.value = false
  }
}

async function onCancel(row) {
  await cancelReservation(row.id)
  ElMessage.success('已取消')
  refresh()
}

function openDrawer() {
  drawer.value = true
  loadNotifications()
}

async function onRead(n) {
  if (n.readFlag) return
  await markNotificationRead(n.id)
  await loadNotifications()
}

async function onMarkAll() {
  await markAllNotificationsRead()
  ElMessage.success('已全部标记为已读')
  await loadNotifications()
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  refresh()
  // 实时通知:审批结果即时推送到学生端
  ws = connectNotify(auth.user.id, (msg) => {
    ElNotification({ title: '新通知', message: msg.content, type: 'info' })
    loadNotifications()
    loadMine()
  })
  // 兜底轮询:WebSocket 断开时未读数仍然同步
  unreadTimer = setInterval(() => loadNotifications(), 60000)
})
onUnmounted(() => {
  ws?.close()
  clearInterval(unreadTimer)
})
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
.filters { display: flex; gap: 12px; margin-bottom: 12px; }
.pager { margin-top: 12px; justify-content: flex-end; }
.device-name { font-weight: 600; margin-right: 12px; }
.device-meta { color: #909399; font-size: 12px; }
.slot-wrap { width: 100%; }
.timeline { display: flex; gap: 4px; width: 100%; }
.slot {
  flex: 1; height: 42px; line-height: 42px; text-align: center;
  border-radius: 4px; background: #f0f9eb; color: #67c23a;
  border: 1px solid #c2e7b0; cursor: pointer; font-size: 12px; user-select: none;
  transition: all 0.15s;
}
.slot:hover { transform: translateY(-1px); }
.slot.busy { background: #fef0f0; color: #f56c6c; border-color: #fbc4c4; cursor: not-allowed; }
.slot.past { background: #f4f4f5; color: #c0c4cc; border-color: #e9e9eb; cursor: not-allowed; }
.slot.selected { background: #409eff; color: #fff; border-color: #409eff; }
.slot.inRange { background: #d9ecff; color: #409eff; border-color: #a0cfff; }
.slot-legend { display: flex; align-items: center; gap: 14px; margin-top: 8px; font-size: 12px; color: #909399; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; }
.dot.free { background: #f0f9eb; border: 1px solid #c2e7b0; }
.dot.busy { background: #fef0f0; border: 1px solid #fbc4c4; }
.dot.selected { background: #409eff; }
.picked { margin-left: auto; color: #409eff; font-weight: 600; }
.drawer-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.unread-tip { color: #909399; font-size: 13px; }
.notice { padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 8px; background: #f5f7fa; }
.notice.unread { background: #ecf5ff; border-left: 3px solid #409eff; }
.notice-content { font-size: 13px; color: #303133; }
.notice-time { font-size: 12px; color: #909399; margin-top: 4px; }
</style>

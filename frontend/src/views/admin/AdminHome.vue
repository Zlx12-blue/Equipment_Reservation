<template>
  <el-container class="page">
    <el-header class="header">
      <span class="title">{{ isAdmin ? '管理端' : '教师端' }} · 审批与设备管理</span>
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
        <!-- 待审批任务 -->
        <el-tab-pane name="approvals">
          <template #label>
            待审批 <el-badge :value="tasks.length" :hidden="tasks.length === 0" />
          </template>
          <el-table :data="tasks" v-loading="loading">
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="deviceName" label="设备" min-width="120" />
            <el-table-column label="预约时段" min-width="260">
              <template #default="{ row }">{{ row.startTime || '—' }} ~ {{ row.endTime || '—' }}</template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
            <el-table-column prop="createTime" label="提交时间" width="200" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="onApprove(row, true)">通过</el-button>
                <el-button type="danger" size="small" @click="onApprove(row, false)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 已办审批(教师 / 管理员) -->
        <el-tab-pane label="已办审批" name="history">
          <el-table :data="history">
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="deviceName" label="设备" min-width="120" />
            <el-table-column label="预约时段" min-width="260">
              <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="reserveTag(row.status)">{{ reserveText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approvalComment" label="审批意见" show-overflow-tooltip />
            <el-table-column prop="finishedAt" label="审批时间" width="200" />
          </el-table>
        </el-tab-pane>

        <!-- 全部预约记录(仅管理员) -->
        <el-tab-pane v-if="isAdmin" label="预约记录" name="records">
          <div class="filters">
            <el-select v-model="recordFilter.status" placeholder="预约状态" clearable style="width: 140px">
              <el-option label="待审批" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-input v-model="recordFilter.keyword" placeholder="搜索设备 / 用途" clearable
                      style="width: 240px" @keyup.enter="loadRecords" />
            <el-button type="primary" @click="loadRecords">查询</el-button>
            <el-button @click="resetRecordFilter">重置</el-button>
            <el-button type="success" plain @click="onExport">导出记录</el-button>
          </div>
          <el-table :data="records">
            <el-table-column prop="applicant.realName" label="申请人" width="100" />
            <el-table-column prop="device.name" label="设备" />
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="endTime" label="结束时间" width="180" />
            <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="reserveTag(row.status)">{{ reserveText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approvalComment" label="审批意见" show-overflow-tooltip />
            <el-table-column label="审批老师" width="100">
              <template #default="{ row }">{{ row.approverName || '—' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 设备管理(仅管理员) -->
        <el-tab-pane v-if="isAdmin" label="设备管理" name="devices">
          <el-button type="primary" style="margin-bottom: 12px" @click="openEdit(null)">
            新增设备
          </el-button>
          <el-table :data="devices">
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="model" label="型号" />
            <el-table-column prop="location" label="位置" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="openEdit(row)">编辑</el-button>
                <el-popconfirm title="确认删除该设备?" @confirm="onDeleteDevice(row)">
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 用户管理(仅管理员) -->
        <el-tab-pane v-if="isAdmin" label="用户管理" name="users">
          <el-button type="primary" style="margin-bottom: 12px" @click="onAddAdmin">
            添加管理员
          </el-button>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
            title="管理员账号只能由已登录的管理员创建;禁用或删除账号会立即生效。存在预约记录的用户不可删除。" />
          <el-table :data="users">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="roleTag(row.role)">{{ roleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '正常' : '已禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="240">
              <template #default="{ row }">
                <el-button v-if="row.id !== auth.user?.id" size="small"
                           :type="row.enabled ? 'warning' : 'success'"
                           @click="onToggleUser(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
                <el-button size="small" @click="onResetPassword(row)">重置密码</el-button>
                <el-button v-if="row.id !== auth.user?.id" type="danger" size="small"
                           @click="onDeleteUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 数据统计(仅管理员) -->
        <el-tab-pane v-if="isAdmin" label="数据统计" name="stats">
          <div class="stat-cards">
            <div class="stat-card">
              <div class="num">{{ stats.totalReservations ?? 0 }}</div>
              <div class="label">预约总数</div>
            </div>
            <div class="stat-card warning">
              <div class="num">{{ stats.pendingCount ?? 0 }}</div>
              <div class="label">待审批</div>
            </div>
            <div class="stat-card success">
              <div class="num">{{ stats.approvalRate ?? 0 }}%</div>
              <div class="label">审批通过率</div>
            </div>
            <div class="stat-card">
              <div class="num">{{ stats.availableDevices ?? 0 }} / {{ stats.totalDevices ?? 0 }}</div>
              <div class="label">可用设备</div>
            </div>
            <div class="stat-card">
              <div class="num">{{ stats.totalUsers ?? 0 }}</div>
              <div class="label">用户总数</div>
            </div>
          </div>

          <div class="panel-grid">
            <el-card shadow="never">
              <template #header><b>近 7 天预约趋势</b></template>
              <div class="bar-chart">
                <div v-for="d in stats.trend || []" :key="d.date" class="bar-item">
                  <div class="bar-value">{{ d.count }}</div>
                  <div class="bar" :style="{ height: barHeight(d.count) }"></div>
                  <div class="bar-label">{{ d.date }}</div>
                </div>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header><b>预约状态分布</b></template>
              <div v-for="s in statusRows" :key="s.label" class="status-row">
                <div class="status-head">
                  <span>{{ s.label }}</span>
                  <span class="muted">{{ s.count }} 条 · {{ s.percent }}%</span>
                </div>
                <el-progress :percentage="s.percent" :color="s.color" :show-text="false" />
              </div>
            </el-card>
          </div>

          <el-card shadow="never" style="margin-top: 16px">
            <template #header><b>热门设备 TOP5</b></template>
            <el-empty v-if="(stats.topDevices || []).length === 0" description="暂无预约数据" />
            <div v-for="d in stats.topDevices || []" :key="d.name" class="status-row">
              <div class="status-head">
                <span>{{ d.name }}</span>
                <span class="muted">{{ d.count }} 次</span>
              </div>
              <el-progress :percentage="devicePercent(d.count)" color="#409eff" :show-text="false" />
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveVisible" :title="approveForm.approved ? '通过申请' : '驳回申请'" width="560px">
      <div v-if="currentTask" class="approve-info">
        <div><span class="muted">申请人:</span>{{ currentTask.applicantName }}</div>
        <div><span class="muted">设备:</span>{{ currentTask.deviceName }}</div>
        <div><span class="muted">时段:</span>{{ currentTask.startTime }} ~ {{ currentTask.endTime }}</div>
      </div>
      <el-alert v-if="approveConflicts.length" type="warning" :closable="false" show-icon
        style="margin-bottom: 12px"
        :title="`注意:该设备在同一时段还有 ${approveConflicts.length} 条待审批/已通过预约,通过后可能产生冲突`" />
      <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="审批意见(可选)" />
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确认</el-button>
      </template>
    </el-dialog>

    <!-- 设备编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑设备' : '新增设备'" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="editForm.model" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="editForm.location" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="可预约" value="AVAILABLE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDevice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加管理员弹窗 -->
    <el-dialog v-model="adminVisible" title="添加管理员账号" width="420px">
      <el-form :model="adminForm" label-width="70px">
        <el-form-item label="用户名"><el-input v-model="adminForm.username" placeholder="3-50 位" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="adminForm.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="adminForm.password" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="adminForm.confirm" type="password" show-password placeholder="再次输入密码" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminVisible = false">取消</el-button>
        <el-button type="primary" :loading="adminLoading" @click="saveAdmin">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 消息中心 -->
    <el-drawer v-model="drawer" title="消息中心" size="380px">
      <div class="drawer-actions">
        <span class="muted">未读 {{ unread }} 条</span>
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
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  listDevices, createDevice, updateDevice, deleteDevice, allReservations, exportReservations,
  pendingApprovals, approvalHistory, completeApproval, deviceBusy,
  myNotifications, markNotificationRead, markAllNotificationsRead, unreadNotificationCount,
  adminStats, listUsers, setUserEnabled, resetUserPassword, deleteUser,
  connectNotify, registerAdmin
} from '../../api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const tab = ref('approvals')
const tasks = ref([])
const history = ref([])
const devices = ref([])
const records = ref([])
const users = ref([])
const stats = ref({})
const notifications = ref([])
const unread = ref(0)
const drawer = ref(false)
const loading = ref(false)

const approveVisible = ref(false)
const approveConflicts = ref([])
const currentTask = ref(null)
const approveForm = reactive({ taskId: '', approved: true, comment: '' })

const editVisible = ref(false)
const editForm = reactive({ id: null, name: '', model: '', location: '', status: 'AVAILABLE', description: '' })

const adminVisible = ref(false)
const adminLoading = ref(false)
const adminForm = reactive({ username: '', realName: '', password: '', confirm: '' })

const recordFilter = reactive({ status: '', keyword: '' })
let ws = null

const statusRows = computed(() => {
  const total = stats.value.totalReservations || 0
  const rows = [
    { label: '已通过', count: stats.value.approvedCount || 0, color: '#67c23a' },
    { label: '待审批', count: stats.value.pendingCount || 0, color: '#e6a23c' },
    { label: '已驳回', count: stats.value.rejectedCount || 0, color: '#f56c6c' },
    { label: '已取消', count: stats.value.cancelledCount || 0, color: '#909399' }
  ]
  return rows.map(r => ({ ...r, percent: total === 0 ? 0 : Math.round(r.count * 100 / total) }))
})

const maxTrend = computed(() =>
  Math.max(1, ...(stats.value.trend || []).map(d => d.count)))

const maxDevice = computed(() =>
  Math.max(1, ...(stats.value.topDevices || []).map(d => d.count)))

function barHeight(count) {
  return `${Math.max(4, Math.round(count * 120 / maxTrend.value))}px`
}
function devicePercent(count) {
  return Math.round(count * 100 / maxDevice.value)
}

function reserveText(s) {
  return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }[s] || s
}
function reserveTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s]
}
function roleText(r) {
  return { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[r] || r
}
function roleTag(r) {
  return { STUDENT: 'info', TEACHER: 'success', ADMIN: 'danger' }[r]
}

async function loadTasks() {
  tasks.value = await pendingApprovals()
}
async function loadHistory() {
  history.value = await approvalHistory()
}
async function loadRecords() {
  records.value = await allReservations({
    status: recordFilter.status || undefined,
    keyword: recordFilter.keyword || undefined
  })
}
async function loadNotifications() {
  notifications.value = await myNotifications()
  const res = await unreadNotificationCount()
  unread.value = res.count
}

async function refresh() {
  loading.value = true
  try {
    await Promise.all([loadTasks(), loadHistory(), loadNotifications()])
    if (isAdmin.value) {
      await Promise.all([loadRecords(), loadDevices(), loadUsers(), loadStats()])
    }
  } finally {
    loading.value = false
  }
}

async function loadDevices() {
  devices.value = await listDevices()
}
async function loadUsers() {
  users.value = await listUsers()
}
async function loadStats() {
  stats.value = await adminStats()
}

function resetRecordFilter() {
  recordFilter.status = ''
  recordFilter.keyword = ''
  loadRecords()
}

async function onExport() {
  try {
    await exportReservations({
      status: recordFilter.status || undefined,
      keyword: recordFilter.keyword || undefined
    })
    ElMessage.success('导出已开始')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function onApprove(row, approved) {
  currentTask.value = row
  approveForm.taskId = row.taskId
  approveForm.approved = approved
  approveForm.comment = ''
  approveConflicts.value = []
  approveVisible.value = true
  // 审批前检查该设备当日其他占用,避免误批导致冲突
  if (row.deviceId && row.startTime) {
    try {
      const busy = await deviceBusy(row.deviceId, row.startTime.slice(0, 10),
        row.reservationId ? Number(row.reservationId) : undefined)
      const s = new Date(row.startTime).getTime()
      const e = new Date(row.endTime).getTime()
      approveConflicts.value = busy.filter(b =>
        new Date(b.startTime).getTime() < e && new Date(b.endTime).getTime() > s)
    } catch {
      approveConflicts.value = []
    }
  }
}

async function submitApproval() {
  await completeApproval(approveForm)
  ElMessage.success('审批完成')
  approveVisible.value = false
  refresh()
}

function openEdit(row) {
  Object.assign(editForm, row
    ? { ...row }
    : { id: null, name: '', model: '', location: '', status: 'AVAILABLE', description: '' })
  editVisible.value = true
}

async function saveDevice() {
  if (editForm.id) await updateDevice(editForm.id, editForm)
  else await createDevice(editForm)
  ElMessage.success('已保存')
  editVisible.value = false
  refresh()
}

async function onDeleteDevice(row) {
  await deleteDevice(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function onToggleUser(row) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}账号「${row.realName}(${row.username})」吗?${row.enabled ? '禁用后该账号将立即无法登录。' : ''}`,
      `${action}账号`, { type: 'warning' })
  } catch {
    return
  }
  await setUserEnabled(row.id, !row.enabled)
  ElMessage.success(`已${action}`)
  loadUsers()
  loadStats()
}

async function onResetPassword(row) {
  let value
  try {
    const res = await ElMessageBox.prompt(
      `为「${row.realName}(${row.username})」设置新密码`, '重置密码',
      {
        inputType: 'password',
        inputPlaceholder: '至少 6 位',
        inputValidator: (v) => (v && v.length >= 6) || '密码至少 6 位'
      })
    value = res.value
  } catch {
    return
  }
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function onDeleteUser(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除账号「${row.realName}(${row.username})」吗?该操作不可恢复。`,
      '删除账号', { type: 'warning', confirmButtonText: '确认删除' })
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('已删除')
  loadUsers()
  loadStats()
}

async function onAddAdmin() {
  try {
    await ElMessageBox.confirm(
      '管理员拥有审批、预约记录与设备管理等全部权限。确认要添加一个新的管理员账号吗?',
      '添加管理员',
      { type: 'warning', confirmButtonText: '确认添加', cancelButtonText: '取消' })
  } catch {
    return
  }
  Object.assign(adminForm, { username: '', realName: '', password: '', confirm: '' })
  adminVisible.value = true
}

async function saveAdmin() {
  if (!adminForm.username.trim() || !adminForm.realName.trim()) {
    ElMessage.warning('请填写用户名和姓名')
    return
  }
  if (adminForm.username.trim().length < 3) {
    ElMessage.warning('用户名至少 3 位')
    return
  }
  if (adminForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (adminForm.password !== adminForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  adminLoading.value = true
  try {
    await registerAdmin({
      username: adminForm.username.trim(),
      realName: adminForm.realName.trim(),
      password: adminForm.password
    })
    ElMessage.success('管理员账号已添加')
    adminVisible.value = false
    loadUsers()
  } finally {
    adminLoading.value = false
  }
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
  // 实时通知:学生提交预约即时推送到审批端
  ws = connectNotify(auth.user.id, (msg) => {
    ElNotification({ title: '新待办', message: msg.content, type: 'warning' })
    loadNotifications()
    loadTasks()
  })
})
onUnmounted(() => ws?.close())
</script>

<style scoped>
.page { height: 100vh; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: #2c5e2e; color: #fff;
}
.title { font-size: 18px; font-weight: bold; }
.user { margin: 0 12px; }
.badge { margin-right: 8px; }
.filters { display: flex; gap: 12px; margin-bottom: 12px; }
.muted { color: #909399; }
.approve-info { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; font-size: 13px; }
.drawer-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.notice { padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 8px; background: #f5f7fa; }
.notice.unread { background: #ecf5ff; border-left: 3px solid #409eff; }
.notice-content { font-size: 13px; color: #303133; }
.notice-time { font-size: 12px; color: #909399; margin-top: 4px; }

.stat-cards { display: flex; gap: 16px; flex-wrap: wrap; }
.stat-card {
  flex: 1; min-width: 150px; background: #fff; border-radius: 8px;
  padding: 18px 20px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  border-left: 4px solid #409eff;
}
.stat-card.success { border-left-color: #67c23a; }
.stat-card.warning { border-left-color: #e6a23c; }
.stat-card .num { font-size: 26px; font-weight: 700; color: #303133; }
.stat-card .label { margin-top: 6px; font-size: 13px; color: #909399; }

.panel-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; margin-top: 16px; }
.bar-chart { display: flex; align-items: flex-end; gap: 18px; height: 180px; padding: 0 8px; }
.bar-item { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; align-items: center; height: 100%; }
.bar-value { font-size: 12px; color: #606266; margin-bottom: 4px; }
.bar { width: 60%; background: linear-gradient(180deg, #79bbff, #409eff); border-radius: 4px 4px 0 0; min-height: 4px; }
.bar-label { margin-top: 6px; font-size: 12px; color: #909399; }

.status-row { margin-bottom: 14px; }
.status-head { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; color: #303133; }
</style>

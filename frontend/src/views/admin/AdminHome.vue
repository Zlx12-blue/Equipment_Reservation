<template>
  <el-container class="page">
    <el-header class="header">
      <span class="title">{{ isAdmin ? '管理端' : '教师端' }} · 审批与设备管理</span>
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
        <!-- 待审批任务 -->
        <el-tab-pane name="approvals">
          <template #label>
            待审批 <el-badge :value="tasks.length" :hidden="tasks.length === 0" />
          </template>
          <el-table :data="tasks" v-loading="loading">
            <el-table-column prop="applicantName" label="申请人" />
            <el-table-column prop="deviceName" label="设备" />
            <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
            <el-table-column prop="createTime" label="提交时间" width="200" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="onApprove(row, true)">通过</el-button>
                <el-button type="danger" size="small" @click="onApprove(row, false)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 全部预约记录(仅管理员) -->
        <el-tab-pane v-if="isAdmin" label="预约记录" name="records">
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
                <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="openEdit(row)">编辑</el-button>
                <el-popconfirm title="确认删除该设备?" @confirm="onDelete(row)">
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 审批意见弹窗 -->
    <el-dialog v-model="approveVisible" :title="approveForm.approved ? '通过申请' : '驳回申请'" width="420px">
      <el-input v-model="approveForm.comment" type="textarea" placeholder="审批意见(可选)" />
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  listDevices, createDevice, updateDevice, deleteDevice, allReservations,
  pendingApprovals, completeApproval, myNotifications, connectNotify
} from '../../api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const tab = ref('approvals')
const tasks = ref([])
const devices = ref([])
const records = ref([])
const notifications = ref([])
const unread = ref(0)
const drawer = ref(false)
const loading = ref(false)
const approveVisible = ref(false)
const editVisible = ref(false)
const approveForm = reactive({ taskId: '', approved: true, comment: '' })
const editForm = reactive({ id: null, name: '', model: '', location: '', status: 'AVAILABLE', description: '' })
let ws = null

function reserveText(s) {
  return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }[s] || s
}
function reserveTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s]
}

async function refresh() {
  loading.value = true
  try {
    tasks.value = await pendingApprovals()
    if (isAdmin.value) {
      devices.value = await listDevices()
      records.value = await allReservations()
    }
    notifications.value = await myNotifications()
  } finally {
    loading.value = false
  }
}

function onApprove(row, approved) {
  approveForm.taskId = row.taskId
  approveForm.approved = approved
  approveForm.comment = ''
  approveVisible.value = true
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

async function onDelete(row) {
  await deleteDevice(row.id)
  ElMessage.success('已删除')
  refresh()
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  refresh()
  // 实时通知:学生提交预约即时推送到审批端
  ws = connectNotify(auth.user.id, (msg) => {
    unread.value++
    ElNotification({ title: '新待办', message: msg.content, type: 'warning' })
    refresh()
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
</style>

<template>
  <div class="schedule-page">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>排班管理</span>
          <div>
            <el-button type="primary" @click="batchDialog=true">一键排班</el-button>
            <el-button type="danger" size="small" @click="reviewDialog=true" v-if="pendingCount > 0" style="margin-left:10px">
              待审批 {{ pendingCount }} 项变更
            </el-button>
          </div>
        </div>
      </template>

      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" @change="loadData" style="margin-bottom:20px" />

      <el-row :gutter="15">
        <el-col :span="8" v-for="l in lines" :key="l.lineId" style="margin-bottom:15px">
          <el-card shadow="hover" size="small" :class="['line-card', l.hasSchedule && !l.cancelled ? 'active' : 'inactive']">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <div>
                <b>{{ l.lineCode }}</b> - {{ l.lineName }}
                <div v-if="l.hasSchedule && !l.cancelled" style="color:#67C23A;font-size:13px;margin-top:4px">
                  {{ l.inspectorName }}
                </div>
                <div v-else style="color:#909399;font-size:13px;margin-top:4px">未安排</div>
              </div>
              <div>
                <el-popconfirm v-if="l.hasSchedule && !l.cancelled" title="确认取消该排班？" @confirm="cancelSchedule(l.scheduleId)">
                  <template #reference><el-button type="danger" size="small">取消</el-button></template>
                </el-popconfirm>
                <el-button v-else type="primary" size="small" @click="openAdd(l.lineId)">安排</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 安排审核者弹窗 -->
      <el-dialog v-model="addDialog" title="安排审核者" width="350px">
        <el-select v-model="selectedInspector" placeholder="选择审核者" style="width:100%">
          <el-option v-for="i in inspectors" :key="i.userId" :label="i.realName" :value="i.userId" />
        </el-select>
        <template #footer>
          <el-button @click="addDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!selectedInspector" @click="createSchedule">确定安排</el-button>
        </template>
      </el-dialog>

      <!-- 一键排班弹窗 -->
      <el-dialog v-model="batchDialog" title="一键排班" width="350px">
        <p style="color:#666;margin-bottom:12px">将 {{ date }} 全部产线排给同一人，后续仍可单独调整。</p>
        <el-select v-model="batchInspector" placeholder="选择审核者" style="width:100%">
          <el-option v-for="i in inspectors" :key="i.userId" :label="i.realName" :value="i.userId" />
        </el-select>
        <template #footer>
          <el-button @click="batchDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!batchInspector" @click="doBatchAssign">确定</el-button>
        </template>
      </el-dialog>

      <!-- 变更审批弹窗 -->
      <el-dialog v-model="reviewDialog" title="变更审批" width="600px" @opened="loadPending">
        <div v-if="pendingList.length === 0">暂无待审批申请</div>
        <div v-for="r in pendingList" :key="r.requestId" style="margin-bottom:15px;padding:12px;border:1px solid #ebeef5;border-radius:4px">
          <p><b>{{ r.requestedByName }}</b> 申请 {{ typeLabel(r.requestType) }}</p>
          <p v-if="r.lineCode">产线: {{ r.lineCode }} - {{ r.lineName }}</p>
          <p v-if="r.proposedInspectorName">替换为: {{ r.proposedInspectorName }}</p>
          <p style="color:#666">原因: {{ r.reason }}</p>
          <div style="margin-top:8px">
            <el-button type="success" size="small" @click="approve(r.requestId, 'APPROVED')">通过</el-button>
            <el-popconfirm title="确定拒绝？原排班保持不变" @confirm="approve(r.requestId, 'REJECTED')">
              <template #reference><el-button type="danger" size="small">拒绝</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { productionLineApi } from '@/api'
import { ElMessage } from 'element-plus'

const now = new Date()
const date = ref(now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0'))
const lines = ref([])
const inspectors = ref([])
const addDialog = ref(false)
const batchDialog = ref(false)
const batchInspector = ref(null)
const reviewDialog = ref(false)
const selectedInspector = ref(null)
const selectedLine = ref(null)
const pendingList = ref([])
const pendingCount = ref(0)

const typeLabel = (t) => ({ REASSIGN: '换人', CANCEL: '取消' }[t] || t)

const loadData = async () => {
  const [lRes, sRes] = await Promise.all([
    productionLineApi.list(),
    request.get('/schedule/list', { params: { date: date.value } })
  ])
  const schedules = sRes.code === 200 ? sRes.data : []
  const schedMap = {}
  schedules.forEach(s => { schedMap[s.lineId] = s })

  lines.value = (lRes.code === 200 ? lRes.data : []).map(l => {
    const s = schedMap[l.lineId]
    return { ...l, hasSchedule: !!s, inspectorName: s ? s.inspectorName : '', scheduleId: s ? s.scheduleId : null, cancelled: s ? s.status === 'CANCELLED' : false }
  })
}

const loadInspectors = async () => {
  const res = await request.get('/schedule/inspectors')
  if (res.code === 200) inspectors.value = res.data
}

const openAdd = (lineId) => { selectedLine.value = lineId; selectedInspector.value = null; addDialog.value = true }

const createSchedule = async () => {
  const res = await request.post('/schedule/create', { inspectorId: selectedInspector.value, lineId: selectedLine.value, date: date.value })
  if (res.code === 200) { ElMessage.success('排班成功'); addDialog.value = false; loadData() }
}

const doBatchAssign = async () => {
  const res = await request.post('/schedule/batch-assign', { inspectorId: batchInspector.value, date: date.value })
  if (res.code === 200) { ElMessage.success(`已排 ${res.data.assignedCount} 条产线`); batchDialog.value = false; loadData() }
}

const cancelSchedule = async (scheduleId) => {
  const res = await request.put('/schedule/cancel', null, { params: { scheduleId } })
  if (res.code === 200) { ElMessage.success('已取消'); loadData() }
}

const loadPending = async () => {
  const res = await request.get('/schedule/pending-requests')
  if (res.code === 200) { pendingList.value = res.data; pendingCount.value = res.data.length }
}

const approve = async (requestId, action) => {
  await request.put('/schedule/approve-request', { requestId, action, comment: action === 'REJECTED' ? '已拒绝，原排班不变' : '' })
  ElMessage.success(action === 'APPROVED' ? '已通过' : '已拒绝')
  loadPending(); loadData()
}

onMounted(() => { loadData(); loadInspectors(); loadPending() })
</script>

<style scoped>
.line-card { min-height: 80px; }
.active { border-left: 4px solid #67C23A; background: #f0f9eb; }
.inactive { border-left: 4px solid #eee; }
</style>

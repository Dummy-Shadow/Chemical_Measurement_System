<template>
  <div class="my-schedule">
    <el-card>
      <template #header><span>我的排班 - {{ date }}</span></template>
      <div v-if="loading" style="text-align:center;padding:40px"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="schedules.length === 0">
        <el-empty description="今日暂无排班任务，如有需要请联系分区管理者安排" />
      </div>
      <el-row :gutter="15" v-else>
        <el-col :span="8" v-for="s in schedules" :key="s.scheduleId" style="margin-bottom:15px">
          <el-card shadow="hover" size="small" class="line-card" @click="$router.push('/manual-entry')">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <div>
                <b>{{ s.lineCode }}</b> - {{ s.lineName }}
                <div style="color:#606266;font-size:13px;margin-top:4px">点击进入录入</div>
              </div>
              <el-button type="warning" size="small" @click.stop="openChangeDialog(s.scheduleId, s.lineCode, s.lineName)">申请变更</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 我的变更记录 -->
      <el-divider v-if="myRequests.length" />
      <div v-if="myRequests.length">
        <h4 style="margin-bottom:10px">我的变更申请</h4>
        <el-table :data="myRequests" size="small" stripe>
          <el-table-column prop="createTime" label="时间" width="160" />
          <el-table-column prop="lineCode" label="产线" width="80" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">{{ row.requestType === 'REASSIGN' ? '换人' : '取消' }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : (row.status === 'REJECTED' ? 'danger' : 'warning')" size="small">
                {{ {PENDING:'待审',APPROVED:'已通过',REJECTED:'已拒绝'}[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审核意见" width="120" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 申请变更弹窗 -->
      <el-dialog v-model="changeDialog" title="申请变更" width="380px">
        <p style="color:#666;margin-bottom:12px">产线: {{ changingLine }}</p>
        <el-radio-group v-model="changeType" style="margin-bottom:12px">
          <el-radio value="REASSIGN">换人</el-radio>
          <el-radio value="CANCEL">取消排班</el-radio>
        </el-radio-group>
        <el-select v-if="changeType === 'REASSIGN'" v-model="changeTarget" placeholder="选择替换审核者" style="width:100%;margin-bottom:12px">
          <el-option v-for="i in inspectors" :key="i.userId" :label="i.realName" :value="i.userId" />
        </el-select>
        <el-input v-model="changeReason" type="textarea" :rows="3" placeholder="请填写申请理由" />
        <template #footer>
          <el-button @click="changeDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!changeReason" @click="submitChange">提交申请</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const now = new Date()
const date = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
const loading = ref(true)
const schedules = ref([])
const myRequests = ref([])
const changeDialog = ref(false)
const changeType = ref('REASSIGN')
const changeTarget = ref(null)
const changeReason = ref('')
const changingScheduleId = ref(null)
const changingLine = ref('')
const inspectors = ref([])

const load = async () => {
  loading.value = true
  const [sRes, rRes] = await Promise.all([
    request.get('/schedule/my-today'),
    request.get('/schedule/my-requests')
  ])
  if (sRes.code === 200) schedules.value = sRes.data
  if (rRes.code === 200) myRequests.value = rRes.data
  loading.value = false
}

const loadInspectors = async () => {
  const res = await request.get('/schedule/inspectors')
  if (res.code === 200) inspectors.value = res.data.filter(i => i.userId != userStore.userId)
}

const openChangeDialog = (scheduleId, lineCode, lineName) => {
  changingScheduleId.value = scheduleId
  changingLine.value = lineCode + ' - ' + lineName
  changeType.value = 'REASSIGN'
  changeTarget.value = null
  changeReason.value = ''
  changeDialog.value = true
}

const submitChange = async () => {
  const res = await request.post('/schedule/change-request', {
    scheduleId: changingScheduleId.value,
    requestType: changeType.value,
    reason: changeReason.value,
    proposedInspector: changeType.value === 'REASSIGN' ? changeTarget.value : null
  })
  if (res.code === 200) { ElMessage.success('申请已提交'); changeDialog.value = false; load() }
}

onMounted(() => { load(); loadInspectors() })
</script>

<style scoped>
.line-card { cursor: pointer; border-left: 4px solid #409EFF; }
.line-card:hover { background: #ecf5ff; }
</style>

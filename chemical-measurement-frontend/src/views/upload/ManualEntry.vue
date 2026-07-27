<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="manual-entry">
    <el-card>
      <template #header><span>手动录入检测数据</span></template>

      <el-steps :active="step" finish-status="success" align-center style="margin-bottom:25px">
        <el-step title="选择产线" />
        <el-step title="选择工位介质" />
        <el-step title="填写指标" />
        <el-step title="提交完成" />
      </el-steps>

      <!-- Step 0 -->
      <div v-if="step === 0">
        <el-radio-group v-model="selectedLine" size="large">
          <el-radio-button v-for="l in lines" :key="l.lineId" :value="l" style="margin:5px">
            {{ l.lineCode }} - {{ l.lineName }}
            <span style="color:#909399;font-size:11px;margin-left:4px">
              {{ '(' }}需检{{ l.needCount }} / 已检{{ l.doneCount }}
              <template v-if="l.abNormalUnfixed > 0">
                / 待复测{{ l.abNormalUnfixed }}{{ ')' }}
              </template>
              <template v-else>{{ ')' }}</template>
            </span>
          </el-radio-button>
        </el-radio-group>
        <div style="margin-top:20px">
          <el-button type="primary" :disabled="!selectedLine" @click="goStep1">下一步</el-button>
        </div>
      </div>

      <!-- Step 1 -->
      <div v-else-if="step === 1">
        <div style="margin-bottom:15px;font-size:14px">
          检测日期：<b>{{ todayStr }}</b>
          <el-button type="text" size="small" style="margin-left:20px" @click="$router.push('/history')">查看历史记录</el-button>
        </div>
        <br/>
        <div style="margin-bottom:6px"><b>选择工位：</b></div>
        <el-radio-group v-model="form.stationId" @change="loadMedia" size="default">
          <el-radio-button v-for="s in stations" :key="s.stationId" :value="s.stationId" style="margin:4px">
            {{ s.stationCode }}
            <span style="font-size:11px" :style="{color: s.needRetest > 0 ? '#F56C6C' : '#909399'}">
              {{ '(' + s.mediaStats + (s.needRetest > 0 ? ' / 待复测' + s.needRetest : '') + ')' }}
            </span>
          </el-radio-button>
        </el-radio-group>
        <br/>
        <div v-if="form.stationId" style="margin-top:15px;margin-bottom:6px"><b>选择介质：</b></div>
        <el-radio-group v-model="form.mediaId" size="default">
          <el-radio-button v-for="m in mediaOptions" :key="m.mediaId" :value="m.mediaId" :disabled="m.locked" style="margin:4px">
            {{ m.mediaCode }}
            <span style="font-size:11px" :style="{color: m.entryLabel.includes('异常') ? '#F56C6C' : (m.entryLabel === '未录入' ? '#409EFF' : '#67C23A')}">
              {{ '(' + m.entryLabel + ')' }}
            </span>
          </el-radio-button>
        </el-radio-group>
        <div v-if="selectedMediaInfo && selectedMediaInfo.entryLabel.includes('异常')" style="margin-top:10px">
          <el-button type="warning" @click="goRetest">
            复测 {{ selectedMediaInfo.mediaCode }}（{{ selectedMediaInfo.entryLabel }}）
          </el-button>
        </div>
        <div style="margin-top:20px">
          <el-button @click="backToStep0">上一步</el-button>
          <el-button type="primary" :disabled="!form.stationId || !form.mediaId" @click="goStep2">下一步</el-button>
        </div>
      </div>

      <!-- Step 2 -->
      <div v-else-if="step === 2" v-loading="loadingIndicators">
        <el-table :data="indicatorList" border stripe style="max-width:700px">
          <el-table-column prop="indicatorName" label="指标" width="140" />
          <el-table-column label="标准范围" width="160">
            <template #default="{ row }">
              <el-tooltip v-if="row.warnMin != null && row.standardMin == null && row.standardMax == null" content="该指标仅有警戒值，无标准上下限（如ET46折光浓度）" placement="top">
                <span style="color:#E6A23C;cursor:help;border-bottom:1px dashed #E6A23C">仅警戒值</span>
              </el-tooltip>
              <span v-else-if="row.standardMin != null && row.standardMax != null">{{ row.standardMin }} ~ {{ row.standardMax }}</span>
              <span v-else-if="row.standardMax != null">≤ {{ row.standardMax }}</span>
              <span v-else class="text-gray">—</span>
            </template>
          </el-table-column>
          <el-table-column label="警戒范围" width="160">
            <template #default="{ row }">
              <span v-if="row.warnMin != null && row.warnMax != null">{{ row.warnMin }} ~ {{ row.warnMax }}</span>
              <span v-else class="text-gray">—</span>
            </template>
          </el-table-column>
          <el-table-column label="实测值" min-width="180">
            <template #default="{ row }">
              <el-input-number v-model="row.inputValue" :precision="4" :step="0.1" size="small" style="width:130px" controls-position="right" />
              <span v-if="row.indicatorUnit" style="margin-left:5px;color:#999">{{ row.indicatorUnit }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:20px">
          <el-button @click="backToStep1">上一步</el-button>
          <el-button type="primary" @click="submitEntry" :loading="submitting">提交录入</el-button>
        </div>
      </div>

      <!-- Step 3 -->
      <div v-else>
        <el-result :icon="submitResult.icon" :title="submitResult.title" :sub-title="submitResult.desc">
          <template #extra>
            <el-button type="primary" @click="resetAll">继续录入</el-button>
            <el-button @click="$router.push('/inspection')">查看检测列表</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const step = ref(0)
const lines = ref([])
const selectedLine = ref(null)
const stations = ref([])
const mediaOptions = ref([])
const indicatorList = ref([])
const loadingIndicators = ref(false)
const submitting = ref(false)
const form = reactive({ stationId: null, mediaId: null })
const now = new Date()
const todayStr = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
const submitResult = reactive({ icon: 'success', title: '', desc: '' })

const loadLines = async () => {
  const res = await request.get('/manual/today-stats')
  if (res.code === 200) {
    let list = res.data
    if (userStore.isInspector) {
      // 优先用排班过滤，其次用managedLines
      const sr = await request.get('/schedule/check-today')
      const scheduledIds = (sr.code === 200 && sr.data) ? sr.data : []
      if (scheduledIds.length > 0) {
        list = list.filter(l => scheduledIds.includes(l.lineId))
      } else if (userStore.managedLines) {
        const allowed = userStore.managedLines.split(',')
        list = list.filter(l => allowed.includes(l.lineCode))
      } else {
        list = []
      }
    }
    lines.value = list
  }
}

const goStep1 = () => {
  form.stationId = null; form.mediaId = null
  loadWorkstations()
  step.value = 1
}

const disabledDate = (time) => {
  const today = new Date(new Date().toDateString()).getTime()
  return time.getTime() !== today
}

const backToStep0 = () => { step.value = 0; loadLines() }
const backToStep1 = () => { step.value = 1; loadWorkstations(); if (form.stationId) loadMedia() }

const selectedMediaInfo = computed(() => {
  return mediaOptions.value.find(m => m.mediaId === form.mediaId)
})

const goRetest = async () => {
  if (!form.stationId || !form.mediaId) return
  const res = await request.get('/manual/latest-abnormal', { params: { stationId: form.stationId, mediaId: form.mediaId } })
  if (res.code === 200 && res.data.recordId) {
    router.push('/retest/' + res.data.recordId)
  } else {
    ElMessage.warning('未找到异常记录')
  }
}

const loadWorkstations = async () => {
  const lineId = selectedLine.value.lineId
  const [wsRes, stRes] = await Promise.all([
    request.get('/manual/workstations', { params: { lineId } }),
    request.get('/manual/station-today-stats', { params: { lineId } })
  ])
  if (wsRes.code !== 200) return
  const statsMap = {}
  if (stRes.code === 200) {
    stRes.data.forEach(s => {
      statsMap[s.stationId] = { done: s.doneCount, total: s.total, needRetest: s.needRetest }
    })
  }
  stations.value = wsRes.data.map(s => {
    const st = statsMap[s.stationId] || { done: 0, total: 0, needRetest: 0 }
    return { ...s, mediaStats: '已检' + st.done + '/' + st.total, needRetest: st.needRetest }
  })
  mediaOptions.value = []
}

const loadMedia = async () => {
  if (!form.stationId) return
  const res = await request.get('/manual/media-options', { params: { stationId: form.stationId } })
  if (res.code !== 200) return
  const list = res.data
  for (const m of list) {
    const hRes = await request.get('/manual/entry-history', { params: { stationId: form.stationId, mediaId: m.mediaId } })
    m.entryLabel = (hRes.code === 200) ? hRes.data.label : '?'
    m.locked = (hRes.code === 200) ? hRes.data.locked : false
  }
  mediaOptions.value = list
  form.mediaId = null; indicatorList.value = []
}

const goStep2 = () => {
  if (selectedMediaInfo.value && selectedMediaInfo.value.entryLabel.includes('异常')) {
    goRetest()
    return
  }
  loadIndicators()
  step.value = 2
}

const loadIndicators = async () => {
  loadingIndicators.value = true
  try {
    const res = await request.get('/manual/indicators', { params: { stationId: form.stationId, mediaId: form.mediaId } })
    if (res.code === 200) {
      let list = res.data.map(d => ({ ...d, inputValue: null }))
      if (userStore.isInspector) {
        list = list.filter(d => d.indicatorName.includes('pH') || d.indicatorName === '浓度')
      }
      indicatorList.value = list
    }
  } finally { loadingIndicators.value = false }
}

const submitEntry = async () => {
  const vals = indicatorList.value.filter(d => d.inputValue != null).map(d => ({ indicatorId: d.indicatorId, value: d.inputValue }))
  if (!vals.length) { ElMessage.warning('请至少填写一项指标'); return }
  submitting.value = true
  try {
    const res = await request.post('/manual/entry', { stationId: form.stationId, mediaId: form.mediaId, inspectionDate: todayStr, values: vals })
    if (res.code === 200) {
      const d = res.data
      const lbl = { 1: '正常', 2: '预警', 3: '超差' }
      submitResult.icon = d.status === 3 ? 'error' : (d.status === 2 ? 'warning' : 'success')
      submitResult.title = '录入完成 - ' + lbl[d.status]
      submitResult.desc = '预警' + d.warnCount + '项, 超差' + d.overCount + '项, 记录ID: ' + d.recordId
      step.value = 3
    }
  } finally { submitting.value = false }
}

const resetAll = () => {
  step.value = 0; selectedLine.value = null
  form.stationId = null; form.mediaId = null
  indicatorList.value = []; loadLines()
}

onMounted(loadLines)
</script>

<style scoped>
.text-gray { color: #ccc; }
</style>

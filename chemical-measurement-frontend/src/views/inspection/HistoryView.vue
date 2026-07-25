<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="history-view">
    <el-card>
      <template #header><span>历史检测记录查看</span></template>

      <!-- Step 0: 选产线 -->
      <div v-if="step === 0">
        <el-radio-group v-model="selectedLine" size="large">
          <el-radio-button v-for="l in lines" :key="l.lineId" :value="l" style="margin:5px">
            {{ l.lineCode }} - {{ l.lineName }}
          </el-radio-button>
        </el-radio-group>
        <div style="margin-top:20px">
          <el-button type="primary" :disabled="!selectedLine" @click="goStep1">下一步</el-button>
        </div>
      </div>

      <!-- Step 1: 选工位+介质+日期 -->
      <div v-else-if="step === 1">
        <el-date-picker v-model="form.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="margin-bottom:15px" />
        <br/>
        <div style="margin-bottom:6px"><b>选择工位：</b></div>
        <el-radio-group v-model="form.stationId" @change="loadMedia" size="default">
          <el-radio-button v-for="s in stations" :key="s.stationId" :value="s.stationId" style="margin:4px">{{ s.stationCode }}</el-radio-button>
        </el-radio-group>
        <br/>
        <div v-if="form.stationId" style="margin-top:15px;margin-bottom:6px"><b>选择介质：</b></div>
        <el-radio-group v-model="form.mediaId" @change="loadRecords" size="default">
          <el-radio-button v-for="m in mediaOptions" :key="m.mediaId" :value="m.mediaId" style="margin:4px">{{ m.mediaCode }}</el-radio-button>
        </el-radio-group>
        <div style="margin-top:20px">
          <el-button @click="step=0">返回</el-button>
          <el-button type="primary" :disabled="!form.date || !form.mediaId" @click="loadRecords">查看</el-button>
        </div>
      </div>

      <!-- Step 2: 查看记录 -->
      <div v-else v-loading="loading">
        <div style="margin-bottom:15px">
          <el-button @click="step=1">返回选择</el-button>
        </div>
        <div v-if="records.length === 0">
          <el-empty description="该日期无检测记录" />
        </div>
        <div v-for="(rec, idx) in records" :key="rec.recordId" style="margin-bottom:20px">
          <el-card shadow="hover" size="small">
            <template #header>
              <span>第{{ idx + 1 }}次录入 | {{ rec.createTime }}</span>
              <el-tag :type="statusTag(rec.status)" size="small" style="margin-left:10px">{{ statusLabel(rec.status) }}</el-tag>
            </template>
            <el-table :data="rec.values" border size="small" style="max-width:650px">
              <el-table-column prop="indicatorName" label="指标" width="140" />
              <el-table-column label="标准范围" width="140">
                <template #default="{ row }">
                  <el-tooltip v-if="row.warnMin != null && row.standardMin == null && row.standardMax == null" content="该指标仅有警戒值，无标准上下限" placement="top">
                    <span style="color:#E6A23C;cursor:help;border-bottom:1px dashed #E6A23C">仅警戒值</span>
                  </el-tooltip>
                  <span v-else-if="row.standardMin != null && row.standardMax != null">{{ row.standardMin }} ~ {{ row.standardMax }}</span>
                  <span v-else-if="row.standardMax != null">≤ {{ row.standardMax }}</span>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="检测值" width="120">
                <template #default="{ row }">
                  <span :style="{color: row.warnStatus === 2 ? '#F56C6C' : (row.warnStatus === 1 ? '#E6A23C' : '#333'), fontWeight: row.warnStatus > 0 ? 'bold' : 'normal'}">
                    {{ row.finalValue }} {{ row.indicatorUnit }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.warnStatus === 2 ? 'danger' : (row.warnStatus === 1 ? 'warning' : 'success')" size="small">
                    {{ row.warnStatus === 2 ? '超差' : (row.warnStatus === 1 ? '预警' : '正常') }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { productionLineApi } from '@/api'

const step = ref(0)
const lines = ref([])
const selectedLine = ref(null)
const stations = ref([])
const mediaOptions = ref([])
const records = ref([])
const loading = ref(false)
const form = reactive({ date: '', stationId: null, mediaId: null })

const statusTag = (s) => ({ 1: 'success', 2: 'warning', 3: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ 1: '正常', 2: '预警', 3: '超差', 0: '待勘正' }[s] || '未知')

const loadLines = async () => {
  const res = await productionLineApi.list()
  if (res.code === 200) lines.value = res.data
}

const goStep1 = () => {
  loadWorkstations()
  step.value = 1
}

const loadWorkstations = async () => {
  const res = await request.get('/manual/workstations', { params: { lineId: selectedLine.value.lineId } })
  if (res.code === 200) stations.value = res.data
  form.stationId = null; mediaOptions.value = []
}

const loadMedia = async () => {
  if (!form.stationId) return
  const res = await request.get('/manual/media-options', { params: { stationId: form.stationId } })
  if (res.code === 200) mediaOptions.value = res.data
  form.mediaId = null; records.value = []
}

const loadRecords = async () => {
  if (!form.stationId || !form.mediaId || !form.date) return
  if (step.value === 1) step.value = 2
  loading.value = true
  try {
    // 获取该工位介质指定日期的所有记录
    const params = { stationId: form.stationId, mediaId: form.mediaId, date: form.date }
    const res = await request.get('/manual/history-records', { params })
    if (res.code === 200) records.value = res.data
  } finally { loading.value = false }
}

onMounted(loadLines)
</script>

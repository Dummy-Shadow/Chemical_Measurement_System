<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #409EFF">
          <div class="stat-value">{{ stats.detectionCount }}</div>
          <div class="stat-title">检测次数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #67C23A">
          <div class="stat-value">{{ stats.completedCount }}</div>
          <div class="stat-title">已完成</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #F56C6C">
          <div class="stat-value">{{ stats.pendingRetestCount }}</div>
          <div class="stat-title">待复测</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #E6A23C">
          <div class="stat-value">{{ stats.abnormalCount }}</div>
          <div class="stat-title">异常项目</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #909399">
          <div v-if="stats.spotCheckAllDone" class="stat-text" style="color:#67C23A">本周抽检已完成</div>
          <div v-else class="stat-text">
            已完成抽检 <b>{{ stats.completedSpotCheckLines || 0 }}</b> / <b>{{ stats.totalLines || 0 }}</b> 条产线
          </div>
          <div class="stat-sub">（未检测 {{ stats.pendingSpotCheckLines || 0 }} 条）</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>近7天检测趋势</span></template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>预警分布</span></template>
          <div ref="pieChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header><span>本周异常项目清单</span></template>
      <el-table :data="abnormalList" stripe size="small" empty-text="本周无异常项目">
        <el-table-column label="工位" prop="stationName" width="120" />
        <el-table-column label="介质" prop="mediaName" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'danger' : 'warning'" size="small">
              {{ row.status === 3 ? '超差' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发现方式" width="80">
          <template #default="{ row }">
            <el-tag :type="row.inspectionType === '抽检' ? 'info' : ''" size="small">
              {{ row.inspectionType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常指标" min-width="250">
          <template #default="{ row }">
            <template v-for="(ind, idx) in row.abnormalIndicators" :key="idx">
              <el-tag :type="ind.warnStatus === 2 ? 'danger' : 'warning'" size="small" style="margin:2px">
                {{ ind.indicatorName }}: {{ ind.value }}{{ ind.indicatorUnit }}
              </el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="日期" prop="date" width="110" />
      </el-table>
    </el-card>

    <el-card style="margin-top:20px" v-if="userStore.isAreaManager || userStore.isDeveloper">
      <template #header><span>数据导出</span></template>
      <el-button type="primary" @click="exportDaily" :loading="exportingDaily" icon="Download">
        导出审核者日常检测（本周）
      </el-button>
      <el-button type="warning" @click="exportSpot" :loading="exportingSpot" icon="Download" style="margin-left:12px">
        导出管理者抽检数据（本周）
      </el-button>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>
        <span>智能知识库推荐</span>
        <el-button type="text" size="small" style="float:right" @click="$router.push('/knowledge')">查看更多</el-button>
      </template>
      <el-table :data="knowledgeList" stripe size="small">
        <el-table-column prop="title" label="问题" width="160" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="catTagType(row.category)" size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="symptom" label="异常现象" min-width="200" show-overflow-tooltip />
        <el-table-column prop="solution" label="处理措施" min-width="250" show-overflow-tooltip />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority === 3 ? 'danger' : (row.priority === 2 ? 'warning' : 'info')" size="small">
              {{ ['', '低', '中', '高'][row.priority] }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { dashboardApi, knowledgeApi, exportApi } from '@/api'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const stats = reactive({
  detectionCount: 0, completedCount: 0, pendingRetestCount: 0,
  abnormalCount: 0, normalCount: 0, retestOkCount: 0,
  warnCount: 0, overCount: 0,
  completedSpotCheckLines: 0, pendingSpotCheckLines: 0, totalLines: 0,
  spotCheckAllDone: false
})
const abnormalList = ref([])
const knowledgeList = ref([])
const trendChart = ref(null)
const pieChart = ref(null)

const catTagType = (cat) => {
  const map = { '浓度异常': 'warning', 'pH异常': 'danger', '细菌超标': 'danger', '电导率异常': 'info' }
  return map[cat] || ''
}

const loadStats = async () => {
  const res = await dashboardApi.stats()
  if (res.code === 200) Object.assign(stats, res.data)
}

const loadAbnormal = async () => {
  const res = await dashboardApi.weeklyAbnormal()
  if (res.code === 200) abnormalList.value = res.data
}

const loadTrend = async () => {
  const res = await dashboardApi.trend()
  if (res.code !== 200 || !trendChart.value) return
  const data = res.data
  const chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['正常', '复测正常', '预警', '超差'] },
    xAxis: { type: 'category', data: data.map(d => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '正常', type: 'line', data: data.map(d => d.normal), smooth: true, color: '#67C23A' },
      { name: '复测正常', type: 'line', data: data.map(d => d.retestOk), smooth: true, color: '#409EFF' },
      { name: '预警', type: 'line', data: data.map(d => d.warn), smooth: true, color: '#E6A23C' },
      { name: '超差', type: 'line', data: data.map(d => d.over), smooth: true, color: '#F56C6C' }
    ]
  })
}

const loadPie = async () => {
  await nextTick()
  if (!pieChart.value) return
  const chart = echarts.init(pieChart.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: [
        { value: stats.normalCount, name: '正常' },
        { value: stats.retestOkCount, name: '复测正常' },
        { value: stats.warnCount, name: '预警' },
        { value: stats.overCount, name: '超差' }
      ],
      color: ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C']
    }]
  })
}

const exportingDaily = ref(false)
const exportingSpot = ref(false)

const doExport = async (apiFn, filenamePrefix) => {
  const res = await apiFn()
  if (res.code !== 200) { ElMessage.error('导出失败'); return }
  const data = res.data || []
  if (!data.length) { ElMessage.warning('无数据可导出'); return }
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  const now = new Date()
  const dstr = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
  XLSX.writeFile(wb, `${filenamePrefix}_${dstr}.xlsx`)
  ElMessage.success('导出成功')
}

const exportDaily = async () => {
  exportingDaily.value = true
  try { await doExport(() => exportApi.dailyInspection(), 'PFEP日常检测') } 
  finally { exportingDaily.value = false }
}

const exportSpot = async () => {
  exportingSpot.value = true
  try { await doExport(() => exportApi.spotCheck(), 'PFEP抽检数据') }
  finally { exportingSpot.value = false }
}

const loadKnowledge = async () => {
  const res = await knowledgeApi.list({ page: 1, size: 5 })
  if (res.code === 200) knowledgeList.value = res.data.records
}

onMounted(async () => {
  await loadStats()
  loadAbnormal()
  loadTrend()
  loadPie()
  loadKnowledge()
})
</script>

<style scoped>
.stat-card { border-left: 4px solid; cursor: pointer; }
.stat-card .stat-value { font-size: 28px; font-weight: bold; color: #333; }
.stat-card .stat-title { font-size: 14px; color: #999; margin-top: 8px; }
.stat-card .stat-text { font-size: 16px; color: #333; line-height: 1.6; }
.stat-card .stat-text b { font-size: 24px; margin: 0 4px; }
.stat-card .stat-sub { font-size: 12px; color: #999; margin-top: 4px; }
</style>

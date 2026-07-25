<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #409EFF">
          <div class="stat-value">{{ stats.todayTotal }}</div>
          <div class="stat-title">今日检测</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #67C23A">
          <div class="stat-value">{{ stats.normalCount }}</div>
          <div class="stat-title">正常</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #409EFF">
          <div class="stat-value">{{ stats.retestOkCount }}</div>
          <div class="stat-title">复测正常</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left-color: #E6A23C">
          <div class="stat-value">{{ stats.warnCount }}</div>
          <div class="stat-title">预警</div>
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
import { dashboardApi, knowledgeApi } from '@/api'
import * as echarts from 'echarts'

const stats = reactive({ todayTotal: 0, normalCount: 0, warnCount: 0, overCount: 0, retestOkCount: 0 })
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

const loadKnowledge = async () => {
  const res = await knowledgeApi.list({ page: 1, size: 5 })
  if (res.code === 200) knowledgeList.value = res.data.records
}

onMounted(async () => {
  await loadStats()
  loadTrend()
  loadPie()
  loadKnowledge()
})
</script>

<style scoped>
.stat-card { border-left: 4px solid; cursor: pointer; }
.stat-card .stat-value { font-size: 28px; font-weight: bold; color: #333; }
.stat-card .stat-title { font-size: 14px; color: #999; margin-top: 8px; }
</style>

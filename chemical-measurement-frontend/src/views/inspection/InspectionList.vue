<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="inspection-list">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>检测数据查询</span>
          <el-button type="success" @click="handleExport" :disabled="!tableData.length">
            <el-icon><Download /></el-icon>导出Excel
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="产线">
          <el-select v-model="query.lineId" placeholder="全部" clearable style="width:140px" @change="handleSearch">
            <el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" @change="handleSearch" />
        </el-form-item>
        <el-form-item label="预警状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="预警" :value="2" />
            <el-option label="超差" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe style="width:100%" v-loading="loading">
        <el-table-column prop="recordId" label="编号" width="80" />
        <el-table-column prop="inspectionDate" label="检测日期" width="120" />
        <el-table-column prop="lineCode" label="产线" width="80" />
        <el-table-column prop="stationCode" label="工位" width="110" />
        <el-table-column prop="mediaCode" label="介质牌号" min-width="160" />
        <el-table-column prop="entryType" label="录入方式" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.entryType === 'OCR' ? 'success' : 'info'" size="small">
              {{ row.entryType === 'OCR' ? '拍照' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/inspection/${row.recordId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" layout="total, prev, pager, next" @current-change="handleSearch"
        style="margin-top:20px; justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { productionLineApi } from '@/api'

const lines = ref([])
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ lineId: null, date: '', status: null, page: 1, size: 10 })

const statusTag = (s) => ({ 1: 'success', 2: 'warning', 3: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ 1: '正常', 2: '预警', 3: '超差', 0: '待勘正' }[s] || '未知')

const loadLines = async () => {
  const res = await productionLineApi.list()
  if (res.code === 200) lines.value = res.data
}

const handleSearch = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.lineId) params.lineId = query.lineId
    if (query.date) { params.dateFrom = query.date; params.dateTo = query.date }
    if (query.status) params.status = query.status
    const res = await request.get('/manual/records', { params })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  query.lineId = null
  query.date = ''
  query.status = null
  query.page = 1
  handleSearch()
}

const handleExport = () => {
  const data = tableData.value.map(r => ({
    '编号': r.recordId, '检测日期': r.inspectionDate, '产线': r.lineCode,
    '工位': r.stationCode, '介质牌号': r.mediaCode,
    '录入方式': r.entryType === 'OCR' ? '拍照' : '手动', '预警状态': statusLabel(r.status)
  }))
  const ws = XLSX.utils.json_to_sheet(data)
  ws['!cols'] = [{wch:8},{wch:14},{wch:10},{wch:14},{wch:22},{wch:10},{wch:10}]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '检测数据')
  const dn = new Date()
  const dstr = dn.getFullYear() + '-' + String(dn.getMonth()+1).padStart(2,'0') + '-' + String(dn.getDate()).padStart(2,'0')
  XLSX.writeFile(wb, `PFEP检测数据_${dstr}.xlsx`)
  ElMessage.success('导出成功')
}

onMounted(() => { loadLines(); handleSearch() })
</script>

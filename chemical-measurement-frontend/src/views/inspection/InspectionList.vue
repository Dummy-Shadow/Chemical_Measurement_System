<template>
  <div class="inspection-list">
    <el-card>
      <template #header>
        <span>检测数据查询</span>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="日期范围">
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="产线">
          <el-select v-model="query.lineId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警状态">
          <el-select v-model="query.warnStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="正常" :value="0" />
            <el-option label="预警" :value="1" />
            <el-option label="超差" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="recordId" label="编号" width="80" />
        <el-table-column prop="inspectionDate" label="检测日期" width="120" />
        <el-table-column prop="lineName" label="产线" width="100" />
        <el-table-column prop="stationName" label="工位" width="110" />
        <el-table-column prop="mediaName" label="介质牌号" min-width="150" />
        <el-table-column label="预警状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="warnTagType(row.status)" size="small">
              {{ warnLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/inspection/${row.recordId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top:20px; justify-content:flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const lines = ref([])
const tableData = ref([])
const total = ref(0)

const query = reactive({
  dateRange: [],
  lineId: null,
  warnStatus: null,
  page: 1,
  pageSize: 10
})

const handleSearch = () => { /* TODO: implement */ }
const handleReset = () => { Object.assign(query, { dateRange: [], lineId: null, warnStatus: null, page: 1 }) }

const warnTagType = (status) => ({ 0: 'success', 1: 'warning', 2: 'danger' }[status] || 'info')
const warnLabel = (status) => ({ 0: '正常', 1: '预警', 2: '超差' }[status] || '未知')
</script>

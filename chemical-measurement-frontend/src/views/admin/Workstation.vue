<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <span>工位管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增工位</el-button>
      </template>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="stationId" label="ID" width="80" />
        <el-table-column prop="stationCode" label="编码" width="120" />
        <el-table-column prop="stationName" label="名称" min-width="150" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { workstationApi } from '@/api'

const tableData = ref([])
const loadData = async () => {
  const res = await workstationApi.list()
  if (res.code === 200) tableData.value = res.data
}
const handleAdd = () => { /* TODO */ }
const handleEdit = (row) => { /* TODO */ }
const handleDelete = (row) => { /* TODO */ }
loadData()
</script>

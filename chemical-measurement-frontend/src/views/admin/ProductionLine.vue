<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <span>产线管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增产线</el-button>
      </template>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="lineId" label="ID" width="80" />
        <el-table-column prop="lineCode" label="编码" width="100" />
        <el-table-column prop="lineName" label="名称" min-width="150" />
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
import { productionLineApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loadData = async () => {
  const res = await productionLineApi.list()
  if (res.code === 200) tableData.value = res.data
}
const handleAdd = () => { /* TODO */ }
const handleEdit = (row) => { /* TODO */ }
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除产线 "${row.lineName}" 吗？`, '警告', { type: 'warning' })
    .then(async () => {
      await productionLineApi.delete(row.lineId)
      ElMessage.success('删除成功')
      loadData()
    })
}
loadData()
</script>

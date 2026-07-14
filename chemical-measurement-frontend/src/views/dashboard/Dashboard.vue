<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card" :style="{ borderLeftColor: card.color }">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-title">{{ card.title }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="welcome-card" style="margin-top:20px">
      <h3>欢迎使用PFEP车间冷却介质检测数据管理系统</h3>
      <p>本系统用于管理车间冷却介质（乳化液、珩磨液、清洗液、淬火液、去离子水）的日常检测数据，支持拍照上传、OCR识别、自动预警等功能。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const cards = ref([
  { title: '今日检测', value: '--', color: '#409EFF' },
  { title: '预警数量', value: '--', color: '#E6A23C' },
  { title: '超差数量', value: '--', color: '#F56C6C' },
  { title: '待勘正', value: '--', color: '#909399' }
])
</script>

<style scoped>
.stat-card { border-left: 4px solid; }
.stat-card .stat-value { font-size: 28px; font-weight: bold; color: #333; }
.stat-card .stat-title { font-size: 14px; color: #999; margin-top: 8px; }
.welcome-card h3 { color: #333; }
.welcome-card p { color: #666; line-height: 1.8; }
</style>

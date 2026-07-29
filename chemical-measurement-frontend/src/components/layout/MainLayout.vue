<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <el-container class="main-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo"><h2>PFEP检测系统</h2></div>
      <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>

        <!-- 开发者 + 分区管理者可见 -->
        <el-menu-item index="/upload" v-if="userStore.role !== 'INSPECTOR'">
          <el-icon><Upload /></el-icon><span>拍照上传</span>
        </el-menu-item>

        <!-- 全部可见 -->
        <el-menu-item index="/manual-entry">
          <el-icon><EditPen /></el-icon><span>手动录入</span>
        </el-menu-item>

        <!-- 全部可见 -->
        <el-menu-item index="/history">
          <el-icon><Clock /></el-icon><span>历史记录</span>
        </el-menu-item>

        <!-- 全部可见 -->
        <el-menu-item index="/inspection">
          <el-icon><Document /></el-icon><span>检测数据</span>
        </el-menu-item>

        <!-- 开发者 + 分区管理者可见 -->
        <el-menu-item index="/knowledge" v-if="userStore.role !== 'INSPECTOR'">
          <el-icon><Collection /></el-icon><span>知识库</span>
        </el-menu-item>

        <!-- 开发者 + 分区管理者可见 -->
        <el-menu-item index="/schedule" v-if="userStore.role !== 'INSPECTOR'">
          <el-icon><Calendar /></el-icon><span>排班管理</span>
        </el-menu-item>

        <!-- 仅审核者 -->
        <el-menu-item index="/my-schedule" v-if="userStore.role === 'INSPECTOR'">
          <el-icon><Calendar /></el-icon><span>我的排班</span>
        </el-menu-item>

        <!-- 仅开发者 -->
        <el-menu-item index="/admin" v-if="userStore.role === 'DEVELOPER' || userStore.role === 'AREA_MANAGER'">
          <el-icon><Setting /></el-icon><span>系统管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-right">
          <el-tag :type="roleTag" size="small" style="margin-right:10px">{{ roleLabel }}</el-tag>
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-button type="primary" text size="small" @click="$router.push('/profile')">个人信息</el-button>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/admin')) return '/admin'
  return path
})

const roleLabel = computed(() => ({
  DEVELOPER: '开发者', AREA_MANAGER: '分区管理者', INSPECTOR: '单日审核者'
}[userStore.role] || userStore.role))

const roleTag = computed(() => ({
  DEVELOPER: 'danger', AREA_MANAGER: 'warning', INSPECTOR: 'info'
}[userStore.role] || 'info'))

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { userStore.logout(); router.push('/login') }).catch(() => {})
}
</script>

<style scoped>
.main-container { height: 100vh; }
.sidebar { background-color: #304156; overflow-y: auto; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo h2 { font-size: 16px; margin: 0; }
.header { background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.username { color: #666; }
.main-content { background: #f0f2f5; padding: 20px; overflow-y: auto; }
</style>

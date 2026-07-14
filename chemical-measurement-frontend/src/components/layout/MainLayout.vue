<template>
  <el-container class="main-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h2>PFEP检测系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/upload">
          <el-icon><Upload /></el-icon>
          <span>数据上传</span>
        </el-menu-item>
        <el-menu-item index="/inspection">
          <el-icon><Document /></el-icon>
          <span>检测数据</span>
        </el-menu-item>
        <el-sub-menu index="admin" v-if="userStore.role === 'ADMIN'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/production-line">产线管理</el-menu-item>
          <el-menu-item index="/admin/workstation">工位管理</el-menu-item>
          <el-menu-item index="/admin/media">介质管理</el-menu-item>
          <el-menu-item index="/admin/indicator-config">指标配置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
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

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
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

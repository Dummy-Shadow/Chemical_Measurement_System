// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const R = { DEV: 'DEVELOPER', MGR: 'AREA_MANAGER', INS: 'INSPECTOR' }

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Dashboard.vue'), meta: { title: '首页' } },
      { path: 'upload', name: 'Upload', component: () => import('@/views/upload/Upload.vue'), meta: { title: '拍照上传', roles: [R.DEV, R.MGR] } },
      { path: 'manual-entry', name: 'ManualEntry', component: () => import('@/views/upload/ManualEntry.vue'), meta: { title: '手动录入' } },
      { path: 'retest/:id', name: 'RetestEntry', component: () => import('@/views/upload/RetestEntry.vue'), meta: { title: '复测录入' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/login/Profile.vue'), meta: { title: '个人信息' } },
      { path: 'history', name: 'History', component: () => import('@/views/inspection/HistoryView.vue'), meta: { title: '历史记录', roles: [R.MGR, R.INS] } },
      { path: 'inspection', name: 'InspectionList', component: () => import('@/views/inspection/InspectionList.vue'), meta: { title: '检测数据' } },
      { path: 'inspection/:id', name: 'InspectionDetail', component: () => import('@/views/inspection/InspectionDetail.vue'), meta: { title: '检测详情' } },
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/inspection/Knowledge.vue'), meta: { title: '知识库' } },
      { path: 'schedule', name: 'Schedule', component: () => import('@/views/admin/ScheduleManagement.vue'), meta: { title: '排班管理', roles: [R.MGR] } },
      { path: 'my-schedule', name: 'MySchedule', component: () => import('@/views/admin/MySchedule.vue'), meta: { title: '我的排班', roles: [R.INS] } },
      { path: 'admin', name: 'Admin', component: () => import('@/views/admin/SystemManagement.vue'), meta: { title: '系统管理', roles: [R.DEV, R.MGR] } }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - PFEP检测系统` : 'PFEP检测系统'

  if (to.path === '/login') { next(); return }
  if (!userStore.token) { next('/login'); return }

  const allowed = to.meta.roles
  if (allowed && !allowed.includes(userStore.role)) {
    if (userStore.role === R.INS) next('/my-schedule')
    else if (userStore.role === R.MGR) next('/schedule')
    else next('/dashboard')
    return
  }
  next()
})

export default router

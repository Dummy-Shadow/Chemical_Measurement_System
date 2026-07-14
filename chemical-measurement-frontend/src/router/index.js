import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

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
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'upload',
        name: 'Upload',
        component: () => import('@/views/upload/Upload.vue'),
        meta: { title: '数据上传' }
      },
      {
        path: 'correction/:id?',
        name: 'Correction',
        component: () => import('@/views/correction/Correction.vue'),
        meta: { title: '数据勘正' }
      },
      {
        path: 'inspection',
        name: 'InspectionList',
        component: () => import('@/views/inspection/InspectionList.vue'),
        meta: { title: '检测数据' }
      },
      {
        path: 'inspection/:id',
        name: 'InspectionDetail',
        component: () => import('@/views/inspection/InspectionDetail.vue'),
        meta: { title: '检测详情' }
      },
      {
        path: 'admin',
        name: 'Admin',
        redirect: '/admin/workstation',
        meta: { title: '系统管理', requireAdmin: true },
        children: [
          {
            path: 'production-line',
            name: 'AdminProductionLine',
            component: () => import('@/views/admin/ProductionLine.vue'),
            meta: { title: '产线管理' }
          },
          {
            path: 'workstation',
            name: 'AdminWorkstation',
            component: () => import('@/views/admin/Workstation.vue'),
            meta: { title: '工位管理' }
          },
          {
            path: 'media',
            name: 'AdminMedia',
            component: () => import('@/views/admin/Media.vue'),
            meta: { title: '介质管理' }
          },
          {
            path: 'indicator-config',
            name: 'AdminIndicatorConfig',
            component: () => import('@/views/admin/IndicatorConfig.vue'),
            meta: { title: '指标配置' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - PFEP检测系统` : 'PFEP检测系统'

  if (to.path === '/login') {
    next()
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }
  if (to.meta.requireAdmin && userStore.role !== 'ADMIN') {
    next('/dashboard')
    return
  }
  next()
})

export default router

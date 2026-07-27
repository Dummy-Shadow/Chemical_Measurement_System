// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    userId: sessionStorage.getItem('userId') || '',
    username: sessionStorage.getItem('username') || '',
    realName: sessionStorage.getItem('realName') || '',
    role: sessionStorage.getItem('role') || '',
    managedLines: sessionStorage.getItem('managedLines') || ''
  }),
  getters: {
    isDeveloper: (state) => state.role === 'DEVELOPER',
    isAreaManager: (state) => state.role === 'AREA_MANAGER',
    isInspector: (state) => state.role === 'INSPECTOR',
    canManageSystem: (state) => state.role === 'DEVELOPER',
    canManageSchedule: (state) => state.role === 'AREA_MANAGER',
    canEditAllIndicators: (state) => state.role !== 'INSPECTOR',
  },
  actions: {
    setUser(data) {
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.role = data.role
      this.managedLines = data.managedLines || ''
      sessionStorage.setItem('token', data.token)
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('username', data.username)
      sessionStorage.setItem('realName', data.realName)
      sessionStorage.setItem('role', data.role)
      sessionStorage.setItem('managedLines', data.managedLines || '')
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      this.managedLines = ''
      sessionStorage.clear()
    }
  }
})

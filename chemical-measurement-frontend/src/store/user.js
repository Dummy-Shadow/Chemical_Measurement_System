// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || '',
    managedLines: localStorage.getItem('managedLines') || ''
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
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', data.userId)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName)
      localStorage.setItem('role', data.role)
      localStorage.setItem('managedLines', data.managedLines || '')
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      this.managedLines = ''
      localStorage.clear()
    }
  }
})

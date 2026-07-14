import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || ''
  }),
  actions: {
    setUser(data) {
      this.token = data.token
      this.username = data.username
      this.realName = data.realName
      this.role = data.role
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName)
      localStorage.setItem('role', data.role)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      localStorage.clear()
    }
  }
})

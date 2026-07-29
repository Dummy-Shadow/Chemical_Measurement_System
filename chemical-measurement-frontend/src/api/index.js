// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

import request from '@/utils/request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

export const productionLineApi = {
  list: () => request.get('/production-line'),
  create: (data) => request.post('/production-line', data),
  update: (id, data) => request.put(`/production-line/${id}`, data),
  delete: (id) => request.delete(`/production-line/${id}`)
}

export const workstationApi = {
  list: (lineId) => request.get('/workstation', { params: { lineId } }),
  create: (data) => request.post('/workstation', data),
  update: (id, data) => request.put(`/workstation/${id}`, data),
  delete: (id) => request.delete(`/workstation/${id}`)
}

export const mediaApi = {
  list: (categoryId) => request.get('/media', { params: { categoryId } }),
  create: (data) => request.post('/media', data),
  update: (id, data) => request.put(`/media/${id}`, data),
  delete: (id) => request.delete(`/media/${id}`)
}

export const dashboardApi = {
  stats: () => request.get('/dashboard/stats'),
  trend: () => request.get('/dashboard/trend'),
  weeklyAbnormal: () => request.get('/dashboard/weekly-abnormal')
}

export const knowledgeApi = {
  list: (params) => request.get('/knowledge', { params }),
  detail: (id) => request.get(`/knowledge/${id}`),
  recommend: (params) => request.get('/knowledge/recommend', { params })
}

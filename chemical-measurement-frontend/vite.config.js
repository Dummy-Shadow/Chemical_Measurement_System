// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import obfuscator from 'rollup-plugin-obfuscator'

export default defineConfig({
  plugins: [
    vue(),
    ...(process.env.NODE_ENV === 'production' ? [obfuscator({
      compact: true,
      controlFlowFlattening: false,
      deadCodeInjection: false,
      simplify: true,
      stringArrayEncoding: ['base64'],
      stringArrayThreshold: 0.5
    })] : [])
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
})

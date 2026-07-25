<template>
  <div class="retest-entry">
    <el-card>
      <template #header>
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <span style="margin-left:10px;font-weight:bold">复测录入</span>
      </template>

      <div v-if="loading" style="text-align:center;padding:40px">加载中...</div>
      <div v-else-if="!data">
        <el-empty description="未找到记录" />
      </div>
      <div v-else>
        <p style="color:#666;margin-bottom:15px">
          原记录 ID: {{ data.recordId }} |
          状态: <el-tag :type="data.status===3?'danger':(data.status===2?'warning':'success')" size="small">
            {{ {1:'正常',2:'预警',3:'超差'}[data.status] }}
          </el-tag>
        </p>

        <el-table :data="indicatorList" border stripe style="max-width:750px">
          <el-table-column prop="indicatorName" label="指标" width="120" />
          <el-table-column label="原值" width="120">
            <template #default="{ row }">
              <span :style="{color: row.warnStatus > 0 ? '#F56C6C' : '#333', fontWeight: row.warnStatus > 0 ? 'bold' : 'normal'}">
                {{ row.value }} {{ row.indicatorUnit }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="标准范围" width="140">
            <template #default="{ row }">
              <el-tooltip v-if="row.warnMin != null && row.standardMin == null && row.standardMax == null" content="仅警戒值" placement="top">
                <span style="color:#E6A23C;cursor:help;border-bottom:1px dashed #E6A23C">仅警戒值</span>
              </el-tooltip>
              <span v-else-if="row.standardMin != null && row.standardMax != null">{{ row.standardMin }} ~ {{ row.standardMax }}</span>
              <span v-else-if="row.standardMax != null">≤ {{ row.standardMax }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="警戒范围" width="140">
            <template #default="{ row }">
              <span v-if="row.warnMin != null && row.warnMax != null">{{ row.warnMin }} ~ {{ row.warnMax }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="复测值" min-width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.newVal" :precision="4" :step="0.1" size="small" style="width:130px" controls-position="right" />
              <span v-if="row.indicatorUnit" style="margin-left:4px;color:#999">{{ row.indicatorUnit }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:20px">
          <el-button type="primary" @click="submitRetest" :loading="submitting">提交复测</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top:15px" v-if="chain.length > 0">
      <template #header><span>复测链</span></template>
      <el-timeline>
        <el-timeline-item v-for="(item, idx) in chain" :key="idx"
          :timestamp="item.createTime"
          :color="item.status === 1 ? '#67C23A' : (item.status === 2 ? '#E6A23C' : '#F56C6C')">
          {{ idx === 0 ? '原始记录' : '复测第' + idx + '次' }}
          <el-tag :type="item.status === 1 ? 'success' : (item.status === 2 ? 'warning' : 'danger')" size="small" style="margin-left:8px">
            {{ item.status === 1 ? '正常' : (item.status === 2 ? '预警' : '超差') }}
          </el-tag>
          <div v-for="v in item.values" :key="v.indicatorName" style="font-size:12px;color:#666;margin-top:2px">
            {{ v.indicatorName }}: {{ v.value }} {{ v.indicatorUnit }}
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const indicatorList = ref([])
const chain = ref([])
const loading = ref(true)
const submitting = ref(false)

onMounted(async () => {
  const rid = route.params.id
  if (!rid) { loading.value = false; return }
  loading.value = true
  const [dRes, cRes] = await Promise.all([
    request.get('/retest/detail', { params: { recordId: rid } }),
    request.get('/retest/chain', { params: { recordId: rid } })
  ])
  if (dRes.code === 200) {
    data.value = dRes.data
    indicatorList.value = dRes.data.values.map(v => ({ ...v, newVal: null }))
  }
  if (cRes.code === 200) chain.value = cRes.data
  loading.value = false
})

const submitRetest = async () => {
  const vals = indicatorList.value.filter(v => v.newVal != null).map(v => ({
    indicatorId: v.indicatorId,
    value: v.newVal
  }))
  if (!vals.length) { ElMessage.warning('请至少填写一项复测值'); return }
  submitting.value = true
  try {
    const res = await request.post('/retest', {
      originalRecordId: data.value.recordId,
      values: vals
    })
    if (res.code === 200) {
      const d = res.data
      const lbl = { 1: '正常', 2: '预警', 3: '超差' }
      ElMessage.success('复测提交成功 - ' + lbl[d.status])
      // 刷新复测链
      const cRes2 = await request.get('/retest/chain', { params: { recordId: data.value.recordId } })
      if (cRes2.code === 200) chain.value = cRes2.data
      // 清空填写值，保持页面可继续复测
      indicatorList.value.forEach(v => v.newVal = null)
    }
  } finally { submitting.value = false }
}
</script>

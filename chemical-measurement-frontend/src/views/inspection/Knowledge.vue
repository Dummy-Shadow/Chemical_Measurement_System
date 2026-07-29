<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="knowledge-page">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>知识库</span>
          <div>
            <el-button v-if="userStore.isDeveloper || userStore.isAreaManager" type="primary" @click="openAddDialog">新增知识</el-button>
            <el-button v-if="userStore.isInspector" type="success" @click="suggestDialog=true">提交建议</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="知识条目" name="knowledge">
          <el-select v-model="filterLineId" placeholder="筛选产线" clearable @change="loadKnowledge" style="width:180px;margin-bottom:12px">
            <el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" />
          </el-select>

          <el-table :data="tableData" stripe v-loading="loading" @row-click="showDetail">
            <el-table-column prop="lineCode" label="产线" width="70" />
            <el-table-column prop="stationCode" label="工位" width="100" />
            <el-table-column prop="mediaCode" label="介质" width="140" />
            <el-table-column prop="title" label="问题" width="160" />
            <el-table-column prop="symptom" label="异常现象" min-width="180" show-overflow-tooltip />
            <el-table-column prop="solution" label="处理措施" min-width="220" show-overflow-tooltip />
            <el-table-column prop="sourceType" label="来源" width="80">
              <template #default="{ row }"><el-tag :type="row.sourceType==='DIRECT'?'':'warning'" size="small">{{row.sourceType==='DIRECT'?'直接':'建议'}}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="100" v-if="userStore.isDeveloper || userStore.isAreaManager">
              <template #default="{ row }">
                <el-button type="primary" link @click.stop="editKb(row)">编辑</el-button>
                <el-button v-if="userStore.isDeveloper || userStore.isAreaManager" type="danger" link @click.stop="deleteKb(row.kbId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" style="margin-top:15px;justify-content:flex-end" @current-change="loadKnowledge" />
        </el-tab-pane>

        <!-- 建议管理 Tab（管理者+审核者） -->
        <el-tab-pane label="建议管理" name="suggestions">
          <div v-if="userStore.isAreaManager || userStore.isDeveloper">
            <h4 style="margin-bottom:10px">待审批建议</h4>
            <div v-if="pendingList.length===0" style="color:#999">暂无</div>
            <div v-for="s in pendingList" :key="s.suggestionId" class="sug-item">
              <p><b>{{ s.suggestedByName }}</b> | {{ s.lineCode }} / {{ s.stationCode }} / {{ s.mediaCode }}</p>
              <p style="color:#666">异常: {{ s.symptom }}</p>
              <p style="color:#666">原因: {{ s.cause }}</p>
              <p style="color:#67C23A">建议: {{ s.proposedSolution }}</p>
              <div style="margin-top:8px">
                <el-button type="success" size="small" @click="openApproveDialog(s)">采纳</el-button>
                <el-popconfirm title="确定拒绝？" @confirm="rejectSug(s.suggestionId)">
                  <template #reference><el-button type="danger" size="small">拒绝</el-button></template>
                </el-popconfirm>
              </div>
            </div>
          </div>

          <!-- 审核者：我的建议 -->
          <div v-if="userStore.isInspector" style="margin-top:10px">
            <h4 style="margin-bottom:10px">我的建议</h4>
            <el-table :data="mySugList" size="small" stripe>
              <el-table-column prop="createTime" label="时间" width="150" />
              <el-table-column prop="lineCode" label="产线" width="70" />
              <el-table-column prop="symptom" label="异常" min-width="150" show-overflow-tooltip />
              <el-table-column label="状态" width="80">
                <template #default="{row}"><el-tag :type="row.status==='APPROVED'?'success':(row.status==='REJECTED'?'danger':'warning')" size="small">{{{PENDING:'待审',APPROVED:'已采纳',REJECTED:'已拒绝'}[row.status]}}</el-tag></template>
              </el-table-column>
              <el-table-column prop="reviewComment" label="回复" width="120" show-overflow-tooltip />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 新增/编辑知识弹窗 -->
      <el-dialog v-model="kbDialog" :title="editingKb?'编辑知识':'新增知识'" width="500px">
        <el-form label-width="80px" size="small">
          <el-form-item label="问题标题"><el-input v-model="kbForm.title" /></el-form-item>
          <el-form-item label="产线"><el-select v-model="kbForm.lineId" @change="onLineChange" style="width:100%"><el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" /></el-select></el-form-item>
          <el-form-item label="工位"><el-select v-model="kbForm.stationId" @change="onStationChange" style="width:100%"><el-option v-for="s in kbStations" :key="s.stationId" :label="s.stationCode" :value="s.stationId" /></el-select></el-form-item>
          <el-form-item label="介质"><el-select v-model="kbForm.mediaId" style="width:100%"><el-option v-for="m in kbMedia" :key="m.mediaId" :label="m.mediaCode" :value="m.mediaId" /></el-select></el-form-item>
          <el-form-item label="异常现象"><el-input v-model="kbForm.symptom" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="可能原因"><el-input v-model="kbForm.cause" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="处理措施"><el-input v-model="kbForm.solution" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="kbDialog=false">取消</el-button>
          <el-button type="primary" @click="saveKb">{{editingKb?'保存':'添加'}}</el-button>
        </template>
      </el-dialog>

      <!-- 采纳建议弹窗 -->
      <el-dialog v-model="approveDialog" title="采纳建议" width="450px">
        <el-form label-width="80px" size="small">
          <el-form-item label="标题"><el-input v-model="approveForm.title" /></el-form-item>
          <el-form-item label="处理措施"><el-input v-model="approveForm.solution" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="回复信息"><el-input v-model="approveForm.comment" type="textarea" :rows="2" placeholder="给审核者的回复（可与建议不同）" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="approveDialog=false">取消</el-button>
          <el-button type="primary" @click="doApprove">确认采纳</el-button>
        </template>
      </el-dialog>

      <!-- 提交建议弹窗 -->
      <el-dialog v-model="suggestDialog" title="提交知识建议" width="450px">
        <el-form label-width="80px" size="small">
          <el-form-item label="产线"><el-select v-model="sugForm.lineId" @change="onSugLineChange" style="width:100%"><el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" /></el-select></el-form-item>
          <el-form-item label="工位"><el-select v-model="sugForm.stationId" @change="onSugStationChange" style="width:100%"><el-option v-for="s in sugStations" :key="s.stationId" :label="s.stationCode" :value="s.stationId" /></el-select></el-form-item>
          <el-form-item label="介质"><el-select v-model="sugForm.mediaId" style="width:100%"><el-option v-for="m in sugMedia" :key="m.mediaId" :label="m.mediaCode" :value="m.mediaId" /></el-select></el-form-item>
          <el-form-item label="异常现象"><el-input v-model="sugForm.symptom" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="可能原因"><el-input v-model="sugForm.cause" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="建议措施"><el-input v-model="sugForm.proposedSolution" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="suggestDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!sugForm.symptom || !sugForm.proposedSolution" @click="submitSug">提交建议</el-button>
        </template>
      </el-dialog>

      <!-- 详情弹窗 -->
      <el-dialog v-model="detailDialog" title="知识详情" width="500px">
        <div v-if="detailRow">
          <p><b>产线/工位:</b> {{ detailRow.lineCode }} / {{ detailRow.stationCode }}</p>
          <p><b>介质:</b> {{ detailRow.mediaCode }}</p>
          <p><b>异常:</b> {{ detailRow.symptom }}</p>
          <p><b>原因:</b> {{ detailRow.cause || '-' }}</p>
          <p><b>处理措施:</b> {{ detailRow.solution }}</p>
          <p style="color:#999;font-size:12px">创建人: {{ detailRow.createdByName || '-' }} | 来源: {{ detailRow.sourceType==='DIRECT'?'直接添加':'审核者建议' }}</p>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { productionLineApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('knowledge')
const loading = ref(false)
const tableData = ref([])
const page = ref(1); const size = ref(10); const total = ref(0)
const filterLineId = ref(null)
const lines = ref([])

// 知识编辑
const kbDialog = ref(false)
const editingKb = ref(null)
const kbForm = reactive({ title:'',lineId:null,stationId:null,mediaId:null,symptom:'',cause:'',solution:'' })
const kbStations = ref([]); const kbMedia = ref([])

// 建议
const suggestDialog = ref(false)
const sugForm = reactive({ lineId:null,stationId:null,mediaId:null,symptom:'',cause:'',proposedSolution:'' })
const sugStations = ref([]); const sugMedia = ref([])
const pendingList = ref([]); const mySugList = ref([])

// 采纳
const approveDialog = ref(false)
const approveForm = reactive({suggestionId:null, title:'',solution:'',comment:''})

// 详情
const detailDialog = ref(false)
const detailRow = ref(null)

const loadLines = async () => { const r=await productionLineApi.list(); if(r.code===200) lines.value=r.data }

const loadKnowledge = async () => {
  loading.value = true
  const p = { page:page.value, size:size.value }; if(filterLineId.value) p.lineId=filterLineId.value
  const r = await request.get('/knowledge',{params:p})
  if(r.code===200){ tableData.value=r.data.records; total.value=r.data.total }
  loading.value = false
}

const loadSuggestions = async () => {
  if(userStore.isAreaManager || userStore.isDeveloper){ const r=await request.get('/knowledge/suggestions/pending'); if(r.code===200) pendingList.value=r.data }
  if(userStore.isInspector){ const r=await request.get('/knowledge/suggestions/my'); if(r.code===200) mySugList.value=r.data }
}

const openAddDialog = () => { editingKb.value=null; Object.assign(kbForm,{title:'',lineId:null,stationId:null,mediaId:null,symptom:'',cause:'',solution:''}); kbDialog.value=true }
const editKb = (row) => { editingKb.value=row; Object.assign(kbForm,{title:row.title,lineId:row.lineId,stationId:row.stationId,mediaId:row.mediaId,symptom:row.symptom,cause:row.cause||'',solution:row.solution}); kbDialog.value=true }
const showDetail = (row) => { detailRow.value=row; detailDialog.value=true }

const saveKb = async () => {
  const d = {...kbForm}
  if(editingKb.value){ await request.put('/knowledge/'+editingKb.value.kbId, d) }
  else { await request.post('/knowledge', d) }
  ElMessage.success(editingKb.value?'已保存':'已添加'); kbDialog.value=false; loadKnowledge()
}

const deleteKb = async (id) => {
  await ElMessageBox.confirm('确定删除？','警告',{type:'warning'})
  await request.delete('/knowledge/'+id); ElMessage.success('已删除'); loadKnowledge()
}

const onLineChange = async () => {
  kbStations.value=[]; kbMedia.value=[]; kbForm.stationId=null; kbForm.mediaId=null
  if(!kbForm.lineId) return
  const r=await request.get('/manual/workstations',{params:{lineId:kbForm.lineId}})
  if(r.code===200) kbStations.value=r.data
}
const onStationChange = async () => {
  kbMedia.value=[]; kbForm.mediaId=null
  if(!kbForm.stationId) return
  const r=await request.get('/manual/media-options',{params:{stationId:kbForm.stationId}})
  if(r.code===200) kbMedia.value=r.data
}

const onSugLineChange = async () => {
  sugStations.value=[]; sugMedia.value=[]; sugForm.stationId=null; sugForm.mediaId=null
  if(!sugForm.lineId) return
  const r=await request.get('/manual/workstations',{params:{lineId:sugForm.lineId}})
  if(r.code===200) sugStations.value=r.data
}
const onSugStationChange = async () => {
  sugMedia.value=[]; sugForm.mediaId=null
  if(!sugForm.stationId) return
  const r=await request.get('/manual/media-options',{params:{stationId:sugForm.stationId}})
  if(r.code===200) sugMedia.value=r.data
}

const submitSug = async () => {
  const d = {lineId:sugForm.lineId,stationId:sugForm.stationId,mediaId:sugForm.mediaId,symptom:sugForm.symptom,cause:sugForm.cause,proposedSolution:sugForm.proposedSolution}
  await request.post('/knowledge/suggestions', d)
  ElMessage.success('建议已提交'); suggestDialog.value=false; loadSuggestions()
  Object.assign(sugForm,{symptom:'',cause:'',proposedSolution:''})
}

const openApproveDialog = (s) => {
  approveForm.suggestionId = s.suggestionId
  approveForm.title = (s.lineCode||'')+' '+(s.symptom||'').slice(0,20)
  approveForm.solution = s.proposedSolution
  approveForm.comment = '已采纳，感谢建议'
  approveDialog.value = true
}

const doApprove = async () => {
  await request.put('/knowledge/suggestions/'+approveForm.suggestionId+'/approve',{title:approveForm.title,solution:approveForm.solution,reviewComment:approveForm.comment})
  ElMessage.success('已采纳并生成知识条目'); approveDialog.value=false; loadSuggestions(); loadKnowledge()
}

const rejectSug = async (id) => {
  await request.put('/knowledge/suggestions/'+id+'/reject',{reviewComment:'已拒绝，原因不符合实际情况'})
  ElMessage.success('已拒绝'); loadSuggestions()
}

onMounted(()=>{ loadLines(); loadKnowledge(); loadSuggestions() })
</script>

<style scoped>
.sug-item { padding:12px; margin-bottom:10px; border:1px solid #ebeef5; border-radius:4px; }
</style>

<!-- Copyright (c) 2026 郑杭宇. All rights reserved. Licensed under MIT. -->
<template>
  <div class="admin-page">
    <el-card>
      <template #header><span>系统管理</span></template>

      <el-tabs v-model="activeTab">
        <!-- 产线管理 -->
        <el-tab-pane label="产线管理" name="lines">
          <div style="margin-bottom:10px">
            <el-button type="primary" size="small" @click="addLine">新增产线</el-button>
          </div>
          <el-table :data="lines" border stripe size="small">
            <el-table-column prop="lineCode" label="编码" width="100" />
            <el-table-column prop="lineName" label="名称" width="160" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editLine(row)">编辑</el-button>
                <el-button v-if="userStore.isDeveloper" type="danger" link size="small" @click="delLine(row.lineId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 工位管理 -->
        <el-tab-pane label="工位管理" name="stations">
          <div style="margin-bottom:10px">
            <el-select v-model="stationFilter" placeholder="筛选产线" clearable @change="loadStations" size="small" style="width:160px;margin-right:10px">
              <el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" />
            </el-select>
            <el-button type="primary" size="small" @click="addStation">新增工位</el-button>
          </div>
          <el-table :data="stations" border stripe size="small">
            <el-table-column prop="lineCode" label="产线" width="80" />
            <el-table-column prop="stationCode" label="编码" width="120" />
            <el-table-column prop="stationName" label="名称" width="160" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editStation(row)">编辑</el-button>
                <el-button v-if="userStore.isDeveloper" type="danger" link size="small" @click="delStation(row.stationId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 介质管理 -->
        <el-tab-pane label="介质管理" name="media">
          <div style="margin-bottom:10px">
            <el-button type="primary" size="small" @click="addMedia">新增介质</el-button>
          </div>
          <el-table :data="mediaList" border stripe size="small">
            <el-table-column prop="mediaCode" label="牌号编码" width="180" />
            <el-table-column prop="mediaName" label="牌号名称" width="200" />
            <el-table-column prop="categoryName" label="类别" width="130" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editMedia(row)">编辑</el-button>
                <el-popconfirm title="确定删除？" @confirm="delMedia(row)" v-if="userStore.isDeveloper">
                  <template #reference><el-button type="danger" link size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 工位介质关联 -->
        <el-tab-pane label="工位介质关联" name="wm">
          <div style="margin-bottom:10px">
            <el-select v-model="wmStationId" placeholder="选择工位" @change="loadWm" size="small" style="width:180px;margin-right:10px">
              <el-option v-for="s in allStations" :key="s.stationId" :label="s.lineCode + ' - ' + s.stationCode" :value="s.stationId" />
            </el-select>
            <el-button v-if="wmStationId" type="primary" size="small" @click="addWmDialog=true">关联介质</el-button>
          </div>
          <el-table :data="wmList" border stripe size="small" v-if="wmStationId" @row-click="showWmIndicators" highlight-current-row>
            <el-table-column prop="mediaCode" label="介质" width="180" />
            <el-table-column label="指标数" width="80" prop="indicatorCount" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click.stop="showWmIndicators(row)">查看/编辑指标</el-button>
                <el-button v-if="userStore.isDeveloper" type="danger" link size="small" @click.stop="delWm(row.wmId)">解除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 指标范围编辑 -->
          <div v-if="wmIndicators.length" style="margin-top:15px">
            <h4 style="margin-bottom:8px">指标范围编辑 - {{ wmIndicatorMediaCode }}</h4>
            <el-table :data="wmIndicators" border size="small" style="max-width:700px">
              <el-table-column prop="indicatorName" label="指标" width="100" />
              <el-table-column label="标准下限" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.standardMin" :precision="4" size="small" style="width:100px" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="标准上限" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.standardMax" :precision="4" size="small" style="width:100px" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="警戒下限" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.warnMin" :precision="4" size="small" style="width:100px" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="警戒上限" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.warnMax" :precision="4" size="small" style="width:100px" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="saveWmi(row)">保存</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <div style="margin-bottom:10px">
            <el-button type="primary" size="small" @click="openUserDialog()" v-if="userStore.isDeveloper || userStore.isAreaManager">新增用户</el-button>
          </div>
          <el-table :data="users" border stripe size="small">
            <el-table-column prop="userId" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="{'DEVELOPER':'danger','AREA_MANAGER':'warning','INSPECTOR':'info'}[row.role]" size="small">
                  {{ {'DEVELOPER':'开发者','AREA_MANAGER':'分区管理者','INSPECTOR':'审核者'}[row.role] || row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button v-if="canEditUser(row)" type="primary" link size="small" @click="openUserDialog(row)">编辑</el-button>
                <el-button v-if="userStore.isDeveloper" type="danger" link size="small" @click="delUser(row.userId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="logs">
          <el-form :inline="true" size="small" style="margin-bottom:10px">
            <el-form-item label="日期"><el-date-picker v-model="logDate" type="date" value-format="YYYY-MM-DD" @change="loadLogs" /></el-form-item>
            <el-form-item label="操作"><el-select v-model="logAction" placeholder="全部" clearable @change="loadLogs" style="width:120px">
              <el-option label="录入" value="INSERT" /><el-option label="更新" value="UPDATE" /><el-option label="删除" value="DELETE" />
            </el-select></el-form-item>
            <el-form-item><el-button @click="logDate=null;logAction=null;loadLogs()">重置</el-button></el-form-item>
          </el-form>
          <el-table :data="logList" border stripe size="small" v-loading="logLoading">
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column prop="action" label="操作" width="80">
              <template #default="{row}">
                <el-tag :type="{INSERT:'success',UPDATE:'warning',DELETE:'danger'}[row.action]" size="small">{{row.action}}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetType" label="目标" width="110" />
            <el-table-column prop="targetId" label="ID" width="70" />
            <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
          </el-table>
          <el-pagination v-model:current-page="logPage" v-model:page-size="logSize" :total="logTotal" layout="total, prev, pager, next" @current-change="loadLogs" style="margin-top:10px;justify-content:flex-end" size="small" />
        </el-tab-pane>
      </el-tabs>

      <!-- 用户编辑弹窗 -->
      <el-dialog v-model="userDialog" :title="editingUser?'编辑用户':'新增用户'" width="400px">
        <el-form label-width="80px" size="small">
          <el-form-item label="用户名"><el-input v-model="userForm.username" :disabled="!!editingUser" /></el-form-item>
          <el-form-item :label="editingUser?'新密码':'密码'">
            <el-input v-model="userForm.password" type="password" :placeholder="editingUser?'留空不修改':'必填'" />
          </el-form-item>
          <el-form-item label="姓名"><el-input v-model="userForm.realName" /></el-form-item>
          <el-form-item label="角色" v-if="userStore.isDeveloper || !editingUser">
            <el-select v-model="userForm.role" style="width:100%" :disabled="!userStore.isDeveloper">
              <el-option label="开发者" value="DEVELOPER" />
              <el-option label="分区管理者" value="AREA_MANAGER" />
              <el-option label="审核者" value="INSPECTOR" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="userDialog=false">取消</el-button>
          <el-button type="primary" @click="saveUser" :disabled="!editingUser&&!userForm.password">保存</el-button>
        </template>
      </el-dialog>

      <!-- 新增/编辑产线弹窗 -->
      <el-dialog v-model="lineDialog" :title="editLineRow?'编辑产线':'新增产线'" width="400px">
        <el-form label-width="80px" size="small">
          <el-form-item label="编码"><el-input v-model="lineForm.lineCode" /></el-form-item>
          <el-form-item label="名称"><el-input v-model="lineForm.lineName" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="lineDialog=false">取消</el-button>
          <el-button type="primary" @click="saveLine">保存</el-button>
        </template>
      </el-dialog>

      <!-- 新增/编辑工位弹窗 -->
      <el-dialog v-model="stationDialog" :title="editStationRow?'编辑工位':'新增工位'" width="400px">
        <el-form label-width="80px" size="small">
          <el-form-item label="所属产线"><el-select v-model="stationForm.lineId" style="width:100%"><el-option v-for="l in lines" :key="l.lineId" :label="l.lineName" :value="l.lineId" /></el-select></el-form-item>
          <el-form-item label="编码"><el-input v-model="stationForm.stationCode" /></el-form-item>
          <el-form-item label="名称"><el-input v-model="stationForm.stationName" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="stationDialog=false">取消</el-button>
          <el-button type="primary" @click="saveStation">保存</el-button>
        </template>
      </el-dialog>

      <!-- 新增/编辑介质弹窗 -->
      <el-dialog v-model="mediaDialog" :title="editMediaRow?'编辑介质':'新增介质'" width="450px">
        <el-form label-width="80px" size="small">
          <el-form-item label="类别"><el-select v-model="mediaForm.categoryId" style="width:100%"><el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" /></el-select></el-form-item>
          <el-form-item label="牌号编码"><el-input v-model="mediaForm.mediaCode" /></el-form-item>
          <el-form-item label="牌号名称"><el-input v-model="mediaForm.mediaName" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="mediaDialog=false">取消</el-button>
          <el-button type="primary" @click="saveMedia">保存</el-button>
        </template>
      </el-dialog>

      <!-- 介质处理确认弹窗 -->
      <el-dialog v-model="mediaActionDialog" title="介质关联处理" width="450px">
        <p v-if="editMediaRow">修改"{{ editMediaRow.mediaCode }}"将影响 {{ mediaRelationCount }} 条知识库记录和工位关联。</p>
        <p v-else>删除"{{ delMediaRow && delMediaRow.mediaCode }}"将影响关联数据。</p>
        <el-radio-group v-model="mediaAction" style="display:flex;flex-direction:column;gap:10px">
          <el-radio value="">仅更新信息，保留关联数据</el-radio>
          <el-radio value="REPLACE">替换为新介质：<el-select v-model="replaceMediaId" @click.stop size="small" style="width:180px;margin-left:8px"><el-option v-for="m in mediaList.filter(x=>x.mediaId!==editMediaRow?.mediaId)" :key="m.mediaId" :label="m.mediaCode" :value="m.mediaId" /></el-select></el-radio>
          <el-radio value="DELETE_KNOWLEDGE">一并删除关联的知识库条目</el-radio>
        </el-radio-group>
        <template #footer>
          <el-button @click="mediaActionDialog=false">取消</el-button>
          <el-button type="primary" @click="confirmMediaAction">确认</el-button>
        </template>
      </el-dialog>

      <!-- 关联介质弹窗 -->
      <el-dialog v-model="addWmDialog" title="关联介质" width="350px">
        <el-select v-model="wmMediaId" placeholder="选择介质" style="width:100%">
          <el-option v-for="m in mediaList" :key="m.mediaId" :label="m.mediaCode" :value="m.mediaId" />
        </el-select>
        <template #footer>
          <el-button @click="addWmDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!wmMediaId" @click="saveWm">关联</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('lines')

const lines = ref([])
const stations = ref([])
const allStations = ref([])
const stationFilter = ref(null)
const mediaList = ref([])
const categories = ref([])
const wmStationId = ref(null)
const wmList = ref([])
const wmMediaId = ref(null)
const addWmDialog = ref(false)

const lineDialog = ref(false); const lineForm = reactive({}); const editLineRow = ref(null)
const stationDialog = ref(false); const stationForm = reactive({}); const editStationRow = ref(null)
const mediaDialog = ref(false); const mediaForm = reactive({}); const editMediaRow = ref(null)
const delMediaRow = ref(null)
const mediaActionDialog = ref(false); const mediaAction = ref(''); const replaceMediaId = ref(null); const mediaRelationCount = ref(0)
const wmIndicators = ref([])
const wmIndicatorMediaCode = ref('')
const logList = ref([]); const logPage = ref(1); const logSize = ref(20); const logTotal = ref(0)
const logDate = ref(null); const logAction = ref(null); const logLoading = ref(false)
const users = ref([])
const userDialog = ref(false)
const editingUser = ref(null)
const userForm = reactive({ username: '', password: '', realName: '', role: 'INSPECTOR' })

const canEditUser = (row) => {
  if (userStore.isDeveloper) return true  // 开发者可编辑所有人
  if (userStore.isAreaManager) return row.userId == userStore.userId // 管理者只能改自己
  if (userStore.isInspector) return row.userId == userStore.userId  // 审核者只能改自己
  return false
}

const loadLines = async () => { const r=await request.get('/admin/production-lines'); if(r.code===200) lines.value=r.data }
const loadStations = async () => { const p=stationFilter.value?{lineId:stationFilter.value}:{}; const r=await request.get('/admin/workstations',{params:p}); if(r.code===200) stations.value=r.data }
const loadAllStations = async () => { const r=await request.get('/admin/workstations'); if(r.code===200) allStations.value=r.data }
const loadMedia = async () => { const r=await request.get('/admin/media'); if(r.code===200) mediaList.value=r.data }
const loadCategories = async () => { const r=await request.get('/admin/media-categories'); if(r.code===200) categories.value=r.data }

const addLine = () => { editLineRow.value=null; lineForm.lineCode=''; lineForm.lineName=''; lineDialog.value=true }
const editLine = (row) => { editLineRow.value=row; lineForm.lineCode=row.lineCode; lineForm.lineName=row.lineName; lineDialog.value=true }
const saveLine = async () => {
  if(editLineRow.value){ await request.put('/admin/production-lines/'+editLineRow.value.lineId, lineForm) }
  else { await request.post('/admin/production-lines', lineForm) }
  ElMessage.success('已保存'); lineDialog.value=false; loadLines()
}
const delLine = async (id) => { await ElMessageBox.confirm('确定删除？将同时删除该产线下所有工位及指标配置。', '警告', { type: 'warning' }); await request.delete('/admin/production-lines/'+id); ElMessage.success('已删除'); loadLines(); loadStations(); loadAllStations() }

const addStation = () => { editStationRow.value=null; stationForm.lineId=null; stationForm.stationCode=''; stationForm.stationName=''; stationDialog.value=true }
const editStation = (row) => { editStationRow.value=row; stationForm.lineId=row.lineId; stationForm.stationCode=row.stationCode; stationForm.stationName=row.stationName; stationDialog.value=true }
const saveStation = async () => {
  if(editStationRow.value){ await request.put('/admin/workstations/'+editStationRow.value.stationId, stationForm) }
  else { await request.post('/admin/workstations', stationForm) }
  ElMessage.success('已保存'); stationDialog.value=false; loadStations(); loadAllStations()
}
const delStation = async (id) => { await ElMessageBox.confirm('确定删除？将同时删除该工位的介质关联及指标配置。', '警告', { type: 'warning' }); await request.delete('/admin/workstations/'+id); ElMessage.success('已删除'); loadStations(); loadAllStations() }

const addMedia = () => { editMediaRow.value=null; mediaForm.categoryId=null; mediaForm.mediaCode=''; mediaForm.mediaName=''; mediaDialog.value=true }
const editMedia = async (row) => {
  editMediaRow.value=row; mediaForm.categoryId=row.categoryId; mediaForm.mediaCode=row.mediaCode; mediaForm.mediaName=row.mediaName
  // 查关联数
  const r=await request.get('/admin/media/'+row.mediaId+'/relations')
  mediaRelationCount.value = r.code===200 ? (r.data.kbCount||0) : 0
  if(mediaRelationCount.value > 0){ mediaActionDialog.value=true; return }
  mediaDialog.value=true
}
const saveMedia = async () => {
  if(editMediaRow.value){ await request.put('/admin/media/'+editMediaRow.value.mediaId, mediaForm) }
  else { await request.post('/admin/media', mediaForm) }
  ElMessage.success('已保存'); mediaDialog.value=false; loadMedia()
}

const delMedia = async (row) => {
  const r=await request.get('/admin/media/'+row.mediaId+'/relations')
  const cnt = r.code===200 ? (r.data.kbCount||0) : 0
  if(cnt > 0){
    delMediaRow.value=row; mediaAction.value='DELETE_KNOWLEDGE'; mediaActionDialog.value=true; return
  }
  await ElMessageBox.confirm('确定删除？')
  await request.delete('/admin/media/'+row.mediaId); ElMessage.success('已删除'); loadMedia()
}

const confirmMediaAction = async () => {
  if(delMediaRow.value){
    await request.delete('/admin/media/'+delMediaRow.value.mediaId)
    ElMessage.success('已删除'); delMediaRow.value=null
  } else if(editMediaRow.value){
    if(mediaAction.value){
      await request.put('/admin/media/'+editMediaRow.value.mediaId, {action:mediaAction.value, newMediaId:replaceMediaId.value})
    } else {
      await request.put('/admin/media/'+editMediaRow.value.mediaId, mediaForm)
    }
    ElMessage.success('已处理')
  }
  mediaActionDialog.value=false; mediaDialog.value=false; loadMedia()
}

const loadWm = async () => {
  if(!wmStationId.value){ wmList.value=[]; return }
  const r=await request.get('/admin/workstation-media',{params:{stationId:wmStationId.value}})
  if(r.code===200) wmList.value=r.data
}
const saveWm = async () => {
  await request.post('/admin/workstation-media',{stationId:wmStationId.value, mediaId:wmMediaId.value})
  ElMessage.success('已关联'); addWmDialog.value=false; loadWm()
}
const showWmIndicators = async (row) => {
  const r = await request.get('/admin/workstation-media/' + row.wmId + '/indicators')
  if (r.code === 200) { wmIndicators.value = r.data; wmIndicatorMediaCode.value = row.mediaCode }
}
const saveWmi = async (row) => {
  await request.put('/admin/workstation-media-indicator/' + row.wmiId, {
    standardMin: row.standardMin, standardMax: row.standardMax,
    warnMin: row.warnMin, warnMax: row.warnMax
  })
  ElMessage.success('已保存')
}
const delWm = async (wmId) => {
  await ElMessageBox.confirm('确定解除？'); await request.delete('/admin/workstation-media/'+wmId); ElMessage.success('已解除'); loadWm(); wmIndicators.value=[]
}

const loadUsers = async () => { const r = await request.get('/admin/users'); if(r.code===200) users.value = r.data }
const openUserDialog = (row) => {
  editingUser.value = row || null
  if (row) {
    userForm.username = row.username; userForm.password = ''; userForm.realName = row.realName; userForm.role = row.role
  } else {
    userForm.username = ''; userForm.password = ''; userForm.realName = ''; userForm.role = 'INSPECTOR'
  }
  userDialog.value = true
}
const saveUser = async () => {
  const d = { realName: userForm.realName, role: userForm.role }
  if (userForm.password) d.password = userForm.password
  if (editingUser.value) {
    await request.put('/admin/users/' + editingUser.value.userId, d)
  } else {
    d.username = userForm.username
    await request.post('/admin/users', d)
  }
  ElMessage.success('已保存'); userDialog.value = false; loadUsers()
}
const delUser = async (id) => {
  await ElMessageBox.confirm('确定删除该用户？其排班将被取消，检测记录创建人将被清空。', '警告', { type: 'warning' })
  await request.delete('/admin/users/' + id); ElMessage.success('已删除'); loadUsers()
}

const loadLogs = async () => {
  logLoading.value = true
  const p = { page: logPage.value, size: logSize.value }
  if (logDate.value) { p.dateFrom = logDate.value; p.dateTo = logDate.value }
  if (logAction.value) p.action = logAction.value
  const r = await request.get('/admin/logs', { params: p })
  if (r.code === 200) { logList.value = r.data.records; logTotal.value = r.data.total }
  logLoading.value = false
}

onMounted(()=>{ loadLines(); loadStations(); loadAllStations(); loadMedia(); loadCategories(); loadUsers(); loadLogs() })
</script>

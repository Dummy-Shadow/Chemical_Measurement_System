<template>
  <div class="upload-page">
    <el-card>
      <template #header><span>拍照上传检测数据</span></template>
      <el-steps :active="step" finish-status="success" align-center style="margin-bottom:25px">
        <el-step title="拍照上传" />
        <el-step title="OCR识别" />
        <el-step title="确认勘正" />
        <el-step title="提交完成" />
      </el-steps>

      <!-- Step 0: 上传 -->
      <div v-if="step === 0" class="upload-area">
        <el-upload
          class="uploader"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept="image/jpeg,image/png,image/bmp"
          :file-list="fileList"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将照片拖到此处或<em>点击上传</em></div>
          <template #tip><div class="el-upload__tip">支持 jpg/png/bmp，单张≤5MB</div></template>
        </el-upload>
        <div v-if="previewUrl" class="preview">
          <img :src="previewUrl" alt="预览" />
        </div>
        <div style="text-align:center; margin-top:20px">
          <el-button type="primary" :disabled="!file" @click="startOcr" :loading="ocrLoading">
            开始识别
          </el-button>
        </div>
      </div>

      <!-- Step 1: OCR结果 -->
      <div v-else-if="step === 1">
        <el-alert title="OCR识别完成" type="success" :closable="false" style="margin-bottom:15px">
          <template #default>
            整体置信度: {{ (ocrResult.confidence * 100).toFixed(0) }}%
          </template>
        </el-alert>

        <el-card shadow="hover" size="small" style="margin-bottom:15px">
          <template #header><span>识别字段</span></template>
          <el-table :data="ocrResult.fields" border size="small" stripe>
            <el-table-column prop="fieldName" label="字段" width="130" />
            <el-table-column prop="value" label="识别值" width="130">
              <template #default="{ row }">{{ row.value }} {{ row.unit || '' }}</template>
            </el-table-column>
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">
                <el-progress :percentage="(row.confidence*100).toFixed(0)" :color="row.confidence > 0.8 ? '#67C23A' : (row.confidence > 0.6 ? '#E6A23C' : '#F56C6C')" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="手动修正" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.corrected" size="small" placeholder="修正值" style="width:130px" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <div style="text-align:center">
          <el-button @click="step=0">重新拍照</el-button>
          <el-button type="primary" @click="submitOcr">提交录入</el-button>
        </div>
      </div>

      <!-- Step 2: 完成 -->
      <div v-else>
        <el-result icon="success" title="录入成功" sub-title="数据已通过OCR识别并提交">
          <template #extra>
            <el-button type="primary" @click="$router.push('/inspection')">查看检测列表</el-button>
            <el-button @click="resetUpload">继续上传</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const step = ref(0)
const file = ref(null)
const previewUrl = ref('')
const fileList = ref([])
const ocrLoading = ref(false)
const ocrResult = ref({ confidence: 0, fields: [] })

const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
  fileList.value = [uploadFile]
}

const startOcr = async () => {
  ocrLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    const res = await request.post('/ocr/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200) {
      ocrResult.value = res.data
      ocrResult.value.fields.forEach(f => f.corrected = '')
      step.value = 1
    }
  } catch (e) {
    ElMessage.error('OCR识别失败')
  } finally { ocrLoading.value = false }
}

const submitOcr = async () => {
  const fields = ocrResult.value.fields.map(f => ({
    fieldName: f.fieldName,
    value: f.corrected || f.value
  }))
  // TODO: 将OCR结果转换为检测记录入库
  // 当前为演示流程，标记为需对接真实字段映射
  ElMessage.success('OCR数据已接收（需对接真实字段映射逻辑）')
  step.value = 2
}

const resetUpload = () => {
  step.value = 0; file.value = null; fileList.value = []; previewUrl.value = ''
}
</script>

<style scoped>
.upload-area { text-align: center; }
.preview { margin-top: 15px; }
.preview img { max-width: 400px; max-height: 300px; border: 1px solid #ddd; border-radius: 4px; }
</style>

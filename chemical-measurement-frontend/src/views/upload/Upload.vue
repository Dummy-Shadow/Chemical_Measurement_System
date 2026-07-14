<template>
  <div class="upload-page">
    <el-card>
      <template #header><span>检测数据上传</span></template>
      <el-steps :active="step" finish-status="success" align-center style="margin-bottom:30px">
        <el-step title="拍照上传" />
        <el-step title="OCR识别" />
        <el-step title="确认勘正" />
        <el-step title="提交完成" />
      </el-steps>

      <div v-if="step === 0" class="upload-area">
        <el-upload
          class="uploader"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept="image/jpeg,image/png,image/bmp"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将照片拖到此处或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 jpg/png/bmp 格式，单张图片不超过5MB</div>
          </template>
        </el-upload>
        <div v-if="previewUrl" class="preview">
          <img :src="previewUrl" alt="预览" />
        </div>
        <div style="text-align:center; margin-top:20px">
          <el-button type="primary" :disabled="!file" @click="step = 1">
            下一步：开始识别
          </el-button>
        </div>
      </div>

      <div v-else>
        <el-empty description="OCR识别功能开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const step = ref(0)
const file = ref(null)
const previewUrl = ref('')

const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
}
</script>

<style scoped>
.upload-area { text-align: center; }
.preview img { max-width: 400px; max-height: 300px; margin-top: 20px; border: 1px solid #ddd; }
</style>

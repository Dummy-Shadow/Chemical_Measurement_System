<template>
  <div class="profile-page">
    <el-card style="max-width:450px">
      <template #header><span>个人信息</span></template>
      <el-form label-width="80px" size="small">
        <el-form-item label="用户名">
          <el-input :value="userStore.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="{'DEVELOPER':'danger','AREA_MANAGER':'warning','INSPECTOR':'info'}[userStore.role]" size="small">
            {{ {'DEVELOPER':'开发者','AREA_MANAGER':'分区管理者','INSPECTOR':'审核者'}[userStore.role] }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" placeholder="留空不修改" />
        </el-form-item>
        <el-form-item label="旧密码" v-if="form.password">
          <el-input v-model="form.oldPassword" type="password" placeholder="修改密码需验证旧密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)
const form = reactive({ realName: userStore.realName || '', password: '', oldPassword: '' })

const save = async () => {
  saving.value = true
  try {
    const d = { realName: form.realName }
    if (form.password) { d.password = form.password; d.oldPassword = form.oldPassword }
    await request.put('/profile', d)
    userStore.realName = form.realName
    localStorage.setItem('realName', form.realName)
    ElMessage.success('已保存')
    form.password = ''; form.oldPassword = ''
  } finally { saving.value = false }
}
</script>

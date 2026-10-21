<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Avatar, Button, Form, FormItem, Input, InputPassword, message } from '@hmfw/ant-design'
import { UserOutlined } from '@hmfw/icons'
import { getProfile, updatePassword, updateProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

/* --------------------------- 基础资料 --------------------------- */
const username = ref('')
const profileRef = ref()
const profileSaving = ref(false)
const profileForm = reactive({ nickname: '', email: '', phone: '' })
const profileRules = {
  email: [{ type: 'email' as const, message: '邮箱格式不正确' }],
}

async function loadProfile() {
  const p = await getProfile()
  username.value = p.username
  profileForm.nickname = p.nickname ?? ''
  profileForm.email = p.email ?? ''
  profileForm.phone = p.phone ?? ''
}
onMounted(loadProfile)

async function saveProfile() {
  await profileRef.value?.validate()
  profileSaving.value = true
  try {
    await updateProfile({ ...profileForm })
    await auth.refreshUserInfo()
    message.success('资料已更新')
  } finally {
    profileSaving.value = false
  }
}

/* --------------------------- 修改密码 --------------------------- */
const pwdRef = ref()
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码至少 6 位' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (_: unknown, value: unknown) =>
        value === pwdForm.newPassword
          ? Promise.resolve()
          : Promise.reject(new Error('两次输入的密码不一致')),
    },
  ],
}

async function savePassword() {
  await pwdRef.value?.validate()
  pwdSaving.value = true
  try {
    await updatePassword(pwdForm.oldPassword, pwdForm.newPassword)
    message.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdRef.value?.clearValidate?.()
  } finally {
    pwdSaving.value = false
  }
}
</script>

<template>
  <div class="profile">
    <div class="card">
      <div class="card-head">
        <Avatar :size="56" :icon="UserOutlined" style="background-color: #1677ff" />
        <div class="card-head-text">
          <div class="name">{{ profileForm.nickname || username }}</div>
          <div class="account">账号：{{ username }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">基础资料</div>
      <Form ref="profileRef" :model="profileForm" :rules="profileRules" layout="vertical">
        <FormItem label="昵称" name="nickname">
          <Input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
        </FormItem>
        <FormItem label="邮箱" name="email">
          <Input v-model:value="profileForm.email" placeholder="请输入邮箱" />
        </FormItem>
        <FormItem label="手机号" name="phone">
          <Input v-model:value="profileForm.phone" placeholder="请输入手机号" />
        </FormItem>
        <FormItem>
          <Button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</Button>
        </FormItem>
      </Form>
    </div>

    <div class="card">
      <div class="card-title">修改密码</div>
      <Form ref="pwdRef" :model="pwdForm" :rules="pwdRules" layout="vertical">
        <FormItem label="原密码" name="oldPassword">
          <InputPassword v-model:value="pwdForm.oldPassword" placeholder="请输入原密码" />
        </FormItem>
        <FormItem label="新密码" name="newPassword">
          <InputPassword v-model:value="pwdForm.newPassword" placeholder="请输入新密码" />
        </FormItem>
        <FormItem label="确认新密码" name="confirmPassword">
          <InputPassword v-model:value="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
        </FormItem>
        <FormItem>
          <Button type="primary" :loading="pwdSaving" @click="savePassword">修改密码</Button>
        </FormItem>
      </Form>
    </div>
  </div>
</template>

<style scoped>
.profile {
  max-width: 560px;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-head-text .name {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.card-head-text .account {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  color: rgba(0, 0, 0, 0.88);
}
</style>

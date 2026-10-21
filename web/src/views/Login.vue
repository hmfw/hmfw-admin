<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button, Form, FormItem, Input, InputPassword, message } from '@hmfw/ant-design'
import { LockOutlined, SafetyOutlined, UserOutlined } from '@hmfw/icons'
import { getCaptcha, login } from '@/api/auth'
import { setTokenWithExpiry } from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formState = reactive({
  username: 'admin',
  password: 'admin123',
  captchaCode: '',
})

/* ------------------------------ 验证码 ------------------------------ */
const captchaEnabled = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')

async function refreshCaptcha() {
  const res = await getCaptcha()
  captchaEnabled.value = res.enabled
  captchaKey.value = res.captchaKey ?? ''
  captchaImage.value = res.captchaImage ?? ''
  formState.captchaCode = ''
}
onMounted(refreshCaptcha)

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  captchaCode: [{ required: true, message: '请输入验证码' }],
}

const loading = ref(false)

async function handleFinish(values: typeof formState) {
  loading.value = true
  try {
    const res = await login({
      username: values.username,
      password: values.password,
      captchaKey: captchaEnabled.value ? captchaKey.value : undefined,
      captchaCode: captchaEnabled.value ? values.captchaCode : undefined,
    })
    setTokenWithExpiry(res.token, res.expiresIn)
    await auth.loadUserInfo()
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    // 错误提示已由请求层统一弹出；验证码可能已失效，刷新一张
    if (captchaEnabled.value) refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>后台管理系统</h1>
        <p>基于 Spring Boot + RBAC 的权限管理平台</p>
      </div>
      <Form :model="formState" :rules="rules" layout="vertical" @finish="handleFinish">
        <FormItem label="用户名" name="username">
          <Input
            v-model:value="formState.username"
            size="large"
            placeholder="请输入用户名"
            :prefix="UserOutlined"
          />
        </FormItem>
        <FormItem label="密码" name="password">
          <InputPassword
            v-model:value="formState.password"
            size="large"
            placeholder="请输入密码"
            :prefix="LockOutlined"
          />
        </FormItem>
        <FormItem v-if="captchaEnabled" label="验证码" name="captchaCode">
          <div class="captcha-row">
            <Input
              v-model:value="formState.captchaCode"
              size="large"
              placeholder="请输入验证码"
              :prefix="SafetyOutlined"
              @press-enter="handleFinish(formState)"
            />
            <img
              class="captcha-img"
              :src="captchaImage"
              alt="验证码"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </FormItem>
        <FormItem>
          <Button type="primary" html-type="submit" size="large" block :loading="loading">
            登录
          </Button>
        </FormItem>
      </Form>
      <div class="login-tip">
        内置账号：<b>admin / admin123</b>（超管） · <b>viewer / admin123</b>（只读）
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1677ff 0%, #0b4da2 100%);
}

.login-card {
  width: 400px;
  max-width: calc(100vw - 32px);
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: rgba(0, 0, 0, 0.88);
}

.login-header p {
  margin: 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.login-tip {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.captcha-row {
  display: flex;
  gap: 8px;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  flex: 0 0 auto;
}
</style>

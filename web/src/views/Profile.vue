<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import {
  Alert,
  Button,
  Form,
  FormItem,
  Input,
  InputPassword,
  Modal,
  Skeleton,
  Tabs,
  message,
} from '@hmfw/ant-design'
import { DeleteOutlined, UploadOutlined } from '@hmfw/icons'
import {
  getProfile,
  removeAvatar,
  updatePassword,
  updateProfile,
  uploadAvatar,
} from '@/api/auth'
import AvatarCropperModal from '@/components/AvatarCropperModal.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { getAvatarPresentation } from '@/utils/avatar'

const auth = useAuthStore()

/* --------------------------- 页面状态 --------------------------- */
const profileLoading = ref(true)
const profileLoadError = ref('')
const profileLoaded = ref(false)
const activeTab = ref('profile')
const tabItems = [
  { key: 'profile', label: '基本资料' },
  { key: 'security', label: '账号安全' },
]

/* --------------------------- 基础资料 --------------------------- */
const username = ref('')
const identityNickname = ref('')
const avatarUrl = ref<string>()
const avatarFileInput = ref<HTMLInputElement>()
const avatarFile = ref<File | null>(null)
const cropperOpen = ref(false)
const avatarUploading = ref(false)
const avatarRemoving = ref(false)
const profileRef = ref()
const profileSaving = ref(false)
const profileForm = reactive({ nickname: '', email: '', phone: '' })
const profileBaseline = ref({ nickname: '', email: '', phone: '' })
const profileRules = {
  nickname: [{ max: 50, message: '昵称不能超过 50 个字符' }],
  email: [
    { type: 'email' as const, message: '邮箱格式不正确' },
    { max: 100, message: '邮箱不能超过 100 个字符' },
  ],
  phone: [{ max: 20, message: '手机号不能超过 20 个字符' }],
}
const isProfileDirty = computed(() =>
  (['nickname', 'email', 'phone'] as const).some(
    (field) => profileForm[field] !== profileBaseline.value[field],
  ),
)
const avatar = computed(() =>
  getAvatarPresentation({
    nickname: identityNickname.value,
    username: username.value,
    avatarUrl: avatarUrl.value,
  }),
)

function normalizedProfile(profile: {
  nickname?: string | null
  email?: string | null
  phone?: string | null
}) {
  return {
    nickname: profile.nickname ?? '',
    email: profile.email ?? '',
    phone: profile.phone ?? '',
  }
}

function applyProfileValues(values: ReturnType<typeof normalizedProfile>) {
  Object.assign(profileForm, values)
  profileBaseline.value = { ...values }
}

function restoreProfileBaseline() {
  Object.assign(profileForm, profileBaseline.value)
  profileRef.value?.clearValidate?.()
}

let pendingDiscardConfirmation: Promise<boolean> | null = null

function confirmDiscardChanges(): Promise<boolean> {
  if (!isProfileDirty.value) return Promise.resolve(true)
  if (pendingDiscardConfirmation) return pendingDiscardConfirmation

  const confirmation = new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '放弃未保存的修改？',
      content: '当前基本资料尚未保存，离开后修改将丢失。',
      okText: '放弃修改',
      cancelText: '继续编辑',
      okType: 'danger',
      closable: false,
      maskClosable: false,
      keyboard: false,
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
  pendingDiscardConfirmation = confirmation
  void confirmation.finally(() => {
    if (pendingDiscardConfirmation === confirmation) pendingDiscardConfirmation = null
  })
  return confirmation
}

async function handleTabChange(key: string) {
  if (key === activeTab.value || profileSaving.value) return
  if (activeTab.value === 'profile' && !(await confirmDiscardChanges())) return
  if (activeTab.value === 'profile') restoreProfileBaseline()
  activeTab.value = key
}

onBeforeRouteLeave(async (to) => {
  // 注销流程已先清除认证状态，登录页跳转不能再被本页的编辑状态拦截。
  if (to.path === '/login') return true
  if (profileSaving.value) return false
  if (!(await confirmDiscardChanges())) return false
  return true
})

async function loadProfile() {
  profileLoading.value = true
  profileLoadError.value = ''
  try {
    const profile = await getProfile()
    username.value = profile.username
    identityNickname.value = profile.nickname ?? ''
    avatarUrl.value = profile.avatarUrl ?? undefined
    applyProfileValues(normalizedProfile(profile))
    profileLoaded.value = true
  } catch {
    profileLoaded.value = false
    profileLoadError.value = '个人资料加载失败，请稍后重试'
  } finally {
    profileLoading.value = false
  }
}
onMounted(loadProfile)

const MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024
const AVATAR_MIME_TYPES = new Set(['image/jpeg', 'image/png'])
const AVATAR_FILE_EXTENSIONS = ['.jpg', '.jpeg', '.png']

function selectAvatar() {
  if (!avatarUploading.value && !avatarRemoving.value) avatarFileInput.value?.click()
}

function handleAvatarFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const filename = file.name.toLocaleLowerCase()
  const hasSupportedExtension = AVATAR_FILE_EXTENSIONS.some((extension) =>
    filename.endsWith(extension),
  )
  if (!AVATAR_MIME_TYPES.has(file.type) || !hasSupportedExtension) {
    message.error('仅支持 JPEG 或 PNG 图片')
    return
  }
  if (file.size > MAX_AVATAR_FILE_SIZE) {
    message.error('头像图片不能超过 5 MiB')
    return
  }
  avatarFile.value = file
  cropperOpen.value = true
}

function closeCropper() {
  if (avatarUploading.value) return
  cropperOpen.value = false
  avatarFile.value = null
}

async function refreshAvatarInNavigation(failureMessage: string) {
  try {
    await auth.refreshUserInfo()
  } catch {
    message.error(failureMessage)
  }
}

async function saveCroppedAvatar(blob: Blob) {
  if (avatarUploading.value) return
  avatarUploading.value = true
  try {
    const result = await uploadAvatar(blob)
    avatarUrl.value = result.avatarUrl
    cropperOpen.value = false
    avatarFile.value = null
    message.success('头像已更新')
    await refreshAvatarInNavigation('头像已更新，但顶部用户信息刷新失败')
  } catch {
    // 请求层已展示错误；保留裁剪状态供用户重试。
  } finally {
    avatarUploading.value = false
  }
}

function confirmRemoveAvatar() {
  if (avatarRemoving.value || !avatarUrl.value) return
  Modal.confirm({
    title: '移除自定义头像？',
    content: '移除后将恢复显示昵称或账号首字符。',
    okText: '移除',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      avatarRemoving.value = true
      try {
        await removeAvatar()
        avatarUrl.value = undefined
        message.success('头像已移除')
        await refreshAvatarInNavigation('头像已移除，但顶部用户信息刷新失败')
      } catch {
        // 请求层已展示错误；保留当前头像。
      } finally {
        avatarRemoving.value = false
      }
    },
  })
}

async function saveProfile() {
  if (!isProfileDirty.value || profileSaving.value) return
  try {
    await profileRef.value?.validate()
  } catch {
    return
  }
  profileSaving.value = true
  try {
    await updateProfile({ ...profileForm })
    profileBaseline.value = { ...profileForm }
    identityNickname.value = profileForm.nickname
    message.success('资料已更新')
    try {
      await auth.refreshUserInfo()
    } catch {
      message.error('资料已保存，但顶部用户信息刷新失败')
    }
  } catch {
    // 请求层已展示错误；保留当前输入供用户修正或重试。
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
  if (pwdSaving.value) return
  try {
    await pwdRef.value?.validate()
  } catch {
    return
  }
  pwdSaving.value = true
  try {
    await updatePassword(pwdForm.oldPassword, pwdForm.newPassword)
    message.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdRef.value?.clearValidate?.()
  } catch {
    // 请求层已展示错误；保留输入供用户重试。
  } finally {
    pwdSaving.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <header class="page-heading">
      <h1>个人中心</h1>
      <p>维护个人资料与账号安全信息</p>
    </header>

    <div v-if="profileLoading" data-testid="profile-loading" class="profile-loading">
      <Skeleton active avatar :paragraph="{ rows: 4 }" />
    </div>

    <div v-else-if="profileLoadError" data-testid="profile-error" class="profile-error">
      <Alert
        type="error"
        show-icon
        title="个人资料加载失败"
        description="暂时无法获取你的资料，请稍后重试。"
      />
      <Button data-testid="reload-profile" @click="loadProfile">重新加载</Button>
    </div>

    <div v-else-if="profileLoaded" data-testid="profile-content" class="profile-content">
      <aside class="identity-summary">
        <UserAvatar
          data-testid="profile-avatar"
          :size="72"
          :nickname="identityNickname"
          :username="username"
          :avatar-url="avatarUrl"
        />
        <div data-testid="profile-display-name" class="identity-name">{{ avatar.displayName }}</div>
        <div data-testid="profile-account" class="identity-account">账号：{{ username }}</div>
        <div class="avatar-actions">
          <input
            ref="avatarFileInput"
            data-testid="avatar-file-input"
            class="avatar-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,image/jpeg,image/png"
            @change="handleAvatarFileChange"
          />
          <Button
            data-testid="choose-avatar"
            size="small"
            :disabled="avatarUploading || avatarRemoving"
            @click="selectAvatar"
          >
            <UploadOutlined />
            {{ avatarUrl ? '更换头像' : '上传头像' }}
          </Button>
          <Button
            v-if="avatarUrl"
            data-testid="remove-avatar"
            size="small"
            danger
            :loading="avatarRemoving"
            :disabled="avatarUploading"
            @click="confirmRemoveAvatar"
          >
            <DeleteOutlined />
            移除头像
          </Button>
        </div>
      </aside>

      <section class="settings-workspace">
        <Tabs :active-key="activeTab" :items="tabItems" @change="handleTabChange">
          <template #profile>
            <Form
              ref="profileRef"
              :model="profileForm"
              :rules="profileRules"
              :disabled="profileSaving"
              layout="vertical"
            >
              <FormItem label="账号">
                <Input :value="username" disabled />
              </FormItem>
              <FormItem label="昵称" name="nickname">
                <Input
                  v-model:value="profileForm.nickname"
                  data-testid="profile-nickname"
                  :max-length="50"
                  placeholder="请输入昵称"
                />
              </FormItem>
              <FormItem label="邮箱" name="email">
                <Input
                  v-model:value="profileForm.email"
                  data-testid="profile-email"
                  :max-length="100"
                  placeholder="请输入邮箱"
                />
              </FormItem>
              <FormItem label="手机号" name="phone">
                <Input
                  v-model:value="profileForm.phone"
                  data-testid="profile-phone"
                  :max-length="20"
                  placeholder="请输入手机号"
                />
              </FormItem>
              <FormItem>
                <Button
                  type="primary"
                  :disabled="!isProfileDirty || profileSaving"
                  :loading="profileSaving"
                  @click="saveProfile"
                >
                  保存资料
                </Button>
              </FormItem>
            </Form>
          </template>

          <template #security>
            <Form
              ref="pwdRef"
              :model="pwdForm"
              :rules="pwdRules"
              :disabled="pwdSaving"
              layout="vertical"
            >
              <FormItem label="原密码" name="oldPassword">
                <InputPassword
                  v-model:value="pwdForm.oldPassword"
                  placeholder="请输入原密码"
                />
              </FormItem>
              <FormItem label="新密码" name="newPassword">
                <InputPassword
                  v-model:value="pwdForm.newPassword"
                  placeholder="请输入新密码"
                />
              </FormItem>
              <FormItem label="确认新密码" name="confirmPassword">
                <InputPassword
                  v-model:value="pwdForm.confirmPassword"
                  placeholder="请再次输入新密码"
                />
              </FormItem>
              <FormItem>
                <Button
                  type="primary"
                  :disabled="pwdSaving"
                  :loading="pwdSaving"
                  @click="savePassword"
                >
                  修改密码
                </Button>
              </FormItem>
            </Form>
          </template>
        </Tabs>
      </section>
    </div>

    <AvatarCropperModal
      :open="cropperOpen"
      :file="avatarFile"
      :busy="avatarUploading"
      @cancel="closeCropper"
      @confirm="saveCroppedAvatar"
    />
  </div>
</template>

<style scoped>
.profile-page {
  width: 100%;
}

.page-heading {
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0;
  color: rgba(0, 0, 0, 0.88);
  font-size: 22px;
  line-height: 1.4;
}

.page-heading p {
  margin: 6px 0 0;
  color: rgba(0, 0, 0, 0.45);
}

.profile-loading,
.profile-error {
  max-width: 760px;
  padding: 24px 0;
}

.profile-error > :last-child {
  margin-top: 16px;
}

.profile-content {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 32px;
}

.identity-summary {
  padding-right: 32px;
  border-right: 1px solid #f0f0f0;
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.avatar-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

.identity-name {
  margin-top: 16px;
  color: rgba(0, 0, 0, 0.88);
  font-size: 18px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.identity-account {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.settings-workspace {
  min-width: 0;
  max-width: 720px;
}

.settings-workspace :deep(.hmfw-tabs-content) {
  padding-top: 20px;
}

@media (max-width: 760px) {
  .page-heading {
    margin-bottom: 20px;
  }

  .profile-loading,
  .profile-error {
    padding: 12px 0;
  }

  .profile-content {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
  }

  .identity-summary {
    display: grid;
    grid-template-columns: 72px minmax(0, 1fr);
    column-gap: 16px;
    align-items: center;
    padding: 0 0 20px;
    border-right: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .identity-summary :deep(.hmfw-avatar) {
    grid-column: 1;
    grid-row: 1 / span 2;
  }

  .identity-name {
    grid-column: 2;
    grid-row: 1;
    align-self: end;
    margin-top: 0;
  }

  .identity-account {
    grid-column: 2;
    grid-row: 2;
    align-self: start;
  }

  .avatar-actions {
    grid-column: 1 / -1;
    grid-row: 3;
    margin-top: 4px;
  }

  .settings-workspace {
    width: 100%;
  }

  .settings-workspace :deep(.hmfw-btn-primary) {
    width: 100%;
    min-height: 40px;
  }
}
</style>

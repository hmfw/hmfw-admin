import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Modal } from '@hmfw/ant-design'
import type { Profile as ProfileData } from '@/types/api'

const getProfile = vi.fn<() => Promise<ProfileData>>()
const updateProfile = vi.fn()

vi.mock('@/api/auth', () => ({
  getProfile: () => getProfile(),
  updateProfile: (...args: unknown[]) => updateProfile(...args),
  updatePassword: vi.fn(),
  getMe: vi.fn(),
  getMenus: vi.fn(),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ refreshUserInfo: vi.fn() }),
}))

import Profile from './Profile.vue'

const normalProfile: ProfileData = {
  username: 'admin',
  nickname: '管理员',
  email: 'admin@example.com',
  phone: '13800138000',
}

const EmptyView = defineComponent({ template: '<div />' })
const RouterHost = defineComponent({ components: { RouterView }, template: '<RouterView />' })

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

async function mountProfileWithRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/profile', component: Profile },
      { path: '/messages', component: EmptyView },
      { path: '/dashboard', component: EmptyView },
      { path: '/login', component: EmptyView },
    ],
  })
  await router.push('/profile')
  await router.isReady()
  const wrapper = mount(RouterHost, { global: { plugins: [router] } })
  await flushPromises()
  return { router, wrapper }
}

describe('Profile 路由离开保护', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    getProfile.mockReset()
    updateProfile.mockReset()
    getProfile.mockResolvedValue(normalProfile)
  })

  it('脏表单不阻止登录页跳转', async () => {
    const confirm = vi.spyOn(Modal, 'confirm')
    const { router, wrapper } = await mountProfileWithRouter()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    await router.push('/login')

    expect(router.currentRoute.value.path).toBe('/login')
    expect(confirm).not.toHaveBeenCalled()
  })

  it('资料保存请求未完成时也不阻止登录页跳转', async () => {
    updateProfile.mockReturnValue(deferred<void>().promise)
    const { router, wrapper } = await mountProfileWithRouter()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('保存中昵称')
    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await vi.waitFor(() => expect(updateProfile).toHaveBeenCalledTimes(1))

    await router.push('/login')

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('取消重叠导航时只显示一个确认并保留当前输入', async () => {
    let cancelConfirmation: (() => void) | undefined
    const confirm = vi.spyOn(Modal, 'confirm').mockImplementation((options) => {
      cancelConfirmation = () => void options.onCancel?.()
      return { destroy: vi.fn(), update: vi.fn() }
    })
    const { router, wrapper } = await mountProfileWithRouter()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    const firstNavigation = router.push('/messages')
    await vi.waitFor(() => expect(confirm).toHaveBeenCalledTimes(1))
    const secondNavigation = router.push('/dashboard')
    await flushPromises()
    expect(confirm).toHaveBeenCalledTimes(1)

    cancelConfirmation?.()
    await Promise.all([firstNavigation, secondNavigation])

    expect(router.currentRoute.value.path).toBe('/profile')
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '未保存昵称',
    )
  })
})

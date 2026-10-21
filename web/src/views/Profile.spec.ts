import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Modal } from '@hmfw/ant-design'
import type { Profile as ProfileData } from '@/types/api'

const routerHooks = vi.hoisted(() => ({
  leaveGuard: undefined as undefined | ((to: { path: string }) => unknown),
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    onBeforeRouteLeave: (guard: (to: { path: string }) => unknown) => {
      routerHooks.leaveGuard = guard
    },
  }
})

const getProfile = vi.fn<() => Promise<ProfileData>>()
const updateProfile = vi.fn()
const updatePassword = vi.fn()
const refreshUserInfo = vi.fn()

vi.mock('@/api/auth', () => ({
  getProfile: () => getProfile(),
  updateProfile: (...args: unknown[]) => updateProfile(...args),
  updatePassword: (...args: unknown[]) => updatePassword(...args),
  getMe: vi.fn(),
  getMenus: vi.fn(),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ refreshUserInfo }),
}))

import Profile from './Profile.vue'

const normalProfile: ProfileData = {
  username: 'admin',
  nickname: '管理员',
  email: 'admin@example.com',
  phone: '13800138000',
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function answerNextConfirmation(confirmed: boolean) {
  return vi.spyOn(Modal, 'confirm').mockImplementation((options) => {
    if (confirmed) void options.onOk?.()
    else void options.onCancel?.()
    return { destroy: vi.fn(), update: vi.fn() }
  })
}

async function openSecurityTab(wrapper: ReturnType<typeof mount>) {
  await wrapper.findAll('[role="tab"]').find((tab) => tab.text() === '账号安全')!.trigger('click')
  await flushPromises()
}

describe('Profile', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    routerHooks.leaveGuard = undefined
    getProfile.mockReset()
    updateProfile.mockReset()
    updatePassword.mockReset()
    refreshUserInfo.mockReset()
  })

  it('资料请求未完成时只展示加载占位', () => {
    getProfile.mockReturnValue(deferred<ProfileData>().promise)

    const wrapper = mount(Profile)

    expect(wrapper.find('[data-testid="profile-loading"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="profile-content"]').exists()).toBe(false)
  })

  it('加载成功后展示身份摘要和资料表单', async () => {
    getProfile.mockResolvedValue(normalProfile)

    const wrapper = mount(Profile)
    await flushPromises()

    expect(wrapper.get('[data-testid="profile-display-name"]').text()).toBe('管理员')
    expect(wrapper.get('[data-testid="profile-account"]').text()).toContain('admin')
    expect(wrapper.get('[data-testid="profile-avatar"]').text()).toBe('管')
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '管理员',
    )
    expect(wrapper.get('[data-testid="profile-email"]').element).toHaveProperty(
      'value',
      'admin@example.com',
    )
    expect(wrapper.get('[data-testid="profile-phone"]').element).toHaveProperty(
      'value',
      '13800138000',
    )
  })

  it('昵称为空时身份摘要回退到账号', async () => {
    getProfile.mockResolvedValue({ ...normalProfile, nickname: '  ' })

    const wrapper = mount(Profile)
    await flushPromises()

    expect(wrapper.get('[data-testid="profile-display-name"]').text()).toBe('admin')
    expect(wrapper.get('[data-testid="profile-avatar"]').text()).toBe('A')
  })

  it('加载失败时展示错误状态且不展示空白表单', async () => {
    getProfile.mockRejectedValue(new Error('network unavailable'))

    const wrapper = mount(Profile)
    await flushPromises()

    expect(wrapper.find('[data-testid="profile-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="profile-content"]').exists()).toBe(false)
  })

  it('可从错误状态重新加载资料', async () => {
    getProfile
      .mockRejectedValueOnce(new Error('network unavailable'))
      .mockResolvedValueOnce(normalProfile)

    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="reload-profile"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="profile-display-name"]').text()).toBe('管理员')
    expect(getProfile).toHaveBeenCalledTimes(2)
  })

  it('资料未修改时禁用保存，修改后启用保存', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const wrapper = mount(Profile)
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!
    expect(saveButton.attributes('disabled')).toBeDefined()

    await wrapper.get('[data-testid="profile-nickname"]').setValue('新昵称')

    expect(saveButton.attributes('disabled')).toBeUndefined()
  })

  it.each([
    ['昵称', 'profile-nickname', '昵称不能超过 50 个字符', '昵'.repeat(51)],
    ['邮箱长度', 'profile-email', '邮箱不能超过 100 个字符', `${'a'.repeat(89)}@example.com`],
    ['邮箱格式', 'profile-email', '邮箱格式不正确', 'invalid-email'],
    ['手机号', 'profile-phone', '手机号不能超过 20 个字符', '1'.repeat(21)],
  ])('%s 无效时阻止保存并展示字段校验', async (_name, testId, message, value) => {
    getProfile.mockResolvedValue(normalProfile)
    const wrapper = mount(Profile)
    await flushPromises()

    await wrapper.get(`[data-testid="${testId}"]`).setValue(value)
    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain(message)
    expect(updateProfile).not.toHaveBeenCalled()
  })

  it('保存期间锁定资料输入与保存操作', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updateProfile.mockReturnValue(deferred<void>().promise)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('新昵称')

    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await vi.waitFor(() => expect(updateProfile).toHaveBeenCalledTimes(1))

    expect(wrapper.get('[data-testid="profile-nickname"]').attributes('disabled')).toBeDefined()
    expect(
      wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.attributes('disabled'),
    ).toBeDefined()
  })

  it('保存成功后更新身份摘要和资料基线并刷新全局用户信息', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updateProfile.mockResolvedValue(undefined)
    refreshUserInfo.mockResolvedValue(undefined)
    const wrapper = mount(Profile)
    await flushPromises()

    await wrapper.get('[data-testid="profile-nickname"]').setValue('新昵称')
    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await flushPromises()

    expect(updateProfile).toHaveBeenCalledWith({
      nickname: '新昵称',
      email: 'admin@example.com',
      phone: '13800138000',
    })
    expect(wrapper.get('[data-testid="profile-display-name"]').text()).toBe('新昵称')
    expect(refreshUserInfo).toHaveBeenCalledTimes(1)
    expect(
      wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.attributes('disabled'),
    ).toBeDefined()
  })

  it('保存失败后保留用户输入并允许重试', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updateProfile.mockRejectedValue(new Error('save failed'))
    const wrapper = mount(Profile)
    await flushPromises()

    await wrapper.get('[data-testid="profile-nickname"]').setValue('待重试昵称')
    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '待重试昵称',
    )
    expect(
      wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.attributes('disabled'),
    ).toBeUndefined()
  })

  it('资料未修改时直接切换到账号安全页签', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const confirm = vi.spyOn(Modal, 'confirm')
    const wrapper = mount(Profile)
    await flushPromises()

    await wrapper.findAll('[role="tab"]').find((tab) => tab.text() === '账号安全')!.trigger('click')
    await flushPromises()

    expect(wrapper.get('#tab-security').attributes('aria-selected')).toBe('true')
    expect(confirm).not.toHaveBeenCalled()
  })

  it('取消放弃修改时保留基本资料页签和输入', async () => {
    getProfile.mockResolvedValue(normalProfile)
    answerNextConfirmation(false)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    await wrapper.findAll('[role="tab"]').find((tab) => tab.text() === '账号安全')!.trigger('click')
    await flushPromises()

    expect(wrapper.get('#tab-profile').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '未保存昵称',
    )
  })

  it('确认放弃修改后恢复基线并切换到账号安全页签', async () => {
    getProfile.mockResolvedValue(normalProfile)
    answerNextConfirmation(true)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    await wrapper.findAll('[role="tab"]').find((tab) => tab.text() === '账号安全')!.trigger('click')
    await flushPromises()

    expect(wrapper.get('#tab-security').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '管理员',
    )
  })

  it('取消放弃修改时阻止应用内路由离开', async () => {
    getProfile.mockResolvedValue(normalProfile)
    answerNextConfirmation(false)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    expect(routerHooks.leaveGuard).toBeTypeOf('function')
    expect(await routerHooks.leaveGuard!({ path: '/messages' })).toBe(false)
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '未保存昵称',
    )
  })

  it('确认放弃修改时允许应用内路由离开且不提前清空输入', async () => {
    getProfile.mockResolvedValue(normalProfile)
    answerNextConfirmation(true)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    expect(routerHooks.leaveGuard).toBeTypeOf('function')
    expect(await routerHooks.leaveGuard!({ path: '/messages' })).toBe(true)
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '未保存昵称',
    )
  })

  it('离开到登录页时直接放行脏表单且不弹出二次确认', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const confirm = vi.spyOn(Modal, 'confirm')
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    expect(await routerHooks.leaveGuard!({ path: '/login' })).toBe(true)
    expect(confirm).not.toHaveBeenCalled()
  })

  it('资料保存期间离开到登录页仍直接放行', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updateProfile.mockReturnValue(deferred<void>().promise)
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('保存中昵称')
    await wrapper.findAll('button').find((button) => button.text().includes('保存资料'))!.trigger('click')
    await vi.waitFor(() => expect(updateProfile).toHaveBeenCalledTimes(1))

    expect(await routerHooks.leaveGuard!({ path: '/login' })).toBe(true)
  })

  it('重叠的路由离开请求共享同一个放弃修改确认', async () => {
    getProfile.mockResolvedValue(normalProfile)
    let cancelConfirmation: (() => void) | undefined
    const confirm = vi.spyOn(Modal, 'confirm').mockImplementation((options) => {
      cancelConfirmation = () => void options.onCancel?.()
      return { destroy: vi.fn(), update: vi.fn() }
    })
    const wrapper = mount(Profile)
    await flushPromises()
    await wrapper.get('[data-testid="profile-nickname"]').setValue('未保存昵称')

    const firstNavigation = routerHooks.leaveGuard!({ path: '/messages' })
    const secondNavigation = routerHooks.leaveGuard!({ path: '/dashboard' })
    expect(confirm).toHaveBeenCalledTimes(1)

    cancelConfirmation?.()
    expect(await firstNavigation).toBe(false)
    expect(await secondNavigation).toBe(false)
    expect(wrapper.get('[data-testid="profile-nickname"]').element).toHaveProperty(
      'value',
      '未保存昵称',
    )
  })

  it('密码字段为空时展示必填校验且不提交', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)

    await wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('请输入原密码')
    expect(wrapper.text()).toContain('请输入新密码')
    expect(wrapper.text()).toContain('请再次输入新密码')
    expect(updatePassword).not.toHaveBeenCalled()
  })

  it('新密码不足 6 位时阻止提交', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)
    await wrapper.get('input[placeholder="请输入原密码"]').setValue('old-password')
    await wrapper.get('input[placeholder="请输入新密码"]').setValue('12345')
    await wrapper.get('input[placeholder="请再次输入新密码"]').setValue('12345')

    await wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('密码至少 6 位')
    expect(updatePassword).not.toHaveBeenCalled()
  })

  it('两次新密码不一致时阻止提交', async () => {
    getProfile.mockResolvedValue(normalProfile)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)
    await wrapper.get('input[placeholder="请输入原密码"]').setValue('old-password')
    await wrapper.get('input[placeholder="请输入新密码"]').setValue('new-password')
    await wrapper.get('input[placeholder="请再次输入新密码"]').setValue('different-password')

    await wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('两次输入的密码不一致')
    expect(updatePassword).not.toHaveBeenCalled()
  })

  it('修改密码期间锁定密码输入与提交操作', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updatePassword.mockReturnValue(deferred<void>().promise)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)
    await wrapper.get('input[placeholder="请输入原密码"]').setValue('old-password')
    await wrapper.get('input[placeholder="请输入新密码"]').setValue('new-password')
    await wrapper.get('input[placeholder="请再次输入新密码"]').setValue('new-password')

    await wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.trigger('click')
    await vi.waitFor(() => expect(updatePassword).toHaveBeenCalledTimes(1))

    expect(wrapper.get('input[placeholder="请输入原密码"]').attributes('disabled')).toBeDefined()
    expect(
      wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.attributes('disabled'),
    ).toBeDefined()
  })

  it('修改密码成功后清空全部敏感字段', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updatePassword.mockResolvedValue(undefined)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)
    await wrapper.get('input[placeholder="请输入原密码"]').setValue('old-password')
    await wrapper.get('input[placeholder="请输入新密码"]').setValue('new-password')
    await wrapper.get('input[placeholder="请再次输入新密码"]').setValue('new-password')

    await wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!.trigger('click')
    await flushPromises()

    expect(updatePassword).toHaveBeenCalledWith('old-password', 'new-password')
    expect(wrapper.get('input[placeholder="请输入原密码"]').element).toHaveProperty('value', '')
    expect(wrapper.get('input[placeholder="请输入新密码"]').element).toHaveProperty('value', '')
    expect(wrapper.get('input[placeholder="请再次输入新密码"]').element).toHaveProperty('value', '')
  })

  it('修改密码失败后保留输入并允许重试', async () => {
    getProfile.mockResolvedValue(normalProfile)
    updatePassword.mockRejectedValueOnce(new Error('wrong password')).mockResolvedValueOnce(undefined)
    const wrapper = mount(Profile)
    await flushPromises()
    await openSecurityTab(wrapper)
    await wrapper.get('input[placeholder="请输入原密码"]').setValue('wrong-password')
    await wrapper.get('input[placeholder="请输入新密码"]').setValue('new-password')
    await wrapper.get('input[placeholder="请再次输入新密码"]').setValue('new-password')
    const submit = wrapper.findAll('button').find((button) => button.text().includes('修改密码'))!

    await submit.trigger('click')
    await flushPromises()

    expect(wrapper.get('input[placeholder="请输入原密码"]').element).toHaveProperty(
      'value',
      'wrong-password',
    )
    expect(submit.attributes('disabled')).toBeUndefined()

    await submit.trigger('click')
    await flushPromises()
    expect(updatePassword).toHaveBeenCalledTimes(2)
  })
})

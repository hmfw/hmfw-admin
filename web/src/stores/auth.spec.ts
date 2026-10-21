import { beforeEach, describe, expect, it, vi } from 'vitest'

// mock 请求层，避免真实网络；store 依赖 getToken
vi.mock('@/api/request', () => ({
  getToken: vi.fn(() => 'fake-token'),
  clearToken: vi.fn(),
}))

// mock 鉴权接口
const getMe = vi.fn()
const getMenus = vi.fn()
vi.mock('@/api/auth', () => ({
  getMe: () => getMe(),
  getMenus: () => getMenus(),
}))

import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    getMe.mockReset()
    getMenus.mockReset()
    useAuthStore().reset()
  })

  it('loadUserInfo 拉取用户与菜单并置 loaded', async () => {
    getMe.mockResolvedValue({
      userId: '1',
      username: 'admin',
      nickname: '管理员',
      roles: ['admin'],
      permissions: ['*:*:*'],
    })
    getMenus.mockResolvedValue([{ id: '1', menuName: '系统管理' }])

    const auth = useAuthStore()
    await auth.loadUserInfo()

    expect(auth.loaded.value).toBe(true)
    expect(auth.userInfo.value?.username).toBe('admin')
    expect(auth.menus.value).toHaveLength(1)
  })

  it('超级管理员(*:*:*) 对任意权限返回 true', async () => {
    getMe.mockResolvedValue({
      userId: '1',
      username: 'admin',
      nickname: '',
      roles: ['admin'],
      permissions: ['*:*:*'],
    })
    getMenus.mockResolvedValue([])
    const auth = useAuthStore()
    await auth.loadUserInfo()

    expect(auth.hasPermission('system:user:remove')).toBe(true)
    expect(auth.hasPermission('anything:at:all')).toBe(true)
  })

  it('普通用户只对已授权限返回 true', async () => {
    getMe.mockResolvedValue({
      userId: '2',
      username: 'viewer',
      nickname: '',
      roles: ['readonly'],
      permissions: ['system:user:list'],
    })
    getMenus.mockResolvedValue([])
    const auth = useAuthStore()
    await auth.loadUserInfo()

    expect(auth.hasPermission('system:user:list')).toBe(true)
    expect(auth.hasPermission('system:user:add')).toBe(false)
  })

  it('reset 清空状态', async () => {
    getMe.mockResolvedValue({ userId: '1', username: 'a', nickname: '', roles: [], permissions: [] })
    getMenus.mockResolvedValue([])
    const auth = useAuthStore()
    await auth.loadUserInfo()
    auth.reset()

    expect(auth.loaded.value).toBe(false)
    expect(auth.userInfo.value).toBeNull()
    expect(auth.menus.value).toHaveLength(0)
  })
})

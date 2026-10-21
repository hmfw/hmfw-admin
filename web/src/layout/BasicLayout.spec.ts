import { ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const router = {
  push: vi.fn(),
  replace: vi.fn(),
}

vi.mock('vue-router', () => ({
  useRouter: () => router,
  useRoute: () => ({ path: '/dashboard', meta: { title: '首页' } }),
}))

vi.mock('@/api/auth', () => ({ logout: vi.fn() }))
vi.mock('@/api/message', () => ({ getUnreadCount: vi.fn().mockResolvedValue(0) }))

const userInfo = ref({
  userId: '1',
  username: 'admin',
  nickname: '管理员',
  avatarUrl: '/api/profile-avatars/custom.png',
  roles: ['admin'],
  permissions: ['*:*:*'],
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    userInfo,
    menus: ref([]),
    reset: vi.fn(),
  }),
}))

import BasicLayout from './BasicLayout.vue'

describe('BasicLayout 用户头像', () => {
  beforeEach(() => {
    router.push.mockReset()
    router.replace.mockReset()
    userInfo.value.avatarUrl = '/api/profile-avatars/custom.png'
  })

  it('顶部导航优先展示当前用户的自定义头像', async () => {
    const wrapper = mount(BasicLayout, {
      global: {
        stubs: {
          RouterView: true,
          Watermark: { template: '<div><slot /></div>' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="nav-user-avatar"] img').attributes('src')).toBe(
      '/api/profile-avatars/custom.png',
    )

    wrapper.unmount()
  })
})

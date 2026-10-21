import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import UserAvatar from '@/components/UserAvatar.vue'
import { getAvatarPresentation } from './avatar'

describe('getAvatarPresentation', () => {
  it('优先使用去除首尾空白的昵称作为显示名称', () => {
    expect(getAvatarPresentation({ nickname: '  管理员  ', username: 'admin' })).toMatchObject({
      displayName: '管理员',
      initial: '管',
    })
  })

  it('昵称为空时回退到账号并将首个字母转成大写', () => {
    expect(getAvatarPresentation({ nickname: '  ', username: 'alice' })).toMatchObject({
      displayName: 'alice',
      initial: 'A',
    })
  })

  it('显示名称相同时始终映射到同一背景色', () => {
    const first = getAvatarPresentation({ nickname: 'Alice', username: 'alice' })
    const second = getAvatarPresentation({ nickname: 'Alice', username: 'other' })

    expect(first.backgroundColor).toBe(second.backgroundColor)
    expect(first.backgroundColor).toMatch(/^#[0-9a-f]{6}$/i)
  })

  it('昵称和账号均为空时回退为通用名称且不生成字符', () => {
    expect(getAvatarPresentation({ nickname: '', username: '' })).toMatchObject({
      displayName: '用户',
      initial: '',
    })
  })

  it('有自定义头像时优先返回去除空白后的图片地址', () => {
    expect(
      getAvatarPresentation({
        nickname: '管理员',
        username: 'admin',
        avatarUrl: '  /api/profile-avatars/avatar.png  ',
      }),
    ).toMatchObject({ imageUrl: '/api/profile-avatars/avatar.png', initial: '管' })
  })

  it('空头像地址仍使用字符头像', () => {
    expect(
      getAvatarPresentation({ nickname: '管理员', username: 'admin', avatarUrl: '  ' }),
    ).toMatchObject({ imageUrl: undefined, initial: '管' })
  })
})

describe('UserAvatar', () => {
  it('自定义图片加载成功时优先展示图片', () => {
    const wrapper = mount(UserAvatar, {
      props: {
        nickname: '管理员',
        username: 'admin',
        avatarUrl: '/api/profile-avatars/avatar.png',
      },
    })

    expect(wrapper.get('img').attributes('src')).toBe('/api/profile-avatars/avatar.png')
    expect(wrapper.text()).not.toContain('管')
  })

  it('自定义图片加载失败后回退到字符头像', async () => {
    const wrapper = mount(UserAvatar, {
      props: {
        nickname: '管理员',
        username: 'admin',
        avatarUrl: '/api/profile-avatars/missing.png',
      },
    })

    await wrapper.get('img').trigger('error')

    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('管')
  })
})

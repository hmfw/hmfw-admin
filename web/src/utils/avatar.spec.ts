import { describe, expect, it } from 'vitest'
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
})

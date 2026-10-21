import { describe, expect, it } from 'vitest'
import { BarsOutlined, SettingOutlined, UserOutlined, UploadOutlined } from '@hmfw/icons'
import { resolveMenuIcon } from './icon'

describe('resolveMenuIcon', () => {
  it('映射已知图标名到对应组件', () => {
    expect(resolveMenuIcon('setting')).toBe(SettingOutlined)
    expect(resolveMenuIcon('user')).toBe(UserOutlined)
    expect(resolveMenuIcon('upload')).toBe(UploadOutlined)
  })

  it('未知名或空值回退到 BarsOutlined', () => {
    expect(resolveMenuIcon('no-such-icon')).toBe(BarsOutlined)
    expect(resolveMenuIcon(undefined)).toBe(BarsOutlined)
    expect(resolveMenuIcon('')).toBe(BarsOutlined)
  })
})

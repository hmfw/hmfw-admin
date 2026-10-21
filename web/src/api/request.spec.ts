import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@hmfw/ant-design', () => ({
  message: { error: vi.fn() },
}))

import { BizError, clearToken, getToken, http, setTokenWithExpiry } from './request'

function mockFetchOnce(body: unknown, status = 200) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => ({
      status,
      json: async () => body,
    })),
  )
}

describe('request / http', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('code=200 时解包返回 data', async () => {
    mockFetchOnce({ code: 200, msg: 'ok', data: { id: '7', name: 'x' } })
    const data = await http.get<{ id: string; name: string }>('/demo')
    expect(data).toEqual({ id: '7', name: 'x' })
  })

  it('code!=200 时抛出带码的 BizError', async () => {
    mockFetchOnce({ code: 1001, msg: '用户名或密码错误', data: null })
    await expect(http.post('/auth/login', {}, true)).rejects.toMatchObject({
      name: 'BizError',
      code: 1001,
    })
  })

  it('登录失败时保留 401 响应中的业务码和消息', async () => {
    mockFetchOnce({ code: 1001, msg: '用户名或密码错误', data: null }, 401)
    await expect(http.post('/auth/login', {}, true)).rejects.toMatchObject({
      name: 'BizError',
      code: 1001,
      message: '用户名或密码错误',
    })
  })

  it('BizError 携带 code 与消息', () => {
    const e = new BizError(403, '无权限')
    expect(e.code).toBe(403)
    expect(e.message).toBe('无权限')
  })

  it('token 读写与清除', () => {
    expect(getToken()).toBeNull()
    setTokenWithExpiry('abc', 7200)
    expect(getToken()).toBe('abc')
    clearToken()
    expect(getToken()).toBeNull()
  })
})

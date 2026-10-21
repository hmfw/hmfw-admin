import { computed, reactive, readonly } from 'vue'
import { getMe, getMenus } from '@/api/auth'
import { clearToken, getToken } from '@/api/request'
import type { MenuTreeNode, UserInfo } from '@/types/api'

const ALL_PERMISSION = '*:*:*'

interface AuthState {
  userInfo: UserInfo | null
  menus: MenuTreeNode[]
  loaded: boolean
}

const state = reactive<AuthState>({
  userInfo: null,
  menus: [],
  loaded: false,
})

/** 从后端拉取当前用户信息与菜单树（登录后、刷新页面后调用） */
async function loadUserInfo(): Promise<void> {
  const [userInfo, menus] = await Promise.all([getMe(), getMenus()])
  state.userInfo = userInfo
  state.menus = menus
  state.loaded = true
}

/** 只刷新用户信息（如个人中心改了昵称后，让头像区昵称即时更新） */
async function refreshUserInfo(): Promise<void> {
  state.userInfo = await getMe()
}

function reset(): void {
  state.userInfo = null
  state.menus = []
  state.loaded = false
  clearToken()
}

/** 是否拥有某权限标识；超级管理员（含 *:*:*）恒为 true */
function hasPermission(perm: string): boolean {
  const perms = state.userInfo?.permissions ?? []
  return perms.includes(ALL_PERMISSION) || perms.includes(perm)
}

export function useAuthStore() {
  return {
    state: readonly(state),
    isLoggedIn: computed(() => !!getToken()),
    userInfo: computed(() => state.userInfo),
    menus: computed(() => state.menus),
    loaded: computed(() => state.loaded),
    loadUserInfo,
    refreshUserInfo,
    reset,
    hasPermission,
  }
}

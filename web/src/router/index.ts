import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      {
        path: '/profile',
        name: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: '/messages',
        name: 'messages',
        component: () => import('@/views/MessageView.vue'),
        meta: { title: '消息中心' },
      },
      {
        path: '/system/user',
        name: 'system-user',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理', perm: 'system:user:list' },
      },
      {
        path: '/system/role',
        name: 'system-role',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色管理', perm: 'system:role:list' },
      },
      {
        path: '/system/menu',
        name: 'system-menu',
        component: () => import('@/views/system/MenuView.vue'),
        meta: { title: '菜单管理', perm: 'system:menu:list' },
      },
      {
        path: '/system/dept',
        name: 'system-dept',
        component: () => import('@/views/system/DeptView.vue'),
        meta: { title: '部门管理', perm: 'system:dept:list' },
      },
      {
        path: '/system/dict',
        name: 'system-dict',
        component: () => import('@/views/system/DictView.vue'),
        meta: { title: '字典管理', perm: 'system:dict:list' },
      },
      {
        path: '/system/file',
        name: 'system-file',
        component: () => import('@/views/system/FileView.vue'),
        meta: { title: '文件管理', perm: 'system:file:list' },
      },
      {
        path: '/system/notice',
        name: 'system-notice',
        component: () => import('@/views/system/NoticeView.vue'),
        meta: { title: '通知公告', perm: 'system:notice:list' },
      },
      {
        path: '/monitor/operlog',
        name: 'monitor-operlog',
        component: () => import('@/views/monitor/OperLogView.vue'),
        meta: { title: '操作日志', perm: 'monitor:operlog:list' },
      },
      {
        path: '/monitor/loginlog',
        name: 'monitor-loginlog',
        component: () => import('@/views/monitor/LoginLogView.vue'),
        meta: { title: '登录日志', perm: 'monitor:loginlog:list' },
      },
      {
        path: '/monitor/online',
        name: 'monitor-online',
        component: () => import('@/views/monitor/OnlineView.vue'),
        meta: { title: '在线用户', perm: 'monitor:online:list' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const hasToken = !!getToken()

  // 已登录访问登录页 → 回首页
  if (to.path === '/login') {
    return hasToken ? { path: '/' } : true
  }

  if (to.meta.public) return true

  if (!hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 刷新后 store 为空，补拉用户信息与菜单
  if (!auth.loaded.value) {
    try {
      await auth.loadUserInfo()
    } catch {
      auth.reset()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  // 细粒度权限：无对应 list 权限则拦到首页
  const perm = to.meta.perm as string | undefined
  if (perm && !auth.hasPermission(perm)) {
    return { path: '/dashboard' }
  }

  return true
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? ''
  document.title = title ? `${title} · 后台管理系统` : '后台管理系统'
})

export default router

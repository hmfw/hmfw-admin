<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Badge,
  Breadcrumb,
  Content,
  Dropdown,
  Header,
  Layout,
  Menu,
  Modal,
  Sider,
  Watermark,
  message,
} from '@hmfw/ant-design'
import {
  BellOutlined,
  DownOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@hmfw/icons'
import { logout } from '@/api/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import { getUnreadCount } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import { getAvatarPresentation } from '@/utils/avatar'
import { resolveMenuIcon } from '@/utils/icon'
import type { MenuTreeNode } from '@/types/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const collapsed = ref(false)
const unreadCount = ref(0)

interface MenuItem {
  key: string
  label: string
  icon?: () => ReturnType<typeof h>
  children?: MenuItem[]
}

/** 把后端菜单树转成 Menu 组件 items；只保留目录(M)与菜单(C)，按钮(F)不进导航 */
function toMenuItems(nodes: MenuTreeNode[]): MenuItem[] {
  return nodes
    .filter((n) => n.menuType !== 'F' && n.visible !== 0)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
    .map((n) => {
      const children = toMenuItems(n.children ?? [])
      const item: MenuItem = {
        key: n.path || `menu-${n.id}`,
        label: n.menuName,
        icon: () => h(resolveMenuIcon(n.icon)),
      }
      if (children.length) item.children = children
      return item
    })
}

const menuItems = computed(() => toMenuItems(auth.menus.value))
const selectedKeys = computed(() => [route.path])
const openKeys = ref<string[]>([])

/** 展开当前路由所在的父级目录 */
function syncOpenKeys() {
  const parents: string[] = []
  const walk = (nodes: MenuItem[], trail: string[]) => {
    for (const item of nodes) {
      if (item.key === route.path) parents.push(...trail)
      if (item.children) walk(item.children, [...trail, item.key])
    }
  }
  walk(menuItems.value, [])
  openKeys.value = Array.from(new Set([...openKeys.value, ...parents]))
}
watch([() => route.path, menuItems], syncOpenKeys, { immediate: true })

function onMenuClick({ key }: { key: string }) {
  if (key !== route.path && !key.startsWith('menu-')) router.push(key)
}

const currentUserAvatar = computed(() =>
  getAvatarPresentation({
    nickname: auth.userInfo.value?.nickname,
    username: auth.userInfo.value?.username,
    avatarUrl: auth.userInfo.value?.avatarUrl,
  }),
)
const displayName = computed(() => currentUserAvatar.value.displayName)

// 水印配置
const WATERMARK_TIME_FORMAT = 'yyyy-MM-dd HH:mm:ss'

// 时间格式化辅助函数
function pad(n: number) {
  return String(n).padStart(2, '0')
}

function formatDateTime(date: Date): string {
  const map: Record<string, string> = {
    yyyy: String(date.getFullYear()),
    MM: pad(date.getMonth() + 1),
    dd: pad(date.getDate()),
    HH: pad(date.getHours()),
    mm: pad(date.getMinutes()),
    ss: pad(date.getSeconds()),
  }
  return WATERMARK_TIME_FORMAT.replace(/yyyy|MM|dd|HH|mm|ss/g, (k) => map[k])
}

// 当前时间（每 5 秒刷新）
const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | undefined
let messageTimer: ReturnType<typeof setInterval> | undefined

async function refreshUnreadCount() {
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    unreadCount.value = 0
  }
}

onMounted(() => {
  timer = setInterval(() => (now.value = new Date()), 5000)
  refreshUnreadCount()
  messageTimer = setInterval(refreshUnreadCount, 60000)
  window.addEventListener('message-read', refreshUnreadCount)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (messageTimer) clearInterval(messageTimer)
  window.removeEventListener('message-read', refreshUnreadCount)
})

// 水印样式配置
const watermarkFont = {
  color: 'rgba(0, 0, 0, 0.2)', // 半透明黑色
  fontSize: 15,
  fontWeight: 'lighter' as const,
}

// 水印内容（用户名 + 时间）
const watermarkContent = computed(() => [displayName.value, formatDateTime(now.value)])

const HOME_PATH = '/dashboard'

/** 在后端菜单树中按 path 定位当前节点，返回从根目录到该节点的链路 */
function findTrail(
  nodes: MenuTreeNode[],
  target: string,
  trail: MenuTreeNode[] = [],
): MenuTreeNode[] | null {
  for (const n of nodes) {
    const next = [...trail, n]
    if (n.path === target) return next
    const found = findTrail(n.children ?? [], target, next)
    if (found) return found
  }
  return null
}

/** 面包屑项；复用侧边栏同一棵菜单树，层级与导航保持一致 */
const breadcrumbItems = computed(() => {
  const home = { title: '首页', onClick: () => goBreadcrumb(HOME_PATH), className: 'bc-link' }
  if (route.path === HOME_PATH) return [{ title: '首页' }]

  const trail = findTrail(auth.menus.value, route.path)
  if (!trail) {
    // 不在菜单树中（如动态详情页）退回 meta.title
    const title = (route.meta.title as string | undefined) ?? ''
    return title ? [home, { title }] : [home]
  }

  const crumbs = trail.map((n) => ({ title: n.menuName }))
  return [home, ...crumbs]
})

function goBreadcrumb(path: string) {
  if (path !== route.path) router.push(path)
}

const userMenu = {
  items: [
    { key: 'profile', label: '个人中心', icon: () => h(UserOutlined) },
    { key: 'logout', label: '退出登录', icon: () => h(LogoutOutlined) },
  ],
  onClick: ({ key }: { key: string }) => {
    if (key === 'profile') router.push('/profile')
    else if (key === 'logout') handleLogout()
  },
}

function handleLogout() {
  Modal.confirm({
    title: '提示',
    content: '确认退出登录？',
    okText: '退出',
    cancelText: '取消',
    onOk: async () => {
      try {
        await logout()
      } catch {
        // 即使服务端注销失败，客户端仍需清理本地登录态。
      }
      auth.reset()
      message.success('已退出登录')
      router.replace('/login')
    },
  })
}
</script>

<template>
  <Layout style="min-height: 100vh">
    <Sider v-model:collapsed="collapsed" :trigger="null" collapsible :width="220" theme="dark">
      <div class="logo">
        <span v-if="!collapsed">后台管理系统</span>
        <span v-else>后台</span>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        :items="menuItems"
        :selected-keys="selectedKeys"
        :open-keys="openKeys"
        @open-change="(keys: string[]) => (openKeys = keys)"
        @click="onMenuClick"
      />
    </Sider>
    <Layout>
      <Header class="header">
        <div class="header-left">
          <component
            :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <Breadcrumb :items="breadcrumbItems" />
        </div>
        <div class="header-actions">
          <Badge :count="unreadCount" :overflow-count="99">
            <button class="message-button" title="消息中心" @click="router.push('/messages')">
              <BellOutlined />
            </button>
          </Badge>
          <Dropdown :menu="userMenu" trigger="click">
            <div class="user-area">
              <UserAvatar
                data-testid="nav-user-avatar"
                :nickname="auth.userInfo.value?.nickname"
                :username="auth.userInfo.value?.username"
                :avatar-url="auth.userInfo.value?.avatarUrl"
              />
              <span class="user-name">{{ displayName }}</span>
              <DownOutlined style="font-size: 12px" />
            </div>
          </Dropdown>
        </div>
      </Header>
      <Content class="content">
        <Watermark
          :content="watermarkContent"
          :width="220"
          :height="140"
          :rotate="-30"
          :z-index="9"
          :gap="[100, 80]"
          :font="watermarkFont"
        >
          <RouterView />
        </Watermark>
      </Content>
    </Layout>
  </Layout>
</template>

<style scoped>
.logo {
  height: 48px;
  margin: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  color: rgba(0, 0, 0, 0.65);
}

.trigger:hover {
  color: #1677ff;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
}

/* 可点击的面包屑项（首页）悬浮高亮 */
.header-left :deep(.bc-link) {
  cursor: pointer;
}

.header-left :deep(.bc-link:hover) {
  color: #1677ff;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 8px;
  height: 100%;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.message-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: rgba(0, 0, 0, 0.65);
  font-size: 18px;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
}

.message-button:hover {
  color: #1677ff;
  background: #f5f5f5;
}

.user-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}

.content {
  margin: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  overflow: auto;
}

@media (max-width: 760px) {
  .header {
    gap: 8px;
    padding: 0 12px;
  }

  .header-left,
  .header-actions {
    gap: 8px;
  }

  .header-left {
    min-width: 0;
  }

  .user-area {
    padding: 0 2px;
  }

  .user-name,
  .user-area > .anticon-down {
    display: none;
  }

  .content {
    margin: 8px;
    padding: 12px;
  }
}
</style>

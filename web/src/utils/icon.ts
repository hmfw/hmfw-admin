import type { Component } from 'vue'
import {
  ApartmentOutlined,
  AppstoreOutlined,
  BarsOutlined,
  DashboardOutlined,
  FileTextOutlined,
  KeyOutlined,
  LoginOutlined,
  MenuOutlined,
  MonitorOutlined,
  NotificationOutlined,
  ProfileOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  TeamOutlined,
  UploadOutlined,
  UserOutlined,
} from '@hmfw/icons'

/**
 * 后端菜单 icon 字段是字符串（如 'setting' / 'user' / 'peoples' / 'tree'），
 * 这里映射到 @hmfw/icons 的组件。未命中时回退到通用图标。
 */
const ICON_MAP: Record<string, Component> = {
  setting: SettingOutlined,
  user: UserOutlined,
  peoples: TeamOutlined,
  team: TeamOutlined,
  tree: ProfileOutlined,
  menu: MenuOutlined,
  dashboard: DashboardOutlined,
  key: KeyOutlined,
  role: SafetyCertificateOutlined,
  system: AppstoreOutlined,
  monitor: MonitorOutlined,
  log: FileTextOutlined,
  logininfor: LoginOutlined,
  dept: ApartmentOutlined,
  upload: UploadOutlined,
  notice: NotificationOutlined,
}

export function resolveMenuIcon(icon?: string): Component {
  if (icon && ICON_MAP[icon]) return ICON_MAP[icon]
  return BarsOutlined
}

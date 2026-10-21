<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button, Card, Empty, Progress, Statistic, Tag } from '@hmfw/ant-design'
import { ReloadOutlined } from '@hmfw/icons'
import { useRouter } from 'vue-router'
import { getDashboardSummary } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'
import type { DashboardSummary } from '@/types/api'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const summary = ref<DashboardSummary | null>(null)
const info = computed(() => auth.userInfo.value)
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const maxTrend = computed(() =>
  Math.max(1, ...(summary.value?.loginTrend.map((item) => item.success + item.failure) ?? [1])),
)

async function fetchSummary() {
  loading.value = true
  try {
    summary.value = await getDashboardSummary()
  } finally {
    loading.value = false
  }
}

function formatBytes(bytes = 0) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

onMounted(fetchSummary)
</script>

<template>
  <div class="dashboard">
    <section class="welcome-band">
      <div>
        <h1>{{ greeting }}，{{ info?.nickname || info?.username }}</h1>
        <p>这里是今天的系统运行概况。</p>
      </div>
      <Button :icon="ReloadOutlined" :loading="loading" @click="fetchSummary">刷新</Button>
    </section>

    <section class="metric-grid">
      <div class="metric metric-users">
        <span>用户总数</span><strong>{{ summary?.totalUsers ?? 0 }}</strong
        ><small>启用 {{ summary?.enabledUsers ?? 0 }} 人</small>
      </div>
      <div class="metric metric-online">
        <span>当前在线</span><strong>{{ summary?.onlineSessions ?? 0 }}</strong
        ><small>活跃会话</small>
      </div>
      <div class="metric metric-login">
        <span>今日登录</span><strong>{{ summary?.todayLogins ?? 0 }}</strong
        ><small>失败 {{ summary?.todayFailures ?? 0 }} 次</small>
      </div>
      <div class="metric metric-message" @click="router.push('/messages')">
        <span>未读消息</span><strong>{{ summary?.unreadMessages ?? 0 }}</strong
        ><small>点击进入消息中心</small>
      </div>
      <div class="metric metric-file">
        <span>文件存储</span><strong>{{ formatBytes(summary?.fileSize) }}</strong
        ><small>{{ summary?.fileCount ?? 0 }} 个文件</small>
      </div>
    </section>

    <section class="dashboard-grid">
      <Card title="近 7 日登录趋势" :bordered="false">
        <div class="trend-legend"><i class="success-dot"></i>成功 <i class="fail-dot"></i>失败</div>
        <div class="trend-chart">
          <div v-for="item in summary?.loginTrend ?? []" :key="item.date" class="trend-column">
            <div class="trend-bars">
              <div
                class="trend-bar success-bar"
                :style="{ height: `${Math.max(3, (item.success / maxTrend) * 150)}px` }"
                :title="`成功 ${item.success}`"
              ></div>
              <div
                class="trend-bar fail-bar"
                :style="{
                  height: `${item.failure ? Math.max(3, (item.failure / maxTrend) * 150) : 0}px`,
                }"
                :title="`失败 ${item.failure}`"
              ></div>
            </div>
            <span>{{ item.date.slice(5) }}</span>
          </div>
        </div>
      </Card>

      <Card title="系统健康" :bordered="false">
        <div class="health-row">
          <span>账号启用率</span>
          <Progress
            :percent="
              summary?.totalUsers
                ? Math.round((summary.enabledUsers / summary.totalUsers) * 100)
                : 0
            "
          />
        </div>
        <div class="health-stat">
          <Statistic title="在线会话" :value="summary?.onlineSessions ?? 0" />
          <Statistic title="今日失败登录" :value="summary?.todayFailures ?? 0" />
        </div>
      </Card>

      <Card title="最近操作" :bordered="false">
        <Empty v-if="!summary?.recentActivities.length" description="暂无操作记录" />
        <div v-else class="activity-list">
          <div
            v-for="(item, index) in summary?.recentActivities"
            :key="index"
            class="activity-item"
          >
            <span class="activity-mark" :class="item.status === 0 ? 'ok' : 'error'"></span>
            <div>
              <strong>{{ item.title || '系统操作' }}</strong
              ><small>{{ item.operator || '-' }} · {{ formatDate(item.time) }}</small>
            </div>
            <Tag :color="item.status === 0 ? 'green' : 'red'">{{
              item.businessType || 'OTHER'
            }}</Tag>
          </div>
        </div>
      </Card>

      <Card title="最新消息" :bordered="false">
        <template #extra
          ><Button type="link" @click="router.push('/messages')">查看全部</Button></template
        >
        <Empty v-if="!summary?.recentMessages.length" description="暂无消息" />
        <div v-else class="message-list">
          <button
            v-for="item in summary?.recentMessages"
            :key="item.id"
            class="message-item"
            @click="router.push({ path: '/messages', query: { id: item.id } })"
          >
            <span :class="['message-dot', { read: item.readStatus === 1 }]"></span>
            <span class="message-title">{{ item.noticeTitle }}</span>
            <small>{{ item.publishTime?.slice(5, 16).replace('T', ' ') }}</small>
          </button>
        </div>
      </Card>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  background: #f5f7fa;
  margin: -20px;
  padding: 20px;
  min-height: calc(100vh - 104px);
}
.welcome-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.welcome-band h1 {
  margin: 0 0 5px;
  font-size: 22px;
  letter-spacing: 0;
}
.welcome-band p {
  margin: 0;
  color: rgba(0, 0, 0, 0.5);
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}
.metric {
  min-width: 0;
  padding: 17px 18px;
  color: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.metric span,
.metric small {
  display: block;
  opacity: 0.88;
}
.metric strong {
  display: block;
  margin: 7px 0 2px;
  font-size: 27px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}
.metric small {
  font-size: 12px;
}
.metric-users {
  background: #2878d0;
}
.metric-online {
  background: #237b65;
}
.metric-login {
  background: #536273;
}
.metric-message {
  background: #b45f3c;
  cursor: pointer;
}
.metric-file {
  background: #596b95;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(280px, 2fr);
  gap: 14px;
}
.dashboard-grid :deep(.ant-card) {
  border-radius: 6px;
}
.trend-legend {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.55);
  font-size: 12px;
}
.trend-legend i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-left: 8px;
}
.success-dot,
.success-bar {
  background: #2878d0;
}
.fail-dot,
.fail-bar {
  background: #d65a4a;
}
.trend-chart {
  height: 195px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  align-items: end;
  gap: 10px;
  padding: 14px 5px 0;
  border-bottom: 1px solid #e7eaf0;
}
.trend-column {
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  gap: 7px;
  color: rgba(0, 0, 0, 0.5);
  font-size: 12px;
}
.trend-bars {
  height: 155px;
  display: flex;
  align-items: end;
  gap: 3px;
}
.trend-bar {
  width: 12px;
  border-radius: 2px 2px 0 0;
  transition: height 0.2s;
}
.health-row > span {
  display: block;
  margin-bottom: 9px;
  color: rgba(0, 0, 0, 0.65);
}
.health-stat {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
.activity-list,
.message-list {
  display: grid;
}
.activity-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  border-bottom: 1px solid #f0f0f0;
}
.activity-item:last-child,
.message-item:last-child {
  border-bottom: 0;
}
.activity-item strong,
.activity-item small {
  display: block;
}
.activity-item small {
  margin-top: 2px;
  color: rgba(0, 0, 0, 0.45);
}
.activity-mark,
.message-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2878d0;
}
.activity-mark.error {
  background: #d65a4a;
}
.activity-mark.ok {
  background: #2f8f6b;
}
.message-item {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0;
  background: none;
  border: 0;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  cursor: pointer;
}
.message-item:hover .message-title {
  color: #1677ff;
}
.message-dot.read {
  background: #c5cad1;
}
.message-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-item small {
  color: rgba(0, 0, 0, 0.45);
}
@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 760px) {
  .dashboard {
    margin: -20px;
    padding: 14px;
  }
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  .welcome-band {
    align-items: flex-start;
  }
}
</style>

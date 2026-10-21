<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Button, Input, Modal, Space, Table, Tag, message } from '@hmfw/ant-design'
import { DisconnectOutlined, ReloadOutlined } from '@hmfw/icons'
import { kickSessions, pageOnlineSessions } from '@/api/online'
import { useAuthStore } from '@/stores/auth'
import type { OnlineSession } from '@/types/api'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<OnlineSession[]>([])
const total = ref(0)
const selectedRowKeys = ref<string[]>([])
const query = reactive({ pageNum: 1, pageSize: 10, username: '', ip: '' })

async function fetchData() {
  loading.value = true
  try {
    const result = await pageOnlineSessions({ ...query })
    rows.value = result.records
    total.value = result.total
    selectedRowKeys.value = []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  fetchData()
}

function onReset() {
  Object.assign(query, { pageNum: 1, username: '', ip: '' })
  fetchData()
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}
function confirmKick(ids: string[]) {
  Modal.confirm({
    title: '确认强制下线？',
    content: '被下线的会话需要重新登录。',
    okText: '下线',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await kickSessions(ids)
      message.success('会话已下线')
      fetchData()
    },
  })
}

const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => `共 ${count} 条`,
  onChange: (page: number, pageSize: number) => {
    query.pageNum = page
    query.pageSize = pageSize
    fetchData()
  },
}))
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => (selectedRowKeys.value = keys.map(String)),
  getCheckboxProps: (record: OnlineSession) => ({ disabled: record.current }),
}))
const columns = [
  {
    title: '用户',
    width: 150,
    render: (_: unknown, row: OnlineSession) =>
      row.nickname ? `${row.nickname} (${row.username})` : row.username,
  },
  { title: '登录 IP', dataIndex: 'ip', width: 150 },
  { title: '客户端', dataIndex: 'userAgent', ellipsis: true },
  {
    title: '登录时间',
    width: 180,
    render: (_: unknown, row: OnlineSession) => formatDate(row.loginTime),
  },
  {
    title: '最后活跃',
    width: 180,
    render: (_: unknown, row: OnlineSession) => formatDate(row.lastActiveTime),
  },
  {
    title: '状态',
    width: 90,
    render: (_: unknown, row: OnlineSession) =>
      h(Tag, { color: row.current ? 'blue' : 'green' }, () => (row.current ? '当前会话' : '在线')),
  },
  {
    title: '操作',
    width: 100,
    render: (_: unknown, row: OnlineSession) =>
      auth.hasPermission('monitor:online:kick') && !row.current
        ? h(
            Button,
            { type: 'link', size: 'small', danger: true, onClick: () => confirmKick([row.id]) },
            () => '强制下线',
          )
        : '-',
  },
]
onMounted(fetchData)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <Space wrap>
        <Input
          v-model:value="query.username"
          placeholder="用户名"
          allow-clear
          style="width: 180px"
          @press-enter="onSearch"
        />
        <Input
          v-model:value="query.ip"
          placeholder="登录 IP"
          allow-clear
          style="width: 180px"
          @press-enter="onSearch"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button :icon="ReloadOutlined" :loading="loading" @click="fetchData">刷新</Button>
    </div>
    <div
      v-if="selectedRowKeys.length && auth.hasPermission('monitor:online:kick')"
      class="batch-toolbar"
    >
      <span>已选择 {{ selectedRowKeys.length }} 个会话</span>
      <Button danger :icon="DisconnectOutlined" @click="confirmKick(selectedRowKeys)"
        >批量下线</Button
      >
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-selection="auth.hasPermission('monitor:online:kick') ? rowSelection : undefined"
      :scroll="{ x: 1100 }"
      row-key="id"
    />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 46px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
}
</style>

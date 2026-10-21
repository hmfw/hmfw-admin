<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Button, Input, Modal, Select, Space, Table, Tag, message } from '@hmfw/ant-design'
import { cleanLoginLogs, deleteLoginLogs, pageLoginLogs } from '@/api/log'
import { useAuthStore } from '@/stores/auth'
import type { SysLoginLog } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

const loading = ref(false)
const rows = ref<SysLoginLog[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  status: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageLoginLogs({ ...query })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
onMounted(fetchData)

function onSearch() {
  query.pageNum = 1
  fetchData()
}
function onReset() {
  query.username = ''
  query.status = undefined
  query.pageNum = 1
  fetchData()
}

const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
  onChange: (p: number, ps: number) => {
    query.pageNum = p
    query.pageSize = ps
    fetchData()
  },
}))

function confirmDelete(record: SysLoginLog) {
  Modal.confirm({
    title: '确认删除该条日志？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteLoginLogs([record.id])
      message.success('删除成功')
      fetchData()
    },
  })
}
function confirmClean() {
  Modal.confirm({
    title: '确认清空全部登录日志？',
    content: '该操作不可恢复。',
    okText: '清空',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await cleanLoginLogs()
      message.success('已清空')
      fetchData()
    },
  })
}

const columns = [
  { title: '登录账号', dataIndex: 'username', key: 'username' },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 150 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (_: unknown, record: SysLoginLog) =>
      record.status === 0
        ? h(Tag, { color: 'success' }, () => '成功')
        : h(Tag, { color: 'error' }, () => '失败'),
  },
  { title: '提示信息', dataIndex: 'msg', key: 'msg' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 180 },
  {
    title: '操作',
    key: 'action',
    width: 90,
    render: (_: unknown, record: SysLoginLog) => {
      if (!can('monitor:loginlog:remove')) return h('span', { style: 'color:#bbb' }, '-')
      return h(
        Button,
        { type: 'link', size: 'small', danger: true, onClick: () => confirmDelete(record) },
        () => '删除',
      )
    },
  },
]
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <Space wrap>
        <Input
          v-model:value="query.username"
          placeholder="登录账号"
          allow-clear
          style="width: 180px"
          @press-enter="onSearch"
        />
        <Select
          v-model:value="query.status"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '成功', value: 0 },
            { label: '失败', value: 1 },
          ]"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button v-if="can('monitor:loginlog:clean')" danger @click="confirmClean">清空</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
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
</style>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Button, Input, Modal, Select, Space, Table, Tag, message } from '@hmfw/ant-design'
import { CheckOutlined } from '@hmfw/icons'
import { TiptapEditor } from '@hmfw/tiptap-editor'
import { useRoute, useRouter } from 'vue-router'
import { getMessage, markAllMessagesRead, markMessagesRead, pageMessages } from '@/api/message'
import type { MessageItem } from '@/types/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref<MessageItem[]>([])
const total = ref(0)
const selectedRowKeys = ref<string[]>([])
const detailOpen = ref(false)
const detail = ref<MessageItem | null>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  noticeType: undefined as number | undefined,
  readStatus: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const result = await pageMessages({ ...query })
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
  Object.assign(query, { pageNum: 1, title: '', noticeType: undefined, readStatus: undefined })
  fetchData()
}

async function openDetail(id: string) {
  detail.value = await getMessage(id)
  detailOpen.value = true
  const row = rows.value.find((item) => item.id === id)
  if (row) row.readStatus = 1
  window.dispatchEvent(new Event('message-read'))
}

async function markSelected() {
  await markMessagesRead(selectedRowKeys.value)
  message.success('已标记为已读')
  window.dispatchEvent(new Event('message-read'))
  fetchData()
}

async function markAll() {
  await markAllMessagesRead()
  message.success('全部消息已读')
  window.dispatchEvent(new Event('message-read'))
  fetchData()
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ') : '-'
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
  getCheckboxProps: (record: MessageItem) => ({ disabled: record.readStatus === 1 }),
}))
const columns = [
  {
    title: '状态',
    width: 80,
    render: (_: unknown, record: MessageItem) =>
      h(Tag, { color: record.readStatus === 0 ? 'blue' : 'default' }, () =>
        record.readStatus === 0 ? '未读' : '已读',
      ),
  },
  {
    title: '标题',
    dataIndex: 'noticeTitle',
    ellipsis: true,
    render: (text: string, record: MessageItem) =>
      h(
        Button,
        {
          type: 'link',
          class: ['title-link', { unread: record.readStatus === 0 }],
          onClick: () => openDetail(record.id),
        },
        () => text,
      ),
  },
  {
    title: '类型',
    width: 80,
    render: (_: unknown, record: MessageItem) => (record.noticeType === 1 ? '通知' : '公告'),
  },
  { title: '发布人', dataIndex: 'creatorName', width: 130 },
  {
    title: '发布时间',
    width: 180,
    render: (_: unknown, record: MessageItem) => formatDate(record.publishTime),
  },
  {
    title: '阅读时间',
    width: 180,
    render: (_: unknown, record: MessageItem) => formatDate(record.readTime),
  },
]

onMounted(async () => {
  await fetchData()
  const id = typeof route.query.id === 'string' ? route.query.id : undefined
  if (id) {
    await openDetail(id)
    router.replace({ path: '/messages' })
  }
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <Space wrap>
        <Input
          v-model:value="query.title"
          placeholder="消息标题"
          allow-clear
          style="width: 220px"
          @press-enter="onSearch"
        />
        <Select
          v-model:value="query.noticeType"
          placeholder="类型"
          allow-clear
          style="width: 110px"
          :options="[
            { label: '通知', value: 1 },
            { label: '公告', value: 2 },
          ]"
        />
        <Select
          v-model:value="query.readStatus"
          placeholder="阅读状态"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '未读', value: 0 },
            { label: '已读', value: 1 },
          ]"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button :icon="CheckOutlined" @click="markAll">全部已读</Button>
    </div>
    <div v-if="selectedRowKeys.length" class="batch-toolbar">
      <span>已选择 {{ selectedRowKeys.length }} 条未读消息</span>
      <Button type="primary" :icon="CheckOutlined" @click="markSelected">标记已读</Button>
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 900 }"
      row-key="id"
    />

    <Modal v-model:open="detailOpen" title="消息详情" :width="900" :footer="null" destroy-on-hidden>
      <article v-if="detail" class="message-detail">
        <h1>{{ detail.noticeTitle }}</h1>
        <div class="message-meta">
          <Tag :color="detail.noticeType === 1 ? 'blue' : 'orange'">{{
            detail.noticeType === 1 ? '通知' : '公告'
          }}</Tag>
          <span>发布人：{{ detail.creatorName || '-' }}</span>
          <span>发布时间：{{ formatDate(detail.publishTime) }}</span>
        </div>
        <TiptapEditor v-model="detail.noticeContent" readonly />
      </article>
    </Modal>
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
  background: #f5f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
}
.title-link {
  max-width: 100%;
  padding-inline: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 400;
}
.title-link.unread {
  font-weight: 600;
}
.message-detail h1 {
  margin: 8px 0 14px;
  text-align: center;
  font-size: 24px;
  letter-spacing: 0;
}
.message-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 16px;
  margin-bottom: 14px;
  color: rgba(0, 0, 0, 0.55);
  border-bottom: 1px solid #f0f0f0;
}
.message-detail :deep(.tiptap) {
  min-height: 280px;
  padding: 8px 16px;
}
</style>

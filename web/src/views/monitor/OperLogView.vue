<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Button, Modal, Select, Space, Table, Tag, message } from '@hmfw/ant-design'
import { cleanOperLogs, deleteOperLogs, pageOperLogs } from '@/api/log'
import { useAuthStore } from '@/stores/auth'
import type { SysOperLog } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

const typeMeta: Record<string, { text: string; color: string }> = {
  OTHER: { text: '其它', color: 'default' },
  INSERT: { text: '新增', color: 'green' },
  UPDATE: { text: '修改', color: 'blue' },
  DELETE: { text: '删除', color: 'red' },
  CLEAN: { text: '清空', color: 'orange' },
}

const loading = ref(false)
const rows = ref<SysOperLog[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  operName: '',
  status: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageOperLogs({ ...query })
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
  query.title = ''
  query.operName = ''
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

/* ------------------------------ 详情 ------------------------------ */
const detailOpen = ref(false)
const detail = ref<SysOperLog | null>(null)
function openDetail(record: SysOperLog) {
  detail.value = record
  detailOpen.value = true
}

/* ------------------------------ 删除/清空 ------------------------------ */
function confirmDelete(record: SysOperLog) {
  Modal.confirm({
    title: '确认删除该条日志？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteOperLogs([record.id])
      message.success('删除成功')
      fetchData()
    },
  })
}
function confirmClean() {
  Modal.confirm({
    title: '确认清空全部操作日志？',
    content: '该操作不可恢复。',
    okText: '清空',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await cleanOperLogs()
      message.success('已清空')
      fetchData()
    },
  })
}

const columns = [
  { title: '模块', dataIndex: 'title', key: 'title', width: 120 },
  {
    title: '类型',
    key: 'businessType',
    width: 80,
    render: (_: unknown, record: SysOperLog) => {
      const m = typeMeta[record.businessType ?? 'OTHER'] ?? typeMeta.OTHER
      return h(Tag, { color: m.color }, () => m.text)
    },
  },
  { title: '请求方式', dataIndex: 'requestMethod', key: 'requestMethod', width: 90 },
  { title: '操作人', dataIndex: 'operName', key: 'operName', width: 110 },
  { title: 'IP', dataIndex: 'operIp', key: 'operIp', width: 130 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (_: unknown, record: SysOperLog) =>
      record.status === 0
        ? h(Tag, { color: 'success' }, () => '正常')
        : h(Tag, { color: 'error' }, () => '异常'),
  },
  {
    title: '耗时',
    key: 'costTime',
    width: 90,
    render: (_: unknown, record: SysOperLog) => `${record.costTime} ms`,
  },
  { title: '操作时间', dataIndex: 'operTime', key: 'operTime', width: 170 },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render: (_: unknown, record: SysOperLog) =>
      h(Space, { size: 4 }, () => {
        const btns = [
          h(Button, { type: 'link', size: 'small', onClick: () => openDetail(record) }, () => '详情'),
        ]
        if (can('monitor:operlog:remove'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', danger: true, onClick: () => confirmDelete(record) },
              () => '删除',
            ),
          )
        return btns
      }),
  },
]
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <Space wrap>
        <Select
          v-model:value="query.status"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '正常', value: 0 },
            { label: '异常', value: 1 },
          ]"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button v-if="can('monitor:operlog:clean')" danger @click="confirmClean">清空</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      :scroll="{ x: 1100 }"
    />

    <Modal v-model:open="detailOpen" title="操作日志详情" :width="640" :footer="null">
      <div v-if="detail" class="detail">
        <div class="row"><span class="label">模块</span>{{ detail.title }}</div>
        <div class="row"><span class="label">方法</span>{{ detail.method }}</div>
        <div class="row"><span class="label">请求地址</span>{{ detail.operUrl }}</div>
        <div class="row"><span class="label">请求参数</span><pre>{{ detail.operParam }}</pre></div>
        <div class="row"><span class="label">返回结果</span><pre>{{ detail.jsonResult }}</pre></div>
        <div v-if="detail.status !== 0" class="row">
          <span class="label">异常信息</span><pre>{{ detail.errorMsg }}</pre>
        </div>
      </div>
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

.detail .row {
  display: flex;
  margin-bottom: 12px;
  font-size: 13px;
}

.detail .label {
  flex: 0 0 80px;
  color: rgba(0, 0, 0, 0.45);
}

.detail pre {
  flex: 1;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f6f6f6;
  padding: 8px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
}
</style>

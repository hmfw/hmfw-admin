<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from '@hmfw/ant-design'
import { DeleteOutlined, PlusOutlined } from '@hmfw/icons'
import {
  CodeBlockFeature,
  ImageFeature,
  ListFeature,
  MathFeature,
  SeparatorFeature,
  TableFeature,
  TextAlignFeature,
  TextStyleFeature,
  TiptapEditor,
  UndoRedoFeature,
} from '@hmfw/tiptap-editor'
import type { FeaturePlugin, UploadFn } from '@hmfw/tiptap-editor'
import {
  createNotice,
  deleteNotices,
  getNotice,
  getNoticeTargetOptions,
  pageNotices,
  updateNotice,
} from '@/api/notice'
import { uploadFile } from '@/api/file'
import { useAuthStore } from '@/stores/auth'
import type { NoticeRequest, OptionItem, SysNotice } from '@/types/api'

const auth = useAuthStore()
const can = (permission: string) => auth.hasPermission(permission)

const noticeTypeOptions = [
  { label: '通知', value: 1 },
  { label: '公告', value: 2 },
]
const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
]
const audienceTypeOptions = [
  { label: '全体用户', value: 'ALL' },
  { label: '指定角色', value: 'ROLE' },
  { label: '指定部门', value: 'DEPT' },
  { label: '指定用户', value: 'USER' },
]
const roleOptions = ref<OptionItem[]>([])
const deptOptions = ref<OptionItem[]>([])
const userOptions = ref<OptionItem[]>([])

async function loadTargetOptions() {
  if (roleOptions.value.length || deptOptions.value.length || userOptions.value.length) return
  const options = await getNoticeTargetOptions()
  roleOptions.value = options.roles
  deptOptions.value = options.departments
  userOptions.value = options.users
}
const editorFeatures: FeaturePlugin[] = [
  UndoRedoFeature,
  TextStyleFeature,
  SeparatorFeature,
  TextAlignFeature,
  ListFeature,
  CodeBlockFeature,
  SeparatorFeature,
  TableFeature,
  MathFeature,
  ImageFeature,
]

const loading = ref(false)
const rows = ref<SysNotice[]>([])
const total = ref(0)
const selectedRowKeys = ref<string[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  noticeTitle: '',
  noticeType: undefined as number | undefined,
  status: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const result = await pageNotices({ ...query })
    rows.value = result.records
    total.value = result.total
    selectedRowKeys.value = []
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
  query.noticeTitle = ''
  query.noticeType = undefined
  query.status = undefined
  query.pageNum = 1
  fetchData()
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
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys.map(String)
  },
}))

type NoticeForm = NoticeRequest
const emptyForm = (): NoticeForm => ({
  noticeTitle: '',
  noticeType: 1,
  noticeContent: '',
  status: 0,
  audienceType: 'ALL',
  targetIds: [],
  remark: '',
})
const form = reactive<NoticeForm>(emptyForm())
const targetOptions = computed(() => {
  if (form.audienceType === 'ROLE') return roleOptions.value
  if (form.audienceType === 'DEPT') return deptOptions.value
  if (form.audienceType === 'USER') return userOptions.value
  return []
})
const formRef = ref()
const editingId = ref<string | null>(null)
const modalOpen = ref(false)
const modalTitle = ref('')
const saving = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<SysNotice | null>(null)

const formRules = {
  noticeTitle: [
    { required: true, message: '请输入标题' },
    { max: 200, message: '标题最多 200 个字符' },
  ],
  noticeType: [{ required: true, message: '请选择类型' }],
  status: [{ required: true, message: '请选择状态' }],
}

function openCreate() {
  Object.assign(form, emptyForm())
  editingId.value = null
  modalTitle.value = '新增通知公告'
  modalOpen.value = true
  loadTargetOptions()
}

async function openEdit(row: SysNotice) {
  const notice = await getNotice(row.id)
  Object.assign(form, {
    noticeTitle: notice.noticeTitle,
    noticeType: notice.noticeType,
    noticeContent: notice.noticeContent,
    status: notice.status,
    audienceType: notice.audienceType || 'ALL',
    targetIds: notice.targetIds || [],
    remark: notice.remark ?? '',
  })
  editingId.value = row.id
  modalTitle.value = '编辑通知公告'
  modalOpen.value = true
  loadTargetOptions()
}

watch(
  () => form.audienceType,
  () => {
    if (form.audienceType === 'ALL') form.targetIds = []
  },
)

async function openDetail(row: SysNotice) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getNotice(row.id)
  } finally {
    detailLoading.value = false
  }
}

function hasEditorContent(html: string): boolean {
  const document = new DOMParser().parseFromString(html, 'text/html')
  return !!document.body.textContent?.trim() || !!document.body.querySelector('img, table, hr')
}

const uploadImage: UploadFn = async (file) => {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    throw new Error('文件类型不支持')
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('图片不能超过 10 MB')
    throw new Error('图片过大')
  }
  const uploaded = await uploadFile(file)
  return uploaded.url
}

async function save() {
  await formRef.value?.validate()
  if (!hasEditorContent(form.noticeContent)) {
    message.error('请输入公告正文')
    return
  }
  if (form.audienceType !== 'ALL' && !form.targetIds?.length) {
    message.error('请选择接收对象')
    return
  }
  saving.value = true
  try {
    const payload: NoticeRequest = {
      noticeTitle: form.noticeTitle.trim(),
      noticeType: form.noticeType,
      noticeContent: form.noticeContent,
      status: form.status,
      audienceType: form.audienceType,
      targetIds: form.targetIds,
      remark: form.remark?.trim(),
    }
    if (editingId.value) await updateNotice(editingId.value, payload)
    else await createNotice(payload)
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

function confirmDelete(ids: string[], title: string) {
  Modal.confirm({
    title,
    content: '删除后无法恢复，请确认是否继续。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteNotices(ids)
      message.success('删除成功')
      fetchData()
    },
  })
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

const columns = [
  {
    title: '标题',
    dataIndex: 'noticeTitle',
    key: 'noticeTitle',
    ellipsis: true,
    render: (text: string, record: SysNotice) =>
      can('system:notice:query')
        ? h(
            Button,
            { type: 'link', class: 'title-button', onClick: () => openDetail(record) },
            () => text,
          )
        : text,
  },
  {
    title: '类型',
    key: 'noticeType',
    width: 90,
    render: (_: unknown, record: SysNotice) =>
      h(Tag, { color: record.noticeType === 1 ? 'blue' : 'orange' }, () =>
        record.noticeType === 1 ? '通知' : '公告',
      ),
  },
  {
    title: '接收范围',
    key: 'audienceType',
    width: 110,
    render: (_: unknown, record: SysNotice) =>
      audienceTypeOptions.find((item) => item.value === record.audienceType)?.label || '全体用户',
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (_: unknown, record: SysNotice) =>
      h(Tag, { color: record.status === 1 ? 'green' : 'default' }, () =>
        record.status === 1 ? '已发布' : '草稿',
      ),
  },
  { title: '发布人', dataIndex: 'creatorName', key: 'creatorName', width: 130 },
  {
    title: '发布时间',
    key: 'publishTime',
    width: 180,
    render: (_: unknown, record: SysNotice) => formatDateTime(record.publishTime),
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
    render: (_: unknown, record: SysNotice) => formatDateTime(record.createTime),
  },
  {
    title: '操作',
    key: 'action',
    width: 190,
    fixed: 'right' as const,
    render: (_: unknown, record: SysNotice) =>
      h(Space, { size: 2 }, () => {
        const buttons = []
        if (can('system:notice:query')) {
          buttons.push(
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                onClick: () => openDetail(record),
              },
              () => '查看',
            ),
          )
        }
        if (can('system:notice:edit')) {
          buttons.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openEdit(record) },
              () => '编辑',
            ),
          )
        }
        if (can('system:notice:remove')) {
          buttons.push(
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                danger: true,
                onClick: () => confirmDelete([record.id], `确认删除「${record.noticeTitle}」？`),
              },
              () => '删除',
            ),
          )
        }
        return buttons.length ? buttons : h('span', { style: 'color:#bbb' }, '-')
      }),
  },
]
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <Space wrap>
        <Input
          v-model:value="query.noticeTitle"
          placeholder="公告标题"
          allow-clear
          style="width: 220px"
          @press-enter="onSearch"
        />
        <Select
          v-model:value="query.noticeType"
          placeholder="类型"
          allow-clear
          style="width: 120px"
          :options="noticeTypeOptions"
        />
        <Select
          v-model:value="query.status"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          :options="statusOptions"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button
        v-if="can('system:notice:add')"
        type="primary"
        :icon="PlusOutlined"
        @click="openCreate"
      >
        新增公告
      </Button>
    </div>

    <div v-if="selectedRowKeys.length && can('system:notice:remove')" class="batch-toolbar">
      <span
        >已选择 <strong>{{ selectedRowKeys.length }}</strong> 项</span
      >
      <Button
        danger
        :icon="DeleteOutlined"
        @click="confirmDelete(selectedRowKeys, `确认删除选中的 ${selectedRowKeys.length} 条公告？`)"
      >
        批量删除
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-selection="can('system:notice:remove') ? rowSelection : undefined"
      :scroll="{ x: 1050 }"
      row-key="id"
      size="middle"
    />

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="saving"
      :width="1040"
      :mask-closable="false"
      destroy-on-hidden
      ok-text="保存"
      @ok="save"
    >
      <Form ref="formRef" class="notice-form" :model="form" :rules="formRules" layout="horizontal">
        <FormItem label="标题" name="noticeTitle">
          <Input
            v-model:value="form.noticeTitle"
            placeholder="请输入公告标题"
            :maxlength="200"
            show-count
          />
        </FormItem>
        <div class="form-grid">
          <FormItem label="类型" name="noticeType">
            <Select
              v-model:value="form.noticeType"
              class="form-select"
              :options="noticeTypeOptions"
            />
          </FormItem>
          <FormItem label="状态" name="status">
            <Select v-model:value="form.status" class="form-select" :options="statusOptions" />
          </FormItem>
          <FormItem label="接收范围" name="audienceType">
            <Select
              v-model:value="form.audienceType"
              class="form-select"
              :options="audienceTypeOptions"
            />
          </FormItem>
          <FormItem v-if="form.audienceType !== 'ALL'" label="接收对象" required>
            <Select
              v-model:value="form.targetIds"
              class="form-select"
              mode="multiple"
              show-search
              :max-tag-count="3"
              :options="targetOptions"
              placeholder="请选择接收对象"
            />
          </FormItem>
        </div>
        <FormItem label="正文" required>
          <div class="editor-shell">
            <TiptapEditor
              v-model="form.noticeContent"
              placeholder="请输入通知公告正文..."
              :features="editorFeatures"
              :upload="uploadImage"
            />
          </div>
        </FormItem>
        <FormItem label="备注" name="remark">
          <Input v-model:value="form.remark" placeholder="选填，最多 255 个字符" :maxlength="255" />
        </FormItem>
      </Form>
    </Modal>

    <Modal
      v-model:open="detailOpen"
      title="通知公告详情"
      :width="960"
      :footer="null"
      destroy-on-hidden
    >
      <div v-if="detailLoading" class="detail-loading">正在加载...</div>
      <article v-else-if="detail" class="notice-detail">
        <h1>{{ detail.noticeTitle }}</h1>
        <div class="notice-meta">
          <Tag :color="detail.noticeType === 1 ? 'blue' : 'orange'">
            {{ detail.noticeType === 1 ? '通知' : '公告' }}
          </Tag>
          <Tag :color="detail.status === 1 ? 'green' : 'default'">
            {{ detail.status === 1 ? '已发布' : '草稿' }}
          </Tag>
          <span>发布人：{{ detail.creatorName || '-' }}</span>
          <span>发布时间：{{ formatDateTime(detail.publishTime) }}</span>
        </div>
        <div class="readonly-editor">
          <TiptapEditor v-model="detail.noticeContent" readonly />
        </div>
        <div v-if="detail.remark" class="notice-remark">备注：{{ detail.remark }}</div>
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
  min-height: 46px;
  margin-bottom: 12px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f5f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-width: 760px;
  gap: 16px;
}

.notice-form :deep(.ant-form-item-label) {
  flex: 0 0 72px;
  width: 72px;
}

.notice-form :deep(.ant-form-item-control) {
  min-width: 0;
}

.notice-form :deep(.form-select),
.notice-form :deep(.form-select.ant-select) {
  width: 100%;
}

.editor-shell {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.editor-shell :deep(.tiptap) {
  min-height: 360px;
  max-height: 52vh;
  overflow-y: auto;
}

.title-button {
  max-width: 100%;
  padding-inline: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-loading {
  min-height: 260px;
  display: grid;
  place-items: center;
  color: rgba(0, 0, 0, 0.45);
}

.notice-detail h1 {
  margin: 8px 0 14px;
  text-align: center;
  font-size: 24px;
  line-height: 1.4;
}

.notice-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 16px;
  color: rgba(0, 0, 0, 0.55);
  border-bottom: 1px solid #f0f0f0;
}

.readonly-editor {
  min-height: 280px;
  padding-top: 14px;
}

.readonly-editor :deep(.tiptap) {
  min-height: 260px;
  padding: 8px 16px;
}

.notice-remark {
  margin-top: 12px;
  padding: 10px 12px;
  color: rgba(0, 0, 0, 0.55);
  background: #fafafa;
  border-radius: 6px;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .notice-form :deep(.ant-form-item-label) {
    flex-basis: 64px;
    width: 64px;
  }

  .toolbar {
    align-items: stretch;
  }
}
</style>

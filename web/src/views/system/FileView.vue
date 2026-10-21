<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  Button,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  UploadDragger,
  message,
} from '@hmfw/ant-design'
import {
  AudioOutlined,
  DeleteOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileMarkdownOutlined,
  FileOutlined,
  FilePdfOutlined,
  FilePptOutlined,
  FileTextOutlined,
  FileWordOutlined,
  FileZipOutlined,
  UploadOutlined,
  VideoCameraOutlined,
} from '@hmfw/icons'
import { deleteFiles, downloadFile, listFiles, uploadFile } from '@/api/file'
import { useAuthStore } from '@/stores/auth'
import type { FileQuery, SysFile } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

/* ------------------------------ 列表 ------------------------------ */
const loading = ref(false)
const rows = ref<SysFile[]>([])
const total = ref(0)
const query = reactive<FileQuery>({
  pageNum: 1,
  pageSize: 10,
  originalName: '',
  mimeType: undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await listFiles({ ...query })
    rows.value = res.records
    total.value = res.total
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
  query.originalName = ''
  query.mimeType = undefined
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

/* ------------------------------ 上传 ------------------------------ */
const uploadOpen = ref(false)
const uploading = ref(false)
const uploadCandidate = ref<File | null>(null)
const MAX_FILE_SIZE = 50 * 1024 * 1024

function openUpload() {
  uploadCandidate.value = null
  uploadOpen.value = true
}

function onFileSelect(file: File) {
  if (file.size > MAX_FILE_SIZE) {
    message.error('文件不能超过 50 MB')
    return false
  }
  uploadCandidate.value = file
  return false
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function submitUpload() {
  if (!uploadCandidate.value) return
  uploading.value = true
  try {
    const res = await uploadFile(uploadCandidate.value)
    message.success(`上传成功：${res.originalName}`)
    uploadOpen.value = false
    await fetchData()
  } finally {
    uploading.value = false
  }
}

function closeUpload() {
  if (!uploading.value) uploadOpen.value = false
}

/* ------------------------------ 下载 ------------------------------ */
async function handleDownload(record: SysFile) {
  try {
    const blob = await downloadFile(record.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = record.originalName
    a.click()
    URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch {
    // 请求层已提示
  }
}

/* ------------------------------ 删除 ------------------------------ */
async function refreshAfterDelete(deletedCount: number) {
  if (rows.value.length <= deletedCount && query.pageNum > 1) query.pageNum--
  await fetchData()
}

function confirmDelete(record: SysFile) {
  Modal.confirm({
    title: `确认删除文件「${record.originalName}」？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteFiles([record.id])
      message.success('删除成功')
      await refreshAfterDelete(1)
    },
  })
}

const selectedRowKeys = ref<string[]>([])
const rowSelection = computed(() =>
  can('system:file:remove')
    ? {
        selectedRowKeys: selectedRowKeys.value,
        onChange: (keys: Array<string | number>) => {
          selectedRowKeys.value = keys.map(String)
        },
      }
    : undefined,
)

function confirmBatchDelete() {
  const ids = [...selectedRowKeys.value]
  if (!ids.length) return
  Modal.confirm({
    title: `确认删除已选择的 ${ids.length} 个文件？`,
    content: '文件删除后无法恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteFiles(ids)
      message.success(`已删除 ${ids.length} 个文件`)
      await refreshAfterDelete(ids.length)
    },
  })
}

const mimeTypeOptions = [
  { label: '图片', value: 'image/' },
  { label: '视频', value: 'video/' },
  { label: '音频', value: 'audio/' },
  { label: 'PDF', value: 'application/pdf' },
  { label: '文本', value: 'text/' },
]

function fileTypeInfo(mimeType?: string, fileName = '') {
  const type = mimeType ?? ''
  const extension = fileName.split('.').pop()?.toLowerCase() ?? ''

  if (type.startsWith('image/')) return { label: '图片', color: 'green', icon: FileImageOutlined }
  if (type.startsWith('video/'))
    return { label: '视频', color: 'purple', icon: VideoCameraOutlined }
  if (type.startsWith('audio/')) return { label: '音频', color: 'cyan', icon: AudioOutlined }
  if (type === 'application/pdf' || extension === 'pdf')
    return { label: 'PDF', color: 'red', icon: FilePdfOutlined }
  if (type.includes('word') || ['doc', 'docx'].includes(extension))
    return { label: 'Word', color: 'blue', icon: FileWordOutlined }
  if (
    type.includes('sheet') ||
    type.includes('excel') ||
    ['xls', 'xlsx', 'csv'].includes(extension)
  )
    return { label: 'Excel', color: 'green', icon: FileExcelOutlined }
  if (
    type.includes('presentation') ||
    type.includes('powerpoint') ||
    ['ppt', 'pptx'].includes(extension)
  )
    return { label: 'PPT', color: 'orange', icon: FilePptOutlined }
  if (
    type.includes('zip') ||
    type.includes('compressed') ||
    ['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)
  )
    return { label: '压缩包', color: 'gold', icon: FileZipOutlined }
  if (type.includes('markdown') || ['md', 'markdown'].includes(extension))
    return { label: 'Markdown', color: 'blue', icon: FileMarkdownOutlined }
  if (type.startsWith('text/')) return { label: '文本', color: 'default', icon: FileTextOutlined }
  if (!type) return { label: '未知', color: 'default', icon: FileOutlined }
  return { label: '其他', color: 'default', icon: FileOutlined }
}

/* ------------------------------ 列 ------------------------------ */
const columns = [
  {
    title: '文件名',
    dataIndex: 'originalName',
    key: 'originalName',
    render: (_: unknown, record: SysFile) => {
      const info = fileTypeInfo(record.mimeType, record.originalName)
      return h('div', { class: 'file-name-cell' }, [
        h(info.icon, { class: 'file-type-icon' }),
        h(Tooltip, { title: record.originalName }, () =>
          h('span', { class: 'file-name-text' }, record.originalName),
        ),
      ])
    },
  },
  {
    title: '大小',
    dataIndex: 'size',
    key: 'size',
    width: 100,
    render: (_: unknown, record: SysFile) => formatFileSize(record.size),
  },
  {
    title: '类型',
    dataIndex: 'mimeType',
    key: 'mimeType',
    width: 100,
    render: (_: unknown, record: SysFile) => {
      const info = fileTypeInfo(record.mimeType, record.originalName)
      return h(Tooltip, { title: record.mimeType || '未知类型' }, () =>
        h(Tag, { color: info.color }, () => info.label),
      )
    },
  },
  { title: '上传者', dataIndex: 'uploaderName', key: 'uploaderName', width: 140 },
  { title: '上传时间', dataIndex: 'uploadTime', key: 'uploadTime', width: 180 },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render: (_: unknown, record: SysFile) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:file:query'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => handleDownload(record) },
              () => '下载',
            ),
          )
        if (can('system:file:remove'))
          btns.push(
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                danger: true,
                onClick: () => confirmDelete(record),
              },
              () => '删除',
            ),
          )
        return btns.length ? btns : h('span', { style: 'color:#bbb' }, '-')
      }),
  },
]
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div class="toolbar-filters">
        <Space wrap>
          <Input
            v-model:value="query.originalName"
            placeholder="文件名"
            allow-clear
            style="width: 200px"
            @press-enter="onSearch"
          />
          <Select
            v-model:value="query.mimeType"
            placeholder="文件类型"
            allow-clear
            style="width: 140px"
            :options="mimeTypeOptions"
          />
          <Button type="primary" @click="onSearch">查询</Button>
          <Button @click="onReset">重置</Button>
        </Space>
      </div>
      <Button
        v-if="can('system:file:add')"
        type="primary"
        :icon="UploadOutlined"
        @click="openUpload"
      >
        上传文件
      </Button>
    </div>

    <div v-if="selectedRowKeys.length" class="batch-toolbar">
      <span
        >已选择 <strong>{{ selectedRowKeys.length }}</strong> 项</span
      >
      <Button danger :icon="DeleteOutlined" @click="confirmBatchDelete">批量删除</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 900 }"
      row-key="id"
      size="middle"
    />

    <Modal
      v-model:open="uploadOpen"
      title="上传文件"
      :width="560"
      :closable="!uploading"
      :keyboard="!uploading"
      :mask-closable="!uploading"
      :footer="null"
      destroy-on-hidden
      @cancel="closeUpload"
    >
      <UploadDragger
        :show-upload-list="false"
        :before-upload="onFileSelect"
        :disabled="uploading"
        accept="*"
      >
        <div class="upload-content">
          <UploadOutlined class="upload-icon" />
          <div class="upload-title">点击或拖拽文件到此处</div>
          <div class="upload-hint">单个文件不超过 50 MB</div>
        </div>
      </UploadDragger>

      <div v-if="uploadCandidate" class="selected-file">
        <component
          :is="fileTypeInfo(uploadCandidate.type, uploadCandidate.name).icon"
          class="selected-file-icon"
        />
        <div class="selected-file-meta">
          <div class="selected-file-name" :title="uploadCandidate.name">
            {{ uploadCandidate.name }}
          </div>
          <div class="selected-file-size">{{ formatFileSize(uploadCandidate.size) }}</div>
        </div>
        <Tooltip title="移除文件">
          <Button
            type="text"
            danger
            :icon="DeleteOutlined"
            :disabled="uploading"
            aria-label="移除文件"
            @click="uploadCandidate = null"
          />
        </Tooltip>
      </div>

      <div class="upload-footer">
        <Button :disabled="uploading" @click="closeUpload">取消</Button>
        <Button
          type="primary"
          :disabled="!uploadCandidate"
          :loading="uploading"
          @click="submitUpload"
        >
          开始上传
        </Button>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.toolbar-filters {
  min-width: 0;
  flex: 1;
}
.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border-left: 3px solid #1677ff;
  background: #e6f4ff;
  color: #434343;
}
.file-name-cell {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}
.file-type-icon {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 18px;
}
.file-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-content {
  padding: 18px 12px;
  text-align: center;
}
.upload-icon {
  color: #1677ff;
  font-size: 36px;
}
.upload-title {
  margin-top: 8px;
  color: #262626;
  font-weight: 500;
}
.upload-hint,
.selected-file-size {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}
.selected-file {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 54px;
  margin-top: 12px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}
.selected-file-icon {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 24px;
}
.selected-file-meta {
  min-width: 0;
  flex: 1;
}
.selected-file-name {
  overflow: hidden;
  color: #262626;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

@media (max-width: 760px) {
  .toolbar {
    flex-wrap: wrap;
  }
  .toolbar-filters {
    width: 100%;
  }
  .toolbar > :last-child {
    margin-left: auto;
  }
}
</style>

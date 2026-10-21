<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  InputPassword,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  TreeSelect,
  UploadDragger,
  message,
} from '@hmfw/ant-design'
import {
  DeleteOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  InboxOutlined,
  PlusOutlined,
  UploadOutlined,
} from '@hmfw/icons'
import {
  createUser,
  deleteUser,
  downloadTemplate,
  exportUsers,
  getUser,
  importUsers,
  pageUsers,
  resetPassword,
  updateUser,
  updateUserStatus,
} from '@/api/user'
import { getRoleOptions } from '@/api/role'
import { getDeptTree } from '@/api/dept'
import { useAuthStore } from '@/stores/auth'
import type { DeptTreeNode, OptionItem, SysUser, UserImportResult } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

/** 内置超级管理员（id === '1'）：不可停用、不可删除、不可改角色 */
const BUILTIN_ADMIN_ID = '1'
const isBuiltin = (record: SysUser) => record.id === BUILTIN_ADMIN_ID

/* ------------------------------ 列表 ------------------------------ */
const loading = ref(false)
const rows = ref<SysUser[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  status: undefined as number | undefined,
  deptId: undefined as string | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageUsers({ ...query })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const roleOptions = ref<OptionItem[]>([])
async function loadRoles() {
  roleOptions.value = await getRoleOptions()
}

const deptTree = ref<DeptTreeNode[]>([])
const deptFieldNames = { label: 'deptName', value: 'id', children: 'children' }
async function loadDepts() {
  deptTree.value = await getDeptTree()
}

onMounted(() => {
  fetchData()
  loadDepts()
  if (can('system:user:edit') || can('system:user:add')) loadRoles()
})

function onSearch() {
  query.pageNum = 1
  fetchData()
}
function onReset() {
  query.username = ''
  query.status = undefined
  query.deptId = undefined
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

async function toggleStatus(record: SysUser, checked: boolean) {
  const next = checked ? 1 : 0
  try {
    await updateUserStatus(record.id, next)
    record.status = next
    message.success(checked ? '已启用' : '已停用')
  } catch {
    // 失败时请求层已提示，开关自动回弹
  }
}

function confirmDelete(record: SysUser) {
  Modal.confirm({
    title: `确认删除用户「${record.username}」？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteUser(record.id)
      message.success('删除成功')
      fetchData()
    },
  })
}

/* --------------------------- 新增/编辑 --------------------------- */
const modalOpen = ref(false)
const modalTitle = ref('')
const editingId = ref<string | null>(null)
const saving = ref(false)
const formRef = ref()

interface UserForm {
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  deptId: string | undefined
  status: number
  remark: string
  roleIds: string[]
}
const emptyForm = (): UserForm => ({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: undefined,
  status: 1,
  remark: '',
  roleIds: [],
})
const form = reactive<UserForm>(emptyForm())
const editingBuiltin = computed(() => editingId.value === BUILTIN_ADMIN_ID)

const formRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名' }],
  password: editingId.value
    ? []
    : [
        { required: true, message: '请输入密码' },
        { min: 6, message: '密码至少 6 位' },
      ],
  email: [{ type: 'email' as const, message: '邮箱格式不正确' }],
}))

function openCreate() {
  Object.assign(form, emptyForm())
  editingId.value = null
  modalTitle.value = '新增用户'
  modalOpen.value = true
}

async function openEdit(record: SysUser) {
  const detail = await getUser(record.id)
  Object.assign(form, {
    ...emptyForm(),
    username: detail.username,
    nickname: detail.nickname ?? '',
    email: detail.email ?? '',
    phone: detail.phone ?? '',
    deptId: detail.deptId ?? undefined,
    status: detail.status,
    remark: detail.remark ?? '',
    roleIds: detail.roleIds ?? [],
  })
  editingId.value = record.id
  modalTitle.value = '编辑用户'
  modalOpen.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      const builtin = editingId.value === BUILTIN_ADMIN_ID
      await updateUser(editingId.value, {
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId,
        // 内置管理员不可停用、不可改角色：这两项不随表单提交
        status: builtin ? 1 : form.status,
        remark: form.remark,
        ...(builtin ? {} : { roleIds: form.roleIds }),
      })
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId,
        status: form.status,
        remark: form.remark,
        roleIds: form.roleIds,
      })
    }
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

/* --------------------------- 重置密码 --------------------------- */
const pwdOpen = ref(false)
const pwdSaving = ref(false)
const pwdId = ref<string | null>(null)
const pwdRef = ref()
const pwdForm = reactive({ password: '' })
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码至少 6 位' },
  ],
}

function openResetPwd(record: SysUser) {
  pwdId.value = record.id
  pwdForm.password = ''
  pwdOpen.value = true
}

async function saveResetPwd() {
  await pwdRef.value?.validate()
  pwdSaving.value = true
  try {
    await resetPassword(pwdId.value!, pwdForm.password)
    message.success('密码已重置')
    pwdOpen.value = false
  } finally {
    pwdSaving.value = false
  }
}

/* --------------------------- Excel 导入 --------------------------- */
const importOpen = ref(false)
const importUploading = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<UserImportResult | null>(null)
const MAX_IMPORT_FILE_SIZE = 5 * 1024 * 1024

const importErrorColumns = [
  { title: '行号', dataIndex: 'row', key: 'row', width: 70 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
  { title: '失败原因', dataIndex: 'message', key: 'message' },
]

function openImport() {
  importFile.value = null
  importResult.value = null
  importOpen.value = true
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function onImportFileSelect(file: File) {
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    message.error('请选择 .xlsx 格式的 Excel 文件')
    return false
  }
  if (file.size > MAX_IMPORT_FILE_SIZE) {
    message.error('文件不能超过 5 MB')
    return false
  }
  importFile.value = file
  importResult.value = null
  return false
}

function clearImportFile() {
  importFile.value = null
}

async function submitImport() {
  if (!importFile.value) return
  importUploading.value = true
  try {
    importResult.value = await importUsers(importFile.value)
    if (importResult.value.failCount === 0) {
      message.success(`成功导入 ${importResult.value.successCount} 条用户`)
    } else if (importResult.value.successCount > 0) {
      message.warning('导入完成，部分数据未能导入')
    } else {
      message.error('导入失败，请根据错误明细修改文件')
    }
    fetchData()
  } finally {
    importUploading.value = false
  }
}

function resetImport() {
  importFile.value = null
  importResult.value = null
}

function closeImport() {
  if (!importUploading.value) importOpen.value = false
}

async function downloadImportTemplate() {
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '用户导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 请求层已提示
  }
}

/* --------------------------- Excel 导出 --------------------------- */
async function handleExport() {
  try {
    const blob = await exportUsers({ ...query })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `用户列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    // 请求层已提示
  }
}

/* ------------------------------ 列 ------------------------------ */
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 120 },
  {
    title: '角色',
    key: 'roleNames',
    width: 180,
    render: (_: unknown, record: SysUser) => {
      const names = record.roleNames ?? []
      if (!names.length) return h('span', { class: 'empty-value' }, '-')
      const tags = names.slice(0, 2).map((name) => h(Tag, { key: name, color: 'blue' }, () => name))
      if (names.length > 2) {
        tags.push(
          h(Tooltip, { title: names.slice(2).join('、') }, () =>
            h(Tag, { class: 'more-roles' }, () => `+${names.length - 2}`),
          ),
        )
      }
      return h('div', { class: 'role-tags' }, tags)
    },
  },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (_: unknown, record: SysUser) => {
      const sw = h(Switch, {
        checked: record.status === 1,
        checkedChildren: '启用',
        unCheckedChildren: '停用',
        disabled: !can('system:user:edit') || isBuiltin(record),
        onChange: (checked: boolean) => toggleStatus(record, checked),
      })
      return isBuiltin(record)
        ? h(Tooltip, { title: '内置管理员账号不允许停用' }, () =>
            h('span', { style: 'display:inline-block' }, [sw]),
          )
        : sw
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: 240,
    render: (_: unknown, record: SysUser) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:user:edit'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openEdit(record) },
              () => '编辑',
            ),
          )
        if (can('system:user:resetPwd'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openResetPwd(record) },
              () => '重置密码',
            ),
          )
        if (can('system:user:remove')) {
          const delBtn = h(
            Button,
            {
              type: 'link',
              size: 'small',
              danger: true,
              disabled: isBuiltin(record),
              onClick: () => confirmDelete(record),
            },
            () => '删除',
          )
          btns.push(
            isBuiltin(record)
              ? h(Tooltip, { title: '内置管理员账号不允许删除' }, () =>
                  h('span', { style: 'display:inline-block' }, [delBtn]),
                )
              : delBtn,
          )
        }
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
            v-model:value="query.username"
            placeholder="用户名"
            allow-clear
            style="width: 180px"
            @press-enter="onSearch"
          />
          <TreeSelect
            v-model:value="query.deptId"
            placeholder="部门"
            allow-clear
            style="width: 180px"
            :tree-data="deptTree as any"
            :field-names="deptFieldNames"
            tree-default-expand-all
          />
          <Select
            v-model:value="query.status"
            placeholder="状态"
            allow-clear
            style="width: 140px"
            :options="[
              { label: '启用', value: 1 },
              { label: '停用', value: 0 },
            ]"
          />
          <Button type="primary" @click="onSearch">查询</Button>
          <Button @click="onReset">重置</Button>
        </Space>
      </div>
      <Space class="toolbar-actions">
        <Button v-if="can('system:user:import')" :icon="UploadOutlined" @click="openImport">
          导入
        </Button>
        <Button v-if="can('system:user:export')" :icon="DownloadOutlined" @click="handleExport">
          导出
        </Button>
        <Button
          v-if="can('system:user:add')"
          type="primary"
          :icon="PlusOutlined"
          @click="openCreate"
        >
          新增用户
        </Button>
      </Space>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
    />

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="saving"
      :width="520"
      destroy-on-hidden
      @ok="save"
    >
      <Form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <FormItem label="用户名" name="username">
          <Input v-model:value="form.username" :disabled="!!editingId" placeholder="请输入用户名" />
        </FormItem>
        <FormItem v-if="!editingId" label="密码" name="password">
          <InputPassword v-model:value="form.password" placeholder="请输入初始密码" />
        </FormItem>
        <FormItem label="昵称" name="nickname">
          <Input v-model:value="form.nickname" placeholder="请输入昵称" />
        </FormItem>
        <FormItem label="邮箱" name="email">
          <Input v-model:value="form.email" placeholder="请输入邮箱" />
        </FormItem>
        <FormItem label="手机号" name="phone">
          <Input v-model:value="form.phone" placeholder="请输入手机号" />
        </FormItem>
        <FormItem label="部门" name="deptId">
          <TreeSelect
            v-model:value="form.deptId"
            placeholder="请选择部门"
            allow-clear
            :tree-data="deptTree as any"
            :field-names="deptFieldNames"
            tree-default-expand-all
          />
        </FormItem>
        <FormItem label="角色" name="roleIds">
          <Select
            v-model:value="form.roleIds"
            mode="multiple"
            placeholder="请选择角色"
            :options="roleOptions"
            :disabled="editingBuiltin"
            allow-clear
          />
          <div v-if="editingBuiltin" style="color: #999; font-size: 12px; margin-top: 4px">
            内置管理员账号的角色不可修改
          </div>
        </FormItem>
        <FormItem label="状态" name="status">
          <Switch
            :checked="form.status === 1"
            :disabled="editingBuiltin"
            checked-children="启用"
            un-checked-children="停用"
            @change="
              (c: boolean) => {
                form.status = c ? 1 : 0
              }
            "
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Input v-model:value="form.remark" placeholder="备注" />
        </FormItem>
      </Form>
    </Modal>

    <Modal
      v-model:open="pwdOpen"
      title="重置密码"
      :confirm-loading="pwdSaving"
      :width="420"
      destroy-on-hidden
      @ok="saveResetPwd"
    >
      <Form ref="pwdRef" :model="pwdForm" :rules="pwdRules" layout="vertical">
        <FormItem label="新密码" name="password">
          <InputPassword v-model:value="pwdForm.password" placeholder="请输入新密码" />
        </FormItem>
      </Form>
    </Modal>

    <Modal
      v-model:open="importOpen"
      title="导入用户"
      :width="640"
      destroy-on-hidden
      :closable="!importUploading"
      :keyboard="!importUploading"
      :mask-closable="!importUploading"
      :footer="null"
      @cancel="closeImport"
    >
      <template v-if="!importResult">
        <div class="import-toolbar">
          <div class="import-note">请使用导入模板，新用户默认密码为 123456</div>
          <Button
            :icon="DownloadOutlined"
            :disabled="importUploading"
            @click="downloadImportTemplate"
          >
            下载模板
          </Button>
        </div>
        <UploadDragger
          :show-upload-list="false"
          :before-upload="onImportFileSelect"
          :disabled="importUploading"
          accept=".xlsx"
        >
          <div class="upload-content">
            <InboxOutlined class="upload-icon" />
            <div class="upload-title">点击或拖拽 Excel 文件到此处</div>
            <div class="upload-hint">仅支持 .xlsx，文件不超过 5 MB</div>
          </div>
        </UploadDragger>

        <div v-if="importFile" class="selected-file">
          <FileExcelOutlined class="file-icon" />
          <div class="file-meta">
            <div class="file-name" :title="importFile.name">{{ importFile.name }}</div>
            <div class="file-size">{{ formatFileSize(importFile.size) }}</div>
          </div>
          <Tooltip title="移除文件">
            <Button
              type="text"
              danger
              :icon="DeleteOutlined"
              :disabled="importUploading"
              aria-label="移除文件"
              @click="clearImportFile"
            />
          </Tooltip>
        </div>

        <div class="import-footer">
          <Button :disabled="importUploading" @click="closeImport">取消</Button>
          <Button
            type="primary"
            :disabled="!importFile"
            :loading="importUploading"
            @click="submitImport"
          >
            开始导入
          </Button>
        </div>
      </template>

      <template v-else>
        <div
          class="import-result-summary"
          :class="{
            'is-success': importResult.failCount === 0,
            'is-warning': importResult.successCount > 0 && importResult.failCount > 0,
            'is-error': importResult.successCount === 0,
          }"
        >
          <div class="result-title">导入完成</div>
          <div class="import-summary">
            <span>成功 <strong>{{ importResult.successCount }}</strong> 条</span>
            <span>失败 <strong>{{ importResult.failCount }}</strong> 条</span>
          </div>
        </div>

        <div class="import-summary">
          <span v-if="importResult.errors.length" class="error-title">失败明细</span>
        </div>
        <Table
          v-if="importResult.errors.length"
          class="import-error-table"
          :columns="importErrorColumns"
          :data-source="importResult.errors"
          :pagination="false"
          :scroll="{ y: 240 }"
          row-key="row"
          size="small"
        />

        <div class="import-footer">
          <Button @click="resetImport">重新导入</Button>
          <Button type="primary" @click="closeImport">关闭</Button>
        </div>
      </template>
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
.toolbar-actions {
  flex: 0 0 auto;
}

@media (max-width: 960px) {
  .toolbar {
    flex-wrap: wrap;
  }
  .toolbar-filters,
  .toolbar-actions {
    width: 100%;
  }
  .toolbar-actions {
    display: flex;
    justify-content: flex-end;
  }
}

.role-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.role-tags :deep(.ant-tag) {
  margin-inline-end: 0;
}
.empty-value {
  color: #999;
}

.import-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.import-note {
  color: #666;
  font-size: 13px;
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
.upload-hint {
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
.file-icon {
  flex: 0 0 auto;
  color: #389e0d;
  font-size: 24px;
}
.file-meta {
  min-width: 0;
  flex: 1;
}
.file-name {
  overflow: hidden;
  color: #262626;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size {
  margin-top: 2px;
  color: #8c8c8c;
  font-size: 12px;
}
.import-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.import-result-summary {
  padding: 14px 16px;
  border-left: 3px solid;
  background: #f6ffed;
}
.import-result-summary.is-success {
  border-color: #52c41a;
}
.import-result-summary.is-warning {
  border-color: #faad14;
  background: #fffbe6;
}
.import-result-summary.is-error {
  border-color: #ff4d4f;
  background: #fff2f0;
}
.result-title {
  color: #262626;
  font-weight: 600;
}
.import-summary {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  color: #595959;
}
.error-title {
  margin-top: 10px;
  color: #262626;
  font-weight: 600;
}
.import-error-table {
  margin-top: 10px;
}
</style>

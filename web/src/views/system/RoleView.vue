<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tree,
  message,
} from '@hmfw/ant-design'
import { PlusOutlined } from '@hmfw/icons'
import { assignRoleMenus, createRole, deleteRole, getRole, pageRoles, updateRole } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import { useAuthStore } from '@/stores/auth'
import type { DeptTreeNode, MenuTreeNode, SysRole } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

/* ------------------------------ 列表 ------------------------------ */
const loading = ref(false)
const rows = ref<SysRole[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  status: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageRoles({ ...query })
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
  query.roleName = ''
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

function confirmDelete(record: SysRole) {
  Modal.confirm({
    title: `确认删除角色「${record.roleName}」？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteRole(record.id)
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

interface RoleForm {
  roleName: string
  roleKey: string
  sort: number
  dataScope: number
  status: number
  remark: string
}
const emptyForm = (): RoleForm => ({
  roleName: '',
  roleKey: '',
  sort: 0,
  dataScope: 1,
  status: 1,
  remark: '',
})
const form = reactive<RoleForm>(emptyForm())
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleKey: [{ required: true, message: '请输入权限字符' }],
}

const dataScopeOptions = [
  { label: '全部数据', value: 1 },
  { label: '自定义部门', value: 2 },
  { label: '本部门', value: 3 },
  { label: '本部门及以下', value: 4 },
  { label: '仅本人', value: 5 },
]

/* 自定义数据范围（dataScope=2）用的部门树 */
const deptTree = ref<DeptTreeNode[]>([])
const deptFieldNames = { title: 'deptName', key: 'id', children: 'children' }
const deptCheckedKeys = ref<string[]>([])
const deptHalfCheckedKeys = ref<string[]>([])
async function ensureDeptTree() {
  if (!deptTree.value.length) deptTree.value = await getDeptTree()
}
function onDeptCheck(keys: unknown, info: { halfCheckedKeys?: (string | number)[] }) {
  deptCheckedKeys.value = keys as string[]
  deptHalfCheckedKeys.value = (info.halfCheckedKeys ?? []) as string[]
}

async function openCreate() {
  Object.assign(form, emptyForm())
  deptCheckedKeys.value = []
  deptHalfCheckedKeys.value = []
  await ensureDeptTree()
  editingId.value = null
  modalTitle.value = '新增角色'
  modalOpen.value = true
}
async function openEdit(record: SysRole) {
  await ensureDeptTree()
  const detail = await getRole(record.id)
  Object.assign(form, {
    roleName: detail.roleName,
    roleKey: detail.roleKey,
    sort: detail.sort ?? 0,
    dataScope: detail.dataScope ?? 1,
    status: detail.status,
    remark: detail.remark ?? '',
  })
  deptCheckedKeys.value = detail.deptIds ?? []
  deptHalfCheckedKeys.value = []
  editingId.value = record.id
  modalTitle.value = '编辑角色'
  modalOpen.value = true
}
async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const deptIds =
      form.dataScope === 2
        ? Array.from(new Set([...deptCheckedKeys.value, ...deptHalfCheckedKeys.value]))
        : []
    const payload = { ...form, deptIds }
    if (editingId.value) await updateRole(editingId.value, payload)
    else await createRole(payload)
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

/* --------------------------- 分配菜单 --------------------------- */
const menuModalOpen = ref(false)
const menuSaving = ref(false)
const menuTree = ref<MenuTreeNode[]>([])
const assignRoleId = ref<string | null>(null)
const assignRoleName = ref('')
const checkedKeys = ref<string[]>([])
const halfCheckedKeys = ref<string[]>([])
const treeFieldNames = { title: 'menuName', key: 'id', children: 'children' }

/** 收集所有「含子节点」的目录 id，用于回显时只勾选叶子，让 Tree 自动推导父级半选 */
function collectParentIds(nodes: MenuTreeNode[], set: Set<string>) {
  for (const n of nodes) {
    if (n.children && n.children.length) {
      set.add(n.id)
      collectParentIds(n.children, set)
    }
  }
}

async function openAssignMenus(record: SysRole) {
  assignRoleId.value = record.id
  assignRoleName.value = record.roleName
  if (!menuTree.value.length) menuTree.value = await getMenuTree()
  const detail = await getRole(record.id)
  const parents = new Set<string>()
  collectParentIds(menuTree.value, parents)
  checkedKeys.value = (detail.menuIds ?? []).filter((id) => !parents.has(id))
  halfCheckedKeys.value = []
  menuModalOpen.value = true
}

function onCheck(keys: unknown, info: { halfCheckedKeys?: (string | number)[] }) {
  checkedKeys.value = keys as string[]
  halfCheckedKeys.value = (info.halfCheckedKeys ?? []) as string[]
}

async function saveMenus() {
  menuSaving.value = true
  try {
    const ids = Array.from(new Set([...checkedKeys.value, ...halfCheckedKeys.value]))
    await assignRoleMenus(assignRoleId.value!, ids)
    message.success('分配成功')
    menuModalOpen.value = false
  } finally {
    menuSaving.value = false
  }
}

/* ------------------------------ 列 ------------------------------ */
const columns = [
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName' },
  { title: '权限字符', dataIndex: 'roleKey', key: 'roleKey' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (_: unknown, record: SysRole) =>
      record.status === 1
        ? h(Tag, { color: 'success' }, () => '启用')
        : h(Tag, { color: 'default' }, () => '停用'),
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: 220,
    render: (_: unknown, record: SysRole) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:role:edit'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openEdit(record) },
              () => '编辑',
            ),
          )
        if (can('system:role:edit'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openAssignMenus(record) },
              () => '分配菜单',
            ),
          )
        if (can('system:role:remove'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', danger: true, onClick: () => confirmDelete(record) },
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
      <Space wrap>
        <Input
          v-model:value="query.roleName"
          placeholder="角色名称"
          allow-clear
          style="width: 180px"
          @press-enter="onSearch"
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
      <Button v-if="can('system:role:add')" type="primary" :icon="PlusOutlined" @click="openCreate">
        新增角色
      </Button>
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
      :width="480"
      destroy-on-hidden
      @ok="save"
    >
      <Form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <FormItem label="角色名称" name="roleName">
          <Input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </FormItem>
        <FormItem label="权限字符" name="roleKey">
          <Input v-model:value="form.roleKey" placeholder="如 admin、viewer" />
        </FormItem>
        <FormItem label="排序" name="sort">
          <InputNumber v-model:value="form.sort" :min="0" style="width: 100%" />
        </FormItem>
        <FormItem label="数据范围" name="dataScope">
          <Select v-model:value="form.dataScope" :options="dataScopeOptions" />
        </FormItem>
        <FormItem v-if="form.dataScope === 2" label="自定义部门" name="deptIds">
          <Tree
            :tree-data="deptTree as any"
            :field-names="deptFieldNames"
            :checked-keys="deptCheckedKeys"
            checkable
            :height="260"
            default-expand-all
            @check="onDeptCheck"
          />
        </FormItem>
        <FormItem label="状态" name="status">
          <Switch
            :checked="form.status === 1"
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
      v-model:open="menuModalOpen"
      :title="`分配菜单权限 - ${assignRoleName}`"
      :confirm-loading="menuSaving"
      :width="420"
      destroy-on-hidden
      @ok="saveMenus"
    >
      <Tree
        :tree-data="menuTree as any"
        :field-names="treeFieldNames"
        :checked-keys="checkedKeys"
        checkable
        :height="420"
        default-expand-all
        @check="onCheck"
      />
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
</style>

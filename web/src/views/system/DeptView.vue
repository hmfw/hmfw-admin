<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Space,
  Switch,
  Table,
  Tag,
  TreeSelect,
  message,
} from '@hmfw/ant-design'
import { PlusOutlined } from '@hmfw/icons'
import { createDept, deleteDept, getDept, getDeptTree, updateDept } from '@/api/dept'
import { useAuthStore } from '@/stores/auth'
import type { DeptRequest, DeptTreeNode } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

const loading = ref(false)
const tree = ref<DeptTreeNode[]>([])

function pruneEmptyChildren(nodes: DeptTreeNode[]): DeptTreeNode[] {
  return nodes.map((n) => {
    const children = n.children?.length ? pruneEmptyChildren(n.children) : undefined
    return { ...n, children } as DeptTreeNode
  })
}

async function fetchData() {
  loading.value = true
  try {
    tree.value = pruneEmptyChildren(await getDeptTree())
  } finally {
    loading.value = false
  }
}
onMounted(fetchData)

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const parentTreeData = computed<any[]>(() => [
  { id: '0', deptName: '顶级部门', children: tree.value },
])
const treeSelectFieldNames = { label: 'deptName', value: 'id', children: 'children' }

function confirmDelete(record: DeptTreeNode) {
  Modal.confirm({
    title: `确认删除部门「${record.deptName}」？`,
    content: record.children?.length ? '该部门存在子部门，请先删除子部门。' : undefined,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteDept(record.id)
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

const emptyForm = (): DeptRequest => ({
  parentId: '0',
  deptName: '',
  leader: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1,
})
const form = reactive<DeptRequest>(emptyForm())
const formRules = {
  deptName: [{ required: true, message: '请输入部门名称' }],
  email: [{ type: 'email' as const, message: '邮箱格式不正确' }],
}

function openCreate(parentId = '0') {
  Object.assign(form, emptyForm(), { parentId })
  editingId.value = null
  modalTitle.value = '新增部门'
  modalOpen.value = true
}

async function openEdit(record: DeptTreeNode) {
  const d = await getDept(record.id)
  Object.assign(form, {
    parentId: d.parentId ?? '0',
    deptName: d.deptName,
    leader: d.leader ?? '',
    phone: d.phone ?? '',
    email: d.email ?? '',
    sort: d.sort ?? 0,
    status: d.status ?? 1,
  })
  editingId.value = record.id
  modalTitle.value = '编辑部门'
  modalOpen.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) await updateDept(editingId.value, { ...form })
    else await createDept({ ...form })
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const columns = [
  {
    title: '部门名称',
    key: 'deptName',
    render: (_: unknown, record: DeptTreeNode) => h('span', record.deptName),
  },
  { title: '负责人', dataIndex: 'leader', key: 'leader', width: 120 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (_: unknown, record: DeptTreeNode) =>
      record.status === 0
        ? h(Tag, { color: 'default' }, () => '停用')
        : h(Tag, { color: 'success' }, () => '正常'),
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    render: (_: unknown, record: DeptTreeNode) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:dept:add'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openCreate(record.id) },
              () => '新增',
            ),
          )
        if (can('system:dept:edit'))
          btns.push(
            h(Button, { type: 'link', size: 'small', onClick: () => openEdit(record) }, () => '编辑'),
          )
        if (can('system:dept:remove'))
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
      <span class="hint">组织架构按树形维护，部门可挂到用户上做数据范围</span>
      <Button
        v-if="can('system:dept:add')"
        type="primary"
        :icon="PlusOutlined"
        @click="openCreate('0')"
      >
        新增部门
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="tree"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
      children-column-name="children"
      default-expand-all-rows
      :indent-size="22"
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
        <FormItem label="上级部门" name="parentId">
          <TreeSelect
            v-model:value="form.parentId"
            :tree-data="parentTreeData"
            :field-names="treeSelectFieldNames"
            tree-default-expand-all
            placeholder="选择上级部门"
          />
        </FormItem>
        <FormItem label="部门名称" name="deptName">
          <Input v-model:value="form.deptName" placeholder="请输入部门名称" />
        </FormItem>
        <FormItem label="负责人" name="leader">
          <Input v-model:value="form.leader" placeholder="请输入负责人" />
        </FormItem>
        <FormItem label="联系电话" name="phone">
          <Input v-model:value="form.phone" placeholder="请输入联系电话" />
        </FormItem>
        <FormItem label="邮箱" name="email">
          <Input v-model:value="form.email" placeholder="请输入邮箱" />
        </FormItem>
        <FormItem label="排序" name="sort">
          <InputNumber v-model:value="form.sort" :min="0" style="width: 100%" />
        </FormItem>
        <FormItem label="状态" name="status">
          <Switch
            :checked="form.status === 1"
            checked-children="正常"
            un-checked-children="停用"
            @change="
              (c: boolean) => {
                form.status = c ? 1 : 0
              }
            "
          />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.hint {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}
</style>

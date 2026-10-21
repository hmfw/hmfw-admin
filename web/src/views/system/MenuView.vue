<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  RadioGroup,
  Space,
  Switch,
  Table,
  Tag,
  TreeSelect,
  message,
} from '@hmfw/ant-design'
import { PlusOutlined } from '@hmfw/icons'
import { createMenu, deleteMenu, getMenu, getMenuTree, updateMenu } from '@/api/menu'
import { resolveMenuIcon } from '@/utils/icon'
import { useAuthStore } from '@/stores/auth'
import type { MenuRequest, MenuTreeNode } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

const typeMeta: Record<string, { text: string; color: string }> = {
  M: { text: '目录', color: 'blue' },
  C: { text: '菜单', color: 'green' },
  F: { text: '按钮', color: 'orange' },
}

/* ------------------------------ 列表 ------------------------------ */
const loading = ref(false)
const tree = ref<MenuTreeNode[]>([])

/** 去掉空的 children，避免表格为叶子节点渲染多余的展开箭头 */
function pruneEmptyChildren(nodes: MenuTreeNode[]): MenuTreeNode[] {
  return nodes.map((n) => {
    const children = n.children?.length ? pruneEmptyChildren(n.children) : undefined
    return { ...n, children } as MenuTreeNode
  })
}

async function fetchData() {
  loading.value = true
  try {
    tree.value = pruneEmptyChildren(await getMenuTree())
  } finally {
    loading.value = false
  }
}
onMounted(fetchData)

/** 供 TreeSelect 选父级：顶层加一个「顶级目录」(id=0) */
// TreeSelect 的 treeData 要求 value/label/key 字段，这里用 fieldNames 映射，故放开 any
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const parentTreeData = computed<any[]>(() => [
  { id: '0', menuName: '顶级目录', children: tree.value },
])
const treeSelectFieldNames = { label: 'menuName', value: 'id', children: 'children' }

function confirmDelete(record: MenuTreeNode) {
  Modal.confirm({
    title: `确认删除菜单「${record.menuName}」？`,
    content: record.children?.length ? '该节点存在子菜单，请先删除子菜单。' : undefined,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteMenu(record.id)
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

const emptyForm = (): MenuRequest => ({
  parentId: '0',
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
})
const form = reactive<MenuRequest>(emptyForm())
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称' }],
}

function openCreate(parentId = '0') {
  Object.assign(form, emptyForm(), { parentId })
  editingId.value = null
  modalTitle.value = '新增菜单'
  modalOpen.value = true
}

async function openEdit(record: MenuTreeNode) {
  const d = await getMenu(record.id)
  Object.assign(form, {
    parentId: d.parentId ?? '0',
    menuName: d.menuName,
    menuType: d.menuType,
    path: d.path ?? '',
    component: d.component ?? '',
    perms: d.perms ?? '',
    icon: d.icon ?? '',
    sort: d.sort ?? 0,
    visible: d.visible ?? 1,
    status: d.status ?? 1,
  })
  editingId.value = record.id
  modalTitle.value = '编辑菜单'
  modalOpen.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) await updateMenu(editingId.value, { ...form })
    else await createMenu({ ...form })
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 列 ------------------------------ */
const columns = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
    render: (_: unknown, record: MenuTreeNode) =>
      h(Space, { size: 6 }, () => [h(resolveMenuIcon(record.icon)), record.menuName]),
  },
  {
    title: '类型',
    key: 'menuType',
    width: 80,
    render: (_: unknown, record: MenuTreeNode) =>
      h(Tag, { color: typeMeta[record.menuType]?.color }, () => typeMeta[record.menuType]?.text),
  },
  { title: '权限标识', dataIndex: 'perms', key: 'perms' },
  { title: '路由路径', dataIndex: 'path', key: 'path' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (_: unknown, record: MenuTreeNode) =>
      record.status === 0
        ? h(Tag, { color: 'default' }, () => '停用')
        : h(Tag, { color: 'success' }, () => '正常'),
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    render: (_: unknown, record: MenuTreeNode) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:menu:add') && record.menuType !== 'F')
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openCreate(record.id) },
              () => '新增',
            ),
          )
        if (can('system:menu:edit'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', onClick: () => openEdit(record) },
              () => '编辑',
            ),
          )
        if (can('system:menu:remove'))
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
      <span class="hint">菜单 / 目录 / 按钮权限统一在此维护</span>
      <Button
        v-if="can('system:menu:add')"
        type="primary"
        :icon="PlusOutlined"
        @click="openCreate('0')"
      >
        新增菜单
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="tree"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
      default-expand-all-rows
    />

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="saving"
      :width="560"
      destroy-on-hidden
      @ok="save"
    >
      <Form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <FormItem label="上级菜单" name="parentId">
          <TreeSelect
            v-model:value="form.parentId"
            :tree-data="parentTreeData"
            :field-names="treeSelectFieldNames"
            tree-default-expand-all
            placeholder="选择上级菜单"
          />
        </FormItem>
        <FormItem label="菜单类型" name="menuType">
          <RadioGroup
            v-model:value="form.menuType"
            option-type="button"
            :options="[
              { label: '目录', value: 'M' },
              { label: '菜单', value: 'C' },
              { label: '按钮', value: 'F' },
            ]"
          />
        </FormItem>
        <FormItem label="菜单名称" name="menuName">
          <Input v-model:value="form.menuName" placeholder="请输入菜单名称" />
        </FormItem>
        <FormItem v-if="form.menuType !== 'F'" label="路由路径" name="path">
          <Input v-model:value="form.path" placeholder="如 /system/user" />
        </FormItem>
        <FormItem v-if="form.menuType === 'C'" label="组件路径" name="component">
          <Input v-model:value="form.component" placeholder="如 system/UserView" />
        </FormItem>
        <FormItem v-if="form.menuType !== 'M'" label="权限标识" name="perms">
          <Input v-model:value="form.perms" placeholder="如 system:user:list" />
        </FormItem>
        <FormItem v-if="form.menuType !== 'F'" label="图标" name="icon">
          <Input v-model:value="form.icon" placeholder="如 user、setting、peoples" />
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
        <FormItem v-if="form.menuType !== 'F'" label="显示" name="visible">
          <Switch
            :checked="form.visible === 1"
            checked-children="显示"
            un-checked-children="隐藏"
            @change="
              (c: boolean) => {
                form.visible = c ? 1 : 0
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

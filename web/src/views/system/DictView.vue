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
  message,
} from '@hmfw/ant-design'
import { DeleteOutlined, PlusOutlined } from '@hmfw/icons'
import { deleteDictType, pageDictData, pageDictTypes, saveDictType } from '@/api/dict'
import { useAuthStore } from '@/stores/auth'
import type { DictDataItem, DictTypeSaveRequest, SysDictData, SysDictType } from '@/types/api'

const auth = useAuthStore()
const can = (p: string) => auth.hasPermission(p)

const loading = ref(false)
const rows = ref<SysDictType[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, dictName: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await pageDictTypes({ ...query })
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
  query.dictName = ''
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

/* ============================ 新增 / 编辑弹窗 ============================ */
const modalOpen = ref(false)
const modalTitle = ref('')
const editingId = ref<string | null>(null)
const saving = ref(false)
const itemsLoading = ref(false)
const typeFormRef = ref()

type FormState = {
  dictName: string
  dictType: string
  status: number
  remark: string
  dataList: DictDataItem[]
}
const emptyForm = (): FormState => ({
  dictName: '',
  dictType: '',
  status: 1,
  remark: '',
  dataList: [],
})
const form = reactive<FormState>(emptyForm())
const formRules = {
  dictName: [{ required: true, message: '请输入字典名称' }],
  dictType: [{ required: true, message: '请输入字典类型' }],
}

function newItem(): DictDataItem {
  return { dictLabel: '', dictValue: '', sort: form.dataList.length + 1, status: 1, remark: '' }
}
function addItem() {
  form.dataList.push(newItem())
}
function removeItem(idx: number) {
  form.dataList.splice(idx, 1)
}

function openCreate() {
  Object.assign(form, emptyForm())
  form.dataList = [newItem()]
  editingId.value = null
  modalTitle.value = '新增字典'
  modalOpen.value = true
}

async function openEdit(row: SysDictType) {
  Object.assign(form, {
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status,
    remark: row.remark ?? '',
    dataList: [],
  })
  editingId.value = row.id
  modalTitle.value = '编辑字典'
  modalOpen.value = true
  // 载入该类型下全部字典项（含停用），作为可编辑最终态
  itemsLoading.value = true
  try {
    const res = await pageDictData({ pageNum: 1, pageSize: 500, dictType: row.dictType })
    form.dataList = res.records.map((d: SysDictData) => ({
      id: d.id,
      dictLabel: d.dictLabel,
      dictValue: d.dictValue,
      sort: d.sort ?? 0,
      status: d.status,
      remark: d.remark ?? '',
    }))
  } finally {
    itemsLoading.value = false
  }
}

async function save() {
  await typeFormRef.value?.validate()
  // 字典项逐行校验（标签/键值必填）
  for (let i = 0; i < form.dataList.length; i++) {
    const it = form.dataList[i]
    if (!it.dictLabel.trim() || !it.dictValue.trim()) {
      message.error(`第 ${i + 1} 行字典项的标签和键值不能为空`)
      return
    }
  }
  const payload: DictTypeSaveRequest = {
    dictName: form.dictName,
    dictType: form.dictType,
    status: form.status,
    remark: form.remark,
    dataList: form.dataList.map((it, idx) => ({ ...it, sort: it.sort ?? idx + 1 })),
  }
  if (editingId.value) payload.id = editingId.value
  saving.value = true
  try {
    await saveDictType(payload)
    message.success('保存成功')
    modalOpen.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

function confirmDelete(row: SysDictType) {
  Modal.confirm({
    title: `确认删除字典「${row.dictName}」？`,
    content: '将连同该类型下的字典项一并删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteDictType(row.id)
      message.success('删除成功')
      fetchData()
    },
  })
}

const columns = [
  { title: '字典名称', dataIndex: 'dictName', key: 'dictName' },
  { title: '字典类型', dataIndex: 'dictType', key: 'dictType' },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (_: unknown, row: SysDictType) =>
      row.status === 1
        ? h(Tag, { color: 'success' }, () => '启用')
        : h(Tag, { color: 'default' }, () => '停用'),
  },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  {
    title: '操作',
    key: 'action',
    width: 140,
    render: (_: unknown, row: SysDictType) =>
      h(Space, { size: 4 }, () => {
        const btns = []
        if (can('system:dict:edit'))
          btns.push(
            h(Button, { type: 'link', size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
          )
        if (can('system:dict:remove'))
          btns.push(
            h(
              Button,
              { type: 'link', size: 'small', danger: true, onClick: () => confirmDelete(row) },
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
          v-model:value="query.dictName"
          placeholder="字典名称"
          allow-clear
          style="width: 160px"
          @press-enter="onSearch"
        />
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="onReset">重置</Button>
      </Space>
      <Button v-if="can('system:dict:add')" type="primary" :icon="PlusOutlined" @click="openCreate">
        新增字典
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
      :width="760"
      destroy-on-hidden
      @ok="save"
    >
      <Form ref="typeFormRef" :model="form" :rules="formRules" layout="vertical">
        <div class="type-grid">
          <FormItem label="字典名称" name="dictName">
            <Input v-model:value="form.dictName" placeholder="如 用户性别" />
          </FormItem>
          <FormItem label="字典类型" name="dictType">
            <Input v-model:value="form.dictType" placeholder="如 sys_user_sex" />
          </FormItem>
          <FormItem label="状态" name="status">
            <Switch
              :checked="form.status === 1"
              checked-children="启用"
              un-checked-children="停用"
              @change="(c: boolean) => { form.status = c ? 1 : 0 }"
            />
          </FormItem>
          <FormItem label="备注" name="remark">
            <Input v-model:value="form.remark" placeholder="备注" />
          </FormItem>
        </div>
      </Form>

      <div class="items">
        <div class="items-bar">
          <span class="items-title">字典项</span>
          <Button type="dashed" size="small" :icon="PlusOutlined" @click="addItem">添加字典项</Button>
        </div>

        <div class="item-head">
          <span>标签</span>
          <span>键值</span>
          <span>排序</span>
          <span>状态</span>
          <span></span>
        </div>

        <div v-if="itemsLoading" class="items-empty">加载中…</div>
        <div v-else-if="!form.dataList.length" class="items-empty">
          暂无字典项，点击「添加字典项」新增
        </div>
        <div v-for="(it, idx) in form.dataList" :key="idx" class="item-row">
          <Input v-model:value="it.dictLabel" placeholder="展示文本，如 男" />
          <Input v-model:value="it.dictValue" placeholder="存储值，如 0" />
          <InputNumber v-model:value="it.sort" :min="0" style="width: 100%" />
          <Switch
            :checked="it.status === 1"
            checked-children="启用"
            un-checked-children="停用"
            class="col-status"
            @change="(c: boolean) => { it.status = c ? 1 : 0 }"
          />
          <Button
            type="text"
            danger
            :icon="DeleteOutlined"
            @click="removeItem(idx)"
            aria-label="删除此行"
          />
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

.type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.items {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.items-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.items-title {
  font-weight: 600;
}

/* 标签 / 键值 / 排序 / 状态 / 删除 */
.item-head,
.item-row {
  display: grid;
  grid-template-columns: 1fr 1fr 96px 96px 40px;
  gap: 8px;
  align-items: center;
}
.item-head {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  margin-bottom: 6px;
}
.item-row {
  margin-bottom: 8px;
}
/* 状态列是 96px 的网格轨道，Switch 默认会被拉伸填满；按内容靠左取自然宽度 */
.item-row .col-status {
  justify-self: start;
}
.items-empty {
  color: rgba(0, 0, 0, 0.35);
  font-size: 13px;
  padding: 12px 0;
  text-align: center;
}
</style>

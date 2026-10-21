<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Button, Modal, Tooltip, message } from '@hmfw/ant-design'
import { ZoomInOutlined, ZoomOutOutlined } from '@hmfw/icons'
import Cropper from 'cropperjs'

const props = defineProps<{
  open: boolean
  file: File | null
  busy?: boolean
}>()

const emit = defineEmits<{
  cancel: []
  confirm: [blob: Blob]
}>()

const CROPPER_TEMPLATE = `
  <cropper-canvas background>
    <cropper-image rotatable scalable translatable></cropper-image>
    <cropper-shade hidden></cropper-shade>
    <cropper-handle action="move" plain></cropper-handle>
    <cropper-selection
      initial-aspect-ratio="1"
      aspect-ratio="1"
      initial-coverage="0.72"
      movable
      resizable
      zoomable
    >
      <cropper-grid role="grid" bordered covered></cropper-grid>
      <cropper-crosshair centered></cropper-crosshair>
      <cropper-handle action="move" theme-color="rgba(255, 255, 255, 0.35)"></cropper-handle>
      <cropper-handle action="n-resize"></cropper-handle>
      <cropper-handle action="e-resize"></cropper-handle>
      <cropper-handle action="s-resize"></cropper-handle>
      <cropper-handle action="w-resize"></cropper-handle>
      <cropper-handle action="ne-resize"></cropper-handle>
      <cropper-handle action="nw-resize"></cropper-handle>
      <cropper-handle action="se-resize"></cropper-handle>
      <cropper-handle action="sw-resize"></cropper-handle>
    </cropper-selection>
  </cropper-canvas>
`

const imageRef = ref<HTMLImageElement>()
const stageRef = ref<HTMLElement>()
const previewUrl = ref('')
const exporting = ref(false)
let cropper: Cropper | null = null

function destroyCropper() {
  cropper?.destroy()
  cropper = null
}

function releasePreview() {
  destroyCropper()
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

watch(
  [() => props.open, () => props.file],
  ([open, file]) => {
    releasePreview()
    if (open && file) previewUrl.value = URL.createObjectURL(file)
  },
  { immediate: true },
)

function initializeCropper() {
  destroyCropper()
  if (!imageRef.value || !stageRef.value) return
  cropper = new Cropper(imageRef.value, {
    container: stageRef.value,
    template: CROPPER_TEMPLATE,
  })
}

function zoom(scale: number) {
  cropper?.getCropperImage()?.$zoom(scale)
}

function canvasToPng(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('头像图片导出失败'))
    }, 'image/png')
  })
}

async function confirmCrop() {
  if (exporting.value || props.busy) return
  const selection = cropper?.getCropperSelection()
  if (!selection) return
  exporting.value = true
  try {
    const canvas = await selection.$toCanvas({ width: 512, height: 512 })
    emit('confirm', await canvasToPng(canvas))
  } catch {
    message.error('头像裁剪失败，请重试')
  } finally {
    exporting.value = false
  }
}

function cancelCrop() {
  if (props.busy || exporting.value) return
  releasePreview()
  emit('cancel')
}

onBeforeUnmount(releasePreview)
</script>

<template>
  <Modal
    :open="open"
    title="裁剪头像"
    ok-text="使用此头像"
    cancel-text="取消"
    :confirm-loading="exporting || busy"
    :cancel-button-props="{ disabled: busy || exporting }"
    :mask-closable="false"
    width="520px"
    @ok="confirmCrop"
    @cancel="cancelCrop"
  >
    <div ref="stageRef" class="cropper-stage">
      <img
        v-if="previewUrl"
        ref="imageRef"
        :src="previewUrl"
        alt="待裁剪头像"
        @load="initializeCropper"
      />
    </div>
    <div class="cropper-tools" aria-label="头像缩放工具">
      <Tooltip title="放大图片">
        <Button shape="circle" aria-label="放大图片" @click="zoom(0.1)">
          <ZoomInOutlined />
        </Button>
      </Tooltip>
      <Tooltip title="缩小图片">
        <Button shape="circle" aria-label="缩小图片" @click="zoom(-0.1)">
          <ZoomOutOutlined />
        </Button>
      </Tooltip>
    </div>
  </Modal>
</template>

<style scoped>
.cropper-stage {
  width: 100%;
  height: min(360px, 62vh);
  min-height: 280px;
  overflow: hidden;
  background: #181a1f;
}

.cropper-stage img {
  display: block;
  max-width: 100%;
}

.cropper-stage :deep(cropper-canvas) {
  width: 100%;
  height: 100%;
}

.cropper-tools {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 560px) {
  .cropper-stage {
    height: min(320px, 52vh);
    min-height: 240px;
  }
}
</style>

import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { message } from '@hmfw/ant-design'

const cropperMocks = vi.hoisted(() => {
  const zoom = vi.fn()
  const toCanvas = vi.fn()
  const destroy = vi.fn()
  const instances: Array<{ options: { template?: string } }> = []
  const Cropper = vi.fn(function (_image: HTMLImageElement, options: { template?: string }) {
    instances.push({ options })
    return {
      getCropperImage: () => ({ $zoom: zoom }),
      getCropperSelection: () => ({ $toCanvas: toCanvas }),
      destroy,
    }
  })
  return { Cropper, destroy, instances, toCanvas, zoom }
})

vi.mock('cropperjs', () => ({ default: cropperMocks.Cropper }))

import AvatarCropperModal from './AvatarCropperModal.vue'

const ModalStub = defineComponent({
  emits: ['ok', 'cancel'],
  template: `
    <div v-if="$attrs.open" data-testid="cropper-modal">
      <slot />
      <button data-testid="modal-ok" @click="$emit('ok')">确定</button>
      <button data-testid="modal-cancel" @click="$emit('cancel')">取消</button>
    </div>
  `,
})

function mountCropper(file = new File(['image'], 'avatar.png', { type: 'image/png' })) {
  return mount(AvatarCropperModal, {
    props: { open: true, file },
    global: { stubs: { Modal: ModalStub } },
  })
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

describe('AvatarCropperModal', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    cropperMocks.Cropper.mockClear()
    cropperMocks.destroy.mockReset()
    cropperMocks.toCanvas.mockReset()
    cropperMocks.zoom.mockReset()
    cropperMocks.instances.length = 0
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:avatar-preview')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
  })

  it('为所选文件创建预览并初始化可拖动缩放的方形选区', async () => {
    const wrapper = mountCropper()

    expect(wrapper.get('img').attributes('src')).toBe('blob:avatar-preview')
    await wrapper.get('img').trigger('load')

    expect(cropperMocks.Cropper).toHaveBeenCalledTimes(1)
    const template = cropperMocks.instances[0]!.options.template ?? ''
    expect(template).toContain('initial-aspect-ratio="1"')
    expect(template).toContain('aspect-ratio="1"')
    expect(template).toContain('movable')
    expect(template).toContain('translatable')
    expect(template).toContain('scalable')
  })

  it('通过工具按钮缩放裁剪图片', async () => {
    const wrapper = mountCropper()
    await wrapper.get('img').trigger('load')

    await wrapper.get('[aria-label="放大图片"]').trigger('click')
    await wrapper.get('[aria-label="缩小图片"]').trigger('click')

    expect(cropperMocks.zoom).toHaveBeenNthCalledWith(1, 0.1)
    expect(cropperMocks.zoom).toHaveBeenNthCalledWith(2, -0.1)
  })

  it('确认后输出 512x512 PNG Blob', async () => {
    const blob = new Blob(['png'], { type: 'image/png' })
    const canvas = document.createElement('canvas')
    Object.defineProperty(canvas, 'toBlob', {
      value: (callback: BlobCallback, type?: string) => {
        expect(type).toBe('image/png')
        callback(blob)
      },
    })
    cropperMocks.toCanvas.mockResolvedValue(canvas)
    const wrapper = mountCropper()
    await wrapper.get('img').trigger('load')

    await wrapper.get('[data-testid="modal-ok"]').trigger('click')

    expect(cropperMocks.toCanvas).toHaveBeenCalledWith({ width: 512, height: 512 })
    expect(wrapper.emitted('confirm')).toEqual([[blob]])
  })

  it('取消时通知父组件并回收对象 URL', async () => {
    const wrapper = mountCropper()

    await wrapper.get('[data-testid="modal-cancel"]').trigger('click')

    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:avatar-preview')
  })

  it('裁剪导出失败时提示且不提交空结果', async () => {
    cropperMocks.toCanvas.mockRejectedValue(new Error('canvas failed'))
    const error = vi.spyOn(message, 'error')
    const wrapper = mountCropper()
    await wrapper.get('img').trigger('load')

    await wrapper.get('[data-testid="modal-ok"]').trigger('click')

    expect(error).toHaveBeenCalledWith('头像裁剪失败，请重试')
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('异步导出期间阻止取消，避免取消后仍提交头像', async () => {
    const pendingCanvas = deferred<HTMLCanvasElement>()
    cropperMocks.toCanvas.mockReturnValue(pendingCanvas.promise)
    const wrapper = mountCropper()
    await wrapper.get('img').trigger('load')

    await wrapper.get('[data-testid="modal-ok"]').trigger('click')
    await wrapper.get('[data-testid="modal-cancel"]').trigger('click')

    expect(wrapper.emitted('cancel')).toBeUndefined()

    const canvas = document.createElement('canvas')
    Object.defineProperty(canvas, 'toBlob', {
      value: (callback: BlobCallback) => callback(new Blob(['png'], { type: 'image/png' })),
    })
    pendingCanvas.resolve(canvas)
    await vi.waitFor(() => expect(wrapper.emitted('confirm')).toHaveLength(1))
  })

  it('文件变化和组件卸载都会销毁实例并回收对象 URL', async () => {
    const wrapper = mountCropper()
    await wrapper.get('img').trigger('load')
    vi.mocked(URL.createObjectURL).mockReturnValueOnce('blob:next-preview')

    await wrapper.setProps({ file: new File(['next'], 'next.jpg', { type: 'image/jpeg' }) })

    expect(cropperMocks.destroy).toHaveBeenCalledTimes(1)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:avatar-preview')
    wrapper.unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:next-preview')
  })
})

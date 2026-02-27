<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    :width="width"
    :centered="false"
    :destroyOnClose="destroyOnClose"
    :maskClosable="maskClosable"
    :keyboard="keyboard"
    :closable="closable"
    :okText="okText"
    :cancelText="cancelText"
    :okButtonProps="okButtonProps"
    :cancelButtonProps="cancelButtonProps"
    :confirmLoading="confirmLoading"
    :footer="showFooter ? undefined : null"
    :wrapClassName="wrapClassName"
    :styles="modalStyles"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <div class="dialog-content">
      <slot />
    </div>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ModalProps } from 'ant-design-vue'

interface Props {
  open: boolean
  title?: string
  width?: number | string
  okText?: string
  cancelText?: string
  centered?: boolean
  destroyOnClose?: boolean
  maskClosable?: boolean
  keyboard?: boolean
  closable?: boolean
  confirmLoading?: boolean
  showOk?: boolean
  showCancel?: boolean
  showFooter?: boolean
  maxHeight?: string
  okButtonProps?: ModalProps['okButtonProps']
  cancelButtonProps?: ModalProps['cancelButtonProps']
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  width: 520,
  okText: '确定',
  cancelText: '取消',
  centered: true,
  destroyOnClose: true,
  maskClosable: false,
  keyboard: false,
  closable: true,
  confirmLoading: false,
  showOk: true,
  showCancel: true,
  showFooter: true,
  maxHeight: '80vh',
  okButtonProps: undefined,
  cancelButtonProps: undefined
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'ok'): void
  (e: 'cancel'): void
}>()

const visible = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value)
})

const wrapClassName = computed(() => {
  const classes = ['dialog-modal-wrap', 'dialog-centered']
  if (!props.showOk || !props.showCancel) {
    classes.push('dialog-partial-buttons')
  }
  return classes.join(' ')
})

const modalStyles = computed(() => ({
  body: {
    maxHeight: props.maxHeight
  }
}))

const handleOk = (e: Event) => {
  // 阻止默认行为，让父组件控制关闭时机
  e.preventDefault()
  emit('ok')
}

const handleCancel = () => {
  emit('cancel')
  emit('update:open', false)
}
</script>

<style>
/* ========================================
   Dialog Modal - Refined Design System
   ======================================== */

/* 居中容器 - 使用 flexbox 实现完美居中，避免闪烁 */
.dialog-centered {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.dialog-centered .ant-modal {
  max-height: 80vh;
  top: 0;
  padding-bottom: 0;
  margin: 0;
}

/* ========================================
   Modal Content Container
   ======================================== */
.dialog-modal-wrap .ant-modal-content {
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-secondary) !important;
  border: none;
  border-radius: var(--radius-md) !important;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06),
    0 20px 25px -5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* ========================================
   Header Section - 40px height
   ======================================== */
.dialog-modal-wrap .ant-modal-header {
  background: var(--color-bg-secondary) !important;
  border-bottom: 1px solid var(--color-border) !important;
  padding: 0 40px 0 20px !important;
  height: 40px;
  min-height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.dialog-modal-wrap .ant-modal-title {
  color: var(--color-text-primary) !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  line-height: 1.4;
  flex: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 关闭按钮 - 右上角 */
.dialog-modal-wrap .ant-modal-close {
  top: 6px !important;
  right: 12px !important;
  width: 28px !important;
  height: 28px !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary) !important;
  background: transparent !important;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  margin: 0 !important;
  padding: 0 !important;
  position: absolute !important;
  transform: none !important;
}

.dialog-modal-wrap .ant-modal-close:hover {
  color: var(--color-text-primary) !important;
  background: var(--color-bg-tertiary) !important;
}

.dialog-modal-wrap .ant-modal-close:focus {
  outline: none;
}

.dialog-modal-wrap .ant-modal-close .ant-modal-close-x {
  width: 28px;
  height: 28px;
  line-height: 28px;
  font-size: 14px;
}

/* ========================================
   Body Section - Content Area
   ======================================== */
.dialog-modal-wrap .ant-modal-body {
  padding: 0 !important;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.dialog-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
  color: var(--color-text-secondary);
  min-height: 0;
  line-height: 1.6;
}

/* 自定义滚动条 */
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-content::-webkit-scrollbar-track {
  background: transparent;
}

.dialog-content::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-tertiary);
}

/* ========================================
   Footer Section - 52px height
   ======================================== */
.dialog-modal-wrap .ant-modal-footer {
  border-top: 1px solid var(--color-border) !important;
  padding: 10px 20px !important;
  height: 52px;
  min-height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  background: var(--color-bg-secondary) !important;
}

/* 按钮样式优化 */
.dialog-modal-wrap .ant-modal-footer .ant-btn {
  height: 32px;
  min-height: 32px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dialog-modal-wrap .ant-modal-footer .ant-btn-primary {
  background: var(--color-accent-primary);
  border-color: var(--color-accent-primary);
}

.dialog-modal-wrap .ant-modal-footer .ant-btn-primary:hover {
  background: #0257c2;
  border-color: #0257c2;
}

.dialog-modal-wrap .ant-modal-footer .ant-btn-default {
  background: var(--color-bg-secondary);
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

.dialog-modal-wrap .ant-modal-footer .ant-btn-default:hover {
  border-color: var(--color-accent-primary);
  color: var(--color-accent-primary);
}

/* 当只显示部分按钮时的样式调整 */
.dialog-partial-buttons .ant-modal-footer {
  justify-content: flex-end;
}

/* ========================================
   Mask Overlay
   ======================================== */
.ant-modal-mask {
  background-color: rgba(0, 0, 0, 0.45) !important;
  backdrop-filter: blur(1px);
}

.ant-modal-wrap {
  background-color: transparent;
}

/* ========================================
   Animation Enhancements
   ======================================== */
.dialog-modal-wrap .ant-modal-content {
  animation: dialogSlideIn 0.2s ease-out;
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(-8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 无 footer 时的 body 底部圆角 */
.dialog-modal-wrap .ant-modal-content:not(:has(.ant-modal-footer)) .ant-modal-body {
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}
</style>

<template>
  <div :class="['splitter-container', direction]">
    <div 
      :class="['splitter-pane', 'first-pane']"
      :style="firstPaneStyle"
    >
      <slot name="first" />
    </div>
    
    <div 
      class="splitter-bar"
      :class="{ dragging: isDragging }"
      @mousedown="startDrag"
    >
      <div class="splitter-line" />
    </div>
    
    <div 
      :class="['splitter-pane', 'second-pane']"
      :style="secondPaneStyle"
    >
      <slot name="second" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  direction?: 'horizontal' | 'vertical'
  initialSplit?: number
  minSize?: number
  maxSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  direction: 'horizontal',
  initialSplit: 50,
  minSize: 10,
  maxSize: 90
})

const splitPercentage = ref(props.initialSplit)
const isDragging = ref(false)
const containerRef = ref<HTMLElement | null>(null)

const firstPaneStyle = computed(() => {
  if (props.direction === 'horizontal') {
    return { width: `${splitPercentage.value}%` }
  } else {
    return { height: `${splitPercentage.value}%` }
  }
})

const secondPaneStyle = computed(() => {
  if (props.direction === 'horizontal') {
    return { width: `${100 - splitPercentage.value}%` }
  } else {
    return { height: `${100 - splitPercentage.value}%` }
  }
})

const startDrag = (e: MouseEvent) => {
  e.preventDefault()
  isDragging.value = true
  
  const handleMouseMove = (moveEvent: MouseEvent) => {
    if (!isDragging.value) return
    
    const container = (e.target as HTMLElement).closest('.splitter-container')
    if (!container) return
    
    const rect = container.getBoundingClientRect()
    let newPercentage: number
    
    if (props.direction === 'horizontal') {
      newPercentage = ((moveEvent.clientX - rect.left) / rect.width) * 100
    } else {
      newPercentage = ((moveEvent.clientY - rect.top) / rect.height) * 100
    }
    
    newPercentage = Math.max(props.minSize, Math.min(props.maxSize, newPercentage))
    splitPercentage.value = newPercentage
  }
  
  const handleMouseUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = props.direction === 'horizontal' ? 'col-resize' : 'row-resize'
  document.body.style.userSelect = 'none'
}
</script>

<style scoped>
.splitter-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.splitter-container.horizontal {
  flex-direction: row;
}

.splitter-container.vertical {
  flex-direction: column;
}

.splitter-pane {
  overflow: hidden;
  position: relative;
}

.first-pane {
  flex-shrink: 0;
}

.second-pane {
  flex: 1;
}

.splitter-bar {
  flex-shrink: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.horizontal .splitter-bar {
  width: 8px;
  cursor: col-resize;
  flex-direction: column;
}

.vertical .splitter-bar {
  height: 8px;
  cursor: row-resize;
  flex-direction: row;
}

.splitter-bar:hover .splitter-line,
.splitter-bar.dragging .splitter-line {
  background: var(--color-accent-primary);
}

.splitter-line {
  background: var(--color-border);
  transition: background-color var(--transition-fast);
}

.horizontal .splitter-line {
  width: 1px;
  height: 100%;
}

.vertical .splitter-line {
  height: 1px;
  width: 100%;
}
</style>

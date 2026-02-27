<template>
  <div class="file-tree-container">
    <!-- Search Bar -->
    <div v-if="showSearch" class="file-tree-search">
      <a-input
        v-model:value="searchText"
        placeholder="Search files..."
        allow-clear
        @change="handleSearch"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
    </div>

    <!-- Tree Header -->
    <div v-if="showHeader && title" class="file-tree-header">
      <span class="file-tree-title">{{ title }}</span>
      <div class="file-tree-actions">
        <a-tooltip v-if="showRefresh" title="Refresh">
          <ReloadOutlined class="action-icon" @click="handleRefresh" />
        </a-tooltip>
        <a-tooltip v-if="showCollapseAll" title="Collapse All">
          <MinusSquareOutlined class="action-icon" @click="handleCollapseAll" />
        </a-tooltip>
      </div>
    </div>

    <!-- File Tree -->
    <div class="file-tree-content" :style="treeHeightStyle">
      <a-tree
        v-if="(filteredTreeData?.length || 0) > 0"
        v-model:expandedKeys="expandedKeys"
        v-model:selectedKeys="selectedKeys"
        v-model:checkedKeys="checkedKeys"
        :tree-data="filteredTreeData"
        :show-icon="showIcon"
        :show-line="showLine"
        :checkable="checkable"
        :draggable="draggable"
        :block-node="blockNode"
        :virtual="virtual"
        :height="virtual ? virtualHeight : undefined"
        @select="handleSelect"
        @expand="handleExpand"
        @check="handleCheck"
        @dragstart="handleDragStart"
        @dragenter="handleDragEnter"
        @drop="handleDrop"
        @right-click="handleRightClick"
      >
        <template #icon="node">
          <component :is="getNodeIcon(node)" />
        </template>
        <template #title="node">
          <span :class="['tree-node-title', { 'node-highlight': isHighlighted(node) }]">
            {{ node.title }}
          </span>
        </template>
      </a-tree>
      <div v-else-if="searchText" class="file-tree-empty">
        <FileSearchOutlined class="empty-icon" />
        <span>No files found</span>
      </div>
      <div v-else class="file-tree-empty">
        <FolderOpenOutlined class="empty-icon" />
        <span>No files</span>
      </div>
    </div>

    <!-- Context Menu -->
    <div
      v-if="contextMenuVisible"
      class="file-tree-context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
    >
      <div class="context-menu-item" @click="handleContextAction('open')">
        <FolderOpenOutlined /> Open
      </div>
      <div class="context-menu-item" @click="handleContextAction('newFile')">
        <FileOutlined /> New File
      </div>
      <div class="context-menu-item" @click="handleContextAction('newFolder')">
        <FolderAddOutlined /> New Folder
      </div>
      <div class="context-menu-divider" />
      <div class="context-menu-item" @click="handleContextAction('rename')">
        <EditOutlined /> Rename
      </div>
      <div class="context-menu-item" @click="handleContextAction('copy')">
        <CopyOutlined /> Copy Path
      </div>
      <div class="context-menu-divider" />
      <div class="context-menu-item danger" @click="handleContextAction('delete')">
        <DeleteOutlined /> Delete
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, type Component } from 'vue'
import {
  SearchOutlined,
  ReloadOutlined,
  MinusSquareOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  FileOutlined,
  FileTextOutlined,
  FileMarkdownOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  FilePptOutlined,
  FileZipOutlined,
  FileUnknownOutlined,
  CodeOutlined,
  Html5Outlined,
  FileSearchOutlined,
  FolderAddOutlined,
  EditOutlined,
  CopyOutlined,
  DeleteOutlined,
  GithubOutlined,

  SettingOutlined,
  DatabaseOutlined,
  ConsoleSqlOutlined,
} from '@ant-design/icons-vue'
import type { TreeProps } from 'ant-design-vue'
import type { FileNode } from '../types'

interface Props {
  /** Tree data source */
  treeData: FileNode[]
  /** Tree title */
  title?: string
  /** Show search input */
  showSearch?: boolean
  /** Show header bar with title and actions */
  showHeader?: boolean
  /** Show refresh button */
  showRefresh?: boolean
  /** Show collapse all button */
  showCollapseAll?: boolean
  /** Show tree line */
  showLine?: boolean
  /** Show node icon */
  showIcon?: boolean
  /** Enable checkbox selection */
  checkable?: boolean
  /** Enable drag and drop */
  draggable?: boolean
  /** Use block node style */
  blockNode?: boolean
  /** Enable virtual scroll */
  virtual?: boolean
  /** Virtual scroll height */
  virtualHeight?: number
  /** Custom tree height */
  treeHeight?: string
  /** Default expanded keys */
  defaultExpandedKeys?: string[]
  /** Default selected keys */
  defaultSelectedKeys?: string[]
  /** Enable context menu */
  contextMenu?: boolean
  /** File icons mapping */
  fileIcons?: Record<string, Component>
  /** Folder icons */
  folderIcon?: Component
  /** Folder open icon */
  folderOpenIcon?: Component
  /** Merge single folder into parent (compact mode) */
  mergeSingleFolder?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showSearch: false,
  showHeader: false,
  showRefresh: true,
  showCollapseAll: true,
  showLine: false,
  showIcon: true,
  checkable: false,
  draggable: false,
  blockNode: true,
  virtual: false,
  virtualHeight: 500,
  treeHeight: '',
  defaultExpandedKeys: () => [],
  defaultSelectedKeys: () => [],
  contextMenu: false,
  mergeSingleFolder: false,
})

const emit = defineEmits<{
  (e: 'select', node: FileNode, selected: boolean): void
  (e: 'expand', node: FileNode, expanded: boolean): void
  (e: 'check', node: FileNode, checked: boolean): void
  (e: 'refresh'): void
  (e: 'collapseAll'): void
  (e: 'search', text: string): void
  (e: 'dragStart', node: FileNode): void
  (e: 'dragEnter', node: FileNode): void
  (e: 'drop', node: FileNode, dragNode: FileNode): void
  (e: 'contextAction', action: string, node: FileNode): void
}>()

// State
const searchText = ref('')
const expandedKeys = ref<string[]>([...props.defaultExpandedKeys])
const selectedKeys = ref<string[]>([...props.defaultSelectedKeys])
const checkedKeys = ref<string[]>([])
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuNode = ref<FileNode | null>(null)

// Computed
const treeHeightStyle = computed(() => {
  if (!props.treeHeight || props.treeHeight === '100%') {
    return {}
  }
  return { height: props.treeHeight }
})
// File extension to icon mapping
const defaultFileIcons: Record<string, Component> = {
  // JavaScript/TypeScript
  js: CodeOutlined,
  jsx: CodeOutlined,
  ts: CodeOutlined,
  tsx: CodeOutlined,
  mjs: CodeOutlined,
  cjs: CodeOutlined,
  // HTML/CSS
  html: Html5Outlined,
  htm: Html5Outlined,
  css: CodeOutlined,
  scss: CodeOutlined,
  less: CodeOutlined,
  // Config/Data
  json: CodeOutlined,
  yaml: SettingOutlined,
  yml: SettingOutlined,
  xml: CodeOutlined,
  toml: SettingOutlined,
  ini: SettingOutlined,
  env: SettingOutlined,
  // Documentation
  md: FileMarkdownOutlined,
  mdx: FileMarkdownOutlined,
  txt: FileTextOutlined,
  rst: FileTextOutlined,
  // Images
  png: FileImageOutlined,
  jpg: FileImageOutlined,
  jpeg: FileImageOutlined,
  gif: FileImageOutlined,
  svg: FileImageOutlined,
  ico: FileImageOutlined,
  webp: FileImageOutlined,
  // Documents
  pdf: FilePdfOutlined,
  doc: FileWordOutlined,
  docx: FileWordOutlined,
  xls: FileExcelOutlined,
  xlsx: FileExcelOutlined,
  ppt: FilePptOutlined,
  pptx: FilePptOutlined,
  // Archives
  zip: FileZipOutlined,
  tar: FileZipOutlined,
  gz: FileZipOutlined,
  rar: FileZipOutlined,
  '7z': FileZipOutlined,
  // Database
  sql: ConsoleSqlOutlined,
  db: DatabaseOutlined,
  sqlite: DatabaseOutlined,
  // Source control
  gitignore: GithubOutlined,
  gitattributes: GithubOutlined,
  gitmodules: GithubOutlined,
  // Java
  java: CodeOutlined,
  kt: CodeOutlined,
  kts: CodeOutlined,
  groovy: CodeOutlined,
  gradle: CodeOutlined,
  // Python
  py: CodeOutlined,
  pyw: CodeOutlined,
  // Go
  go: CodeOutlined,
  mod: CodeOutlined,
  sum: CodeOutlined,
  // Rust
  rs: CodeOutlined,
  // Ruby
  rb: CodeOutlined,
  gemfile: CodeOutlined,
  // PHP
  php: CodeOutlined,
  // C/C++
  c: CodeOutlined,
  cpp: CodeOutlined,
  h: CodeOutlined,
  hpp: CodeOutlined,
  // Shell
  sh: CodeOutlined,
  bash: CodeOutlined,
  zsh: CodeOutlined,
  fish: CodeOutlined,
  // Docker
  dockerfile: CodeOutlined,
  dockerignore: CodeOutlined,
}

// Convert FileNode to Ant Design TreeData
const convertToTreeData = (nodes: FileNode[]): TreeProps['treeData'] => {
  return nodes.map((node) => ({
    key: node.path,
    title: node.name,
    value: node.path,
    isLeaf: node.type === 'file',
    children: node.children ? convertToTreeData(node.children) : undefined,
    data: node,
  }))
}

// Filter tree data based on search text
const filterTreeData = (nodes: FileNode[], search: string): FileNode[] => {
  if (!search) return nodes

  const result: FileNode[] = []
  for (const node of nodes) {
    if (node.type === 'directory' && node.children) {
      const filteredChildren = filterTreeData(node.children, search)
      if (filteredChildren.length > 0) {
        result.push({ ...node, children: filteredChildren })
      }
    } else if (node.name.toLowerCase().includes(search.toLowerCase())) {
      result.push(node)
    }
  }
  return result
}

// Merge single folder into parent (compact mode)
const mergeSingleFolderNodes = (nodes: FileNode[]): FileNode[] => {
  if (!props.mergeSingleFolder) return nodes

  return nodes.map(node => {
    if (node.type === 'directory' && node.children && node.children.length === 1) {
      const onlyChild = node.children[0]
      // Only merge if the only child is also a directory
      if (onlyChild.type === 'directory') {
        // Recursively merge the child
        const mergedChild = mergeSingleFolderNodes([onlyChild])[0]
        return {
          name: `${node.name}/${mergedChild.name}`,
          path: mergedChild.path, // Keep the deepest path for selection
          type: 'directory' as const,
          children: mergedChild.children
        }
      }
    }
    // Recursively process children
    if (node.children) {
      return {
        ...node,
        children: mergeSingleFolderNodes(node.children)
      }
    }
    return node
  })
}

// Filtered tree data for display
const filteredTreeData = computed((): NonNullable<TreeProps['treeData']> => {
  let filtered = filterTreeData(props.treeData, searchText.value)
  if (props.mergeSingleFolder) {
    filtered = mergeSingleFolderNodes(filtered)
  }
  return convertToTreeData(filtered) || []
})

// Get icon for a node
const getNodeIcon = (node: any): Component => {
  if (node.isLeaf) {
    const ext = getFileExtension(node.title)
    const iconMap = { ...defaultFileIcons, ...props.fileIcons }
    return iconMap[ext] || FileUnknownOutlined
  }
  const isExpanded = expandedKeys.value.includes(node.key)
  return isExpanded ? (props.folderOpenIcon || FolderOpenOutlined) : (props.folderIcon || FolderOutlined)
}

// Get file extension
const getFileExtension = (filename: string): string => {
  const parts = filename.split('.')
  if (parts.length <= 1) return ''
  // Handle dotfiles like .gitignore
  if (parts.length === 2 && parts[0] === '') {
    return parts[1]
  }
  return parts[parts.length - 1].toLowerCase()
}

// Check if node should be highlighted
const isHighlighted = (node: any): boolean => {
  return searchText.value && node.title.toLowerCase().includes(searchText.value.toLowerCase())
}

// Event handlers
const handleSearch = () => {
  emit('search', searchText.value)
}

const handleSelect = (keys: any, info: any) => {
  const node = info.node?.data as FileNode
  if (node) {
    // 如果是文件夹，切换展开状态
    if (node.type === 'directory') {
      const isExpanded = expandedKeys.value.includes(node.path)
      if (isExpanded) {
        expandedKeys.value = expandedKeys.value.filter(k => k !== node.path)
      } else {
        expandedKeys.value.push(node.path)
      }
      emit('expand', node, !isExpanded)
    }
    emit('select', node, keys.includes(node.path))
  }
}

const handleExpand = (_keys: any, info: any) => {
  const node = info.node?.data as FileNode
  if (node) {
    emit('expand', node, info.expanded)
  }
}

const handleCheck = (_checked: any, info: any) => {
  const node = info.node?.data as FileNode
  if (node) {
    emit('check', node, info.checked)
  }
}

const handleRefresh = () => {
  emit('refresh')
}

const handleCollapseAll = () => {
  expandedKeys.value = []
  emit('collapseAll')
}

const handleDragStart = (info: any) => {
  const node = info.node?.data as FileNode
  if (node) {
    emit('dragStart', node)
  }
}

const handleDragEnter = (info: any) => {
  const node = info.node?.data as FileNode
  if (node) {
    emit('dragEnter', node)
  }
}

const handleDrop = (info: any) => {
  const node = info.node?.data as FileNode
  const dragNode = info.dragNode?.data as FileNode
  if (node && dragNode) {
    emit('drop', node, dragNode)
  }
}

const handleRightClick = (info: any) => {
  if (props.contextMenu) {
    info.event.preventDefault()
    contextMenuNode.value = info.node?.data as FileNode
    contextMenuX.value = info.event.clientX
    contextMenuY.value = info.event.clientY
    contextMenuVisible.value = true
  }
}

const handleContextAction = (action: string) => {
  if (contextMenuNode.value) {
    emit('contextAction', action, contextMenuNode.value)
  }
  contextMenuVisible.value = false
  contextMenuNode.value = null
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.file-tree-context-menu')) {
    contextMenuVisible.value = false
    contextMenuNode.value = null
  }
}

// Watch for tree data changes
watch(
  () => props.treeData,
  () => {
    // Auto-expand first level directories
    if (expandedKeys.value.length === 0 && props.treeData.length > 0) {
      const firstLevelKeys = props.treeData
        .filter((node) => node.type === 'directory')
        .map((node) => node.path)
      if (firstLevelKeys.length > 0) {
        expandedKeys.value = firstLevelKeys
      }
    }
  },
  { immediate: true }
)

// Watch for defaultExpandedKeys changes from parent
watch(
  () => props.defaultExpandedKeys,
  (newKeys) => {
    if (newKeys.length > 0 && expandedKeys.value.length === 0) {
      expandedKeys.value = [...newKeys]
    }
  },
  { immediate: true }
)

// Lifecycle
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// Expose methods
defineExpose({
  expandAll: () => {
    const getAllKeys = (nodes: FileNode[]): string[] => {
      return nodes.reduce((keys: string[], node) => {
        if (node.type === 'directory') {
          keys.push(node.path)
          if (node.children) {
            keys.push(...getAllKeys(node.children))
          }
        }
        return keys
      }, [])
    }
    expandedKeys.value = getAllKeys(props.treeData)
  },
  collapseAll: () => {
    expandedKeys.value = []
  },
  selectNode: (path: string) => {
    selectedKeys.value = [path]
  },
  expandNode: (path: string) => {
    if (!expandedKeys.value.includes(path)) {
      expandedKeys.value.push(path)
    }
  },
  collapseNode: (path: string) => {
    expandedKeys.value = expandedKeys.value.filter((k) => k !== path)
  },
  // 展开到指定节点的所有父级目录
  expandToNode: (targetPath: string) => {
    const parentPaths: string[] = []
    
    const findPath = (nodes: FileNode[], currentPath: string): boolean => {
      for (const node of nodes) {
        if (node.path === targetPath) {
          return true
        }
        if (node.type === 'directory' && node.children) {
          parentPaths.push(node.path)
          if (findPath(node.children, currentPath)) {
            return true
          }
          parentPaths.pop()
        }
      }
      return false
    }
    
    findPath(props.treeData, '')
    
    // 展开所有父级目录
    for (const path of parentPaths) {
      if (!expandedKeys.value.includes(path)) {
        expandedKeys.value.push(path)
      }
    }
  },
  // 选中并展开到指定节点
  selectAndExpandTo: (path: string) => {
    selectedKeys.value = [path]
    // 展开到目标节点
    const parentPaths: string[] = []
    
    const findPath = (nodes: FileNode[]): boolean => {
      for (const node of nodes) {
        if (node.path === path) {
          return true
        }
        if (node.type === 'directory' && node.children) {
          parentPaths.push(node.path)
          if (findPath(node.children)) {
            return true
          }
          parentPaths.pop()
        }
      }
      return false
    }
    
    findPath(props.treeData)
    
    // 展开所有父级目录（不包括目标本身）
    for (const p of parentPaths) {
      if (!expandedKeys.value.includes(p)) {
        expandedKeys.value.push(p)
      }
    }
  },
})
</script>

<style scoped>
.file-tree-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #ffffff;
  font-family: var(--font-ui, 'Space Grotesk', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  font-size: 13px;
}

.file-tree-search {
  flex-shrink: 0;
  padding: 8px 12px;
  border-bottom: 1px solid #e8e8e8;
}

.file-tree-search :deep(.ant-input-affix-wrapper) {
  background: #fafafa;
  border-color: #d9d9d9;
}

.file-tree-search :deep(.ant-input) {
  background: transparent;
  color: #333;
}

.file-tree-search :deep(.ant-input::placeholder) {
  color: #999;
}

.file-tree-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.file-tree-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-tree-actions {
  display: flex;
  gap: 4px;
}

.action-icon {
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: #666;
  transition: all 0.2s ease;
}

.action-icon:hover {
  background: #e6e6e6;
  color: #333;
}

.file-tree-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.file-tree-content :deep(.ant-tree) {
  background: transparent;
  color: #333;
}

.file-tree-content :deep(.ant-tree .ant-tree-treenode) {
  display: flex;
  align-items: center;
  padding: 0 4px;
  width: 100%;
  height: 28px;
  border-radius: 4px;
  transition: background-color 0.1s ease;
  overflow: hidden;
}

.file-tree-content :deep(.ant-tree .ant-tree-treenode:hover) {
  background: #f5f5f5;
}

.file-tree-content :deep(.ant-tree .ant-tree-treenode-selected) {
  background: #e6f4ff !important;
}

.file-tree-content :deep(.ant-tree .ant-tree-node-content-wrapper) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  padding: 0 4px;
  min-width: 0;
  overflow: hidden;
  height: 28px;
  line-height: 28px;
}

.file-tree-content :deep(.ant-tree .ant-tree-iconEle) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 28px;
  color: #666;
  flex-shrink: 0;
}

.file-tree-content :deep(.ant-tree .ant-tree-iconEle > .anticon) {
  font-size: 14px;
  vertical-align: middle;
}

.file-tree-content :deep(.ant-tree-indent-unit) {
  width: 16px;
  height: 28px;
  flex-shrink: 0;
}

.file-tree-content :deep(.ant-tree-switcher) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 28px;
  line-height: 28px;
  flex-shrink: 0;
}

.file-tree-content :deep(.ant-tree-switcher-icon) {
  font-size: 12px;
  color: #666;
  vertical-align: middle;
}

.tree-node-title {
  display: block;
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-family: var(--font-ui, 'Space Grotesk', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  line-height: 28px;
  height: 28px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.file-tree-content :deep(.ant-tree .ant-tree-title) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  height: 28px;
  overflow: hidden;
}

.node-highlight {
  background: #fff3cd;
  border-radius: 2px;
}

.file-tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 120px;
  color: #999;
  gap: 8px;
}

.empty-icon {
  font-size: 32px;
  opacity: 0.5;
}

/* Context Menu */
.file-tree-context-menu {
  position: fixed;
  z-index: 1000;
  min-width: 160px;
  padding: 4px 0;
  background: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.1s ease;
}

.context-menu-item:hover {
  background: #f5f5f5;
}

.context-menu-item.danger {
  color: #ff4d4f;
}

.context-menu-item.danger:hover {
  background: #fff1f0;
}

.context-menu-divider {
  height: 1px;
  margin: 4px 0;
  background: #e8e8e8;
}

/* Scrollbar */
.file-tree-content::-webkit-scrollbar {
  width: 6px;
}

.file-tree-content::-webkit-scrollbar-track {
  background: transparent;
}

.file-tree-content::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.file-tree-content::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>

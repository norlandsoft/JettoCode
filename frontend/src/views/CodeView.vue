<template>
  <div class="code-view">
    <Splitter direction="horizontal" :initial-split="25" :min-size="15" :max-size="50">
      <template #first>
        <aside class="file-tree">
          <div class="tree-header">
            <FolderOutlined />
            <a-select
              v-model:value="selectedBranch"
              style="flex: 1"
              placeholder="选择分支"
              :loading="loadingBranches"
              @change="handleBranchChange"
            >
              <a-select-option v-for="branch in branches" :key="branch.name" :value="branch.name">
                {{ branch.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="tree-content">
            <FileTree
              ref="fileTreeRef"
              :tree-data="fileTree"
              :merge-single-folder="true"
              @select="handleFileSelect"
              @expand="handleFolderExpand"
              @refresh="loadFileTree"
            />
          </div>
        </aside>
      </template>

      <template #second>
        <main class="code-content">
          <!-- 文件内容 - Monaco Editor -->
          <template v-if="currentFile && currentFileType === 'file'">
            <header class="content-header">
              <div class="file-info">
                <FileOutlined />
                <span class="file-path">{{ currentFile }}</span>
              </div>
            </header>
            <div class="editor-container">
              <div v-if="loadingContent" class="loading-container">
                <a-spin />
              </div>
              <div v-else ref="editorRef" class="monaco-editor"></div>
            </div>
          </template>

          <!-- 文件夹内容列表 -->
          <template v-else-if="currentFolder">
            <header class="content-header">
              <div class="file-info">
                <FolderOutlined />
                <span class="file-path">{{ currentFolder }}</span>
                <span class="file-count">{{ folderContents.length }} 项</span>
              </div>
            </header>
            <div class="folder-content">
              <div class="content-list">
                <div
                  v-for="item in sortedFolderContents"
                  :key="item.path"
                  class="content-item"
                  @click="handleItemClick(item)"
                >
                  <div class="item-icon">
                    <FolderOutlined v-if="item.type === 'directory'" class="icon-folder" />
                    <FileOutlined v-else class="icon-file" />
                  </div>
                  <div class="item-info">
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-meta" v-if="item.type === 'file'">{{ getFileExtension(item.name) || '文件' }}</span>
                  </div>
                </div>
              </div>
              <div v-if="folderContents.length === 0" class="empty-folder">
                <FolderOpenOutlined class="empty-icon" />
                <p>此文件夹为空</p>
              </div>
            </div>
          </template>

          <!-- 占位符 -->
          <div v-else class="placeholder">
            <div class="empty-icon">
              <FileSearchOutlined />
            </div>
            <h3>选择文件或文件夹</h3>
            <p>从左侧文件树中选择一个文件查看内容，或选择文件夹浏览内容</p>
          </div>
        </main>
      </template>
    </Splitter>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FileNode, Branch } from '@/types'
import { FolderOutlined, FolderOpenOutlined, FileOutlined, FileSearchOutlined } from '@ant-design/icons-vue'
import { serviceApi } from '@/api/project'
import Splitter from '@/components/Splitter.vue'
import FileTree from '@/components/FileTree.vue'
import * as monaco from 'monaco-editor'

const route = useRoute()
const serviceId = computed(() => Number(route.params.serviceId))

const branches = ref<Branch[]>([])
const selectedBranch = ref<string>('')
const loadingBranches = ref(false)

const fileTree = ref<FileNode[]>([])
const loadingTree = ref(false)

// FileTree 组件引用
const fileTreeRef = ref<InstanceType<typeof FileTree> | null>(null)

const currentFile = ref('')
const currentFolder = ref('')
const currentFileType = ref<'file' | 'directory' | null>(null)
const folderContents = ref<FileNode[]>([])
const fileContent = ref('')
const loadingContent = ref(false)
const selectedKeys = ref<string[]>([])

// Monaco Editor
const editorRef = ref<HTMLElement | null>(null)
const editor = shallowRef<monaco.editor.IStandaloneCodeEditor | null>(null)

// 语言映射
const languageMap: Record<string, string> = {
  js: 'javascript',
  jsx: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  mjs: 'javascript',
  cjs: 'javascript',
  html: 'html',
  htm: 'html',
  css: 'css',
  scss: 'scss',
  less: 'less',
  json: 'json',
  xml: 'xml',
  yaml: 'yaml',
  yml: 'yaml',
  md: 'markdown',
  mdx: 'markdown',
  java: 'java',
  kt: 'kotlin',
  kts: 'kotlin',
  py: 'python',
  go: 'go',
  rs: 'rust',
  rb: 'ruby',
  php: 'php',
  c: 'c',
  cpp: 'cpp',
  h: 'c',
  hpp: 'cpp',
  cs: 'csharp',
  sh: 'shell',
  bash: 'shell',
  zsh: 'shell',
  sql: 'sql',
  dockerfile: 'dockerfile',
  vue: 'vue',
  svelte: 'svelte',
}

// 获取文件扩展名
const getFileExtension = (filename: string): string => {
  const parts = filename.split('.')
  if (parts.length <= 1) return ''
  if (parts.length === 2 && parts[0] === '') return parts[1]
  return parts[parts.length - 1].toLowerCase()
}

// 获取 Monaco 语言
const getMonacoLanguage = (filename: string): string => {
  const ext = getFileExtension(filename)
  return languageMap[ext] || 'plaintext'
}

// 排序后的文件夹内容
const sortedFolderContents = computed(() => {
  return [...folderContents.value].sort((a, b) => {
    // 文件夹在前，文件在后
    if (a.type !== b.type) {
      return a.type === 'directory' ? -1 : 1
    }
    // 同类型按名称排序
    return a.name.localeCompare(b.name)
  })
})

// 销毁编辑器
const destroyEditor = () => {
  if (editor.value) {
    editor.value.dispose()
    editor.value = null
  }
}

// 初始化 Monaco Editor
const initEditor = () => {
  if (!editorRef.value) {
    console.warn('Editor ref not found')
    return false
  }

  // 销毁旧编辑器
  destroyEditor()

  editor.value = monaco.editor.create(editorRef.value, {
    value: fileContent.value,
    language: getMonacoLanguage(currentFile.value),
    theme: 'vs',
    readOnly: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    lineNumbers: 'on',
    fontSize: 13,
    fontFamily: "'JetBrains Mono', 'Consolas', 'Monaco', monospace",
    automaticLayout: true,
    wordWrap: 'off',
    folding: true,
    renderLineHighlight: 'line',
    scrollbar: {
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10,
    },
  })
  
  return true
}



const loadBranches = async () => {
  loadingBranches.value = true
  try {
    const { data } = await serviceApi.getBranches(serviceId.value)
    branches.value = data.data

    const service = await serviceApi.getById(serviceId.value)
    selectedBranch.value = service.data.data.currentBranch || (branches.value[0]?.name || '')

    if (selectedBranch.value) {
      loadFileTree()
    }
  } catch (error) {
    console.error(error)
    message.error('加载分支失败')
  } finally {
    loadingBranches.value = false
  }
}

const handleBranchChange = async (branch: unknown) => {
  const branchName = String(branch)
  try {
    await serviceApi.checkoutBranch(serviceId.value, branchName)
    message.success(`已切换到分支 ${branchName}`)
    loadFileTree()
    currentFile.value = ''
    currentFolder.value = ''
    currentFileType.value = null
    folderContents.value = []
    fileContent.value = ''
    selectedKeys.value = []
  } catch (error) {
    console.error(error)
    message.error('切换分支失败')
  }
}

const loadFileTree = async () => {
  loadingTree.value = true
  try {
    const { data } = await serviceApi.getFileTree(serviceId.value)
    fileTree.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载文件树失败')
  } finally {
    loadingTree.value = false
  }
}

const handleFileSelect = (node: FileNode, selected: boolean) => {
  if (!selected) return

  selectedKeys.value = [node.path]

  if (node.type === 'file') {
    currentFile.value = node.path
    currentFolder.value = ''
    currentFileType.value = 'file'
    folderContents.value = []
    loadFileContent(node.path)
  } else if (node.type === 'directory') {
    // 切换到文件夹视图时销毁编辑器
    destroyEditor()
    currentFolder.value = node.path
    currentFile.value = ''
    currentFileType.value = 'directory'
    folderContents.value = node.children || []
    fileContent.value = ''
  }
}

const handleFolderExpand = (_node: FileNode, _expanded: boolean) => {
  // 文件夹展开时不做特殊处理
}

const handleItemClick = (item: FileNode) => {
  // 同步文件树选中状态
  fileTreeRef.value?.selectAndExpandTo(item.path)
  
  if (item.type === 'file') {
    currentFile.value = item.path
    currentFolder.value = ''
    currentFileType.value = 'file'
    selectedKeys.value = [item.path]
    loadFileContent(item.path)
  } else if (item.type === 'directory') {
    // 切换到文件夹视图时销毁编辑器
    destroyEditor()
    currentFolder.value = item.path
    currentFile.value = ''
    currentFileType.value = 'directory'
    folderContents.value = item.children || []
    selectedKeys.value = [item.path]
  }
}

const loadFileContent = async (path: string) => {
  // 先销毁旧编辑器
  destroyEditor()
  loadingContent.value = true
  
  try {
    const { data } = await serviceApi.getFileContent(serviceId.value, path)
    fileContent.value = data.data.content
    loadingContent.value = false
    
    // 等待 DOM 渲染
    await nextTick()
    await nextTick()
    
    // 初始化编辑器
    initEditor()
  } catch (error) {
    console.error(error)
    message.error('加载文件内容失败')
    fileContent.value = ''
    loadingContent.value = false
  }
}



// 窗口大小变化时重新布局编辑器
const handleResize = () => {
  if (editor.value) {
    editor.value.layout()
  }
}

onMounted(() => {
  loadBranches()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  destroyEditor()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.code-view {
  height: 100%;
  overflow: hidden;
}

.file-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.tree-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-base);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.tree-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.code-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-bg-secondary);
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
  flex-shrink: 0;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.file-path {
  font-family: var(--font-mono);
  font-size: 12px;
}

.file-count {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-left: 8px;
}

.editor-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.monaco-editor {
  width: 100%;
  height: 100%;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* 文件夹内容列表 */
.folder-content {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.content-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.content-item:hover {
  background: var(--color-bg-tertiary);
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.icon-folder {
  color: #f0c040;
  font-size: 16px;
}

.icon-file {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 13px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.empty-folder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: var(--color-text-tertiary);
}

.empty-folder .empty-icon {
  font-size: 48px;
  opacity: 0.3;
  margin-bottom: 16px;
}

.empty-folder p {
  margin: 0;
  font-size: 14px;
}

/* 占位符 */
.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.placeholder .empty-icon {
  font-size: 48px;
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.placeholder h3 {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-xs);
}

.placeholder p {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin: 0;
}
</style>

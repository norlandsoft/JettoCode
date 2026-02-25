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
            <a-tree
              v-if="fileTree.length > 0"
              :tree-data="fileTree as any"
              :field-names="{ children: 'children', title: 'name', key: 'path' }"
              :selected-keys="selectedKeys"
              @select="handleNodeSelect as any"
              show-icon
              class="custom-tree"
            >
              <template #icon="node">
                <FolderOutlined v-if="node.type === 'directory'" />
                <FileOutlined v-else />
              </template>
            </a-tree>
            <div v-else class="empty-tree">
              <FileSearchOutlined />
              <p>{{ loadingTree ? '加载中...' : '暂无文件' }}</p>
            </div>
          </div>
        </aside>
      </template>
      
      <template #second>
        <main class="code-content">
          <header class="content-header" v-if="currentFile">
            <div class="file-info">
              <FileOutlined />
              <span class="file-path code-text">{{ currentFile }}</span>
            </div>
          </header>
          <div class="editor-container" v-if="currentFile">
            <div v-if="loadingContent" class="loading-container">
              <a-spin />
            </div>
            <pre v-else class="code-block"><code>{{ fileContent }}</code></pre>
          </div>
          <div class="placeholder" v-else>
            <div class="empty-icon">
              <FileSearchOutlined />
            </div>
            <h3>选择文件查看代码</h3>
            <p>从左侧文件树中选择一个文件开始浏览</p>
          </div>
        </main>
      </template>
    </Splitter>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FileNode, Branch, CodeContent } from '@/types'
import { FolderOutlined, FileOutlined, FileSearchOutlined } from '@ant-design/icons-vue'
import { serviceApi } from '@/api/project'
import Splitter from '@/components/Splitter.vue'

const route = useRoute()
const serviceId = computed(() => Number(route.params.serviceId))

const branches = ref<Branch[]>([])
const selectedBranch = ref<string>('')
const loadingBranches = ref(false)

const fileTree = ref<FileNode[]>([])
const loadingTree = ref(false)

const currentFile = ref('')
const fileContent = ref('')
const loadingContent = ref(false)
const selectedKeys = ref<string[]>([])

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

const handleBranchChange = async (branch: string) => {
  try {
    await serviceApi.checkoutBranch(serviceId.value, branch)
    message.success(`已切换到分支 ${branch}`)
    loadFileTree()
    currentFile.value = ''
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

const handleNodeSelect = async (selected: string[], info: { node: FileNode }) => {
  if (info.node.type === 'file') {
    currentFile.value = info.node.path
    selectedKeys.value = selected
    loadFileContent(info.node.path)
  }
}

const loadFileContent = async (path: string) => {
  loadingContent.value = true
  try {
    const { data } = await serviceApi.getFileContent(serviceId.value, path)
    fileContent.value = data.data.content
  } catch (error) {
    console.error(error)
    message.error('加载文件内容失败')
    fileContent.value = ''
  } finally {
    loadingContent.value = false
  }
}

onMounted(() => {
  loadBranches()
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
}

.tree-content {
  flex: 1;
  padding: var(--spacing-sm);
  overflow-y: auto;
}

.custom-tree {
  background: transparent !important;
}

.custom-tree :deep(li) {
  overflow: hidden;
}

.custom-tree :deep(.ant-tree-treenode) {
  display: flex !important;
  overflow: hidden !important;
  align-items: center !important;
  max-width: 100% !important;
}

.custom-tree :deep(.ant-tree-node-content-wrapper) {
  flex: 1 !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  max-width: 100% !important;
  display: inline-block !important;
  padding: 4px 8px !important;
  border-radius: var(--radius-sm) !important;
  transition: all var(--transition-fast) !important;
}

.custom-tree :deep(.ant-tree-title) {
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  display: inline-block !important;
  max-width: 100% !important;
}

.custom-tree :deep(.ant-tree-node-selected) {
  background: var(--color-info-bg) !important;
}

.custom-tree :deep(.ant-tree-switcher) {
  color: var(--color-text-tertiary);
  flex-shrink: 0 !important;
}

.custom-tree :deep(.ant-tree-iconEle) {
  color: var(--color-accent-primary);
  flex-shrink: 0 !important;
}

.empty-tree {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl);
  color: var(--color-text-tertiary);
  text-align: center;
}

.empty-tree > .anticon {
  font-size: 32px;
  margin-bottom: var(--spacing-base);
  opacity: 0.4;
}

.empty-tree p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.code-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.content-header {
  padding: var(--spacing-base);
  border-bottom: 1px solid var(--color-border);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.file-path {
  font-size: var(--font-size-sm);
}

.editor-container {
  flex: 1;
  overflow: auto;
  padding: var(--spacing-base);
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.code-block {
  margin: 0;
  padding: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: var(--font-size-sm);
  line-height: 1.6;
  color: var(--color-text-primary);
  background: transparent;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.code-block code {
  font-family: inherit;
}

.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-icon {
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

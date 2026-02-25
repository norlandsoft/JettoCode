<template>
  <div class="code-view">
    <aside class="file-tree">
      <div class="tree-header">
        <FolderOutlined />
        <span>文件浏览器</span>
      </div>
      <div class="tree-content">
        <a-tree
          v-if="fileTree.length > 0"
          :tree-data="fileTree as any"
          :field-names="{ children: 'children', title: 'name', key: 'path' }"
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
          <p>暂无文件</p>
        </div>
      </div>
    </aside>
    
    <main class="code-content">
      <header class="content-header" v-if="currentFile">
        <div class="file-info">
          <FileOutlined />
          <span class="file-path code-text">{{ currentFile }}</span>
        </div>
      </header>
      <div class="editor-placeholder" v-if="currentFile">
        <CodeOutlined />
        <p>代码编辑器</p>
        <span class="hint">Monaco Editor 集成位置</span>
      </div>
      <div class="placeholder" v-else>
        <div class="empty-icon">
          <FileSearchOutlined />
        </div>
        <h3>选择文件查看代码</h3>
        <p>从左侧文件树中选择一个文件开始浏览</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FileNode } from '@/types'
import { FolderOutlined, FileOutlined, FileSearchOutlined, CodeOutlined } from '@ant-design/icons-vue'

const fileTree = ref<FileNode[]>([])
const currentFile = ref('')

const handleNodeSelect = (_selectedKeys: string[], info: { node: FileNode }) => {
  if (info.node.type === 'file') {
    currentFile.value = info.node.path
  }
}
</script>

<style scoped>
.code-view {
  display: flex;
  height: 100%;
  gap: var(--spacing-base);
}

.file-tree {
  width: 260px;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.tree-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.tree-content {
  flex: 1;
  padding: var(--spacing-sm);
  overflow-y: auto;
}

.custom-tree {
  background: transparent !important;
}

.custom-tree :deep(.ant-tree-node-content-wrapper) {
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.custom-tree :deep(.ant-tree-node-content-wrapper:hover) {
  background: var(--color-bg-tertiary);
}

.custom-tree :deep(.ant-tree-node-selected) {
  background: var(--color-info-bg) !important;
}

.custom-tree :deep(.ant-tree-switcher) {
  color: var(--color-text-tertiary);
}

.custom-tree :deep(.ant-tree-iconEle) {
  color: var(--color-accent-primary);
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
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.content-header {
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
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

.editor-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  text-align: center;
}

.editor-placeholder > .anticon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.editor-placeholder p {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.editor-placeholder .hint {
  margin-top: var(--spacing-sm);
  font-size: var(--font-size-sm);
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

@media (max-width: 768px) {
  .code-view {
    flex-direction: column;
  }
  
  .file-tree {
    width: 100%;
    height: 200px;
  }
}
</style>

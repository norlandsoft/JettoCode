<template>
  <div class="platform-management">
    <div class="page-header">
      <div class="header-left">
        <h1>平台管理</h1>
        <p>系统配置与规则管理</p>
      </div>
    </div>

    <div class="content">
      <!-- 左侧设置列表 -->
      <div class="settings-sidebar">
        <div class="sidebar-title">设置项</div>
        <div class="sidebar-menu">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['sidebar-menu-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            <span class="menu-text">{{ tab.label }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="settings-content">
        <!-- 质量检查项内容 -->
        <div class="content-panel quality-check-panel" v-if="activeTab === 'quality-check'">
          <!-- 左侧：检查项列表 -->
          <div class="check-categories">
            <FileTree
              :tree-data="checkTreeData"
              title="检查类别"
              :show-header="true"
              :show-search="false"
              :show-line="false"
              :merge-single-folder="false"
              :default-expanded-keys="defaultExpandedKeys"
              @select="handleTreeSelect"
            />
          </div>

          <!-- 右侧：代码编辑器 -->
          <div class="check-editor">
            <div class="editor-header">
              <span class="editor-title">{{ currentItemLabel }}</span>
              <div class="editor-actions">
                <button class="btn-icon" @click="saveCheckConfig" title="保存">
                  <SaveOutlined />
                </button>
                <button class="btn-icon" @click="resetCheckConfig" title="重置">
                  <ReloadOutlined />
                </button>
              </div>
            </div>
            <div class="editor-container">
              <div ref="checkEditorRef" class="monaco-editor"></div>
            </div>
          </div>
        </div>

        <!-- 其他设置项（开发中） -->
        <div class="content-panel" v-else>
          <div class="coming-soon">
            <SettingOutlined class="icon" />
            <p>功能开发中...</p>
          </div>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="showCreateModal"
      :title="editingItem ? '编辑检查项' : '新建检查项'"
      width="700px"
      @ok="handleSubmit"
      @cancel="closeModal"
      :confirmLoading="submitting"
      okText="保存"
      cancelText="取消"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="规则ID" required>
          <a-input v-model:value="formData.ruleId" placeholder="如：SEC001" :disabled="!!editingItem" />
        </a-form-item>
        <a-form-item label="规则名称" required>
          <a-input v-model:value="formData.ruleName" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="问题分类" required>
          <a-select v-model:value="formData.category" placeholder="请选择问题分类">
            <a-select-option value="SECURITY">安全问题</a-select-option>
            <a-select-option value="RELIABILITY">可靠性问题</a-select-option>
            <a-select-option value="MAINTAINABILITY">可维护性问题</a-select-option>
            <a-select-option value="CODE_SMELL">代码异味</a-select-option>
            <a-select-option value="PERFORMANCE">性能问题</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="严重程度" required>
          <a-select v-model:value="formData.severity" placeholder="请选择严重程度">
            <a-select-option value="BLOCKER">阻塞</a-select-option>
            <a-select-option value="CRITICAL">严重</a-select-option>
            <a-select-option value="MAJOR">主要</a-select-option>
            <a-select-option value="MINOR">次要</a-select-option>
            <a-select-option value="INFO">提示</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" placeholder="请输入规则描述" :rows="2" />
        </a-form-item>
        <a-form-item label="提示词模版" required>
          <a-textarea
            v-model:value="formData.promptTemplate"
            placeholder="请输入用于智能检查的提示词模版，可使用 {code}, {file}, {line} 等变量"
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formData.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="formData.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showPromptModal"
      :title="promptItem?.ruleName + ' - 提示词模版'"
      width="700px"
      :footer="null"
    >
      <div class="prompt-content">
        <pre>{{ promptItem?.promptTemplate }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, shallowRef, watch, nextTick, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import * as monaco from 'monaco-editor'
import {
  SaveOutlined,
  ReloadOutlined,
  SettingOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import { qualityCheckApi, type QualityCheckTreeDTO, type QualityCheckConfig } from '@/api/project'
import FileTree from '@/components/FileTree.vue'
import type { FileNode } from '@/types'

// 质量检查原始数据（从 API 加载）
interface CheckItem {
  id: number
  key: string
  label: string
  promptTemplate: string
}

interface CheckCategory {
  key: string
  label: string
  items: CheckItem[]
}

const qualityCategories = ref<CheckCategory[]>([])
const loading = ref(false)
const saving = ref(false)

// 配置数据（从 API 加载后缓存）
const checkConfigs = ref<Record<string, string>>({})
const defaultConfigs = ref<Record<string, string>>({})

// 当前选中
const selectedItem = ref<string>('')
const selectedItemId = ref<number | null>(null)
const checkEditorRef = ref<HTMLElement | null>(null)
const checkEditor = shallowRef<monaco.editor.IStandaloneCodeEditor | null>(null)

// 保存 itemKey 到 id 的映射
const itemIdMap = ref<Record<string, number>>({})

// 将 API 数据转换为 FileTree 格式
const checkTreeData = computed<FileNode[]>(() => {
  return qualityCategories.value.map(category => ({
    name: category.label,
    path: category.key,
    type: 'directory' as const,
    children: category.items.map(item => ({
      name: item.label,
      path: `${category.key}/${item.key}`,
      type: 'file' as const
    }))
  }))
})

// 默认展开的 keys
const defaultExpandedKeys = ref<string[]>([])

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const response = await qualityCheckApi.getTree()
    const treeData = response.data.data || []
    const categories: CheckCategory[] = []
    const configs: Record<string, string> = {}
    const defaults: Record<string, string> = {}
    const idMap: Record<string, number> = {}

    for (const group of treeData) {
      const items: CheckItem[] = (group.items || []).map(item => ({
        id: item.id,
        key: item.itemKey,
        label: item.itemName,
        promptTemplate: item.promptTemplate || ''
      }))

      categories.push({
        key: group.groupKey,
        label: group.groupName,
        items
      })

      // 缓存配置和 ID 映射
      for (const item of items) {
        configs[item.key] = item.promptTemplate
        defaults[item.key] = item.promptTemplate
        idMap[item.key] = item.id
      }

      // 默认展开第一个分组并选中第一个项目
      if (categories.length === 1 && items.length > 0) {
        defaultExpandedKeys.value = [group.groupKey]
        selectedItem.value = items[0].key
        selectedItemId.value = items[0].id
      }
    }

    qualityCategories.value = categories
    checkConfigs.value = configs
    defaultConfigs.value = defaults
    itemIdMap.value = idMap
  } catch (error) {
    console.error('加载配置失败:', error)
    message.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 处理树节点选择
const handleTreeSelect = (node: FileNode, selected: boolean) => {
  if (node.type === 'file' && selected) {
    // 从 path 中提取 itemKey (格式: groupKey/itemKey)
    const parts = node.path.split('/')
    if (parts.length === 2) {
      const itemKey = parts[1]
      selectedItem.value = itemKey
      selectedItemId.value = itemIdMap.value[itemKey] || null
    }
  }
}

const currentItemLabel = computed(() => {
  for (const cat of qualityCategories.value) {
    const item = cat.items.find(i => i.key === selectedItem.value)
    if (item) return item.label
  }
  return ''
})

// 初始化检查配置编辑器
const initCheckEditor = async () => {
  if (!checkEditorRef.value) {
    console.warn('Editor ref not ready')
    return
  }

  // 销毁旧编辑器
  if (checkEditor.value) {
    checkEditor.value.dispose()
    checkEditor.value = null
  }

  try {
    checkEditor.value = monaco.editor.create(checkEditorRef.value, {
      value: checkConfigs.value[selectedItem.value] || '',
      language: 'markdown',
      theme: 'vs',
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      lineNumbers: 'on',
      fontSize: 13,
      fontFamily: "'JetBrains Mono', 'Consolas', 'Monaco', monospace",
      automaticLayout: true,
      wordWrap: 'on',
      folding: true,
      scrollbar: {
        verticalScrollbarSize: 10,
        horizontalScrollbarSize: 10
      }
    })
  } catch (error) {
    console.error('Failed to init editor:', error)
  }
}

// 保存配置
const saveCheckConfig = async () => {
  if (!checkEditor.value || !selectedItemId.value) return

  const content = checkEditor.value.getValue()
  saving.value = true

  try {
    await qualityCheckApi.updateConfig(selectedItemId.value, { promptTemplate: content })
    checkConfigs.value[selectedItem.value] = content
    message.success('配置已保存')
  } catch (error) {
    console.error('保存失败:', error)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 重置配置
const resetCheckConfig = () => {
  if (checkEditor.value && defaultConfigs.value[selectedItem.value]) {
    checkEditor.value.setValue(defaultConfigs.value[selectedItem.value])
    message.success('配置已重置')
  }
}

// 监听检查项切换
watch(selectedItem, () => {
  nextTick(() => {
    if (checkEditor.value) {
      checkEditor.value.setValue(checkConfigs.value[selectedItem.value] || '')
    }
  })
})

const tabs = [
  { key: 'quality-check', label: '质量检查项' },
  { key: 'vulnerability-rule', label: '漏洞规则' },
  { key: 'system-config', label: '系统配置' }
]

const activeTab = ref('quality-check')

// 监听 tab 切换，初始化编辑器
watch(activeTab, (tab) => {
  if (tab === 'quality-check') {
    nextTick(() => {
      initCheckEditor()
    })
  }
})

const submitting = ref(false)
const showCreateModal = ref(false)
const showPromptModal = ref(false)

interface QualityCheckItem {
  id: number
  category: string
  ruleId: string
  ruleName: string
  severity: string
  description: string
  promptTemplate: string
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

const editingItem = ref<QualityCheckItem | null>(null)
const promptItem = ref<QualityCheckItem | null>(null)

const formData = ref({
  ruleId: '',
  ruleName: '',
  category: 'SECURITY',
  severity: 'MAJOR',
  description: '',
  promptTemplate: '',
  enabled: true,
  sortOrder: 0
})

const handleSubmit = async () => {
  if (!formData.value.ruleId || !formData.value.ruleName || !formData.value.promptTemplate) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    // TODO: 实现 API 调用
    message.success('保存成功')
    closeModal()
  } catch (error: any) {
    console.error(error)
    message.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const closeModal = () => {
  showCreateModal.value = false
  editingItem.value = null
  formData.value = {
    ruleId: '',
    ruleName: '',
    category: 'SECURITY',
    severity: 'MAJOR',
    description: '',
    promptTemplate: '',
    enabled: true,
    sortOrder: 0
  }
}

onMounted(async () => {
  await loadData()
  nextTick(() => {
    initCheckEditor()
  })
})

onUnmounted(() => {
  if (checkEditor.value) {
    checkEditor.value.dispose()
    checkEditor.value = null
  }
})
</script>

<style scoped>
.platform-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
}

.page-header {
  height: 72px;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-xl);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
}

.header-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.header-left h1 {
  margin: 0 0 var(--spacing-xs);
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-left p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 左右布局容器 */
.content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧设置列表 */
.settings-sidebar {
  width: 180px;
  flex-shrink: 0;
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-base);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  flex: 1;
  padding: 0 var(--spacing-sm);
}

.sidebar-menu-item {
  padding: var(--spacing-sm) var(--spacing-base);
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
}

.sidebar-menu-item:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.sidebar-menu-item.active {
  background: var(--color-info-bg);
  color: var(--color-accent-primary);
  font-weight: 500;
}

.sidebar-menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 20px;
  background: var(--color-accent-primary);
  border-radius: 0 2px 2px 0;
}

.sidebar-menu-item {
  position: relative;
}

/* 右侧内容区域 */
.settings-content {
  flex: 1;
  padding: var(--spacing-sm);
  overflow-y: auto;
}

.content-panel {
  height: 100%;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-base);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 质量检查面板 - 左右布局 */
.quality-check-panel {
  flex-direction: row;
  padding: 0;
}

.check-categories {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  background: var(--color-bg-tertiary);
}

/* 覆盖 FileTree 组件的 header 高度 */
.check-categories :deep(.file-tree-header) {
  height: 40px;
  padding: 0 var(--spacing-base);
}

.check-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-header {
  height: 40px;
  padding: 0 var(--spacing-base);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.editor-title {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.editor-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.editor-container {
  flex: 1;
  min-height: 0;
}

.editor-container .monaco-editor {
  height: 100%;
  width: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
  flex-shrink: 0;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-accent-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
}

.btn-primary:hover {
  background: var(--color-accent-primary-hover);
}

.filter-bar {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-base);
  flex-shrink: 0;
}

.check-items-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.check-item {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border-light);
}

.item-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.item-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.severity-badge {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
}

.severity-badge.blocker { background: #b60200; color: white; }
.severity-badge.critical { background: #d73a49; color: white; }
.severity-badge.major { background: #e36209; color: white; }
.severity-badge.minor { background: #fbca04; color: black; }
.severity-badge.info { background: #28a745; color: white; }

.category-badge {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

.category-badge.security { background: #ffdce0; color: #d73a49; }
.category-badge.reliability { background: #fff5e5; color: #e36209; }
.category-badge.maintainability { background: #fff9c4; color: #f9a825; }
.category-badge.code_smell { background: #f3e5f5; color: #6f42c1; }
.category-badge.performance { background: #e3f2fd; color: #1976d2; }

.rule-id {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.btn-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-icon:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.btn-icon.danger:hover {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.item-body {
  padding: var(--spacing-base);
}

.item-title {
  margin: 0 0 var(--spacing-xs);
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.item-description {
  margin: 0 0 var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.prompt-preview {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-accent-primary);
  cursor: pointer;
}

.prompt-preview:hover {
  text-decoration: underline;
}

.empty-state,
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-tertiary);
}

.empty-icon,
.icon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.loading-state {
  gap: var(--spacing-base);
}

.coming-soon {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-tertiary);
}

.coming-soon .icon {
  font-size: 64px;
}

.prompt-content {
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
  max-height: 400px;
  overflow-y: auto;
}

.prompt-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}
</style>

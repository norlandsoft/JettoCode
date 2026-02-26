<template>
  <div class="platform-management">
    <div class="page-header">
      <div class="header-left">
        <h1>平台管理</h1>
        <p>系统配置与规则管理</p>
      </div>
    </div>

    <div class="content">
      <div class="tabs-container">
        <div class="tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>

      <div class="tab-content" v-show="activeTab === 'quality-check'">
        <div class="panel-header">
          <h2>质量扫描内容管理</h2>
          <button class="btn btn-primary" @click="showCreateModal = true">
            <PlusOutlined />
            <span>新建检查项</span>
          </button>
        </div>

        <div class="filter-bar">
          <a-select v-model:value="categoryFilter" style="width: 160px" placeholder="问题分类" allowClear>
            <a-select-option value="SECURITY">安全问题</a-select-option>
            <a-select-option value="RELIABILITY">可靠性问题</a-select-option>
            <a-select-option value="MAINTAINABILITY">可维护性问题</a-select-option>
            <a-select-option value="CODE_SMELL">代码异味</a-select-option>
            <a-select-option value="PERFORMANCE">性能问题</a-select-option>
          </a-select>
          <a-select v-model:value="severityFilter" style="width: 140px" placeholder="严重程度" allowClear>
            <a-select-option value="BLOCKER">阻塞</a-select-option>
            <a-select-option value="CRITICAL">严重</a-select-option>
            <a-select-option value="MAJOR">主要</a-select-option>
            <a-select-option value="MINOR">次要</a-select-option>
            <a-select-option value="INFO">提示</a-select-option>
          </a-select>
          <a-select v-model:value="enabledFilter" style="width: 120px" placeholder="状态" allowClear>
            <a-select-option :value="true">已启用</a-select-option>
            <a-select-option :value="false">已禁用</a-select-option>
          </a-select>
        </div>

        <div class="check-items-list" v-if="!loading && filteredItems.length > 0">
          <div v-for="item in filteredItems" :key="item.id" class="check-item">
            <div class="item-header">
              <div class="item-info">
                <span :class="['severity-badge', item.severity.toLowerCase()]">{{ item.severity }}</span>
                <span :class="['category-badge', item.category.toLowerCase()]">{{ getCategoryText(item.category) }}</span>
                <span class="rule-id">{{ item.ruleId }}</span>
              </div>
              <div class="item-actions">
                <a-switch
                  :checked="item.enabled"
                  @change="(checked: boolean) => toggleEnabled(item, checked)"
                  size="small"
                />
                <button class="btn-icon" @click="editItem(item)" title="编辑">
                  <EditOutlined />
                </button>
                <a-popconfirm
                  title="确定要删除此检查项吗？"
                  @confirm="deleteItem(item)"
                  okText="确定"
                  cancelText="取消"
                >
                  <button class="btn-icon danger" title="删除">
                    <DeleteOutlined />
                  </button>
                </a-popconfirm>
              </div>
            </div>
            <div class="item-body">
              <h3 class="item-title">{{ item.ruleName }}</h3>
              <p class="item-description" v-if="item.description">{{ item.description }}</p>
              <div class="prompt-preview" @click="viewPrompt(item)">
                <CodeOutlined />
                <span>查看提示词模版</span>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-state" v-if="!loading && filteredItems.length === 0">
          <FileSearchOutlined class="empty-icon" />
          <p>暂无检查项数据</p>
        </div>

        <div class="loading-state" v-if="loading">
          <a-spin />
          <span>加载中...</span>
        </div>
      </div>

      <div class="tab-content" v-show="activeTab !== 'quality-check'">
        <div class="coming-soon">
          <SettingOutlined class="icon" />
          <p>功能开发中...</p>
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
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CodeOutlined,
  FileSearchOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
import { qualityCheckApi } from '@/api/project'

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

const tabs = [
  { key: 'quality-check', label: '质量检查项' },
  { key: 'vulnerability-rule', label: '漏洞规则' },
  { key: 'system-config', label: '系统配置' }
]

const activeTab = ref('quality-check')
const items = ref<QualityCheckItem[]>([])
const loading = ref(false)
const submitting = ref(false)

const categoryFilter = ref<string | undefined>(undefined)
const severityFilter = ref<string | undefined>(undefined)
const enabledFilter = ref<boolean | undefined>(undefined)

const showCreateModal = ref(false)
const showPromptModal = ref(false)
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

const filteredItems = computed(() => {
  let result = items.value
  
  if (categoryFilter.value) {
    result = result.filter(i => i.category === categoryFilter.value)
  }
  if (severityFilter.value) {
    result = result.filter(i => i.severity === severityFilter.value)
  }
  if (enabledFilter.value !== undefined) {
    result = result.filter(i => i.enabled === enabledFilter.value)
  }
  
  return result
})

const loadItems = async () => {
  loading.value = true
  try {
    const { data } = await qualityCheckApi.getAll()
    items.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formData.value.ruleId || !formData.value.ruleName || !formData.value.promptTemplate) {
    message.warning('请填写必填项')
    return
  }
  
  submitting.value = true
  try {
    if (editingItem.value) {
      await qualityCheckApi.update(editingItem.value.id, formData.value)
      message.success('更新成功')
    } else {
      await qualityCheckApi.create(formData.value)
      message.success('创建成功')
    }
    closeModal()
    loadItems()
  } catch (error: any) {
    console.error(error)
    message.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const editItem = (item: QualityCheckItem) => {
  editingItem.value = item
  formData.value = {
    ruleId: item.ruleId,
    ruleName: item.ruleName,
    category: item.category,
    severity: item.severity,
    description: item.description || '',
    promptTemplate: item.promptTemplate,
    enabled: item.enabled,
    sortOrder: item.sortOrder || 0
  }
  showCreateModal.value = true
}

const deleteItem = async (item: QualityCheckItem) => {
  try {
    await qualityCheckApi.delete(item.id)
    message.success('删除成功')
    loadItems()
  } catch (error) {
    console.error(error)
    message.error('删除失败')
  }
}

const toggleEnabled = async (item: QualityCheckItem, enabled: boolean) => {
  try {
    await qualityCheckApi.updateEnabled(item.id, enabled)
    item.enabled = enabled
    message.success(enabled ? '已启用' : '已禁用')
  } catch (error) {
    console.error(error)
    message.error('操作失败')
  }
}

const viewPrompt = (item: QualityCheckItem) => {
  promptItem.value = item
  showPromptModal.value = true
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

const getCategoryText = (category: string) => {
  const texts: Record<string, string> = {
    SECURITY: '安全',
    RELIABILITY: '可靠性',
    MAINTAINABILITY: '可维护性',
    CODE_SMELL: '代码异味',
    PERFORMANCE: '性能'
  }
  return texts[category] || category
}

onMounted(() => {
  loadItems()
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

.content {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.tabs-container {
  margin-bottom: var(--spacing-base);
}

.tabs {
  display: flex;
  gap: var(--spacing-xs);
  background: var(--color-bg-secondary);
  padding: var(--spacing-xs);
  border-radius: var(--radius-md);
  width: fit-content;
}

.tab-item {
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.tab-item:hover {
  color: var(--color-text-primary);
}

.tab-item.active {
  background: var(--color-accent-primary);
  color: white;
}

.tab-content {
  flex: 1;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

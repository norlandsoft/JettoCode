<template>
  <div class="service-info-page">
    <div class="info-section">
      <div class="section-header">
        <h3 class="section-title">基本信息</h3>
        <button class="btn btn-secondary btn-sm" @click="handlePull" :disabled="pulling">
          <SyncOutlined v-if="pulling" class="animate-spin" />
          <SyncOutlined v-else />
          <span>{{ pulling ? '拉取中...' : '拉取代码' }}</span>
        </button>
      </div>
      <div class="info-grid" v-if="service">
        <div class="info-item">
          <span class="info-label">服务名称</span>
          <span class="info-value">{{ service.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Git 地址</span>
          <span class="info-value">{{ service.gitUrl }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">当前分支</span>
          <span class="info-value">{{ service.currentBranch || 'main' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">最近提交</span>
          <span class="info-value">{{ service.lastCommit || '-' }}</span>
        </div>
        <div class="info-item full-width">
          <span class="info-label">服务描述</span>
          <span class="info-value">{{ service.description || '暂无描述' }}</span>
        </div>
      </div>
    </div>

    <div class="version-section">
      <div class="section-header">
        <h3 class="section-title">版本列表</h3>
        <button class="btn btn-primary btn-sm" @click="showCreateModal = true">
          <PlusOutlined />
          <span>新建版本</span>
        </button>
      </div>

      <div class="version-list" v-if="!loading && versions.length > 0">
        <div 
          v-for="version in versions" 
          :key="version.id"
          class="version-card"
        >
          <div class="version-header">
            <span class="version-name">
              <TagOutlined />
              {{ version.version }}
            </span>
            <span class="version-time">{{ formatDate(version.createdAt) }}</span>
          </div>
          <div class="version-body">
            <div class="version-commit">
              <span class="commit-label">Commit:</span>
              <span class="commit-value">{{ version.commitId }}</span>
            </div>
            <div class="version-desc">{{ version.description || '暂无描述' }}</div>
          </div>
          <div class="version-actions">
            <a-dropdown :trigger="['click']">
              <button class="action-btn" @click.stop>
                <MoreOutlined />
              </button>
              <template #overlay>
                <a-menu @click="(e: any) => handleVersionMenuClick(e.key, version)">
                  <a-menu-item key="edit">
                    <EditOutlined />
                    <span>编辑</span>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" class="menu-item-danger">
                    <DeleteOutlined />
                    <span>删除</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!loading && versions.length === 0">
        <div class="empty-icon">
          <TagOutlined />
        </div>
        <h3>暂无版本</h3>
        <p>点击右上角的"新建版本"按钮开始</p>
      </div>

      <div class="loading-state" v-if="loading">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
    </div>

    <a-modal
      v-model:open="showCreateModal"
      :footer="null"
      :width="520"
      @cancel="resetForm"
    >
      <template #title>
        <div class="modal-title">
          <PlusOutlined />
          <span>新建版本</span>
        </div>
      </template>
      
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            版本号
            <span class="required">*</span>
          </label>
          <input 
            v-model="createForm.version"
            type="text"
            class="form-input"
            placeholder="例如: v1.0.0"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">
            Commit ID
            <span class="required">*</span>
          </label>
          <input 
            v-model="createForm.commitId"
            type="text"
            class="form-input"
            placeholder="请输入 Commit ID"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">版本描述</label>
          <textarea 
            v-model="createForm.description"
            class="form-textarea"
            placeholder="请输入版本描述"
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showCreateModal = false">
            取消
          </button>
          <button 
            class="btn btn-primary" 
            :disabled="!createForm.version || !createForm.commitId || creating"
            @click="handleCreate"
          >
            <LoadingOutlined v-if="creating" class="animate-spin" />
            <PlusOutlined v-else />
            {{ creating ? '创建中...' : '创建版本' }}
          </button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="showEditModal"
      :footer="null"
      :width="520"
      @cancel="resetEditForm"
    >
      <template #title>
        <div class="modal-title">
          <EditOutlined />
          <span>编辑版本</span>
        </div>
      </template>
      
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            版本号
            <span class="required">*</span>
          </label>
          <input 
            v-model="editForm.version"
            type="text"
            class="form-input"
            placeholder="例如: v1.0.0"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">
            Commit ID
            <span class="required">*</span>
          </label>
          <input 
            v-model="editForm.commitId"
            type="text"
            class="form-input"
            placeholder="请输入 Commit ID"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">版本描述</label>
          <textarea 
            v-model="editForm.description"
            class="form-textarea"
            placeholder="请输入版本描述"
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showEditModal = false">
            取消
          </button>
          <button 
            class="btn btn-primary" 
            :disabled="!editForm.version || !editForm.commitId || editing"
            @click="handleEdit"
          >
            <LoadingOutlined v-if="editing" class="animate-spin" />
            <SaveOutlined v-else />
            {{ editing ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="showDeleteModal"
      :footer="null"
      :width="400"
    >
      <template #title>
        <div class="modal-title text-error">
          <ExclamationCircleOutlined />
          <span>确认删除</span>
        </div>
      </template>
      
      <div class="confirm-content">
        <p>确定要删除版本 <strong>{{ versionToDelete?.version }}</strong> 吗？</p>
        <p class="warning-text">此操作不可恢复</p>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showDeleteModal = false">
            取消
          </button>
          <button class="btn btn-danger" @click="handleDelete">
            <DeleteOutlined />
            确认删除
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { versionApi, serviceApi } from '@/api/project'
import type { ServiceEntity, ServiceVersion, CreateVersionRequest } from '@/types'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SaveOutlined,
  TagOutlined,
  MoreOutlined,
  LoadingOutlined,
  ExclamationCircleOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const service = inject<Ref<ServiceEntity | null>>('service')
const refreshService = inject<() => void>('refreshService')

const versions = ref<ServiceVersion[]>([])
const loading = ref(false)
const pulling = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const creating = ref(false)
const editing = ref(false)
const versionToDelete = ref<ServiceVersion | null>(null)
const versionToEdit = ref<ServiceVersion | null>(null)

const createForm = ref<CreateVersionRequest>({
  serviceId: 0,
  version: '',
  commitId: '',
  description: ''
})

const editForm = ref({
  version: '',
  commitId: '',
  description: ''
})

const serviceId = ref(Number(route.params.serviceId))

const loadVersions = async () => {
  loading.value = true
  try {
    const { data } = await versionApi.getByServiceId(serviceId.value)
    versions.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const handlePull = async () => {
  if (!service?.value) return
  
  pulling.value = true
  try {
    await serviceApi.pull(service.value.id)
    message.success('代码拉取成功')
    refreshService?.()
  } catch (error) {
    console.error(error)
    message.error('代码拉取失败')
  } finally {
    pulling.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.value.version || !createForm.value.commitId) {
    message.warning('请填写必填项')
    return
  }

  creating.value = true
  try {
    createForm.value.serviceId = serviceId.value
    await versionApi.create(createForm.value)
    message.success('创建成功')
    showCreateModal.value = false
    resetForm()
    loadVersions()
  } catch (error) {
    console.error(error)
    message.error('创建失败')
  } finally {
    creating.value = false
  }
}

const handleVersionMenuClick = (key: string, version: ServiceVersion) => {
  if (key === 'edit') {
    versionToEdit.value = version
    editForm.value = {
      version: version.version,
      commitId: version.commitId,
      description: version.description || ''
    }
    showEditModal.value = true
  } else if (key === 'delete') {
    versionToDelete.value = version
    showDeleteModal.value = true
  }
}

const handleEdit = async () => {
  if (!versionToEdit.value || !editForm.value.version || !editForm.value.commitId) {
    message.warning('请填写必填项')
    return
  }

  editing.value = true
  try {
    await versionApi.update(versionToEdit.value.id, editForm.value)
    message.success('保存成功')
    showEditModal.value = false
    resetEditForm()
    loadVersions()
  } catch (error) {
    console.error(error)
    message.error('保存失败')
  } finally {
    editing.value = false
  }
}

const handleDelete = async () => {
  if (!versionToDelete.value) return
  
  try {
    await versionApi.delete(versionToDelete.value.id)
    message.success('删除成功')
    showDeleteModal.value = false
    versionToDelete.value = null
    loadVersions()
  } catch (error) {
    console.error(error)
    message.error('删除失败')
  }
}

const resetForm = () => {
  createForm.value = { serviceId: serviceId.value, version: '', commitId: '', description: '' }
}

const resetEditForm = () => {
  versionToEdit.value = null
  editForm.value = { version: '', commitId: '', description: '' }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadVersions()
})
</script>

<style scoped>
.service-info-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  overflow-y: auto;
}

.info-section,
.version-section {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-base) var(--spacing-lg);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border);
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-base) var(--spacing-lg);
  padding: var(--spacing-lg);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  word-break: break-all;
}

.btn-sm {
  padding: 4px 10px;
  font-size: var(--font-size-sm);
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
  padding: var(--spacing-lg);
}

.version-card {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-base);
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.version-card:hover {
  border-color: var(--color-border-active);
  box-shadow: var(--shadow-sm);
}

.version-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}

.version-name {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.version-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.version-body {
  flex: 1;
  min-width: 0;
}

.version-commit {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-xs);
}

.commit-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.commit-value {
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
  color: var(--color-accent-primary);
  background: var(--color-info-bg);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.version-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.version-actions {
  flex-shrink: 0;
}

.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-base);
  opacity: 0.5;
}

.empty-state h3 {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-xs);
}

.empty-state p {
  color: var(--color-text-tertiary);
  margin: 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-base);
}

.loading-state p {
  color: var(--color-text-tertiary);
  margin: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-md);
  font-weight: 600;
}

.text-error {
  color: var(--color-error);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
  padding-top: var(--spacing-base);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

.required {
  color: var(--color-error);
  margin-left: 2px;
}

.form-input,
.form-textarea {
  padding: 8px 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  font-family: var(--font-ui);
  transition: all var(--transition-fast);
  resize: none;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--color-accent-primary);
  box-shadow: 0 0 0 3px rgba(3, 102, 214, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: var(--color-text-tertiary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-base);
  margin-top: var(--spacing-base);
}

.confirm-content {
  text-align: center;
}

.confirm-content p {
  margin: 0 0 var(--spacing-sm);
  color: var(--color-text-secondary);
}

.warning-text {
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
}

.menu-item-danger {
  color: var(--color-error) !important;
}
</style>

<template>
  <div class="applications-page">
    <div class="page-header">
      <div class="header-left">
        <h1>应用系统</h1>
        <p>管理和分析您的代码仓库</p>
      </div>
      <button class="btn btn-primary" @click="showCreateModal = true">
        <PlusOutlined />
        <span>新建应用</span>
      </button>
    </div>

    <div class="content">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <AppstoreOutlined />
          </div>
          <div class="stat-body">
            <span class="stat-value">{{ applications.length }}</span>
            <span class="stat-label">应用总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <CloudServerOutlined />
          </div>
          <div class="stat-body">
            <span class="stat-value">{{ totalServices }}</span>
            <span class="stat-label">服务总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <HistoryOutlined />
          </div>
          <div class="stat-body">
            <span class="stat-value">{{ recentApplications }}</span>
            <span class="stat-label">近期更新</span>
          </div>
        </div>
      </div>

      <div class="applications-section" v-if="!loading && applications.length > 0">
        <div class="applications-grid">
          <div
            v-for="app in applications"
            :key="app.id"
            class="application-card"
          >
            <div class="card-header">
              <div class="app-icon">
                <AppstoreOutlined />
              </div>
              <div class="app-info">
                <span class="app-name" @click="goToApplication(app.id)">{{ app.name }}</span>
                <span class="app-meta">{{ serviceCounts[app.id] || 0 }} 服务 · {{ formatDate(app.updatedAt) }}</span>
              </div>
              <a-dropdown :trigger="['click']">
                <button class="card-menu-btn" @click.stop>
                  <MoreOutlined />
                </button>
                <template #overlay>
                  <a-menu @click="(e: any) => handleMenuClick(e.key, app)">
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

            <div class="card-body">
              <p class="app-description">{{ app.description || '暂无描述' }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!loading && applications.length === 0">
        <div class="empty-icon">
          <AppstoreAddOutlined />
        </div>
        <h3>暂无应用</h3>
        <p>点击右上角的"新建应用"按钮开始</p>
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
          <span>新建应用</span>
        </div>
      </template>
      
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            应用名称
            <span class="required">*</span>
          </label>
          <input 
            v-model="createForm.name"
            type="text"
            class="form-input"
            placeholder="请输入应用名称"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">应用描述</label>
          <textarea 
            v-model="createForm.description"
            class="form-textarea"
            placeholder="请输入应用描述"
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showCreateModal = false">
            取消
          </button>
          <button 
            class="btn btn-primary" 
            :disabled="!createForm.name || creating"
            @click="handleCreate"
          >
            <LoadingOutlined v-if="creating" class="animate-spin" />
            <PlusOutlined v-else />
            {{ creating ? '创建中...' : '创建应用' }}
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
          <span>编辑应用</span>
        </div>
      </template>
      
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            应用名称
            <span class="required">*</span>
          </label>
          <input 
            v-model="editForm.name"
            type="text"
            class="form-input"
            placeholder="请输入应用名称"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">应用描述</label>
          <textarea 
            v-model="editForm.description"
            class="form-textarea"
            placeholder="请输入应用描述"
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showEditModal = false">
            取消
          </button>
          <button 
            class="btn btn-primary" 
            :disabled="!editForm.name || editing"
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
        <p>确定要删除应用 <strong>{{ appToDelete?.name }}</strong> 吗？</p>
        <p class="warning-text">此操作将同时删除该应用下的所有服务，且不可恢复</p>
        
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
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { applicationApi, serviceApi } from '@/api/project'
import type { Application, CreateApplicationRequest } from '@/types'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  SaveOutlined,
  MoreOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  HistoryOutlined,
  AppstoreAddOutlined,
  LoadingOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const applications = ref<Application[]>([])
const serviceCounts = ref<Record<number, number>>({})
const loading = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const creating = ref(false)
const editing = ref(false)
const appToDelete = ref<Application | null>(null)
const appToEdit = ref<Application | null>(null)
const createForm = ref<CreateApplicationRequest>({
  name: '',
  description: ''
})
const editForm = ref<CreateApplicationRequest>({
  name: '',
  description: ''
})

const totalServices = computed(() => {
  return Object.values(serviceCounts.value).reduce((sum, count) => sum + count, 0)
})

const recentApplications = computed(() => {
  const weekAgo = dayjs().subtract(7, 'day')
  return applications.value.filter(a => dayjs(a.updatedAt).isAfter(weekAgo)).length
})

const loadApplications = async () => {
  loading.value = true
  try {
    const { data } = await applicationApi.getAll()
    applications.value = data.data
    for (const app of applications.value) {
      const { data: servicesData } = await serviceApi.getByApplicationId(app.id)
      serviceCounts.value[app.id] = servicesData.data.length
    }
  } catch (error) {
    console.error(error)
    message.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

const handleMenuClick = (key: string, app: Application) => {
  if (key === 'edit') {
    appToEdit.value = app
    editForm.value = {
      name: app.name,
      description: app.description || ''
    }
    showEditModal.value = true
  } else if (key === 'delete') {
    appToDelete.value = app
    showDeleteModal.value = true
  }
}

const handleCreate = async () => {
  if (!createForm.value.name) {
    message.warning('请输入应用名称')
    return
  }

  creating.value = true
  try {
    await applicationApi.create(createForm.value)
    message.success('创建成功')
    showCreateModal.value = false
    resetForm()
    loadApplications()
  } catch (error) {
    console.error(error)
    message.error('创建失败')
  } finally {
    creating.value = false
  }
}

const handleEdit = async () => {
  if (!appToEdit.value || !editForm.value.name) {
    message.warning('请输入应用名称')
    return
  }

  editing.value = true
  try {
    await applicationApi.update(appToEdit.value.id, editForm.value)
    message.success('保存成功')
    showEditModal.value = false
    resetEditForm()
    loadApplications()
  } catch (error) {
    console.error(error)
    message.error('保存失败')
  } finally {
    editing.value = false
  }
}

const handleDelete = async () => {
  if (!appToDelete.value) return
  
  try {
    await applicationApi.delete(appToDelete.value.id)
    message.success('删除成功')
    showDeleteModal.value = false
    appToDelete.value = null
    loadApplications()
  } catch (error) {
    console.error(error)
    message.error('删除失败')
  }
}

const goToApplication = (id: number) => {
  router.push(`/application/${id}/services`)
}

const resetForm = () => {
  createForm.value = { name: '', description: '' }
}

const resetEditForm = () => {
  appToEdit.value = null
  editForm.value = { name: '', description: '' }
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD HH:mm')
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.applications-page {
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
}

.applications-section {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-top: var(--spacing-lg);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--spacing-base);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-base);
  padding: var(--spacing-base);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.stat-card:hover {
  border-color: var(--color-border-active);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-info-bg);
  border-radius: var(--radius-md);
  font-size: 16px;
  color: var(--color-accent-primary);
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.applications-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-base);
}

.application-card {
  display: flex;
  flex-direction: column;
  height: 110px;
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.application-card:hover {
  border-color: var(--color-border-active);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border-light);
  height: 52px;
  flex-shrink: 0;
}

.app-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent-primary);
  border-radius: var(--radius-sm);
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

.app-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-name:hover {
  color: var(--color-accent-primary);
}

.app-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.card-menu-btn {
  width: 24px;
  height: 24px;
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

.card-menu-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.card-body {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-base);
  display: flex;
  align-items: center;
  overflow: hidden;
}

.app-description {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

@media (max-width: 768px) {
  .applications-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>

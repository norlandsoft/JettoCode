<template>
  <div class="services-page">
    <div class="services-grid" v-if="!loading && services.length > 0">
      <div 
        v-for="service in services" 
        :key="service.id"
        class="service-card"
      >
        <div class="card-header">
          <div class="service-icon">
            <CloudServerOutlined />
          </div>
          <div class="service-info">
            <span class="service-name" @click="goToService(service.id)">{{ service.name }}</span>
            <span class="service-meta">{{ service.currentBranch || 'main' }} · {{ formatDate(service.updatedAt) }}</span>
          </div>
          <a-dropdown :trigger="['click']">
            <button class="card-menu-btn" @click.stop>
              <MoreOutlined />
            </button>
            <template #overlay>
              <a-menu @click="(e: any) => handleMenuClick(e.key, service)">
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
          <p class="service-description">{{ service.description || service.gitUrl || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <div class="empty-state" v-if="!loading && services.length === 0">
      <div class="empty-icon">
        <CloudServerOutlined />
      </div>
      <h3>暂无服务</h3>
      <p>点击右上角的"添加服务"按钮开始</p>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <a-modal
      v-model:open="showEditModal"
      :footer="null"
      :width="520"
      @cancel="resetEditForm"
    >
      <template #title>
        <div class="modal-title">
          <EditOutlined />
          <span>编辑服务</span>
        </div>
      </template>
      
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            服务名称
            <span class="required">*</span>
          </label>
          <input 
            v-model="editForm.name"
            type="text"
            class="form-input"
            placeholder="请输入服务名称"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">
            Git 地址
            <span class="required">*</span>
          </label>
          <input 
            v-model="editForm.gitUrl"
            type="text"
            class="form-input"
            placeholder="https://github.com/user/repo.git"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">服务描述</label>
          <textarea 
            v-model="editForm.description"
            class="form-textarea"
            placeholder="请输入服务描述"
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showEditModal = false">
            取消
          </button>
          <button 
            class="btn btn-primary" 
            :disabled="!editForm.name || !editForm.gitUrl || editing"
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
        <p>确定要删除服务 <strong>{{ serviceToDelete?.name }}</strong> 吗？</p>
        <p class="warning-text">此操作将同时删除该服务下的所有版本，且不可恢复</p>
        
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
import { ref, onMounted, inject, watch, type Ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { serviceApi } from '@/api/project'
import type { ServiceEntity } from '@/types'
import { message } from 'ant-design-vue'
import {
  CloudServerOutlined,
  DeleteOutlined,
  EditOutlined,
  SaveOutlined,
  MoreOutlined,
  LoadingOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const applicationId = Number(route.params.id)

const services = ref<ServiceEntity[]>([])
const loading = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const editing = ref(false)
const serviceToDelete = ref<ServiceEntity | null>(null)
const serviceToEdit = ref<ServiceEntity | null>(null)
const editForm = ref({
  name: '',
  gitUrl: '',
  description: ''
})

const refreshServicesKey = inject<Ref<number>>('refreshServicesKey')

const loadServices = async () => {
  loading.value = true
  try {
    const { data } = await serviceApi.getByApplicationId(applicationId)
    services.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载服务列表失败')
  } finally {
    loading.value = false
  }
}

watch(refreshServicesKey!, () => {
  loadServices()
})

const handleMenuClick = (key: string, service: ServiceEntity) => {
  if (key === 'edit') {
    serviceToEdit.value = service
    editForm.value = {
      name: service.name,
      gitUrl: service.gitUrl,
      description: service.description || ''
    }
    showEditModal.value = true
  } else if (key === 'delete') {
    serviceToDelete.value = service
    showDeleteModal.value = true
  }
}

const handleEdit = async () => {
  if (!serviceToEdit.value || !editForm.value.name || !editForm.value.gitUrl) {
    message.warning('请填写必填项')
    return
  }

  editing.value = true
  try {
    await serviceApi.update(serviceToEdit.value.id, editForm.value)
    message.success('保存成功')
    showEditModal.value = false
    resetEditForm()
    loadServices()
  } catch (error) {
    console.error(error)
    message.error('保存失败')
  } finally {
    editing.value = false
  }
}

const handleDelete = async () => {
  if (!serviceToDelete.value) return
  
  try {
    await serviceApi.delete(serviceToDelete.value.id)
    message.success('删除成功')
    showDeleteModal.value = false
    serviceToDelete.value = null
    loadServices()
  } catch (error) {
    console.error(error)
    message.error('删除失败')
  }
}

const goToService = (serviceId: number) => {
  router.push(`/application/${applicationId}/service/${serviceId}/info`)
}

const resetEditForm = () => {
  serviceToEdit.value = null
  editForm.value = { name: '', gitUrl: '', description: '' }
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD HH:mm')
}

onMounted(() => {
  loadServices()
})
</script>

<style scoped>
.services-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: var(--spacing-lg);
  overflow-y: auto;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-base);
  flex: 1;
}

.service-card {
  display: flex;
  flex-direction: column;
  height: 110px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.service-card:hover {
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

.service-icon {
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

.service-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.service-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-name:hover {
  color: var(--color-accent-primary);
}

.service-meta {
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

.service-description {
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
  flex: 1;
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
  .services-page {
    padding: var(--spacing-base);
  }
  
  .services-grid {
    grid-template-columns: 1fr;
  }
}
</style>

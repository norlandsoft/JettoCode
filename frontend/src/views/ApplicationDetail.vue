<template>
  <div class="application-detail">
    <div class="detail-header" v-if="application">
      <div class="header-left">
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="goToApplications">应用系统</span>
          <span class="breadcrumb-separator">/</span>
          <template v-if="isServicePage">
            <span class="breadcrumb-item" @click="goToApplicationServices">{{ application.name }}</span>
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-item active">{{ service?.name || '加载中...' }}</span>
          </template>
          <template v-else>
            <span class="breadcrumb-item active">{{ application.name }}</span>
          </template>
        </div>
      </div>
      <div class="header-right" v-if="!isServicePage">
        <button class="btn btn-primary" @click="showAddServiceModal = true">
          <PlusOutlined />
          <span>添加服务</span>
        </button>
      </div>
    </div>

    <div class="detail-content">
      <router-view :application-id="applicationId" />
    </div>

    <Dialog
      v-model:open="showAddServiceModal"
      title="添加服务"
      :width="520"
      okText="添加服务"
      cancelText="取消"
      :confirmLoading="creating"
      @ok="handleAddService"
      @cancel="resetServiceForm"
    >
      <div class="form">
        <div class="form-group">
          <label class="form-label">
            服务名称
            <span class="required">*</span>
          </label>
          <input
            v-model="serviceForm.name"
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
            v-model="serviceForm.gitUrl"
            type="text"
            class="form-input"
            placeholder="https://github.com/user/repo.git"
          />
        </div>

        <div class="form-group">
          <label class="form-label">服务描述</label>
          <textarea
            v-model="serviceForm.description"
            class="form-textarea"
            placeholder="请输入服务描述"
            rows="3"
          ></textarea>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, provide, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { applicationApi, serviceApi } from '@/api/project'
import type { Application, CreateServiceRequest, ServiceEntity } from '@/types'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import Dialog from '@/components/Dialog.vue'

const router = useRouter()
const route = useRoute()
const applicationId = Number(route.params.id)

const application = ref<Application | null>(null)
const service = ref<ServiceEntity | null>(null)
const showAddServiceModal = ref(false)
const creating = ref(false)
const serviceForm = ref<CreateServiceRequest>({
  applicationId: applicationId,
  name: '',
  gitUrl: '',
  description: ''
})

const refreshServicesKey = ref(0)

provide('application', application)
provide('service', service)
provide('refreshServicesKey', refreshServicesKey)

const isServicePage = computed(() => !!route.params.serviceId)

const loadApplication = async () => {
  try {
    const { data } = await applicationApi.getById(applicationId)
    application.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载应用信息失败')
  }
}

const loadService = async () => {
  const serviceId = route.params.serviceId
  if (!serviceId) {
    service.value = null
    return
  }
  
  if (Number(serviceId) === service.value?.id) return
  
  try {
    const { data } = await serviceApi.getById(Number(serviceId))
    service.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载服务信息失败')
  }
}

const refreshService = async () => {
  const serviceId = route.params.serviceId
  if (!serviceId) return
  
  try {
    const { data } = await serviceApi.getById(Number(serviceId))
    service.value = data.data
  } catch (error) {
    console.error(error)
  }
}

watch(() => route.params.serviceId, loadService, { immediate: true })

const handleAddService = async () => {
  if (!serviceForm.value.name || !serviceForm.value.gitUrl) {
    message.warning('请填写必填项')
    return
  }

  creating.value = true
  try {
    await serviceApi.create(serviceForm.value)
    message.success('添加成功')
    showAddServiceModal.value = false
    resetServiceForm()
    refreshServicesKey.value++
  } catch (error) {
    console.error(error)
    message.error('添加失败')
  } finally {
    creating.value = false
  }
}

const resetServiceForm = () => {
  serviceForm.value = {
    applicationId: applicationId,
    name: '',
    gitUrl: '',
    description: ''
  }
}

const goToApplications = () => {
  router.push('/applications')
}

const goToApplicationServices = () => {
  router.push(`/application/${applicationId}/services`)
}

provide('refreshService', refreshService)

onMounted(() => {
  loadApplication()
})
</script>

<style scoped>
.application-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-lg);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.detail-content {
  flex: 1;
  overflow: hidden;
}

.header-left {
  display: flex;
  align-items: center;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 16px;
}

.breadcrumb-item {
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.breadcrumb-item:hover {
  color: var(--color-accent-primary);
}

.breadcrumb-item.active {
  color: var(--color-text-primary);
  font-weight: 600;
  cursor: default;
}

.breadcrumb-separator {
  color: var(--color-text-tertiary);
}

.header-right {
  display: flex;
  gap: var(--spacing-sm);
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
</style>

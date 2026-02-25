<template>
  <div class="service-detail">
    <div class="content-container">
      <div class="tabs-nav">
        <button 
          v-for="tab in tabs" 
          :key="tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
          @click="handleTabChange(tab.key)"
        >
          <component :is="tab.icon" />
          <span>{{ tab.label }}</span>
        </button>
      </div>
      
      <div class="tab-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { serviceApi } from '@/api/project'
import type { ServiceEntity } from '@/types'
import { message } from 'ant-design-vue'
import {
  InfoCircleOutlined,
  FolderOutlined,
  ApartmentOutlined,
  ShareAltOutlined,
  NodeIndexOutlined,
  ApiOutlined,
  DiffOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const service = ref<ServiceEntity | null>(null)
const activeTab = ref('info')

const tabs = [
  { key: 'info', label: '服务信息', icon: InfoCircleOutlined },
  { key: 'code', label: '代码浏览', icon: FolderOutlined },
  { key: 'architecture', label: '项目架构', icon: ApartmentOutlined },
  { key: 'flow', label: '业务流程', icon: ShareAltOutlined },
  { key: 'call-chain', label: '调用链路', icon: NodeIndexOutlined },
  { key: 'interface-trace', label: '接口追踪', icon: ApiOutlined },
  { key: 'diff', label: 'Diff分析', icon: DiffOutlined }
]

const applicationId = computed(() => Number(route.params.id))
const serviceId = computed(() => Number(route.params.serviceId))

const loadService = async () => {
  try {
    const { data } = await serviceApi.getById(serviceId.value)
    service.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载服务信息失败')
  }
}

const handleTabChange = (key: string) => {
  activeTab.value = key
  router.push(`/application/${applicationId.value}/service/${serviceId.value}/${key}`)
}

onMounted(() => {
  loadService()
  
  const pathParts = route.path.split('/')
  const lastPart = pathParts[pathParts.length - 1]
  const tabKeys = tabs.map(t => t.key)
  if (tabKeys.includes(lastPart)) {
    activeTab.value = lastPart
  } else {
    activeTab.value = 'info'
  }
})
</script>

<style scoped>
.service-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
}

.content-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-secondary);
  overflow: hidden;
}

.tabs-nav {
  display: flex;
  gap: 2px;
  padding: 0 var(--spacing-base);
  height: 36px;
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
  align-items: flex-end;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  height: 32px;
}

.tab-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.tab-btn.active {
  background: var(--color-bg-secondary);
  color: var(--color-accent-primary);
  box-shadow: inset 1px 1px 0 var(--color-border), inset -1px 1px 0 var(--color-border);
}

.tab-content {
  flex: 1;
  overflow: auto;
  padding: 0;
}

@media (max-width: 768px) {
  .tabs-nav {
    padding: 0 var(--spacing-sm);
  }
  
  .tab-btn {
    padding: 4px 8px;
    font-size: var(--font-size-xs);
  }
  
  .tab-btn span {
    display: none;
  }
}
</style>

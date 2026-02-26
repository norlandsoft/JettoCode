<template>
  <div class="layout-container">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo-container">
          <div class="logo-icon">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="4" width="56" height="56" rx="12" fill="#0366d6"/>
              <g fill="white">
                <rect x="14" y="16" width="8" height="8" rx="1.5"/>
                <rect x="14" y="28" width="8" height="8" rx="1.5"/>
                <rect x="14" y="40" width="8" height="8" rx="1.5"/>
                <rect x="28" y="16" width="22" height="8" rx="1.5" opacity="0.9"/>
                <rect x="28" y="28" width="22" height="8" rx="1.5" opacity="0.75"/>
                <rect x="28" y="40" width="16" height="8" rx="1.5" opacity="0.6"/>
              </g>
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-title">JettoCode</span>
            <span class="logo-subtitle">智能代码分析</span>
          </div>
        </div>
      </div>
      
      <nav class="sidebar-nav">
        <div class="nav-section">
          <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="inline"
            class="sidebar-menu"
            :inline-collapsed="isCollapsed"
            @click="handleMenuClick"
          >
            <a-menu-item key="/applications" class="nav-item">
              <template #icon>
                <AppstoreOutlined />
              </template>
              <span>应用系统</span>
            </a-menu-item>
            <a-menu-item key="/supply-chain" class="nav-item">
              <template #icon>
                <SafetyOutlined />
              </template>
              <span>供应链安全</span>
            </a-menu-item>
            <a-menu-item key="/code-quality" class="nav-item">
              <template #icon>
                <CodeOutlined />
              </template>
              <span>代码质量</span>
            </a-menu-item>
          </a-menu>
        </div>
      </nav>
      
      <div class="sidebar-footer">
        <div class="status-indicator">
          <span class="status-dot"></span>
          <span class="status-text">系统运行中</span>
        </div>
        <button class="collapse-btn" @click="toggleCollapse" :title="isCollapsed ? '展开' : '折叠'">
          <MenuFoldOutlined v-if="!isCollapsed" />
          <MenuUnfoldOutlined v-else />
        </button>
      </div>
    </aside>
    
    <main class="main-content">
      <div class="content-wrapper animate-fade-in">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined, MenuFoldOutlined, MenuUnfoldOutlined, SafetyOutlined, CodeOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const selectedKeys = ref<string[]>(['/applications'])
const isCollapsed = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/application/')) {
    return '/applications'
  }
  if (path.includes('/supply-chain')) {
    return '/supply-chain'
  }
  if (path.includes('/code-quality')) {
    return '/code-quality'
  }
  return path
})

watch(activeMenu, (val) => {
  selectedKeys.value = [val]
}, { immediate: true })

const handleMenuClick = (info: { key: string | number }) => {
  router.push(String(info.key))
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--color-bg-primary);
}

.sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border);
  position: relative;
  z-index: 100;
  transition: width var(--transition-normal);
}

.sidebar.collapsed {
  width: 72px;
}

.sidebar.collapsed .logo-text,
.sidebar.collapsed .status-text {
  display: none;
}

.sidebar.collapsed .sidebar-header {
  width: 72px;
}

.sidebar.collapsed .sidebar-nav {
  padding: 16px 8px 16px 16px;
}

.sidebar.collapsed .sidebar-menu :deep(.ant-menu-item) {
  width: 40px;
  height: 40px;
  padding: 0 !important;
  margin: 0 0 4px 0 !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar.collapsed .sidebar-menu :deep(.ant-menu-item .anticon) {
  margin-right: 0 !important;
}

.sidebar.collapsed .sidebar-menu :deep(.ant-menu-item span:not(.anticon)) {
  display: none !important;
}

.sidebar.collapsed .sidebar-footer {
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.sidebar.collapsed .status-indicator {
  justify-content: center;
  padding: var(--spacing-sm);
}

.sidebar-header {
  width: 220px;
  height: 72px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  position: relative;
}

.logo-container {
  position: absolute;
  top: 18px;
  left: 18px;
  display: flex;
  align-items: flex-start;
}

.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  display: flex;
  flex-direction: column;
  margin-left: 12px;
  margin-top: -4px;
}

.logo-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--color-text-primary);
}

.logo-subtitle {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.sidebar-nav {
  flex: 1;
  padding: 16px;
  overflow: hidden;
}

.nav-section {
  margin-bottom: var(--spacing-lg);
}

.sidebar-menu {
  background: transparent !important;
  border: none !important;
}

.sidebar-menu :deep(.ant-menu-item) {
  width: 188px;
  height: 40px;
  margin: 0 0 4px 0 !important;
  padding: 0 12px !important;
  line-height: 40px !important;
  border-radius: var(--radius-md) !important;
  color: var(--color-text-secondary) !important;
  background: transparent !important;
  transition: all var(--transition-fast) !important;
  display: flex;
  align-items: center;
}

.sidebar-menu :deep(.ant-menu-item:hover) {
  background: var(--color-bg-tertiary) !important;
  color: var(--color-text-primary) !important;
}

.sidebar-menu :deep(.ant-menu-item-selected) {
  background: var(--color-info-bg) !important;
  color: var(--color-accent-primary) !important;
  font-weight: 500;
}

.sidebar-menu :deep(.ant-menu-item-selected::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-accent-primary);
  border-radius: 0 2px 2px 0;
}

.sidebar-menu :deep(.ant-menu-item .anticon) {
  font-size: 16px !important;
  margin-right: 10px !important;
}

.sidebar-footer {
  padding: var(--spacing-base);
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-success-bg);
  border-radius: var(--radius-md);
}

.status-dot {
  width: 6px;
  height: 6px;
  background: var(--color-success);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.status-text {
  font-size: 12px;
  color: var(--color-success);
  font-weight: 500;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-primary);
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
}

@media (max-width: 768px) {
  .sidebar {
    width: 72px;
  }
  
  .sidebar .logo-text,
  .sidebar .status-text {
    display: none;
  }
  
  .sidebar-nav {
    padding: 16px 8px 16px 16px;
    overflow: hidden;
  }
  
  .sidebar-menu :deep(.ant-menu-item) {
    width: 40px;
    height: 40px;
    padding: 0 !important;
    margin: 0 0 4px 0 !important;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .sidebar-menu :deep(.ant-menu-item .anticon) {
    margin-right: 0 !important;
  }
  
  .sidebar-menu :deep(.ant-menu-item span:not(.anticon)) {
    display: none !important;
  }
  
  .sidebar-footer {
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-sm);
  }
  
  .sidebar .status-indicator {
    justify-content: center;
    padding: var(--spacing-sm);
  }
}
</style>

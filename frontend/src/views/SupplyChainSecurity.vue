<template>
  <div class="supply-chain-security">
    <div class="page-header">
      <h1>供应链安全</h1>
      <p>软件物料清单(SBOM)管理与安全扫描</p>
    </div>

    <div class="content">
      <div class="service-selector">
        <label>选择应用系统：</label>
        <a-select
          v-model:value="selectedApplicationId"
          style="width: 300px"
          placeholder="请选择应用系统"
          @change="handleApplicationChange"
        >
          <a-select-option v-for="app in applications" :key="app.id" :value="app.id">
            {{ app.name }}
          </a-select-option>
        </a-select>
      </div>

      <div v-if="selectedApplicationId" class="scan-section">
        <div class="section-header">
          <h2>安全扫描</h2>
          <button class="btn btn-primary" @click="showScanModal = true" :disabled="loadingServices || isScanning">
            <ScanOutlined />
            {{ isScanning ? '扫描中...' : '开始扫描' }}
          </button>
        </div>

        <div v-if="latestScan" class="scan-summary">
          <div class="summary-card">
            <div class="card-title">扫描概览</div>
            <div class="card-content">
              <div class="stat-item">
                <span class="stat-label">总依赖数</span>
                <span class="stat-value">{{ latestScan.totalDependencies }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">漏洞依赖数</span>
                <span class="stat-value danger">{{ latestScan.vulnerableDependencies }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">许可证违规</span>
                <span class="stat-value warning">{{ latestScan.licenseViolationCount }}</span>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">漏洞统计</div>
            <div class="card-content severity-stats">
              <div class="stat-item critical">
                <span class="stat-label">严重</span>
                <span class="stat-value">{{ latestScan.criticalCount }}</span>
              </div>
              <div class="stat-item high">
                <span class="stat-label">高危</span>
                <span class="stat-value">{{ latestScan.highCount }}</span>
              </div>
              <div class="stat-item medium">
                <span class="stat-label">中危</span>
                <span class="stat-value">{{ latestScan.mediumCount }}</span>
              </div>
              <div class="stat-item low">
                <span class="stat-label">低危</span>
                <span class="stat-value">{{ latestScan.lowCount }}</span>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">扫描信息</div>
            <div class="card-content">
              <div class="stat-item">
                <span class="stat-label">扫描状态</span>
                <span :class="['stat-value', latestScan.status === 'IN_PROGRESS' ? 'scanning' : 'status-' + latestScan.status.toLowerCase()]">
                  {{ getScanStatusText(latestScan.status) }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-label">扫描时间</span>
                <span class="stat-value">{{ formatDateTime(latestScan.completedAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="no-scan">
          <FileSearchOutlined class="empty-icon" />
          <p>暂无扫描记录，点击"开始扫描"进行安全检查</p>
        </div>
      </div>

      <div v-if="selectedApplicationId && dependencies.length > 0" class="sbom-section">
        <div class="section-header">
          <h2>软件物料清单 (SBOM)</h2>
          <div class="filter-group">
            <a-input-search
              v-model:value="searchText"
              placeholder="搜索依赖"
              style="width: 200px"
            />
            <a-select v-model:value="licenseFilter" style="width: 150px" placeholder="许可证状态">
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="APPROVED">已批准</a-select-option>
              <a-select-option value="VIOLATION">违规</a-select-option>
              <a-select-option value="UNKNOWN">未知</a-select-option>
            </a-select>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="filteredDependencies"
          :loading="loading"
          :pagination="{ pageSize: 20 }"
          row-key="id"
          class="dependency-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="dep-name">
                <span>{{ record.name }}</span>
                <span class="dep-version">{{ record.version }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'licenseStatus'">
              <a-tag :color="getLicenseStatusColor(record.licenseStatus)">
                {{ getLicenseStatusText(record.licenseStatus) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'vulnerabilities'">
              <a-button type="link" size="small" @click="showVulnerabilities(record)" :disabled="!record.vulnerabilityCount">
                {{ record.vulnerabilityCount || 0 }} 个漏洞
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <a-modal
      v-model:open="vulnerabilityModalVisible"
      title="漏洞详情"
      width="800px"
      :footer="null"
    >
      <div v-if="loadingVulnerabilities" class="loading-container">
        <a-spin />
      </div>
      <div v-else-if="currentVulnerabilities.length === 0" class="no-data">
        暂无漏洞信息
      </div>
      <div v-else class="vulnerability-list">
        <div v-for="vuln in currentVulnerabilities" :key="vuln.id" class="vulnerability-item">
          <div class="vuln-header">
            <span :class="['severity-badge', vuln.severity.toLowerCase()]">
              {{ vuln.severity }}
            </span>
            <span class="cve-id">{{ vuln.cveId }}</span>
            <span class="cvss-score">CVSS: {{ vuln.cvssScore }}</span>
          </div>
          <h4 class="vuln-title">{{ vuln.title }}</h4>
          <p class="vuln-description">{{ vuln.description }}</p>
          <div class="vuln-details">
            <div class="detail-item">
              <strong>受影响版本：</strong>{{ vuln.affectedVersion }}
            </div>
            <div class="detail-item">
              <strong>修复版本：</strong>{{ vuln.fixedVersion }}
            </div>
            <div v-if="vuln.references" class="detail-item references">
              <strong>参考链接：</strong>
              <div class="reference-links">
                <a
                  v-for="(ref, index) in parseReferences(vuln.references)"
                  :key="index"
                  :href="ref"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="reference-link"
                >
                  <LinkOutlined /> {{ formatReferenceUrl(ref) }}
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="showScanModal"
      title="选择扫描服务"
      width="500px"
      @ok="handleScan"
      @cancel="cancelScan"
      :confirmLoading="scanning"
      okText="开始扫描"
      cancelText="取消"
    >
      <div v-if="loadingServices" class="loading-container">
        <a-spin />
      </div>
      <div v-else-if="applicationServices.length === 0" class="no-data">
        该应用下暂无服务
      </div>
      <div v-else class="service-list">
        <a-checkbox-group v-model:value="selectedServiceIds" style="width: 100%">
          <div v-for="service in applicationServices" :key="service.id" class="service-item">
            <a-checkbox :value="service.id">
              {{ service.name }}
            </a-checkbox>
          </div>
        </a-checkbox-group>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Application, ServiceEntity, Dependency, Vulnerability, SecurityScan } from '@/types'
import { applicationApi, serviceApi, supplyChainApi } from '@/api/project'
import { ScanOutlined, LoadingOutlined, FileSearchOutlined, LinkOutlined } from '@ant-design/icons-vue'

const applications = ref<Application[]>([])
const selectedApplicationId = ref<number | null>(null)
const dependencies = ref<Dependency[]>([])
const latestScan = ref<SecurityScan | null>(null)
const loading = ref(false)
const scanning = ref(false)
const searchText = ref('')
const licenseFilter = ref('')
const vulnerabilityModalVisible = ref(false)
const currentVulnerabilities = ref<Vulnerability[]>([])
const loadingVulnerabilities = ref(false)

const showScanModal = ref(false)
const applicationServices = ref<ServiceEntity[]>([])
const loadingServices = ref(false)
const selectedServiceIds = ref<number[]>([])

const scanningScanIds = ref<number[]>([])
let progressPollingTimer: ReturnType<typeof setInterval> | null = null

const isScanning = computed(() => latestScan.value?.status === 'IN_PROGRESS')

const columns = [
  { title: '依赖名称', key: 'name', dataIndex: 'name', sorter: true },
  { title: '类型', dataIndex: 'type', width: 100 },
  { title: '许可证', dataIndex: 'license', width: 150 },
  { title: '许可证状态', key: 'licenseStatus', width: 120 },
  { title: '漏洞', key: 'vulnerabilities', width: 100 }
]

const filteredDependencies = computed(() => {
  let deps = dependencies.value
  
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    deps = deps.filter(d => 
      d.name.toLowerCase().includes(search) ||
      d.license?.toLowerCase().includes(search)
    )
  }
  
  if (licenseFilter.value) {
    deps = deps.filter(d => d.licenseStatus === licenseFilter.value)
  }
  
  return deps
})

const loadApplications = async () => {
  try {
    const { data } = await applicationApi.getAll()
    applications.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载应用系统列表失败')
  }
}

const loadApplicationServices = async () => {
  if (!selectedApplicationId.value) return
  
  loadingServices.value = true
  try {
    const { data } = await serviceApi.getByApplicationId(selectedApplicationId.value)
    applicationServices.value = data.data
    selectedServiceIds.value = []
  } catch (error) {
    console.error(error)
    message.error('加载服务列表失败')
  } finally {
    loadingServices.value = false
  }
}

const handleApplicationChange = async () => {
  if (!selectedApplicationId.value) return
  
  loading.value = true
  loadApplicationServices()
  try {
    const [depRes, scanRes] = await Promise.all([
      supplyChainApi.getDependenciesByApplication(selectedApplicationId.value),
      supplyChainApi.getLatestScanByApplication(selectedApplicationId.value)
    ])
    
    dependencies.value = depRes.data.data
    latestScan.value = scanRes.data.data
    
    if (scanRes.data.data?.status === 'IN_PROGRESS' && scanRes.data.data?.id) {
      scanningScanIds.value = [scanRes.data.data.id]
      startProgressPolling()
    }
  } catch (error) {
    console.error(error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleScan = async () => {
  if (selectedServiceIds.value.length === 0) {
    message.warning('请至少选择一个服务')
    return
  }
  
  scanning.value = true
  let successCount = 0
  let failCount = 0
  
  for (const serviceId of selectedServiceIds.value) {
    try {
      const { data } = await supplyChainApi.performScan(serviceId)
      if (data.data?.id) {
        scanningScanIds.value.push(data.data.id)
        if (!latestScan.value || latestScan.value.status !== 'IN_PROGRESS') {
          latestScan.value = data.data
        }
      }
      successCount++
    } catch (error) {
      console.error(error)
      failCount++
    }
  }
  
  scanning.value = false
  showScanModal.value = false
  
  if (successCount > 0) {
    message.success(`已启动 ${successCount} 个服务的扫描`)
    startProgressPolling()
  }
  if (failCount > 0) {
    message.error(`${failCount} 个服务扫描启动失败`)
  }
}

const startProgressPolling = () => {
  if (progressPollingTimer) {
    clearInterval(progressPollingTimer)
  }
  
  progressPollingTimer = setInterval(async () => {
    if (scanningScanIds.value.length === 0) {
      stopProgressPolling()
      return
    }
    
    const completedIds: number[] = []
    
    for (const scanId of scanningScanIds.value) {
      try {
        const { data } = await supplyChainApi.getScanProgress(scanId)
        const scan = data.data
        
        if (!latestScan.value || latestScan.value.id === scanId || scan.status === 'IN_PROGRESS') {
          latestScan.value = scan
        }
        
        if (scan.status === 'COMPLETED' || scan.status === 'FAILED') {
          completedIds.push(scanId)
          
          if (scan.status === 'COMPLETED') {
            message.success(`扫描完成: 发现 ${scan.vulnerableDependencies || 0} 个有漏洞的依赖`)
          } else {
            message.error(`扫描失败`)
          }
        }
      } catch (error) {
        console.error('Failed to poll scan progress:', error)
        completedIds.push(scanId)
      }
    }
    
    scanningScanIds.value = scanningScanIds.value.filter(id => !completedIds.includes(id))
    
    if (completedIds.length > 0 && selectedApplicationId.value) {
      const depRes = await supplyChainApi.getDependenciesByApplication(selectedApplicationId.value)
      dependencies.value = depRes.data.data
    }
    
    if (scanningScanIds.value.length === 0) {
      stopProgressPolling()
    }
  }, 1000)
}

const stopProgressPolling = () => {
  if (progressPollingTimer) {
    clearInterval(progressPollingTimer)
    progressPollingTimer = null
  }
}

const cancelScan = () => {
  showScanModal.value = false
  selectedServiceIds.value = []
}

const showVulnerabilities = async (dependency: Dependency) => {
  vulnerabilityModalVisible.value = true
  loadingVulnerabilities.value = true
  
  try {
    const { data } = await supplyChainApi.getVulnerabilities(dependency.id)
    currentVulnerabilities.value = data.data
  } catch (error) {
    console.error(error)
    message.error('加载漏洞信息失败')
    currentVulnerabilities.value = []
  } finally {
    loadingVulnerabilities.value = false
  }
}

const getLicenseStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    APPROVED: 'green',
    VIOLATION: 'red',
    UNKNOWN: 'orange'
  }
  return colors[status] || 'default'
}

const getLicenseStatusText = (status: string) => {
  const texts: Record<string, string> = {
    APPROVED: '已批准',
    VIOLATION: '违规',
    UNKNOWN: '未知'
  }
  return texts[status] || status
}

const getScanStatusText = (status: string) => {
  const texts: Record<string, string> = {
    COMPLETED: '已完成',
    IN_PROGRESS: '扫描中',
    FAILED: '失败'
  }
  return texts[status] || status
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const parseReferences = (references: string): string[] => {
  if (!references) return []
  return references.split('\n').filter(ref => ref.trim())
}

const formatReferenceUrl = (url: string): string => {
  try {
    const urlObj = new URL(url)
    // 显示域名和路径的最后部分
    const pathParts = urlObj.pathname.split('/').filter(p => p)
    if (pathParts.length > 0) {
      return `${urlObj.hostname}/.../${pathParts[pathParts.length - 1].substring(0, 20)}`
    }
    return urlObj.hostname
  } catch {
    return url.length > 40 ? url.substring(0, 40) + '...' : url
  }
}

onMounted(() => {
  loadApplications()
})

onUnmounted(() => {
  stopProgressPolling()
})
</script>

<style scoped>
.supply-chain-security {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
}

.page-header {
  height: 72px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 var(--spacing-xl);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
}

.page-header h1 {
  margin: 0 0 var(--spacing-xs);
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-header p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.content {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
}

.service-selector {
  margin-bottom: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.service-selector label {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.scan-section,
.sbom-section {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.filter-group {
  display: flex;
  gap: var(--spacing-sm);
}

.scan-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-base);
}

.summary-card {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
}

.card-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-base);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.stat-value {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.stat-value.danger {
  color: var(--color-error);
}

.stat-value.warning {
  color: var(--color-warning);
}

.severity-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-sm);
}

.severity-stats .stat-item {
  flex-direction: column;
  align-items: flex-start;
  padding: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-sm);
}

.stat-item.critical .stat-value { color: #d73a49; }
.stat-item.high .stat-value { color: #e36209; }
.stat-item.medium .stat-value { color: #fbca04; }
.stat-item.low .stat-value { color: #28a745; }

.no-scan {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--color-text-tertiary);
}

.no-scan .empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.no-scan p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.dependency-table {
  margin-top: var(--spacing-base);
}

.dep-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dep-version {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl);
}

.no-data {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--color-text-tertiary);
}

.vulnerability-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
}

.vulnerability-item {
  padding: var(--spacing-base);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.vuln-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.severity-badge {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
}

.severity-badge.critical { background: #d73a49; color: white; }
.severity-badge.high { background: #e36209; color: white; }
.severity-badge.medium { background: #fbca04; color: black; }
.severity-badge.low { background: #28a745; color: white; }

.cve-id {
  font-family: monospace;
  font-size: var(--font-size-sm);
  color: var(--color-accent-primary);
}

.cvss-score {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.vuln-title {
  margin: 0 0 var(--spacing-xs);
  font-size: var(--font-size-md);
  font-weight: 600;
}

.vuln-description {
  margin: 0 0 var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.vuln-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.detail-item {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.detail-item.references {
  margin-top: var(--spacing-sm);
}

.reference-links {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-xs);
}

.reference-link {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-accent-primary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  padding: 4px 8px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.reference-link:hover {
  background: var(--color-bg-primary);
  text-decoration: underline;
}

.status-completed { color: var(--color-success); }
.status-in_progress { color: var(--color-warning); }
.status-failed { color: var(--color-error); }
.scanning { color: #d73a49; font-weight: 600; animation: blink 1s infinite; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.service-list {
  max-height: 300px;
  overflow-y: auto;
}

.service-item {
  padding: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.service-item:last-child {
  border-bottom: none;
}
</style>

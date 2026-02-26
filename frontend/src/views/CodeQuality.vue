<template>
  <div class="code-quality">
    <div class="page-header">
      <h1>代码质量</h1>
      <p>代码安全、可靠性、可维护性检查</p>
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
          <h2>质量扫描</h2>
          <button class="btn btn-primary" @click="showScanModal = true" :disabled="loadingServices">
            <ScanOutlined />
            开始扫描
          </button>
        </div>

        <div v-if="latestScan" class="scan-summary">
          <div v-if="latestScan.status === 'IN_PROGRESS'" class="progress-card">
            <div class="card-title">扫描进度</div>
            <div class="card-content">
              <a-progress :percent="latestScan.progress || 0" :status="'active'" />
              <div class="progress-info">
                <span class="phase">{{ latestScan.currentPhase }}</span>
                <span class="count">{{ latestScan.checkedCount || 0 }} / {{ latestScan.totalFiles || 0 }} 文件</span>
              </div>
              <div v-if="latestScan.currentFile" class="current-dep">
                正在检查: {{ latestScan.currentFile }}
              </div>
            </div>
          </div>

          <div class="summary-card score-card">
            <div class="card-title">质量评分</div>
            <div class="card-content">
              <div class="score-circle">
                <a-progress type="circle" :percent="Math.round(latestScan.qualityScore || 0)" :width="80" />
              </div>
              <div class="score-breakdown">
                <div class="score-item">
                  <span class="score-label">安全</span>
                  <span class="score-value">{{ Math.round(latestScan.securityScore || 0) }}</span>
                </div>
                <div class="score-item">
                  <span class="score-label">可靠性</span>
                  <span class="score-value">{{ Math.round(latestScan.reliabilityScore || 0) }}</span>
                </div>
                <div class="score-item">
                  <span class="score-label">可维护性</span>
                  <span class="score-value">{{ Math.round(latestScan.maintainabilityScore || 0) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">问题分类</div>
            <div class="card-content category-stats">
              <div class="stat-item security">
                <span class="stat-label">安全问题</span>
                <span class="stat-value">{{ latestScan.securityIssues }}</span>
              </div>
              <div class="stat-item reliability">
                <span class="stat-label">可靠性问题</span>
                <span class="stat-value">{{ latestScan.reliabilityIssues }}</span>
              </div>
              <div class="stat-item maintainability">
                <span class="stat-label">可维护性问题</span>
                <span class="stat-value">{{ latestScan.maintainabilityIssues }}</span>
              </div>
              <div class="stat-item smell">
                <span class="stat-label">代码异味</span>
                <span class="stat-value">{{ latestScan.codeSmellIssues }}</span>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">严重程度</div>
            <div class="card-content severity-stats">
              <div class="stat-item blocker">
                <span class="stat-label">阻塞</span>
                <span class="stat-value">{{ latestScan.blockerCount }}</span>
              </div>
              <div class="stat-item critical">
                <span class="stat-label">严重</span>
                <span class="stat-value">{{ latestScan.criticalCount }}</span>
              </div>
              <div class="stat-item major">
                <span class="stat-label">主要</span>
                <span class="stat-value">{{ latestScan.majorCount }}</span>
              </div>
              <div class="stat-item minor">
                <span class="stat-label">次要</span>
                <span class="stat-value">{{ latestScan.minorCount }}</span>
              </div>
              <div class="stat-item info">
                <span class="stat-label">提示</span>
                <span class="stat-value">{{ latestScan.infoCount }}</span>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">扫描信息</div>
            <div class="card-content">
              <div class="stat-item">
                <span class="stat-label">扫描状态</span>
                <span :class="['stat-value', 'status-' + latestScan.status.toLowerCase()]">
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
          <p>暂无扫描记录，点击"开始扫描"进行代码质量检查</p>
        </div>

        <div v-if="latestScan && latestScan.status === 'COMPLETED' && issues.length > 0" class="issues-section">
          <div class="section-header">
            <h2>问题列表</h2>
            <div class="filter-group">
              <a-select v-model:value="categoryFilter" style="width: 150px" placeholder="问题分类" allowClear>
                <a-select-option value="SECURITY">安全问题</a-select-option>
                <a-select-option value="RELIABILITY">可靠性问题</a-select-option>
                <a-select-option value="MAINTAINABILITY">可维护性问题</a-select-option>
                <a-select-option value="CODE_SMELL">代码异味</a-select-option>
              </a-select>
              <a-select v-model:value="severityFilter" style="width: 120px" placeholder="严重程度" allowClear>
                <a-select-option value="BLOCKER">阻塞</a-select-option>
                <a-select-option value="CRITICAL">严重</a-select-option>
                <a-select-option value="MAJOR">主要</a-select-option>
                <a-select-option value="MINOR">次要</a-select-option>
                <a-select-option value="INFO">提示</a-select-option>
              </a-select>
            </div>
          </div>

          <div class="issues-list">
            <div v-for="issue in filteredIssues" :key="issue.id" class="issue-item">
              <div class="issue-header">
                <span :class="['severity-badge', issue.severity.toLowerCase()]">
                  {{ issue.severity }}
                </span>
                <span :class="['category-badge', issue.category.toLowerCase()]">
                  {{ getCategoryText(issue.category) }}
                </span>
                <span class="rule-id">{{ issue.ruleId }}</span>
              </div>
              <div class="issue-body">
                <div class="issue-title">{{ issue.ruleName }}</div>
                <div class="issue-message">{{ issue.message }}</div>
                <div class="issue-location">
                  <FileOutlined />
                  <span>{{ getShortPath(issue.filePath) }}:{{ issue.line }}</span>
                </div>
                <div v-if="issue.codeSnippet" class="issue-code">
                  <code>{{ issue.codeSnippet }}</code>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Application, ServiceEntity, CodeQualityScan, CodeQualityIssue } from '@/types'
import { applicationApi, serviceApi, codeQualityApi } from '@/api/project'
import { ScanOutlined, FileSearchOutlined, FileOutlined } from '@ant-design/icons-vue'

const applications = ref<Application[]>([])
const selectedApplicationId = ref<number | null>(null)
const latestScan = ref<CodeQualityScan | null>(null)
const issues = ref<CodeQualityIssue[]>([])
const loading = ref(false)
const scanning = ref(false)
const categoryFilter = ref<string | undefined>(undefined)
const severityFilter = ref<string | undefined>(undefined)

const showScanModal = ref(false)
const applicationServices = ref<ServiceEntity[]>([])
const loadingServices = ref(false)
const selectedServiceIds = ref<number[]>([])

const scanningScanIds = ref<number[]>([])
let progressPollingTimer: ReturnType<typeof setInterval> | null = null

const filteredIssues = computed(() => {
  let result = issues.value
  
  if (categoryFilter.value) {
    result = result.filter(i => i.category === categoryFilter.value)
  }
  
  if (severityFilter.value) {
    result = result.filter(i => i.severity === severityFilter.value)
  }
  
  return result
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
    const { data } = await codeQualityApi.getLatestScanByApplication(selectedApplicationId.value)
    latestScan.value = data.data
    
    if (data.data?.status === 'IN_PROGRESS' && data.data?.id) {
      scanningScanIds.value = [data.data.id]
      startProgressPolling()
    } else if (data.data?.id) {
      loadIssues(data.data.id)
    }
  } catch (error) {
    console.error(error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadIssues = async (scanId: number) => {
  try {
    const { data } = await codeQualityApi.getIssues(scanId)
    issues.value = data.data
  } catch (error) {
    console.error(error)
    issues.value = []
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
      const { data } = await codeQualityApi.startScan(serviceId)
      if (data.data?.id) {
        scanningScanIds.value.push(data.data.id)
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
        const { data } = await codeQualityApi.getScan(scanId)
        const scan = data.data
        
        if (latestScan.value?.id === scanId) {
          latestScan.value = scan
        }
        
        if (scan.status === 'COMPLETED' || scan.status === 'FAILED') {
          completedIds.push(scanId)
          
          if (scan.status === 'COMPLETED') {
            message.success(`扫描完成: 发现 ${scan.totalIssues || 0} 个问题`)
            loadIssues(scanId)
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
    
    if (scanningScanIds.value.length === 0) {
      stopProgressPolling()
    }
  }, 2000)
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

const getShortPath = (path: string) => {
  if (!path) return ''
  const parts = path.split('/')
  return parts.slice(-3).join('/')
}

const getCategoryText = (category: string) => {
  const texts: Record<string, string> = {
    SECURITY: '安全',
    RELIABILITY: '可靠性',
    MAINTAINABILITY: '可维护性',
    CODE_SMELL: '代码异味'
  }
  return texts[category] || category
}

const getScanStatusText = (status: string) => {
  const texts: Record<string, string> = {
    COMPLETED: '已完成',
    IN_PROGRESS: '进行中',
    FAILED: '失败'
  }
  return texts[status] || status
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadApplications()
})

onUnmounted(() => {
  stopProgressPolling()
})
</script>

<style scoped>
.code-quality {
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
.issues-section {
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

.progress-card {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
  grid-column: 1 / -1;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
}

.progress-info .phase {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.progress-info .count {
  color: var(--color-accent-primary);
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.current-dep {
  margin-top: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.score-card .card-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.score-circle {
  flex-shrink: 0;
}

.score-breakdown {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.score-value {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
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

.category-stats,
.severity-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-sm);
}

.category-stats .stat-item,
.severity-stats .stat-item {
  flex-direction: column;
  align-items: flex-start;
  padding: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-sm);
}

.stat-item.security .stat-value { color: #d73a49; }
.stat-item.reliability .stat-value { color: #e36209; }
.stat-item.maintainability .stat-value { color: #fbca04; }
.stat-item.smell .stat-value { color: #6f42c1; }
.stat-item.blocker .stat-value { color: #b60200; }
.stat-item.critical .stat-value { color: #d73a49; }
.stat-item.major .stat-value { color: #e36209; }
.stat-item.minor .stat-value { color: #fbca04; }
.stat-item.info .stat-value { color: #28a745; }

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

.issues-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
}

.issue-item {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.issue-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border-light);
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

.rule-id {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.issue-body {
  padding: var(--spacing-base);
}

.issue-title {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.issue-message {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
}

.issue-location {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-accent-primary);
  font-family: monospace;
  margin-bottom: var(--spacing-sm);
}

.issue-code {
  padding: var(--spacing-sm);
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  overflow-x: auto;
}

.issue-code code {
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.status-completed { color: var(--color-success); }
.status-in_progress { color: var(--color-warning); }
.status-failed { color: var(--color-error); }

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
</style>

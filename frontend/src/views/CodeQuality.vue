<template>
  <div class="code-quality">
    <div class="page-header">
      <div class="header-left">
        <h1>代码质量</h1>
        <p>代码安全、可靠性、可维护性检查</p>
      </div>
    </div>

    <div class="content">
      <div class="operation-panel">
        <div class="panel-row">
          <div class="form-item">
            <label>选择系统</label>
            <a-select
              v-model:value="selectedApplicationId"
              style="width: 240px"
              placeholder="请选择应用系统"
              @change="handleApplicationChange"
            >
              <a-select-option v-for="app in applications" :key="app.id" :value="app.id">
                {{ app.name }}
              </a-select-option>
            </a-select>
          </div>

          <div class="form-item">
            <label>扫描内容</label>
            <a-select
              v-model:value="selectedServiceIds"
              mode="multiple"
              style="width: 360px"
              placeholder="请选择要扫描的服务"
              :disabled="!selectedApplicationId"
              :loading="loadingServices"
              :options="serviceOptions"
            />
          </div>

          <button
            class="btn btn-primary scan-btn"
            :disabled="!selectedApplicationId || selectedServiceIds.length === 0 || isScanning"
            @click="handleScan"
          >
            <ScanOutlined />
            <span>{{ isScanning ? '扫描中...' : '执行扫描' }}</span>
          </button>
        </div>
      </div>

      <div class="history-section">
        <div class="section-header">
          <h2>扫描历史</h2>
          <span class="record-count" v-if="scanHistory.length > 0">共 {{ scanHistory.length }} 条记录</span>
        </div>

        <div class="history-list" v-if="!loadingHistory && scanHistory.length > 0">
          <div
            v-for="scan in scanHistory"
            :key="scan.id"
            class="history-item"
            @click="viewScanReport(scan)"
          >
            <div class="item-left">
              <div class="scan-service">{{ getServiceName(scan.serviceId) }}</div>
              <div class="scan-time">{{ formatDateTime(scan.completedAt || scan.startedAt) }}</div>
            </div>
            <div class="item-center">
              <div class="score-info">
                <span class="score-label">质量评分</span>
                <span class="score-value" :class="getScoreClass(scan.qualityScore)">
                  {{ Math.round(scan.qualityScore || 0) }}
                </span>
              </div>
              <div class="issue-summary">
                <span class="issue-item security" v-if="scan.securityIssues">
                  <span class="dot"></span>
                  安全 {{ scan.securityIssues }}
                </span>
                <span class="issue-item reliability" v-if="scan.reliabilityIssues">
                  <span class="dot"></span>
                  可靠性 {{ scan.reliabilityIssues }}
                </span>
                <span class="issue-item maintainability" v-if="scan.maintainabilityIssues">
                  <span class="dot"></span>
                  可维护性 {{ scan.maintainabilityIssues }}
                </span>
              </div>
            </div>
            <div class="item-right">
              <span :class="['status-badge', scan.status.toLowerCase()]">
                {{ getScanStatusText(scan.status) }}
              </span>
              <RightOutlined class="arrow-icon" />
            </div>
          </div>
        </div>

        <div class="empty-state" v-if="!loadingHistory && scanHistory.length === 0">
          <FileSearchOutlined class="empty-icon" />
          <p v-if="!selectedApplicationId">请先选择应用系统</p>
          <p v-else>暂无扫描记录</p>
        </div>

        <div class="loading-state" v-if="loadingHistory">
          <a-spin />
          <span>加载中...</span>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="showReportModal"
      :title="reportTitle"
      width="90%"
      style="top: 20px"
      :footer="null"
      @cancel="closeReport"
    >
      <div class="report-content" v-if="currentScan">
        <div class="report-summary">
          <div class="summary-card score-card">
            <div class="card-title">质量评分</div>
            <div class="card-content">
              <div class="score-circle">
                <a-progress type="circle" :percent="Math.round(currentScan.qualityScore || 0)" :width="80" />
              </div>
              <div class="score-breakdown">
                <div class="score-item">
                  <span class="score-label">安全</span>
                  <span class="score-value">{{ Math.round(currentScan.securityScore || 0) }}</span>
                </div>
                <div class="score-item">
                  <span class="score-label">可靠性</span>
                  <span class="score-value">{{ Math.round(currentScan.reliabilityScore || 0) }}</span>
                </div>
                <div class="score-item">
                  <span class="score-label">可维护性</span>
                  <span class="score-value">{{ Math.round(currentScan.maintainabilityScore || 0) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">问题分类</div>
            <div class="card-content category-stats">
              <div class="stat-item security">
                <span class="stat-label">安全问题</span>
                <span class="stat-value">{{ currentScan.securityIssues }}</span>
              </div>
              <div class="stat-item reliability">
                <span class="stat-label">可靠性问题</span>
                <span class="stat-value">{{ currentScan.reliabilityIssues }}</span>
              </div>
              <div class="stat-item maintainability">
                <span class="stat-label">可维护性问题</span>
                <span class="stat-value">{{ currentScan.maintainabilityIssues }}</span>
              </div>
              <div class="stat-item smell">
                <span class="stat-label">代码异味</span>
                <span class="stat-value">{{ currentScan.codeSmellIssues }}</span>
              </div>
            </div>
          </div>

          <div class="summary-card">
            <div class="card-title">严重程度</div>
            <div class="card-content severity-stats">
              <div class="stat-item blocker">
                <span class="stat-label">阻塞</span>
                <span class="stat-value">{{ currentScan.blockerCount }}</span>
              </div>
              <div class="stat-item critical">
                <span class="stat-label">严重</span>
                <span class="stat-value">{{ currentScan.criticalCount }}</span>
              </div>
              <div class="stat-item major">
                <span class="stat-label">主要</span>
                <span class="stat-value">{{ currentScan.majorCount }}</span>
              </div>
              <div class="stat-item minor">
                <span class="stat-label">次要</span>
                <span class="stat-value">{{ currentScan.minorCount }}</span>
              </div>
              <div class="stat-item info">
                <span class="stat-label">提示</span>
                <span class="stat-value">{{ currentScan.infoCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="issues-section" v-if="issues.length > 0">
          <div class="section-header">
            <h3>问题列表</h3>
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

        <div class="no-issues" v-else-if="currentScan.status === 'COMPLETED'">
          <CheckCircleOutlined class="success-icon" />
          <p>扫描完成，未发现问题</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Application, ServiceEntity, CodeQualityScan, CodeQualityIssue } from '@/types'
import { applicationApi, serviceApi, codeQualityApi } from '@/api/project'
import { ScanOutlined, FileSearchOutlined, FileOutlined, RightOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'

const applications = ref<Application[]>([])
const selectedApplicationId = ref<number | null>(null)
const applicationServices = ref<ServiceEntity[]>([])
const selectedServiceIds = ref<number[]>([])
const loadingServices = ref(false)

const scanHistory = ref<CodeQualityScan[]>([])
const loadingHistory = ref(false)
const isScanning = ref(false)

const showReportModal = ref(false)
const currentScan = ref<CodeQualityScan | null>(null)
const issues = ref<CodeQualityIssue[]>([])
const categoryFilter = ref<string | undefined>(undefined)
const severityFilter = ref<string | undefined>(undefined)

let progressPollingTimer: ReturnType<typeof setInterval> | null = null
const scanningScanIds = ref<number[]>([])

const serviceOptions = computed(() => {
  return applicationServices.value.map(s => ({
    label: s.name,
    value: s.id
  }))
})

const reportTitle = computed(() => {
  if (!currentScan.value) return '扫描报告'
  const serviceName = getServiceName(currentScan.value.serviceId)
  return `${serviceName} - 扫描报告`
})

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

const handleApplicationChange = async () => {
  scanHistory.value = []
  selectedServiceIds.value = []
  issues.value = []
  
  if (!selectedApplicationId.value) return
  
  loadingServices.value = true
  loadingHistory.value = true
  
  try {
    const [servicesRes, scansRes] = await Promise.all([
      serviceApi.getByApplicationId(selectedApplicationId.value),
      codeQualityApi.getScansByApplication(selectedApplicationId.value)
    ])
    applicationServices.value = servicesRes.data.data
    scanHistory.value = scansRes.data.data
  } catch (error) {
    console.error(error)
    message.error('加载数据失败')
  } finally {
    loadingServices.value = false
    loadingHistory.value = false
  }
}

const handleScan = async () => {
  if (selectedServiceIds.value.length === 0) {
    message.warning('请至少选择一个服务')
    return
  }
  
  isScanning.value = true
  let successCount = 0
  let failCount = 0
  
  for (const serviceId of selectedServiceIds.value) {
    try {
      const { data } = await codeQualityApi.startScan(serviceId)
      if (data.data?.id) {
        scanningScanIds.value.push(data.data.id)
        successCount++
      }
    } catch (error) {
      console.error(error)
      failCount++
    }
  }
  
  if (successCount > 0) {
    message.success(`已启动 ${successCount} 个服务的扫描`)
    startProgressPolling()
  }
  if (failCount > 0) {
    message.error(`${failCount} 个服务扫描启动失败`)
  }
  
  isScanning.value = false
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
        
        if (scan.status === 'COMPLETED' || scan.status === 'FAILED') {
          completedIds.push(scanId)
          
          if (scan.status === 'COMPLETED') {
            message.success(`扫描完成: 发现 ${scan.totalIssues || 0} 个问题`)
          } else {
            message.error(`扫描失败`)
          }
          
          await handleApplicationChange()
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

const viewScanReport = async (scan: CodeQualityScan) => {
  currentScan.value = scan
  showReportModal.value = true
  categoryFilter.value = undefined
  severityFilter.value = undefined
  
  if (scan.status === 'COMPLETED') {
    try {
      const { data } = await codeQualityApi.getIssues(scan.id)
      issues.value = data.data
    } catch (error) {
      console.error(error)
      issues.value = []
    }
  }
}

const closeReport = () => {
  showReportModal.value = false
  currentScan.value = null
  issues.value = []
}

const getServiceName = (serviceId: number) => {
  const service = applicationServices.value.find(s => s.id === serviceId)
  return service?.name || `服务 ${serviceId}`
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

const getScoreClass = (score: number) => {
  if (score >= 80) return 'good'
  if (score >= 60) return 'medium'
  return 'poor'
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
  gap: var(--spacing-lg);
}

.operation-panel {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
}

.panel-row {
  display: flex;
  align-items: flex-end;
  gap: var(--spacing-lg);
  flex-wrap: wrap;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-item label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.scan-btn {
  height: 32px;
  padding: 0 var(--spacing-lg);
}

.history-section {
  flex: 1;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
  flex-shrink: 0;
}

.history-section .section-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.record-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.history-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.history-item:hover {
  border-color: var(--color-accent-primary);
  background: var(--color-bg-secondary);
}

.item-left {
  min-width: 180px;
}

.scan-service {
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.scan-time {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.item-center {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  padding: 0 var(--spacing-lg);
}

.score-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
}

.score-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xs);
}

.score-value {
  font-size: 24px;
  font-weight: 700;
}

.score-value.good { color: var(--color-success); }
.score-value.medium { color: var(--color-warning); }
.score-value.poor { color: var(--color-error); }

.issue-summary {
  display: flex;
  gap: var(--spacing-base);
}

.issue-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.issue-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.issue-item.security .dot { background: #d73a49; }
.issue-item.reliability .dot { background: #e36209; }
.issue-item.maintainability .dot { background: #fbca04; }

.item-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-base);
}

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.status-badge.completed {
  background: rgba(40, 167, 69, 0.1);
  color: var(--color-success);
}

.status-badge.in_progress {
  background: rgba(255, 193, 7, 0.1);
  color: var(--color-warning);
}

.status-badge.failed {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.arrow-icon {
  color: var(--color-text-tertiary);
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

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.loading-state {
  gap: var(--spacing-base);
}

.report-content {
  max-height: calc(90vh - 120px);
  overflow-y: auto;
}

.report-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-base);
  margin-bottom: var(--spacing-lg);
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

.score-card .card-content {
  flex-direction: row;
  align-items: center;
  gap: var(--spacing-lg);
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

.category-stats,
.severity-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-sm);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-sm);
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

.stat-item.security .stat-value { color: #d73a49; }
.stat-item.reliability .stat-value { color: #e36209; }
.stat-item.maintainability .stat-value { color: #fbca04; }
.stat-item.smell .stat-value { color: #6f42c1; }
.stat-item.blocker .stat-value { color: #b60200; }
.stat-item.critical .stat-value { color: #d73a49; }
.stat-item.major .stat-value { color: #e36209; }
.stat-item.minor .stat-value { color: #fbca04; }
.stat-item.info .stat-value { color: #28a745; }

.issues-section {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
}

.issues-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
}

.issues-section .section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.filter-group {
  display: flex;
  gap: var(--spacing-sm);
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
}

.issue-item {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.issue-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-tertiary);
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

.no-issues {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--color-text-tertiary);
}

.success-icon {
  font-size: 48px;
  color: var(--color-success);
  margin-bottom: var(--spacing-base);
}
</style>

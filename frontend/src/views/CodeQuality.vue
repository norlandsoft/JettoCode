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
              v-model:value="selectedApplicationId as number | undefined"
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
            <label>扫描服务</label>
            <a-select
              v-model:value="selectedServiceIds"
              mode="multiple"
              style="width: 240px"
              placeholder="请选择要扫描的服务"
              :disabled="!selectedApplicationId"
              :loading="loadingServices"
              :options="serviceOptions"
            />
          </div>

          <div class="form-item">
            <label>扫描内容</label>
            <a-tree-select
              v-model:value="selectedCheckItems"
              style="width: 400px"
              placeholder="请选择检查项"
              :disabled="!selectedApplicationId"
              :tree-data="checkItemTreeData"
              tree-checkable
              :show-checked-strategy="SHOW_PARENT"
              tree-node-filter-prop="title"
              allow-clear
              :max-tag-count="3"
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

          <button
            v-if="isScanning"
            class="btn btn-danger scan-btn"
            @click="handleCancelScan"
          >
            <StopOutlined />
            <span>取消扫描</span>
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
              <button
                class="delete-btn"
                @click.stop="handleDeleteScan(scan)"
                title="删除"
              >
                <DeleteOutlined />
              </button>
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

    <!-- 侧边栏日志详情 -->
    <transition name="slide">
      <div class="sidebar-overlay" v-if="showSidebar" @click.self="closeSidebar">
        <div class="sidebar-panel">
          <!-- 侧边栏头部 -->
          <div class="sidebar-header">
            <h3>扫描日志详情</h3>
            <button class="sidebar-close-btn" @click="closeSidebar">
              <CloseOutlined />
            </button>
          </div>

          <!-- 侧边栏内容 -->
          <div class="sidebar-content" v-if="currentScan">
            <!-- 扫描概览 -->
            <div class="scan-overview">
              <div class="overview-header">
                <h3>扫描概览</h3>
                <span :class="['scan-status', currentScan.status.toLowerCase()]">
                  {{ getScanStatusText(currentScan.status) }}
                </span>
              </div>
              <div class="overview-grid">
                <div class="overview-item">
                  <span class="label">扫描时间</span>
                  <span class="value">{{ formatDateTime(currentScan.startedAt) }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">完成时间</span>
                  <span class="value">{{ currentScan.completedAt ? formatDateTime(currentScan.completedAt) : '-' }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">执行阶段</span>
                  <span class="value">{{ currentScan.currentPhase || '-' }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">任务数量</span>
                  <span class="value">{{ currentScan.totalFiles || 0 }} 个</span>
                </div>
                <div class="overview-item">
                  <span class="label">质量评分</span>
                  <span class="value" :class="getScoreClass(currentScan.qualityScore)">
                    {{ Math.round(currentScan.qualityScore || 0) }}
                  </span>
                </div>
                <div class="overview-item">
                  <span class="label">发现问题</span>
                  <span class="value">{{ currentScan.totalIssues || 0 }} 个</span>
                </div>
              </div>
            </div>

            <!-- 加载状态 -->
            <div class="loading-state" v-if="loadingLogs">
              <a-spin />
              <span>加载日志中...</span>
            </div>

            <!-- 日志列表 -->
            <div class="logs-section" v-else-if="scanLogs.length > 0">
              <div class="section-header">
                <h3>执行日志</h3>
                <span class="log-count">共 {{ scanLogs.length }} 条记录</span>
              </div>

              <div class="logs-list">
                <div v-for="log in scanLogs" :key="log.taskId" class="log-item">
                  <!-- 日志头部 -->
                  <div class="log-header" @click="toggleLogDetail(log.taskId)">
                    <div class="log-header-left">
                      <span :class="['log-status', log.status.toLowerCase()]">
                        {{ getTaskStatusText(log.status) }}
                      </span>
                      <span class="log-service">{{ log.serviceName }}</span>
                      <span class="log-check-item">{{ log.checkItemName }}</span>
                    </div>
                    <div class="log-header-right">
                      <span v-if="log.issueCount > 0" class="log-issue-count">
                        {{ log.issueCount }} 个问题
                      </span>
                      <span v-if="log.duration" class="log-duration">
                        {{ formatDuration(log.duration) }}
                      </span>
                      <DownOutlined :class="['expand-icon', { expanded: expandedLogs.has(log.taskId) }]" />
                    </div>
                  </div>

                  <!-- 日志详情（展开后显示） -->
                  <div class="log-detail" v-if="expandedLogs.has(log.taskId)">
                    <!-- 执行时间 -->
                    <div class="detail-section">
                      <div class="detail-title">执行时间</div>
                      <div class="detail-content time-info">
                        <div class="time-item">
                          <ClockCircleOutlined />
                          <span>开始: {{ log.startedAt ? formatDateTime(log.startedAt) : '-' }}</span>
                        </div>
                        <div class="time-item">
                          <CheckCircleOutlined />
                          <span>结束: {{ log.completedAt ? formatDateTime(log.completedAt) : '-' }}</span>
                        </div>
                        <div class="time-item" v-if="log.duration">
                          <HourglassOutlined />
                          <span>耗时: {{ formatDuration(log.duration) }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- 扫描内容 -->
                    <div class="detail-section">
                      <div class="detail-title">扫描内容</div>
                      <div class="detail-content">
                        <div class="info-grid">
                          <div class="info-item">
                            <span class="info-label">服务:</span>
                            <span class="info-value">{{ log.serviceName }}</span>
                          </div>
                          <div class="info-item">
                            <span class="info-label">检查项:</span>
                            <span class="info-value">{{ log.checkItemName }}</span>
                          </div>
                          <div class="info-item">
                            <span class="info-label">检查项 Key:</span>
                            <span class="info-value">{{ log.checkItemKey }}</span>
                          </div>
                          <div class="info-item">
                            <span class="info-label">严重级别:</span>
                            <span class="info-value">{{ log.severity }}</span>
                          </div>
                          <div class="info-item">
                            <span class="info-label">问题数量:</span>
                            <span class="info-value">{{ log.issueCount }}</span>
                          </div>
                          <div class="info-item" v-if="log.retryCount > 0">
                            <span class="info-label">重试次数:</span>
                            <span class="info-value">{{ log.retryCount }}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 提示词 -->
                    <div class="detail-section" v-if="log.promptText">
                      <div class="detail-title">
                        <span>输入提示词</span>
                        <span class="text-length">({{ log.promptLength || log.promptText.length }} 字符)</span>
                      </div>
                      <div class="detail-content code-block">
                        <pre>{{ log.promptText }}</pre>
                      </div>
                    </div>

                    <!-- AI 响应 -->
                    <div class="detail-section" v-if="log.responseText">
                      <div class="detail-title">
                        <span>AI 分析结果</span>
                        <span class="text-length">({{ log.responseLength || log.responseText.length }} 字符)</span>
                      </div>
                      <div class="detail-content code-block">
                        <pre>{{ log.responseText }}</pre>
                      </div>
                    </div>

                    <!-- 错误信息 -->
                    <div class="detail-section error-section" v-if="log.errorMessage">
                      <div class="detail-title error-title">
                        <ExclamationCircleOutlined />
                        <span>错误信息</span>
                      </div>
                      <div class="detail-content error-content">
                        {{ log.errorMessage }}
                      </div>
                    </div>

                    <!-- 结果摘要 -->
                    <div class="detail-section" v-if="log.resultSummary">
                      <div class="detail-title">结果摘要</div>
                      <div class="detail-content summary-content">
                        {{ log.resultSummary }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 无日志 -->
            <div class="no-logs" v-else>
              <FileSearchOutlined class="empty-icon" />
              <p>暂无执行日志</p>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message, TreeSelect, Modal } from 'ant-design-vue'
import type { Application, ServiceEntity, CodeQualityScan, CodeQualityIssue } from '@/types'
import { applicationApi, serviceApi, codeQualityApi, qualityCheckApi, scanLogApi, type QualityCheckTreeDTO, type TaskExecutionLog } from '@/api/project'
import { ScanOutlined, FileSearchOutlined, RightOutlined, CheckCircleOutlined, StopOutlined, DownOutlined, ClockCircleOutlined, HourglassOutlined, ExclamationCircleOutlined, CloseOutlined, DeleteOutlined } from '@ant-design/icons-vue'

const { SHOW_PARENT } = TreeSelect

const applications = ref<Application[]>([])
const selectedApplicationId = ref<number | null>(null)
const applicationServices = ref<ServiceEntity[]>([])
const selectedServiceIds = ref<number[]>([])
const loadingServices = ref(false)

// 质量检查项
const qualityCheckTree = ref<QualityCheckTreeDTO[]>([])
const selectedCheckItems = ref<number[]>([])

const scanHistory = ref<CodeQualityScan[]>([])
const loadingHistory = ref(false)
const isScanning = ref(false)

const showSidebar = ref(false)
const currentScan = ref<CodeQualityScan | null>(null)
const issues = ref<CodeQualityIssue[]>([])
let progressPollingTimer: ReturnType<typeof setInterval> | null = null
const scanningScanIds = ref<number[]>([])

// 日志相关状态
const scanLogs = ref<TaskExecutionLog[]>([])
const expandedLogs = ref<Set<number>>(new Set())
const loadingLogs = ref(false)

const serviceOptions = computed(() => {
  return applicationServices.value.map(s => ({
    label: s.name,
    value: s.id
  }))
})

// 将质量检查项转换为 TreeSelect 格式
// value 使用 item.id 以便发送到后端
const checkItemTreeData = computed(() => {
  return qualityCheckTree.value.map(group => ({
    title: group.groupName,
    value: `group_${group.groupId}`,
    key: `group_${group.groupId}`,
    selectable: false,
    children: (group.items || []).map(item => ({
      title: item.itemName,
      value: item.id,  // 使用 ID 而不是 key
      key: item.id
    }))
  }))
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
  selectedCheckItems.value = []
  issues.value = []

  if (!selectedApplicationId.value) return

  loadingServices.value = true
  loadingHistory.value = true

  try {
    const [servicesRes, scansRes, checkRes] = await Promise.all([
      serviceApi.getByApplicationId(selectedApplicationId.value),
      codeQualityApi.getScansByApplication(selectedApplicationId.value),
      qualityCheckApi.getTree()
    ])
    applicationServices.value = servicesRes.data.data
    scanHistory.value = scansRes.data.data
    qualityCheckTree.value = checkRes.data.data || []
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

  // 过滤出真正的检查项ID（排除分组ID）
  const checkItemIds = selectedCheckItems.value.filter(id => typeof id === 'number')

  if (checkItemIds.length === 0) {
    message.warning('请至少选择一个检查项')
    return
  }

  isScanning.value = true

  try {
    // 使用批量扫描 API
    const { data } = await codeQualityApi.startBatchScan(selectedServiceIds.value, checkItemIds)
    if (data.data?.id) {
      scanningScanIds.value.push(data.data.id)
      currentScanningId.value = data.data.id
      message.success('已启动扫描任务')
      // 尝试使用 WebSocket，失败则回退到轮询
      if (!connectWebSocket(data.data.id)) {
        startProgressPolling()
      }
    }
  } catch (error: any) {
    console.error(error)
    message.error(error.response?.data?.message || '启动扫描失败')
    isScanning.value = false
  }
}

const currentScanningId = ref<number | null>(null)

// 取消扫描
const handleCancelScan = async () => {
  if (!currentScanningId.value) return

  try {
    await codeQualityApi.cancelScan(currentScanningId.value)
    message.info('正在取消扫描...')
  } catch (error: any) {
    console.error(error)
    message.error(error.response?.data?.message || '取消扫描失败')
  }
}

// 删除扫描记录
const handleDeleteScan = async (scan: CodeQualityScan) => {
  // 使用确认对话框
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该扫描记录吗？此操作不可恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await codeQualityApi.deleteScan(scan.id)
        message.success('扫描记录已删除')
        // 从列表中移除
        scanHistory.value = scanHistory.value.filter(s => s.id !== scan.id)
        // 如果当前侧边栏显示的是被删除的扫描，关闭侧边栏
        if (currentScan.value?.id === scan.id) {
          closeSidebar()
        }
      } catch (error: any) {
        console.error(error)
        message.error(error.response?.data?.message || '删除失败')
      }
    }
  })
}

// WebSocket 相关
let websocket: WebSocket | null = null
let reconnectAttempts = 0
const maxReconnectAttempts = 3

const connectWebSocket = (scanId: number): boolean => {
  try {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/api/ws`

    websocket = new WebSocket(wsUrl)

    websocket.onopen = () => {
      console.log('WebSocket connected')
      reconnectAttempts = 0
      // 订阅扫描进度
      websocket?.send(JSON.stringify({
        type: 'subscribe',
        topic: `/topic/scan/${scanId}`
      }))
    }

    websocket.onmessage = (event) => {
      try {
        const progress = JSON.parse(event.data)
        handleProgressUpdate(progress)
      } catch (e) {
        // 可能是订阅确认消息，忽略
      }
    }

    websocket.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    websocket.onclose = () => {
      console.log('WebSocket closed')
      // 如果扫描仍在进行，回退到轮询
      if (isScanning.value && reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        console.log('Falling back to polling')
        startProgressPolling()
      }
    }

    return true
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
    return false
  }
}

const handleProgressUpdate = (progress: any) => {
  switch (progress.type) {
    case 'PROGRESS':
      // 更新进度
      break
    case 'TASK_COMPLETE':
      if (progress.issueCount > 0) {
        message.info(`任务完成: 发现 ${progress.issueCount} 个问题`)
      }
      break
    case 'COMPLETED':
      message.success(`扫描完成: 共发现 ${progress.issueCount || 0} 个问题`)
      stopScanning()
      handleApplicationChange()
      break
    case 'CANCELLED':
      message.warning(progress.message || '扫描已取消')
      stopScanning()
      handleApplicationChange()
      break
    case 'ERROR':
      message.error(progress.message || '扫描出错')
      stopScanning()
      break
  }
}

const stopScanning = () => {
  isScanning.value = false
  currentScanningId.value = null
  scanningScanIds.value = []
  stopProgressPolling()
  disconnectWebSocket()
}

const disconnectWebSocket = () => {
  if (websocket) {
    websocket.close()
    websocket = null
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

        if (scan.status === 'COMPLETED' || scan.status === 'FAILED' || scan.status === 'CANCELLED') {
          completedIds.push(scanId)

          if (scan.status === 'COMPLETED') {
            message.success(`扫描完成: 发现 ${scan.totalIssues || 0} 个问题`)
          } else if (scan.status === 'CANCELLED') {
            message.warning('扫描已取消')
          } else {
            message.error(`扫描失败`)
          }

          stopScanning()
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
  showSidebar.value = true
  scanLogs.value = []
  expandedLogs.value = new Set()
  loadingLogs.value = true

  try {
    // 获取扫描日志
    const { data } = await scanLogApi.getScanLogs(scan.id)
    scanLogs.value = data.data || []
  } catch (error) {
    console.error(error)
    message.error('加载日志失败')
  } finally {
    loadingLogs.value = false
  }
}

const closeSidebar = () => {
  showSidebar.value = false
  currentScan.value = null
  scanLogs.value = []
  expandedLogs.value = new Set()
}

// 切换日志详情展开/收起
const toggleLogDetail = (taskId: number) => {
  if (expandedLogs.value.has(taskId)) {
    expandedLogs.value.delete(taskId)
  } else {
    expandedLogs.value.add(taskId)
  }
  // 触发响应式更新
  expandedLogs.value = new Set(expandedLogs.value)
}

// 格式化时长
const formatDuration = (ms: number | null): string => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = Math.floor((ms % 60000) / 1000)
  return `${minutes}m ${seconds}s`
}

const getServiceName = (serviceId: number) => {
  const service = applicationServices.value.find(s => s.id === serviceId)
  return service?.name || `服务 ${serviceId}`
}

const getScanStatusText = (status: string) => {
  const texts: Record<string, string> = {
    COMPLETED: '已完成',
    IN_PROGRESS: '扫描中',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

const getTaskStatusText = (status: string) => {
  const texts: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '执行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
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
  disconnectWebSocket()
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
  height: 60px;
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
  margin: 0 0 2px;
  font-size: 16px;
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

.status-badge.cancelled {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.task-status.cancelled {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.btn-danger {
  background: var(--color-error);
  color: white;
  border: none;
}

.btn-danger:hover {
  background: #c82333;
}

.status-badge.cancelled {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.arrow-icon {
  color: var(--color-text-tertiary);
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  opacity: 0;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
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

/* OpenCode 分析结果样式 */
.opencode-section {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
  margin-top: var(--spacing-lg);
}

.opencode-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
}

.opencode-section .section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.task-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
}

.task-item {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.task-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border-light);
  flex-wrap: wrap;
}

.task-status {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.task-status.completed {
  background: rgba(40, 167, 69, 0.1);
  color: var(--color-success);
}

.task-status.running {
  background: rgba(255, 193, 7, 0.1);
  color: var(--color-warning);
}

.task-status.pending {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.task-status.failed {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.task-service {
  font-weight: 500;
  color: var(--color-text-primary);
}

.task-check-item {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.task-issue-count {
  font-size: var(--font-size-xs);
  background: var(--color-error);
  color: white;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.task-body {
  padding: var(--spacing-base);
}

.response-label,
.error-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
}

.response-content,
.prompt-content {
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  overflow-x: auto;
}

.response-content pre,
.prompt-content pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 400px;
  overflow-y: auto;
}

.error-content {
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

.task-footer {
  padding: var(--spacing-sm) var(--spacing-base);
  border-top: 1px solid var(--color-border-light);
}

.task-footer details {
  font-size: var(--font-size-sm);
}

.task-footer summary {
  cursor: pointer;
  color: var(--color-accent-primary);
  padding: var(--spacing-xs) 0;
}

.task-footer summary:hover {
  text-decoration: underline;
}

.task-footer .prompt-content {
  margin-top: var(--spacing-sm);
}

/* 日志视图样式 */
.log-content {
  max-height: calc(90vh - 120px);
  overflow-y: auto;
}

.scan-overview {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
  margin-bottom: var(--spacing-lg);
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
}

.overview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.scan-status {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.scan-status.completed {
  background: rgba(40, 167, 69, 0.1);
  color: var(--color-success);
}

.scan-status.in_progress {
  background: rgba(255, 193, 7, 0.1);
  color: var(--color-warning);
}

.scan-status.failed {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.scan-status.cancelled {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-base);
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.overview-item .label {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.overview-item .value {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.overview-item .value.good { color: var(--color-success); }
.overview-item .value.medium { color: var(--color-warning); }
.overview-item .value.poor { color: var(--color-error); }

.logs-section {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
}

.logs-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
}

.logs-section .section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.log-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.logs-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.log-item {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-secondary);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.log-header:hover {
  background: var(--color-bg-tertiary);
}

.log-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.log-header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-base);
}

.log-status {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.log-status.completed {
  background: rgba(40, 167, 69, 0.1);
  color: var(--color-success);
}

.log-status.running {
  background: rgba(255, 193, 7, 0.1);
  color: var(--color-warning);
}

.log-status.pending {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.log-status.failed {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.log-status.cancelled {
  background: rgba(108, 117, 125, 0.1);
  color: var(--color-text-tertiary);
}

.log-service {
  font-weight: 500;
  color: var(--color-text-primary);
}

.log-check-item {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.log-issue-count {
  font-size: var(--font-size-xs);
  background: var(--color-error);
  color: white;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.log-duration {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
}

.expand-icon {
  color: var(--color-text-tertiary);
  transition: transform var(--transition-fast);
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.log-detail {
  border-top: 1px solid var(--color-border-light);
  padding: var(--spacing-base);
}

.detail-section {
  margin-bottom: var(--spacing-base);
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.detail-title .text-length {
  font-weight: normal;
  color: var(--color-text-tertiary);
}

.detail-content {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.time-info {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.time-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-text-secondary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-sm);
}

.info-item {
  display: flex;
  gap: var(--spacing-xs);
}

.info-label {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.info-value {
  color: var(--color-text-primary);
  word-break: break-all;
}

.code-block {
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  overflow-x: auto;
}

.code-block pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 400px;
  overflow-y: auto;
}

.error-section {
  background: rgba(220, 53, 69, 0.05);
  border: 1px solid rgba(220, 53, 69, 0.2);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
}

.error-title {
  color: var(--color-error);
}

.error-content {
  color: var(--color-error);
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
}

.summary-content {
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  line-height: 1.6;
}

.no-logs {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--color-text-tertiary);
}

.no-logs .empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

/* Sidebar 样式 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
}

.sidebar-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 680px;
  max-width: 90vw;
  background: var(--color-bg-secondary);
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-base) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-tertiary);
  flex-shrink: 0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.sidebar-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.sidebar-close-btn:hover {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

/* Slide transition */
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.3s ease;
}

.slide-enter-active .sidebar-panel,
.slide-leave-active .sidebar-panel {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
}

.slide-enter-from .sidebar-panel,
.slide-leave-to .sidebar-panel {
  transform: translateX(100%);
}
</style>

<template>
  <div class="platform-management">
    <div class="page-header">
      <div class="header-left">
        <h1>平台管理</h1>
        <p>系统配置与规则管理</p>
      </div>
    </div>

    <div class="content">
      <!-- 左侧设置列表 -->
      <div class="settings-sidebar">
        <div class="sidebar-title">设置项</div>
        <div class="sidebar-menu">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['sidebar-menu-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            <span class="menu-text">{{ tab.label }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="settings-content">
        <!-- 质量检查项内容 -->
        <div class="content-panel quality-check-panel" v-if="activeTab === 'quality-check'">
          <!-- 左侧：检查项列表 -->
          <div class="check-categories">
            <div class="categories-header">检查类别</div>
            <div class="categories-list">
              <div v-for="category in qualityCategories" :key="category.key" class="category-group">
                <div
                  :class="['category-header', { expanded: expandedCategories.has(category.key) }]"
                  @click="toggleCategory(category.key)"
                >
                  <span class="expand-icon">{{ expandedCategories.has(category.key) ? '▼' : '▶' }}</span>
                  <span class="category-label">{{ category.label }}</span>
                </div>
                <div v-if="expandedCategories.has(category.key)" class="category-items">
                  <div
                    v-for="item in category.items"
                    :key="item.key"
                    :class="['category-item', { active: selectedItem === item.key }]"
                    @click="selectItem(item.key)"
                  >
                    <span class="item-label">{{ item.label }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：代码编辑器 -->
          <div class="check-editor">
            <div class="editor-header">
              <span class="editor-title">{{ currentItemLabel }}</span>
              <div class="editor-actions">
                <button class="btn-icon" @click="saveCheckConfig" title="保存">
                  <SaveOutlined />
                </button>
                <button class="btn-icon" @click="resetCheckConfig" title="重置">
                  <ReloadOutlined />
                </button>
              </div>
            </div>
            <div class="editor-container">
              <div ref="checkEditorRef" class="monaco-editor"></div>
            </div>
          </div>
        </div>

        <!-- 其他设置项（开发中） -->
        <div class="content-panel" v-else>
          <div class="coming-soon">
            <SettingOutlined class="icon" />
            <p>功能开发中...</p>
          </div>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="showCreateModal"
      :title="editingItem ? '编辑检查项' : '新建检查项'"
      width="700px"
      @ok="handleSubmit"
      @cancel="closeModal"
      :confirmLoading="submitting"
      okText="保存"
      cancelText="取消"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="规则ID" required>
          <a-input v-model:value="formData.ruleId" placeholder="如：SEC001" :disabled="!!editingItem" />
        </a-form-item>
        <a-form-item label="规则名称" required>
          <a-input v-model:value="formData.ruleName" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="问题分类" required>
          <a-select v-model:value="formData.category" placeholder="请选择问题分类">
            <a-select-option value="SECURITY">安全问题</a-select-option>
            <a-select-option value="RELIABILITY">可靠性问题</a-select-option>
            <a-select-option value="MAINTAINABILITY">可维护性问题</a-select-option>
            <a-select-option value="CODE_SMELL">代码异味</a-select-option>
            <a-select-option value="PERFORMANCE">性能问题</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="严重程度" required>
          <a-select v-model:value="formData.severity" placeholder="请选择严重程度">
            <a-select-option value="BLOCKER">阻塞</a-select-option>
            <a-select-option value="CRITICAL">严重</a-select-option>
            <a-select-option value="MAJOR">主要</a-select-option>
            <a-select-option value="MINOR">次要</a-select-option>
            <a-select-option value="INFO">提示</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" placeholder="请输入规则描述" :rows="2" />
        </a-form-item>
        <a-form-item label="提示词模版" required>
          <a-textarea
            v-model:value="formData.promptTemplate"
            placeholder="请输入用于智能检查的提示词模版，可使用 {code}, {file}, {line} 等变量"
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formData.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="formData.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showPromptModal"
      :title="promptItem?.ruleName + ' - 提示词模版'"
      width="700px"
      :footer="null"
    >
      <div class="prompt-content">
        <pre>{{ promptItem?.promptTemplate }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, shallowRef, watch, nextTick, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import * as monaco from 'monaco-editor'
import {
  SaveOutlined,
  ReloadOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'

interface QualityCheckItem {
  id: number
  category: string
  ruleId: string
  ruleName: string
  severity: string
  description: string
  promptTemplate: string
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

// 质量检查分组和子项
interface CheckItem {
  key: string
  label: string
}

interface CheckCategory {
  key: string
  label: string
  items: CheckItem[]
}

const qualityCategories: CheckCategory[] = [
  {
    key: 'security',
    label: '代码安全检查',
    items: [
      { key: 'security-owasp', label: 'OWASP Top 10 / CWE Top 25 / OWASP Mobile/LLM' },
      { key: 'security-injection', label: '注入（SQL/XSS/命令）、路径遍历' },
      { key: 'security-credentials', label: '硬编码凭证、不安全反序列化、弱加密' },
      { key: 'security-secrets', label: '秘密凭证检测（450+ 模式，熵 + 语义 + 正则）' },
      { key: 'security-hotspots', label: '安全热点（潜在风险需人工复核）' }
    ]
  },
  {
    key: 'reliability',
    label: '可靠性检查',
    items: [
      { key: 'reliability-null', label: '空指针与资源泄漏' },
      { key: 'reliability-exception', label: '异常处理与错误处理' },
      { key: 'reliability-concurrency', label: '并发与线程安全' }
    ]
  },
  {
    key: 'maintainability',
    label: '可维护性与代码异味',
    items: [
      { key: 'maintainability-duplication', label: '代码重复检测' },
      { key: 'maintainability-complexity', label: '复杂度与代码异味' },
      { key: 'maintainability-size', label: '文件与方法大小' }
    ]
  },
  {
    key: 'readability',
    label: '可读性与一致性',
    items: [
      { key: 'readability-naming', label: '命名规范检查' },
      { key: 'readability-comment', label: '注释质量检查' },
      { key: 'readability-format', label: '格式化与风格一致性' }
    ]
  },
  {
    key: 'performance',
    label: '性能与可扩展性',
    items: [
      { key: 'performance-query', label: 'N+1 查询与内存泄漏' },
      { key: 'performance-loop', label: '低效循环与阻塞 IO' },
      { key: 'performance-cache', label: '缓存策略检查' }
    ]
  },
  {
    key: 'testability',
    label: '可测试性',
    items: [
      { key: 'testability-coverage', label: '单元测试覆盖率' },
      { key: 'testability-mock', label: '可模拟性与依赖注入' },
      { key: 'testability-isolation', label: '测试隔离性' }
    ]
  },
  {
    key: 'operability',
    label: '可操作性',
    items: [
      { key: 'operability-logging', label: '日志与监控' },
      { key: 'operability-health', label: '健康检查与优雅关闭' },
      { key: 'operability-config', label: '配置管理检查' }
    ]
  },
  {
    key: 'architecture',
    label: '架构与质量检查',
    items: [
      { key: 'architecture-layer', label: '分层违规与循环依赖' },
      { key: 'architecture-principle', label: '设计原则检查（SOLID）' },
      { key: 'architecture-api', label: 'API 设计规范' }
    ]
  }
]

const expandedCategories = ref<Set<string>>(new Set(['security']))
const selectedItem = ref('security-owasp')
const checkEditorRef = ref<HTMLElement | null>(null)
const checkEditor = shallowRef<monaco.editor.IStandaloneCodeEditor | null>(null)

// 切换分组展开/折叠
const toggleCategory = (categoryKey: string) => {
  if (expandedCategories.value.has(categoryKey)) {
    expandedCategories.value.delete(categoryKey)
  } else {
    expandedCategories.value.add(categoryKey)
  }
}

// 选择检查项
const selectItem = (itemKey: string) => {
  selectedItem.value = itemKey
}

const currentItemLabel = computed(() => {
  for (const cat of qualityCategories) {
    const item = cat.items.find(i => i.key === selectedItem.value)
    if (item) return item.label
  }
  return ''
})

// 检查配置内容
const checkConfigs: Record<string, string> = {
  'security-owasp': `# OWASP Top 10 / CWE Top 25 / OWASP Mobile/LLM 检查配置

[owasp_top10]
# A01 - 访问控制失效
broken_access_control = true
# A02 - 加密失败
cryptographic_failures = true
# A03 - 注入
injection = true
# A04 - 不安全设计
insecure_design = true
# A05 - 安全配置错误
security_misconfiguration = true
# A06 - 易受攻击和过时的组件
vulnerable_components = true
# A07 - 身份识别和身份验证失败
auth_failures = true
# A08 - 软件和数据完整性失败
integrity_failures = true
# A09 - 安全日志和监控失败
logging_failures = true
# A10 - 服务器端请求伪造
ssrf = true

[cwe_top25]
enabled = true
# CWE-79: XSS
cwe_79 = true
# CWE-89: SQL注入
cwe_89 = true
# CWE-20: 输入验证不当
cwe_20 = true

[owasp_mobile]
enabled = true
platform_specific_checks = true

[owasp_llm]
# LLM 应用安全检查
enabled = true
prompt_injection = true
data_leakage = true`,

  'security-injection': `# 注入（SQL/XSS/命令）、路径遍历 检查配置

[sql_injection]
enabled = true
# 检测动态 SQL 拼接
detect_dynamic_sql = true
# 检测用户输入直接拼接到 SQL
detect_user_input_concat = true
# 检测 ORM 框架中的不安全用法
detect_unsafe_orm = true

[xss]
enabled = true
# 反射型 XSS
reflected = true
# 存储型 XSS
stored = true
# DOM 型 XSS
dom_based = true
# 检测 innerHTML 等危险 DOM 操作
detect_dangerous_dom = true

[command_injection]
enabled = true
# 系统命令注入
system_command = true
# 代码执行注入
code_execution = true
# 表达式语言注入
el_injection = true

[path_traversal]
enabled = true
# 检测路径遍历漏洞
detect_traversal = true
# 检测不安全的文件操作
detect_unsafe_file_ops = true
# 检测用户控制的文件路径
detect_user_controlled_path = true`,

  'security-credentials': `# 硬编码凭证、不安全反序列化、弱加密 检查配置

[hardcoded_credentials]
enabled = true
# 检测硬编码密码
passwords = true
# 检测硬编码 API 密钥
api_keys = true
# 检测硬编码私钥
private_keys = true
# 检测硬编码 Token
tokens = true

[insecure_deserialization]
enabled = true
# Java 原生反序列化
java_native = true
# pickle 反序列化
pickle = true
# YAML 反序列化
yaml_load = true
# JSONP 远程调用
jsonp = true

[weak_cryptography]
enabled = true
# 弱哈希算法（MD5, SHA1 用于安全场景）
weak_hash = true
# 弱加密算法（DES, RC4）
weak_encryption = true
# 弱随机数生成器
weak_random = true
# 硬编码盐值/IV
hardcoded_salt_iv = true
# 不安全的 TLS 版本
insecure_tls = true`,

  'security-secrets': `# 秘密凭证检测（450+ 模式，熵 + 语义 + 正则）配置

[secrets_detection]
enabled = true

[detection_methods]
# 熵值检测
entropy_detection = true
entropy_threshold = 4.5
# 语义分析
semantic_analysis = true
# 正则匹配
regex_patterns = true

[pattern_categories]
# AWS 凭证
aws = true
# Azure 凭证
azure = true
# GCP 凭证
gcp = true
# GitHub Token
github = true
# GitLab Token
gitlab = true
# Slack Token
slack = true
# JWT Secret
jwt = true
# 数据库连接字符串
database_connection = true
# 私钥文件
private_key_files = true

[false_positive_reduction]
# 验证凭证有效性
verify_credentials = false
# 排除测试文件
exclude_test_files = true
# 排除示例/文档
exclude_docs = true`,

  'security-hotspots': `# 安全热点（潜在风险需人工复核）配置

[security_hotspots]
enabled = true

[hotspot_categories]
# 敏感数据暴露
sensitive_data_exposure = true
# 不安全的配置
insecure_configuration = true
# 权限过大
excessive_permissions = true
# 缺少访问控制
missing_access_control = true
# 不安全的默认值
insecure_defaults = true
# 调试信息泄露
debug_info_leak = true
# 不安全的重定向
unsafe_redirect = true
# 文件上传风险
file_upload_risks = true

[review_settings]
# 自动标记需要人工复核的代码
auto_flag_for_review = true
# 严重程度阈值
severity_threshold = "medium"
# 生成安全报告
generate_report = true`,

  'reliability-null': `# 空指针与资源泄漏 检查配置

[null_pointer]
enabled = true
detect_null_deref = true
detect_null_check_after_deref = true

[resource_leak]
enabled = true
detect_unclosed_streams = true
detect_unclosed_connections = true`,

  'reliability-exception': `# 异常处理与错误处理 检查配置

[exception_handling]
enabled = true
detect_empty_catch = true
detect_catch_all = true

[error_handling]
enabled = true
detect_swallowed_exceptions = true
detect_unlogged_errors = true`,

  'reliability-concurrency': `# 并发与线程安全 检查配置

[concurrency]
enabled = true
detect_race_conditions = true
detect_deadlock_risks = true`,

  'maintainability-duplication': `# 代码重复检测 配置

[code_duplication]
enabled = true
min_duplicate_lines = 5
similarity_threshold = 80`,

  'maintainability-complexity': `# 复杂度与代码异味 配置

[complexity]
enabled = true
max_cyclomatic_complexity = 15
max_cognitive_complexity = 20`,

  'maintainability-size': `# 文件与方法大小 配置

[size_checks]
enabled = true
max_file_lines = 500
max_method_lines = 50`,

  'readability-naming': `# 命名规范检查 配置

[naming_convention]
enabled = true
check_class_names = true
check_method_names = true
check_variable_names = true`,

  'readability-comment': `# 注释质量检查 配置

[comment_quality]
enabled = true
min_comment_ratio = 10
check_todo_fixme = true`,

  'readability-format': `# 格式化与风格一致性 配置

[formatting]
enabled = true
max_line_length = 120
check_indentation = true`,

  'performance-query': `# N+1 查询与内存泄漏 配置

[n_plus_one]
enabled = true
detect_loop_queries = true

[memory_leak]
enabled = true
detect_large_allocations = true`,

  'performance-loop': `# 低效循环与阻塞 IO 配置

[inefficient_loop]
enabled = true
detect_nested_loops = true

[blocking_io]
enabled = true
detect_sync_io_in_async = true`,

  'performance-cache': `# 缓存策略检查 配置

[cache_strategy]
enabled = true
check_cache_invalidations = true
check_cache_keys = true`,

  'testability-coverage': `# 单元测试覆盖率 配置

[unit_test_coverage]
enabled = true
min_coverage = 80
exclude_patterns = ["*.generated.*", "*.dto.*"]`,

  'testability-mock': `# 可模拟性与依赖注入 配置

[mockability]
enabled = true
check_sealed_classes = true
check_static_dependencies = true`,

  'testability-isolation': `# 测试隔离性 配置

[test_isolation]
enabled = true
check_shared_state = true
check_global_mutable = true`,

  'operability-logging': `# 日志与监控 配置

[logging]
enabled = true
check_sensitive_data_logging = true
check_log_levels = true

[monitoring]
enabled = true
check_metrics_export = true`,

  'operability-health': `# 健康检查与优雅关闭 配置

[health_check]
enabled = true
check_health_endpoints = true

[graceful_shutdown]
enabled = true
check_shutdown_hooks = true`,

  'operability-config': `# 配置管理检查 配置

[config_management]
enabled = true
check_hardcoded_config = true
check_env_variables = true`,

  'architecture-layer': `# 分层违规与循环依赖 配置

[layer_violation]
enabled = true
enforce_layer_rules = true

[circular_dependency]
enabled = true
detect_cycles = true`,

  'architecture-principle': `# 设计原则检查（SOLID）配置

[solid_principles]
enabled = true
single_responsibility = true
open_closed = true
liskov_substitution = true
interface_segregation = true
dependency_inversion = true`,

  'architecture-api': `# API 设计规范 配置

[api_design]
enabled = true
check_restful_conventions = true
check_versioning = true
check_error_responses = true`
}

// 初始化检查配置编辑器
const initCheckEditor = async () => {
  if (!checkEditorRef.value) {
    console.warn('Editor ref not ready')
    return
  }

  // 销毁旧编辑器
  if (checkEditor.value) {
    checkEditor.value.dispose()
    checkEditor.value = null
  }

  try {
    checkEditor.value = monaco.editor.create(checkEditorRef.value, {
      value: checkConfigs[selectedItem.value] || '',
      language: 'ini',
      theme: 'vs',
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      lineNumbers: 'on',
      fontSize: 13,
      fontFamily: "'JetBrains Mono', 'Consolas', 'Monaco', monospace",
      automaticLayout: true,
      wordWrap: 'on',
      folding: true,
      scrollbar: {
        verticalScrollbarSize: 10,
        horizontalScrollbarSize: 10
      }
    })
  } catch (error) {
    console.error('Failed to init editor:', error)
  }
}
// 保存配置
const saveCheckConfig = () => {
  if (!checkEditor.value) return
  const content = checkEditor.value.getValue()
  checkConfigs[selectedItem.value] = content
  message.success('配置已保存')
}

// 默认配置（用于重置）
const defaultConfigs: Record<string, string> = { ...checkConfigs }

// 重置配置
const resetCheckConfig = () => {
  if (checkEditor.value && defaultConfigs[selectedItem.value]) {
    checkEditor.value.setValue(defaultConfigs[selectedItem.value])
    message.success('配置已重置')
  }
}

// 监听检查项切换
watch(selectedItem, () => {
  nextTick(() => {
    if (checkEditor.value) {
      checkEditor.value.setValue(checkConfigs[selectedItem.value] || '')
    }
  })
})

const tabs = [
  { key: 'quality-check', label: '质量检查项' },
  { key: 'vulnerability-rule', label: '漏洞规则' },
  { key: 'system-config', label: '系统配置' }
]

const activeTab = ref('quality-check')

// 监听 tab 切换，初始化编辑器
watch(activeTab, (tab) => {
  if (tab === 'quality-check') {
    nextTick(() => {
      initCheckEditor()
    })
  }
})
const submitting = ref(false)

const showCreateModal = ref(false)
const showPromptModal = ref(false)
const editingItem = ref<QualityCheckItem | null>(null)
const promptItem = ref<QualityCheckItem | null>(null)

const formData = ref({
  ruleId: '',
  ruleName: '',
  category: 'SECURITY',
  severity: 'MAJOR',
  description: '',
  promptTemplate: '',
  enabled: true,
  sortOrder: 0
})

const handleSubmit = async () => {
  if (!formData.value.ruleId || !formData.value.ruleName || !formData.value.promptTemplate) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    // TODO: 实现 API 调用
    message.success('保存成功')
    closeModal()
  } catch (error: any) {
    console.error(error)
    message.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const closeModal = () => {
  showCreateModal.value = false
  editingItem.value = null
  formData.value = {
    ruleId: '',
    ruleName: '',
    category: 'SECURITY',
    severity: 'MAJOR',
    description: '',
    promptTemplate: '',
    enabled: true,
    sortOrder: 0
  }
}

onMounted(() => {
  nextTick(() => {
    initCheckEditor()
  })
})

onUnmounted(() => {
  if (checkEditor.value) {
    checkEditor.value.dispose()
    checkEditor.value = null
  }
})
</script>

<style scoped>
.platform-management {
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

/* 左右布局容器 */
.content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧设置列表 */
.settings-sidebar {
  width: 180px;
  flex-shrink: 0;
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-base);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  flex: 1;
  padding: 0 var(--spacing-sm);
}

.sidebar-menu-item {
  padding: var(--spacing-sm) var(--spacing-base);
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
}

.sidebar-menu-item:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.sidebar-menu-item.active {
  background: var(--color-info-bg);
  color: var(--color-accent-primary);
  font-weight: 500;
}

.sidebar-menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 20px;
  background: var(--color-accent-primary);
  border-radius: 0 2px 2px 0;
}

.sidebar-menu-item {
  position: relative;
}

/* 右侧内容区域 */
.settings-content {
  flex: 1;
  padding: var(--spacing-sm);
  overflow-y: auto;
}

.content-panel {
  height: 100%;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-base);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 质量检查面板 - 左右布局 */
.quality-check-panel {
  flex-direction: row;
  padding: 0;
}

.check-categories {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  background: var(--color-bg-tertiary);
}

.categories-header {
  padding: var(--spacing-base) var(--spacing-base);
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--color-border);
}

.categories-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.category-group {
  margin-bottom: var(--spacing-xs);
}

.category-header {
  padding: var(--spacing-sm) var(--spacing-base);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  transition: all var(--transition-fast);
}

.category-header:hover {
  background: var(--color-bg-tertiary);
}

.expand-icon {
  font-size: 10px;
  color: var(--color-text-tertiary);
  transition: transform var(--transition-fast);
}

.category-header.expanded .expand-icon {
  transform: rotate(0deg);
}

.category-items {
  margin-top: 2px;
  padding-left: var(--spacing-base);
}

.category-item {
  padding: var(--spacing-xs) var(--spacing-sm);
  margin-bottom: 1px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.category-item:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.category-item.active {
  background: var(--color-info-bg);
  color: var(--color-accent-primary);
  font-weight: 500;
}

.check-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-header {
  padding: var(--spacing-sm) var(--spacing-base);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.editor-title {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.editor-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.editor-container {
  flex: 1;
  min-height: 0;
}

.editor-container .monaco-editor {
  height: 100%;
  width: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-base);
  flex-shrink: 0;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-accent-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
}

.btn-primary:hover {
  background: var(--color-accent-primary-hover);
}

.filter-bar {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-base);
  flex-shrink: 0;
}

.check-items-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.check-item {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-base);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border-light);
}

.item-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.item-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
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
.category-badge.performance { background: #e3f2fd; color: #1976d2; }

.rule-id {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.btn-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-icon:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.btn-icon.danger:hover {
  background: rgba(220, 53, 69, 0.1);
  color: var(--color-error);
}

.item-body {
  padding: var(--spacing-base);
}

.item-title {
  margin: 0 0 var(--spacing-xs);
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--color-text-primary);
}

.item-description {
  margin: 0 0 var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.prompt-preview {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-accent-primary);
  cursor: pointer;
}

.prompt-preview:hover {
  text-decoration: underline;
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

.empty-icon,
.icon {
  font-size: 48px;
  margin-bottom: var(--spacing-base);
  opacity: 0.3;
}

.loading-state {
  gap: var(--spacing-base);
}

.coming-soon {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-tertiary);
}

.coming-soon .icon {
  font-size: 64px;
}

.prompt-content {
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  padding: var(--spacing-base);
  max-height: 400px;
  overflow-y: auto;
}

.prompt-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}
</style>

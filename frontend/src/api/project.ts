import api from '@/utils/request'
import type { 
  Application, 
  ServiceEntity, 
  ServiceVersion,
  CreateApplicationRequest,
  CreateServiceRequest,
  CreateVersionRequest,
  ApiResponse,
  Branch,
  FileNode,
  CodeContent,
  Dependency,
  Vulnerability,
  SecurityScan,
  CodeQualityScan,
  CodeQualityIssue
} from '@/types'

export const applicationApi = {
  getAll() {
    return api.get<ApiResponse<Application[]>>('/applications')
  },

  getById(id: number) {
    return api.get<ApiResponse<Application>>(`/applications/${id}`)
  },

  create(data: CreateApplicationRequest) {
    return api.post<ApiResponse<Application>>('/applications', data)
  },

  update(id: number, data: Partial<Application>) {
    return api.put<ApiResponse<Application>>(`/applications/${id}`, data)
  },

  delete(id: number) {
    return api.delete<ApiResponse<void>>(`/applications/${id}`)
  }
}

export const serviceApi = {
  getByApplicationId(applicationId: number) {
    return api.get<ApiResponse<ServiceEntity[]>>(`/services/application/${applicationId}`)
  },

  getById(id: number) {
    return api.get<ApiResponse<ServiceEntity>>(`/services/${id}`)
  },

  create(data: CreateServiceRequest) {
    return api.post<ApiResponse<ServiceEntity>>('/services', data)
  },

  update(id: number, data: Partial<ServiceEntity>) {
    return api.put<ApiResponse<ServiceEntity>>(`/services/${id}`, data)
  },

  delete(id: number) {
    return api.delete<ApiResponse<void>>(`/services/${id}`)
  },

  getVersions(serviceId: number) {
    return api.get<ApiResponse<ServiceVersion[]>>(`/services/${serviceId}/versions`)
  },

  pull(id: number) {
    return api.post<ApiResponse<ServiceEntity>>(`/services/${id}/pull`)
  },

  getBranches(serviceId: number) {
    return api.get<ApiResponse<Branch[]>>(`/services/${serviceId}/branches`)
  },

  checkoutBranch(serviceId: number, branch: string) {
    return api.post<ApiResponse<void>>(`/services/${serviceId}/checkout`, null, {
      params: { branch }
    })
  },

  getFileTree(serviceId: number, path?: string) {
    return api.get<ApiResponse<FileNode[]>>(`/services/${serviceId}/files`, {
      params: { path }
    })
  },

  getFileContent(serviceId: number, path: string) {
    return api.get<ApiResponse<CodeContent>>(`/services/${serviceId}/content`, {
      params: { path }
    })
  }
}

export const versionApi = {
  getByServiceId(serviceId: number) {
    return api.get<ApiResponse<ServiceVersion[]>>(`/versions/service/${serviceId}`)
  },

  getById(id: number) {
    return api.get<ApiResponse<ServiceVersion>>(`/versions/${id}`)
  },

  create(data: CreateVersionRequest) {
    return api.post<ApiResponse<ServiceVersion>>('/versions', data)
  },

  update(id: number, data: Partial<ServiceVersion>) {
    return api.put<ApiResponse<ServiceVersion>>(`/versions/${id}`, data)
  },

  delete(id: number) {
    return api.delete<ApiResponse<void>>(`/versions/${id}`)
  }
}

export const supplyChainApi = {
  getDependencies(serviceId: number) {
    return api.get<ApiResponse<Dependency[]>>(`/supply-chain/services/${serviceId}/dependencies`)
  },

  parseDependencies(serviceId: number) {
    return api.get<ApiResponse<Dependency[]>>(`/supply-chain/services/${serviceId}/parse`)
  },

  getDependency(id: number) {
    return api.get<ApiResponse<Dependency>>(`/supply-chain/dependencies/${id}`)
  },

  getScans(serviceId: number) {
    return api.get<ApiResponse<SecurityScan[]>>(`/supply-chain/services/${serviceId}/scans`)
  },

  getLatestScan(serviceId: number) {
    return api.get<ApiResponse<SecurityScan>>(`/supply-chain/services/${serviceId}/scans/latest`)
  },

  performScan(serviceId: number) {
    return api.post<ApiResponse<SecurityScan>>(`/supply-chain/services/${serviceId}/scan`)
  },

  getScanProgress(scanId: number) {
    return api.get<ApiResponse<SecurityScan>>(`/supply-chain/scans/${scanId}`)
  },

  getVulnerabilities(dependencyId: number) {
    return api.get<ApiResponse<Vulnerability[]>>(`/supply-chain/dependencies/${dependencyId}/vulnerabilities`)
  },

  getDependenciesByApplication(applicationId: number) {
    return api.get<ApiResponse<Dependency[]>>(`/supply-chain/applications/${applicationId}/dependencies`)
  },

  getLatestScanByApplication(applicationId: number) {
    return api.get<ApiResponse<SecurityScan>>(`/supply-chain/applications/${applicationId}/scans/latest`)
  }
}

export const codeQualityApi = {
  getScans(serviceId: number) {
    return api.get<ApiResponse<CodeQualityScan[]>>(`/code-quality/services/${serviceId}/scans`)
  },

  getScan(scanId: number) {
    return api.get<ApiResponse<CodeQualityScan>>(`/code-quality/scans/${scanId}`)
  },

  getLatestScan(serviceId: number) {
    return api.get<ApiResponse<CodeQualityScan>>(`/code-quality/services/${serviceId}/scans/latest`)
  },

  getLatestScanByApplication(applicationId: number) {
    return api.get<ApiResponse<CodeQualityScan>>(`/code-quality/applications/${applicationId}/scans/latest`)
  },

  getScansByApplication(applicationId: number) {
    return api.get<ApiResponse<CodeQualityScan[]>>(`/code-quality/applications/${applicationId}/scans`)
  },

  startScan(serviceId: number) {
    return api.post<ApiResponse<CodeQualityScan>>(`/code-quality/services/${serviceId}/scan`)
  },

  getIssues(scanId: number, category?: string, severity?: string) {
    const params: Record<string, string> = {}
    if (category) params.category = category
    if (severity) params.severity = severity
    return api.get<ApiResponse<CodeQualityIssue[]>>(`/code-quality/scans/${scanId}/issues`, { params })
  },

  getIssue(issueId: number) {
    return api.get<ApiResponse<CodeQualityIssue>>(`/code-quality/issues/${issueId}`)
  }
}

export const qualityCheckApi = {
  getAll(category?: string, enabled?: boolean) {
    const params: Record<string, string | boolean> = {}
    if (category) params.category = category
    if (enabled !== undefined) params.enabled = enabled
    return api.get<ApiResponse<QualityCheckItem[]>>('/quality-check-items', { params })
  },

  getById(id: number) {
    return api.get<ApiResponse<QualityCheckItem>>(`/quality-check-items/${id}`)
  },

  create(data: Partial<QualityCheckItem>) {
    return api.post<ApiResponse<QualityCheckItem>>('/quality-check-items', data)
  },

  update(id: number, data: Partial<QualityCheckItem>) {
    return api.put<ApiResponse<QualityCheckItem>>(`/quality-check-items/${id}`, data)
  },

  delete(id: number) {
    return api.delete<ApiResponse<void>>(`/quality-check-items/${id}`)
  },

  updateEnabled(id: number, enabled: boolean) {
    return api.put<ApiResponse<void>>(`/quality-check-items/${id}/enabled`, null, {
      params: { enabled }
    })
  }
}

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

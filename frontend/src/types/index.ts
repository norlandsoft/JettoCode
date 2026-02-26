export interface Application {
  id: number
  name: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface ServiceEntity {
  id: number
  applicationId: number
  name: string
  gitUrl: string
  localPath: string
  currentBranch: string
  lastCommit: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface ServiceVersion {
  id: number
  serviceId: number
  version: string
  commitId: string
  description: string
  createdAt: string
}

export interface CreateApplicationRequest {
  name: string
  description?: string
}

export interface CreateServiceRequest {
  applicationId: number
  name: string
  gitUrl: string
  description?: string
}

export interface CreateVersionRequest {
  serviceId: number
  version: string
  commitId: string
  description?: string
}

export interface Project {
  id: number
  name: string
  gitUrl: string
  localPath: string
  currentBranch: string
  lastCommit: string
  lastAnalyzedAt: string | null
  createdAt: string
}

export interface CloneRequest {
  gitUrl: string
  name?: string
  branch?: string
}

export interface Branch {
  name: string
  objectId: string
}

export interface FileNode {
  name: string
  path: string
  type: 'file' | 'directory'
  children?: FileNode[]
}

export interface CodeContent {
  path: string
  content: string
  language: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface ArchitectureNode {
  id: string
  name: string
  type: 'package' | 'class' | 'interface' | 'method'
  children?: ArchitectureNode[]
}

export interface FlowNode {
  id: string
  label: string
  type: 'start' | 'end' | 'process' | 'decision'
}

export interface FlowEdge {
  id: string
  source: string
  target: string
  label?: string
}

export interface CallChainNode {
  id: string
  name: string
  type: 'method' | 'api' | 'service' | 'repository'
}

export interface CallChainEdge {
  source: string
  target: string
}

export interface DiffResult {
  file: string
  additions: number
  deletions: number
  changes: DiffChange[]
}

export interface DiffChange {
  type: 'add' | 'delete' | 'modify'
  oldLine?: number
  newLine?: number
  content: string
  affectedApis: string[]
}

export interface Dependency {
  id: number
  serviceId: number
  name: string
  version: string
  groupId: string
  artifactId: string
  type: string
  scope: string
  license: string
  licenseStatus: string
  purl: string
  filePath: string
  checksum: string
  createdAt: string
  vulnerabilityCount?: number
}

export interface Vulnerability {
  id: number
  dependencyId: number
  cveId: string
  cweId: string
  severity: string
  cvssScore: number
  title: string
  description: string
  affectedVersion: string
  fixedVersion: string
  references: string
  status: string
  createdAt: string
}

export interface SecurityScan {
  id: number
  serviceId: number
  scanType: string
  status: string
  totalDependencies: number
  vulnerableDependencies: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  licenseViolationCount: number
  malwareCount: number
  reportPath: string
  startedAt: string
  completedAt: string
  createdAt: string
}

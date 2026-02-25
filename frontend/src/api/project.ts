import api from '@/utils/request'
import type { 
  Application, 
  ServiceEntity, 
  ServiceVersion,
  CreateApplicationRequest,
  CreateServiceRequest,
  CreateVersionRequest,
  ApiResponse 
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

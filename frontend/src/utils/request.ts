import axios from 'axios'
import type { ApiResponse } from '@/types'
import { message } from 'ant-design-vue'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse<unknown>
    if (!data.success) {
      message.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return response
  },
  (error) => {
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default api

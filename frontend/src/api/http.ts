import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: '',
  timeout: 180000,
})

http.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || error.message || '请求失败，请检查后端服务'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

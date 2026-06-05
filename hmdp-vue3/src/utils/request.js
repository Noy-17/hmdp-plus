// 引入axios
import axios from 'axios'
import JSONbig from 'json-bigint'
import { useUserStore } from '@/stores'
import router from '@/router'
const baseURL = '/api'
const instance = axios.create({
  // TODO 1.设置基础地址和超时时间
  baseURL,
  timeout: 10000,
  // 使用 json-bigint 将超过安全整数范围的数以字符串存储，避免精度丢失
  transformResponse: [
    function (data) {
      try {
        // axios 传入的 data 是原始字符串
        const parser = JSONbig({ storeAsString: true })
        return data ? parser.parse(data) : data
      } catch (e) {
        // 非 JSON 或解析失败，按原始返回
        return data
      }
    }
  ]
})
// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    // TODO 2.请求头里添加token
    if (userStore.token) {
      config.headers.Authorization = `${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)
//响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 判断执行结果
    if (response.status !== 200) {
      // TODO 3.处理业务失败 给出弹窗提示
      ElMessage.error(response.data.errorMsg || '响应拦截器提示：服务异常')
      return Promise.reject(response.data.errorMsg)
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // TODO 4.未登录或者token过期 跳转到登录页
      ElMessage.error('响应拦截器提示：请先登录')
      router.push('/login')
      return
    }
    // TODO 5. 处理一般错误
    ElMessage.error(error.response?.data.message || '响应拦截器提示：服务异常')
    return Promise.reject(error)
  }
)
export default instance
export { baseURL }

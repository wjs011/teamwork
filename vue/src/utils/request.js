import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',  // 后端接口地址
    timeout: 5000  // 请求超时时间
})

// 请求拦截器
request.interceptors.request.use(config => {
    // 从sessionStorage获取token
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    if (user.token) {
        config.headers['token'] = user.token
    }
    return config
}, error => {
    return Promise.reject(error)
})

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        // 如果返回的状态码不是0，说明接口请求有误
        if (res.code !== '0') {
            // 401: 未登录或token过期
            if (res.code === '401') {
                // 清除用户信息
                sessionStorage.removeItem('user')
                // 跳转到登录页
                window.location.href = '/login'
            }
            return Promise.reject(new Error(res.msg || 'Error'))
        } else {
            return res
        }
    },
    error => {
        console.log('err' + error)
        return Promise.reject(error)
    }
)

export default request
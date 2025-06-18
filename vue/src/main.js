import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import i18n from './i18n'

Vue.config.productionTip = false

// 使用ElementUI
Vue.use(ElementUI)

// 配置axios
axios.defaults.baseURL = '/api' // 使用代理地址，将由vue.config.js的proxy配置转发到后端

// 添加请求拦截器，用于调试
axios.interceptors.request.use(
  config => {
    console.log(`发送请求到: ${config.url}`, config);
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

Vue.prototype.$axios = axios

// 响应拦截处理
axios.interceptors.response.use(
  response => {
    console.log(`收到响应: ${response.config.url}`, response.data);
    return response
  },
  error => {
    console.error('API请求错误:', error)
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误数据:', error.response.data);
    }
    ElementUI.Message.error('请求失败: ' + (error.response && error.response.data && error.response.data.message || error.message))
    return Promise.reject(error)
  }
)

new Vue({
  router,
  i18n,
  render: h => h(App)
}).$mount('#app') 
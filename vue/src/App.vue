<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      user: null
    }
  },
  created() {
    // 从sessionStorage获取用户信息
    const userStr = sessionStorage.getItem("user")
    if (userStr) {
      this.user = JSON.parse(userStr)
    }
  }
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}

.el-card {
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-card__header {
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: bold;
}

.el-card__body {
  padding: 20px;
}
</style> 
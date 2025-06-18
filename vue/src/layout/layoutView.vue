<script>
import Header from "@/components/Header.vue";

export default {
  name: 'LayoutView',
  components: {
    Header
  },
  data() {
    return {
      user: null
    }
  },
  created() {
    // 获取用户信息
    const userStr = sessionStorage.getItem("user")
    if (userStr) {
      try {
        this.user = JSON.parse(userStr)
        console.log('Layout中的用户信息:', this.user)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  },
  watch: {
    // 监听路由变化，重新获取用户信息
    '$route': {
      handler() {
        const userStr = sessionStorage.getItem("user")
        if (userStr) {
          try {
            this.user = JSON.parse(userStr)
          } catch (e) {
            console.error('解析用户信息失败:', e)
          }
        }
      },
      immediate: true
    }
  }
}
</script>

<template>
  <div class="layout-container">
    <!--  头部  -->
    <Header :user="user" />
    <!--主体-->
    <div class="main-container">
      <!--内容区域-->
      <div class="content-container">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-container {
  flex: 1;
  display: flex;
}

.content-container {
  flex: 1;
  padding: 20px;
  background-color: #f0f2f5;
}
</style>
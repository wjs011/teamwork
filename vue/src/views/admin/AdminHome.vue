<template>
  <div class="admin-container">
    <el-container>
      <el-aside width="220px">
        <div class="logo-container">
<!--          <img src="@/assets/logo.png" alt="Logo" class="logo">-->
          <span class="system-title">JD评论分析系统</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item index="/admin/dashboard">
            <i class="el-icon-s-home"></i>
            <span>控制台</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <i class="el-icon-user"></i>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/comments">
            <i class="el-icon-chat-line-square"></i>
            <span>评论管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header>
          <div class="header-left">
            <i class="el-icon-s-fold toggle-sidebar" @click="toggleSidebar"></i>
            <breadcrumb class="breadcrumb-container" />
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="el-dropdown-link">
                <el-avatar :size="30" :src="user.avatar || defaultAvatar" style="margin-right: 10px;">
                  <img v-if="!user.avatar" :src="defaultAvatar" alt="" />
                </el-avatar>
                {{ user.nickName || user.username }}
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="person">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout">退出系统</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </el-header>
        
        <el-main>
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>

import Breadcrumb from '@/components/Breadcrumb.vue';

export default {
  name: 'AdminHome',
  components: {
    Breadcrumb
  },
  data() {
    return {
      user: JSON.parse(sessionStorage.getItem('user') || '{}'),
      activeMenu: this.$route.path,
      isCollapse: false,
      defaultAvatar: require('@/assets/default-avatar.png')
    }
  },
  created() {
    console.log('AdminHome created, current route:', this.$route.path);
    // 检查用户权限
    if (this.user.role !== 'admin') {
      this.$message.error('无权访问管理后台');
      this.$router.push('/');
    }
  },
  watch: {
    '$route'(to, from) {
      console.log('Route changed:', { to: to.path, from: from.path });
      this.activeMenu = to.path;
    }
  },
  mounted() {
    console.log('AdminHome mounted');
  },
  methods: {
    toggleSidebar() {
      this.isCollapse = !this.isCollapse;
    },
    async handleCommand(command) {
      if (command === 'person') {
        this.$router.push('/person');
      } else if (command === 'logout') {
        try {
          sessionStorage.removeItem("user")
          this.$message.success('退出成功')
          this.$router.push('/login')
        } catch (error) {
          console.error('退出登录失败:', error);
        }
      }
    }
  }
}
</script>

<style scoped>
.admin-container {
  height: 100vh;
  display: flex;
}

.el-aside {
  background-color: #304156;
  height: 100vh;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  padding: 10px;
  display: flex;
  align-items: center;
  background: #2b3649;
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.system-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.el-header {
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.toggle-sidebar {
  font-size: 20px;
  cursor: pointer;
  margin-right: 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  line-height: 60px;
  cursor: pointer;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
}

.el-menu-vertical {
  border-right: none;
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 220px;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.breadcrumb-container {
  margin-left: 8px;
}
</style> 
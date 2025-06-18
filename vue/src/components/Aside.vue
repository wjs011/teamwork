<script>
export default {
  name: 'Aside',
  data() {
    return {
      activeMenu: 'user'
    }
  },
  methods: {
    navigateTo(routeName) {
      this.$router.push({ name: routeName })
    },
    handleOpen(key, keyPath) {
      console.log(key, keyPath)
    },
    handleClose(key, keyPath) {
      console.log(key, keyPath)
    }
  }
}
</script>

<template>
  <div>
    <el-menu
      :default-active="activeMenu"
      class="el-menu-vertical-demo"
      @open="handleOpen"
      @close="handleClose"
      :default-openeds='["1"]'
    >
      <el-sub-menu index="1">
        <template slot="title">
          <i class="el-icon-location"></i>
          <span>系统管理</span>
        </template>
        <el-menu-item index="user" @click="navigateTo('home')">用户管理</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<style scoped>
.el-menu-vertical-demo {
  height: 100%;
  border-right: none;
}
</style>
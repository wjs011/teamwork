<script setup>
import { useRouter, useRoute } from 'vue-router';
import {
  Document,
  Menu as IconMenu,
  Location,
  Setting,
} from '@element-plus/icons-vue'
import { computed } from 'vue';

const router = useRouter();
const route = useRoute();

const defaultActive = computed(() => {
  // 根据当前路由名称设置 active 菜单项
  switch (route.name) {

    default:
      return 'user'; // 默认激活 user
  }
});

const navigateTo = (routeName) => {
  router.push({ name: routeName });
};

const handleOpen = (key, keyPath) => {
  console.log(key, keyPath);
};

const handleClose = (key, keyPath) => {
  console.log(key, keyPath);
};
</script>

<template>
  <div>
    <el-menu
      :default-active="defaultActive"
      class="el-menu-vertical-demo"
      @open="handleOpen"
      @close="handleClose"
      :default-openeds='["1"]'
    >
    <el-sub-menu index="1">
      <template #title>
        <el-icon><location /></el-icon>
        <span>系统管理</span>
      </template>
      <el-menu-item index="user" @click="navigateTo('home')">用户管理</el-menu-item>
    </el-sub-menu>
    </el-menu>
  </div>
</template>

<style scoped>
</style>
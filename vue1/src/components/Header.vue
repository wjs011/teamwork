<template>
  <div style="height: 50px; line-height: 50px; border-bottom: 1px solid #ccc; display:flex;" >
    <div style="width: 200px; padding-left: 30px; font-weight: bold;color: dodgerblue ">后台管理</div>
    <div style="flex: 1"></div>
    <div style="width: 150px; display: flex; align-items: center; justify-content: flex-end;">
      <el-dropdown>
        <span class="el-dropdown-link">
          <!-- 头像 -->
          <el-avatar :size="30" :src="user?.avatar" style="margin-right: 10px;">
            <img v-if="!user?.avatar" src="../../../src/main/resources/files/OIP-C.jpg"  alt=""/> <!-- 默认头像 -->
          </el-avatar>
<!--          {{ JSON.parse(sessionStorage.getItem("user")).nickName }}-->
          {{user?.nickName }}
          <el-icon class="el-icon--right">
            <arrow-down />
          </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handlePersonClick">个人信息</el-dropdown-item>
            <el-dropdown-item @click="handleLogoutClick">退出系统</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

  </div>
</template>

<script>
import { ArrowDown } from '@element-plus/icons-vue'
import router from "@/router";
import {createRouter as $router} from "vue-router";
export default {
  name: 'Header',
  props:['user'],
  data(){
    return {
      user:{}
    }
  },
  created() {
    let userStr = sessionStorage.getItem("user") || "{}"
    this.user = JSON.parse(userStr)

  },
  methods: {
    handlePersonClick() {
      console.log("点击了个人信息"); // 打印日志
      this.$router.push('/person');
    },
    handleLogoutClick() {
      console.log("点击了退出系统"); // 打印日志
      this.$router.push('/login');
    }
  },
  components: {ArrowDown}
}
</script>

<style scoped>
.el-dropdown-link {
  line-height: 50px;
  cursor: pointer;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;

}
el-dropdown {
  border: none; /* 移除边框 */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); /* 添加阴影（可选） */
}

.el-dropdown-link {
  border: none; /* 移除边框 */
  outline: none; /* 移除轮廓 */
}
</style>
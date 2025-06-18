<template>
  <div class="login-container">
    <div class="login-box">
      <h2>JD评论分析系统</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="role">
          <el-radio-group v-model="loginForm.role" class="role-select">
            <el-radio label="user">普通用户</el-radio>
            <el-radio label="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            prefix-icon="el-icon-user"
            placeholder="请输入用户名">
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请输入密码"
            @keyup.enter.native="handleLogin">
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            class="login-button" 
            :loading="loading"
            @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        <div class="register-link" v-if="loginForm.role === 'user'">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
        role: 'user' // 默认选择普通用户
      },
      loading: false,
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择登录身份', trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          const loginUrl = this.loginForm.role === 'admin' ? '/admin/login' : '/user/login';
          
          axios.post(loginUrl, this.loginForm).then(res => {
            if (res.data.code === '0') {
              // 保存用户信息到sessionStorage
              const userData = res.data.data;
              userData.role = this.loginForm.role; // 添加角色标识
              console.log('登录成功，保存的用户信息:', userData);
              sessionStorage.setItem('user', JSON.stringify(userData));
              this.$message.success('登录成功');
              
              // 根据角色跳转到不同页面
              if (this.loginForm.role === 'admin') {
                this.$router.push('/admin/dashboard');
              } else {
                this.$router.push('/');
              }
            } else {
              this.$message.error(res.data.msg || '登录失败');
            }
          }).catch(error => {
            console.error('登录失败:', error);
            this.$message.error('登录失败: ' + (error.response?.data?.msg || error.message));
          }).finally(() => {
            this.loading = false;
          });
        }
      });
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  color: #409EFF;
  margin-bottom: 30px;
}

.login-form {
  margin-top: 20px;
}

.role-select {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.login-button {
  width: 100%;
}

.register-link {
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}

.register-link a {
  color: #409EFF;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style> 
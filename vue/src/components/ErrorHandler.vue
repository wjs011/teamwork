<template>
  <div v-if="visible" class="error-container">
    <div class="error-box">
      <i class="el-icon-error error-icon"></i>
      <h3 class="error-title">{{ title }}</h3>
      <p class="error-message">{{ message }}</p>
      <div class="error-actions">
        <el-button type="primary" @click="tryAgain">重试</el-button>
        <el-button @click="close">关闭</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ErrorHandler',
  props: {
    title: {
      type: String,
      default: '出错了'
    },
    message: {
      type: String,
      default: '系统发生错误，请稍后重试'
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    tryAgain() {
      this.$emit('retry');
    },
    close() {
      this.$emit('close');
    }
  }
}
</script>

<style scoped>
.error-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.error-box {
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  width: 400px;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.error-icon {
  font-size: 60px;
  color: #F56C6C;
  margin-bottom: 20px;
}

.error-title {
  font-size: 24px;
  margin: 0 0 10px 0;
  color: #303133;
}

.error-message {
  font-size: 16px;
  color: #606266;
  margin: 0 0 20px 0;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}
</style> 
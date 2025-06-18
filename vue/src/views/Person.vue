<template>
  <div class="person">
    <el-card style="width: 40%;margin: 10px">
      <div style="text-align: center; margin-bottom: 20px">
        <el-upload
            class="avatar-uploader"
            action="http://localhost:8080/files/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
        >
          <el-avatar :size="100" :src="form.avatar">
            <img :src="defaultAvatar" v-if="!form.avatar" alt=""/>
          </el-avatar>
        </el-upload>
      </div>
      <el-form ref="form" :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickName"></el-input>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input v-model="form.age"></el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-input v-model="form.gender"></el-input>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password></el-input>
        </el-form-item>
      </el-form>
      <div style="text-align: center">
        <el-button type="primary" @click="update">确定</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import request from "../utils/request";

export default {
  name: 'Person',
  data() {
    return {
      form: {},
      defaultAvatar: require('@/assets/default-avatar.png'),
      fromPath: '/'
    }
  },
  created() {
    let str = sessionStorage.getItem("user") || "{}";
    this.form = JSON.parse(str);
    this.fromPath = this.$route.query.from || '/';
  },
  methods: {
    handleAvatarSuccess(res) {
      if (res.code === '0') {
        this.form.avatar = res.data; // 更新头像路径
      }
    },
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 5;
      if (!isImage) this.$message.error('只能上传图片格式!');
      if (!isLt2M) this.$message.error('图片大小不能超过5MB!');
      return isImage && isLt2M;
    },
    update() {
      const user = JSON.parse(sessionStorage.getItem('user') || '{}');
      if(user.role === 'user'){
        request.put('/user', this.form).then((res) => {
          console.log(res)
          if (res.code === '0') {
            this.$message({
              type: 'success',
              message: "更新成功"
            })
            sessionStorage.setItem("user", JSON.stringify(this.form));
            this.$router.push(this.fromPath);
          } else {
            this.$message({
              type: 'error',
              message: res.data.msg
            })
          }
        })
      }
      else{
        request.put('/admin/update', this.form).then((res) => {
          console.log(res)
          if (res.code === '0') {
            this.$message({
              type: 'success',
              message: "更新成功"
            })
            sessionStorage.setItem("user", JSON.stringify(this.form));
            this.$router.push(this.fromPath);
          } else {
            this.$message({
              type: 'error',
              message: res.data.msg
            })
          }
        })
      }

      
    }
  }
}
</script>

<style scoped>
.person {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.avatar-uploader >>> .el-upload {
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}

.avatar-uploader >>> .el-upload:hover {
  border-color: #409EFF;
}
</style>
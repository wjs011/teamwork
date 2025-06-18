<template>
  <div class="user-management">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索昵称"
        class="search-input"
        prefix-icon="el-icon-search"
        clearable
        @keyup.enter.native="handleSearch"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
      </el-input>
    </div>
    <el-card>
      <el-table
        :data="users"
        border
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{background:'#f5f7fa'}"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="150" align="center" />
        <el-table-column prop="nickName" label="昵称" width="150" align="center" />
        <el-table-column prop="age" label="年龄" width="80" align="center" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="address" label="地址" width="180" align="center" />
        <el-table-column prop="avatar" label="头像" width="100" align="center">
          <template slot-scope="scope">
            <el-avatar :src="scope.row.avatar" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'UserManagement',
  data() {
    return {
      users: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      searchKeyword: '',
      loading: false
    }
  },
  created() {
    this.loadUsers();
  },
  methods: {
    async loadUsers() {
      this.loading = true;
      try {
        const response = await axios.get('/user', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            search: this.searchKeyword
          }
        });
        if (response.data.code === '0') {
          this.users = response.data.data.records || [];
          this.total = response.data.data.total || 0;
        }
      } catch (error) {
        this.$message.error('加载用户列表失败');
      } finally {
        this.loading = false;
      }
    },
    handleSearch() {
      this.currentPage = 1;
      this.loadUsers();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.loadUsers();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadUsers();
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该用户?', '提示', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        });
        const user = JSON.parse(sessionStorage.getItem('user') || '{}');
        const token = user.token || '';
        const response = await axios.delete(`/user/${row.id}`, {
          headers: { Authorization: token }
        });
        if (response.data.code === '0') {
          this.$message.success('删除成功');
          this.loadUsers();
        } else {
          this.$message.error(response.data.msg || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败');
        }
      }
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.search-input {
  width: 300px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style> 
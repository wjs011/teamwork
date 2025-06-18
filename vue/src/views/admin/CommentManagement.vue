<template>
  <div class="comment-management">
    <div class="page-header">
      <div class="header-left">
        <h3>评论管理</h3>
        <el-tag type="success" effect="plain" class="total-tag">
          总评论数: {{ total }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索评论内容"
          class="search-input"
          prefix-icon="el-icon-search"
          clearable
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
        <el-button type="primary" icon="el-icon-refresh" @click="loadComments">刷新</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="评分">
          <el-select v-model="filterForm.score" placeholder="选择评分" clearable>
            <el-option label="5星" :value="5"></el-option>
            <el-option label="4星" :value="4"></el-option>
            <el-option label="3星" :value="3"></el-option>
            <el-option label="2星" :value="2"></el-option>
            <el-option label="1星" :value="1"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="商品ID">
          <el-input v-model="filterForm.productId" placeholder="输入商品ID" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">筛选</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        :data="comments"
        border
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{background:'#f5f7fa'}"
      >
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="productId" label="商品ID" width="120" align="center"></el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="comment-content">
              <span>{{ scope.row.content }}</span>
              <el-button
                type="text"
                size="mini"
                @click="showFullContent(scope.row)"
                v-if="scope.row.content.length > 50"
              >
                查看全文
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户昵称" width="120" align="center"></el-table-column>
        <el-table-column prop="score" label="评分" width="120" align="center">
          <template slot-scope="scope">
            <el-rate
              v-model="scope.row.score"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}">
            </el-rate>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180" align="center"></el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
            >查看</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
            >删除</el-button>
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
          background>
        </el-pagination>
      </div>
    </el-card>

    <!-- 评论详情对话框 -->
    <el-dialog
      title="评论详情"
      :visible.sync="dialogVisible"
      width="50%"
      :before-close="handleClose">
      <div class="comment-detail" v-if="currentComment">
        <div class="detail-item">
          <span class="label">评论ID：</span>
          <span>{{ currentComment.id }}</span>
        </div>
        <div class="detail-item">
          <span class="label">商品ID：</span>
          <span>{{ currentComment.productId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">用户昵称：</span>
          <span>{{ currentComment.nickname }}</span>
        </div>
        <div class="detail-item">
          <span class="label">评分：</span>
          <el-rate
            v-model="currentComment.score"
            disabled
            show-score
            text-color="#ff9900">
          </el-rate>
        </div>
        <div class="detail-item">
          <span class="label">评论时间：</span>
          <span>{{ currentComment.createTime }}</span>
        </div>
        <div class="detail-item">
          <span class="label">评论内容：</span>
          <div class="content-box">{{ currentComment.content }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'CommentManagement',
  data() {
    return {
      comments: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      searchKeyword: '',
      loading: false,
      dialogVisible: false,
      currentComment: null,
      filterForm: {
        score: '',
        dateRange: [],
        productId: ''
      }
    }
  },
  created() {
    this.loadComments();
  },
  methods: {
    async loadComments() {
      this.loading = true;
      try {
        // 普通加载（支持多条件筛选）
        const response = await axios.get('/comments', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            keyword: this.searchKeyword,
            score: this.filterForm.score,
            minScore: this.filterForm.score || undefined,
            maxScore: this.filterForm.score || undefined,
            startDate: this.filterForm.dateRange[0],
            endDate: this.filterForm.dateRange[1],
            productId: this.filterForm.productId
          }
        });
        if (response.data.code === '0') {
          this.comments = response.data.data.records;
          this.total = response.data.data.total;
        }
      } catch (error) {
        console.error('加载评论列表失败:', error);
        this.$message.error('加载评论列表失败');
      } finally {
        this.loading = false;
      }
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该评论?', '提示', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        });
        const response = await axios.delete(`/comments/${row.productId}/${row.id}`);
        if (response.data.code === '0') {
          this.$message.success('删除成功');
          this.loadComments();
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除评论失败:', error);
          this.$message.error('删除评论失败');
        }
      }
    },
    async handleFilter() {
      this.currentPage = 1;
      this.loadComments();
    },
    async handleSearch() {
      this.currentPage = 1;
      this.loading = true;
      try {
        if (this.searchKeyword) {
          // 关键词筛选，支持全局和单商品
          const response = await axios.get('/comments/search', {
            params: {
              productId: this.productId || '',
              keyword: this.searchKeyword
            }
          });
          if (response.data.code === '0') {
            this.comments = response.data.data;
            this.total = this.comments.length;
          }
          this.loading = false;
          return;
        }
        this.loadComments();
      } catch (error) {
        this.$message.error('搜索评论失败');
        this.loading = false;
      }
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.loadComments();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadComments();
    },
    resetFilter() {
      this.filterForm = {
        score: '',
        dateRange: [],
        productId: ''
      };
      this.loadComments();
    },
    handleView(row) {
      this.currentComment = row;
      this.dialogVisible = true;
    },
    handleClose() {
      this.dialogVisible = false;
      this.currentComment = null;
    },
    showFullContent(row) {
      this.currentComment = row;
      this.dialogVisible = true;
    }
  }
}
</script>

<style scoped>
.comment-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.total-tag {
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 15px;
}

.search-input {
  width: 300px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-card {
  margin-bottom: 20px;
}

.comment-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.comment-detail {
  padding: 20px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
}

.label {
  font-weight: bold;
  width: 100px;
  color: #606266;
}

.content-box {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style> 
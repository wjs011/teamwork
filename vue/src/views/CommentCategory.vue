<template>
  <div class="comment-category">
    <div class="header-area">
      <div class="back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i> 返回
      </div>
      <h2 class="page-title">评论分类</h2>
    </div>

    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>分类管理</span>
        <el-tooltip content="管理评论分类，可以添加、编辑和删除分类" placement="top">
          <i class="el-icon-question"></i>
        </el-tooltip>
        <el-button style="float: right; padding: 3px 0" type="text" @click="showAddDialog">添加分类</el-button>
      </div>
      
      <!-- 分类列表 -->
      <el-table :data="categories" style="width: 100%">
        <el-table-column prop="name" label="分类名称" width="180"></el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
        <el-table-column prop="priority" label="优先级" width="100"></el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 添加/编辑分类对话框 -->
      <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
        <el-form :model="categoryForm" :rules="rules" ref="categoryForm" label-width="100px">
          <el-form-item label="分类名称" prop="name">
            <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
            <el-tooltip content="分类的名称，用于标识不同类型的评论" placement="top">
              <i class="el-icon-question form-tooltip"></i>
            </el-tooltip>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="categoryForm.description" placeholder="请输入分类描述"></el-input>
            <el-tooltip content="分类的详细描述，帮助理解分类的用途" placement="top">
              <i class="el-icon-question form-tooltip"></i>
            </el-tooltip>
          </el-form-item>
          <el-form-item label="关键词" prop="keywords">
            <el-tag
              :key="index"
              v-for="(keyword, index) in categoryForm.keywords"
              closable
              :disable-transitions="false"
              @close="handleKeywordClose(index)">
              {{keyword}}
            </el-tag>
            <el-input
              class="input-new-keyword"
              v-if="inputVisible"
              v-model="inputValue"
              ref="saveTagInput"
              size="small"
              @keyup.enter.native="handleInputConfirm"
              @blur="handleInputConfirm">
            </el-input>
            <el-button v-else class="button-new-keyword" size="small" @click="showInput">+ 添加关键词</el-button>
            <el-tooltip content="用于匹配评论内容的关键词，多个关键词用逗号分隔" placement="top">
              <i class="el-icon-question form-tooltip"></i>
            </el-tooltip>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-input-number v-model="categoryForm.priority" :min="0" :max="100"></el-input-number>
            <el-tooltip content="分类的优先级，数字越大优先级越高" placement="top">
              <i class="el-icon-question form-tooltip"></i>
            </el-tooltip>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleSave">确 定</el-button>
        </div>
      </el-dialog>
    </el-card>

    <!-- 评论分类结果 -->
    <el-card class="box-card" style="margin-top: 20px">
      <div slot="header" class="clearfix">
        <span>分类统计</span>
        <el-tooltip content="显示每个分类下的评论数量统计" placement="top">
          <i class="el-icon-question"></i>
        </el-tooltip>
      </div>
      <div class="category-stats">
        <div v-for="(count, category) in categoryStats" :key="category" class="stat-item">
          <div class="stat-name">{{ category }}</div>
          <div class="stat-count">{{ count }}</div>
          <el-button 
            type="text" 
            size="small" 
            @click="showCategoryComments(category)">
            查看评论
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 分类评论列表对话框 -->
    <el-dialog
      :title="`${currentCategory} 的评论`"
      :visible.sync="commentsDialogVisible"
      width="70%">
      <div class="category-comments">
        <div v-if="categoryComments.length === 0" class="empty-comments">
          暂无评论
        </div>
        <div v-else class="comment-list">
          <div v-for="comment in categoryComments" :key="comment.id" class="comment-item">
            <div class="comment-user">
              <i class="el-icon-user"></i>
              <span class="username">{{ comment.nickname || '匿名用户' }}</span>
            </div>
            <div class="score-display">
              <i v-for="n in 5" :key="n"
                 :class="n <= comment.score ? 'el-icon-star-on' : 'el-icon-star-off'"
                 :style="{ color: n <= comment.score ? '#f7ba2a' : '#dcdfe6' }">
              </i>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-time" v-if="comment.createTime">
              {{ new Date(comment.createTime).toLocaleString() }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'CommentCategory',
  data() {
    return {
      categories: [],
      categoryStats: {},
      dialogVisible: false,
      dialogTitle: '添加分类',
      categoryForm: {
        id: null,
        name: '',
        description: '',
        keywords: [],
        priority: 0
      },
      inputVisible: false,
      inputValue: '',
      productId: '',
      commentsDialogVisible: false,
      currentCategory: '',
      categoryComments: [],
      rules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符' }
        ],
        description: [
          { required: true, message: '请输入分类描述', trigger: 'blur' },
          { min: 2, max: 200, message: '长度在 2 到 200 个字符' }
        ],
        keywords: [
          { required: true, message: '请添加关键词', trigger: 'blur' }
        ],
        priority: [
          { required: true, message: '请输入优先级', trigger: 'blur' },
          { type: 'number', min: 0, max: 100, message: '优先级应在 0 到 100 之间' }
        ]
      }
    }
  },
  created() {
    this.productId = this.$route.query.productId
    this.loadCategories()
    if (this.productId) {
      this.loadCategoryStats()
    }
  },
  methods: {
    async loadCategories() {
      try {
        const response = await axios.get('/api/categories')
        this.categories = response.data.data
      } catch (error) {
        this.$message.error('加载分类失败')
      }
    },
    async loadCategoryStats() {
      try {
        const response = await axios.get(`/api/categories/statistics/${this.productId}`)
        this.categoryStats = response.data.data
      } catch (error) {
        this.$message.error('加载分类统计失败')
      }
    },
    showAddDialog() {
      this.dialogTitle = '添加分类'
      this.categoryForm = {
        id: null,
        name: '',
        description: '',
        keywords: [],
        priority: 0
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑分类'
      this.categoryForm = {
        id: row.id,
        name: row.name,
        description: row.description,
        keywords: JSON.parse(row.keywords),
        priority: row.priority
      }
      this.dialogVisible = true
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该分类吗？', '提示', {
          type: 'warning'
        })
        await axios.delete(`/api/categories/${row.id}`)
        this.$message.success('删除成功')
        this.loadCategories()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },
    handleClose(tag) {
      this.categoryForm.keywords.splice(this.categoryForm.keywords.indexOf(tag), 1)
    },
    showInput() {
      this.inputVisible = true
      this.$nextTick(() => {
        this.$refs.saveTagInput.$refs.input.focus()
      })
    },
    handleInputConfirm() {
      let inputValue = this.inputValue
      if (inputValue) {
        this.categoryForm.keywords.push(inputValue)
      }
      this.inputVisible = false
      this.inputValue = ''
    },
    async handleSave() {
      try {
        const data = {
          ...this.categoryForm,
          keywords: JSON.stringify(this.categoryForm.keywords)
        }
        if (this.categoryForm.id) {
          await axios.put(`/api/categories/${this.categoryForm.id}`, data)
        } else {
          await axios.post('/api/categories', data)
        }
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.loadCategories()
        if (this.productId) {
          this.loadCategoryStats()
        }
      } catch (error) {
        this.$message.error('保存失败')
      }
    },
    async showCategoryComments(category) {
      this.currentCategory = category
      this.commentsDialogVisible = true
      try {
        const response = await axios.get(`/api/categories/category-comments/${this.productId}`, {
          params: { category }
        })
        if (response.data.code === '0') {
          this.categoryComments = response.data.data || []
        } else {
          this.$message.error('获取评论失败')
        }
      } catch (error) {
        console.error('获取评论失败:', error)
        this.$message.error('获取评论失败')
      }
    },
    goBack() {
      this.$router.go(-1)
    },
    handleKeywordClose(index) {
      this.categoryForm.keywords.splice(index, 1)
    }
  }
}
</script>

<style scoped>
.comment-category {
  padding: 20px;
}
.category-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.stat-item {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  min-width: 120px;
  text-align: center;
  position: relative;
  padding-bottom: 30px;
}
.stat-name {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}
.stat-count {
  font-size: 24px;
  color: #409EFF;
  font-weight: bold;
}
.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
}
.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
.category-comments {
  max-height: 500px;
  overflow-y: auto;
}

.comment-list {
  padding: 10px;
}

.comment-item {
  background: #f5f7fa;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 4px;
}

.comment-user {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.comment-user i {
  margin-right: 5px;
  color: #909399;
}

.username {
  color: #606266;
  font-weight: 500;
}

.score-display {
  margin-bottom: 8px;
}

.score-display i {
  margin-right: 2px;
}

.comment-content {
  color: #303133;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.empty-comments {
  text-align: center;
  color: #909399;
  padding: 20px;
}

.stat-item .el-button {
  position: absolute;
  bottom: 5px;
  left: 50%;
  transform: translateX(-50%);
}

.header-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.back-button {
  cursor: pointer;
  color: #409EFF;
  font-size: 14px;
  margin-right: 20px;
  transition: color 0.3s;
  display: flex;
  align-items: center;
}

.back-button:hover {
  color: #66b1ff;
}

.back-button i {
  margin-right: 5px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 500;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 5px;
}

.el-icon-question {
  color: #909399;
  font-size: 16px;
  cursor: help;
  margin-left: 5px;
}

.el-icon-question:hover {
  color: #409EFF;
}

.el-form-item {
  position: relative;
  display: flex;
  align-items: flex-start;
}

.el-form-item .el-form-item__content {
  position: relative;
  display: flex;
  align-items: center;
}

.form-tooltip {
  color: #909399;
  font-size: 16px;
  cursor: help;
  margin-left: 8px;
  vertical-align: middle;
}

.form-tooltip:hover {
  color: #409EFF;
}
</style> 
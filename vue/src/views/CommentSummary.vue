<template>
  <div class="comment-summary">
    <div class="header-area">
      <div class="back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i> 返回
      </div>
      <h2 class="page-title">评论摘要</h2>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <div class="filter-container">
        <div class="filter-item">
          <el-select v-model="timeRange" placeholder="时间范围" @change="generateSummary">
            <el-option label="全部时间" value="all"></el-option>
            <el-option label="最近一周" value="week"></el-option>
            <el-option label="最近一月" value="month"></el-option>
            <el-option label="最近三月" value="quarter"></el-option>
          </el-select>
          <el-tooltip content="选择要分析的评论时间范围" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>

        <div class="filter-item">
          <el-select v-model="scoreFilter" placeholder="评分范围" @change="generateSummary">
            <el-option label="全部评分" value="0"></el-option>
            <el-option label="5星" value="5"></el-option>
            <el-option label="4星及以上" value="4"></el-option>
            <el-option label="3星及以上" value="3"></el-option>
            <el-option label="2星及以上" value="2"></el-option>
            <el-option label="1星及以上" value="1"></el-option>
          </el-select>
          <el-tooltip content="选择要分析的评论评分范围" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>

        <el-button type="primary" @click="generateSummary">生成摘要</el-button>
      </div>
    </el-card>

    <!-- 摘要内容 -->
    <el-row :gutter="20" class="summary-content">
      <!-- 核心观点 -->
      <el-col :span="16">
        <el-card class="summary-card">
          <div slot="header" class="card-header">
            <span>核心观点</span>
            <el-tooltip content="系统自动提取的评论核心观点，包含提及次数和平均评分" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </div>
          <div class="key-points">
            <div v-for="(point, index) in summary.keyPoints" :key="index" class="point-item">
              <div class="point-header">
                <i class="el-icon-star-on" :style="{ color: getScoreColor(point.score) }"></i>
                <span class="point-title">{{ point.title }}</span>
              </div>
              <div class="point-content">{{ point.content }}</div>
              <div class="point-meta">
                <span class="point-count">提及次数: {{ point.count }}</span>
                <span class="point-score">平均评分: {{ point.score.toFixed(1) }}</span>
              </div>
            </div>
            <div class="pagination-container" v-if="total > 0">
              <el-pagination
                @current-change="handleCurrentChange"
                :current-page="currentPage"
                :page-size="pageSize"
                layout="prev, pager, next"
                :total="total">
              </el-pagination>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 统计信息 -->
      <el-col :span="8">
        <el-card class="summary-card">
          <div slot="header" class="card-header">
            <span>统计信息</span>
            <el-tooltip content="评论总数、平均评分、正面评价和负面评价的百分比" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </div>
          <div class="statistics">
            <div class="stat-item">
              <div class="stat-label">评论总数</div>
              <div class="stat-value">{{ summary.totalComments }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">平均评分</div>
              <div class="stat-value">{{ summary.averageScore.toFixed(1) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">正面评价</div>
              <div class="stat-value">{{ summary.positiveRate }}%</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">负面评价</div>
              <div class="stat-value">{{ summary.negativeRate }}%</div>
            </div>
          </div>
        </el-card>

        <!-- 情感分布 -->
        <el-card class="summary-card" style="margin-top: 20px">
          <div slot="header" class="card-header">
            <span>情感分布</span>
            <el-tooltip content="评论情感分布饼图，展示正面、负面和中性评价的比例" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </div>
          <div class="sentiment-chart" ref="sentimentChart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import axios from 'axios'
import * as echarts from 'echarts'

export default {
  name: 'CommentSummary',
  data() {
    return {
      productId: '',
      timeRange: 'all',
      scoreFilter: '0',
      currentPage: 1,
      pageSize: 5,
      total: 0,
      summary: {
        keyPoints: [],
        totalComments: 0,
        averageScore: 0,
        positiveRate: 0,
        negativeRate: 0
      },
      sentimentChart: null
    }
  },
  created() {
    this.productId = this.$route.query.productId
    this.generateSummary()
  },
  mounted() {
    this.initSentimentChart()
  },
  beforeDestroy() {
    if (this.sentimentChart) {
      this.sentimentChart.dispose()
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1)
    },
    async generateSummary() {
      try {
        const response = await axios.get(`/comments/summary/${this.productId}`, {
          params: {
            timeRange: this.timeRange,
            scoreFilter: this.scoreFilter,
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })
        if (response.data.code === '0') {
          this.summary = response.data.data
          this.total = response.data.data.total
          this.$nextTick(() => {
            this.updateSentimentChart()
          })
        } else {
          this.$message.error('生成摘要失败')
        }
      } catch (error) {
        console.error('生成摘要失败:', error)
        this.$message.error('生成摘要失败')
      }
    },
    initSentimentChart() {
      this.sentimentChart = echarts.init(this.$refs.sentimentChart)
      this.updateSentimentChart()
    },
    updateSentimentChart() {
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            type: 'pie',
            radius: '50%',
            data: [
              { value: this.summary.positiveRate, name: '正面评价' },
              { value: this.summary.negativeRate, name: '负面评价' },
              { value: 100 - this.summary.positiveRate - this.summary.negativeRate, name: '中性评价' }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      this.sentimentChart.setOption(option)
    },
    getScoreColor(score) {
      if (score >= 4.5) return '#67C23A'
      if (score >= 4) return '#85CE61'
      if (score >= 3) return '#E6A23C'
      if (score >= 2) return '#F56C6C'
      return '#F56C6C'
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.generateSummary()
    }
  }
}
</script>

<style scoped>
.comment-summary {
  padding: 20px;
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

.filter-card {
  margin-bottom: 20px;
}

.filter-container {
  display: flex;
  gap: 15px;
}

.summary-content {
  margin-top: 20px;
}

.summary-card {
  margin-bottom: 20px;
}

.key-points {
  padding: 10px;
}

.point-item {
  background: #f5f7fa;
  padding: 15px;
  margin-bottom: 15px;
  border-radius: 4px;
}

.point-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.point-title {
  margin-left: 8px;
  font-weight: 500;
  color: #303133;
}

.point-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.point-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.statistics {
  padding: 10px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #606266;
}

.stat-value {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.sentiment-chart {
  height: 300px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
  padding: 10px 0;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 5px;
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
}

.el-icon-question:hover {
  color: #409EFF;
}
</style> 
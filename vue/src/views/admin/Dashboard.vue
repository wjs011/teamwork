<template>
  <div class="dashboard">
    <div v-if="loading" class="loading-container">
      <el-loading :fullscreen="true" text="加载中..."></el-loading>
    </div>
    
    <div v-else>
      <!-- 关键词分析卡片 -->
      <el-row :gutter="20" class="data-overview">
        <el-col :span="12">
          <el-card shadow="hover" class="data-card">
            <div slot="header" class="card-header">
              <span>热门关键词</span>
              <el-select v-model="selectedProduct" placeholder="选择商品" clearable @change="loadKeywordData">
                <el-option
                  v-for="item in productList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id">
                </el-option>
              </el-select>
            </div>
            <div class="chart-container">
              <v-chart :options="keywordOptions" autoresize />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover" class="data-card">
            <div slot="header" class="card-header">
              <span>关键词趋势</span>
              <el-radio-group v-model="trendTimeRange" size="small" @change="loadKeywordTrend">
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="halfYear">半年</el-radio-button>
              </el-radio-group>
            </div>
            <div class="chart-container">
              <v-chart :options="keywordTrendOptions" autoresize />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 评论质量分析 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card class="chart-card">
            <div slot="header" class="card-header">
              <span>评论质量分析</span>
              <div class="header-actions">
                <el-select v-model="qualityMetric" placeholder="选择指标" @change="loadQualityData">
                  <el-option label="评论长度分布" value="length"></el-option>
                  <el-option label="评论情感分布" value="sentiment"></el-option>
                </el-select>
              </div>
            </div>
            <div class="chart-container">
              <v-chart :options="qualityOptions" autoresize />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card">
            <div slot="header" class="card-header">
              <span>评论质量趋势</span>
              <el-radio-group v-model="qualityTrendRange" size="small" @change="loadQualityTrend">
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="halfYear">半年</el-radio-button>
              </el-radio-group>
            </div>
            <div class="chart-container">
              <v-chart :options="qualityTrendOptions" autoresize />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最新评论 -->
      <el-card class="recent-comments">
        <div slot="header" class="recent-header">
          <span>最新评论</span>
          <el-button type="text" @click="$router.push('/admin/comments')">查看更多</el-button>
        </div>
        <el-table :data="recentComments" style="width: 100%" v-loading="loading">
          <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
            <template slot-scope="scope">
              <div class="comment-content">
                <span>{{ scope.row.content }}</span>
                <el-tag size="mini" type="info" v-if="scope.row.keywords">
                  {{ scope.row.keywords.join(', ') }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="productId" label="商品" width="150"></el-table-column>
          <el-table-column prop="createTime" label="时间" width="180"></el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import VChart from 'vue-echarts';
import * as echarts from 'echarts';

export default {
  name: 'Dashboard',
  components: {
    VChart
  },
  data() {
    console.log('Dashboard data initialized');
    return {
      loading: false,
      selectedProduct: '',
      trendTimeRange: 'month',
      qualityMetric: 'length',
      qualityTrendRange: 'month',
      productList: [],
      recentComments: [],
      keywordOptions: {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value'
        },
        yAxis: {
          type: 'category',
          data: []
        },
        series: [{
          name: '出现频次',
          type: 'bar',
          data: []
        }]
      },
      keywordTrendOptions: {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: []
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: []
        },
        yAxis: {
          type: 'value'
        },
        series: []
      },
      qualityOptions: {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          right: '5%',
          top: 'middle',
          itemWidth: 10,
          itemHeight: 10,
          textStyle: {
            fontSize: 12
          }
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            position: 'outside',
            formatter: '{b}: {c} ({d}%)'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '14',
              fontWeight: 'bold'
            }
          },
          data: []
        }]
      },
      qualityTrendOptions: {
        tooltip: { trigger: 'axis' },
        legend: { data: [] },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }
    }
  },
  created() {
    console.log('Dashboard created');
    // 初始化 echarts
    echarts.init(document.createElement('div'));
    this.loadProductList();
    this.loadDashboardData();
    this.loadQualityTrend();
  },
  mounted() {
    console.log('Dashboard mounted');
  },
  beforeDestroy() {
    console.log('Dashboard beforeDestroy');
  },
  watch: {
    selectedProduct() {
      this.loadKeywordData();
      this.loadKeywordTrend();
      this.loadQualityData();
      this.loadQualityTrend();
    },
    qualityTrendRange() {
      this.loadQualityTrend();
    }
  },
  methods: {
    async loadProductList() {
      try {
        console.log('开始加载商品列表');
        const response = await axios.get('/products/list');
        console.log('商品列表响应:', response.data);
        if (response.data.code === '0') {
          this.productList = response.data.data;
          console.log('商品列表加载成功:', this.productList);
        } else {
          console.error('商品列表加载失败:', response.data);
        }
      } catch (error) {
        console.error('加载商品列表失败:', error);
        this.$message.error('加载商品列表失败: ' + error.message);
      }
    },
    async loadDashboardData() {
      this.loading = true;
      try {
        console.log('开始加载仪表盘数据');
        
        const response = await axios.get('/admin/recent-comments');
        console.log('最新评论数据:', response.data);
        if (response.data.code === '0') {
          this.recentComments = response.data.data;
        }

        await this.loadKeywordData();
        await this.loadKeywordTrend();
        await this.loadQualityData();
      } catch (error) {
        console.error('加载仪表盘数据失败:', error);
        this.$message.error('加载仪表盘数据失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    async loadKeywordData() {
      try {
        console.log('开始加载关键词分析数据');
        const response = await axios.get('/admin/keyword-analysis', {
          params: { productId: this.selectedProduct }
        });
        console.log('关键词分析数据:', response.data);
        if (response.data.code === '0') {
          const { keywords, frequencies } = response.data.data;
          this.keywordOptions.yAxis.data = keywords;
          this.keywordOptions.series[0].data = frequencies;
        }
      } catch (error) {
        console.error('加载关键词分析失败:', error);
        this.$message.error('加载关键词分析失败: ' + error.message);
      }
    },
    async loadKeywordTrend() {
      try {
        console.log('开始加载关键词趋势数据，时间范围:', this.trendTimeRange);
        const response = await axios.get('/admin/keyword-trend', {
          params: { 
            timeRange: this.trendTimeRange,
            productId: this.selectedProduct
          }
        });
        console.log('关键词趋势数据:', response.data);
        if (response.data.code === '0') {
          const { dates, keywords, trends } = response.data.data;
          this.keywordTrendOptions.xAxis.data = dates;
          this.keywordTrendOptions.legend.data = keywords;
          this.keywordTrendOptions.series = keywords.map(keyword => ({
            name: keyword,
            type: 'line',
            smooth: true,
            data: trends[keyword]
          }));
        }
      } catch (error) {
        console.error('加载关键词趋势失败:', error);
        this.$message.error('加载关键词趋势失败: ' + error.message);
      }
    },
    async loadQualityData() {
      try {
        console.log('开始加载质量分析数据');
        const response = await axios.get('/admin/quality-analysis', {
          params: { 
            metric: this.qualityMetric,
            productId: this.selectedProduct
          }
        });
        console.log('质量分析数据:', response.data);
        if (response.data.code === '0') {
          this.qualityOptions.series[0].data = response.data.data;
        }
      } catch (error) {
        console.error('加载质量分析失败:', error);
        this.$message.error('加载质量分析失败: ' + error.message);
      }
    },
    async loadQualityTrend() {
      try {
        console.log('开始加载评论质量趋势数据，时间范围:', this.qualityTrendRange);
        const response = await axios.get('/admin/quality-trend', {
          params: {
            timeRange: this.qualityTrendRange,
            productId: this.selectedProduct
          }
        });
        console.log('评论质量趋势数据:', response.data);
        if (response.data.code === '0') {
          const { dates, metrics, trends } = response.data.data;
          this.qualityTrendOptions.xAxis.data = dates;
          this.qualityTrendOptions.legend.data = metrics;
          this.qualityTrendOptions.series = metrics.map(metric => ({
            name: metric,
            type: 'line',
            smooth: true,
            data: trends[metric]
          }));
        }
      } catch (error) {
        console.error('加载评论质量趋势失败:', error);
        this.$message.error('加载评论质量趋势失败: ' + error.message);
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
  height: 100%;
}

.data-overview {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.chart-row {
  margin-bottom: 20px;
}

.recent-comments {
  margin-bottom: 20px;
}

.recent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.el-tag {
  margin-left: 8px;
}

.el-card {
  height: 100%;
  margin-bottom: 20px;
}

.el-card__body {
  height: calc(100% - 55px);
  padding: 20px;
}
</style> 
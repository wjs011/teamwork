<template>
  <div class="comment-compare">
    <div class="header-area">
      <div class="back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i> 返回
      </div>
      <h2 class="page-title">
        <i class="el-icon-data-analysis"></i>
        评论对比分析
      </h2>
    </div>

    <!-- 商品选择区域 -->
    <el-card class="select-card" shadow="hover">
      <div slot="header" class="card-header">
        <span><i class="el-icon-goods"></i> 选择对比商品</span>
        <el-tooltip content="选择需要对比的商品，最多支持3个商品同时对比" placement="top">
          <i class="el-icon-question"></i>
        </el-tooltip>
      </div>
      <div class="product-select">
        <el-select
          v-model="selectedProducts"
          multiple
          filterable
          placeholder="请选择商品"
          :multiple-limit="3"
          @change="handleProductChange">
          <el-option
            v-for="item in productList"
            :key="item.id"
            :label="item.name"
            :value="item.id">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>{{ item.name }}</span>
              <el-tag size="small" type="info" style="margin-left: 8px;">ID: {{ item.id }}</el-tag>
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" @click="startCompare" :disabled="selectedProducts.length < 2">
          <i class="el-icon-refresh"></i> 开始对比
        </el-button>
      </div>
    </el-card>

    <!-- 对比结果区域 -->
    <div v-if="showCompare" class="compare-content">
      <!-- 基础数据对比 -->
      <el-card class="compare-card" shadow="hover">
        <div slot="header" class="card-header">
          <span><i class="el-icon-s-data"></i> 基础数据对比</span>
          <el-tooltip content="展示各商品的评论总数、平均评分等基础数据。鼠标悬浮在商品ID上可查看商品名称" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
        <div class="basic-stats">
          <el-table :data="basicStats" style="width: 100%" border stripe>
            <el-table-column prop="name" label="指标" width="120"></el-table-column>
            <el-table-column v-for="product in compareData" :key="product.id" :label="product.id">
              <template slot="header">
                <el-tooltip :content="product.name" placement="top">
                  <span>商品{{ product.id }}</span>
                </el-tooltip>
              </template>
              <template slot-scope="scope">
                <span :style="{ color: getValueColor(scope.row[product.id]) }">
                  {{ formatValue(scope.row[product.id]) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>

      <!-- 情感分布对比 -->
      <el-card class="compare-card" shadow="hover">
        <div slot="header" class="card-header">
          <span><i class="el-icon-pie-chart"></i> 情感分布对比</span>
          <el-tooltip content="展示各商品评论的情感分布情况" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
        <div class="sentiment-chart" ref="sentimentChart"></div>
      </el-card>

      <!-- 关键词对比 -->
      <el-card class="compare-card" shadow="hover">
        <div slot="header" class="card-header">
          <span><i class="el-icon-collection-tag"></i> 关键词对比</span>
          <el-tooltip content="展示各商品评论中出现频率最高的关键词" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
        <div class="keyword-chart" ref="keywordChart"></div>
      </el-card>

      <!-- 竞品分析报告 -->
      <el-card class="compare-card" shadow="hover">
        <div slot="header" class="card-header">
          <span><i class="el-icon-document"></i> 竞品分析报告</span>
          <div class="header-actions">
            <el-tooltip content="基于评论数据的竞品分析报告" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
            <el-button 
              type="primary" 
              size="small" 
              :loading="aiAnalyzing"
              @click="showAIAnalysis"
              class="ai-button">
              <i class="el-icon-magic-stick"></i>
              AI智能分析
            </el-button>
          </div>
        </div>
        <div class="analysis-report">
          <div v-for="(section, index) in analysisReport" :key="index" class="report-section">
            <h3>
              <i :class="getReportIcon(index)"></i>
              {{ section.title }}
            </h3>
            <p>{{ section.content }}</p>
          </div>
        </div>
      </el-card>

      <!-- AI分析对话框 -->
      <el-dialog
        title="AI智能分析报告"
        :visible.sync="aiDialogVisible"
        width="70%"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        class="ai-dialog">
        <div v-if="aiAnalyzing" class="ai-loading">
          <i class="el-icon-loading"></i>
          <span>AI正在分析中，请稍候...</span>
        </div>
        <div v-else class="ai-analysis-content">
          <div class="report-header">
            <h2>竞品分析报告</h2>
            <div class="report-time">{{ new Date().toLocaleString() }}</div>
          </div>
          
          <div class="report-body" v-html="formatAIReport()"></div>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="aiDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="exportToPDF" :disabled="aiAnalyzing">
            <i class="el-icon-download"></i> 导出PDF
          </el-button>
        </span>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import * as echarts from 'echarts'
import html2pdf from 'html2pdf.js'

export default {
  name: 'CommentCompare',
  data() {
    return {
      productList: [],
      selectedProducts: [],
      showCompare: false,
      compareData: [],
      basicStats: [],
      analysisReport: [],
      sentimentChart: null,
      keywordChart: null,
      aiAnalyzing: false,
      aiDialogVisible: false,
      aiAnalysisReport: []
    }
  },
  created() {
    this.loadProductList()
  },
  mounted() {
    this.initCharts()
  },
  beforeDestroy() {
    if (this.sentimentChart) {
      this.sentimentChart.dispose()
    }
    if (this.keywordChart) {
      this.keywordChart.dispose()
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1)
    },
    async loadProductList() {
      try {
        const response = await axios.get('/comments/products')
        console.log('商品列表数据:', response.data.data)
        this.productList = response.data.data
      } catch (error) {
        this.$message.error('加载商品列表失败')
      }
    },
    async handleProductChange() {
      this.showCompare = false
    },
    async startCompare() {
      try {
        console.log('开始对比，选中的商品ID:', this.selectedProducts);
        const response = await axios.post('/comments/compare', {
          productIds: this.selectedProducts
        });
        console.log('对比数据响应:', response.data);
        
        if (response.data.code === '0') {
          this.compareData = response.data.data;
          console.log('处理后的对比数据:', this.compareData);
          this.processCompareData();
          this.showCompare = true;
          this.$nextTick(() => {
            this.updateCharts();
          });
        } else {
          this.$message.error(response.data.msg || '获取对比数据失败');
        }
      } catch (error) {
        console.error('对比请求错误:', error);
        this.$message.error('获取对比数据失败: ' + (error.response?.data?.msg || error.message));
      }
    },
    processCompareData() {
      console.log('开始处理对比数据');
      // 处理基础统计数据
      this.basicStats = [
        {
          name: '评论总数',
          ...this.compareData.reduce((acc, product) => {
            acc[product.id] = product.totalComments;
            return acc;
          }, {})
        },
        {
          name: '平均评分',
          ...this.compareData.reduce((acc, product) => {
            acc[product.id] = product.averageScore;
            return acc;
          }, {})
        },
        {
          name: '正面评价率',
          ...this.compareData.reduce((acc, product) => {
            acc[product.id] = product.positiveRate;
            return acc;
          }, {})
        }
      ];
      console.log('基础统计数据:', this.basicStats);

      // 生成分析报告
      this.generateAnalysisReport();
    },
    generateAnalysisReport() {
      // 基于对比数据生成分析报告
      this.analysisReport = [
        {
          title: '总体评价对比',
          content: this.generateOverallComparison()
        },
        {
          title: '优势分析',
          content: this.generateAdvantageAnalysis()
        },
        {
          title: '改进建议',
          content: this.generateImprovementSuggestions()
        }
      ]
    },
    generateOverallComparison() {
      // 生成总体评价对比内容
      const bestProduct = this.compareData.reduce((a, b) => 
        a.averageScore > b.averageScore ? a : b
      )
      return `商品${bestProduct.id}的总体评价最好，平均评分达到${bestProduct.averageScore.toFixed(1)}分。`
    },
    generateAdvantageAnalysis() {
      // 生成优势分析内容
      return this.compareData.map(() => 
        `商品${this.getProductAdvantage()}方面表现突出。`
      ).join(' ')
    },
    generateImprovementSuggestions() {
      // 生成改进建议内容
      return this.compareData.map(() => 
        `商品${this.getProductImprovement()}方面进行改进。`
      ).join(' ')
    },
    getProductAdvantage() {
      // 获取商品优势
      return '用户满意度'
    },
    getProductImprovement() {
      // 获取改进建议
      return '服务响应速度'
    },
    initCharts() {
      // 确保DOM元素已经渲染
      this.$nextTick(() => {
        if (this.$refs.sentimentChart) {
          this.sentimentChart = echarts.init(this.$refs.sentimentChart)
        }
        if (this.$refs.keywordChart) {
          this.keywordChart = echarts.init(this.$refs.keywordChart)
        }
      })
    },
    updateCharts() {
      if (!this.compareData || this.compareData.length === 0) {
        return
      }

      // 确保图表实例存在
      this.$nextTick(() => {
        if (!this.sentimentChart && this.$refs.sentimentChart) {
          this.sentimentChart = echarts.init(this.$refs.sentimentChart)
        }
        if (!this.keywordChart && this.$refs.keywordChart) {
          this.keywordChart = echarts.init(this.$refs.keywordChart)
        }

        // 更新情感分布图表
        if (this.sentimentChart) {
          const sentimentOption = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              },
              formatter: (params) => {
                const productId = params[0].name;
                const product = this.compareData.find(p => p.id === productId);
                const title = product ? `商品${productId} (${product.name})` : `商品${productId}`;
                let result = title + '<br/>';
                params.forEach(param => {
                  result += `${param.seriesName}: ${param.value.toFixed(1)}%<br/>`;
                });
                return result;
              }
            },
            legend: {
              data: ['正面评价', '中性评价', '负面评价'],
              textStyle: {
                color: '#606266'
              },
              top: 0
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '40px',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: this.compareData.map(p => p.id),
              axisLabel: {
                color: '#606266'
              }
            },
            yAxis: {
              type: 'value',
              axisLabel: {
                formatter: '{value}%',
                color: '#606266'
              }
            },
            series: [
              {
                name: '正面评价',
                type: 'bar',
                stack: 'total',
                barWidth: '40%',
                data: this.compareData.map(p => p.positiveRate),
                itemStyle: {
                  color: '#95D475'
                }
              },
              {
                name: '中性评价',
                type: 'bar',
                stack: 'total',
                barWidth: '40%',
                data: this.compareData.map(p => 100 - p.positiveRate - p.negativeRate),
                itemStyle: {
                  color: '#B0BEC5'
                }
              },
              {
                name: '负面评价',
                type: 'bar',
                stack: 'total',
                barWidth: '40%',
                data: this.compareData.map(p => p.negativeRate),
                itemStyle: {
                  color: '#FF9E80'
                }
              }
            ]
          }
          this.sentimentChart.setOption(sentimentOption)
        }

        // 更新关键词图表
        if (this.keywordChart) {
          const keywordOption = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              },
              formatter: (params) => {
                const productId = params[0].seriesName;
                const product = this.compareData.find(p => p.id === productId);
                const title = product ? `商品${productId} (${product.name})` : `商品${productId}`;
                let result = title + '<br/>';
                params.forEach(param => {
                  result += `${param.name}: ${param.value.toFixed(1)}%<br/>`;
                });
                return result;
              }
            },
            legend: {
              data: this.compareData.map(p => p.name),
              textStyle: {
                color: '#606266'
              },
              top: 0
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '40px',
              containLabel: true
            },
            xAxis: {
              type: 'value',
              axisLabel: {
                formatter: '{value}%',
                color: '#606266'
              }
            },
            yAxis: {
              type: 'category',
              data: Object.keys(this.compareData[0].keywords || {}),
              axisLabel: {
                color: '#606266'
              }
            },
            series: this.compareData.map((product, index) => ({
              name: product.name,
              type: 'bar',
              barWidth: '40%',
              data: Object.keys(product.keywords || {}).map(key => product.keywords[key]),
              itemStyle: {
                color: ['#64B5F6', '#81C784', '#FFB74D'][index % 3]
              }
            }))
          }
          this.keywordChart.setOption(keywordOption)
        }
      })
    },
    getValueColor(value) {
      if (typeof value === 'number') {
        if (value >= 4) return '#67C23A'
        if (value >= 3) return '#E6A23C'
        return '#F56C6C'
      }
      return '#303133'
    },
    formatValue(value) {
      if (typeof value === 'number') {
        return value.toFixed(1)
      }
      return value
    },
    getReportIcon(index) {
      const icons = ['el-icon-s-marketing', 'el-icon-star-on', 'el-icon-warning-outline']
      return icons[index] || 'el-icon-document'
    },
    async showAIAnalysis() {
      if (!this.compareData || this.compareData.length === 0) {
        this.$message.warning('请先选择商品进行对比')
        return
      }

      this.aiDialogVisible = true
      this.aiAnalyzing = true
      try {
        // 准备分析数据
        const analysisData = {
          products: this.compareData.map(product => ({
            id: product.id,
            name: product.name,
            totalComments: product.totalComments,
            averageScore: product.averageScore,
            positiveRate: product.positiveRate,
            negativeRate: product.negativeRate,
            keywords: product.keywords
          }))
        }

        // 调用后端AI分析接口
        const response = await axios.post('/comments/ai-analysis', analysisData)
        
        if (response.data.code === '0') {
          console.log('AI分析返回数据:', response.data.data)
          console.log('AI分析原始内容:', response.data.data[0].content)
          this.aiAnalysisReport = response.data.data
          this.$message.success('AI分析完成')
        } else {
          throw new Error(response.data.msg || 'AI分析失败')
        }
      } catch (error) {
        console.error('AI分析错误:', error)
        this.$message.error('AI分析失败: ' + (error.response?.data?.msg || error.message))
      } finally {
        this.aiAnalyzing = false
      }
    },
    formatAIReport() {
      if (!this.aiAnalysisReport || this.aiAnalysisReport.length === 0) {
        return '<div class="empty-report">暂无分析报告</div>'
      }

      // 获取完整的分析内容
      const content = this.aiAnalysisReport[0].content

      // 将内容转换为HTML格式
      let formattedContent = content
        // 先处理标题，确保不会互相影响
        .replace(/^###\s+(.*?)(?:\n|$)/gm, '<h1>$1</h1>')
        .replace(/^####\s+(.*?)(?:\n|$)/gm, '<h2>$1</h2>')
        .replace(/^#####\s+(.*?)(?:\n|$)/gm, '<h3>$1</h3>')
        // 处理换行
        .replace(/\n/g, '<br>')
        // 处理加粗
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        // 处理段落
        .replace(/([^<br>]+)(<br>|$)/g, '<p>$1</p>')
        // 处理列表项
        .replace(/<p>- (.*?)：<\/p>/g, '<li class="list-title">$1：</li>')
        .replace(/<p>- (.*?)<\/p>/g, '<li>$1</li>')
        // 将连续的列表项包装在ul中
        .replace(/(<li.*?<\/li>)+/g, match => `<ul>${match}</ul>`)
        // 移除多余的空段落
        .replace(/<p><\/p>/g, '')
        // 移除段落中的br标签
        .replace(/<p>(.*?)<br>(.*?)<\/p>/g, '<p>$1$2</p>')
        // 处理连续的空格
        .replace(/\s+/g, ' ')
        // 处理连续的空行
        .replace(/(<br>){2,}/g, '<br>')
        // 移除末尾的#
        .replace(/#$/, '')
        // 处理冒号后的换行
        .replace(/：<br>/g, '：')
        // 处理水平分割线
        .replace(/---/g, '<hr>')
        // 处理列表项中的冒号
        .replace(/<li>(.*?)：<\/li>/g, '<li class="list-title">$1：</li>')
        // 清理多余的标题标记
        .replace(/<br>####/g, '<br>')
        .replace(/<br>###/g, '<br>')
        .replace(/<br>#####/g, '<br>')
        // 处理冒号后的内容
        .replace(/：<br><br>/g, '：<br>')
        .replace(/：<br><p>/g, '：<br>')
        .replace(/：<br><ul>/g, '：<br>')

      return `
        <div class="report-section">
          <div class="section-content">${formattedContent}</div>
        </div>
      `
    },
    async exportToPDF() {
      if (!this.aiAnalysisReport || this.aiAnalysisReport.length === 0) {
        this.$message.warning('请先生成分析报告')
        return
      }

      // 创建临时容器
      const element = document.createElement('div')
      element.innerHTML = `
        <div class="pdf-container">
          <div class="pdf-header">
            <h1>竞品分析报告</h1>
            <div class="pdf-time">${new Date().toLocaleString()}</div>
          </div>
          <div class="pdf-content">
            ${this.formatAIReport()}
          </div>
        </div>
      `

      // 配置 PDF 选项
      const opt = {
        margin: 1,
        filename: `竞品分析报告_${new Date().toLocaleDateString()}.pdf`,
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2 },
        jsPDF: { unit: 'in', format: 'a4', orientation: 'portrait' }
      }

      try {
        this.$message.info('正在生成PDF，请稍候...')
        await html2pdf().set(opt).from(element).save()
        this.$message.success('PDF导出成功')
      } catch (error) {
        console.error('PDF导出错误:', error)
        this.$message.error('PDF导出失败')
      }
    }
  }
}
</script>

<style scoped>
.comment-compare {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.back-button {
  cursor: pointer;
  color: #409EFF;
  font-size: 14px;
  margin-right: 20px;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  padding: 10px;
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
  display: flex;
  align-items: center;
}

.page-title i {
  margin-right: 10px;
  color: #409EFF;
}

.select-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.select-card:hover {
  transform: translateY(-2px);
}

.product-select {
  display: flex;
  gap: 15px;
  align-items: center;
}

.compare-content {
  margin-top: 20px;
}

.compare-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.compare-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 5px;
}

.card-header i {
  margin-right: 5px;
  color: #409EFF;
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

.sentiment-chart,
.keyword-chart {
  height: 400px;
  background-color: white;
  border-radius: 4px;
  padding: 20px;
}

.analysis-report {
  padding: 20px;
}

.report-section {
  margin-bottom: 2em;
  padding: 1em;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.report-section h3 {
  color: #303133;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  font-size: 18px;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 10px;
}

.report-section h3 i {
  margin-right: 10px;
  color: #409EFF;
}

.section-content {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
  word-wrap: break-word;
  padding: 1em;
}

.section-content h1 {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 1.5em 0;
  padding-bottom: 0.5em;
  border-bottom: 3px solid #409EFF;
  text-align: center;
}

.section-content h2 {
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  margin: 1.5em 0 1em;
  padding-bottom: 0.5em;
  border-bottom: 2px solid #409EFF;
}

.section-content p {
  margin: 0 0 1em 0;
  text-align: justify;
  white-space: normal;
}

.section-content ul {
  list-style: none;
  padding-left: 20px;
  margin: 10px 0;
}

.section-content li {
  position: relative;
  padding-left: 15px;
  margin-bottom: 8px;
  white-space: normal;
}

.section-content li.list-title {
  font-weight: 600;
  color: #303133;
  margin-top: 1em;
}

.section-content li:before {
  content: "•";
  color: #409EFF;
  position: absolute;
  left: 0;
}

.section-content li.list-title:before {
  content: "";
}

.section-content br {
  display: block;
  content: "";
  margin: 0.5em 0;
}

.section-content strong {
  color: #303133;
  font-weight: 600;
}

.section-content hr {
  border: none;
  border-top: 1px solid #EBEEF5;
  margin: 2em 0;
}

.empty-report {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-button {
  margin-left: 10px;
}

.ai-button i {
  margin-right: 5px;
}

.ai-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.ai-loading i {
  font-size: 24px;
  margin-bottom: 10px;
}

.ai-loading span {
  font-size: 14px;
}

.ai-dialog {
  .el-dialog__body {
    padding: 20px;
  }
}

.ai-analysis-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.report-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #EBEEF5;
}

.report-header h2 {
  color: #303133;
  margin: 0 0 10px 0;
}

.report-time {
  color: #909399;
  font-size: 14px;
}

.analysis-summary {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 4px solid #409EFF;
}

.analysis-summary p {
  color: #606266;
  line-height: 1.8;
  margin: 0;
}

.advantage-analysis,
.improvement-suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.product-advantage,
.product-improvement {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 4px solid #67C23A;
}

.product-improvement {
  border-left-color: #E6A23C;
}

.product-advantage h4,
.product-improvement h4 {
  color: #303133;
  margin: 0 0 10px 0;
  font-size: 16px;
}

.advantage-content,
.improvement-content {
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.report-body {
  padding: 20px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.pdf-container {
  padding: 20px;
  background-color: white;
}

.pdf-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #409EFF;
}

.pdf-header h1 {
  color: #303133;
  margin: 0 0 10px 0;
  font-size: 24px;
}

.pdf-time {
  color: #909399;
  font-size: 14px;
}

.pdf-content {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

.pdf-content h1 {
  color: #303133;
  font-size: 20px;
  margin: 1.5em 0 1em;
  padding-bottom: 0.5em;
  border-bottom: 2px solid #409EFF;
}

.pdf-content h2 {
  color: #303133;
  font-size: 16px;
  margin: 1em 0 0.8em;
}

.pdf-content p {
  margin: 0.8em 0;
  text-align: justify;
}

.pdf-content ul {
  margin: 0.8em 0;
  padding-left: 20px;
}

.pdf-content li {
  margin: 0.5em 0;
}

.pdf-content strong {
  color: #303133;
  font-weight: 600;
}

.pdf-content hr {
  margin: 1.5em 0;
  border: none;
  border-top: 1px solid #EBEEF5;
}
</style> 
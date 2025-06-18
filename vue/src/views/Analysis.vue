<template>
  <div class="analysis-container">
    <div class="header-area">
      <div class="back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i> {{ $t('analysis.back') }}
      </div>
      <h2 class="product-title">{{ $t('analysis.title') }}</h2>
      <div class="action-buttons">
        <div class="sentiment-button" @click="analyzeSentiment" v-if="!loading && !isAnalyzing">
          <i class="el-icon-data-analysis"></i> {{ $t('analysis.sentimentAnalysis') }}
        </div>
        <div class="refresh-button" @click="fetchNewComments" v-if="!loading">
          <i class="el-icon-refresh"></i> {{ $t('analysis.refreshComments') }}
        </div>
        <div class="loading-indicator" v-else>
          <i class="el-icon-loading"></i> {{ $t('analysis.loading') }}
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="main-content">
      <!-- 左侧区域 - 评论列表和筛选 -->
      <el-col :span="14">
        <el-card class="comment-card">
          <div slot="header" class="comment-header">
            <span>{{ $t('analysis.commentList') }}</span>
            <div class="comment-filter">
              <el-button 
                style="margin-right: 10px" 
                type="primary" 
                size="small"
                @click="goToCommentSummary">
                评论摘要
              </el-button>
              <el-button 
                style="margin-right: 10px" 
                type="primary" 
                size="small"
                @click="goToCommentCategory">
                评论分类
              </el-button>
              <el-button 
                style="margin-right: 10px" 
                type="primary" 
                size="small"
                @click="goToCommentCompare">
                评论对比
              </el-button>
              <el-select v-model="scoreFilter" :placeholder="$t('analysis.filterByScore')" size="small" @change="filterComments">
                <el-option :label="$t('analysis.allScores')" value="0"></el-option>
                <el-option :label="$t('analysis.star1')" value="1"></el-option>
                <el-option :label="$t('analysis.star2')" value="2"></el-option>
                <el-option :label="$t('analysis.star3')" value="3"></el-option>
                <el-option :label="$t('analysis.star4')" value="4"></el-option>
                <el-option :label="$t('analysis.star5')" value="5"></el-option>
              </el-select>
              <el-input
                  :placeholder="$t('analysis.searchPlaceholder')"
                  v-model="searchKeyword"
                  class="search-input"
                  size="small"
                  clearable
                  @keyup.enter.native="searchComments"
                  prefix-icon="el-icon-search"
              >
                <el-button slot="append" icon="el-icon-search" @click="searchComments"></el-button>
              </el-input>
            </div>
          </div>

          <!-- 情感分析颜色说明 -->
          <div v-if="showSentimentLegend" class="sentiment-legend">
            <div class="legend-item">
              <div class="color-block positive"></div>
              <span>{{ $t('analysis.sentimentLegend.positive') }}</span>
            </div>
            <div class="legend-item">
              <div class="color-block neutral"></div>
              <span>{{ $t('analysis.sentimentLegend.neutral') }}</span>
            </div>
            <div class="legend-item">
              <div class="color-block negative"></div>
              <span>{{ $t('analysis.sentimentLegend.negative') }}</span>
            </div>
          </div>

          <div v-if="comments.length === 0" class="empty-comments">
            <i class="el-icon-chat-dot-square empty-icon"></i>
            <p>{{ $t('analysis.emptyComments') }}</p>
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" 
                 class="comment-item"
                 :class="{'has-sentiment': showSentimentLegend && comment.sentimentScore !== null}"
                 :style="showSentimentLegend && comment.sentimentScore !== null ? getSentimentStyle(comment.sentimentScore) : { backgroundColor: '#ffffff' }">
              <div class="comment-user">
                <i class="el-icon-user"></i>
                <span class="username">{{ comment.nickname || $t('analysis.anonymousUser') }}</span>
              </div>
              <div class="score-display">
                <i v-for="n in 5" :key="n"
                   :class="n <= comment.score ? 'el-icon-star-on' : 'el-icon-star-off'"
                   :style="{ color: n <= comment.score ? '#f7ba2a' : '#dcdfe6' }">
                </i>
                <span class="score-text">{{ comment.score }}{{ $t('analysis.score') }}</span>
                <span v-if="showSentimentLegend && comment.sentimentScore !== null" class="sentiment-score">
                  {{ $t('analysis.sentimentScore') }}: {{ comment.sentimentScore.toFixed(2) }}
                </span>
              </div>
              <div class="comment-content">
                <template v-if="currentLang === 'en' && comment.translatedContent">
                  <template v-if="!comment.showOriginal">
                    {{ comment.translatedContent }}
                    <div class="original-content" @click="toggleOriginalContent(comment)">
                      {{ $t('analysis.showOriginal') }}
                    </div>
                  </template>
                  <template v-else>
                    {{ comment.content }}
                    <div class="original-content" @click="toggleOriginalContent(comment)">
                      {{ $t('analysis.showTranslation') }}
                    </div>
                  </template>
                </template>
                <template v-else>
                  {{ comment.content }}
                  <div v-if="currentLang === 'en'" class="translate-button" @click="translateComment(comment)">
                    {{ $t('analysis.translate') }}
                  </div>
                </template>
              </div>
              <div class="comment-time" v-if="comment.createTime">
                {{ new Date(comment.createTime).toLocaleString() }}
              </div>
            </div>
          </div>

          <div class="pagination-container" v-if="comments.length > 0">
            <el-pagination
                @current-change="handleCurrentChange"
                :current-page="currentPage"
                :page-size="pageSize"
                layout="prev, pager, next"
                :total="total">
            </el-pagination>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧区域 - 评论分析 -->
      <el-col :span="10">
        <!-- 评分统计卡片 -->
        <el-card class="analysis-card score-card">
          <div slot="header" class="card-header">
            <span>{{ $t('analysis.scoreDistribution') }}</span>
            <el-tooltip :content="$t('analysis.scoreDistributionTip')" placement="top">
              <i class="el-icon-info"></i>
            </el-tooltip>
          </div>
          <div class="score-summary">
            <div class="average-score" :style="{ color: getScoreColor(averageScore) }">
              {{ averageScore.toFixed(1) }}
              <span class="score-label">{{ $t('analysis.averageScore') }}</span>
            </div>
            <div class="score-bars">
              <div v-for="i in 5" :key="i" class="score-bar-item">
                <span class="score-label">{{ $t('analysis.star' + i) }}</span>
                <div class="score-bar-container">
                  <div class="score-bar"
                       :style="{ width: scoreDistribution[i] + '%', backgroundColor: getScoreColor(i) }">
                  </div>
                </div>
                <span class="score-percentage">{{ scoreDistribution[i] }}%</span>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 词云卡片 -->
        <el-card class="analysis-card word-cloud-card">
          <div slot="header" class="card-header">
            <span>{{ $t('analysis.keywordCloud') }}</span>
            <el-tooltip :content="$t('analysis.keywordCloudTip')" placement="top">
              <i class="el-icon-info"></i>
            </el-tooltip>
          </div>
          <div class="word-cloud-container" ref="wordCloudContainer">
            <div v-if="keywords.length === 0" class="empty-keywords">
              <i class="el-icon-data-analysis empty-icon"></i>
              <p>{{ $t('analysis.emptyKeywords') }}</p>
            </div>
            <div v-else class="word-cloud" ref="wordCloud"></div>
          </div>
          <div class="word-cloud-footer" v-if="keywords.length > 0">
            <span>{{ $t('analysis.totalKeywords', { count: keywords.length }) }}</span>
            <el-button type="text" size="mini" @click="exportKeywords">{{ $t('analysis.exportKeywords') }}</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Analysis',
  data() {
    return {
      productId: null,
      productUrl: '',
      comments: [],
      allcomments: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      scoreFilter: '0',
      searchKeyword: '',
      keywords: [],
      currentLang: 'zh',

      // 评分统计
      averageScore: 0,
      scoreDistribution: {
        1: 0,
        2: 0,
        3: 0,
        4: 0,
        5: 0
      },
      isAnalyzing: false,
      showSentimentLegend: false,
    };
  },
  created() {
    // 从路由参数获取商品ID
    this.productId = this.$route.params.productId;
    this.productUrl = this.$route.query.url || '';
    // 获取当前语言设置
    this.currentLang = localStorage.getItem('language') || 'zh';

    if (this.productId) {
      this.getComments();
      this.getscore();
    }
  },
  watch: {
    // 监听语言变化
    '$i18n.locale': {
      handler(newLang) {
        this.currentLang = newLang;
        // 如果切换到英文，自动翻译所有评论
        if (newLang === 'en') {
          this.translateAllComments();
        }
      },
      immediate: true
    }
  },
  mounted() {
    window.addEventListener('resize', this.resizeWordCloud);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeWordCloud);
  },
  methods: {
    goBack() {
      this.$router.push('/');
    },

    async fetchNewComments() {
      if (!this.productUrl) {
        this.$message.warning(this.$t('analysis.noCommentsWarning'));
        return;
      }

      this.loading = true;

      try {
        const response = await axios.get('/comments/fetch', {
          params: { url: this.productUrl }
        });

        if (response.data.code === '0') {
          this.$message.success(this.$t('analysis.fetchCommentsSuccess'));
          this.getComments();
          this.getscore();
        } else {
          this.$message.error(response.data.msg || this.$t('analysis.fetchCommentsFailed'));
        }
      } catch (error) {
        console.error('获取评论失败:', error);
        this.$message.error(this.$t('analysis.fetchCommentsFailed') + ': ' + (error.response?.data?.msg || error.message));
      } finally {
        this.loading = false;
      }
    },

    async getscore() {
      this.loading = true;

      try {
        const response = await axios.get('/comments', {
          params: {
            pageNum: 1,
            pageSize: 20000,
            productId: this.productId
          }
        });

        if (response.data.code === '0') {
          const data = response.data.data;
          this.allcomments = data.records || [];

          if (this.allcomments.length === 0) {
            this.$message.warning('暂无评论数据，请点击上方按钮获取评论');
            return;
          }

          // 计算评分统计
          this.calculateScoreDistribution();
        } else {
          this.$message.warning(response.data.msg || '未找到评论数据');
        }
      } catch (error) {
        console.error('获取评论列表失败:', error);
        this.$message.error('获取评论列表失败: ' + (error.response?.data?.msg || error.message));
      } finally {
        this.loading = false;
      }
    },

    async getComments() {
      this.loading = true;

      try {
        const response = await axios.get('/comments', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            productId: this.productId
          }
        });

        if (response.data.code === '0') {
          const data = response.data.data;
          // 确保评论数据中包含翻译相关字段
          this.comments = (data.records || []).map(comment => ({
            ...comment,
            sentimentScore: comment.sentimentScore || null,
            translatedContent: null,
            showOriginal: false
          }));
          this.total = data.total || 0;

          if (this.comments.length === 0) {
            this.$message.warning(this.$t('analysis.noCommentsWarning'));
            return;
          }

          // 获取关键词
          this.getKeywords();
          
          // 如果当前是英文，自动翻译所有评论
          if (this.currentLang === 'en') {
            this.translateAllComments();
          }
        } else {
          this.$message.warning(response.data.msg || this.$t('analysis.fetchCommentsFailed'));
        }
      } catch (error) {
        console.error('获取评论列表失败:', error);
        this.$message.error(this.$t('analysis.fetchCommentsFailed') + ': ' + (error.response?.data?.msg || error.message));
      } finally {
        this.loading = false;
      }
    },

    async getKeywords() {
      try {
        const response = await axios.get(`/comments/keywords1/${this.productId}`, {
          params: { topN: 50 }
        });

        if (response.data.code === '0') {
          const keywordsObj = response.data.data.keywords || {};

          // 将对象转换为数组格式，适用于词云
          this.keywords = Object.entries(keywordsObj).map(([text, value]) => ({
            text,
            value
          }));

          // 在下一个渲染周期绘制词云
          this.$nextTick(() => {
            this.renderWordCloud();
          });
        }
      } catch (error) {
        console.error('获取关键词失败:', error);
      }
    },

    handleCurrentChange(page) {
      this.currentPage = page;
      if (this.scoreFilter !== '0') {
        // 如果当前有评分筛选，使用评分筛选接口
        this.filterComments();
      } else {
        // 否则使用普通评论获取接口
        this.getComments();
      }
    },

    async filterComments() {
      if (this.scoreFilter === '0') {
        // 重置为全部评论
        this.currentPage = 1;
        this.getComments();
        return;
      }

      this.loading = true;
      const minScore = parseInt(this.scoreFilter);
      const maxScore = parseInt(this.scoreFilter);

      try {
        const response = await axios.get(`/comments/score/${this.productId}`, {
          params: {
            minScore,
            maxScore,
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        });

        if (response.data.code === '0') {
          // 检查返回数据的结构
          if (response.data.data.records) {
            // 如果返回的是分页格式
            this.comments = response.data.data.records.map(comment => ({
              ...comment,
              sentimentScore: comment.sentimentScore || null
            }));
            this.total = response.data.data.total;
          } else if (Array.isArray(response.data.data)) {
            // 如果返回的是数组格式，手动处理分页
            const start = (this.currentPage - 1) * this.pageSize;
            const end = start + this.pageSize;
            this.comments = response.data.data.slice(start, end).map(comment => ({
              ...comment,
              sentimentScore: comment.sentimentScore || null
            }));
            this.total = response.data.data.length;
          } else {
            this.comments = [];
            this.total = 0;
          }

          // 如果有情感分析数据，显示图例
          if (this.comments.some(comment => comment.sentimentScore !== null)) {
            this.showSentimentLegend = true;
          }
        } else {
          this.$message.warning(response.data.msg || this.$t('analysis.filterCommentsFailed'));
        }
      } catch (error) {
        console.error('筛选评论失败:', error);
        this.$message.error(this.$t('analysis.filterCommentsFailed') + ': ' + (error.response?.data?.msg || error.message));
      } finally {
        this.loading = false;
      }
    },

    async searchComments() {
      if (!this.searchKeyword.trim()) {
        // 如果搜索关键词为空，恢复显示所有评论
        this.currentPage = 1;
        this.getComments();
        return;
      }

      this.loading = true;

      try {
        const response = await axios.get(`/comments/search/${this.productId}`, {
          params: {
            keyword: this.searchKeyword.trim()
          }
        });

        if (response.data.code === '0') {
          this.comments = (response.data.data || []).map(comment => ({
            ...comment,
            sentimentScore: comment.sentimentScore || null
          }));
          this.total = this.comments.length;
          this.currentPage = 1;

          // 如果有情感分析数据，显示图例
          if (this.comments.some(comment => comment.sentimentScore !== null)) {
            this.showSentimentLegend = true;
          }
        } else {
          this.$message.warning(response.data.msg || this.$t('analysis.searchCommentsFailed'));
        }
      } catch (error) {
        console.error('搜索评论失败:', error);
        this.$message.error(this.$t('analysis.searchCommentsFailed') + ': ' + (error.response?.data?.msg || error.message));
      } finally {
        this.loading = false;
      }
    },

    calculateScoreDistribution() {
      if (!this.allcomments || this.allcomments.length === 0) {
        this.averageScore = 0;
        this.scoreDistribution = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
        return;
      }

      // 初始化计数器
      const scoreCounts = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
      let totalScore = 0;

      // 计算每个评分的数量和总分
      this.allcomments.forEach(comment => {
        if (comment.score >= 1 && comment.score <= 5) {
          scoreCounts[comment.score]++;
          totalScore += comment.score;
        }
      });

      // 计算平均分
      this.averageScore = totalScore / this.allcomments.length;

      // 计算每个评分的百分比
      const total = this.allcomments.length;
      for (let i = 1; i <= 5; i++) {
        this.scoreDistribution[i] = Math.round((scoreCounts[i] / total) * 100);
      }
    },

    renderWordCloud() {
      if (!this.keywords || this.keywords.length === 0 || !this.$refs.wordCloud) return;

      const container = this.$refs.wordCloud;
      container.innerHTML = '';

      // 设置字体大小范围
      const maxFontSize = 42;
      const minFontSize = 14;

      // 找出最大和最小权重值
      const values = this.keywords.map(item => item.value);
      const maxValue = Math.max(...values);
      const minValue = Math.min(...values);

      // 定义一组美观的颜色
      const colors = [
        '#3a7bd5', '#00d2ff', '#11998e', '#38ef7d',
        '#a8ff78', '#f857a6', '#ff5858', '#fcb045',
        '#7b4397', '#dc2430', '#4568dc', '#b06ab3'
      ];

      this.keywords.forEach(keyword => {
        const span = document.createElement('span');
        span.className = 'keyword';
        span.textContent = keyword.text;

        // 计算字体大小
        const fontSize = minValue === maxValue
            ? (maxFontSize + minFontSize) / 2
            : minFontSize + (keyword.value - minValue) / (maxValue - minValue) * (maxFontSize - minFontSize);

        // 随机选择颜色
        const color = colors[Math.floor(Math.random() * colors.length)];

        // 设置样式
        span.style.fontSize = `${fontSize}px`;
        span.style.color = color;
        span.style.fontWeight = Math.floor(fontSize / 5) * 100;
        span.style.opacity = 0.9 - (0.3 * (fontSize - minFontSize) / (maxFontSize - minFontSize));
        span.style.transition = 'all 0.3s ease';

        // 鼠标悬停效果
        span.onmouseenter = () => {
          span.style.backgroundColor = '#f5f7fa';
          span.style.opacity = '1';
        };
        span.onmouseleave = () => {
          span.style.backgroundColor = 'transparent';
        };

        container.appendChild(span);
      });
    },

    resizeWordCloud() {
      this.renderWordCloud();
    },

    getScoreColor(score) {
      if (score >= 4.5) return '#67C23A'; // 绿色 - 优秀
      if (score >= 4) return '#85CE61';   // 浅绿色 - 良好
      if (score >= 3) return '#E6A23C';   // 黄色 - 一般
      if (score >= 2) return '#F56C6C';   // 红色 - 较差
      return '#F56C6C';                   // 深红色 - 很差
    },

    exportKeywords() {
      // 将关键词转换为CSV格式
      const csvContent = "data:text/csv;charset=utf-8,"
          + "关键词,频率\n"
          + this.keywords.map(k => `${k.text},${k.value}`).join("\n");

      // 创建下载链接
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement("a");
      link.setAttribute("href", encodedUri);
      link.setAttribute("download", `商品_${this.productId}_关键词.csv`);
      document.body.appendChild(link);

      // 触发下载
      link.click();
      document.body.removeChild(link);

      this.$message.success(this.$t('analysis.exportKeywordsSuccess'));
    },

    async analyzeSentiment() {
      if (this.comments.length === 0) {
        this.$message.warning(this.$t('analysis.noCommentsWarning'));
        return;
      }

      this.isAnalyzing = true;
      this.showSentimentLegend = true;

      try {
        const response = await axios.post(`/comments/sentiment/${this.productId}`, {
          comments: this.comments.map(comment => ({
            id: comment.id,
            content: comment.content
          }))
        });

        if (response.data.code === '0') {
          const sentimentResults = response.data.data;
          // 更新评论的情感分数
          this.comments = this.comments.map(comment => {
            const sentimentScore = sentimentResults[comment.id];
            return {
              ...comment,
              sentimentScore: sentimentScore !== undefined ? sentimentScore : 0.5
            };
          });
          this.$message.success(this.$t('analysis.sentimentAnalysisSuccess'));
        } else {
          this.$message.error(response.data.msg || this.$t('analysis.sentimentAnalysisFailed'));
        }
      } catch (error) {
        console.error('情感分析失败:', error);
        this.$message.error(this.$t('analysis.sentimentAnalysisFailed') + ': ' + (error.response?.data?.msg || error.message));
      } finally {
        this.isAnalyzing = false;
      }
    },

    getSentimentStyle(score) {
      // 根据情感分数返回对应的背景色
      if (score >= 0.6) {
        return { backgroundColor: 'rgba(103, 194, 58, 0.1)' }; // 积极 - 浅绿色
      } else if (score >= 0.4) {
        return { backgroundColor: 'rgba(230, 162, 60, 0.1)' }; // 中性 - 浅黄色
      } else {
        return { backgroundColor: 'rgba(245, 108, 108, 0.1)' }; // 消极 - 浅红色
      }
    },

    async translateComment(comment) {
      if (!comment.content) return;
      
      try {
        const response = await axios.post('/api/translate', {
          text: comment.content,
          targetLang: 'en'
        });

        if (response.data && response.data.code === '0') {
          // 使用 Vue.set 确保响应式更新
          this.$set(comment, 'translatedContent', response.data.data);
          this.$set(comment, 'showOriginal', false);
        } else {
          this.$message.error(response.data.msg || this.$t('analysis.translateFailed'));
        }
      } catch (error) {
        console.error('翻译失败:', error);
        this.$message.error(this.$t('analysis.translateFailed'));
      }
    },

    async translateAllComments() {
      // 创建一个 Promise 数组，每个评论一个翻译任务
      const translatePromises = this.comments.map(async (comment) => {
        if (!comment.translatedContent) {
          try {
            const response = await axios.post('/api/translate', {
              text: comment.content,
              targetLang: 'en'
            });

            if (response.data && response.data.code === '0') {
              // 立即更新当前评论的翻译内容
              this.$set(comment, 'translatedContent', response.data.data);
              this.$set(comment, 'showOriginal', false);
            }
          } catch (error) {
            console.error('翻译失败:', error);
            this.$message.error(this.$t('analysis.translateFailed'));
          }
        }
      });

      // 并行执行所有翻译任务
      await Promise.all(translatePromises);
    },

    toggleOriginalContent(comment) {
      this.$set(comment, 'showOriginal', !comment.showOriginal);
    },

    goToCommentCategory() {
      this.$router.push({
        path: '/comment-category',
        query: {
          productId: this.productId
        }
      })
    },

    goToCommentSummary() {
      this.$router.push({
        path: '/comment-summary',
        query: {
          productId: this.productId
        }
      })
    },

    goToCommentCompare() {
      this.$router.push({
        path: '/comment-compare',
        query: { productId: this.productId }
      })
    }
  }
};
</script>

<style scoped>
.analysis-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
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
}

.back-button:hover {
  color: #66b1ff;
}

.product-title {
  flex: 1;
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

.sentiment-button {
  cursor: pointer;
  color: #409EFF;
  font-size: 14px;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.sentiment-button:hover {
  color: #66b1ff;
}

.refresh-button, .loading-indicator {
  cursor: pointer;
  color: #409EFF;
  font-size: 14px;
  transition: color 0.3s;
}

.refresh-button:hover {
  color: #66b1ff;
}

.loading-indicator i {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.main-content {
  margin-bottom: 20px;
}

.comment-card, .analysis-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.comment-card:hover, .analysis-card:hover {
  box-shadow: 0 2px 16px 0 rgba(0, 0, 0, 0.1);
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.comment-filter {
  display: flex;
  align-items: center;
}

.search-input {
  width: 200px;
  margin-left: 10px;
  transition: width 0.3s;
}

.search-input:focus-within {
  width: 250px;
}

.empty-comments, .empty-keywords {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.6;
}

.comment-list {
  max-height: 600px;
  overflow-y: auto;
  padding: 0 10px;
}

.comment-item {
  position: relative;
  transition: background-color 0.3s;
  background-color: #ffffff;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.comment-item:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.comment-item.has-sentiment {
  border-radius: 4px;
  padding: 15px;
}

.comment-user {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.comment-user i {
  font-size: 16px;
  color: #909399;
}

.username {
  margin-left: 8px;
  font-weight: 500;
  color: #606266;
}

.score-display {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.score-display i {
  margin-right: 2px;
}

.score-text {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}

.comment-content {
  position: relative;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
  word-break: break-word;
}

.translate-button {
  display: inline-block;
  margin-left: 10px;
  color: #409EFF;
  cursor: pointer;
  font-size: 12px;
}

.translate-button:hover {
  color: #66b1ff;
}

.original-content {
  display: inline-block;
  margin-left: 10px;
  color: #409EFF;
  cursor: pointer;
  font-size: 12px;
}

.original-content:hover {
  color: #66b1ff;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
  padding: 10px 0;
}

/* 评分统计卡片 */
.score-card .card-header {
  border-bottom: 1px solid #f0f2f5;
}

.score-summary {
  display: flex;
  padding: 20px 10px;
}

.average-score {
  width: 100px;
  font-size: 36px;
  font-weight: bold;
  text-align: center;
  margin-right: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-label {
  display: block;
  font-size: 14px;
  font-weight: normal;
  color: #909399;
  margin-top: 5px;
}

.score-bars {
  flex: 1;
}

.score-bar-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.score-bar-container {
  flex: 1;
  height: 10px;
  background-color: #ebeef5;
  border-radius: 5px;
  overflow: hidden;
  margin: 0 10px;
}

.score-bar {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s ease;
}

.score-percentage {
  width: 40px;
  text-align: right;
  font-size: 13px;
  color: #606266;
}

/* 词云卡片 */
.word-cloud-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.word-cloud-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header .el-icon-info {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.3s;
}

.card-header .el-icon-info:hover {
  color: #409EFF;
}

.word-cloud-container {
  height: 300px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f9fafc 0%, #f0f2f5 100%);
  border-radius: 8px;
  margin: 10px;
  overflow: hidden;
}

.word-cloud {
  width: 90%;
  height: 90%;
  text-align: center;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  padding: 15px;
  position: relative;
}

.keyword {
  cursor: pointer;
  transition: all 0.3s ease;
  margin: 5px;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.1);
}

.keyword:hover {
  transform: scale(1.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.empty-keywords {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  height: 100%;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.6;
}

.word-cloud-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-top: 1px solid #f0f2f5;
  font-size: 12px;
  color: #909399;
}

.word-cloud-footer .el-button {
  padding: 0;
}

.sentiment-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 10px;
  margin-bottom: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #606266;
}

.color-block {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.color-block.positive {
  background-color: rgba(103, 194, 58, 0.1);
}

.color-block.neutral {
  background-color: rgba(230, 162, 60, 0.1);
}

.color-block.negative {
  background-color: rgba(245, 108, 108, 0.1);
}

.sentiment-score {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
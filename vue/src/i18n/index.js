import Vue from 'vue'
import VueI18n from 'vue-i18n'

Vue.use(VueI18n)

const messages = {
  zh: {
    header: {
      login: '登录',
      logout: '退出系统',
      personInfo: '个人信息'
    },
    analysis: {
      back: '返回',
      title: '商品评论分析',
      sentimentAnalysis: '情感分析',
      refreshComments: '重新获取评论',
      loading: '正在加载...',
      commentList: '评论列表',
      filterByScore: '按评分筛选',
      allScores: '全部评分',
      star1: '1星',
      star2: '2星',
      star3: '3星',
      star4: '4星',
      star5: '5星',
      searchPlaceholder: '搜索评论关键词',
      sentimentLegend: {
        positive: '积极 (0.6-1.0)',
        neutral: '中性 (0.4-0.6)',
        negative: '消极 (0-0.4)'
      },
      emptyComments: '暂无评论数据，点击上方按钮获取评论',
      score: '分',
      sentimentScore: '情感得分',
      scoreDistribution: '评分分布',
      scoreDistributionTip: '显示该商品所有评分的分布情况',
      averageScore: '平均评分',
      keywordCloud: '评论关键词云',
      keywordCloudTip: '词云展示了评论中出现频率最高的关键词，字体越大表示出现频率越高',
      emptyKeywords: '暂无关键词数据',
      totalKeywords: '共 {count} 个关键词',
      exportKeywords: '导出关键词',
      anonymousUser: '匿名用户',
      fetchCommentsSuccess: '评论获取成功',
      fetchCommentsFailed: '评论获取失败',
      noCommentsWarning: '暂无评论数据，请点击上方按钮获取评论',
      filterCommentsFailed: '筛选评论失败',
      searchCommentsFailed: '搜索评论失败',
      sentimentAnalysisSuccess: '情感分析完成',
      sentimentAnalysisFailed: '情感分析失败',
      exportKeywordsSuccess: '关键词导出成功',
      translate: '翻译',
      showOriginal: '显示原文',
      translateFailed: '翻译失败'
    }
  },
  en: {
    header: {
      login: 'Login',
      logout: 'Logout',
      personInfo: 'Profile'
    },
    analysis: {
      back: 'Back',
      title: 'Product Review Analysis',
      sentimentAnalysis: 'Sentiment Analysis',
      refreshComments: 'Refresh Comments',
      loading: 'Loading...',
      commentList: 'Comment List',
      filterByScore: 'Filter by Score',
      allScores: 'All Scores',
      star1: '1 Star',
      star2: '2 Stars',
      star3: '3 Stars',
      star4: '4 Stars',
      star5: '5 Stars',
      searchPlaceholder: 'Search comments',
      sentimentLegend: {
        positive: 'Positive (0.6-1.0)',
        neutral: 'Neutral (0.4-0.6)',
        negative: 'Negative (0-0.4)'
      },
      emptyComments: 'No comments available, click button above to fetch comments',
      score: 'points',
      sentimentScore: 'Sentiment Score',
      scoreDistribution: 'Score Distribution',
      scoreDistributionTip: 'Shows the distribution of all scores for this product',
      averageScore: 'Average Score',
      keywordCloud: 'Comment Keywords',
      keywordCloudTip: 'Word cloud shows the most frequent keywords in comments, larger font size indicates higher frequency',
      emptyKeywords: 'No keyword data available',
      totalKeywords: 'Total {count} keywords',
      exportKeywords: 'Export Keywords',
      anonymousUser: 'Anonymous User',
      fetchCommentsSuccess: 'Comments fetched successfully',
      fetchCommentsFailed: 'Failed to fetch comments',
      noCommentsWarning: 'No comments available, please click the button above to fetch comments',
      filterCommentsFailed: 'Failed to filter comments',
      searchCommentsFailed: 'Failed to search comments',
      sentimentAnalysisSuccess: 'Sentiment analysis completed',
      sentimentAnalysisFailed: 'Sentiment analysis failed',
      exportKeywordsSuccess: 'Keywords exported successfully',
      translate: 'Translate',
      showOriginal: 'Show Original',
      translateFailed: 'Translation failed'
    }
  }
}

const i18n = new VueI18n({
  locale: localStorage.getItem('language') || 'zh',
  messages
})

export default i18n 
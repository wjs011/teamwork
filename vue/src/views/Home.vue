<template>
  <div class="home-container">
    <div class="search-box">
      <h1>JD评论分析系统</h1>
      <div class="input-group">
        <el-autocomplete
          v-model="productUrl"
          :fetch-suggestions="querySearch"
          class="url-input"
          placeholder="请输入京东商品URL..."
          @select="handleSelect"
          @keyup.enter.native="analyzeComments"
        >
          <template #default="{ item }">
            <div class="suggestion-item">
              <div class="product-name">{{ item.productName }}</div>
              <div class="product-url">{{ item.productUrl }}</div>
            </div>
          </template>
        </el-autocomplete>
        <button class="search-button" @click="analyzeComments">
          <i class="el-icon-search"></i> 分析评论
        </button>
      </div>
      <div class="examples">
        <p>示例: https://item.jd.com/10132276662024.html</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Home',
  data() {
    return {
      productUrl: '',
      loading: false,
      searchHistory: []
    }
  },
  created() {
    //this.loadSearchHistory();
  },
  methods: {
    async loadSearchHistory() {
      try {
        const response = await axios.get('/search/history');
        if (response.data.code === '0') {
          this.searchHistory = response.data.data;
        }
      } catch (error) {
        if (error.response && error.response.status === 404) {
          console.log('搜索历史功能暂未实现');
          this.searchHistory = [];
        } else {
          console.error('加载搜索历史失败:', error);
        }
      }
    },
    
    querySearch(queryString, callback) {
      const results = this.searchHistory.filter(item => {
        return item.productName.toLowerCase().includes(queryString.toLowerCase()) ||
               item.productUrl.toLowerCase().includes(queryString.toLowerCase());
      });
      callback(results);
    },
    
    handleSelect(item) {
      this.productUrl = item.productUrl;
      this.analyzeComments();
    },
    
    async analyzeComments() {
      if (!this.productUrl) {
        this.$message.warning('请输入京东商品URL');
        return;
      }
      
      this.loading = true;
      
      // 提取商品ID
      let productId = this.extractProductId(this.productUrl);
      
      if (productId) {
        try {
          // 先获取评论数据
          const response = await axios.get('/comments/fetch', {
            params: { url: this.productUrl }
          });
          
          if (response.data.code === '0') {
            // 尝试保存搜索历史，如果失败也不影响主要功能
            // try {
            //   await axios.post('/search/history', {
            //     productId,
            //     productUrl: this.productUrl
            //   });
            // } catch (error) {
            //   console.log('保存搜索历史失败，但不影响主要功能');
            // }
            
            this.$message.success('评论获取成功');
            this.$router.push({
              name: 'analysis',
              params: { productId },
              query: { url: this.productUrl }
            });
          } else {
            this.$message.error(response.data.msg || '评论获取失败');
          }
        } catch (error) {
          console.error('获取评论失败:', error);
          this.$message.error('获取评论失败: ' + (error.response?.data?.msg || error.message));
        }
      } else {
        this.$message.error('无效的京东商品URL，请检查格式');
      }
      
      this.loading = false;
    },
    
    extractProductId(url) {
      const regex = /\/(\d+)\.html/;
      const match = url.match(regex);
      return match ? match[1] : null;
    }
  }
}
</script>

<style scoped>
.home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.search-box {
  width: 650px;
  padding: 40px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

h1 {
  margin-bottom: 30px;
  color: #409EFF;
  font-size: 32px;
}

.input-group {
  display: flex;
  margin-bottom: 15px;
}

.url-input {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px 0 0 4px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.2s;
}

.url-input:focus {
  border-color: #409EFF;
}

.search-button {
  padding: 0 20px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.search-button:hover {
  background-color: #66b1ff;
}

.examples {
  color: #909399;
  font-size: 14px;
  text-align: left;
}

.suggestion-item {
  padding: 8px 0;
}

.product-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.product-url {
  font-size: 12px;
  color: #909399;
}

.url-input {
  width: 100%;
}
</style> 
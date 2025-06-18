# JD评论分析系统前端

基于Vue.js和Element UI的京东商品评论分析系统前端界面。

## 功能特性

- 输入京东商品URL，自动解析商品ID
- 查看商品评论列表，支持分页
- 按评分筛选评论
- 关键词搜索评论内容
- 评分统计与分布可视化
- 评论关键词云展示

## 技术栈

- Vue.js 2.x
- Vue Router
- Element UI
- Axios

## 项目设置与运行

### 安装依赖
```
npm install
```

### 开发服务器启动
```
npm run serve
```

### 构建生产版本
```
npm run build
```

## 后端API

前端依赖后端提供的以下API：

- `/comments/fetch`: 爬取京东商品评论
- `/comments`: 分页获取评论列表
- `/comments/keywords1/{productId}`: 获取评论关键词
- `/comments/score/{productId}`: 按评分筛选评论
- `/comments/search/{productId}`: 搜索评论关键词

## 页面说明

1. **首页**: 提供商品URL搜索框
2. **分析页**: 评论列表与各种数据分析展示 
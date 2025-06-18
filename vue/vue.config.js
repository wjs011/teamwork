const path = require('path')

module.exports = {
  devServer: {
    port: 8081, // 修改为8081
    open: true, // 自动打开浏览器
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端API地址
        changeOrigin: true,
        pathRewrite: {
          '^/api': '' // 重写路径
        }
        },
      '/comments': {
        target: 'http://localhost:8080', // 后端API地址
        changeOrigin: true
      }
    }
  },
  // 生产环境不生成sourcemap
  productionSourceMap: false,
  // 输出目录设置
  outputDir: 'dist',
  // 静态资源目录
  assetsDir: 'static',
  // CSS配置
  css: {
    loaderOptions: {
      sass: {
        additionalData: `@import "@/styles/variables.scss";`
      }
    }
  },
  transpileDependencies: [],
  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    }
  }
} 
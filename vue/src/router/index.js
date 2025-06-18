import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Analysis from '../views/Analysis.vue'
import AdminHome from '../views/admin/AdminHome.vue'
import UserManagement from '../views/admin/UserManagement.vue'
import CommentManagement from '../views/admin/CommentManagement.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import LayoutView from '../layout/layoutView.vue'
import Person from '../views/Person.vue'
import CommentCategory from '@/views/CommentCategory.vue'
import CommentSummary from '@/views/CommentSummary.vue'
import CommentCompare from '@/views/CommentCompare.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/',
    component: LayoutView,
    children: [
      {
        path: '',
        name: 'home',
        component: Home,
        meta: { requiresAuth: true }
      },
      {
        path: 'analysis/:productId',
        name: 'analysis',
        component: Analysis,
        meta: { requiresAuth: true }
      },
      {
        path: 'person',
        name: 'person',
        component: Person,
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminHome,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: 'dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: Dashboard,
        meta: { title: '控制台' }
      },
      {
        path: 'users',
        name: 'user-management',
        component: UserManagement,
        meta: { title: '用户管理' }
      },
      {
        path: 'comments',
        name: 'comment-management',
        component: CommentManagement,
        meta: { title: '评论管理' }
      }
    ]
  },
  {
    path: '/comment-category',
    name: 'CommentCategory',
    component: CommentCategory,
    meta: {
      title: '评论分类'
    }
  },
  {
    path: '/comment-summary',
    name: 'CommentSummary',
    component: CommentSummary,
    meta: {
      title: '评论摘要'
    }
  },
  {
    path: '/comment-compare',
    name: 'CommentCompare',
    component: CommentCompare,
    meta: {
      title: '评论对比'
    }
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const user = JSON.parse(sessionStorage.getItem('user') || '{}')
  
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!user.id) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else if (to.matched.some(record => record.meta.requiresAdmin) && user.role !== 'admin') {
      next({ path: '/' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router 
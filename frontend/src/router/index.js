import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/progress-monitor',
    children: [
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '用户管理', roles: ['admin'] },
      },
      {
        path: 'dict-config',
        name: 'DictConfig',
        component: () => import('@/views/DictConfig.vue'),
        meta: { title: '元数据中心', roles: ['admin'] },
      },
      {
        path: 'nexus-roster',
        name: 'NexusRoster',
        component: () => import('@/views/CourseStudentImport.vue'),
        meta: { title: '学籍课表网关', roles: ['academic', 'admin'] },
      },
      {
        path: 'progress-monitor',
        name: 'ProgressMonitor',
        component: () => import('@/views/ProgressMonitor.vue'),
        meta: { title: '全景算力雷达', roles: ['academic', 'director', 'admin'] },
      },
      {
        path: 'outcome-blueprint',
        name: 'OutcomeBlueprint',
        component: () => import('@/views/GraduationRequirements.vue'),
        meta: { title: '毕业要求设计器', roles: ['director', 'admin'] },
      },
      {
        path: 'macro-matrix',
        name: 'MacroMatrix',
        component: () => import('@/views/MacroMatrix.vue'),
        meta: { title: '宏观矩阵工坊', roles: ['director', 'admin'] },
      },
      {
        path: 'my-courses',
        name: 'MyCourses',
        component: () => import('@/views/MyCourses.vue'),
        meta: { title: '我的教学空间', roles: ['teacher', 'admin'] },
      },
      {
        path: 'syllabus-config',
        name: 'SyllabusConfig',
        component: () => import('@/views/SyllabusConfig.vue'),
        meta: { title: '大纲映射器', roles: ['teacher', 'admin'] },
      },
      {
        path: 'assessment-mapping',
        name: 'AssessmentMapping',
        component: () => import('@/views/AssessmentMapping.vue'),
        meta: { title: '考核点追溯板', roles: ['teacher', 'admin'] },
      },
      {
        path: 'grade-entry',
        name: 'GradeEntry',
        component: () => import('@/views/GradeEntry.vue'),
        meta: { title: '成绩工场', roles: ['teacher', 'admin'] },
      },
      {
        path: 'report-view',
        name: 'ReportView',
        component: () => import('@/views/ReportView.vue'),
        meta: { title: '洞察报告', roles: ['teacher', 'director', 'academic', 'admin'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  // [DEV] 跳过登录：自动注入 admin 角色以便开发调试
  const DEV_SKIP_AUTH = true
  if (DEV_SKIP_AUTH && !auth.isLoggedIn) {
    auth.setAuth({
      access_token: 'dev-token',
      role: 'admin',
      display_name: '开发用户',
      id: 'dev-001',
    })
  }

  // 登录页：已登录则跳转首页，未登录则放行
  if (to.meta.guest) {
    if (auth.isLoggedIn) {
      return next('/')
    }
    return next()
  }

  // 非登录页：未登录跳转登录
  if (!auth.isLoggedIn) {
    return next('/login')
  }

  // RBAC 角色校验
  const allowedRoles = to.meta.roles
  if (allowedRoles && !allowedRoles.includes(auth.role)) {
    return next('/')
  }

  next()
})

export default router

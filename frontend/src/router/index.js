import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/403.vue'),
    meta: { public: true }
  },
  // ===== 系统管理员 =====
  {
    path: '/admin',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { roles: ['ADMIN'] },
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'semesters',
        name: 'AdminSemesters',
        component: () => import('@/views/admin/SemesterManage.vue'),
        meta: { title: '学期管理', icon: 'Calendar' }
      },
      {
        path: 'colleges',
        name: 'AdminColleges',
        component: () => import('@/views/admin/CollegeManage.vue'),
        meta: { title: '学院管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'majors',
        name: 'AdminMajors',
        component: () => import('@/views/admin/MajorManage.vue'),
        meta: { title: '专业管理', icon: 'Reading' }
      }
    ]
  },
  // ===== 教务管理员 =====
  {
    path: '/academic',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { roles: ['ACADEMIC', 'DIRECTOR'] },
    redirect: '/academic/courses',
    children: [
      {
        path: 'admin-classes',
        name: 'AdminClassManage',
        component: () => import('@/views/admin/ClassManage.vue'),
        meta: { title: '班级管理', icon: 'UserFilled', roles: ['ACADEMIC'] }
      },
      {
        path: 'courses',
        name: 'AcademicCourses',
        component: () => import('@/views/academic/CourseImport.vue'),
        meta: { title: '课程管理', icon: 'Document', roles: ['ACADEMIC'] }
      },
      {
        path: 'students',
        name: 'AcademicStudents',
        component: () => import('@/views/academic/StudentImport.vue'),
        meta: { title: '学生信息管理', icon: 'Tickets', roles: ['ACADEMIC'] }
      },
      {
        path: 'teaching-classes',
        name: 'AcademicTeachingClasses',
        component: () => import('@/views/academic/TeachingClassManage.vue'),
        meta: { title: '教学班级管理', icon: 'School', roles: ['ACADEMIC'] }
      },
      {
        path: 'score-unlock',
        name: 'ScoreUnlock',
        component: () => import('@/views/admin/ScoreUnlock.vue'),
        meta: { title: '成绩管理', icon: 'Checked', roles: ['ACADEMIC'] }
      },
      {
        path: 'dashboard',
        name: 'GlobalDashboard',
        component: () => import('@/views/academic/GlobalDashboard.vue'),
        meta: { title: '达成度监控', icon: 'DataBoard', roles: ['ACADEMIC', 'DIRECTOR'] }
      },
      {
        path: 'batch-import',
        name: 'BatchImport',
        component: () => import('@/views/academic/BatchImport.vue'),
        meta: { title: '批量数据导入', icon: 'UploadFilled', roles: ['ACADEMIC'] }
      },
      {
        path: 'reports',
        name: 'ReportExport',
        component: () => import('@/views/academic/ReportExport.vue'),
        meta: { title: '计算数据管理', icon: 'Histogram', roles: ['ACADEMIC', 'DIRECTOR'] }
      },
    ]
  },
  // ===== 专业负责人 =====
  {
    path: '/director',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { roles: ['DIRECTOR'] },
    redirect: '/director/grad-req',
    children: [
      {
        path: 'grad-req',
        name: 'DirectorGradReq',
        component: () => import('@/views/director/GradReqManage.vue'),
        meta: { title: '毕业要求管理', icon: 'Aim' }
      },
      {
        path: 'macro-matrix',
        name: 'DirectorMacroMatrix',
        component: () => import('@/views/director/MacroMatrix.vue'),
        meta: { title: '宏观支撑矩阵', icon: 'Grid' }
      },
      {
        path: 'global-compute',
        name: 'DirectorGlobalCompute',
        component: () => import('@/views/director/GlobalCompute.vue'),
        meta: { title: '专业级计算', icon: 'TrendCharts' }
      }
    ]
  },
  // ===== 主讲教师 =====
  {
    path: '/teacher',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { roles: ['TEACHER'] },
    redirect: '/teacher/objectives',
    children: [
      {
        path: 'objectives/:courseId?',
        name: 'TeacherObjectives',
        component: () => import('@/views/teacher/ObjectiveSetup.vue'),
        meta: { title: '课程目标设定', icon: 'Flag' }
      },
      {
        path: 'weights/:courseId?',
        name: 'TeacherWeights',
        component: () => import('@/views/teacher/WeightAssign.vue'),
        meta: { title: '内部权重分配', icon: 'PieChart' }
      },
      {
        path: 'assessments/:courseId?',
        name: 'TeacherAssessments',
        component: () => import('@/views/teacher/AssessmentMap.vue'),
        meta: { title: '考核点映射', icon: 'EditPen' }
      },
      {
        path: 'scores/:courseId?',
        name: 'TeacherScores',
        component: () => import('@/views/teacher/ScoreImport.vue'),
        meta: { title: '成绩录入', icon: 'Upload' }
      },
      {
        path: 'compute/:courseId?',
        name: 'TeacherCompute',
        component: () => import('@/views/teacher/CourseCompute.vue'),
        meta: { title: '课程级计算', icon: 'DataAnalysis' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) return next()

  const userStore = useUserStore()
  if (!userStore.token) return next('/login')

  const permStore = usePermissionStore()
  permStore.generateRoutes(userStore.role)

  if (to.path === '/' || to.path === '/login') {
    const redirectMap = { ADMIN: '/admin/users', ACADEMIC: '/academic/courses', DIRECTOR: '/director/grad-req', TEACHER: '/teacher/objectives' }
    return next(redirectMap[userStore.role] || '/login')
  }

  if (!permStore.hasPermission(to.path, userStore.role) && to.path !== '/403') {
    return next('/403')
  }
  next()
})

export default router

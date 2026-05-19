<template>
  <el-container class="main-layout">
    <el-aside width="220px">
      <div class="logo">达成度计算平台</div>
      <el-menu
        :default-active="currentRoute"
        router
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag>{{ roleLabel }}</el-tag>
          <span class="username">{{ auth.displayName }}</span>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import {
  Setting,
  School,
  Monitor,
  DataBoard,
  Grid,
  Reading,
  EditPen,
  DocumentCopy,
  Upload,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const currentRoute = computed(() => route.path)

const currentTitle = computed(() => route.meta?.title || '达成度计算平台')

const roleLabel = computed(() => {
  const labels = { admin: '系统管理员', academic: '教务管理', director: '专业负责人', teacher: '主讲教师' }
  return labels[auth.role] || auth.role
})

const menuItems = computed(() => {
  const menus = {
    admin: [
      { path: '/user-management', title: '用户管理', icon: 'User' },
      { path: '/dict-config', title: '元数据中心', icon: 'Setting' },
      { path: '/nexus-roster', title: '学籍课表网关', icon: 'School' },
      { path: '/progress-monitor', title: '全景算力雷达', icon: 'Monitor' },
      { path: '/outcome-blueprint', title: '毕业要求设计器', icon: 'DataBoard' },
      { path: '/macro-matrix', title: '宏观矩阵工坊', icon: 'Grid' },
      { path: '/report-view', title: '洞察报告', icon: 'TrendCharts' },
    ],
    academic: [
      { path: '/nexus-roster', title: '学籍课表网关', icon: 'School' },
      { path: '/progress-monitor', title: '全景算力雷达', icon: 'Monitor' },
      { path: '/report-view', title: '洞察报告', icon: 'TrendCharts' },
    ],
    director: [
      { path: '/outcome-blueprint', title: '毕业要求设计器', icon: 'DataBoard' },
      { path: '/macro-matrix', title: '宏观矩阵工坊', icon: 'Grid' },
      { path: '/progress-monitor', title: '全景算力雷达', icon: 'Monitor' },
      { path: '/report-view', title: '洞察报告', icon: 'TrendCharts' },
    ],
    teacher: [
      { path: '/my-courses', title: '我的教学空间', icon: 'Reading' },
      { path: '/syllabus-config', title: '大纲映射器', icon: 'EditPen' },
      { path: '/assessment-mapping', title: '考核点追溯板', icon: 'DocumentCopy' },
      { path: '/grade-entry', title: '成绩工场', icon: 'Upload' },
      { path: '/report-view', title: '洞察报告', icon: 'TrendCharts' },
    ],
  }
  return menus[auth.role] || []
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.el-aside {
  background: #f3f4f6;
  border-right: 1px solid #e5e6eb;
  overflow-y: auto;
}
.logo {
  height: 52px;
  line-height: 52px;
  text-align: center;
  color: #1d2129;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  background: #ebedf0;
  border-bottom: 1px solid #e5e6eb;
}
.el-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fb;
  border-bottom: 1px solid #e5e6eb;
  padding: 0 24px;
  height: 48px;
}
.header-left .page-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.username {
  color: #4e5969;
  font-size: 13px;
}
.el-main {
  background: #fff;
  padding: 24px;
}
.el-menu {
  border-right: none;
  background: transparent;
}
:deep(.el-menu-item) {
  color: #4e5969;
}
:deep(.el-menu-item.is-active) {
  color: #165dff;
  background: rgba(22, 93, 255, 0.06);
}
:deep(.el-menu-item:hover) {
  background: rgba(0, 0, 0, 0.04);
}
</style>

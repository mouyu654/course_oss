<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import {
  Fold, Expand, SwitchButton, User, Setting, Bell,
  Flag, PieChart, EditPen, Upload, Warning, DataAnalysis,
  Document, Tickets, School, Checked, DataBoard, UploadFilled,
  Histogram, Aim, Grid, Connection, TrendCharts, Calendar,
  OfficeBuilding, Reading, UserFilled, ArrowRight, ArrowDown,
  Search, FullScreen
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { logout } = useAuth()

const isCollapse = ref(false)
const isHovering = ref(false)

// 根据角色筛选可见的子菜单
const menuItems = computed(() => {
  const parent = route.matched[0]
  if (!parent?.children) return []
  return parent.children
    .filter(child => {
      if (!child.meta?.roles) return true
      return child.meta.roles.includes(userStore.role)
    })
    .map(child => ({
      path: `${parent.path}/${child.path}`,
      title: child.meta?.title || child.name,
      icon: child.meta?.icon || 'Document'
    }))
})

const activeMenu = computed(() => route.path)

// 角色中文名
const roleName = computed(() => {
  const map = {
    'ADMIN': '系统管理员',
    'ACADEMIC': '教务管理员',
    'DIRECTOR': '专业负责人',
    'TEACHER': '主讲教师'
  }
  return map[userStore.role] || userStore.role
})

// 角色颜色
const roleColor = computed(() => {
  const map = {
    'ADMIN': '#E5432E',
    'ACADEMIC': '#2563EB',
    'DIRECTOR': '#7C3AED',
    'TEACHER': '#059669'
  }
  return map[userStore.role] || '#64748B'
})

function handleMenuSelect(path) {
  router.push(path)
}

function handleLogout() {
  logout()
}

function handleUserCommand(command) {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    // TODO: navigate to profile page
  } else if (command === 'settings') {
    // TODO: navigate to settings page
  }
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}
</script>

<template>
  <el-container style="height: 100vh;">
    <!-- 左侧导航栏 -->
    <el-aside
      :width="isCollapse ? '72px' : '240px'"
      class="sidebar"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
    >
      <!-- Logo 区域 -->
      <div class="sidebar-logo" :class="{ collapsed: isCollapse }">
        <div class="logo-icon">
          <svg width="32" height="32" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="10" fill="rgba(255,255,255,0.15)"/>
            <path d="M24 12L36 20V28L24 36L12 28V20L24 12Z" stroke="white" stroke-width="2.5" stroke-linejoin="round"/>
            <path d="M24 20V28" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="24" cy="16" r="2" fill="white"/>
          </svg>
        </div>
        <transition name="fade">
          <div v-if="!isCollapse" class="logo-text">
            <span class="logo-title">OBE</span>
            <span class="logo-subtitle">达成度平台</span>
          </div>
        </transition>
      </div>

      <!-- 菜单区域 -->
      <div class="sidebar-menu">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          @select="handleMenuSelect"
          class="menu-list"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
            class="menu-item"
          >
            <div class="menu-icon">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <template #title>
              <span class="menu-title">{{ item.title }}</span>
            </template>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 用户信息区域 -->
      <div class="sidebar-user" :class="{ collapsed: isCollapse }">
        <div class="user-avatar" :style="{ backgroundColor: roleColor }">
          {{ userStore.realName?.charAt(0) || 'U' }}
        </div>
        <transition name="fade">
          <div v-if="!isCollapse" class="user-info">
            <div class="user-name">{{ userStore.realName }}</div>
            <div class="user-role" :style="{ color: roleColor }">{{ roleName }}</div>
          </div>
        </transition>
      </div>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <div class="breadcrumb-nav">
            <el-icon
              class="collapse-btn"
              :size="22"
              @click="isCollapse = !isCollapse"
            >
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
            <div class="breadcrumb-separator"></div>
            <div class="breadcrumb">
              <span class="breadcrumb-home">首页</span>
              <el-icon class="breadcrumb-arrow" :size="12"><ArrowRight /></el-icon>
              <span class="breadcrumb-current">{{ menuItems.find(m => m.path === activeMenu)?.title || '首页' }}</span>
            </div>
          </div>
        </div>

        <div class="header-right">
<!--          <div class="header-search">-->
<!--            <el-icon class="search-icon" :size="18"><Search /></el-icon>-->
<!--            <input type="text" placeholder="搜索功能..." class="search-input" />-->
<!--            <span class="search-shortcut">⌘K</span>-->
<!--          </div>-->

<!--          <el-badge :value="3" :max="99" class="notification-badge">-->
<!--            <div class="icon-btn">-->
<!--              <el-icon :size="22"><Bell /></el-icon>-->
<!--            </div>-->
<!--          </el-badge>-->

          <div class="icon-btn" @click="toggleFullscreen">
            <el-icon :size="22"><FullScreen /></el-icon>
          </div>

          <div class="header-divider"></div>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-profile">
              <div class="user-avatar-header" :style="{ backgroundColor: roleColor }">
                {{ userStore.realName?.charAt(0) || 'U' }}
              </div>
              <div class="user-details">
                <span class="user-display-name">{{ userStore.realName }}</span>
                <span class="user-display-role">{{ roleName }}</span>
              </div>
              <el-icon class="dropdown-arrow" :size="16"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
<!--                <el-dropdown-item command="profile">-->
<!--                  <el-icon><User /></el-icon>-->
<!--                  个人中心-->
<!--                </el-dropdown-item>-->
<!--                <el-dropdown-item command="settings">-->
<!--                  <el-icon><Setting /></el-icon>-->
<!--                  系统设置-->
<!--                </el-dropdown-item>-->
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
/* ===== CSS Variables ===== */
:root {
  --sidebar-bg: #0F172A;
  --sidebar-hover: #1E293B;
  --sidebar-active: #2563EB;
  --sidebar-text: #94A3B8;
  --sidebar-text-active: #FFFFFF;
  --header-height: 72px;
  --transition-speed: 0.3s;
}

/* ===== Sidebar ===== */
.sidebar {
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-speed) cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  z-index: 100;
}

/* ===== Logo ===== */
.sidebar-logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar-logo.collapsed {
  justify-content: center;
  padding: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
  overflow: hidden;
}

.logo-title {
  font-size: 18px;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 0.5px;
}

.logo-subtitle {
  font-size: 12px;
  color: #64748B;
  margin-top: 2px;
}

/* ===== Menu ===== */
.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 0;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.menu-list {
  border-right: none !important;
  background: transparent !important;
}

.menu-item {
  height: 48px;
  margin: 4px 12px;
  border-radius: 10px;
  color: #94A3B8 !important;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #FFFFFF !important;
}

.menu-item.is-active {
  background: linear-gradient(135deg, #2563EB 0%, #3B82F6 100%) !important;
  color: #FFFFFF !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}

.menu-item.is-active .menu-icon {
  color: #FFFFFF;
}

.menu-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  margin-right: 12px;
  transition: all 0.2s ease;
}

.menu-item.is-active .menu-icon {
  background: rgba(255, 255, 255, 0.15);
}

.menu-title {
  font-size: 14px;
  font-weight: 500;
}

/* ===== User Info ===== */
.sidebar-user {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar-user.collapsed {
  justify-content: center;
  padding: 0;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #F1F5F9;
}

.user-role {
  font-size: 12px;
  font-weight: 500;
  margin-top: 2px;
}

/* ===== Header ===== */
.header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #FFFFFF;
  border-bottom: 1px solid #F1F5F9;
}

.header-left {
  display: flex;
  align-items: center;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748B;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: #F1F5F9;
  color: #1E293B;
}

.breadcrumb-separator {
  width: 1px;
  height: 20px;
  background: #E2E8F0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-home {
  color: #94A3B8;
  font-weight: 500;
}

.breadcrumb-arrow {
  font-size: 12px;
  color: #CBD5E1;
}

.breadcrumb-current {
  color: #1E293B;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-search {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 8px 14px;
  margin-right: 8px;
  transition: all 0.2s ease;
}

.header-search:focus-within {
  border-color: #2563EB;
  background: #FFFFFF;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-icon {
  color: #94A3B8;
}

.search-input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
  color: #1E293B;
  width: 180px;
}

.search-input::placeholder {
  color: #94A3B8;
}

.search-shortcut {
  font-size: 12px;
  color: #94A3B8;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 4px;
  padding: 2px 6px;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.icon-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: #F1F5F9;
  color: #1E293B;
}

.notification-badge :deep(.el-badge__content) {
  background: #EF4444;
  border: 2px solid #FFFFFF;
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
}

.header-divider {
  width: 1px;
  height: 32px;
  background: #E2E8F0;
  margin: 0 8px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-profile:hover {
  background: #F1F5F9;
}

.user-avatar-header {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-display-name {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
  line-height: 1.2;
}

.user-display-role {
  font-size: 12px;
  color: #94A3B8;
  line-height: 1.2;
}

.dropdown-arrow {
  color: #94A3B8;
  margin-left: 4px;
}

/* ===== Main Content ===== */
.main-container {
  background: #F8FAFC;
}

.main-content {
  background: #F8FAFC;
  padding: 24px;
  overflow-y: auto;
}

/* ===== Animations ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .sidebar,
  .menu-item,
  .collapse-btn,
  .icon-btn,
  .user-profile,
  .header-search {
    transition: none;
  }
}

/* ===== Global Dropdown Styles ===== */
:deep(.el-dropdown-menu) {
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 6px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 14px;
  color: #475569;
  transition: all 0.15s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #F1F5F9;
  color: #1E293B;
}

:deep(.el-dropdown-menu__item--divided) {
  border-top: 1px solid #F1F5F9;
  margin-top: 4px;
  padding-top: 10px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 18px;
  color: #64748B;
}

:deep(.el-dropdown-menu__item:hover .el-icon) {
  color: #1E293B;
}
</style>

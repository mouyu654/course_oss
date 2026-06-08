<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { logout } = useAuth()

const isCollapse = ref(false)

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

function handleMenuSelect(path) {
  router.push(path)
}

function handleLogout() {
  logout()
}
</script>

<template>
  <el-container style="height: 100vh;">
    <!-- 左侧导航栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" style="transition: width 0.3s; background-color: #fff; border-right: 1px solid #e4e7ed;">
      <div style="height: 56px; display: flex; align-items: center; justify-content: center; border-bottom: 1px solid #e4e7ed;">
        <span v-if="!isCollapse" style="font-size: 15px; font-weight: 600; color: #303133; white-space: nowrap;">达成度计算平台</span>
        <span v-else style="font-size: 15px; font-weight: 600; color: #303133;">OBE</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        @select="handleMenuSelect"
        style="border-right: none;"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header style="height: 56px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; background-color: #fff; border-bottom: 1px solid #e4e7ed;">
        <el-icon
          style="cursor: pointer; font-size: 20px; color: #606266;"
          @click="isCollapse = !isCollapse"
        >
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>

        <div style="display: flex; align-items: center; gap: 16px;">
          <span style="font-size: 14px; color: #606266;">{{ userStore.realName }}</span>
          <el-tag size="small" type="info">{{ userStore.role }}</el-tag>
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main style="background-color: #f5f7fa; padding: 20px; overflow-y: auto;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

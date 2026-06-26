<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  if (userStore.isLoggedIn) {
    router.push('/')
  } else {
    router.push('/login')
  }
}
</script>

<template>
  <div style="min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #f5f7fa;">
    <h1 style="font-size: 72px; color: #dcdfe6; margin: 0;">403</h1>
    <p style="font-size: 16px; color: #909399; margin: 16px 0 24px;">抱歉，您没有权限访问该页面</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

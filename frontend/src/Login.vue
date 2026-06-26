<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { User, Lock } from '@element-plus/icons-vue'

const { login, loading } = useAuth()

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const formRef = ref(null)

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await login(form.username, form.password)
}
</script>

<template>
  <div class="login-container">
    <!-- Left decorative panel -->
    <div class="login-decoration">
      <div class="decoration-content">
        <div class="logo-mark">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.15)"/>
            <path d="M24 12L36 20V28L24 36L12 28V20L24 12Z" stroke="white" stroke-width="2" stroke-linejoin="round"/>
            <path d="M24 20V28" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="24" cy="16" r="2" fill="white"/>
          </svg>
        </div>
        <h1 class="decoration-title">毕业要求达成度<br/>计算平台</h1>
        <p class="decoration-subtitle">面向专业认证的统一计算系统</p>
        <div class="feature-list">
          <div class="feature-item">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="10" fill="rgba(255,255,255,0.2)"/>
              <path d="M6 10L9 13L14 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>课程达成度分析</span>
          </div>
          <div class="feature-item">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="10" fill="rgba(255,255,255,0.2)"/>
              <path d="M6 10L9 13L14 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>毕业要求评估</span>
          </div>
          <div class="feature-item">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="10" fill="rgba(255,255,255,0.2)"/>
              <path d="M6 10L9 13L14 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>数据可视化报告</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right login form -->
    <div class="login-form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <div class="mobile-logo">
            <svg width="40" height="40" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="12" fill="#1E3A5F"/>
              <path d="M24 12L36 20V28L24 36L12 28V20L24 12Z" stroke="white" stroke-width="2" stroke-linejoin="round"/>
              <path d="M24 20V28" stroke="white" stroke-width="2" stroke-linecap="round"/>
              <circle cx="24" cy="16" r="2" fill="white"/>
            </svg>
          </div>
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账户以继续</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              @keyup.enter="handleLogin"
              class="custom-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
              class="custom-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              <span v-if="!loading">登 录</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <p class="copyright">© 2026 OBE 达成度计算平台</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== CSS Variables ===== */
:root {
  --color-primary: #1E3A5F;
  --color-primary-light: #2D4A6F;
  --color-secondary: #2563EB;
  --color-accent: #059669;
  --color-background: #F8FAFC;
  --color-foreground: #0F172A;
  --color-muted: #F1F3F5;
  --color-border: #E4E7EB;
  --color-text-secondary: #64748B;
  --font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== Container ===== */
.login-container {
  display: flex;
  min-height: 100vh;
  background-color: var(--color-background);
  font-family: var(--font-family);
}

/* ===== Left Decoration Panel ===== */
.login-decoration {
  flex: 1;
  background: linear-gradient(135deg, #1E3A5F 0%, #2563EB 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
  overflow: hidden;
}

.login-decoration::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.login-decoration::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -30%;
  width: 80%;
  height: 80%;
  background: radial-gradient(circle, rgba(5,150,105,0.15) 0%, transparent 70%);
  pointer-events: none;
}

.decoration-content {
  position: relative;
  z-index: 1;
  max-width: 400px;
  color: white;
}

.logo-mark {
  margin-bottom: 32px;
}

.decoration-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 16px;
  letter-spacing: -0.02em;
}

.decoration-subtitle {
  font-size: 16px;
  opacity: 0.85;
  margin: 0 0 40px;
  line-height: 1.6;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  opacity: 0.9;
}

.feature-item svg {
  flex-shrink: 0;
}

/* ===== Right Form Panel ===== */
.login-form-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: white;
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
}

/* ===== Form Header ===== */
.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.mobile-logo {
  display: none;
  justify-content: center;
  margin-bottom: 24px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* ===== Form Styles ===== */
.login-form {
  margin-bottom: 32px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 16px;
  box-shadow: 0 0 0 1px var(--color-border) inset;
  transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #CBD5E1 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-secondary) inset;
}

.login-form :deep(.el-input__prefix) {
  color: var(--color-text-secondary);
}

.login-form :deep(.el-input__inner) {
  height: 48px;
  font-size: 15px;
  color: var(--color-foreground);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #94A3B8;
}

/* ===== Login Button ===== */
.login-button {
  width: 100%;
  height: 52px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #1E3A5F 0%, #2563EB 100%);
  border: none;
  transition: all 0.2s ease;
  letter-spacing: 0.05em;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

.login-button:active {
  transform: translateY(0);
  box-shadow: none;
}

/* ===== Form Footer ===== */
.form-footer {
  text-align: center;
}

.copyright {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .login-decoration {
    display: none;
  }

  .login-form-panel {
    width: 100%;
    background: var(--color-background);
  }

  .mobile-logo {
    display: flex;
  }
}

@media (max-width: 480px) {
  .login-form-panel {
    padding: 32px 24px;
  }

  .form-title {
    font-size: 24px;
  }
}

/* ===== Focus States (Accessibility) ===== */
.login-form :deep(.el-input__wrapper.is-focus) {
  outline: none;
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .login-button {
    transition: none;
  }

  .login-button:hover {
    transform: none;
  }
}
</style>

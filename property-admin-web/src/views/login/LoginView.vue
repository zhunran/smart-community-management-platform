<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-icon">
          <el-icon :size="40"><OfficeBuilding /></el-icon>
        </div>
        <h1 class="brand-title">物业管理收费系统</h1>
        <p class="brand-slogan">智慧物业 · 高效管理</p>
        <p class="brand-desc">数据驱动决策，服务触达每一位业主</p>
      </div>
      <!-- 抽象几何装饰 -->
      <div class="decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
        <div class="decoration-line line-1"></div>
        <div class="decoration-line line-2"></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="login-card">
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-desc">请输入您的账号信息</p>
        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
    })
    router.push('/')
  } catch {
    // 错误已在 loginApi 或 store 中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 布局 ===== */
.login-container {
  display: flex;
  min-height: 100vh;
}

/* ===== 左侧品牌区 ===== */
.brand-panel {
  flex: 0 0 45%;
  background: linear-gradient(180deg, #1e2a3a 0%, #2d4059 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  padding: 60px;
}

.brand-content {
  text-align: center;
  position: relative;
  z-index: 1;
  animation: brand-enter 0.8s ease-out;
}

@keyframes brand-enter {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.brand-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  backdrop-filter: blur(10px);
}

.brand-title {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.brand-slogan {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
  letter-spacing: 4px;
}

.brand-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

/* 抽象几何装饰 */
.decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -100px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: 60px;
  left: -60px;
  border-color: rgba(255, 255, 255, 0.05);
}

.circle-3 {
  width: 120px;
  height: 120px;
  bottom: 30%;
  right: 20%;
  border-color: rgba(255, 255, 255, 0.06);
}

.decoration-line {
  position: absolute;
  background: rgba(255, 255, 255, 0.04);
}

.line-1 {
  width: 1px;
  height: 200px;
  top: 20%;
  right: 35%;
  transform: rotate(30deg);
}

.line-2 {
  width: 1px;
  height: 140px;
  bottom: 25%;
  right: 45%;
  transform: rotate(-20deg);
}

/* ===== 右侧表单区 ===== */
.form-panel {
  flex: 1;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.card-desc {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 28px;
}

.login-form {
  margin-top: 4px;
}

/* 输入框 */
.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: box-shadow 0.25s, border-color 0.25s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.login-form :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15) inset;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 46px;
  background: #3b82f6;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  transition: all 0.2s;
}

.login-btn:hover {
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.login-btn:active {
  transform: scale(0.98);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }

  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
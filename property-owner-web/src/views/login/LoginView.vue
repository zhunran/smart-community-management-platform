<template>
  <div class="login-page">
    <!-- 品牌区 -->
    <div class="brand-section">
      <div class="logo-wrapper">
        <van-icon name="home-o" size="28" color="#3b82f6" />
      </div>
      <p class="brand-name">智慧物业</p>
      <p class="brand-slogan">业主服务端</p>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <!-- 欢迎语 -->
      <div class="welcome-text">
        <h2 class="welcome-title">欢迎回来</h2>
        <p class="welcome-desc">请登录您的业主账号</p>
      </div>

      <van-form @submit="handleLogin" class="login-form">
        <van-field
          v-model="loginForm.phone"
          name="phone"
          placeholder="请输入手机号"
          maxlength="11"
          type="tel"
          left-icon="phone-o"
          :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1\d{10}$/, message: '手机号格式不正确' }]"
        />
        <van-field
          v-model="loginForm.password"
          name="password"
          placeholder="请输入密码"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          left-icon="lock"
          :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPassword = !showPassword"
          :rules="[{ required: true, message: '请输入密码' }, { pattern: /^\S{6,}$/, message: '密码至少 6 位' }]"
        />
        <van-field
          v-model="loginForm.captcha"
          name="captcha"
          placeholder="请输入验证码"
          maxlength="5"
          left-icon="shield-o"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <img
              :src="captchaUrl"
              class="captcha-img"
              alt="验证码"
              title="点击刷新验证码"
              @click="refreshCaptcha"
            />
          </template>
        </van-field>

        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
          class="login-btn"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </van-button>
      </van-form>

      <div class="login-footer">
        <span>默认密码为手机号后 6 位</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOwnerStore } from '@/stores/owner'

const router = useRouter()
const ownerStore = useOwnerStore()

const loading = ref(false)
const showPassword = ref(false)
const captchaUrl = ref('')

function refreshCaptcha() {
  captchaUrl.value = '/api/owner/auth/captcha?t=' + Date.now()
}

const loginForm = reactive({
  phone: '',
  password: '',
  captcha: '',
})

async function handleLogin() {
  loading.value = true
  try {
    await ownerStore.login({
      phone: loginForm.phone,
      password: loginForm.password,
      captcha: loginForm.captcha,
    })
    router.push('/')
  } catch {
    refreshCaptcha()
    loginForm.captcha = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e8f4fd 0%, #f5f7fa 40%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 24px;
}

/* ======== 品牌区 ======== */
.brand-section {
  text-align: center;
  margin-top: 60px;
  margin-bottom: 32px;
  animation: brand-enter 0.6s ease-out;
}

@keyframes brand-enter {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.logo-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: #fff;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.brand-name {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.brand-slogan {
  font-size: 13px;
  color: #8899aa;
  margin: 0;
}

/* ======== 表单卡片 ======== */
.form-card {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 28px 20px 24px;
  box-shadow: 0 2px 24px rgba(0, 0, 0, 0.04);
  animation: card-enter 0.6s ease-out 0.1s both;
}

@keyframes card-enter {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 欢迎语 */
.welcome-text {
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px;
}

.welcome-desc {
  font-size: 14px;
  color: #8899aa;
  margin: 0;
}

/* 表单输入框定制 */
.login-form :deep(.van-field) {
  padding: 12px 0;
  margin-bottom: 4px;
  border-radius: 12px;
  background: #f8fafb;
  transition: background 0.25s, box-shadow 0.25s;
}

.login-form :deep(.van-field:focus-within) {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(79, 140, 255, 0.15);
}

.login-form :deep(.van-field__control) {
  font-size: 15px;
  color: #1a1a2e;
}

.login-form :deep(.van-field__control::placeholder) {
  color: #c0ccda;
}

.login-form :deep(.van-field__left-icon) {
  color: #8899aa;
  font-size: 18px;
  margin-right: 8px;
}

.login-form :deep(.van-field__right-icon) {
  color: #8899aa;
}

/* 验证码图片 */
.captcha-img {
  height: 36px;
  width: 90px;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: #3b82f6;
}

/* 登录按钮 */
.login-btn {
  margin-top: 20px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  background: linear-gradient(135deg, #4F8CFF 0%, #3B6FE8 100%);
  box-shadow: 0 4px 16px rgba(79, 140, 255, 0.3);
  transition: all 0.3s;
}

.login-btn:active {
  transform: scale(0.98);
  box-shadow: 0 2px 8px rgba(79, 140, 255, 0.2);
}

.login-btn :deep(.van-button__text) {
  color: #fff;
}

/* 底部提示 */
.login-footer {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 20px;
}
</style>
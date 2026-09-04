<template>
  <div class="page">
    <van-nav-bar
      title="支付结果"
      left-text="返回"
      left-arrow
      @click-left="goHome"
    />

    <div class="success-body">
      <van-icon name="checked" size="64" color="#07c160" />
      <h2>支付成功</h2>
      <p class="payment-no">支付单号：{{ paymentNo }}</p>
      <p class="tip">{{ countdown }} 秒后自动跳转至支付记录</p>
      <div class="actions">
        <van-button type="primary" block round @click="goRecords"
          >查看支付记录</van-button
        >
        <van-button plain block round style="margin-top: 12px" @click="goHome"
          >返回首页</van-button
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { computed, ref, onMounted, onUnmounted } from "vue";

const route = useRoute();
const router = useRouter();

const paymentNo = computed(() => route.params.paymentNo as string);
const countdown = ref(3);
let timer: ReturnType<typeof setInterval> | null = null;

function goHome() {
  router.push("/");
}
function goRecords() {
  router.push("/records");
}

onMounted(() => {
  sessionStorage.removeItem("__paying__");
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer!);
      router.replace("/records");
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}

.success-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  text-align: center;
}

.success-body h2 {
  margin: 16px 0 8px;
  font-size: 20px;
}

.payment-no {
  font-size: 13px;
  color: #909399;
}

.tip {
  font-size: 13px;
  color: #c0c4cc;
  margin: 8px 0 32px;
}

.actions {
  width: 100%;
  max-width: 300px;
}
</style>

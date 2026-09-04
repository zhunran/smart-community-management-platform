<template>
  <div class="service-home">
    <div class="hero">
      <div class="hero-title">便民服务</div>
      <div class="hero-sub">报修 · 场地预约 · 访客邀请 · 工单查询</div>
    </div>

    <div class="service-grid">
      <div
        v-for="s in services"
        :key="s.path"
        class="service-card"
        @click="router.push(s.path)"
      >
        <div class="service-icon" :style="{ background: s.bg, color: s.color }">
          <van-icon :name="s.icon" size="24" />
        </div>
        <div class="service-info">
          <div class="service-name">{{ s.label }}</div>
          <div class="service-desc">{{ s.desc }}</div>
        </div>
        <van-icon name="arrow" class="service-arrow" color="#cbd5e1" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();

const services = [
  {
    label: "我要报修",
    desc: "提交维修工单",
    icon: "records-o",
    path: "/service/repair",
    color: "#3b82f6",
    bg: "#eff6ff",
  },
  {
    label: "场地预约",
    desc: "健身房/会议室等",
    icon: "shop-o",
    path: "/service/venue",
    color: "#8b5cf6",
    bg: "#f5f3ff",
  },
  {
    label: "访客邀请",
    desc: "生成访客通行码",
    icon: "friends-o",
    path: "/service/visitor",
    color: "#06b6d4",
    bg: "#ecfeff",
  },
  {
    label: "我的工单",
    desc: "查看报修进度",
    icon: "todo-list-o",
    path: "/service/mine",
    color: "#22c55e",
    bg: "#f0fdf4",
  },
];
</script>

<style scoped>
.service-home {
  min-height: 100%;
  background: #f5f7fa;
}
.hero {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  padding: 28px 20px 24px;
  color: #fff;
}
.hero-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 6px;
}
.hero-sub {
  font-size: 13px;
  opacity: 0.9;
}
.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  padding: 16px;
}
.service-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  position: relative;
}
.service-card:active {
  transform: scale(0.96);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}
.service-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.service-info {
  flex: 1;
  min-width: 0;
}
.service-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 3px;
}
.service-desc {
  font-size: 12px;
  color: #94a3b8;
}
.service-arrow {
  position: absolute;
  top: 12px;
  right: 12px;
}
</style>

import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useOwnerStore } from "@/stores/owner";
import { validateSession, resetSessionValidation } from "@/utils/session";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/LoginView.vue"),
    meta: { requiresAuth: false },
  },
  // TabBar 布局下的主页面
  {
    path: "/",
    component: () => import("@/components/TabBarLayout.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "",
        component: () => import("@/views/home/HomeView.vue"),
        meta: { title: "首页" },
      },
      {
        path: "community",
        component: () => import("@/views/community/CommunityHome.vue"),
        meta: { title: "社区" },
      },
      {
        path: "service",
        component: () => import("@/views/service/ServiceHome.vue"),
        meta: { title: "便民服务" },
      },
      {
        path: "bills",
        component: () => import("@/views/bills/BillsView.vue"),
        meta: { title: "我的账单" },
      },
      {
        path: "payment",
        component: () => import("@/views/payment/PaymentView.vue"),
        meta: { title: "在线缴费" },
      },
      {
        path: "profile",
        component: () => import("@/views/profile/ProfileView.vue"),
        meta: { title: "个人信息" },
      },
    ],
  },
  // 子页面（无 TabBar，有返回按钮）
  {
    path: "/bills/:id",
    component: () => import("@/views/bills/BillDetailView.vue"),
    meta: { requiresAuth: true, title: "账单明细" },
  },
  {
    path: "/payment/success/:paymentNo",
    component: () => import("@/views/payment/PaymentSuccessView.vue"),
    meta: { requiresAuth: true, title: "支付成功" },
  },
  {
    path: "/payment/return",
    component: () => import("@/views/payment/PaymentReturnView.vue"),
    meta: { requiresAuth: true, title: "支付结果" },
  },
  {
    path: "/records",
    component: () => import("@/views/records/RecordsView.vue"),
    meta: { requiresAuth: true, title: "支付记录" },
  },
  {
    path: "/rooms",
    component: () => import("@/views/rooms/RoomsView.vue"),
    meta: { requiresAuth: true, title: "我的房屋" },
  },
  {
    path: "/chat",
    name: "chat",
    component: () => import("@/views/chat/ChatView.vue"),
    meta: { requiresAuth: true, title: "AI 客服" },
  },
  {
    path: "/notices",
    component: () => import("@/views/notice/NoticeView.vue"),
    meta: { requiresAuth: true, title: "社区公告" },
  },
  // 社区互动
  {
    path: "/community/activity/:id",
    component: () => import("@/views/community/ActivityDetail.vue"),
    meta: { requiresAuth: true, title: "活动详情" },
  },
  {
    path: "/community/forum/create",
    component: () => import("@/views/community/ForumCreate.vue"),
    meta: { requiresAuth: true, title: "发布帖子" },
  },
  {
    path: "/community/forum/:id",
    component: () => import("@/views/community/ForumDetail.vue"),
    meta: { requiresAuth: true, title: "帖子详情" },
  },
  {
    path: "/community/vote/:id",
    component: () => import("@/views/community/VoteDetail.vue"),
    meta: { requiresAuth: true, title: "投票详情" },
  },
  // 便民服务
  {
    path: "/service/repair",
    component: () => import("@/views/service/RepairView.vue"),
    meta: { requiresAuth: true, title: "我要报修" },
  },
  {
    path: "/service/mine",
    component: () => import("@/views/service/MyRepairs.vue"),
    meta: { requiresAuth: true, title: "我的工单" },
  },
  {
    path: "/service/venue",
    component: () => import("@/views/service/VenueBooking.vue"),
    meta: { requiresAuth: true, title: "场地预约" },
  },
  {
    path: "/service/visitor",
    component: () => import("@/views/service/VisitorInvite.vue"),
    meta: { requiresAuth: true, title: "访客邀请" },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, _from, next) => {
  const store = useOwnerStore();
  // 无需鉴权的路由（如登录页）直接放行
  if (to.meta.requiresAuth === false) {
    next();
    return;
  }
  // 本地未登录 → 直接去登录页
  if (!store.isLoggedIn()) {
    resetSessionValidation();
    next("/login");
    return;
  }
  // 本地已登录，进入受保护路由前先校验会话有效性（不闪现内部页）
  const ok = await validateSession();
  if (!ok) {
    resetSessionValidation();
    next("/login");
    return;
  }
  next();
});

export default router;

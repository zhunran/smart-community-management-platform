import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from 'axios'
import NProgress from 'nprogress'

const AUTH_VERIFIED_KEY = '_auth_verified'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '仪表盘' } },
      // 小区管理
      { path: 'building', name: 'Building', component: () => import('@/views/building/BuildingList.vue'), meta: { title: '楼栋管理' } },
      { path: 'unit', name: 'Unit', component: () => import('@/views/unit/UnitList.vue'), meta: { title: '单元管理' } },
      { path: 'room', name: 'Room', component: () => import('@/views/room/RoomList.vue'), meta: { title: '房屋管理' } },
      // 业主管理
      { path: 'owner', name: 'Owner', component: () => import('@/views/owner/OwnerList.vue'), meta: { title: '业主管理' } },
      // 收费管理
      { path: 'fee-item', name: 'FeeItem', component: () => import('@/views/fee-item/FeeItemList.vue'), meta: { title: '费用项管理' } },
      { path: 'fee-standard', name: 'FeeStandard', component: () => import('@/views/fee-standard/FeeStandardList.vue'), meta: { title: '费用标准管理' } },
      { path: 'bill', name: 'Bill', component: () => import('@/views/bill/BillList.vue'), meta: { title: '账单管理' } },
      { path: 'payment', name: 'Payment', component: () => import('@/views/payment/PaymentList.vue'), meta: { title: '缴费记录' } },
      { path: 'report', name: 'Report', component: () => import('@/views/report/ReportView.vue'), meta: { title: '收费报表' } },
      // 报表统计
      { path: 'report/trend', name: 'FeeTrend', component: () => import('@/views/report/FeeTrendView.vue'), meta: { title: '缴费趋势' } },
      { path: 'audit', name: 'AuditLog', component: () => import('@/views/audit/AuditLogList.vue'), meta: { title: '操作审计' } },
      // 车位管理
      { path: 'parking', name: 'Parking', component: () => import('@/views/parking/ParkingList.vue'), meta: { title: '车位管理' } },
      { path: 'parking-warning', name: 'ParkingWarning', component: () => import('@/views/parking/ParkingWarning.vue'), meta: { title: '车位对账预警' } },
      // 公告管理
      { path: 'notice', name: 'Notice', component: () => import('@/views/notice/NoticeList.vue'), meta: { title: '公告管理' } },
      { path: 'sys-config', name: 'SysConfig', component: () => import('@/views/sysconfig/SysConfigList.vue'), meta: { title: '系统配置' } },
      // 社区管理
      { path: 'community/activity', name: 'CommunityActivity', component: () => import('@/views/community/ActivityList.vue'), meta: { title: '活动管理' } },
      { path: 'community/forum', name: 'CommunityForum', component: () => import('@/views/community/ForumList.vue'), meta: { title: '论坛管理' } },
      { path: 'community/comment', name: 'CommunityComment', component: () => import('@/views/community/CommentList.vue'), meta: { title: '评论管理' } },
      { path: 'community/vote', name: 'CommunityVote', component: () => import('@/views/community/VoteList.vue'), meta: { title: '投票管理' } },
      // 服务管理
      { path: 'service/repair', name: 'ServiceRepair', component: () => import('@/views/service/RepairList.vue'), meta: { title: '报修工单' } },
      { path: 'service/venue', name: 'ServiceVenue', component: () => import('@/views/service/VenueList.vue'), meta: { title: '场地管理' } },
      { path: 'service/visitor', name: 'ServiceVisitor', component: () => import('@/views/service/VisitorPassList.vue'), meta: { title: '访客记录' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()

  // 白名单路由直接放行
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  // 未登录（localStorage 无用户信息）→ 跳转登录页
  if (!userStore.isLoggedIn()) {
    next('/login')
    return
  }

  // 已登录但未验证 token 有效性 → 调用 refresh 接口验证
  if (!sessionStorage.getItem(AUTH_VERIFIED_KEY)) {
    try {
      const resp = await axios.post('/api/admin/auth/refresh', null, { withCredentials: true })
      if (resp.data?.code === 200) {
        sessionStorage.setItem(AUTH_VERIFIED_KEY, '1')
        next()
      } else {
        throw new Error('token expired')
      }
    } catch {
      userStore.clearUser()
      next('/login')
      return
    }
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router

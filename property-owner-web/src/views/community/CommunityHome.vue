<template>
  <div class="community-home">
    <div class="page-header">
      <span class="page-title">社区</span>
    </div>

    <van-tabs
      v-model:active="activeTab"
      sticky
      color="#3b82f6"
      line-width="24"
      line-height="3"
    >
      <van-tab title="活动" name="activity" />
      <van-tab title="论坛" name="forum" />
      <van-tab title="投票" name="vote" />
    </van-tabs>

    <!-- 活动列表 -->
    <div v-show="activeTab === 'activity'" class="tab-panel">
      <!-- 骨架屏 -->
      <div
        v-if="activityLoading && activities.length === 0"
        class="skeleton-list"
      >
        <van-skeleton
          v-for="i in 3"
          :key="i"
          title
          :row="2"
          style="margin-bottom: 12px"
        />
      </div>

      <van-list
        v-model:loading="activityLoading"
        :finished="activityFinished"
        :immediate-check="false"
        finished-text="没有更多活动了"
        @load="loadActivities"
      >
        <div
          v-for="a in activities"
          :key="a.id"
          class="activity-card"
          @click="router.push(`/community/activity/${a.id}`)"
        >
          <div class="activity-cover">
            <van-image
              v-if="a.coverImage"
              :src="a.coverImage"
              fit="cover"
              class="cover-img"
            />
            <div v-else class="cover-placeholder">
              <van-icon name="smile-o" size="28" color="#cbd5e1" />
            </div>
            <span
              class="activity-status"
              :style="{ background: statusColor(a.status) }"
              >{{ a.statusName }}</span
            >
          </div>
          <div class="activity-info">
            <div class="activity-title">{{ a.title }}</div>
            <div class="activity-meta">
              <van-icon name="clock-o" />
              <span>{{ fmt(a?.startTime) }}</span>
            </div>
            <div class="activity-meta">
              <van-icon name="location-o" />
              <span>{{ a?.location }}</span>
            </div>
            <div class="progress-row">
              <div class="progress-bar">
                <div
                  class="progress-inner"
                  :style="{ width: progressPercent(a) + '%' }"
                />
              </div>
              <span class="progress-text"
                >{{ a.signupCount }}/{{ a.maxParticipants }}</span
              >
            </div>
          </div>
        </div>
      </van-list>
      <van-empty
        v-if="!activityLoading && activities.length === 0"
        description="暂无活动"
      />
    </div>

    <!-- 论坛列表 -->
    <div v-show="activeTab === 'forum'" class="tab-panel">
      <div class="forum-toolbar">
        <div class="category-scroll">
          <span
            v-for="c in categories"
            :key="c.value"
            class="category-chip"
            :class="{ active: forumCategory === c.value }"
            @click="switchCategory(c.value)"
          >
            {{ c.label }}
          </span>
        </div>
        <van-button
          type="primary"
          size="small"
          round
          icon="edit"
          @click="router.push('/community/forum/create')"
        >
          发帖
        </van-button>
      </div>

      <div v-if="forumLoading && posts.length === 0" class="skeleton-list">
        <van-skeleton
          v-for="i in 3"
          :key="i"
          title
          :row="2"
          style="margin-bottom: 12px"
        />
      </div>

      <van-list
        v-model:loading="forumLoading"
        :finished="forumFinished"
        :immediate-check="false"
        finished-text="没有更多帖子了"
        @load="loadPosts"
      >
        <div
          v-for="p in posts"
          :key="p.id"
          class="post-card"
          @click="router.push(`/community/forum/${p.id}`)"
        >
          <div class="post-main">
            <div class="post-title">
              <van-tag
                v-if="p.isPinned === 1"
                type="danger"
                style="margin-right: 6px"
                >置顶</van-tag
              >
              <span class="post-title-text">{{ p.title }}</span>
            </div>
            <div class="post-meta">
              <van-tag plain type="primary">{{ p.categoryName }}</van-tag>
              <span class="post-stats">
                <van-icon name="eye-o" />{{ p.viewCount }}
                <van-icon name="good-job-o" style="margin-left: 10px" />{{
                  p.likeCount
                }}
                <van-icon name="chat-o" style="margin-left: 10px" />{{
                  p.commentCount
                }}
              </span>
            </div>
          </div>
          <div v-if="firstImage(p.images)" class="post-thumb">
            <van-image
              :src="firstImage(p.images)"
              fit="cover"
              width="64"
              height="64"
              radius="6"
            />
          </div>
        </div>
      </van-list>
      <van-empty
        v-if="!forumLoading && posts.length === 0"
        description="暂无帖子"
      />
    </div>

    <!-- 投票列表 -->
    <div v-show="activeTab === 'vote'" class="tab-panel">
      <div v-if="voteLoading && votes.length === 0" class="skeleton-list">
        <van-skeleton
          v-for="i in 3"
          :key="i"
          title
          :row="2"
          style="margin-bottom: 12px"
        />
      </div>

      <van-list
        v-model:loading="voteLoading"
        :finished="voteFinished"
        :immediate-check="false"
        finished-text="没有更多投票了"
        @load="loadVotes"
      >
        <div
          v-for="v in votes"
          :key="v.id"
          class="vote-card"
          @click="router.push(`/community/vote/${v.id}`)"
        >
          <div class="vote-head">
            <span class="vote-title">{{ v.title }}</span>
            <van-tag
              :color="voteStatusColor(v.status)"
              text-color="#fff"
              round
              >{{ v.statusName }}</van-tag
            >
          </div>
          <div class="vote-meta">
            <van-tag plain type="primary">{{ v.voteTypeName }}</van-tag>
            <span class="vote-time">
              <van-icon name="clock-o" />{{ fmt(v?.startTime) }} ~
              {{ fmt(v?.endTime) }}
            </span>
          </div>
        </div>
      </van-list>
      <van-empty
        v-if="!voteLoading && votes.length === 0"
        description="暂无投票"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import {
  pageActivity,
  pagePost,
  pageVote,
  ACTIVITY_TYPE_MAP,
  POST_CATEGORY_MAP,
} from "@/api/community";
import type { CommunityActivityVO, ForumPostVO, VoteVO } from "@/api/community";

const router = useRouter();

function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function statusColor(s: number) {
  const m: Record<number, string> = {
    1: "#3b82f6",
    2: "#f59e0b",
    3: "#22c55e",
    4: "#94a3b8",
  };
  return m[s] || "#94a3b8";
}
function voteStatusColor(s: number) {
  const m: Record<number, string> = {
    0: "#94a3b8",
    1: "#3b82f6",
    2: "#22c55e",
  };
  return m[s] || "#94a3b8";
}
function progressPercent(a: CommunityActivityVO) {
  if (!a.maxParticipants) return 0;
  return Math.min(100, Math.round((a.signupCount / a.maxParticipants) * 100));
}
function firstImage(images?: string) {
  if (!images) return "";
  return (
    images
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)[0] || ""
  );
}

const activeTab = ref("activity");

// 活动
const activities = ref<CommunityActivityVO[]>([]);
const activityLoading = ref(false);
const activityFinished = ref(false);
const activityPage = ref(1);

async function loadActivities() {
  try {
    const res = await pageActivity({ current: activityPage.value, size: 10 });
    const records = (res?.data?.records || []).filter(Boolean);
    activities.value.push(...records);
    activityFinished.value =
      activities.value.length >= Number(res?.data?.total || 0);
    activityPage.value += 1;
  } catch {
    activityFinished.value = true;
  } finally {
    activityLoading.value = false;
  }
}

// 论坛
const posts = ref<ForumPostVO[]>([]);
const forumLoading = ref(false);
const forumFinished = ref(false);
const forumPage = ref(1);
const forumCategory = ref<number | undefined>(undefined);

const categories = [
  { value: undefined, label: "全部" },
  ...Object.keys(POST_CATEGORY_MAP).map((k) => ({
    value: Number(k),
    label: POST_CATEGORY_MAP[Number(k)],
  })),
];

async function loadPosts() {
  try {
    const res = await pagePost({
      current: forumPage.value,
      size: 10,
      category: forumCategory.value,
    });
    const records = (res?.data?.records || []).filter(Boolean);
    posts.value.push(...records);
    forumFinished.value = posts.value.length >= Number(res?.data?.total || 0);
    forumPage.value += 1;
  } catch {
    forumFinished.value = true;
  } finally {
    forumLoading.value = false;
  }
}

function switchCategory(v: number | undefined) {
  forumCategory.value = v;
  posts.value = [];
  forumPage.value = 1;
  forumFinished.value = false;
  forumLoading.value = true;
  loadPosts();
}

// 投票
const votes = ref<VoteVO[]>([]);
const voteLoading = ref(false);
const voteFinished = ref(false);
const votePage = ref(1);

async function loadVotes() {
  try {
    const res = await pageVote({ current: votePage.value, size: 10 });
    const records = (res?.data?.records || []).filter(Boolean);
    votes.value.push(...records);
    voteFinished.value = votes.value.length >= Number(res?.data?.total || 0);
    votePage.value += 1;
  } catch {
    voteFinished.value = true;
  } finally {
    voteLoading.value = false;
  }
}

onMounted(() => {
  loadActivities();
});

watch(activeTab, (v) => {
  if (v === "forum" && posts.value.length === 0 && !forumFinished.value) {
    loadPosts();
  }
  if (v === "vote" && votes.value.length === 0 && !voteFinished.value) {
    loadVotes();
  }
});
</script>

<style scoped>
.community-home {
  min-height: 100%;
  background: #f5f7fa;
}
.page-header {
  padding: 14px 16px 8px;
  background: #fff;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}
.tab-panel {
  padding: 12px 12px 20px;
}
.skeleton-list {
  padding: 4px;
}

/* 活动卡片 */
.activity-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s;
  cursor: pointer;
}
.activity-card:active {
  transform: scale(0.99);
}
.activity-cover {
  position: relative;
  height: 150px;
  background: #eef2f7;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #f5f3ff 100%);
}
.activity-status {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
}
.activity-info {
  padding: 12px;
}
.activity-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}
.activity-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.progress-bar {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}
.progress-inner {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 3px;
  transition: width 0.4s ease;
}
.progress-text {
  font-size: 12px;
  color: #3b82f6;
  flex-shrink: 0;
}

/* 论坛 */
.forum-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.category-scroll {
  flex: 1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
}
.category-scroll::-webkit-scrollbar {
  display: none;
}
.category-chip {
  flex-shrink: 0;
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 16px;
  background: #fff;
  color: #475569;
  transition: all 0.2s;
}
.category-chip.active {
  background: #3b82f6;
  color: #fff;
}
.post-card {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  transition: transform 0.2s;
  cursor: pointer;
}
.post-card:active {
  transform: scale(0.99);
}
.post-main {
  flex: 1;
  min-width: 0;
}
.post-title {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.post-title-text {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.post-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.post-thumb {
  flex-shrink: 0;
}

/* 投票 */
.vote-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  transition: transform 0.2s;
  cursor: pointer;
}
.vote-card:active {
  transform: scale(0.99);
}
.vote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.vote-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
.vote-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.vote-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}
</style>

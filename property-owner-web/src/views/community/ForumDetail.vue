<template>
  <div class="forum-detail">
    <van-nav-bar
      title="帖子详情"
      left-text="返回"
      left-arrow
      @click-left="router.back()"
    />

    <div v-if="loading" class="loading-box">
      <van-skeleton title :row="6" />
    </div>

    <template v-else-if="detail">
      <div class="post-body">
        <div class="post-head">
          <div class="post-title">
            <van-tag
              v-if="detail.isPinned === 1"
              type="danger"
              style="margin-right: 6px"
              >置顶</van-tag
            >
            {{ detail.title }}
          </div>
          <div class="post-sub">
            <van-tag plain type="primary">{{ detail.categoryName }}</van-tag>
            <span class="post-time">{{ fmt(detail.createTime) }}</span>
          </div>
        </div>

        <div class="post-content">{{ detail.content }}</div>

        <div v-if="imageList.length" class="img-grid">
          <van-image
            v-for="(img, i) in imageList"
            :key="i"
            :src="img"
            fit="cover"
            class="post-img"
            @click="previewImages(i)"
          />
        </div>

        <div class="post-actions">
          <div class="action" @click="handleLike">
            <van-icon
              :name="detail.isLiked ? 'like' : 'like-o'"
              :color="detail.isLiked ? '#ef4444' : '#64748b'"
              :class="{ 'like-anim': likeAnim }"
            />
            <span :style="{ color: detail.isLiked ? '#ef4444' : '#64748b' }">{{
              detail.likeCount
            }}</span>
          </div>
          <div class="action">
            <van-icon name="eye-o" color="#64748b" />
            <span>{{ detail.viewCount }}</span>
          </div>
          <div class="action">
            <van-icon name="chat-o" color="#64748b" />
            <span>{{ detail.commentCount }}</span>
          </div>
        </div>
      </div>

      <!-- 评论 -->
      <div class="comment-section">
        <div class="section-title">评论 {{ detail.comments?.length || 0 }}</div>

        <div v-if="!detail.comments?.length" class="empty-comment">
          <van-empty description="还没有评论，快来抢沙发" />
        </div>

        <div v-for="c in detail.comments" :key="c.id" class="comment">
          <div class="comment-main">
            <div class="comment-avatar">{{ avatarChar(c) }}</div>
            <div class="comment-body">
              <div class="comment-content">{{ c.content }}</div>
              <div class="comment-footer">
                <span class="comment-time">{{ fmt(c.createTime) }}</span>
                <span class="comment-reply" @click="replyTo(c)">回复</span>
              </div>
              <!-- 子评论 -->
              <div v-if="c.children?.length" class="sub-comments">
                <div
                  v-for="sub in c.children"
                  :key="sub.id"
                  class="sub-comment"
                >
                  <span class="sub-name">{{ avatarChar(sub) }}：</span>
                  <span>{{ sub.content }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 底部评论输入 -->
    <div class="footer-bar" v-if="detail">
      <van-field
        v-model="commentText"
        :placeholder="replyPlaceholder"
        center
        :border="false"
        class="comment-input"
        @keyup.enter="submitComment"
      >
        <template #button>
          <van-button
            size="small"
            type="primary"
            :loading="commentLoading"
            @click="submitComment"
            >发送</van-button
          >
        </template>
      </van-field>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { showToast, showImagePreview } from "vant";
import { getPost, like, createComment } from "@/api/community";
import type { ForumPostDetailVO, ForumCommentVO } from "@/api/community";

const route = useRoute();
const router = useRouter();
const id = String(route.params.id);

function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function avatarChar(c: ForumCommentVO) {
  return "业";
}

const loading = ref(false);
const detail = ref<ForumPostDetailVO>();
const commentText = ref("");
const commentLoading = ref(false);
const likeAnim = ref(false);
const replyTarget = ref<ForumCommentVO>();

const imageList = computed(() => {
  const img = detail.value?.images;
  if (!img) return [];
  return img
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
});

const replyPlaceholder = computed(() => {
  return replyTarget.value
    ? `回复：${replyTarget.value.content.slice(0, 20)}`
    : "写下你的评论…";
});

function previewImages(i: number) {
  showImagePreview({ images: imageList.value, startPosition: i });
}

async function load() {
  loading.value = true;
  try {
    const res = await getPost(id);
    detail.value = res.data;
  } finally {
    loading.value = false;
  }
}

async function handleLike() {
  if (!detail.value) return;
  const before = detail.value.isLiked;
  // 乐观更新
  detail.value.isLiked = !before;
  detail.value.likeCount += before ? -1 : 1;
  likeAnim.value = true;
  setTimeout(() => (likeAnim.value = false), 300);
  try {
    await like(detail.value.id, 1);
  } catch {
    // 失败回滚
    detail.value.isLiked = before;
    detail.value.likeCount += before ? 1 : -1;
  }
}

function replyTo(c: ForumCommentVO) {
  replyTarget.value = c;
}

async function submitComment() {
  const content = commentText.value.trim();
  if (!content) return;
  commentLoading.value = true;
  try {
    await createComment(id, {
      content,
      parentId: replyTarget.value?.id,
      replyTo: replyTarget.value?.ownerId,
    });
    commentText.value = "";
    replyTarget.value = undefined;
    showToast("评论成功");
    await load();
  } finally {
    commentLoading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.forum-detail {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 70px;
}
.loading-box {
  padding: 20px;
}
.post-body {
  background: #fff;
  padding: 16px;
  margin-bottom: 12px;
}
.post-head {
  margin-bottom: 14px;
}
.post-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
  display: flex;
  align-items: center;
}
.post-sub {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.post-time {
  font-size: 12px;
  color: #94a3b8;
}
.post-content {
  font-size: 15px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.img-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 14px;
}
.post-img {
  width: 100%;
  height: 100px;
  border-radius: 8px;
}
.post-actions {
  display: flex;
  gap: 28px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}
.action {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  cursor: pointer;
}
.like-anim {
  animation: like-pop 0.3s ease;
}
@keyframes like-pop {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.4);
  }
  100% {
    transform: scale(1);
  }
}

.comment-section {
  background: #fff;
  padding: 16px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}
.empty-comment {
  padding: 20px 0;
}
.comment {
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}
.comment-main {
  display: flex;
  gap: 10px;
}
.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  word-break: break-word;
}
.comment-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
}
.comment-time {
  font-size: 12px;
  color: #94a3b8;
}
.comment-reply {
  font-size: 12px;
  color: #3b82f6;
}
.sub-comments {
  margin-top: 10px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 10px;
}
.sub-comment {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 4px;
}
.sub-name {
  color: #3b82f6;
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 6px 8px calc(6px + env(safe-area-inset-bottom));
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
}
.comment-input {
  background: #f1f5f9;
  border-radius: 20px;
}
</style>

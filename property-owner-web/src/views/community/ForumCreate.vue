<template>
  <div class="forum-create">
    <van-nav-bar title="发布帖子" left-text="返回" left-arrow @click-left="router.back()" />

    <div class="form-body">
      <van-field
        v-model="form.title"
        label="标题"
        placeholder="请输入帖子标题（最多100字）"
        maxlength="100"
        show-word-limit
        :border="true"
      />

      <div class="field-label">分类</div>
      <div class="category-grid">
        <span
          v-for="c in categories"
          :key="c.value"
          class="category-item"
          :class="{ active: form.category === c.value }"
          @click="form.category = c.value"
        >
          {{ c.label }}
        </span>
      </div>

      <van-field
        v-model="form.content"
        label="内容"
        type="textarea"
        rows="6"
        autosize
        placeholder="说说你的想法…"
        maxlength="2000"
        show-word-limit
      />

      <van-field
        v-model="form.images"
        label="图片URL"
        placeholder="多个图片URL用英文逗号分隔（可留空）"
      />
      <div class="field-tip">说明：暂支持填写图片链接，多个链接用逗号分隔。</div>
    </div>

    <div class="footer-bar">
      <van-button type="primary" block round :loading="submitting" @click="submit">发布</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { createPost, POST_CATEGORY_MAP } from '@/api/community'

const router = useRouter()

const categories = Object.keys(POST_CATEGORY_MAP).map((k) => ({
  value: Number(k),
  label: POST_CATEGORY_MAP[Number(k)],
}))

const form = reactive({
  title: '',
  content: '',
  images: '',
  category: 1,
})
const submitting = ref(false)

async function submit() {
  if (!form.title.trim()) {
    showToast('请输入标题')
    return
  }
  if (!form.content.trim()) {
    showToast('请输入内容')
    return
  }
  submitting.value = true
  try {
    await createPost({
      title: form.title.trim(),
      content: form.content.trim(),
      images: form.images.trim() || undefined,
      category: form.category,
    })
    showSuccessToast('发帖成功')
    router.back()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.forum-create {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 70px;
}
.form-body {
  padding: 12px;
}
.field-label {
  font-size: 13px;
  color: #64748b;
  margin: 14px 4px 8px;
}
.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 4px;
}
.category-item {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 16px;
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.category-item.active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}
.field-tip {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 16px;
}
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
}
</style>

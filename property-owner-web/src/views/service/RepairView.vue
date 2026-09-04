<template>
  <div class="repair-page">
    <van-nav-bar
      title="我要报修"
      left-text="返回"
      left-arrow
      @click-left="router.back()"
    />

    <div class="section-card">
      <div class="section-title">选择报修类型</div>
      <div class="category-grid">
        <div
          v-for="c in categories"
          :key="c.value"
          class="category-item"
          :class="{ active: form.category === c.value }"
          @click="form.category = c.value"
        >
          <van-icon
            :name="c.icon"
            size="26"
            :color="form.category === c.value ? '#3b82f6' : '#64748b'"
          />
          <span>{{ c.label }}</span>
        </div>
      </div>
    </div>

    <div class="section-card">
      <div class="section-title">报修信息</div>
      <van-field
        v-model="form.title"
        label="问题简述"
        placeholder="例如：厨房水管漏水"
        maxlength="50"
        show-word-limit
      />
      <van-field
        v-model="form.description"
        label="问题详情"
        type="textarea"
        rows="4"
        autosize
        placeholder="请描述具体位置和情况…"
        maxlength="500"
        show-word-limit
      />
      <van-field
        v-model="form.images"
        label="照片URL"
        placeholder="多个图片URL用逗号分隔（可留空）"
      />

      <van-field
        v-model="roomName"
        label="报修房屋"
        placeholder="请选择房屋"
        readonly
        is-link
        @click="showRoomPicker = true"
      />

      <div class="field-label">紧急程度</div>
      <div class="urgency-row">
        <span
          v-for="u in urgencies"
          :key="u.value"
          class="urgency-item"
          :class="{ active: form.urgency === u.value }"
          :style="
            form.urgency === u.value
              ? { background: u.color, borderColor: u.color }
              : {}
          "
          @click="form.urgency = u.value"
        >
          {{ u.label }}
        </span>
      </div>
    </div>

    <div class="submit-wrap">
      <van-button
        type="primary"
        block
        round
        :loading="submitting"
        @click="submit"
        >提交报修</van-button
      >
    </div>

    <!-- 房屋选择 -->
    <van-popup v-model:show="showRoomPicker" position="bottom" round>
      <van-picker
        :columns="roomColumns"
        title="选择房屋"
        @confirm="onRoomConfirm"
        @cancel="showRoomPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { showSuccessToast, showToast } from "vant";
import { createRepair, REPAIR_CATEGORY_MAP } from "@/api/service";
import { getMyRooms } from "@/api/profile";
import type { OwnerRoomVO } from "@/api/profile";

const router = useRouter();

const categories = [
  { value: 1, label: "水电", icon: "flash" },
  { value: 2, label: "门窗", icon: "home-o" },
  { value: 3, label: "电梯", icon: "ascending" },
  { value: 4, label: "公共设施", icon: "cluster-o" },
  { value: 5, label: "其他", icon: "more-o" },
];

const urgencies = [
  { value: 1, label: "普通", color: "#94a3b8" },
  { value: 2, label: "紧急", color: "#f59e0b" },
  { value: 3, label: "特急", color: "#ef4444" },
];

const form = reactive({
  title: "",
  description: "",
  images: "",
  category: 1,
  urgency: 1,
  roomId: 0,
});
const submitting = ref(false);

// 房屋选择
const rooms = ref<OwnerRoomVO[]>([]);
const showRoomPicker = ref(false);
const roomName = ref("");
const roomColumns = computed(() =>
  rooms.value.map((r) => ({
    text: r.buildingName + " " + r.roomName,
    value: r.roomId,
  })),
);

async function loadRooms() {
  const res = await getMyRooms();
  rooms.value = res.data || [];
  if (rooms.value.length === 1) {
    form.roomId = rooms.value[0].roomId;
    roomName.value =
      rooms.value[0].buildingName + " " + rooms.value[0].roomName;
  }
}

function onRoomConfirm({
  selectedOptions,
}: {
  selectedOptions: Array<{ value: number; text: string }>;
}) {
  if (selectedOptions.length) {
    form.roomId = selectedOptions[0].value;
    roomName.value = selectedOptions[0].text;
  }
  showRoomPicker.value = false;
}

async function submit() {
  if (!form.title.trim()) {
    showToast("请输入问题简述");
    return;
  }
  if (!form.description.trim()) {
    showToast("请输入问题详情");
    return;
  }
  if (!form.roomId) {
    showToast("请选择报修房屋");
    return;
  }
  submitting.value = true;
  try {
    await createRepair({
      title: form.title.trim(),
      description: form.description.trim(),
      images: form.images.trim() || undefined,
      category: form.category,
      urgency: form.urgency,
      roomId: form.roomId,
    });
    showSuccessToast("报修提交成功");
    router.push("/service/mine");
  } finally {
    submitting.value = false;
  }
}

onMounted(loadRooms);
</script>

<style scoped>
.repair-page {
  min-height: 100%;
  background: #f5f7fa;
  padding-bottom: 30px;
}
.section-card {
  background: #fff;
  border-radius: 12px;
  margin: 12px;
  padding: 14px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 4px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
}
.category-item span {
  font-size: 12px;
  color: #475569;
}
.category-item.active {
  background: #eff6ff;
  border-color: #3b82f6;
}
.field-label {
  font-size: 13px;
  color: #64748b;
  margin: 14px 0 8px;
}
.urgency-row {
  display: flex;
  gap: 10px;
}
.urgency-item {
  font-size: 13px;
  padding: 7px 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  transition: all 0.2s;
  cursor: pointer;
}
.urgency-item.active {
  color: #fff;
}
.submit-wrap {
  padding: 8px 16px;
}
</style>

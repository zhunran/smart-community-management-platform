<template>
  <div class="page">
    <van-nav-bar title="我的房屋" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="room in rooms"
          :key="room.id"
          :title="room.roomName || room.roomCode"
          :label="room.buildingName"
        >
          <template #extra>
            <van-tag :type="room.isPrimary === 1 ? 'danger' : 'primary'" size="medium">
              {{ room.isPrimary === 1 ? '主要' : '其他' }}
            </van-tag>
            <span class="relation">{{ room.relationType === 1 ? '业主' : room.relationType === 2 ? '家属' : '租客' }}</span>
          </template>
        </van-cell>
        <van-empty v-if="rooms.length === 0" description="暂无房屋信息" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRooms } from '@/api/profile'
import type { OwnerRoomVO } from '@/api/profile'

const router = useRouter()
const loading = ref(false)
const rooms = ref<OwnerRoomVO[]>([])
function goBack() { router.back() }

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyRooms()
    rooms.value = res.data as OwnerRoomVO[]
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.relation {
  font-size: 12px;
  color: #67c23a;
  margin-left: 8px;
}
</style>

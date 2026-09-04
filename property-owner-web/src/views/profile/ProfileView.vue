<template>
  <div class="page">
    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-if="profile">
        <!-- 头像区 -->
        <div class="profile-header">
          <div class="avatar-circle">
            <span class="avatar-text">{{ profile.ownerName?.charAt(0) || '业' }}</span>
          </div>
          <div class="profile-name">{{ profile.ownerName }}</div>
          <div class="profile-phone">{{ profile.phone }}</div>
          <van-button
            v-if="!editing"
            class="edit-btn"
            size="small"
            plain
            type="primary"
            icon="edit"
            @click="startEdit"
          >
            编辑
          </van-button>
        </div>

        <!-- 基本信息 -->
        <div class="section-title">基本信息</div>

        <!-- 编辑模式 -->
        <template v-if="editing">
          <van-form @submit="handleSave">
            <van-cell-group inset>
              <van-field
                v-model="form.ownerName"
                label="姓名"
                placeholder="请输入姓名"
                :rules="[{ required: true, message: '请输入姓名' }]"
              />
              <van-field
                v-model="form.phone"
                label="手机号"
                placeholder="请输入手机号"
                type="tel"
                maxlength="11"
                :rules="[
                  { required: true, message: '请输入手机号' },
                  { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
                ]"
              />
              <van-field
                v-model="form.genderLabel"
                readonly
                is-link
                label="性别"
                placeholder="请选择性别"
                @click="showGenderPicker = true"
              />
              <van-field
                v-model="form.birthday"
                readonly
                is-link
                label="出生日期"
                placeholder="请选择日期"
                @click="showDatePicker = true"
              />
              <van-field
                v-model="form.email"
                label="邮箱"
                placeholder="请输入邮箱"
                type="email"
                :rules="[{ pattern: /^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确' }]"
              />
            </van-cell-group>

            <!-- 紧急联系人 -->
            <div class="section-title">紧急联系人</div>
            <van-cell-group inset>
              <van-field
                v-model="form.emergencyContact"
                label="联系人"
                placeholder="请输入紧急联系人"
              />
              <van-field
                v-model="form.emergencyPhone"
                label="联系电话"
                placeholder="请输入紧急联系电话"
                type="tel"
                maxlength="11"
                :rules="[{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确' }]"
              />
            </van-cell-group>

            <!-- 不可编辑信息 -->
            <div class="section-title">账户信息</div>
            <van-cell-group inset>
              <van-cell title="证件号" :value="profile.idCardNo" />
              <van-cell title="业主类型">
                <template #value>
                  <span class="cell-value">{{ profile.ownerType === 1 ? '个人' : profile.ownerType === 2 ? '公司' : '共有' }}</span>
                </template>
              </van-cell>
              <van-cell title="注册时间" :value="profile.registerTime" />
              <van-cell title="最近登录">
                <template #value>
                  <span :class="profile.lastLoginTime ? 'cell-value' : 'cell-value-empty'">{{ profile.lastLoginTime || '首次登录' }}</span>
                </template>
              </van-cell>
            </van-cell-group>

            <div class="btn-group">
              <van-button round block type="primary" native-type="submit" :loading="saving">保存</van-button>
              <van-button round block plain type="default" @click="cancelEdit">取消</van-button>
            </div>
          </van-form>
        </template>

        <!-- 查看模式 -->
        <template v-else>
          <van-cell-group inset>
            <van-cell title="姓名" :value="profile.ownerName" />
            <van-cell title="手机号" :value="profile.phone" />
            <van-cell title="性别">
              <template #value>
                <span class="cell-value">{{ genderLabel(profile.gender) }}</span>
              </template>
            </van-cell>
            <van-cell title="出生日期">
              <template #value>
                <span :class="profile.birthday ? 'cell-value' : 'cell-value-empty'">{{ profile.birthday || '未设置' }}</span>
              </template>
            </van-cell>
            <van-cell title="邮箱">
              <template #value>
                <span :class="profile.email ? 'cell-value' : 'cell-value-empty'">{{ profile.email || '未设置' }}</span>
              </template>
            </van-cell>
          </van-cell-group>

          <!-- 紧急联系人 -->
          <div class="section-title">紧急联系人</div>
          <van-cell-group inset>
            <van-cell title="联系人">
              <template #value>
                <span :class="profile.emergencyContact ? 'cell-value' : 'cell-value-empty'">{{ profile.emergencyContact || '未设置' }}</span>
              </template>
            </van-cell>
            <van-cell title="联系电话">
              <template #value>
                <span :class="profile.emergencyPhone ? 'cell-value' : 'cell-value-empty'">{{ profile.emergencyPhone || '未设置' }}</span>
              </template>
            </van-cell>
          </van-cell-group>

          <!-- 账户信息 -->
          <div class="section-title">账户信息</div>
          <van-cell-group inset>
            <van-cell title="证件号" :value="profile.idCardNo" />
            <van-cell title="业主类型">
              <template #value>
                <span class="cell-value">{{ profile.ownerType === 1 ? '个人' : profile.ownerType === 2 ? '公司' : '共有' }}</span>
              </template>
            </van-cell>
            <van-cell title="注册时间" :value="profile.registerTime" />
            <van-cell title="最近登录">
              <template #value>
                <span :class="profile.lastLoginTime ? 'cell-value' : 'cell-value-empty'">{{ profile.lastLoginTime || '首次登录' }}</span>
              </template>
            </van-cell>
          </van-cell-group>
        </template>
      </template>
    </div>

    <!-- 性别选择器 -->
    <van-popup v-model:show="showGenderPicker" position="bottom" round>
      <van-picker
        :columns="genderOptions"
        @confirm="onGenderConfirm"
        @cancel="showGenderPicker = false"
      />
    </van-popup>

    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        title="选择出生日期"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getProfile, updateProfile } from '@/api/profile'
import type { OwnerDetailVO } from '@/api/profile'
import { useOwnerStore } from '@/stores/owner'

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const profile = ref<OwnerDetailVO>()
const ownerStore = useOwnerStore()

const showGenderPicker = ref(false)
const showDatePicker = ref(false)

const genderOptions = [
  { text: '男', value: 1 },
  { text: '女', value: 2 },
  { text: '未知', value: 0 }
]

const minDate = new Date(1900, 0, 1)
const maxDate = new Date()

const form = reactive({
  ownerName: '',
  phone: '',
  gender: undefined as number | undefined,
  genderLabel: '',
  birthday: '',
  email: '',
  emergencyContact: '',
  emergencyPhone: ''
})

function genderLabel(val: number): string {
  if (val === 1) return '男'
  if (val === 2) return '女'
  return '未知'
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data as OwnerDetailVO
  } finally { loading.value = false }
})

function startEdit() {
  if (!profile.value) return
  form.ownerName = profile.value.ownerName
  form.phone = profile.value.phone
  form.gender = profile.value.gender
  form.genderLabel = genderLabel(profile.value.gender)
  form.birthday = profile.value.birthday || ''
  form.email = profile.value.email || ''
  form.emergencyContact = profile.value.emergencyContact || ''
  form.emergencyPhone = profile.value.emergencyPhone || ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

function onGenderConfirm({ selectedOptions }: any) {
  const val = selectedOptions[0]?.value as number
  form.gender = val
  form.genderLabel = genderLabel(val)
  showGenderPicker.value = false
}

function onDateConfirm({ selectedValues }: any) {
  form.birthday = selectedValues.join('-')
  showDatePicker.value = false
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      ownerName: form.ownerName,
      phone: form.phone,
      gender: form.gender,
      birthday: form.birthday || undefined,
      email: form.email || undefined,
      emergencyContact: form.emergencyContact || undefined,
      emergencyPhone: form.emergencyPhone || undefined
    })
    await ownerStore.updateProfile({
      ownerName: form.ownerName,
      phone: form.phone
    })
    // 刷新页面数据
    const res = await getProfile()
    profile.value = res.data as OwnerDetailVO
    editing.value = false
  } finally { saving.value = false }
}
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0 20px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 60px;
}

/* 头像区 */
.profile-header {
  position: relative;
  text-align: center;
  padding: 28px 0 24px;
  background: #fff;
  margin-bottom: 16px;
}

.avatar-circle {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.avatar-text {
  font-size: 28px;
  font-weight: 600;
  color: #fff;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.profile-phone {
  font-size: 13px;
  color: #94a3b8;
}

.edit-btn {
  position: absolute;
  top: 16px;
  right: 16px;
}

/* 分区标题 */
.section-title {
  font-size: 13px;
  color: #94a3b8;
  padding: 0 20px;
  margin-bottom: 8px;
  margin-top: 16px;
}

.section-title:first-of-type {
  margin-top: 0;
}

/* 值样式 */
.cell-value {
  color: #475569;
}

.cell-value-empty {
  color: #94a3b8;
}

/* 按钮组 */
.btn-group {
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
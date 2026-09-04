<template>
  <div class="owner-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>业主管理</span>
          <div>
            <el-button @click="handleDownloadTemplate">下载模板</el-button>
            <el-button @click="handleImport">导入Excel</el-button>
            <el-button @click="handleExport">导出Excel</el-button>
            <el-button type="primary" @click="handleAdd">新增业主</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="业主姓名">
          <el-input v-model="query.ownerName" placeholder="模糊搜索" clearable style="width:130px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="模糊搜索" clearable style="width:130px" />
        </el-form-item>
        <el-form-item label="房号">
          <el-input v-model="query.roomCode" placeholder="模糊搜索" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="业主类型">
          <el-select v-model="query.ownerType" placeholder="全部" clearable style="width:110px">
            <el-option label="个人" :value="1" />
            <el-option label="公司" :value="2" />
            <el-option label="共有" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
            <el-option label="冻结" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="ownerName" label="业主姓名" width="100" fixed />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="证件" min-width="200">
          <template #default="{ row }">
            {{ idCardTypeMap[row.idCardType] ?? '未知' }} {{ row.idCardNo }}
          </template>
        </el-table-column>
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">{{ ['未知', '男', '女'][row.gender] ?? '未知' }}</template>
        </el-table-column>
        <el-table-column label="业主类型" width="80" align="center">
          <template #default="{ row }">{{ ['', '个人', '公司', '共有'][row.ownerType] ?? '' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ ['禁用', '正常', '冻结'][row.status] ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">修改</el-button>
            <el-button size="small" @click="handleBindRoom(row)">绑定房屋</el-button>
            <el-button size="small" @click="handleViewRooms(row)">房屋</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current" v-model:page-size="query.size"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" @change="fetchData"
        />
      </div>
    </el-card>

    <!-- 业主编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改业主' : '新增业主'" width="680px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业主姓名" prop="ownerName">
              <el-input v-model="form.ownerName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件类型" prop="idCardType">
              <el-select v-model="form.idCardType" placeholder="请选择" style="width:100%">
                <el-option label="身份证" :value="1" />
                <el-option label="护照" :value="2" />
                <el-option label="港澳台证" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号码" prop="idCardNo">
              <el-input v-model="form.idCardNo" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width:100%">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
                <el-option label="未知" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthday" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业主类型" prop="ownerType">
              <el-select v-model="form.ownerType" placeholder="请选择" style="width:100%">
                <el-option label="个人" :value="1" />
                <el-option label="公司" :value="2" />
                <el-option label="共有" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择" style="width:100%">
                <el-option label="正常" :value="1" />
                <el-option label="禁用" :value="0" />
                <el-option label="冻结" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="电子邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContact" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系电话">
              <el-input v-model="form.emergencyPhone" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定房屋弹窗 -->
    <el-dialog v-model="bindDialogVisible" title="绑定房屋" width="500px" :close-on-click-modal="false">
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
        <el-form-item label="业主">
          <el-input :model-value="bindOwnerName" disabled />
        </el-form-item>
        <el-form-item label="房屋" prop="roomId">
          <el-select v-model="bindForm.roomId" placeholder="请选择房屋" filterable style="width:100%">
            <el-option v-for="r in allRooms" :key="r.id" :label="`${r.buildingName} ${r.roomName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select v-model="bindForm.relationType" placeholder="请选择" style="width:100%">
            <el-option label="业主" :value="1" />
            <el-option label="家属" :value="2" />
            <el-option label="租客" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否主要">
          <el-radio-group v-model="bindForm.isPrimary">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="入住时间">
          <el-date-picker v-model="bindForm.moveInTime" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindLoading" @click="handleBindSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入弹窗 -->
    <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange" />

    <!-- 关联房屋列表弹窗 -->
    <el-dialog v-model="roomListVisible" :title="`${roomListOwnerName} - 关联房屋`" width="650px">
      <el-table :data="ownerRooms" stripe border size="small" style="width:100%">
        <el-table-column prop="buildingName" label="楼栋" width="120" />
        <el-table-column prop="unitName" label="单元" width="100" />
        <el-table-column prop="roomCode" label="房号" width="100" />
        <el-table-column prop="roomName" label="房屋名称" min-width="140" />
        <el-table-column label="关系" width="70" align="center"><template #default="{row}">{{ ['','业主','家属','租客'][row.relationType]||'' }}</template></el-table-column>
        <el-table-column label="主要" width="60" align="center"><template #default="{row}"><el-tag :type="row.isPrimary===1?'success':'info'" size="small">{{ row.isPrimary===1?'是':'否' }}</el-tag></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageOwner, createOwner, updateOwner, deleteOwner, exportOwnersExcel, downloadOwnerTemplate, importOwnersExcel, bindOwnerRoom, listOwnerRooms } from '@/api/owner'
import { listAllBuildings } from '@/api/building'
import { pageRoom } from '@/api/room'
import type { OwnerVO, OwnerCreateRequest, OwnerUpdateRequest } from '@/api/owner'
import type { OwnerRoomVO } from '@/api/owner'
import type { BuildingVO } from '@/api/building'
import type { RoomVO } from '@/api/room'

const idCardTypeMap: Record<number, string> = { 1: '身份证', 2: '护照', 3: '港澳台证' }

const loading = ref(false)
const tableData = ref<OwnerVO[]>([])
const total = ref(0)
const fileInputRef = ref<HTMLInputElement>()
const allBuildings = ref<BuildingVO[]>([])
const allRooms = ref<RoomVO[]>([])

const query = reactive({ current: 1, size: 20, ownerName: '', phone: '', roomCode: '', ownerType: undefined as number | undefined, status: undefined as number | undefined })

// 编辑弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<OwnerCreateRequest & { id?: string }>({
  ownerName: '',
  phone: '',
  password: '',
  idCardType: 1,
  idCardNo: '',
  gender: undefined,
  birthday: undefined,
  email: '',
  emergencyContact: '',
  emergencyPhone: '',
  avatar: '',
  ownerType: 1,
  status: 1,
  remark: '',
})
const rules: FormRules = {
  ownerName: [{ required: true, message: '请输入业主姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  idCardType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idCardNo: [{ required: true, message: '请输入证件号码', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// 绑定房屋弹窗
const bindDialogVisible = ref(false)
const bindLoading = ref(false)
const bindOwnerId = ref<string>('')
const bindOwnerName = ref('')
const bindFormRef = ref<FormInstance>()
const bindForm = reactive({ roomId: undefined as string | undefined, relationType: 1, isPrimary: 1, moveInTime: undefined as string | undefined })
const bindRules: FormRules = {
  roomId: [{ required: true, message: '请选择房屋', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
}

// 房屋列表弹窗
const roomListVisible = ref(false)
const roomListOwnerName = ref('')
const ownerRooms = ref<OwnerRoomVO[]>([])

function handleViewRooms(row: OwnerVO) {
  roomListOwnerName.value = row.ownerName
  listOwnerRooms(row.id).then(res => {
    ownerRooms.value = res.data
    roomListVisible.value = true
  })
}

onMounted(async () => {
  fetchData()
  // 预加载楼栋列表和房屋列表（用于绑定房屋弹窗）
  try {
    const res = await listAllBuildings()
    allBuildings.value = res.data
  } catch (e) {
    console.warn('加载楼栋列表失败', e)
  }
  await loadAllRooms()
})

async function loadAllRooms() {
  allRooms.value = []
  try {
    // 使用分页接口一次性查询所有启用房屋（pageSize 设大避免分页）
    const res = await pageRoom({ current: 1, size: 9999, status: 1 })
    allRooms.value = res.data.records || []
  } catch (e) {
    console.warn('加载房屋列表失败，绑定房屋弹窗将无法使用', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await pageOwner(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function search() { query.current = 1; fetchData() }
function reset() { query.ownerName = ''; query.phone = ''; query.roomCode = ''; query.ownerType = undefined; query.status = undefined; search() }

function handleAdd() {
  isEdit.value = false
  form.id = undefined; form.ownerName = ''; form.phone = ''; form.password = ''; form.idCardType = 1; form.idCardNo = ''
  form.gender = undefined; form.birthday = undefined; form.email = ''; form.emergencyContact = ''; form.emergencyPhone = ''
  form.avatar = ''; form.ownerType = 1; form.status = 1; form.remark = ''
  dialogVisible.value = true
}

function handleEdit(row: OwnerVO) {
  isEdit.value = true
  console.log('[handleEdit] row.id =', row.id, ', type =', typeof row.id)
  form.id = row.id; form.ownerName = row.ownerName; form.phone = row.phone; form.password = ''
  form.idCardType = row.idCardType; form.idCardNo = row.idCardNo; form.gender = row.gender
  form.birthday = row.birthday; form.email = row.email ?? ''; form.emergencyContact = row.emergencyContact ?? ''
  form.emergencyPhone = row.emergencyPhone ?? ''; form.avatar = row.avatar ?? ''; form.ownerType = row.ownerType; form.status = row.status
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

function handleDelete(row: OwnerVO) {
  console.log('[handleDelete] row.id =', row.id, ', type =', typeof row.id)
  ElMessageBox.confirm(`确认删除业主「${row.ownerName}」？`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    await deleteOwner(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch((err) => {
    if (err) {
      ElMessage.error(typeof err === 'string' ? err : (err.message || '删除失败'))
    }
  })
}

function handleBindRoom(row: OwnerVO) {
  console.log('[handleBindRoom] row.id =', row.id, ', type =', typeof row.id)
  bindOwnerId.value = row.id
  bindOwnerName.value = row.ownerName
  bindForm.roomId = undefined; bindForm.relationType = 1; bindForm.isPrimary = 1; bindForm.moveInTime = undefined
  bindDialogVisible.value = true
}

async function handleBindSubmit() {
  const valid = await bindFormRef.value?.validate().catch(() => false)
  if (!valid) return
  bindLoading.value = true
  try {
    console.log('[handleBindSubmit] ownerId =', bindOwnerId.value, ', type =', typeof bindOwnerId.value, ', roomId =', bindForm.roomId, ', type =', typeof bindForm.roomId)
    await bindOwnerRoom({
      ownerId: bindOwnerId.value,
      roomId: bindForm.roomId!,
      relationType: bindForm.relationType,
      isPrimary: bindForm.isPrimary,
      moveInTime: bindForm.moveInTime,
    })
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
  } finally {
    bindLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      console.log('[handleSubmit] updateOwner, form.id =', form.id, ', type =', typeof form.id)
      await updateOwner(form as OwnerUpdateRequest)
      ElMessage.success('修改成功')
    } else {
      // 密码留空时由后端自动设为手机号后6位，不发送空字符串
      const submitData = { ...form }
      if (!submitData.password) {
        delete submitData.password
      }
      await createOwner(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleExport() {
  try {
    const res = await exportOwnersExcel()
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `业主列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadOwnerTemplate()
    const blob = new Blob([res as unknown as BlobPart])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = '业主导入模板.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  }
}

function handleImport() {
  fileInputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    const res = await importOwnersExcel(file)
    const data = res.data as { successCount: number; failCount: number; failList: Array<{ errorMsg: string }> }
    if (data.failCount > 0) {
      ElMessage.warning(`导入完成：成功 ${data.successCount} 条，失败 ${data.failCount} 条`)
      console.warn('导入失败明细:', data.failList)
    } else {
      ElMessage.success(`导入成功 ${data.successCount} 条`)
    }
    fetchData()
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  }
  input.value = ''
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.search-form { margin-bottom: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

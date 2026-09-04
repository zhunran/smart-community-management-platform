<template>
  <div class="room-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>房屋管理</span>
          <el-button type="primary" @click="handleAdd">新增房屋</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="所属楼栋">
          <el-select v-model="query.buildingId" placeholder="全部" clearable filterable style="width:160px" @change="onBuildingChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单元">
          <el-select v-model="query.unitId" placeholder="全部" clearable filterable style="width:160px" :disabled="!query.buildingId">
            <el-option v-for="u in units" :key="u.id" :label="u.unitName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房号">
          <el-input v-model="query.roomCode" placeholder="模糊搜索" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="buildingName" label="楼栋" width="120" />
        <el-table-column prop="unitName" label="单元" width="100" />
        <el-table-column prop="roomCode" label="房号" width="100" />
        <el-table-column prop="roomName" label="房屋名称" min-width="140" />
        <el-table-column prop="floor" label="楼层" width="70" align="center" />
        <el-table-column prop="area" label="面积" width="90" align="right">
          <template #default="{ row }">{{ row.area }} m²</template>
        </el-table-column>
        <el-table-column prop="orientation" label="朝向" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">修改</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改房屋' : '新增房屋'" width="680px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属楼栋" prop="buildingId">
              <el-select v-model="form.buildingId" placeholder="请选择" filterable @change="onFormBuildingChange">
                <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属单元" prop="unitId">
              <el-select v-model="form.unitId" placeholder="请选择" filterable :disabled="!form.buildingId">
                <el-option v-for="u in formUnits" :key="u.id" :label="u.unitName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="房号" prop="roomCode">
              <el-input v-model="form.roomCode" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房屋名称" prop="roomName">
              <el-input v-model="form.roomName" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所在楼层" prop="floor">
              <el-input-number v-model="form.floor" :min="-5" :max="200" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建筑面积" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用面积" prop="usableArea">
              <el-input-number v-model="form.usableArea" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="朝向" prop="orientation">
              <el-select v-model="form.orientation" placeholder="请选择" clearable style="width:100%">
                <el-option label="东" value="东" />
                <el-option label="南" value="南" />
                <el-option label="西" value="西" />
                <el-option label="北" value="北" />
                <el-option label="南北" value="南北" />
                <el-option label="东南" value="东南" />
                <el-option label="西南" value="西南" />
                <el-option label="东北" value="东北" />
                <el-option label="西北" value="西北" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物业费率" prop="propertyFeeRate">
              <el-input-number v-model="form.propertyFeeRate" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageRoom, createRoom, updateRoom, deleteRoom } from '@/api/room'
import { listAllBuildings } from '@/api/building'
import { listUnitsByBuilding } from '@/api/unit'
import type { RoomVO, RoomCreateRequest, RoomUpdateRequest } from '@/api/room'
import type { BuildingVO } from '@/api/building'
import type { UnitVO } from '@/api/unit'

const loading = ref(false)
const tableData = ref<RoomVO[]>([])
const total = ref(0)
const buildings = ref<BuildingVO[]>([])
const units = ref<UnitVO[]>([])
const formUnits = ref<UnitVO[]>([])
const query = reactive({ current: 1, size: 20, buildingId: undefined as number | undefined, unitId: undefined as number | undefined, roomCode: '', floor: undefined as number | undefined, roomType: undefined, occupancyStatus: undefined, status: undefined as number | undefined })
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<RoomCreateRequest & { id?: number }>({
  buildingId: undefined as unknown as number,
  unitId: undefined as unknown as number,
  roomCode: '',
  roomName: '',
  floor: 1,
  roomType: undefined,
  area: undefined as unknown as number,
  usableArea: undefined as unknown as number,
  orientation: '',
  decorationStatus: undefined,
  occupancyStatus: undefined,
  propertyFeeRate: undefined as unknown as number,
  status: 1,
  remark: '',
})

const rules: FormRules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  unitId: [{ required: true, message: '请选择单元', trigger: 'change' }],
  roomCode: [{ required: true, message: '请输入房号', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入房屋名称', trigger: 'blur' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
  area: [{ required: true, message: '请输入建筑面积', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

onMounted(async () => {
  const res = await listAllBuildings()
  buildings.value = res.data
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageRoom(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function onBuildingChange(buildingId: number | undefined) {
  query.unitId = undefined
  units.value = []
  if (buildingId) {
    const res = await listUnitsByBuilding(buildingId)
    units.value = res.data
  }
}

async function onFormBuildingChange(buildingId: number | undefined) {
  form.unitId = undefined as unknown as number
  formUnits.value = []
  if (buildingId) {
    const res = await listUnitsByBuilding(buildingId)
    formUnits.value = res.data
  }
}

function search() { query.current = 1; fetchData() }
function reset() { query.buildingId = undefined; query.unitId = undefined; query.roomCode = ''; query.status = undefined; units.value = []; search() }

function handleAdd() {
  isEdit.value = false
  form.id = undefined; form.buildingId = undefined as unknown as number; form.unitId = undefined as unknown as number
  form.roomCode = ''; form.roomName = ''; form.floor = 1; form.area = undefined as unknown as number
  form.usableArea = undefined as unknown as number; form.orientation = ''; form.propertyFeeRate = undefined as unknown as number
  form.status = 1; form.remark = ''
  formUnits.value = []
  dialogVisible.value = true
}

function handleEdit(row: RoomVO) {
  isEdit.value = true
  form.id = row.id; form.buildingId = row.buildingId; form.unitId = row.unitId
  form.roomCode = row.roomCode; form.roomName = row.roomName; form.floor = row.floor; form.area = row.area
  form.usableArea = row.usableArea ?? undefined as unknown as number; form.orientation = row.orientation ?? ''
  form.propertyFeeRate = row.propertyFeeRate ?? undefined as unknown as number
  form.status = row.status; form.remark = row.remark ?? ''
  onFormBuildingChange(row.buildingId)
  dialogVisible.value = true
}

function handleDelete(row: RoomVO) {
  ElMessageBox.confirm(`确认删除房屋「${row.roomName}」？`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    await deleteRoom(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await updateRoom(form as RoomUpdateRequest)
      ElMessage.success('修改成功')
    } else {
      await createRoom(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

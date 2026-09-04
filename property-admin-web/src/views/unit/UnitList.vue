<template>
  <div class="unit-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>单元管理</span>
          <el-button type="primary" @click="handleAdd">新增单元</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="所属楼栋">
          <el-select v-model="query.buildingId" placeholder="全部" clearable filterable style="width:160px">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单元编号">
          <el-input v-model="query.unitCode" placeholder="模糊搜索" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
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
        <el-table-column prop="buildingName" label="所属楼栋" width="150" />
        <el-table-column prop="unitCode" label="单元编号" width="120" />
        <el-table-column prop="unitName" label="单元名称" min-width="160" />
        <el-table-column prop="totalFloors" label="楼层数" width="80" align="center" />
        <el-table-column prop="totalRooms" label="房屋数" width="80" align="center" />
        <el-table-column prop="elevatorCount" label="电梯数" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改单元' : '新增单元'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属楼栋" prop="buildingId">
          <el-select v-model="form.buildingId" placeholder="请选择" filterable style="width:100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单元编号" prop="unitCode">
          <el-input v-model="form.unitCode" maxlength="50" />
        </el-form-item>
        <el-form-item label="单元名称" prop="unitName">
          <el-input v-model="form.unitName" maxlength="100" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="楼层数" prop="totalFloors">
              <el-input-number v-model="form.totalFloors" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房屋数" prop="totalRooms">
              <el-input-number v-model="form.totalRooms" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="电梯数" prop="elevatorCount">
          <el-input-number v-model="form.elevatorCount" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { pageUnit, createUnit, updateUnit, deleteUnit } from '@/api/unit'
import { listAllBuildings } from '@/api/building'
import type { UnitVO, UnitCreateRequest, UnitUpdateRequest } from '@/api/unit'
import type { BuildingVO } from '@/api/building'

const loading = ref(false)
const tableData = ref<UnitVO[]>([])
const total = ref(0)
const buildings = ref<BuildingVO[]>([])
const query = reactive({ current: 1, size: 20, buildingId: undefined as number | undefined, unitCode: '', unitName: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<UnitCreateRequest & { id?: number }>({
  buildingId: undefined as unknown as number,
  unitCode: '',
  unitName: '',
  totalFloors: 0,
  totalRooms: 0,
  elevatorCount: 0,
  sortOrder: 0,
  status: 1,
  remark: '',
})

const rules: FormRules = {
  buildingId: [{ required: true, message: '请选择所属楼栋', trigger: 'change' }],
  unitCode: [{ required: true, message: '请输入单元编号', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单元名称', trigger: 'blur' }],
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
    const res = await pageUnit(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function search() { query.current = 1; fetchData() }
function reset() { query.buildingId = undefined; query.unitCode = ''; query.unitName = ''; query.status = undefined; search() }

function handleAdd() {
  isEdit.value = false
  form.id = undefined; form.buildingId = undefined as unknown as number; form.unitCode = ''; form.unitName = ''
  form.totalFloors = 0; form.totalRooms = 0; form.elevatorCount = 0; form.sortOrder = 0; form.status = 1; form.remark = ''
  dialogVisible.value = true
}

function handleEdit(row: UnitVO) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, buildingId: row.buildingId, unitCode: row.unitCode, unitName: row.unitName,
    totalFloors: row.totalFloors ?? 0, totalRooms: row.totalRooms ?? 0,
    elevatorCount: row.elevatorCount ?? 0, sortOrder: row.sortOrder ?? 0,
    status: row.status, remark: row.remark ?? '',
  })
  dialogVisible.value = true
}

function handleDelete(row: UnitVO) {
  ElMessageBox.confirm(`确认删除单元「${row.unitName}」？`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    await deleteUnit(row.id)
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
      await updateUnit(form as UnitUpdateRequest)
      ElMessage.success('修改成功')
    } else {
      await createUnit(form)
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

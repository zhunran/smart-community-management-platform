<template>
  <div class="building-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>楼栋管理</span>
          <el-button type="primary" @click="handleAdd">新增楼栋</el-button>
        </div>
      </template>

      <!-- 搜索 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="楼栋编号">
          <el-input v-model="query.buildingCode" placeholder="模糊搜索" clearable />
        </el-form-item>
        <el-form-item label="楼栋名称">
          <el-input v-model="query.buildingName" placeholder="模糊搜索" clearable />
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

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="buildingCode" label="楼栋编号" width="120" />
        <el-table-column prop="buildingName" label="楼栋名称" min-width="160" />
        <el-table-column prop="totalUnits" label="单元数" width="80" align="center" />
        <el-table-column prop="totalFloors" label="楼层数" width="80" align="center" />
        <el-table-column prop="totalRooms" label="房屋数" width="80" align="center" />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
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

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="fetchData"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改楼栋' : '新增楼栋'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="楼栋编号" prop="buildingCode">
          <el-input v-model="form.buildingCode" maxlength="50" />
        </el-form-item>
        <el-form-item label="楼栋名称" prop="buildingName">
          <el-input v-model="form.buildingName" maxlength="100" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单元数" prop="totalUnits">
              <el-input-number v-model="form.totalUnits" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼层数" prop="totalFloors">
              <el-input-number v-model="form.totalFloors" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="房屋数" prop="totalRooms">
              <el-input-number v-model="form.totalRooms" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { pageBuilding, createBuilding, updateBuilding, deleteBuilding, listAllBuildings } from '@/api/building'
import type { BuildingVO, BuildingCreateRequest, BuildingUpdateRequest } from '@/api/building'

const loading = ref(false)
const tableData = ref<BuildingVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, buildingCode: '', buildingName: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<BuildingCreateRequest & { id?: number }>({
  buildingCode: '',
  buildingName: '',
  totalUnits: 0,
  totalFloors: 0,
  totalRooms: 0,
  sortOrder: 0,
  status: 1,
  remark: '',
})

const rules: FormRules = {
  buildingCode: [{ required: true, message: '请输入楼栋编号', trigger: 'blur' }],
  buildingName: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await pageBuilding(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  fetchData()
}

function reset() {
  query.buildingCode = ''
  query.buildingName = ''
  query.status = undefined
  search()
}

function handleAdd() {
  isEdit.value = false
  form.id = undefined
  form.buildingCode = ''
  form.buildingName = ''
  form.totalUnits = 0
  form.totalFloors = 0
  form.totalRooms = 0
  form.sortOrder = 0
  form.status = 1
  form.remark = ''
  dialogVisible.value = true
}

function handleEdit(row: BuildingVO) {
  isEdit.value = true
  form.id = row.id
  form.buildingCode = row.buildingCode
  form.buildingName = row.buildingName
  form.totalUnits = row.totalUnits ?? 0
  form.totalFloors = row.totalFloors ?? 0
  form.totalRooms = row.totalRooms ?? 0
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

function handleDelete(row: BuildingVO) {
  ElMessageBox.confirm(`确认删除楼栋「${row.buildingName}」？楼栋下有单元则禁止删除。`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    await deleteBuilding(row.id)
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
      await updateBuilding(form as BuildingUpdateRequest)
      ElMessage.success('修改成功')
    } else {
      await createBuilding(form)
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

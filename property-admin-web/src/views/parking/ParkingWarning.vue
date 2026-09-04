<template>
  <div class="warning-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>车位对账与预警</span>
          <div>
            <el-button type="warning" :loading="running" @click="handleRun">执行对账</el-button>
          </div>
        </div>
      </template>

      <!-- 对账结果提示 -->
      <el-alert v-if="runResult" :title="runResult" type="success" show-icon closable style="margin-bottom:16px" />

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="预警类型">
          <el-select v-model="query.warningType" clearable style="width:160px">
            <el-option label="租赁到期未更新" value="LEASE_EXPIRED" />
            <el-option label="车位闲置" value="SPACE_IDLE" />
            <el-option label="欠费未支付" value="PAYMENT_PENDING" />
            <el-option label="占用异常" value="OCCUPANCY_ANOMALY" />
            <el-option label="租赁即将到期" value="LEASE_EXPIRING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width:120px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 预警统计 -->
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="6"><el-statistic title="待处理" :value="pendingCount" /></el-col>
        <el-col :span="6"><el-statistic title="已处理" :value="handledCount" /></el-col>
        <el-col :span="6"><el-statistic title="已关闭" :value="closedCount" /></el-col>
        <el-col :span="6"><el-statistic title="共 计" :value="total" /></el-col>
      </el-row>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="batchNo" label="批次号" width="160" />
        <el-table-column prop="warningTypeName" label="预警类型" width="130" />
        <el-table-column label="等级" width="70" align="center">
          <template #default="{row}">
            <el-tag :type="row.warningLevel==='HIGH'?'danger':row.warningLevel==='MEDIUM'?'warning':'info'" size="small">{{ ({HIGH:'高',MEDIUM:'中',LOW:'低'} as Record<string,string>)[row.warningLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}">
            <el-tag :type="row.status===0?'danger':row.status===1?'warning':row.status===2?'success':'info'" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="success" :disabled="row.status===2||row.status===3" @click="handleHandle(row)">处理</el-button>
            <el-button size="small" :disabled="row.status===3" @click="handleClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" @change="fetchData" />
      </div>
    </el-card>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理预警" width="450px">
      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="80px">
        <el-form-item label="备注" prop="handleRemark">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible=false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="handleSubmit">确定处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageWarning, runReconciliation, handleWarning, closeWarning } from '@/api/parking'
import type { ParkingWarningVO } from '@/api/parking'

const loading = ref(false)
const running = ref(false)
const runResult = ref('')
const tableData = ref<ParkingWarningVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, warningType: undefined as string | undefined, status: undefined as number | undefined })

const pendingCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const handledCount = computed(() => tableData.value.filter(r => r.status === 2).length)
const closedCount = computed(() => tableData.value.filter(r => r.status === 3).length)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await pageWarning(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally { loading.value = false }
}

function search() { query.current = 1; fetchData() }
function reset() { query.warningType = undefined; query.status = undefined; search() }

async function handleRun() {
  running.value = true
  runResult.value = ''
  try {
    const res = await runReconciliation()
    runResult.value = res.data
    ElMessage.success('对账完成')
    fetchData()
  } finally { running.value = false }
}

// handle
const handleVisible = ref(false)
const handleLoading = ref(false)
const handleFormRef = ref<FormInstance>()
const handleId = ref(0)
const handleForm = reactive({ handleRemark: '' })
const handleRules: FormRules = { handleRemark: [{ required: true, message: '请输入处理备注', trigger: 'blur' }] }

function handleHandle(row: ParkingWarningVO) {
  handleId.value = row.id
  handleForm.handleRemark = ''
  handleVisible.value = true
}

async function handleSubmit() {
  const valid = await handleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  handleLoading.value = true
  try {
    await handleWarning(handleId.value, { handleRemark: handleForm.handleRemark })
    ElMessage.success('处理成功')
    handleVisible.value = false
    fetchData()
  } finally { handleLoading.value = false }
}

// close
function handleClose(row: ParkingWarningVO) {
  ElMessageBox.prompt('关闭备注（可选）', '关闭预警', {
    confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '备注（可选）',
  }).then(async ({ value }) => {
    await closeWarning(row.id, value)
    ElMessage.success('已关闭')
    fetchData()
  }).catch(() => {})
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.search-form { margin-bottom: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

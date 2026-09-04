<template>
  <div class="sys-config-list">
    <el-card>
      <template #header><div class="card-header"><span>系统配置管理</span></div></template>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="分组"><el-input v-model="query.groupName" clearable style="width:140px" /></el-form-item>
        <el-form-item label="配置键"><el-input v-model="query.configKey" clearable style="width:180px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="groupName" label="分组" width="120" />
        <el-table-column prop="configKey" label="配置键" min-width="160" />
        <el-table-column prop="configValue" label="配置值" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ configTypeMap[row.configType] ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">修改</el-button>
            <el-button size="small" type="warning" @click="handleRefreshCache(row)">刷新缓存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
          :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" @change="fetchData" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="修改配置项" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="配置键"><el-input :model-value="form.configKey" disabled /></el-form-item>
        <el-form-item label="配置值" prop="configValue"><el-input v-model="form.configValue" maxlength="500" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageSysConfig, updateSysConfig, refreshSysConfigCache } from '@/api/sysConfig'
import type { SysConfigVO, SysConfigUpdateRequest } from '@/api/sysConfig'

const configTypeMap: Record<number, string> = { 1:'字符串', 2:'数值', 3:'布尔', 4:'JSON' }

const loading = ref(false)
const tableData = ref<SysConfigVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, groupName: '', configKey: '' })
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<SysConfigUpdateRequest & { id: string }>({ id: '', configKey: '', configValue: '', status: 1 })
const rules: FormRules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await pageSysConfig(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally { loading.value = false }
}

function search() { query.current = 1; fetchData() }
function reset() { query.groupName = ''; query.configKey = ''; search() }

function handleEdit(row: SysConfigVO) {
  form.id = row.id
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await updateSysConfig(form.id, { configKey: form.configKey, configValue: form.configValue, status: form.status })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitLoading.value = false }
}

async function handleRefreshCache(row: SysConfigVO) {
  await refreshSysConfigCache(row.id, row.configKey)
  ElMessage.success('缓存已刷新')
}
</script>
<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center}.search-form{margin-bottom:0}.pagination-wrap{margin-top:16px;display:flex;justify-content:flex-end}
</style>
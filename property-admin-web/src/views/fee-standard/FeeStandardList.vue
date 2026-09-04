<template>
  <div class="fee-standard-list">
    <el-card>
      <template #header><div class="card-header"><span>费用标准管理</span><el-button type="primary" @click="handleAdd">新增标准</el-button></div></template>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="费用项"><el-select v-model="query.feeItemId" clearable filterable style="width:160px"><el-option v-for="f in feeItems" :key="f.id" :label="f.itemName" :value="f.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:100px"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="feeItemName" label="费用项" width="140" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right"><template #default="{row}">¥{{ row.unitPrice }}</template></el-table-column>
        <el-table-column prop="roomCode" label="适用房屋" width="130"><template #default="{row}">{{ row.roomCode||'全局默认' }}</template></el-table-column>
        <el-table-column label="生效期" min-width="220">
          <template #default="{row}">{{ row.startDate||'立即' }} ~ {{ row.endDate||'长期' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}"><el-button size="small" @click="handleEdit(row)">修改</el-button><el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap"><el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" @change="fetchData" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit?'修改标准':'新增标准'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="费用项" prop="feeItemId"><el-select v-model="form.feeItemId" filterable style="width:100%"><el-option v-for="f in feeItems" :key="f.id" :label="f.itemName" :value="f.id" /></el-select></el-form-item>
        <el-form-item label="单价" prop="unitPrice"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="生效开始"><el-date-picker v-model="form.startDate" type="date" placeholder="立即生效" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col><el-col :span="12"><el-form-item label="生效结束"><el-date-picker v-model="form.endDate" type="date" placeholder="长期有效" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col></el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageFeeStandard, createFeeStandard, updateFeeStandard, deleteFeeStandard } from '@/api/feeStandard'
import type { FeeStandardVO, FeeStandardCreateRequest, FeeStandardUpdateRequest } from '@/api/feeStandard'
import { listAllFeeItems } from '@/api/feeItem'
import type { FeeItemVO } from '@/api/feeItem'
const loading=ref(false);const tableData=ref<FeeStandardVO[]>([]);const total=ref(0);const feeItems=ref<FeeItemVO[]>([])
const query=reactive({current:1,size:20,feeItemId:undefined as number|undefined,status:undefined as number|undefined})
const dialogVisible=ref(false);const isEdit=ref(false);const submitLoading=ref(false);const formRef=ref<FormInstance>()
const form=reactive<FeeStandardCreateRequest&{id?:number}>({feeItemId:undefined as unknown as number,unitPrice:0,startDate:undefined,endDate:undefined,remark:''})
const rules:FormRules={feeItemId:[{required:true,message:'请选择费用项',trigger:'change'}],unitPrice:[{required:true,message:'请输入单价',trigger:'blur'}]}
onMounted(async()=>{const res=await listAllFeeItems();feeItems.value=res.data;fetchData()})
async function fetchData(){loading.value=true;try{const res=await pageFeeStandard(query);tableData.value=res.data.records;total.value=Number(res.data.total)}finally{loading.value=false}}
function search(){query.current=1;fetchData()}
function reset(){query.feeItemId=undefined;query.status=undefined;search()}
function handleAdd(){isEdit.value=false;form.id=undefined;form.feeItemId=undefined as unknown as number;form.unitPrice=0;form.startDate=undefined;form.endDate=undefined;form.remark='';dialogVisible.value=true}
function handleEdit(row:FeeStandardVO){isEdit.value=true;form.id=row.id;form.feeItemId=row.feeItemId;form.unitPrice=row.unitPrice;form.startDate=row.startDate;form.endDate=row.endDate;form.remark=row.remark??'';dialogVisible.value=true}
function handleDelete(row:FeeStandardVO){ElMessageBox.confirm(`确认删除此标准？`,'提示',{confirmButtonText:'确定',cancelButtonText:'取消',type:'warning'}).then(async()=>{await deleteFeeStandard(row.id);ElMessage.success('删除成功');fetchData()}).catch(()=>{})}
async function handleSubmit(){const valid=await formRef.value?.validate().catch(()=>false);if(!valid)return;submitLoading.value=true;try{if(isEdit.value&&form.id){await updateFeeStandard(form as FeeStandardUpdateRequest);ElMessage.success('修改成功')}else{await createFeeStandard(form);ElMessage.success('新增成功')}dialogVisible.value=false;fetchData()}finally{submitLoading.value=false}}
</script>
<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center}.search-form{margin-bottom:0}.pagination-wrap{margin-top:16px;display:flex;justify-content:flex-end}
</style>

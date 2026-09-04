<template>
  <div class="fee-item-list">
    <el-card>
      <template #header><div class="card-header"><span>费用项管理</span><el-button type="primary" @click="handleAdd">新增费用项</el-button></div></template>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="编码"><el-input v-model="query.itemCode" clearable style="width:120px" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="query.itemName" clearable style="width:150px" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:100px"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="itemCode" label="编码" width="140" />
        <el-table-column prop="itemName" label="费用项名称" min-width="160" />
        <el-table-column label="计费周期" width="90" align="center"><template #default="{ row }">{{ ['','月','季','半年','年','一次性'][row.billingCycle] ?? '' }}</template></el-table-column>
        <el-table-column label="计费方式" width="90" align="center"><template #default="{ row }">{{ ['','面积','户','用量','固定金额'][row.calcType] ?? '' }}</template></el-table-column>
        <el-table-column prop="unitPrice" label="单价" width="100" align="right"><template #default="{ row }">¥{{ row.unitPrice }}</template></el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }"><el-button size="small" @click="handleEdit(row)">修改</el-button><el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap"><el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" @change="fetchData" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit?'修改费用项':'新增费用项'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="itemCode"><el-input v-model="form.itemCode" maxlength="50" /></el-form-item>
        <el-form-item label="名称" prop="itemName"><el-input v-model="form.itemName" maxlength="100" /></el-form-item>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="计费周期"><el-select v-model="form.billingCycle" style="width:100%"><el-option label="月" :value="1" /><el-option label="季" :value="2" /><el-option label="半年" :value="3" /><el-option label="年" :value="4" /><el-option label="一次性" :value="5" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="计费方式"><el-select v-model="form.calcType" style="width:100%"><el-option label="面积" :value="1" /><el-option label="户" :value="2" /><el-option label="用量" :value="3" /><el-option label="固定金额" :value="4" /></el-select></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" style="width:100%" /></el-form-item></el-col></el-row>
        <el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
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
import { pageFeeItem, createFeeItem, updateFeeItem, deleteFeeItem } from '@/api/feeItem'
import type { FeeItemVO, FeeItemCreateRequest, FeeItemUpdateRequest } from '@/api/feeItem'
const loading=ref(false);const tableData=ref<FeeItemVO[]>([]);const total=ref(0)
const query=reactive({current:1,size:20,itemCode:'',itemName:'',status:undefined as number|undefined})
const dialogVisible=ref(false);const isEdit=ref(false);const submitLoading=ref(false);const formRef=ref<FormInstance>()
const form=reactive<FeeItemCreateRequest&{id?:number}>({itemCode:'',itemName:'',billingCycle:1,calcType:1,unitPrice:0,sortOrder:0,status:1,remark:''})
const rules:FormRules={itemCode:[{required:true,message:'请输入编码',trigger:'blur'}],itemName:[{required:true,message:'请输入名称',trigger:'blur'}],status:[{required:true,message:'请选择状态',trigger:'change'}]}
onMounted(()=>fetchData())
async function fetchData(){loading.value=true;try{const res=await pageFeeItem(query);tableData.value=res.data.records;total.value=Number(res.data.total)}finally{loading.value=false}}
function search(){query.current=1;fetchData()}
function reset(){query.itemCode='';query.itemName='';query.status=undefined;search()}
function handleAdd(){isEdit.value=false;form.id=undefined;form.itemCode='';form.itemName='';form.billingCycle=1;form.calcType=1;form.unitPrice=0;form.sortOrder=0;form.status=1;form.remark='';dialogVisible.value=true}
function handleEdit(row:FeeItemVO){isEdit.value=true;form.id=row.id;form.itemCode=row.itemCode;form.itemName=row.itemName;form.billingCycle=row.billingCycle;form.calcType=row.calcType;form.unitPrice=row.unitPrice;form.sortOrder=row.sortOrder;form.status=row.status;form.remark=row.remark??'';dialogVisible.value=true}
function handleDelete(row:FeeItemVO){ElMessageBox.confirm(`确认删除「${row.itemName}」？`,'提示',{confirmButtonText:'确定',cancelButtonText:'取消',type:'warning'}).then(async()=>{await deleteFeeItem(row.id);ElMessage.success('删除成功');fetchData()}).catch(()=>{})}
async function handleSubmit(){const valid=await formRef.value?.validate().catch(()=>false);if(!valid)return;submitLoading.value=true;try{if(isEdit.value&&form.id){await updateFeeItem(form as FeeItemUpdateRequest);ElMessage.success('修改成功')}else{await createFeeItem(form);ElMessage.success('新增成功')}dialogVisible.value=false;fetchData()}finally{submitLoading.value=false}}
</script>
<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center}.search-form{margin-bottom:0}.pagination-wrap{margin-top:16px;display:flex;justify-content:flex-end}
</style>

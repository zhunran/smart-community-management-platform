<template>
  <div class="payment-list">
    <el-card>
      <template #header><div class="card-header"><span>缴费记录</span><div><el-button @click="handleImport">导入Excel</el-button><el-button type="primary" @click="handleExport">导出Excel</el-button></div></div></template>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="支付单号"><el-input v-model="query.paymentNo" clearable style="width:150px" /></el-form-item>
        <el-form-item label="支付方式"><el-select v-model="query.paymentMethod" clearable style="width:100px"><el-option label="支付宝" :value="1" /><el-option label="微信" :value="2" /><el-option label="银行卡" :value="3" /><el-option label="现金" :value="4" /><el-option label="转账" :value="5" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.paymentStatus" clearable style="width:100px"><el-option label="支付成功" :value="2" /><el-option label="待支付" :value="0" /><el-option label="支付失败" :value="3" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="paymentNo" label="支付单号" width="170" />
        <el-table-column prop="billNo" label="账单编号" width="150" />
        <el-table-column prop="buildingName" label="楼栋" width="90" />
        <el-table-column prop="roomCode" label="房号" width="80" />
        <el-table-column prop="ownerName" label="业主" width="80" />
        <el-table-column prop="paymentMethodName" label="支付方式" width="80" align="center" />
        <el-table-column prop="paymentAmount" label="金额" width="90" align="right"><template #default="{row}">¥{{ row.paymentAmount }}</template></el-table-column>
        <el-table-column prop="paymentStatusName" label="状态" width="90" align="center"><template #default="{row}"><el-tag :type="row.paymentStatus===2?'success':'warning'" size="small">{{ row.paymentStatusName }}</el-tag></template></el-table-column>
        <el-table-column prop="paymentTime" label="支付时间" width="160" />
        <el-table-column prop="payerName" label="付款人" width="80" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}"><el-button size="small" @click="handleDetail(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap"><el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" @change="fetchData" /></div>
    </el-card>
    <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange" />
    <el-dialog v-model="detailVisible" title="缴费记录详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="支付单号">{{ detail.paymentNo }}</el-descriptions-item>
          <el-descriptions-item label="账单编号">{{ detail.billNo }}</el-descriptions-item>
          <el-descriptions-item label="业主">{{ detail.ownerName }}</el-descriptions-item>
          <el-descriptions-item label="房号">{{ detail.buildingName }} {{ detail.roomCode }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detail.paymentMethodName }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ detail.paymentAmount }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.paymentStatusName }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detail.paymentTime }}</el-descriptions-item>
          <el-descriptions-item label="付款人">{{ detail.payerName }}</el-descriptions-item>
          <el-descriptions-item label="流水号">{{ detail.transactionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pagePayment, getPaymentDetail, exportPaymentsExcel, importPaymentsExcel } from '@/api/payment'
import type { PaymentOrderVO, PaymentOrderPageQuery } from '@/api/payment'
const loading=ref(false);const tableData=ref<PaymentOrderVO[]>([]);const total=ref(0);const fileInputRef=ref<HTMLInputElement>()
const query=reactive<PaymentOrderPageQuery&{current:number;size:number}>({current:1,size:20,paymentNo:'',paymentMethod:undefined,paymentStatus:undefined})
onMounted(()=>fetchData())
async function fetchData(){loading.value=true;try{const res=await pagePayment(query);tableData.value=res.data.records;total.value=Number(res.data.total)}finally{loading.value=false}}
function search(){query.current=1;fetchData()}
function reset(){query.paymentNo='';query.paymentMethod=undefined;query.paymentStatus=undefined;search()}
// detail
const detailVisible=ref(false);const detail=ref<PaymentOrderVO|null>(null)
async function handleDetail(row:PaymentOrderVO){detailVisible.value=true;const res=await getPaymentDetail(row.id);detail.value=res.data}
// export
async function handleExport(){try{const res=await exportPaymentsExcel(query);const blob=new Blob([res as unknown as BlobPart],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`缴费记录_${new Date().toISOString().slice(0,10)}.xlsx`;a.click();URL.revokeObjectURL(url);ElMessage.success('导出成功')}catch{ElMessage.error('导出失败')}}
// import
function handleImport(){fileInputRef.value?.click()}
async function onFileChange(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;try{const res=await importPaymentsExcel(file);const data=res.data as {successCount:number;failCount:number};ElMessage.success(`导入完成：成功${data.successCount}条，失败${data.failCount}条`);fetchData()}catch{ElMessage.error('导入失败')}input.value=''}
</script>
<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px}
.search-form{margin-bottom:0}.pagination-wrap{margin-top:16px;display:flex;justify-content:flex-end}
</style>

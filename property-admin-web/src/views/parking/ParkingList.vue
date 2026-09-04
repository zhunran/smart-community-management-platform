<template>
  <div class="parking-list">
    <el-card>
      <template #header><div class="card-header"><span>车位管理</span><el-button type="primary" @click="handleBind">绑定车位</el-button></div></template>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="spaceCode" label="车位编号" width="120" />
        <el-table-column prop="spaceName" label="车位名称" min-width="150" />
        <el-table-column label="类型" width="80" align="center"><template #default="{row}">{{ SPACE_TYPE_MAP[row.spaceType]||'标准' }}</template></el-table-column>
        <el-table-column prop="floor" label="楼层" width="70" align="center" />
        <el-table-column prop="zone" label="区域" width="70" align="center" />
        <el-table-column prop="ownerName" label="当前业主" width="100" />
        <el-table-column prop="roomCode" label="关联房号" width="100" />
        <el-table-column label="使用方式" width="80" align="center"><template #default="{row}">{{ RENTAL_TYPE_MAP[row.rentalType]||'-' }}</template></el-table-column>
        <el-table-column prop="monthlyFee" label="月租费" width="90" align="right"><template #default="{row}">¥{{ row.monthlyFee||0 }}</template></el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{row}"><el-tag :type="statusTag(row.status)" size="small">{{ PARKING_STATUS_MAP[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{row}">
            <el-button size="small" :disabled="row.status===0" @click="handleChange(row)">变更</el-button>
            <el-button size="small" type="danger" :disabled="row.status===0" @click="handleUnbind(row)">退租</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 绑定 -->
    <el-dialog v-model="bindVisible" title="绑定车位" width="500px">
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
        <el-form-item label="车位" prop="spaceId"><el-select v-model="bindForm.spaceId" filterable style="width:100%"><el-option v-for="s in idleSpaces" :key="s.id" :label="`${s.spaceCode} ${s.spaceName}`" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="业主" prop="ownerId"><el-select v-model="bindForm.ownerId" filterable style="width:100%"><el-option v-for="o in owners" :key="o.id" :label="`${o.ownerName}(${o.phone})`" :value="o.id" /></el-select></el-form-item>
        <el-form-item label="使用方式" prop="rentalType"><el-radio-group v-model="bindForm.rentalType"><el-radio :value="1">自有</el-radio><el-radio :value="2">租赁</el-radio><el-radio :value="3">临时</el-radio></el-radio-group></el-form-item>
        <el-form-item label="关联房屋"><el-select v-model="bindForm.roomId" clearable filterable style="width:100%"><el-option v-for="r in allRooms" :key="r.id" :label="`${r.buildingName} ${r.roomName}`" :value="r.id" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="bindForm.remark" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="bindVisible=false">取消</el-button><el-button type="primary" :loading="bindLoading" @click="handleBindSubmit">确定</el-button></template>
    </el-dialog>
    <!-- 变更 -->
    <el-dialog v-model="changeVisible" title="变更业主" width="500px">
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="100px">
        <el-form-item label="当前车位"><el-input :model-value="changeSpaceName" disabled /></el-form-item>
        <el-form-item label="新业主" prop="newOwnerId"><el-select v-model="changeForm.newOwnerId" filterable style="width:100%"><el-option v-for="o in owners" :key="o.id" :label="`${o.ownerName}(${o.phone})`" :value="o.id" /></el-select></el-form-item>
        <el-form-item label="新房屋"><el-select v-model="changeForm.newRoomId" clearable filterable style="width:100%"><el-option v-for="r in allRooms" :key="r.id" :label="`${r.buildingName} ${r.roomName}`" :value="r.id" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="changeForm.remark" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="changeVisible=false">取消</el-button><el-button type="primary" :loading="changeLoading" @click="handleChangeSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listParking, bindParking, changeParking, unbindParking, PARKING_STATUS_MAP, RENTAL_TYPE_MAP, SPACE_TYPE_MAP } from '@/api/parking'
import type { ParkingSpaceVO, ParkingBindRequest, ParkingChangeRequest } from '@/api/parking'
import { pageOwner } from '@/api/owner'
import type { OwnerVO } from '@/api/owner'
import { pageRoom } from '@/api/room'
import type { RoomVO } from '@/api/room'

function statusTag(s:number){return s===0?'info':s===1||s===2?'success':'warning'}

const loading=ref(false);const tableData=ref<ParkingSpaceVO[]>([])
const owners=ref<OwnerVO[]>([]);const allRooms=ref<RoomVO[]>([])

onMounted(async()=>{
  try {
    const ownerRes=await pageOwner({current:1,size:1000})
    owners.value=ownerRes.data.records
  } catch(e){console.warn('加载业主列表失败',e)}
  try {
    const roomRes=await pageRoom({current:1,size:9999,status:1})
    allRooms.value=roomRes.data.records||[]
  } catch(e){console.warn('加载房屋列表失败',e)}
  fetchData()
})

async function fetchData(){loading.value=true;try{const res=await listParking();tableData.value=res.data}finally{loading.value=false}}

// bind
const bindVisible=ref(false);const bindLoading=ref(false);const bindFormRef=ref<FormInstance>()
const idleSpaces=ref<ParkingSpaceVO[]>([])
const bindForm=ref<ParkingBindRequest>({spaceId:undefined as unknown as number,ownerId:undefined as unknown as number,rentalType:2,roomId:undefined,remark:''})
const bindRules:FormRules={spaceId:[{required:true,message:'请选择车位',trigger:'change'}],ownerId:[{required:true,message:'请选择业主',trigger:'change'}],rentalType:[{required:true,message:'请选择使用方式',trigger:'change'}]}
function handleBind(){
  idleSpaces.value=tableData.value.filter(s=>s.status===0)
  bindForm.value={spaceId:undefined as unknown as number,ownerId:undefined as unknown as number,rentalType:2,roomId:undefined,remark:''}
  bindVisible.value=true
}
async function handleBindSubmit(){const valid=await bindFormRef.value?.validate().catch(()=>false);if(!valid)return;bindLoading.value=true;try{await bindParking(bindForm.value);ElMessage.success('绑定成功');bindVisible.value=false;fetchData()}finally{bindLoading.value=false}}

// change
const changeVisible=ref(false);const changeLoading=ref(false);const changeFormRef=ref<FormInstance>();const changeSpaceName=ref('')
const changeForm=ref<ParkingChangeRequest>({spaceId:undefined as unknown as number,newOwnerId:undefined as unknown as number,newRoomId:undefined,remark:''})
const changeRules:FormRules={newOwnerId:[{required:true,message:'请选择新业主',trigger:'change'}]}
function handleChange(row:ParkingSpaceVO){
  changeSpaceName.value=`${row.spaceCode} ${row.spaceName}`
  changeForm.value={spaceId:row.id,newOwnerId:undefined as unknown as number,newRoomId:undefined,remark:''}
  changeVisible.value=true
}
async function handleChangeSubmit(){const valid=await changeFormRef.value?.validate().catch(()=>false);if(!valid)return;changeLoading.value=true;try{await changeParking(changeForm.value);ElMessage.success('变更成功');changeVisible.value=false;fetchData()}finally{changeLoading.value=false}}

// unbind
function handleUnbind(row:ParkingSpaceVO){
  ElMessageBox.prompt('请输入退租备注（可选）','退租解绑',{confirmButtonText:'确定',cancelButtonText:'取消',inputPlaceholder:'备注'}).then(async({value})=>{await unbindParking(row.id,value);ElMessage.success('退租成功');fetchData()}).catch(()=>{})
}
</script>
<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center}
</style>

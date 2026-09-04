import request from '@/utils/request'

export interface OwnerRoomVO {
  id: string
  ownerId: string
  roomId: string
  relationType: number
  isPrimary: number
  moveInTime: string
  status: number
  ownerName: string
  ownerPhone: string
  roomCode: string
  roomName: string
  buildingName: string
  unitName: string
}

export interface OwnerVO {
  id: string
  ownerName: string
  phone: string
  idCardType: number
  idCardNo: string
  gender: number
  birthday: string
  email: string
  emergencyContact: string
  emergencyPhone: string
  avatar: string
  ownerType: number
  status: number
  remark: string
  registerTime: string
  createTime: string
}

export interface OwnerDetailVO extends OwnerVO {
  lastLoginTime: string
  updateTime: string
}

export interface OwnerPageQuery {
  current?: number
  size?: number
  ownerName?: string
  phone?: string
  idCardType?: number
  idCardNo?: string
  gender?: number
  ownerType?: number
  status?: number
  roomCode?: string
  birthdayStart?: string
  birthdayEnd?: string
  registerTimeStart?: string
  registerTimeEnd?: string
}

export interface OwnerCreateRequest {
  ownerName: string
  phone: string
  password?: string
  idCardType: number
  idCardNo: string
  gender?: number
  birthday?: string
  email?: string
  emergencyContact?: string
  emergencyPhone?: string
  avatar?: string
  ownerType?: number
  status: number
  remark?: string
}

export interface OwnerUpdateRequest {
  id: string
  ownerName: string
  phone: string
  idCardType: number
  idCardNo: string
  gender?: number
  birthday?: string
  email?: string
  emergencyContact?: string
  emergencyPhone?: string
  avatar?: string
  ownerType?: number
  status: number
  remark?: string
}

/** 业主分页查询 */
export function pageOwner(params: OwnerPageQuery) {
  return request.get('/api/admin/owner/page', { params })
}

/** 业主详情 */
export function getOwnerDetail(id: string) {
  return request.get(`/api/admin/owner/${id}`)
}

/** 新增业主 */
export function createOwner(data: OwnerCreateRequest) {
  return request.post('/api/admin/owner', data)
}

/** 修改业主 */
export function updateOwner(data: OwnerUpdateRequest) {
  return request.put('/api/admin/owner', data)
}

/** 删除业主 */
export function deleteOwner(id: string) {
  return request.delete(`/api/admin/owner/${id}`)
}

/** 导出业主列表 */
export function exportOwnersExcel() {
  return request.get('/api/admin/owner/excel/export', { responseType: 'blob' })
}

/** 下载导入模板 */
export function downloadOwnerTemplate() {
  return request.get('/api/admin/owner/excel/template', { responseType: 'blob' })
}

/** 导入业主 */
export function importOwnersExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/owner/excel/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

/** 绑定业主到房屋 */
export function bindOwnerRoom(data: { ownerId: string; roomId: string; relationType?: number; isPrimary?: number; moveInTime?: string }) {
  return request.post('/api/admin/owner-room/bind', data)
}

/** 查询业主名下的房屋列表 */
export function listOwnerRooms(ownerId: string) {
  return request.get('/api/admin/owner-room/list-by-owner', { params: { ownerId } })
}

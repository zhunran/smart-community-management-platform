import request from '@/utils/request'

export interface BuildingVO {
  id: number
  buildingCode: string
  buildingName: string
  totalUnits: number
  totalFloors: number
  totalRooms: number
  sortOrder: number
  status: number
  remark: string
  createTime: string
  updateTime: string
}

export interface BuildingPageQuery {
  current?: number
  size?: number
  buildingCode?: string
  buildingName?: string
  status?: number
}

export interface BuildingCreateRequest {
  buildingCode: string
  buildingName: string
  totalUnits?: number
  totalFloors?: number
  totalRooms?: number
  sortOrder?: number
  status: number
  remark?: string
}

export interface BuildingUpdateRequest {
  id: number
  buildingCode: string
  buildingName: string
  totalUnits?: number
  totalFloors?: number
  totalRooms?: number
  sortOrder?: number
  status: number
  remark?: string
}

/** 楼栋分页查询 */
export function pageBuilding(params: BuildingPageQuery) {
  return request.get('/api/admin/building/page', { params })
}

/** 楼栋详情 */
export function getBuildingDetail(id: number) {
  return request.get(`/api/admin/building/${id}`)
}

/** 全部启用楼栋列表 */
export function listAllBuildings() {
  return request.get('/api/admin/building/list')
}

/** 新增楼栋 */
export function createBuilding(data: BuildingCreateRequest) {
  return request.post('/api/admin/building', data)
}

/** 修改楼栋 */
export function updateBuilding(data: BuildingUpdateRequest) {
  return request.put('/api/admin/building', data)
}

/** 删除楼栋 */
export function deleteBuilding(id: number) {
  return request.delete(`/api/admin/building/${id}`)
}

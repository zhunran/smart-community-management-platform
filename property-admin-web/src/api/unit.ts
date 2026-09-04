import request from '@/utils/request'

export interface UnitVO {
  id: number
  buildingId: number
  unitCode: string
  unitName: string
  totalFloors: number
  totalRooms: number
  elevatorCount: number
  sortOrder: number
  status: number
  remark: string
  createTime: string
  updateTime: string
  buildingName: string
}

export interface UnitPageQuery {
  current?: number
  size?: number
  buildingId?: number
  unitCode?: string
  unitName?: string
  status?: number
}

export interface UnitCreateRequest {
  buildingId: number
  unitCode: string
  unitName: string
  totalFloors?: number
  totalRooms?: number
  elevatorCount?: number
  sortOrder?: number
  status: number
  remark?: string
}

export interface UnitUpdateRequest {
  id: number
  buildingId: number
  unitCode: string
  unitName: string
  totalFloors?: number
  totalRooms?: number
  elevatorCount?: number
  sortOrder?: number
  status: number
  remark?: string
}

/** 单元分页查询 */
export function pageUnit(params: UnitPageQuery) {
  return request.get('/api/admin/unit/page', { params })
}

/** 单元详情 */
export function getUnitDetail(id: number) {
  return request.get(`/api/admin/unit/${id}`)
}

/** 按楼栋查询单元列表 */
export function listUnitsByBuilding(buildingId: number) {
  return request.get('/api/admin/unit/list-by-building', { params: { buildingId } })
}

/** 新增单元 */
export function createUnit(data: UnitCreateRequest) {
  return request.post('/api/admin/unit', data)
}

/** 修改单元 */
export function updateUnit(data: UnitUpdateRequest) {
  return request.put('/api/admin/unit', data)
}

/** 删除单元 */
export function deleteUnit(id: number) {
  return request.delete(`/api/admin/unit/${id}`)
}

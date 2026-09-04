import request from '@/utils/request'

export interface RoomVO {
  id: string
  buildingId: string
  unitId: string
  roomCode: string
  roomName: string
  floor: number
  roomType: number
  area: number
  usableArea: number
  orientation: string
  decorationStatus: number
  occupancyStatus: number
  propertyFeeRate: number
  status: number
  remark: string
  createTime: string
  updateTime: string
  buildingName: string
  unitName: string
}

export interface RoomPageQuery {
  current?: number
  size?: number
  buildingId?: number
  unitId?: number
  roomCode?: string
  floor?: number
  roomType?: number
  occupancyStatus?: number
  status?: number
}

export interface RoomCreateRequest {
  buildingId: number
  unitId: number
  roomCode: string
  roomName: string
  floor: number
  roomType?: number
  area: number
  usableArea?: number
  orientation?: string
  decorationStatus?: number
  occupancyStatus?: number
  propertyFeeRate?: number
  status: number
  remark?: string
}

export interface RoomUpdateRequest {
  id: number
  buildingId: number
  unitId: number
  roomCode: string
  roomName: string
  floor: number
  roomType?: number
  area: number
  usableArea?: number
  orientation?: string
  decorationStatus?: number
  occupancyStatus?: number
  propertyFeeRate?: number
  status: number
  remark?: string
}

/** 房屋分页查询 */
export function pageRoom(params: RoomPageQuery) {
  return request.get('/api/admin/room/page', { params })
}

/** 房屋详情 */
export function getRoomDetail(id: number) {
  return request.get(`/api/admin/room/${id}`)
}

/** 按单元查询房屋列表 */
export function listRoomsByUnit(unitId: number) {
  return request.get('/api/admin/room/list-by-unit', { params: { unitId } })
}

/** 新增房屋 */
export function createRoom(data: RoomCreateRequest) {
  return request.post('/api/admin/room', data)
}

/** 修改房屋 */
export function updateRoom(data: RoomUpdateRequest) {
  return request.put('/api/admin/room', data)
}

/** 删除房屋 */
export function deleteRoom(id: number) {
  return request.delete(`/api/admin/room/${id}`)
}

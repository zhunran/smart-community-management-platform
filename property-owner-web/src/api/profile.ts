import request from '@/utils/request'

export interface OwnerDetailVO {
  id: number
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
  registerTime: string
  lastLoginTime: string
  createTime: string
}

export interface OwnerRoomVO {
  id: number
  ownerId: number
  roomId: number
  relationType: number
  isPrimary: number
  status: number
  buildingName: string
  roomCode: string
  roomName: string
  ownerName: string
}

export function getProfile() {
  return request.get<OwnerDetailVO>('/api/owner/profile')
}

export function getMyRooms() {
  return request.get<OwnerRoomVO[]>('/api/owner/profile/rooms')
}

export interface OwnerProfileUpdateRequest {
  ownerName: string
  phone: string
  gender?: number
  birthday?: string
  email?: string
  emergencyContact?: string
  emergencyPhone?: string
  avatar?: string
}

export function updateProfile(data: OwnerProfileUpdateRequest) {
  return request.post('/api/owner/profile/owner', data)
}

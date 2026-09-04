import request from '@/utils/request'

// ==================== 枚举映射 ====================

export const ACTIVITY_STATUS_MAP: Record<number, string> = {
  0: '草稿',
  1: '招募中',
  2: '已满员',
  3: '进行中',
  4: '已结束',
  5: '已取消',
}

export const ACTIVITY_TYPE_MAP: Record<number, string> = {
  1: '节日活动',
  2: '亲子',
  3: '运动',
  4: '讲座',
  5: '其他',
}

export const POST_STATUS_MAP: Record<number, string> = {
  0: '待审核',
  1: '已发布',
  2: '已驳回',
  3: '已删除',
}

export const POST_CATEGORY_MAP: Record<number, string> = {
  1: '二手转让',
  2: '失物招领',
  3: '装修推荐',
  4: '邻里互助',
  5: '其他',
}

export const COMMENT_STATUS_MAP: Record<number, string> = {
  0: '待审核',
  1: '正常',
  2: '已删除',
}

// ==================== 活动类型 ====================

export interface CommunityActivityVO {
  id: string
  title: string
  coverImage: string
  activityType: number
  activityTypeName: string
  location: string
  organizer: string
  startTime: string
  endTime: string
  signupStart: string
  signupEnd: string
  maxParticipants: number
  signupCount: number
  status: number
  statusName: string
  createTime: string
}

export interface CommunityActivityDetailVO extends CommunityActivityVO {
  content: string
}

export interface CommunityActivityQuery {
  current?: number
  size?: number
  title?: string
  activityType?: number
  status?: number
  location?: string
}

export interface CommunityActivityCreateRequest {
  title: string
  content: string
  coverImage?: string
  activityType: number
  location: string
  organizer: string
  startTime: string
  endTime: string
  signupStart?: string
  signupEnd?: string
  maxParticipants: number
}

export interface CommunityActivityUpdateRequest extends CommunityActivityCreateRequest {
  id: string
}

// ==================== 活动接口 ====================

export function pageActivity(params: CommunityActivityQuery) {
  return request.get('/api/admin/community/activity/page', { params })
}

export function getActivity(id: string) {
  return request.get(`/api/admin/community/activity/${id}`)
}

export function createActivity(data: CommunityActivityCreateRequest) {
  return request.post('/api/admin/community/activity', data)
}

export function createAndPublishActivity(data: CommunityActivityCreateRequest) {
  return request.post('/api/admin/community/activity/publish', data)
}

export function updateActivity(data: CommunityActivityUpdateRequest) {
  return request.put('/api/admin/community/activity', data)
}

export function deleteActivity(id: string) {
  return request.delete(`/api/admin/community/activity/${id}`)
}

export function publishActivity(id: string) {
  return request.post(`/api/admin/community/activity/${id}/publish`)
}

export function cancelActivity(id: string) {
  return request.post(`/api/admin/community/activity/${id}/cancel`)
}

// ==================== 论坛类型 ====================

export interface ForumPostVO {
  id: string
  title: string
  images: string
  category: number
  categoryName: string
  viewCount: number
  likeCount: number
  commentCount: number
  isPinned: number
  isEssence: number
  status: number
  statusName: string
  createTime: string
}

export interface ForumCommentVO {
  id: string
  postId: string
  parentId: string
  content: string
  status: number
  statusName: string
  createTime: string
}

export interface ForumPostQuery {
  current?: number
  size?: number
  category?: number
  status?: number
  keyword?: string
}

export interface ForumPostAuditRequest {
  status: number
  rejectReason?: string
}

// ==================== 论坛接口 ====================

export function pagePost(params: ForumPostQuery) {
  return request.get('/api/admin/community/forum/post/page', { params })
}

export function auditPost(id: string, data: ForumPostAuditRequest) {
  return request.post(`/api/admin/community/forum/post/${id}/audit`, data)
}

export function togglePin(id: string) {
  return request.post(`/api/admin/community/forum/post/${id}/pin`)
}

export function toggleEssence(id: string) {
  return request.post(`/api/admin/community/forum/post/${id}/essence`)
}

export function deletePost(id: string) {
  return request.delete(`/api/admin/community/forum/post/${id}`)
}

export function commentPage(postId: string, params: { current?: number; size?: number }) {
  return request.get('/api/admin/community/forum/comment/page', { params: { ...params, postId } })
}

export function deleteComment(id: string) {
  return request.delete(`/api/admin/community/forum/comment/${id}`)
}

// ==================== 投票枚举映射 ====================

export const VOTE_STATUS_MAP: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
}

export const VOTE_TYPE_MAP: Record<number, string> = {
  1: '单选',
  2: '多选',
}

// ==================== 投票类型 ====================

export interface VoteOptionVO {
  id: string
  content: string
  voteCount: number
  sortOrder: number
}

export interface VoteVO {
  id: string
  title: string
  voteType: number
  voteTypeName: string
  isAnonymous: number
  startTime: string
  endTime: string
  status: number
  statusName: string
  createTime: string
}

export interface VoteDetailVO extends VoteVO {
  description: string
  totalVotes: number
  options: VoteOptionVO[]
}

export interface VoteQuery {
  current?: number
  size?: number
  title?: string
  status?: number
}

export interface VoteCreateRequest {
  title: string
  description?: string
  voteType: number
  isAnonymous: number
  startTime: string
  endTime: string
  options: string[]
}

// ==================== 投票接口 ====================

export function pageVote(params: VoteQuery) {
  return request.get('/api/admin/community/vote/page', { params })
}

export function getVote(id: string) {
  return request.get(`/api/admin/community/vote/${id}`)
}

export function createVote(data: VoteCreateRequest) {
  return request.post('/api/admin/community/vote', data)
}

export function startVote(id: string) {
  return request.post(`/api/admin/community/vote/${id}/start`)
}

export function endVote(id: string) {
  return request.post(`/api/admin/community/vote/${id}/end`)
}

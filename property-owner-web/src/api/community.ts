import request from "@/utils/request";

// ==================== 枚举映射 ====================

export const ACTIVITY_STATUS_MAP: Record<number, string> = {
  0: "草稿",
  1: "招募中",
  2: "已满员",
  3: "进行中",
  4: "已结束",
  5: "已取消",
};

export const ACTIVITY_TYPE_MAP: Record<number, string> = {
  1: "节日活动",
  2: "亲子",
  3: "运动",
  4: "讲座",
  5: "其他",
};

export const POST_CATEGORY_MAP: Record<number, string> = {
  1: "二手转让",
  2: "失物招领",
  3: "装修推荐",
  4: "邻里互助",
  5: "其他",
};

// ==================== 活动类型 ====================

export interface CommunityActivityVO {
  id: string;
  title: string;
  coverImage: string;
  activityType: number;
  activityTypeName: string;
  location: string;
  organizer: string;
  startTime: string;
  endTime: string;
  signupStart: string;
  signupEnd: string;
  maxParticipants: number;
  signupCount: number;
  status: number;
  statusName: string;
  createTime: string;
}

export interface CommunityActivityDetailVO extends CommunityActivityVO {
  content: string;
  isSignedUp: boolean;
}

export interface ActivitySignupVO {
  id: string;
  activityId: string;
  title: string;
  activityStartTime: string;
  activityEndTime: string;
  location: string;
  participants: number;
  status: number;
  statusName: string;
  signupTime: string;
  checkinTime: string;
}

// ==================== 活动接口 ====================

export function pageActivity(params: {
  current?: number;
  size?: number;
  activityType?: number;
}) {
  return request.get("/api/owner/community/activity/page", { params });
}

export function getActivity(id: string) {
  return request.get(`/api/owner/community/activity/${id}`);
}

export function signupActivity(id: string) {
  return request.post(`/api/owner/community/activity/${id}/signup`);
}

export function cancelSignupActivity(id: string) {
  return request.delete(`/api/owner/community/activity/${id}/signup`);
}

export function mySignups(params: { current?: number; size?: number }) {
  return request.get("/api/owner/community/activity/mine", { params });
}

// ==================== 论坛类型 ====================

export interface ForumPostVO {
  id: string;
  title: string;
  images: string;
  category: number;
  categoryName: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  isPinned: number;
  isEssence: number;
  status: number;
  statusName: string;
  createTime: string;
}

export interface ForumCommentVO {
  id: string;
  postId: string;
  parentId: string;
  replyTo: string;
  ownerId: string;
  content: string;
  likeCount: number;
  status: number;
  statusName: string;
  children: ForumCommentVO[];
  createTime: string;
}

export interface ForumPostDetailVO {
  id: string;
  title: string;
  content: string;
  images: string;
  category: number;
  categoryName: string;
  ownerId: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  isPinned: number;
  isEssence: number;
  rejectReason: string;
  sensitiveWords: string;
  status: number;
  statusName: string;
  isLiked: boolean;
  comments: ForumCommentVO[];
  createTime: string;
}

export interface ForumPostCreateRequest {
  title: string;
  content: string;
  images?: string;
  category: number;
  roomId?: number;
}

export interface ForumCommentCreateRequest {
  postId?: string;
  parentId?: string;
  replyTo?: string;
  content: string;
}

// ==================== 论坛接口 ====================

export function createPost(data: ForumPostCreateRequest) {
  return request.post("/api/owner/community/forum/post", data);
}

export function pagePost(params: {
  current?: number;
  size?: number;
  category?: number;
  keyword?: string;
}) {
  return request.get("/api/owner/community/forum/post/page", { params });
}

export function myPosts(params: { current?: number; size?: number }) {
  return request.get("/api/owner/community/forum/post/mine", { params });
}

export function getPost(id: string) {
  return request.get(`/api/owner/community/forum/post/${id}`);
}

export function createComment(postId: string, data: ForumCommentCreateRequest) {
  return request.post(
    `/api/owner/community/forum/post/${postId}/comment`,
    data,
  );
}

export function getComments(postId: string) {
  return request.get(`/api/owner/community/forum/post/${postId}/comment`);
}

export function like(targetId: string, targetType: number) {
  return request.post("/api/owner/community/forum/like", {
    targetId: Number(targetId),
    targetType,
  });
}

// ==================== 投票枚举映射 ====================

export const VOTE_STATUS_MAP: Record<number, string> = {
  0: "未开始",
  1: "进行中",
  2: "已结束",
};

export const VOTE_TYPE_MAP: Record<number, string> = {
  1: "单选",
  2: "多选",
};

// ==================== 投票类型 ====================

export interface VoteOptionVO {
  id: string;
  content: string;
  voteCount: number;
  sortOrder: number;
}

export interface VoteVO {
  id: string;
  title: string;
  voteType: number;
  voteTypeName: string;
  isAnonymous: number;
  startTime: string;
  endTime: string;
  status: number;
  statusName: string;
  createTime: string;
}

export interface VoteDetailVO extends VoteVO {
  description: string;
  totalVotes: number;
  options: VoteOptionVO[];
  myVotedOptionIds: string[];
}

// ==================== 投票接口 ====================

export function pageVote(params: {
  current?: number;
  size?: number;
  status?: number;
}) {
  return request.get("/api/owner/community/vote/page", { params });
}

export function getVote(id: string) {
  return request.get(`/api/owner/community/vote/${id}`);
}

export function castVote(id: string, optionIds: string[]) {
  return request.post(`/api/owner/community/vote/${id}/cast`, { optionIds });
}

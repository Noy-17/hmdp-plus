import request from '@/utils/request'
//   getBlogById,
// getShopById,
//   getBlogLikes,
//   likeBlog,
//   isFollowed,
//   follow
export const getBlogById = (id) => request.get(`/blog/${id}`)
export const getShopById = (id) => request.get(`/shop/${id}`)
export const getBlogLikes = (id) => request.get(`/blog/likes/${id}`)
export const likeBlog = (id) => request.put(`/blog/like/${id}`)
export const isFollowed = (id) => request.get(`/follow/or/not/${id}`)
export const follow = (id, followed) =>
  request.put(`/follow/${id}/${!followed}`)

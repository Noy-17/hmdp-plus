import request from '@/utils/request'

// 关注用户
export const follow = (id, isFollow) => {
  return request({
    url: `/follow/${id}/${isFollow}`,
    method: 'put'
  })
}

// 取消关注用户
export const unfollow = (id) => {
  return request({
    url: `/follow/${id}`,
    method: 'delete'
  })
}

// 获取用户关注列表
export const getFollows = (id) => {
  return request({
    url: `/follow/${id}`,
    method: 'get'
  })
}

// 获取用户粉丝列表
export const getFans = (id) => {
  return request({
    url: `/follow/fans/${id}`,
    method: 'get'
  })
}

// 获取共同关注列表
export const getCommonFollows = (id) => {
  return request({
    url: `/follow/common/${id}`,
    method: 'get'
  })
}

// 判断是否关注
export const isFollowed = (id) => {
  return request({
    url: `/follow/or/not/${id}`,
    method: 'get'
  })
}

// 获取用户笔记列表
export const getBlogsOfUser = (id, current) => {
  return request({
    url: '/blog/of/user',
    method: 'get',
    params: {
      id,
      current
    }
  })
}

import request from '@/utils/request'

// 用户：获取验证码
// export const userGetCode = (phone) =>
//   request.post('/user/code', { params: { phone } })

export const userGetCode = (phone) =>
  request.post('/user/code', null, { params: { phone } })
// 用户：登录
export const userLogin = (data) => request.post('/user/login', data)

// 首页：获取首页矩阵图数据
export const indexQueryTypes = () => request.get('/shop-type/list')

// 首页：获取博客数据，滑动屏
export const indexQueryHotBlogsScroll = (current) =>
  request.get('/blog/hot', null, { params: { current } })

// 首页：点赞获取博客数据，点击查看更多
export const indexAddLike = (id) => request.put('/blog/like/' + id)
// 首页：获取博客数据
export const indexQueryBlogById = (id) => request.get('/blog/' + id)
// 用户：获取当前登录用户信息
export const getUser = (id) => request.get(id ? `/user/${id}` : '/user/me')
// 用户：获取当前登录用户博客
export const getUserBlog = () => request.get('/blog/of/me')
// 用户：获取用户详情
export const getUserInfo = (id) => request.get(`/user/info/${id}`)

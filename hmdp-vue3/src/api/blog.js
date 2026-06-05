import request from '@/utils/request'

// 上传图片
export function uploadBlogImage(data) {
  return request({
    url: '/upload/blog',
    method: 'post',
    data
  })
}

// 发布博客
export function publishBlog(data) {
  return request({
    url: '/blog',
    method: 'post',
    data
  })
}

// 查询附近的店铺
export function queryNearbyShops(params) {
  return request({
    url: '/shop/of/nearby',
    method: 'get',
    params
  })
}
// 查询店铺: 根据名称 名称为空时 查询所有店铺
export const queryShopsByName = (name) =>
  request.get('/shop/of/name', null, { params: { name } })
// 创建博客
export const createBlog = (data) => request.post('/blog', data)
// 删除博客图片
export const deleteBlogImage = (name) =>
  request.delete('/upload/blog/delete', null, { params: { name } })

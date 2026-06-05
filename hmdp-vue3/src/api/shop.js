import request from '@/utils/request'

export const getShopTypeList = () => request.get('/shop-type/list')

export const getShopList = (params) => request.get('/shop/of/type', { params })
export const getShopById = (id) => request.get('/shop/' + id)

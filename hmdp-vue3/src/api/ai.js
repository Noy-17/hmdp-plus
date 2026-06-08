import request from '@/utils/request'

export const aiSearch = (query) => request.post('/ai/search', { query })

export const aiRecommendVoucher = (userId) => request.post('/ai/recommend/voucher', { userId })

export const aiRecommendShop = (userId) => request.post('/ai/recommend/shop', { userId })

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Location, Timer } from '@element-plus/icons-vue'
import { ElLoading } from 'element-plus'
import { getShopById } from '@/api/shop'
import { getVoucherList, issueSeckillAccessToken, seckillVoucher, getSeckillOrderId, getVoucherOrderIdByVoucherId, cancelVoucherOrder, subscribeVoucher, unsubscribeVoucher, getSubscribeStatusBatch } from '@/api/voucher'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const rate = ref(4.5)

// 响应式数据
const shop = ref({})
const vouchers = ref([])
const seckillInProgress = ref(false)
// 已购状态映射：voucherId -> boolean
const purchasedMap = ref({})
// 订阅状态映射：voucherId -> 0|1|2 （0 未订阅/已取消；1 已订阅；2 自动发券成功）
const subscribeStatusMap = ref({})

const isPurchased = (voucherId) => {
  return !!purchasedMap.value[String(voucherId)]
}

// 查询所有优惠券的已购状态（登录用户）
const refreshPurchaseStatus = async () => {
  try {
    // 未登录则不检查，避免触发 401 重定向
    if (!userStore.token) return
    const list = vouchers.value || []
    await Promise.all(
      list.map(async (v) => {
        if (!v?.id) return
        try {
          const res = await getVoucherOrderIdByVoucherId(String(v.id))
          purchasedMap.value[String(v.id)] = !!res?.data
        } catch (e) {
          // 忽略单项错误，保持既有状态
        }
      })
    )
  } catch (e) {
    // 忽略总体错误
  }
}

// 批量查询订阅状态（登录用户）
const refreshSubscribeStatusBatch = async () => {
  try {
    if (!userStore.token) return
    const ids = (vouchers.value || []).map((v) => v.id).filter(Boolean)
    if (!ids.length) return
    const res = await getSubscribeStatusBatch(ids)
    const arr = Array.isArray(res?.data) ? res.data : []
    const map = {}
    for (const item of arr) {
      if (!item) continue
      const vid = String(item.voucherId)
      const st = Number(item.subscribeStatus)
      map[vid] = Number.isFinite(st) ? st : 0
      // 若状态为自动发券成功，则本地也标记为已购（双保险）
      if (st === 2) {
        purchasedMap.value[vid] = true
      }
    }
    subscribeStatusMap.value = { ...subscribeStatusMap.value, ...map }
  } catch (e) {
    // 静默失败
  }
}

const getSubscribeCode = (voucherId) => {
  return Number(subscribeStatusMap.value[String(voucherId)] ?? 0)
}
const isSubscribed = (voucherId) => getSubscribeCode(voucherId) === 1
const isSubscribeSuccess = (voucherId) => getSubscribeCode(voucherId) === 2

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const pollSeckillOrder = async (orderId, { delay = 800, timeoutMs = 11000 } = {}) => {
  const end = Date.now() + timeoutMs
  while (Date.now() < end) {
    try {
      const { data } = await getSeckillOrderId(String(orderId))
      if (data) {
        ElMessage.success('抢购成功')
        return data
      }
    } catch (e) {
      // 短暂异常，继续重试
    }
    const remaining = end - Date.now()
    await sleep(Math.min(delay, Math.max(0, remaining)))
  }
  ElMessage.warning('确认订单超时，请稍后在订单页查看')
  return null
}

// 获取店铺详情
const queryShopById = async (shopId) => {
  try {
    const { data } = await getShopById(shopId)
    console.log('getShopByIddata', data)
    data.images = data.images.split(',')
    shop.value = data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取店铺详情失败')
  }
}

// 获取优惠券列表（保持后端字段类型为字符串）
const queryVoucher = async (shopId) => {
  try {
    const { data } = await getVoucherList(shopId)
    vouchers.value = data || []
    // 加载后查询每张券的已购状态
    await refreshPurchaseStatus()
    // 批量查询订阅状态
    await refreshSubscribeStatusBatch()
  } catch (error) {
    console.error(error)
    ElMessage.error('获取优惠券列表失败')
  }
}

// 格式化时间
const formatTime = (v) => {
  const b = new Date(v.beginTime)
  const e = new Date(v.endTime)
  return (
    b.getMonth() +
    1 +
    '月' +
    b.getDate() +
    '日 ' +
    b.getHours() +
    ':' +
    formatMinutes(b.getMinutes()) +
    ' ~ ' +
    e.getHours() +
    ':' +
    formatMinutes(e.getMinutes())
  )
}

// 格式化分钟
const formatMinutes = (m) => {
  if (m < 10) m = '0' + m
  return m
}

// 格式化价格，兼容字符串或数字
const formatPrice = (price) => {
  const n = Number(price)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

// 判断是否未开始
const isNotBegin = (v) => {
  return new Date(v.beginTime).getTime() > new Date().getTime()
}

// 判断是否已结束
const isEnd = (v) => {
  return new Date(v.endTime).getTime() < new Date().getTime()
}

// 秒杀抢购（先获取令牌，再携带令牌下单）
const seckill = async (v) => {
  if (!userStore.token) {
    ElMessage.error('请先登录')
    setTimeout(() => {
      router.push('/login')
    }, 200)
    return
  }

  if (isNotBegin(v)) {
    ElMessage.error('优惠券抢购尚未开始！')
    return
  }

  if (isEnd(v)) {
    ElMessage.error('优惠券抢购已经结束！')
    return
  }

  if (Number(v.stock) < 1) {
    ElMessage.error('库存不足，请刷新再试试！')
    return
  }

  // 已购则禁止重复抢购
  if (isPurchased(v.id)) {
    ElMessage.error('您已购买该券，不能重复购买')
    return
  }

  let loading = null
  try {
    if (seckillInProgress.value) {
      // 已在确认中，覆盖层仍在，直接返回以避免重复点击
      return
    }
    seckillInProgress.value = true
    loading = ElLoading.service({
      fullscreen: true,
      lock: true,
      text: '正在确认订单，请稍等…',
      background: 'rgba(0,0,0,0.35)',
      customClass: 'seckill-overlay'
    })
    // 1）先获取访问令牌
    const tokenRes = await issueSeckillAccessToken(v.id)
    if (!tokenRes?.success || !tokenRes?.data) {
      ElMessage.error(tokenRes?.errorMsg || '令牌获取失败，请稍后重试')
      return
    }
    const accessToken = String(tokenRes.data)
    // 2）携带令牌发起下单
    const res = await seckillVoucher(v.id, accessToken)
    // 仅在秒杀接口返回成功时才进行轮询确认订单
    if (res && res.success) {
      const order = await pollSeckillOrder(String(res.data), { delay: 800, timeoutMs: 11000 })
      // 成功拿到订单后，再次查询该券的已购状态并置灰按钮
      if (order) {
        try {
          const check = await getVoucherOrderIdByVoucherId(String(v.id))
          if (check?.data) {
            purchasedMap.value[String(v.id)] = true
          }
        } catch (e) {
          // 忽略错误，不影响既有状态
        }
      }
    } else {
      ElMessage.error(res?.errorMsg || '抢购失败')
      return
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('抢购失败')
  } finally {
    seckillInProgress.value = false
    if (loading) loading.close()
  }
}

// 取消已领取的优惠券
const cancelVoucher = async (v) => {
  if (!userStore.token) {
    ElMessage.error('请先登录')
    setTimeout(() => {
      router.push('/login')
    }, 200)
    return
  }
  if (!v?.id) return
  try {
    const res = await cancelVoucherOrder(String(v.id))
    if (res?.success && String(res.data) === 'true') {
      ElMessage.success('已取消领取')
      // 更新本地状态并刷新列表以同步库存
      purchasedMap.value[String(v.id)] = false
      await queryVoucher(route.params.id)
    } else {
      ElMessage.error(res?.errorMsg || '取消失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('取消失败')
  }
}

// 订阅到券提醒
const subscribeToVoucher = async (v) => {
  if (!userStore.token) {
    ElMessage.error('请先登录')
    setTimeout(() => router.push('/login'), 200)
    return
  }
  if (!v?.id) return
  try {
    const res = await subscribeVoucher(String(v.id))
    if (res?.success) {
      ElMessage.success('已订阅到券提醒')
      subscribeStatusMap.value[String(v.id)] = 1
    } else {
      ElMessage.error(res?.errorMsg || '订阅失败')
    }
  } catch (e) {
    ElMessage.error('订阅失败')
  }
}

// 取消订阅到券提醒
const unsubscribeFromVoucher = async (v) => {
  if (!userStore.token) {
    ElMessage.error('请先登录')
    setTimeout(() => router.push('/login'), 200)
    return
  }
  if (!v?.id) return
  try {
    const res = await unsubscribeVoucher(String(v.id))
    if (res?.success) {
      ElMessage.success('已取消订阅')
      subscribeStatusMap.value[String(v.id)] = 0
    } else {
      ElMessage.error(res?.errorMsg || '取消订阅失败')
    }
  } catch (e) {
    ElMessage.error('取消订阅失败')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 过滤后的优惠券列表
const filteredVouchers = computed(() => {
  return vouchers.value.filter((v) => !isEnd(v))
})

// 初始化
onMounted(() => {
  const shopId = route.params.id
  console.log('shopId', shopId)
  queryShopById(shopId)
  queryVoucher(shopId)
})
</script>

<template>
  <div class="shop-detail-container">
    <!-- 头部 -->
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title"></div>
      <div class="header-share">...</div>
    </div>

    <!-- 店铺信息 -->
    <div class="shop-info-box">
      <div class="shop-title">{{ shop.name }}</div>
      <div class="shop-rate">
        <el-rate
          v-model="shop.score"
          disabled
          :max="5"
          :value="shop.score / 10"
          text-color="#F63"
          show-score
        />
        <span>{{ shop.comments }}条</span>
      </div>
      <div class="shop-rate-info">口味:4.9 环境:4.8 服务:4.7</div>
      <div class="shop-rank">
        <img src="/src/assets/imgs/bd.png" width="63" height="20" alt="" />
        <span>拱墅区好评榜第3名</span>
        <div>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      <div class="shop-images">
        <div v-for="(s, i) in shop.images" :key="i">
          <img :src="s" alt="" />
        </div>
      </div>
      <div class="shop-address">
        <div>
          <el-icon><Location /></el-icon>
        </div>
        <span>{{ shop.address }}</span>
        <div
          style="width: 10px; flex-grow: 2; text-align: center; color: #e1e2e3"
        >
          |
        </div>
        <div style="margin: 0 5px">
          <img
            src="https://p0.meituan.net/travelcube/bf684aa196c870810655e45b1e52ce843484.png@24w_16h_40q"
            alt=""
          />
        </div>
        <div>
          <img
            src="https://p0.meituan.net/travelcube/9277ace32123e0c9f59dedf4407892221566.png@24w_24h_40q"
            alt=""
          />
        </div>
      </div>
    </div>

    <div class="shop-divider"></div>

    <!-- 营业时间 -->
    <div class="shop-open-time">
      <span>
        <el-icon><Timer /></el-icon>
      </span>
      <div>营业时间</div>
      <div>{{ shop.openHours }}</div>
      <span class="line-right">
        查看详情
        <el-icon><ArrowRight /></el-icon>
      </span>
    </div>

    <div class="shop-divider"></div>

    <!-- 优惠券 -->
    <div class="shop-voucher">
      <div>
        <span class="voucher-icon">券</span>
        <span style="font-weight: bold">代金券</span>
      </div>
      <div class="voucher-box" v-for="v in filteredVouchers" :key="v.id">
        <div class="voucher-circle">
          <div class="voucher-b"></div>
          <div class="voucher-b"></div>
          <div class="voucher-b"></div>
        </div>
        <div class="voucher-left">
          <div class="voucher-title">{{ v.title }}</div>
          <div class="voucher-subtitle">{{ v.subTitle }}</div>
          <div class="voucher-price">
            <div>￥ {{ formatPrice(v.payValue) }}</div>
            <span>{{ (v.payValue * 10) / v.actualValue }}折</span>
          </div>
        </div>
        <div class="voucher-right">
          <div v-if="v.type" class="seckill-box">
            <div
              class="voucher-btn"
              :class="{ 'disable-btn': isNotBegin(v) || v.stock < 1 || isPurchased(v.id) }"
              @click="seckill(v)"
            >
              限时抢购
            </div>
            <div class="seckill-stock">
              剩余 <span>{{ v.stock }}</span> 张
            </div>
            <div class="seckill-time">{{ formatTime(v) }}</div>
            <div v-if="isPurchased(v.id)" class="seckill-status">
              <span class="purchased-tag">已购</span>
              <span class="cancel-link" @click="cancelVoucher(v)">取消领取</span>
            </div>
            <!-- 库存为 0 且未购买时，展示订阅到券提醒 -->
            <div v-else-if="Number(v.stock) < 1 && !isPurchased(v.id)" class="subscribe-box">
              <div v-if="isSubscribed(v.id)" class="subscribe-status">
                <span class="subscribed-tag">已订阅到券提醒</span>
                <span class="cancel-subscribe" @click="unsubscribeFromVoucher(v)">取消订阅</span>
              </div>
              <div v-else class="subscribe-status">
                <span class="subscribe-link" @click="subscribeToVoucher(v)">到券提醒</span>
              </div>
            </div>
          </div>
          <div class="voucher-btn" v-else>抢购</div>
        </div>
      </div>
    </div>

    <div class="shop-divider"></div>

    <!-- 评论 -->
    <div class="shop-comments">
      <div class="comments-head">
        <div>网友评价 <span>（119）</span></div>
        <div>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      <div class="comment-tags">
        <div class="tag">味道赞(19)</div>
        <div class="tag">牛肉赞(16)</div>
        <div class="tag">菜品不错(11)</div>
        <div class="tag">回头客(4)</div>
        <div class="tag">分量足(4)</div>
        <div class="tag">停车方便(3)</div>
        <div class="tag">海鲜棒(3)</div>
        <div class="tag">饮品赞(3)</div>
        <div class="tag">朋友聚餐(6)</div>
      </div>
      <div class="comment-list">
        <div class="comment-box" v-for="i in 3" :key="i">
          <div class="comment-icon">
            <img
              src="https://p0.meituan.net/userheadpicbackend/57e44d6eba01aad0d8d711788f30a126549507.jpg%4048w_48h_1e_1c_1l%7Cwatermark%3D0"
              alt=""
            />
          </div>
          <div class="comment-info">
            <div class="comment-user">叶小乙 <span>Lv5</span></div>
            <div style="display: flex">
              打分
              <el-rate disabled v-model="rate"></el-rate>
            </div>
            <div style="padding: 5px 0; font-size: 14px">
              某平台上买的券，价格可以当工作餐吃，虽然价格便宜，但是这家店一点都没有...
            </div>
            <div class="comment-images">
              <img
                src="https://qcloud.dpfile.com/pc/6T7MfXzx7USPIkSy7jzm40qZSmlHUF2jd-FZUL6WpjE9byagjLlrseWxnl1LcbuSGybIjx5eX6WNgCPvcASYAw.jpg"
                alt=""
              />
              <img
                src="https://qcloud.dpfile.com/pc/sZ5q-zgglv4VXEWU71xCFjnLM_jUHq-ylq0GKivtrz3JksWQ1f7oBWZsxm1DWgcaGybIjx5eX6WNgCPvcASYAw.jpg"
                alt=""
              />
              <img
                src="https://qcloud.dpfile.com/pc/xZy6W4NwuRFchlOi43DVLPFsx7KWWvPqifE1JTe_jreqdsBYA9CFkeSm2ZlF0OVmGybIjx5eX6WNgCPvcASYAw.jpg"
                alt=""
              />
              <img
                src="https://qcloud.dpfile.com/pc/xZy6W4NwuRFchlOi43DVLPFsx7KWWvPqifE1JTe_jreqdsBYA9CFkeSm2ZlF0OVmGybIjx5eX6WNgCPvcASYAw.jpg"
                alt=""
              />
            </div>
            <div>浏览641 &nbsp;&nbsp;&nbsp;&nbsp;评论5</div>
          </div>
        </div>
        <div
          style="
            display: flex;
            justify-content: space-between;
            padding: 15px 0;
            border-top: 1px solid #f1f1f1;
            margin-top: 10px;
          "
        >
          <div>查看全部119条评价</div>
          <div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="shop-divider"></div>

    <div class="copyright">copyright ©2021 hmdp.com</div>
  </div>
</template>

<style scoped>
.shop-detail-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-back-btn {
  padding: 5px;
  cursor: pointer;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.header-share {
  padding: 5px;
  cursor: pointer;
}

.shop-info-box {
  padding: 15px;
  background-color: #fff;
}

.shop-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.shop-rate {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.shop-rate-info {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.shop-rank {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.shop-images {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  overflow-x: auto;
}

.shop-images img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
}

.shop-address {
  display: flex;
  align-items: center;
  color: #666;
  font-size: 14px;
}

.shop-divider {
  height: 10px;
  background-color: #f5f5f5;
}

.shop-open-time {
  display: flex;
  align-items: center;
  padding: 15px;
  background-color: #fff;
}

.shop-open-time > div {
  margin-left: 10px;
}

.line-right {
  margin-left: auto;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.shop-voucher {
  padding: 15px;
  background-color: #fff;
}

.voucher-icon {
  background-color: #f63;
  color: #fff;
  padding: 2px 5px;
  border-radius: 2px;
  margin-right: 5px;
}

.voucher-box {
  display: flex;
  margin-top: 10px;
  background-color: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 12px 12px 12px 16px;
}

.voucher-circle {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}

.voucher-b {
  width: 10px;
  height: 10px;
  background-color: #f5f5f5;
  border-radius: 50%;
}

.voucher-left {
  flex: 1;
  padding: 10px;
  border-right: 1px solid #f0f0f0;
}

.voucher-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.voucher-subtitle {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
}

.voucher-price {
  color: #f63;
  font-size: 16px;
  display: flex;
  align-items: baseline;
  gap: 5px;
}

/* 放大价格数字，折扣轻量展示 */
.voucher-price div {
  font-size: 22px;
  font-weight: 700;
}
.voucher-price span {
  font-size: 14px;
  color: #ff6d6d;
  padding: 0 6px;
  line-height: 18px;
  border-radius: 9px;
  background-color: #fff2f0;
}

.voucher-right {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.voucher-btn {
  width: 100%;
  max-width: 240px;
  height: 40px;
  line-height: 40px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  text-align: center;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #fff;
  background: linear-gradient(135deg, #ff7a45 0%, #ff4d4f 100%);
  box-shadow: 0 6px 14px rgba(255, 77, 79, 0.25);
}

.disable-btn {
  opacity: 0.7;
  cursor: not-allowed;
  background: #d9d9d9;
  box-shadow: none;
  color: #fff;
}

.seckill-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.seckill-stock {
  font-size: 12px;
  color: #8a8a8a;
  margin-top: 4px;
}
.seckill-stock span {
  display: inline-block;
  padding: 0 6px;
  line-height: 18px;
  border-radius: 9px;
  background-color: #fff5f0;
  color: #ff4d4f;
  margin: 0 2px;
}

.seckill-time {
  font-size: 12px;
  color: #8a8a8a;
}

.seckill-status {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}
.purchased-tag {
  display: inline-block;
  padding: 2px 8px;
  line-height: 20px;
  border-radius: 10px;
  background-color: #fff2f0;
  color: #ff4d4f;
  font-weight: 600;
}
.cancel-link {
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 12px;
  border: 1px solid #409eff;
  background-color: #409eff;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.cancel-link:hover {
  background-color: #337ecc;
  border-color: #337ecc;
}

/* 订阅到券提醒样式 */
.subscribe-box {
  margin-top: 6px;
}
.subscribe-status {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  font-size: 14px;
  color: #666;
}
.subscribed-tag {
  display: inline-block;
  padding: 2px 8px;
  line-height: 20px;
  border-radius: 10px;
  background-color: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}
.subscribe-link {
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 12px;
  border: 1px solid #ffa940;
  background-color: #ffa940;
  color: #fff;
  cursor: pointer;
}
.subscribe-link:hover {
  background-color: #fa8c16;
  border-color: #fa8c16;
}
.cancel-subscribe {
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
}
.cancel-subscribe:hover {
  background-color: #f5f5f5;
}
.subscribe-tip {
  color: #8a8a8a;
  font-size: 12px;
}

/* 统一订阅相关按钮尺寸与样式，让两个按钮等宽等高 */
.subscribe-link,
.cancel-subscribe,
.subscribed-tag {
  display: inline-block;
  width: 180px;
  height: 36px;
  line-height: 36px;
  border-radius: 18px;
  padding: 0 14px;
  text-align: center;
  font-weight: 600;
}

.shop-comments {
  padding: 15px;
  background-color: #fff;
}

.comments-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.tag {
  padding: 5px 10px;
  background-color: #f5f5f5;
  border-radius: 15px;
  font-size: 14px;
}

.comment-box {
  display: flex;
  margin-bottom: 15px;
}

.comment-icon img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.comment-info {
  flex: 1;
}

.comment-user span {
  color: #666;
  font-size: 12px;
  margin-left: 5px;
}

.comment-images {
  display: flex;
  gap: 5px;
  margin: 10px 0;
}

.comment-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.copyright {
  text-align: center;
  padding: 15px;
  color: #999;
  font-size: 12px;
}

/* 优化：秒杀确认时的加载遮罩与文字样式（全局生效） */
:global(.seckill-overlay) {
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(2px);
  z-index: 2001;
}

:global(.seckill-overlay .el-loading-spinner) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

:global(.seckill-overlay .el-loading-spinner .circular) {
  width: 42px;
  height: 42px;
  stroke: #ff4d4f; /* 与主题色协调 */
}

:global(.seckill-overlay .el-loading-text) {
  margin-top: 2px;
  font-size: 20px; /* 调大文字 */
  font-weight: 700;
  color: #333;
  letter-spacing: 0.3px;
}
</style>

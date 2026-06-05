<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElRate } from 'element-plus'
import { ArrowLeft, ArrowRight, ChatSquare } from '@element-plus/icons-vue'
import {
  getBlogById,
  getShopById,
  getBlogLikes,
  likeBlog,
  isFollowed,
  follow
} from '@/api/blog/blog'
import { getUser } from '@/api/user'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

// 数据定义
const blog = ref({})
const shop = ref({})
const likes = ref([])
const user = ref({})
const followed = ref(false)
const score = ref(4.5)
// 轮播图相关
const swiper = ref(null)
const _width = ref(0)
// const duration = ref(300)
const items = ref([])
const active = ref(0)
const start = ref({ x: 0, y: 0 })
const move = ref({ x: 0, y: 0 })
const sensitivity = ref(60)
const resistance = ref(0.3)
const isMoving = ref(false)

// 初始化数据
const initData = async () => {
  const id = route.params.id
  console.log('id', id)
  try {
    const blogData = await getBlogById(id)
    console.log('博客数据:', blogData)

    if (blogData && blogData.data.images) {
      blogData.data.images = blogData.data.images.split(',')
    } else {
      blogData.data.images = []
    }
    blog.value = blogData.data
    console.log('变换后的博客数据:', blog.value)
    console.log('变换后的博客图片数据:', blog.value.images)

    await nextTick()
    initSwiper()

    await Promise.all([
      queryShopById(blogData.data.shopId),
      queryLikeList(id),
      queryLoginUser()
    ])
  } catch (error) {
    console.error('获取博客详情失败', error)
    ElMessage.error('获取博客详情失败')
  }
}
onMounted(() => {
  initData()
})

// 查询店铺信息
const queryShopById = async (shopId) => {
  try {
    const { data } = await getShopById(shopId)
    console.log('店铺数据:', data)
    if (data && data.images) {
      data.image = data.images.split(',')[0]
    } else {
      data.image = ''
    }
    shop.value = data
  } catch (error) {
    console.error('获取店铺信息失败', error)
    ElMessage.error('获取店铺信息失败')
  }
}

// 查询点赞列表
const queryLikeList = async (id) => {
  try {
    const { data } = await getBlogLikes(id)
    likes.value = data
  } catch (error) {
    console.error('获取点赞列表失败', error)
    ElMessage.error('获取点赞列表失败')
  }
}

// 点赞功能
const addLike = async () => {
  try {
    await likeBlog(blog.value.id)
    const { data } = await getBlogById(blog.value.id)
    data.images = data.images.split(',')
    blog.value = data
    await queryLikeList(blog.value.id)
  } catch (error) {
    console.error('点赞失败', error)
    ElMessage.error('点赞失败')
  }
}

// 查询登录用户
const queryLoginUser = async () => {
  try {
    const { data } = await getUser()
    user.value = data
    if (user.value.id !== blog.value.userId) {
      await checkFollowed()
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
  }
}

// 检查是否关注
const checkFollowed = async () => {
  try {
    const { data } = await isFollowed(blog.value.userId)
    followed.value = data
  } catch (error) {
    console.error('获取关注状态失败', error)
    ElMessage.error('获取关注状态失败')
  }
}

// 关注/取消关注
const handleFollow = async () => {
  try {
    await follow(blog.value.userId, !followed.value)
    ElMessage.success(followed.value ? '已取消关注' : '已关注')
    followed.value = !followed.value
  } catch (error) {
    console.error('关注/取消关注失败', error)
    ElMessage.error('操作失败')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到用户信息页
const toOtherInfo = () => {
  console.log('blog.value.userId', blog.value.userId)
  console.log('user.value.id', user.value.id)
  if (blog.value.userId === user.value.id) {
    router.push('/infoHtml')
  } else {
    router.push(`/InfoOther?id=${blog.value.userId}`)
  }
}

// 轮播图相关方法
const initSwiper = () => {
  const container = swiper.value
  items.value = container.querySelectorAll('.swiper-item')
  updateItemWidth()
  setTransform()
  setTransition('none')
}

const updateItemWidth = () => {
  _width.value =
    swiper.value.offsetWidth || document.documentElement.offsetWidth
}

const setTransform = (offset = 0) => {
  items.value.forEach((item, i) => {
    const distance = (i - active.value) * _width.value + offset
    const transform = `translate3d(${distance}px, 0, 0)`
    item.style.webkitTransform = transform
    item.style.transform = transform
  })
}

const setTransition = (duration = 300) => {
  const transition = typeof duration === 'number' ? `${duration}ms` : duration
  items.value.forEach((item) => {
    item.style.webkitTransition = transition
    item.style.transition = transition
  })
}

const moveStart = (e) => {
  start.value.x = e.touches[0].pageX
  start.value.y = e.touches[0].pageY
  setTransition('none')
}

const moving = (e) => {
  e.preventDefault()
  e.stopPropagation()
  let distanceX = e.touches[0].pageX - start.value.x
  let distanceY = e.touches[0].pageY - start.value.y

  if (Math.abs(distanceX) > Math.abs(distanceY)) {
    isMoving.value = true
    move.value.x = start.value.x + distanceX
    move.value.y = start.value.y + distanceY

    if (
      (active.value === 0 && distanceX > 0) ||
      (active.value === items.value.length - 1 && distanceX < 0)
    ) {
      distanceX = distanceX * resistance.value
    }
    setTransform(distanceX)
  }
}

const moveEnd = (e) => {
  if (isMoving.value) {
    e.preventDefault()
    e.stopPropagation()
    const distance = move.value.x - start.value.x

    if (Math.abs(distance) > sensitivity.value) {
      if (distance < 0) {
        next()
      } else {
        prev()
      }
    } else {
      back()
    }
    reset()
    isMoving.value = false
  }
}

const next = () => {
  go(active.value + 1)
}

const prev = () => {
  go(active.value - 1)
}

const reset = () => {
  start.value.x = 0
  start.value.y = 0
  move.value.x = 0
  move.value.y = 0
}

const back = () => {
  setTransition()
  setTransform()
}

const go = (index) => {
  active.value = index
  if (active.value < 0) {
    active.value = 0
  } else if (active.value > items.value.length - 1) {
    active.value = items.value.length - 1
  }
  setTransition()
  setTransform()
}
</script>

<template>
  <div class="blog-detail">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title"></div>
      <div class="header-share">...</div>
    </div>

    <div class="content">
      <div
        class="blog-info-box"
        ref="swiper"
        @touchstart="moveStart"
        @touchmove="moving"
        @touchend="moveEnd"
      >
        <div class="swiper-item" v-for="(img, i) in blog.images" :key="i">
          <img
            :src="`/src/assets${img}`"
            alt=""
            style="width: 100%"
            height="100%"
          />
        </div>
      </div>

      <div class="basic">
        <div class="basic-icon" @click="toOtherInfo">
          <img
            :src="
              `/src/assets${blog.icon}` ||
              '/src/assets/imgs/icons/default-icon.png'
            "
            alt=""
          />
        </div>
        <div class="basic-info">
          <div class="name">{{ blog.name }}</div>
          <span class="time">{{ formatTime(new Date(blog.createTime)) }}</span>
        </div>
        <div style="width: 20%">
          <div
            class="logout-btn"
            @click="handleFollow"
            v-show="!user || user.id !== blog.userId"
          >
            {{ followed ? '取消关注' : '关注' }}
          </div>
        </div>
      </div>

      <div class="blog-text" v-html="blog.content"></div>

      <div class="shop-basic">
        <div class="shop-icon">
          <img
            :src="
              shop.image
                ? shop.image.startsWith('http')
                  ? shop.image
                  : `/src/assets${shop.image}`
                : '/src/assets/imgs/icons/default-icon.png'
            "
            alt=""
          />
        </div>
        <div style="width: 80%">
          <div class="name">{{ shop.name }}</div>
          <div>
            <el-rate v-model="shop.score" disabled></el-rate>
          </div>
          <div class="shop-avg">￥{{ shop.avgPrice }}/人</div>
        </div>
      </div>

      <div class="zan-box">
        <div>
          <svg
            t="1646634642977"
            class="icon"
            viewBox="0 0 1024 1024"
            version="1.1"
            xmlns="http://www.w3.org/2000/svg"
            p-id="2187"
            width="20"
            height="20"
          >
            <path
              d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
              p-id="2188"
              :fill="blog.isLike ? '#ff6633' : '#82848a'"
            ></path>
          </svg>
        </div>
        <div class="zan-list">
          <div class="user-icon-mini" v-for="u in likes" :key="u.id">
            <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="" />
          </div>
          <div style="margin-left: 10px; text-align: center; line-height: 24px">
            {{ blog.liked }}人点赞
          </div>
        </div>
      </div>

      <div class="blog-divider"></div>

      <div class="blog-comments">
        <div class="comments-head">
          <div>网友评价 <span>（119）</span></div>
          <div>
            <el-icon><ArrowRight /></el-icon>
          </div>
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
                <el-rate disabled v-model="score"></el-rate>
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
      <div class="blog-divider"></div>
    </div>

    <div class="foot">
      <div class="foot-box">
        <div class="foot-view" @click="addLike">
          <svg
            t="1646634642977"
            class="icon"
            viewBox="0 0 1024 1024"
            version="1.1"
            xmlns="http://www.w3.org/2000/svg"
            p-id="2187"
            width="26"
            height="26"
          >
            <path
              d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
              p-id="2188"
              :fill="blog.isLike ? '#ff6633' : '#82848a'"
            ></path>
          </svg>
          <span :class="{ liked: blog.isLike }">{{ blog.liked }}</span>
        </div>
      </div>
      <div style="width: 40%"></div>
      <div class="foot-box">
        <div class="foot-view">
          <el-icon><ChatSquare /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.blog-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  position: relative;
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 15px;
  background: #fff;
  border-bottom: 1px solid #f1f1f1;
  flex-shrink: 0;
}

.header-back-btn {
  font-size: 20px;
  color: #333;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.header-share {
  font-size: 20px;
  color: #333;
}

.content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}

.blog-info-box {
  position: relative;
  width: 100%;
  height: 300px;
  overflow: hidden;
}

.swiper-item {
  position: absolute;
  width: 100%;
  height: 100%;
  transition: transform 0.3s;
}

.swiper-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.basic {
  padding: 15px;
  display: flex;
  align-items: center;
  background: #fff;
}

.basic-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.basic-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.basic-info {
  flex: 1;
}

.basic-info .name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.basic-info .time {
  font-size: 12px;
  color: #999;
}

.logout-btn {
  padding: 5px 10px;
  border: 1px solid #ff6633;
  color: #ff6633;
  border-radius: 15px;
  font-size: 12px;
  text-align: center;
}

.blog-text {
  padding: 15px;
  background: #fff;
  margin-top: 10px;
}

.shop-basic {
  padding: 15px;
  display: flex;
  align-items: center;
  background: #fff;
  margin-top: 10px;
}

.shop-icon {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 10px;
}

.shop-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-avg {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.zan-box {
  padding: 15px;
  display: flex;
  align-items: center;
  background: #fff;
  margin-top: 10px;
}

.zan-list {
  flex: 1;
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.user-icon-mini {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 5px;
}

.user-icon-mini img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-divider {
  height: 10px;
  background: #f1f1f1;
  margin-top: 10px;
}

.blog-comments {
  padding: 15px;
  background: #fff;
}

.comments-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.comment-box {
  display: flex;
  margin-bottom: 15px;
}

.comment-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.comment-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-info {
  flex: 1;
}

.comment-user {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.comment-user span {
  font-size: 12px;
  color: #999;
  margin-left: 5px;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  margin: 10px 0;
}

.comment-images img {
  width: 80px;
  height: 80px;
  margin-right: 5px;
  margin-bottom: 5px;
  object-fit: cover;
}

.foot {
  height: 50px;
  display: flex;
  align-items: center;
  background: #fff;
  border-top: 1px solid #f1f1f1;
  flex-shrink: 0;
}

.foot-box {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.foot-view {
  display: flex;
  align-items: center;
  color: #82848a;
}

.foot-view span {
  font-size: 12px;
  margin-left: 5px;
}

.liked {
  color: #ff6633;
}
</style>

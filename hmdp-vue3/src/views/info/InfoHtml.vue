<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElTabs, ElTabPane } from 'element-plus'
import { ArrowLeft, Edit, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import { getUser, getUserBlog, getUserInfo } from '@/api/user'
import {
  indexQueryHotBlogsScroll,
  indexAddLike,
  indexQueryBlogById
} from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 数据定义
const user = ref({})
const info = ref({})
const blogs = ref([])
const blogs2 = ref([]) // 关注的人的博客
const activeName = ref('1')
const params = reactive({
  minTime: 0, // 上一次拉取到的时间戳
  offset: 0 // 偏移量
})
const isReachBottom = ref(false)

// 生命周期钩子
onMounted(() => {
  queryUser()
})

// 方法定义
const queryUser = () => {
  // 获取用户信息
  getUser()
    .then(({ data }) => {
      // 保存用户
      user.value = data
      // 查询用户详情
      queryUserInfo()
      // 查询用户笔记
      queryBlogs()
    })
    .catch(() => {
      ElMessage.error('获取用户信息失败')
      router.push('/login')
    })
}
const queryBlogs = () => {
  getUserBlog()
    .then(({ data }) => (blogs.value = data))
    .catch(() => {
      ElMessage.error('获取用户博客失败')
    })
}
const queryUserInfo = () => {
  getUserInfo(user.value.id)
    .then(({ data }) => {
      if (!data) {
        return
      }
      // 保存用户详情
      info.value = data
      // 保存到本地
      userStore.setUserInfo(data)
      // sessionStorage.setItem('userInfo', JSON.stringify(data))
    })
    .catch(() => {
      ElMessage.error('获取用户详情失败')
    })
  // 从本地获取用户详情
  // const userInfo = sessionStorage.getItem('userInfo')
  // if (userInfo) {
  //   info.value = JSON.parse(userInfo)
  // }
}

const queryBlogsOfFollow = (clear) => {
  if (clear) {
    params.offset = 0
    params.minTime = new Date().getTime() + 1
  }
  const { offset: os } = params

  // 查询关注的用户的博客
  indexQueryHotBlogsScroll(os)
    .then(({ data }) => {
      if (!data) {
        return
      }
      const { list, ...newParams } = data
      list.forEach((b) => (b.img = b.images.split(',')[0]))
      blogs2.value = clear ? list : blogs2.value.concat(list)
      Object.assign(params, newParams)
    })
    .catch(() => {
      ElMessage.error('获取关注博客失败')
    })
}

const goBack = () => {
  router.back()
}

const toEdit = () => {
  router.push('/infoEdit')
}

const logout = () => {
  // 退出登录
  userStore.resetUserInfo()
  router.push('/login')
}

const handleClick = (tab) => {
  if (tab.props.name === '4') {
    queryBlogsOfFollow(true)
  }
}

const addLike = (b) => {
  indexAddLike(b.id)
    .then(() => {
      queryBlogById(b)
    })
    .catch(() => {
      ElMessage.error('点赞失败')
      b.liked++
    })
}

const queryBlogById = (b) => {
  indexQueryBlogById(b.id)
    .then(({ data }) => {
      b.liked = data.liked
      b.isLike = data.isLike
    })
    .catch(() => {
      ElMessage.error('获取博客详情失败')
      b.liked++
    })
}

const onScroll = (e) => {
  let scrollTop = e.target.scrollTop
  let offsetHeight = e.target.offsetHeight
  let scrollHeight = e.target.scrollHeight
  if (scrollTop === 0) {
    // 到顶部了，查询一次
    queryBlogsOfFollow(true)
  } else if (
    scrollTop + offsetHeight + 1 > scrollHeight &&
    !isReachBottom.value
  ) {
    isReachBottom.value = true
    // 再次查询下一页数据
    queryBlogsOfFollow()
  } else {
    isReachBottom.value = false
  }
}
</script>

<template>
  <div class="info-html">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">个人主页</div>
    </div>

    <div class="basic">
      <div class="basic-icon">
        <img
          :src="
            '/src/assets' + user.icon ||
            '/src/assets/imgs/icons/default-icon.png'
          "
          alt="用户头像"
        />
      </div>
      <div class="basic-info">
        <div class="name">{{ user.nickName }}</div>
        <span>杭州</span>
        <div class="edit-btn" @click="toEdit">编辑资料</div>
      </div>
      <div class="logout-btn" @click="logout">退出登录</div>
    </div>

    <div class="introduce">
      <span v-if="info.introduce">{{ info.introduce }}</span>
      <span v-else
        >添加个人简介，让大家更好的认识你 <el-icon><Edit /></el-icon
      ></span>
    </div>

    <div class="content">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="笔记" name="1">
          <div v-for="b in blogs" :key="b.id" class="blog-item">
            <div class="blog-img">
              <img
                :src="'/src/assets' + b.images.split(',')[0]"
                alt="博客图片"
              />
            </div>
            <div class="blog-info">
              <div class="blog-title">{{ b.title }}</div>
              <div class="blog-liked">
                <img src="/src/assets/imgs/thumbup.png" alt="点赞" />
                {{ b.liked }}
              </div>
              <div class="blog-comments">
                <el-icon><ChatDotRound /></el-icon> {{ b.comments }}
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="评价" name="2">评价</el-tab-pane>
        <el-tab-pane label="粉丝(0)" name="3">粉丝(0)</el-tab-pane>
        <el-tab-pane label="关注(0)" name="4">
          <div class="blog-list" @scroll="onScroll">
            <div class="blog-box" v-for="b in blogs2" :key="b.id">
              <div class="blog-img2" @click="router.push(`/blog/${b.id}`)">
                <img :src="'/src/assets' + b.img" alt="博客图片" />
              </div>
              <div class="blog-title">{{ b.title }}</div>
              <div class="blog-foot">
                <div class="blog-user-icon">
                  <img
                    :src="
                      '/src/assets' + b.icon ||
                      '/src/assets/imgs/icons/default-icon.png'
                    "
                    alt="用户头像"
                  />
                </div>
                <div class="blog-user-name">{{ b.name }}</div>
                <div class="blog-liked" @click="addLike(b)">
                  <svg
                    t="1646634642977"
                    class="icon"
                    viewBox="0 0 1024 1024"
                    version="1.1"
                    xmlns="http://www.w3.org/2000/svg"
                    p-id="2187"
                    width="14"
                    height="14"
                  >
                    <path
                      d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
                      p-id="2188"
                      :fill="b.isLike ? '#ff6633' : '#82848a'"
                    ></path>
                  </svg>
                  {{ b.liked }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="foot-bar">
      <!-- 底部导航栏 -->
    </div>
  </div>
</template>

<style scoped>
.info-html {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
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
  font-size: 16px;
  color: #333;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.basic {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #fff;
  margin-top: 10px;
}

.basic-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
}

.basic-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.basic-info {
  flex: 1;
}

.name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.edit-btn {
  display: inline-block;
  padding: 5px 10px;
  background: #f5f5f5;
  border-radius: 15px;
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.logout-btn {
  padding: 5px 10px;
  background: #f5f5f5;
  border-radius: 15px;
  font-size: 12px;
  color: #666;
}

.introduce {
  padding: 15px;
  background: #fff;
  margin-top: 10px;
  color: #999;
  font-size: 14px;
}

.content {
  flex: 1;
  overflow: hidden;
  background: #fff;
  margin-top: 10px;
}

:deep(.el-tabs__header) {
  margin: 0;
  height: 44px;
}

:deep(.el-tabs__content) {
  height: calc(100% - 44px);
  overflow-y: auto;
}

.blog-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #f1f1f1;
}

.blog-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 10px;
}

.blog-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.blog-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.blog-liked,
.blog-comments {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.blog-liked img {
  width: 14px;
  height: 14px;
  margin-right: 5px;
}

.blog-list {
  height: 100%;
  overflow-y: auto;
}

.blog-box {
  padding: 15px;
  border-bottom: 1px solid #f1f1f1;
}

.blog-img2 {
  width: 100%;
  height: 200px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}

.blog-img2 img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-foot {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.blog-user-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.blog-user-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-user-name {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.foot-bar {
  height: 50px;
  background: #fff;
  border-top: 1px solid #f1f1f1;
  flex-shrink: 0;
}
</style>

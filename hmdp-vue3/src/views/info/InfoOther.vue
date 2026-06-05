<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import { getUser, getUserInfo } from '@/api/user'
import {
  getBlogsOfUser,
  isFollowed,
  follow,
  getCommonFollows
} from '@/api/follow'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 数据定义
const user = ref({})
const loginUser = ref({})
const activeName = ref('1')
const info = ref({})
const blogs = ref([])
const followed = ref(false) // 是否关注了
const commonFollows = ref([]) // 共同关注

// 生命周期钩子
onMounted(() => {
  queryUser()
  queryLoginUser()
})

// 方法定义
const queryBlogs = () => {
  getBlogsOfUser(user.value.id, 1)
    .then(({ data }) => {
      blogs.value = data
    })
    .catch(() => {
      ElMessage.error('获取用户笔记失败')
    })
}

const queryLoginUser = () => {
  // 查询用户信息
  getUser()
    .then(({ data }) => {
      // 保存用户
      loginUser.value = data
    })
    .catch(() => {
      ElMessage.error('获取登录用户信息失败')
    })
}

const queryUser = () => {
  // 查询用户信息
  const id = route.query.id
  console.log('id', id)
  getUser(id)
    .then(({ data }) => {
      // 保存用户
      user.value = data
      console.log('user.value', user.value)
      // 查询用户详情
      queryUserInfo()
      // 查询用户笔记
      queryBlogs()
      // 是否被关注
      isFollowed(id)
        .then(({ data }) => {
          followed.value = data
        })
        .catch(() => {
          ElMessage.error('获取关注状态失败')
        })
    })
    .catch(() => {
      ElMessage.error('获取用户信息失败')
    })
}

const goBack = () => {
  router.back()
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
    })
    .catch(() => {
      ElMessage.error('获取用户详情失败')
    })
}

const queryCommonFollow = () => {
  getCommonFollows(user.value.id)
    .then(({ data }) => {
      commonFollows.value = data
    })
    .catch(() => {
      ElMessage.error('获取共同关注失败')
    })
}

const handleFollow = () => {
  follow(user.value.id, !followed.value)
    .then(() => {
      ElMessage.success(followed.value ? '已取消关注' : '已关注')
      followed.value = !followed.value
    })
    .catch(() => {
      ElMessage.error('操作失败')
    })
}

const handleClick = (tab) => {
  if (tab.props.name === '2') {
    queryCommonFollow()
  }
}

const toOtherInfo = (id) => {
  router.push({
    path: '/info/other',
    query: { id }
  })
}
</script>

<template>
  <div class="info-other">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">&nbsp;&nbsp;&nbsp;</div>
    </div>

    <div class="basic">
      <div class="basic-icon">
        <img
          :src="
            '/src/assets' + user.icon ||
            '/src/assets/imgs/icons/default-icon.png'
          "
          alt=""
        />
      </div>
      <div class="basic-info">
        <div class="name">{{ user.nickName }}</div>
        <span>{{ info.city || '杭州' }}</span>
      </div>
      <div class="logout-btn" @click="handleFollow" style="text-align: center">
        {{ followed ? '取消关注' : '关注' }}
      </div>
    </div>

    <div class="introduce">
      <span v-if="info.introduce">{{ info.introduce }}</span>
      <span v-else>这个人很懒，什么都没有留下</span>
    </div>

    <div class="content">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="笔记" name="1">
          <div v-for="b in blogs" :key="b.id" class="blog-item">
            <div class="blog-img">
              <img :src="'/src/assets' + b.images.split(',')[0]" alt="" />
            </div>
            <div class="blog-info">
              <div class="blog-title" v-html="b.title"></div>
              <div class="blog-liked">
                <img src="/src/assets/imgs/thumbup.png" alt="" /> {{ b.liked }}
              </div>
              <div class="blog-comments">
                <el-icon><ChatDotRound /></el-icon> {{ b.comments }}
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="共同关注" name="2">
          <div>你们都关注了：</div>
          <div class="follow-info" v-for="u in commonFollows" :key="u.id">
            <div class="follow-info-icon" @click="toOtherInfo(u.id)">
              <img
                :src="
                  '/src/assets' + u.icon ||
                  '/src/assets/imgs/icons/default-icon.png'
                "
                alt=""
              />
            </div>
            <div class="follow-info-name">
              <div class="name">{{ u.nickName }}</div>
            </div>
            <div class="follow-info-btn" @click="toOtherInfo(u.id)">
              去主页看看
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
.info-other {
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
  margin-bottom: 10px;
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

.basic-info .name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.basic-info span {
  font-size: 14px;
  color: #999;
}

.logout-btn {
  width: 80px;
  height: 30px;
  line-height: 30px;
  background: #ff6633;
  color: #fff;
  border-radius: 15px;
  font-size: 14px;
}

.introduce {
  padding: 15px;
  background: #fff;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.content {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}

.blog-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #f1f1f1;
}

.blog-img {
  width: 100px;
  height: 100px;
  margin-right: 15px;
  overflow: hidden;
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
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.blog-liked,
.blog-comments {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #999;
}

.blog-liked img {
  width: 16px;
  height: 16px;
  margin-right: 5px;
}

.blog-comments {
  margin-left: 15px;
}

.follow-info {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f1f1f1;
}

.follow-info-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
}

.follow-info-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.follow-info-name {
  flex: 1;
}

.follow-info-name .name {
  font-size: 16px;
  font-weight: bold;
}

.follow-info-btn {
  width: 80px;
  height: 30px;
  line-height: 30px;
  background: #ff6633;
  color: #fff;
  border-radius: 15px;
  font-size: 14px;
  text-align: center;
}

:deep(.el-tabs__header) {
  height: 10%;
}

:deep(.el-tabs__content) {
  height: 90%;
}

:deep(
  .el-tabs--bottom .el-tabs__item.is-bottom:nth-child(2),
  .el-tabs--bottom .el-tabs__item.is-top:nth-child(2),
  .el-tabs--top .el-tabs__item.is-bottom:nth-child(2),
  .el-tabs--top .el-tabs__item.is-top:nth-child(2)
) {
  padding-left: 15px;
}
</style>

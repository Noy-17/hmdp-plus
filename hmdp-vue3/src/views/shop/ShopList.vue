<template>
  <div class="shop-list-container">
    <!-- 头部 -->
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">{{ typeName }}</div>
      <div class="header-search">
        <el-icon><Search /></el-icon>
      </div>
    </div>

    <!-- 排序栏 -->
    <div class="sort-bar">
      <div class="sort-item">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="el-dropdown-link">
            {{ typeName
            }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="t in types" :key="t.id" :command="t">
                {{ t.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="sort-item" @click="sortAndQuery('')">
        距离 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
      </div>
      <div class="sort-item" @click="sortAndQuery('comments')">
        人气 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
      </div>
      <div class="sort-item" @click="sortAndQuery('score')">
        评分 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
      </div>
    </div>

    <!-- 店铺列表 -->
    <div class="shop-list" @scroll="onScroll">
      <div
        class="shop-box"
        v-for="s in shops"
        :key="s.id"
        @click="toDetail(s.id)"
      >
        <div class="shop-img"><img :src="s.images" alt="" /></div>
        <div class="shop-info">
          <div class="shop-title shop-item">{{ s.name }}</div>
          <div class="shop-rate shop-item">
            <el-rate
              v-model="s.score"
              disabled
              :max="5"
              :value="s.score / 10"
              text-color="#F63"
              show-score
            />
            <span>{{ s.comments }}条</span>
          </div>
          <div class="shop-area shop-item">
            <span>{{ s.area }}</span>
            <span v-if="s.distance">
              {{
                s.distance < 1000
                  ? s.distance.toFixed(1) + 'm'
                  : (s.distance / 1000).toFixed(1) + 'km'
              }}
            </span>
          </div>
          <div class="shop-avg shop-item">￥{{ s.avgPrice }}/人</div>
          <div class="shop-address shop-item">
            <el-icon><Location /></el-icon>
            <span>{{ s.address }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, ArrowDown, Search, Location } from '@element-plus/icons-vue'
import { getShopList, getShopTypeList } from '@/api/shop'
import { throttle } from '@/utils/scroll'

const router = useRouter()
const route = useRoute()

// 响应式数据
const types = ref([])
const shops = ref([])
const typeName = ref('')
const isReachBottom = ref(false)
const params = ref({
  typeId: 0,
  current: 1,
  sortBy: '',
  x: 120.149993,
  y: 30.334229
})

// 获取店铺类型列表
const queryTypes = async () => {
  try {
    const { data } = await getShopTypeList()
    types.value = data
    console.log('getShopTypeList', types.value)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取店铺类型失败')
  }
}

// 获取店铺列表
const queryShops = async () => {
  try {
    const { data } = await getShopList(params.value)
    if (!data) return
    data.forEach((s) => (s.images = s.images.split(',')[0]))
    shops.value = shops.value.concat(data)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取店铺列表失败')
  }
}

// 处理类型选择
const handleCommand = (type) => {
  console.log('handleCommand', type)
  router.push({
    path: '/shoplist',
    query: {
      type: type.id,
      name: type.name
    }
  })
}

// 排序和查询
const sortAndQuery = (sortBy) => {
  params.value.sortBy = sortBy
  queryShops()
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到店铺详情
const toDetail = (id) => {
  router.push(`/shopDetail/${id}`)
}

// 滚动加载
const onScroll = throttle((e) => {
  const { scrollTop, offsetHeight, scrollHeight } = e.target
  if (scrollTop + offsetHeight + 1 > scrollHeight && !isReachBottom.value) {
    isReachBottom.value = true
    params.value.current++
    queryShops()
  } else {
    isReachBottom.value = false
  }
}, 200)

// 初始化
const initData = () => {
  params.value.typeId = Number(route.query.type) || 0
  typeName.value = route.query.name || ''
  shops.value = [] // 清空店铺列表
  params.value.current = 1 // 重置页码
  queryTypes()
  queryShops()
}

// 监听路由参数变化
watch(
  () => route.query,
  () => {
    initData()
  }
)
onMounted(() => {
  initData()
})
</script>

<style scoped>
.shop-list-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
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

.header-search {
  padding: 5px;
  cursor: pointer;
}

.sort-bar {
  display: flex;
  background-color: #fff;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.sort-item {
  flex: 1;
  text-align: center;
  cursor: pointer;
}

.shop-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.shop-box {
  display: flex;
  padding: 10px;
  margin-bottom: 10px;
  background-color: #fff;
  border-radius: 8px;
  cursor: pointer;
}

.shop-img {
  width: 80px;
  height: 80px;
  margin-right: 10px;
}

.shop-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.shop-info {
  flex: 1;
}

.shop-item {
  margin-bottom: 5px;
}

.shop-title {
  font-size: 16px;
  font-weight: bold;
}

.shop-rate {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shop-area {
  color: #666;
  font-size: 14px;
}

.shop-avg {
  color: #f63;
  font-size: 14px;
}

.shop-address {
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>

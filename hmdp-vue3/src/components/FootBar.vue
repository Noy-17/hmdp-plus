<script setup>
import router from '@/router'
import { House, Location, ChatDotRound, User } from '@element-plus/icons-vue'
defineProps({
  activeBtn: {
    type: Number,
    default: 1
  }
})
const toPage = (i) => {
  if (i === 0) {
    router.push('/blogEdit')
  } else if (i === 4) {
    router.push('/InfoHtml')
  } else if (i === 1) {
    router.push('/')
  }
}
</script>

<template>
  <div class="foot">
    <div
      class="foot-box"
      :class="{ active: activeBtn === 1 }"
      @click="toPage(1)"
    >
      <div class="foot-view"><el-icon><House /></el-icon></div>
      <div class="foot-text">首页</div>
    </div>
    <div
      class="foot-box"
      :class="{ active: activeBtn === 2 }"
      @click="toPage(2)"
    >
      <div class="foot-view"><el-icon><Location /></el-icon></div>
      <div class="foot-text">地图</div>
    </div>
    <div class="foot-box" @click="toPage(0)">
      <img class="add-btn" src="../assets/imgs/add.png" alt="" />
    </div>
    <div
      class="foot-box"
      :class="{ active: activeBtn === 3 }"
      @click="toPage(3)"
    >
      <div class="foot-view"><el-icon><ChatDotRound /></el-icon></div>
      <div class="foot-text">消息</div>
    </div>
    <div
      class="foot-box"
      :class="{ active: activeBtn === 4 }"
      @click="toPage(4)"
    >
      <div class="foot-view"><el-icon><User /></el-icon></div>
      <div class="foot-text">我的</div>
    </div>
  </div>
</template>

<style scoped>
.foot-view :deep(.el-icon){
  font-size: 22px;
}
</style>

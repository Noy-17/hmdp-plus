<script setup>
import { onMounted } from 'vue'
import AiChatWidget from '@/components/AiChatWidget.vue'
import { useUserStore } from '@/stores'
import { getUser } from '@/api/user'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.token && !userStore.userInfo?.id) {
    try {
      const userRes = await getUser()
      if (userRes.success && userRes.data) {
        userStore.setUserInfo(userRes.data)
      }
    } catch (e) {
      // 静默失败，用户操作时再提示
    }
  }
})
</script>

<template>
  <div>
    <router-view></router-view>
    <AiChatWidget />
  </div>
</template>

<style scoped></style>

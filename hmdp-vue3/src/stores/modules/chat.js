import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { aiSearch, aiRecommendVoucher, aiRecommendShop } from '@/api/ai'
import { getUser } from '@/api/user'
import { useUserStore } from './user'

export const useChatStore = defineStore('Hmdp-Chat', () => {
  const messages = ref([])
  const isExpanded = ref(false)
  const activeTab = ref('search')
  const isLoading = ref(false)
  const error = ref(null)

  let nextId = 1

  const currentMessages = computed(() =>
    messages.value.filter((m) => m.tab === activeTab.value)
  )

  function addMessage(role, content, opts = {}) {
    messages.value.push({
      id: nextId++,
      role,
      content,
      tab: activeTab.value,
      timestamp: Date.now(),
      ...opts
    })
  }

  async function ensureUserId() {
    const userStore = useUserStore()
    if (userStore.userInfo?.id) return userStore.userInfo.id
    if (!userStore.token) return null
    // token 存在但 userInfo 为空，主动补齐
    try {
      const userRes = await getUser()
      if (userRes.success && userRes.data) {
        userStore.setUserInfo(userRes.data)
        return userRes.data.id
      }
    } catch (e) {
      // 静默失败
    }
    return null
  }

  async function sendMessage(text) {
    error.value = null
    isLoading.value = true

    // Add user message (if text provided)
    if (text && text.trim()) {
      addMessage('user', text.trim())
    }

    try {
      if (activeTab.value === 'search') {
        const res = await aiSearch(text || '')
        if (res.success) {
          addMessage('ai', res.data.extractedIntent || '搜索结果如下：', {
            shops: res.data.shops || []
          })
        } else {
          addMessage('ai', res.errorMsg || '搜索失败，请稍后再试')
        }
      } else if (activeTab.value === 'voucher') {
        const userId = await ensureUserId()
        if (!userId) {
          addMessage('ai', '请先登录后再获取券推荐')
          isLoading.value = false
          return
        }
        const res = await aiRecommendVoucher(userId)
        if (res.success) {
          const list = res.data || []
          if (list.length === 0) {
            addMessage('ai', '暂无可推荐的优惠券')
          } else {
            addMessage('ai', '根据您的偏好，为您推荐以下优惠券：', {
              recommends: list
            })
          }
        } else {
          addMessage('ai', res.errorMsg || '推荐失败，请稍后再试')
        }
      } else if (activeTab.value === 'shop') {
        const userId = await ensureUserId()
        if (!userId) {
          addMessage('ai', '请先登录后再获取商铺推荐')
          isLoading.value = false
          return
        }
        const res = await aiRecommendShop(userId)
        if (res.success) {
          const list = res.data || []
          if (list.length === 0) {
            addMessage('ai', '暂无推荐的商铺')
          } else {
            addMessage('ai', '根据您的偏好，为您推荐以下商铺：', {
              recommends: list
            })
          }
        } else {
          addMessage('ai', res.errorMsg || '推荐失败，请稍后再试')
        }
      }
    } catch (e) {
      error.value = e?.message || '网络异常'
      addMessage('ai', '服务繁忙，请稍后再试')
    } finally {
      isLoading.value = false
    }
  }

  function togglePanel() {
    isExpanded.value = !isExpanded.value
  }

  function collapsePanel() {
    isExpanded.value = false
  }

  function clearMessages() {
    messages.value = messages.value.filter((m) => m.tab !== activeTab.value)
  }

  function switchTab(tab) {
    activeTab.value = tab
    error.value = null
  }

  return {
    messages,
    isExpanded,
    activeTab,
    isLoading,
    error,
    currentMessages,
    sendMessage,
    togglePanel,
    collapsePanel,
    clearMessages,
    switchTab
  }
})

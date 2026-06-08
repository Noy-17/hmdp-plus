<script setup>
import { ref, watch, nextTick } from 'vue'
import { Close, Promotion } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores'
import AiChatMessage from './AiChatMessage.vue'

const chatStore = useChatStore()

const inputText = ref('')
const scrollRef = ref(null)
const inputRef = ref(null)

const tabExamples = {
  search: '推荐西湖区评分高的火锅店，人均100以内',
  voucher: '帮我推荐适合我的优惠券',
  shop: '根据我的偏好推荐商铺'
}

function getExample() {
  return tabExamples[chatStore.activeTab] || ''
}

function canSend() {
  if (chatStore.isLoading) return false
  if (chatStore.activeTab === 'search') return inputText.value.trim().length > 0
  return true // recommend tabs work without text
}

async function handleSend() {
  if (!canSend()) return
  const text = inputText.value.trim()
  inputText.value = ''
  await chatStore.sendMessage(text)
  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  if (scrollRef.value) {
    const wrap = scrollRef.value.wrapRef || scrollRef.value.$el?.querySelector('.el-scrollbar__wrap')
    if (wrap) wrap.scrollTop = wrap.scrollHeight
  }
}

watch(() => chatStore.isExpanded, (val) => {
  if (val) {
    nextTick(() => {
      inputRef.value?.focus()
      scrollToBottom()
    })
  }
})

watch(() => chatStore.activeTab, () => {
  nextTick(scrollToBottom)
})
</script>

<template>
  <div class="ai-panel-inner">
    <!-- Header -->
    <div class="ai-panel-header">
      <div class="ai-panel-title">
        <el-icon :size="18"><Promotion /></el-icon>
        <span>AI 助手</span>
      </div>
      <el-button
        :icon="Close"
        circle
        size="small"
        text
        @click="chatStore.collapsePanel()"
      />
    </div>

    <!-- Tabs -->
    <el-tabs
      :model-value="chatStore.activeTab"
      class="ai-tabs"
      @tab-change="chatStore.switchTab"
    >
      <el-tab-pane label="智能搜索" name="search" />
      <el-tab-pane label="券推荐" name="voucher" />
      <el-tab-pane label="商铺推荐" name="shop" />
    </el-tabs>

    <!-- Messages -->
    <el-scrollbar ref="scrollRef" class="ai-messages">
      <!-- Empty / welcome -->
      <div v-if="chatStore.currentMessages.length === 0 && !chatStore.isLoading" class="ai-welcome">
        <div class="ai-welcome-icon">
          <el-icon :size="36"><Promotion /></el-icon>
        </div>
        <div class="ai-welcome-title">你好，我是 AI 助手</div>
        <div class="ai-welcome-desc">
          <template v-if="chatStore.activeTab === 'search'">
            用自然语言描述你想找的商铺，<br />比如区域、类型、价格、评分等
          </template>
          <template v-else-if="chatStore.activeTab === 'voucher'">
            基于你的消费偏好，<br />智能推荐最适合你的优惠券
          </template>
          <template v-else>
            结合你的口味和好友动态，<br />为你发现值得一去的商铺
          </template>
        </div>
        <div class="ai-welcome-example">
          试试问我：{{ getExample() }}
        </div>
      </div>

      <!-- Messages -->
      <AiChatMessage
        v-for="msg in chatStore.currentMessages"
        :key="msg.id"
        :message="msg"
      />

      <!-- Loading -->
      <div v-if="chatStore.isLoading" class="ai-typing">
        <span class="ai-typing-dot"></span>
        <span class="ai-typing-dot"></span>
        <span class="ai-typing-dot"></span>
        <span class="ai-typing-text">AI 正在思考...</span>
      </div>
    </el-scrollbar>

    <!-- Input -->
    <div class="ai-input-area">
      <el-input
        ref="inputRef"
        v-model="inputText"
        :placeholder="getExample()"
        :disabled="chatStore.isLoading"
        size="default"
        maxlength="200"
        @keyup.enter="handleSend"
      >
        <template #append>
          <el-button
            :type="canSend() ? 'primary' : 'default'"
            :disabled="!canSend()"
            :loading="chatStore.isLoading"
            @click="handleSend"
          >
            {{ chatStore.activeTab === 'search' ? '搜索' : '推荐' }}
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<style scoped>
.ai-panel-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f6fa;
}

/* Header */
.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.ai-panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.ai-panel-title .el-icon {
  color: #ff6633;
}

/* Tabs */
.ai-tabs {
  flex-shrink: 0;
  background: #fff;
  padding: 0 8px;
}
.ai-tabs :deep(.el-tabs__header) {
  margin: 0;
}
.ai-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  height: 38px;
  line-height: 38px;
}
.ai-tabs :deep(.el-tabs__active-bar) {
  background-color: #ff6633;
}
.ai-tabs :deep(.el-tabs__item.is-active) {
  color: #ff6633;
}

/* Messages */
.ai-messages {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
}

/* Welcome */
.ai-welcome {
  text-align: center;
  padding: 32px 16px 16px;
}
.ai-welcome-icon {
  color: #ff6633;
  margin-bottom: 12px;
  opacity: 0.7;
}
.ai-welcome-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.ai-welcome-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.7;
  margin-bottom: 16px;
}
.ai-welcome-example {
  font-size: 12px;
  color: #ff6633;
  background: #fff5f0;
  padding: 8px 14px;
  border-radius: 8px;
  display: inline-block;
  max-width: 260px;
}

/* Typing indicator */
.ai-typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
}
.ai-typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ccc;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}
.ai-typing-dot:nth-child(1) { animation-delay: -0.32s; }
.ai-typing-dot:nth-child(2) { animation-delay: -0.16s; }
.ai-typing-dot:nth-child(3) { animation-delay: 0s; }
@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.ai-typing-text {
  font-size: 12px;
  color: #bbb;
  margin-left: 4px;
}

/* Input */
.ai-input-area {
  padding: 10px 12px;
  background: #fff;
  border-top: 1px solid #eee;
  flex-shrink: 0;
}
.ai-input-area :deep(.el-input-group__append) {
  padding: 0 4px;
  background: transparent;
  border: none;
}
.ai-input-area :deep(.el-input__wrapper) {
  border-radius: 20px 0 0 20px;
  background: #f5f6fa;
  box-shadow: none;
  padding-left: 16px;
}
.ai-input-area :deep(.el-input-group__append .el-button) {
  border-radius: 0 20px 20px 0;
  margin-left: -1px;
  height: 100%;
}
</style>

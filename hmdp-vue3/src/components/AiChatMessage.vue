<script setup>
import { useRouter } from 'vue-router'
import AiShopCard from './AiShopCard.vue'

const router = useRouter()

defineProps({
  message: {
    type: Object,
    required: true
  }
})

function goShop(id) {
  if (id) router.push(`/shopDetail/${id}`)
}
</script>

<template>
  <div
    class="ai-message"
    :class="{ 'ai-message--user': message.role === 'user', 'ai-message--ai': message.role === 'ai' }"
  >
    <div class="ai-message-bubble">
      <div class="ai-message-text">{{ message.content }}</div>

      <!-- Shop cards (search results) -->
      <div v-if="message.shops && message.shops.length > 0" class="ai-message-cards">
        <AiShopCard
          v-for="shop in message.shops"
          :key="shop.id"
          :shop="shop"
        />
      </div>

      <!-- Recommend results -->
      <div v-if="message.recommends && message.recommends.length > 0" class="ai-message-recommends">
        <div
          v-for="item in message.recommends"
          :key="item.id"
          class="ai-recommend-item"
          @click="goShop(item.shopId || item.id)"
        >
          <div class="ai-recommend-header">
            <span class="ai-recommend-name">{{ item.name }}</span>
            <el-tag v-if="item.score" size="small" type="warning" round>
              {{ item.score }}分
            </el-tag>
          </div>
          <div v-if="item.reason" class="ai-recommend-reason">
            {{ item.reason }}
          </div>
          <div class="ai-recommend-action">点击查看详情 →</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-message {
  display: flex;
  margin-bottom: 14px;
}
.ai-message--user {
  justify-content: flex-end;
}
.ai-message--ai {
  justify-content: flex-start;
}

.ai-message-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
.ai-message--user .ai-message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-message--ai .ai-message-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.ai-message-text {
  white-space: pre-wrap;
}

.ai-message-cards {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-message-recommends {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-recommend-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.ai-recommend-item:hover {
  background: #f0f0f0;
}
.ai-recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.ai-recommend-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.ai-recommend-reason {
  font-size: 12px;
  color: #888;
  line-height: 1.5;
}
.ai-recommend-action {
  font-size: 11px;
  color: #409eff;
  margin-top: 4px;
}
</style>

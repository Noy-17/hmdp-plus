<script setup>
import { useRouter } from 'vue-router'
import { Star, Location } from '@element-plus/icons-vue'

const props = defineProps({
  shop: {
    type: Object,
    required: true
  }
})

const router = useRouter()

function goDetail() {
  if (props.shop.id) {
    router.push(`/shopDetail/${props.shop.id}`)
  }
}
</script>

<template>
  <div class="ai-shop-card" @click="goDetail">
    <div class="ai-shop-card-body">
      <div class="ai-shop-card-info">
        <div class="ai-shop-card-name">{{ shop.name }}</div>
        <div class="ai-shop-card-meta">
          <span v-if="shop.score" class="ai-shop-card-score">
            <el-icon :size="12"><Star /></el-icon>
            {{ shop.score }}分
          </span>
          <span v-if="shop.avgPrice">人均 ¥{{ shop.avgPrice }}</span>
          <span v-if="shop.area || shop.address" class="ai-shop-card-addr">
            <el-icon :size="12"><Location /></el-icon>
            {{ shop.area || shop.address }}
          </span>
        </div>
        <div v-if="shop.distance != null" class="ai-shop-card-distance">
          距您 {{ (shop.distance / 1000).toFixed(1) }}km
        </div>
      </div>
      <div class="ai-shop-card-arrow">
        <span>查看</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-shop-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.ai-shop-card:hover {
  background: #f0f0f0;
}
.ai-shop-card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ai-shop-card-info {
  flex: 1;
  min-width: 0;
}
.ai-shop-card-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ai-shop-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #888;
}
.ai-shop-card-score {
  color: #ff6633;
  display: flex;
  align-items: center;
  gap: 2px;
}
.ai-shop-card-addr {
  display: flex;
  align-items: center;
  gap: 2px;
}
.ai-shop-card-distance {
  font-size: 11px;
  color: #aaa;
  margin-top: 2px;
}
.ai-shop-card-arrow {
  font-size: 11px;
  color: #409eff;
  flex-shrink: 0;
  margin-left: 8px;
}
</style>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores'
import AiChatPanel from './AiChatPanel.vue'

const chatStore = useChatStore()
const isMobile = ref(window.innerWidth < 768)

function onResize() {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))
</script>

<template>
  <div class="ai-chat-widget">
    <Transition name="fab-fade">
      <div
        v-show="!chatStore.isExpanded"
        class="ai-fab"
        @click="chatStore.togglePanel()"
      >
        <el-icon :size="20"><ChatDotRound /></el-icon>
      </div>
    </Transition>

    <Transition name="backdrop-fade">
      <div
        v-show="chatStore.isExpanded"
        class="ai-backdrop"
        @click="chatStore.collapsePanel()"
      ></div>
    </Transition>

    <Transition name="ai-panel">
      <div
        v-show="chatStore.isExpanded"
        class="ai-panel"
        :class="{ 'ai-panel--mobile': isMobile }"
      >
        <AiChatPanel />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.ai-chat-widget {
  position: fixed;
  z-index: 1000;
}

/* FAB — floating triangle */
.ai-fab {
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 48px;
  height: 48px;
  background: #ff6633;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 102, 51, 0.45);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 1001;
}
.ai-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 102, 51, 0.55);
}
.ai-fab:active {
  transform: scale(0.95);
}

/* Backdrop */
.ai-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1000;
}

/* Panel — desktop: floating window */
.ai-panel {
  position: fixed;
  bottom: 90px;
  right: 20px;
  width: 380px;
  height: 520px;
  max-height: calc(100vh - 120px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1001;
}

/* Panel — mobile: bottom drawer */
.ai-panel--mobile {
  bottom: 0;
  left: 0;
  right: 0;
  width: auto;
  height: 70vh;
  max-height: 70vh;
  border-radius: 16px 16px 0 0;
}

/* Transitions */
.fab-fade-enter-active,
.fab-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fab-fade-enter-from,
.fab-fade-leave-to {
  opacity: 0;
  transform: scale(0.5);
}

.backdrop-fade-enter-active,
.backdrop-fade-leave-active {
  transition: opacity 0.25s ease;
}
.backdrop-fade-enter-from,
.backdrop-fade-leave-to {
  opacity: 0;
}

.ai-panel-enter-active,
.ai-panel-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.ai-panel-enter-from,
.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
.ai-panel--mobile.ai-panel-enter-from,
.ai-panel--mobile.ai-panel-leave-to {
  transform: translateY(100%);
}
</style>

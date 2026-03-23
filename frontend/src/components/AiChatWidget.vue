<template>
  <div class="ai-chat-widget">
    <Transition name="widget-panel">
      <section
        v-if="isAssistantOpen"
        class="assistant-panel"
        aria-label="AI 智能航班助手"
      >
        <header class="assistant-panel__header">
          <div class="assistant-panel__title-group">
            <div class="assistant-panel__badge">AI</div>
            <div>
              <h3 class="assistant-panel__title">智能航班助手</h3>
              <p class="assistant-panel__subtitle">实时问票、航线建议、多轮追问</p>
            </div>
          </div>

          <button
            type="button"
            class="assistant-panel__close"
            aria-label="关闭助手"
            @click="toggleAssistant"
          >
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div class="assistant-panel__body">
          <FlightAssistant />
        </div>
      </section>
    </Transition>

    <button
      type="button"
      class="assistant-fab"
      :class="{ 'assistant-fab--open': isAssistantOpen }"
      :aria-expanded="isAssistantOpen"
      :aria-label="isAssistantOpen ? '关闭智能航班助手' : '打开智能航班助手'"
      @click="toggleAssistant"
    >
      <span class="assistant-fab__pulse" aria-hidden="true"></span>
      <el-icon class="assistant-fab__icon">
        <Close v-if="isAssistantOpen" />
        <ChatDotRound v-else />
      </el-icon>
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import FlightAssistant from './FlightAssistant.vue'

/**
 * 控制悬浮助手的开关状态。
 * 外层组件只负责容器与交互，不介入 FlightAssistant 内部聊天逻辑。
 */
const isAssistantOpen = ref(false)

function toggleAssistant() {
  isAssistantOpen.value = !isAssistantOpen.value
}
</script>

<style scoped>
.ai-chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: var(--z-modal, 1050);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  pointer-events: none;
}

.ai-chat-widget > * {
  pointer-events: auto;
}

.assistant-fab {
  position: relative;
  width: 64px;
  height: 64px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  box-shadow:
    0 16px 32px rgba(37, 99, 235, 0.24),
    0 6px 16px rgba(15, 23, 42, 0.18);
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal),
    filter var(--transition-fast);
  overflow: hidden;
}

.assistant-fab:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow:
    0 22px 40px rgba(37, 99, 235, 0.28),
    0 10px 20px rgba(15, 23, 42, 0.22);
  filter: saturate(1.08);
}

.assistant-fab:active {
  transform: translateY(-1px) scale(0.98);
}

.assistant-fab--open {
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
}

.assistant-fab__icon {
  position: relative;
  z-index: 2;
  font-size: 28px;
}

.assistant-fab__pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.28), transparent 42%);
}

.assistant-panel {
  width: 380px;
  height: 600px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  backdrop-filter: blur(16px);
  box-shadow:
    0 24px 48px rgba(15, 23, 42, 0.18),
    0 8px 18px rgba(15, 23, 42, 0.08);
}

.assistant-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
}

.assistant-panel__title-group {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.assistant-panel__badge {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: var(--font-weight-bold);
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  border: 1px solid rgba(37, 99, 235, 0.14);
}

.assistant-panel__title {
  margin: 0 0 2px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.assistant-panel__subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.assistant-panel__close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border-primary);
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-secondary);
  background: rgba(255, 255, 255, 0.9);
  transition:
    transform var(--transition-fast),
    color var(--transition-fast),
    border-color var(--transition-fast),
    background var(--transition-fast);
}

.assistant-panel__close:hover {
  color: var(--color-primary);
  border-color: rgba(37, 99, 235, 0.22);
  background: #ffffff;
  transform: rotate(90deg);
}

.assistant-panel__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  background: transparent;
}

.assistant-panel__body::-webkit-scrollbar {
  width: 8px;
}

.assistant-panel__body::-webkit-scrollbar-track {
  background: transparent;
}

.assistant-panel__body::-webkit-scrollbar-thumb {
  background: var(--color-border-secondary);
  border-radius: 999px;
}

.assistant-panel__body :deep(.flight-assistant-page) {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.assistant-panel__body :deep(.page-header) {
  display: none;
}

.assistant-panel__body :deep(.assistant-panel-section) {
  flex: 1;
  margin-top: 0;
  padding: 0;
}

.assistant-panel__body :deep(.container) {
  min-height: 100%;
  max-width: none;
  padding: 0;
}

.assistant-panel__body :deep(.assistant-shell) {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.assistant-panel__body :deep(.assistant-toolbar) {
  display: none;
}

.assistant-panel__body :deep(.chat-viewport) {
  flex: 1;
  height: auto;
  min-height: 320px;
  max-height: none;
}

.assistant-panel__body :deep(.quick-prompts),
.assistant-panel__body :deep(.composer-card) {
  flex-shrink: 0;
}

.assistant-panel__body :deep(.composer-card) {
  position: sticky;
  bottom: 0;
  z-index: 1;
  background: #ffffff;
}

.assistant-panel__body :deep(.composer-card) {
  border-top: 1px solid var(--color-border-primary);
}

.widget-panel-enter-active,
.widget-panel-leave-active {
  transition:
    opacity 220ms ease,
    transform 220ms ease,
    scale 220ms ease;
  transform-origin: bottom right;
}

.widget-panel-enter-from,
.widget-panel-leave-to {
  opacity: 0;
  transform: translateY(18px) scale(0.92);
}

@media (max-width: 767px) {
  .ai-chat-widget {
    right: 16px;
    bottom: 16px;
  }

  .assistant-fab {
    width: 58px;
    height: 58px;
  }

  .assistant-panel {
    position: fixed;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
    margin-bottom: 0;
    border-radius: 0;
    border: none;
  }

  .assistant-panel__header {
    padding: 14px 16px;
  }

  .widget-panel-enter-active,
  .widget-panel-leave-active {
    transform-origin: bottom center;
  }

  .widget-panel-enter-from,
  .widget-panel-leave-to {
    opacity: 0;
    transform: translateY(24px);
  }
}
</style>

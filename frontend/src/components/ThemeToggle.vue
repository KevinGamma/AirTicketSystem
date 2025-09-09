<template>
  <div class="theme-toggle">
    <el-dropdown 
      trigger="click" 
      @command="handleThemeChange"
      placement="bottom-end"
    >
      <el-button 
        circle 
        size="large"
        class="theme-toggle-btn"
        :aria-label="$t('nav.settings')"
      >
        <el-icon>
          <component :is="currentThemeIcon" />
        </el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu class="theme-dropdown">
          <el-dropdown-item 
            command="light" 
            :class="{ 'is-active': currentTheme === 'light' }"
          >
            <div class="theme-option">
              <el-icon><Sunny /></el-icon>
              <span>{{ $t('theme.light') }}</span>
              <el-icon v-if="currentTheme === 'light'" class="check-icon"><Check /></el-icon>
            </div>
          </el-dropdown-item>
          <el-dropdown-item 
            command="dark" 
            :class="{ 'is-active': currentTheme === 'dark' }"
          >
            <div class="theme-option">
              <el-icon><Moon /></el-icon>
              <span>{{ $t('theme.dark') }}</span>
              <el-icon v-if="currentTheme === 'dark'" class="check-icon"><Check /></el-icon>
            </div>
          </el-dropdown-item>
          <el-dropdown-item 
            command="auto" 
            :class="{ 'is-active': currentTheme === 'auto' }"
          >
            <div class="theme-option">
              <el-icon><Monitor /></el-icon>
              <span>{{ $t('theme.auto') }}</span>
              <el-icon v-if="currentTheme === 'auto'" class="check-icon"><Check /></el-icon>
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import { Sunny, Moon, Monitor, Check } from '@element-plus/icons-vue'

export default {
  name: 'ThemeToggle',
  components: {
    Sunny,
    Moon,
    Monitor,
    Check
  },
  computed: {
    ...mapGetters(['currentTheme', 'isDarkMode']),
    currentThemeIcon() {
      if (this.currentTheme === 'light') return 'Sunny'
      if (this.currentTheme === 'dark') return 'Moon'
      return 'Monitor'
    }
  },
  methods: {
    ...mapActions(['setTheme']),
    handleThemeChange(theme) {
      this.setTheme(theme)
    }
  }
}
</script>

<style scoped>
.theme-toggle-btn {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  transition: var(--transition-fast);
}

.theme-toggle-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.theme-dropdown {
  min-width: 150px;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
}

.theme-option .el-icon {
  font-size: var(--icon-sm);
}

.check-icon {
  margin-left: auto;
  color: var(--color-primary);
}

:deep(.el-dropdown-menu__item.is-active) {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

:deep(.el-dropdown-menu__item:hover) {
  background: var(--color-bg-hover);
}

.dark .theme-toggle-btn {
  color: var(--color-text-secondary);
}

.dark .theme-toggle-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}
</style>
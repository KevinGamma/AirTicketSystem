<template>
  <div id="app">
    <AppHeader />
    <AiChatWidget v-if="showAiChatWidget" />

    <div class="app-layout">
      <aside class="app-sidebar" v-if="isLoggedIn && showSidebar">
        <nav class="sidebar-nav">
          <div class="sidebar-header">
            <span class="sidebar-title">菜单</span>
          </div>
          <el-menu
            :default-active="$route.path"
            router
            class="sidebar-menu"
            :collapse="sidebarCollapsed"
          >
            <el-menu-item index="/dashboard">
              <el-icon><House /></el-icon>
              <span>{{ $t('nav.dashboard') }}</span>
            </el-menu-item>
            <el-menu-item index="/flights" v-if="!isAdmin">
              <el-icon><Location /></el-icon>
              <span>{{ $t('nav.flights') }}</span>
            </el-menu-item>
            <el-menu-item index="/my-tickets" v-if="!isAdmin">
              <el-icon><Ticket /></el-icon>
              <span>{{ $t('nav.myTickets') }}</span>
            </el-menu-item>
            <el-menu-item index="/profile" v-if="!isAdmin">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <template v-if="isAdmin">
              <el-menu-item index="/admin/statistics">
                <el-icon><DataAnalysis /></el-icon>
                <span>{{ $t('nav.statistics') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/approval-requests">
                <el-icon><Document /></el-icon>
                <span>{{ $t('nav.approvalRequests') }}</span>
              </el-menu-item>
              <el-sub-menu index="system-management">
                <template #title>
                  <el-icon><Setting /></el-icon>
                  <span>系统管理</span>
                </template>
                <el-menu-item index="/admin/flights">{{ $t('nav.flightManagement') }}</el-menu-item>
                <el-menu-item index="/admin/tickets">{{ $t('nav.ticketManagement') }}</el-menu-item>
                <el-menu-item index="/admin/users">{{ $t('nav.userManagement') }}</el-menu-item>
                <el-menu-item index="/admin/airlines">{{ $t('nav.airlineManagement') }}</el-menu-item>
                <el-menu-item index="/admin/airports">机场管理</el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>

          <div class="sidebar-separator" v-if="isAdmin"></div>
          <el-menu
            v-if="isAdmin"
            router
            class="sidebar-menu reset-menu"
            :collapse="sidebarCollapsed"
          >
            <el-menu-item index="/admin/reset">
              <el-icon><Refresh /></el-icon>
              <span>数据重置</span>
            </el-menu-item>
          </el-menu>
        </nav>
      </aside>

      <main class="app-main" :class="{ 'with-sidebar': isLoggedIn && showSidebar }">
        <div class="main-content">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { House, Location, Ticket, User, DataAnalysis, Document, Setting, Refresh } from '@element-plus/icons-vue'
import AppHeader from './components/AppHeader.vue'
import AiChatWidget from './components/AiChatWidget.vue'

export default {
  name: 'App',
  components: {
    AppHeader,
    AiChatWidget,
    House,
    Location,
    Ticket,
    User,
    DataAnalysis,
    Document,
    Setting,
    Refresh
  },
  data() {
    return {
      sidebarCollapsed: false
    }
  },
  computed: {
    ...mapState(['currentUser']),
    ...mapGetters(['isLoggedIn', 'isAdmin']),
    showAiChatWidget() {
      const authRoutes = ['/login', '/register']
      return this.isLoggedIn && !this.isAdmin && !authRoutes.includes(this.$route.path)
    },
    showSidebar() {
      const authRoutes = ['/login', '/register']
      return !authRoutes.includes(this.$route.path)
    }
  },
  methods: {
    
  },
  created() {
    this.$store.dispatch('initAuth')
  }
}
</script>

<style>

#app {
  font-family: var(--font-family-primary);
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  min-height: 100vh;
}


.app-layout {
  display: flex;
  min-height: calc(100vh - var(--header-height));
}


.app-sidebar {
  width: var(--sidebar-width);
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border-primary);
  position: sticky;
  top: var(--header-height);
  height: calc(100vh - var(--header-height));
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-nav {
  padding: var(--space-4) 0;
}

.sidebar-header {
  padding: 0 var(--space-4) var(--space-3);
  border-bottom: 1px solid var(--color-border-primary);
  margin-bottom: var(--space-4);
}

.sidebar-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-separator {
  height: 1px;
  background: var(--color-border-primary);
  margin: var(--space-4) var(--space-4);
}

.reset-menu :deep(.el-menu-item) {
  color: var(--color-error) !important;
  margin: 0 var(--space-4);
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
}

.reset-menu :deep(.el-menu-item:hover) {
  background: var(--color-error-light) !important;
  color: var(--color-error) !important;
}

.reset-menu :deep(.el-menu-item.is-active) {
  background: var(--color-error-light) !important;
  color: var(--color-error) !important;
  font-weight: var(--font-weight-medium);
}

.sidebar-menu {
  border: none;
  background: transparent;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 0 var(--space-4);
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
  color: var(--color-text-secondary);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.sidebar-menu :deep(.el-sub-menu__title) {
  margin: 0 var(--space-4);
  border-radius: var(--radius-md);

}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  margin-right: var(--space-3);
  font-size: var(--icon-sm);
}


.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-main.with-sidebar {
  width: calc(100% - var(--sidebar-width));
}

.main-content {
  flex: 1;
  padding: var(--space-6);
  max-width: 100%;
}


@media (max-width: 1023px) {
  .app-sidebar {
    position: fixed;
    left: 0;
    top: var(--header-height);
    z-index: var(--z-fixed);
    transform: translateX(-100%);
    transition: transform var(--transition-normal);
  }
  
  .app-sidebar.open {
    transform: translateX(0);
  }
  
  .app-main {
    width: 100%;
  }
  
  .app-main.with-sidebar {
    width: 100%;
  }
}

@media (max-width: 767px) {
  .main-content {
    padding: var(--space-4);
  }
  
  .app-sidebar {
    width: 280px;
  }
}



:deep(.el-menu) {
  background: transparent !important;
}

:deep(.el-menu-item) {
  background: transparent !important;
}

:deep(.el-sub-menu .el-menu-item) {
  background: transparent !important;
  margin: 0 var(--space-2);
  padding-left: calc(var(--space-8) + var(--space-4)) !important;
}


:deep(.el-menu-item:focus) {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

:deep(.el-sub-menu__title:focus) {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}


.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}


.app-sidebar::-webkit-scrollbar {
  width: 6px;
}

.app-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.app-sidebar::-webkit-scrollbar-thumb {
  background: var(--color-border-secondary);
  border-radius: 3px;
}

.app-sidebar::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-hover);
}
</style>

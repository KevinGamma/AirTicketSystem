<template>
  <header class="app-header" :class="{ 'scrolled': isScrolled }">
    <div class="header-content">
      <div class="brand-section">
        <div class="logo">
          <img 
            v-if="false" 
            src="/logo.png" 
            alt="飞了么"
            class="logo-img"
          />
          <div v-else class="logo-placeholder">
            <el-icon class="logo-icon"><Promotion /></el-icon>
          </div>
        </div>
        <h1 class="brand-name">{{ $t('system.title') }}</h1>
      </div>


      <div class="header-actions">
        <div class="notification-btn" v-if="isLoggedIn && !isAdmin">
          <el-badge :value="unreadNotifications" :hidden="unreadNotifications === 0" class="notification-badge">
            <el-button 
              circle 
              size="large"
              class="action-btn"
              @click="toggleNotifications"
              :aria-label="$t('nav.notifications')"
            >
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
        </div>

        <template v-if="isLoggedIn">
          <el-dropdown trigger="click" @command="handleDropdownCommand">
            <div class="user-dropdown-trigger">
              <div class="user-avatar">
                <img 
                  v-if="currentUser && currentUser.avatarUrl" 
                  :src="currentUser.avatarUrl" 
                  :alt="currentUser.fullName"
                  class="avatar-img"
                />
                <el-icon v-else class="default-avatar"><User /></el-icon>
              </div>
              <div class="user-info">
                <span class="user-name">{{ currentUser ? currentUser.fullName : 'User' }}</span>
                <span class="user-role">{{ isAdmin ? $t('admin.admin') : $t('admin.customer') }}</span>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  {{ $t('nav.profile') }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  {{ $t('nav.logout') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button type="primary" @click="goToLogin" size="large">
            {{ $t('nav.login') }}
          </el-button>
        </template>
      </div>
    </div>


    <el-drawer
      v-model="showNotifications"
      title=""
      direction="rtl"
      size="400px"
      class="notifications-drawer"
      :z-index="2000"
      :modal="true"
      :append-to-body="true"
    >
      <template #header>
        <div class="notifications-header">
          <h3>{{ $t('nav.notifications') }}</h3>
          <el-button text @click="markAllAsRead" :disabled="unreadNotifications === 0">
            {{ $t('notifications.markAllRead') }}
          </el-button>
        </div>
      </template>
      
      <div class="notifications-content">
        <div v-if="notifications.length === 0" class="no-notifications">
          <el-icon class="no-notifications-icon"><Bell /></el-icon>
          <p>{{ $t('notifications.noNotifications') }}</p>
        </div>
        
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item"
          :class="{ 'unread': !notification.read }"
          @click="markNotificationAsRead(notification)"
        >
          <div class="notification-icon" :class="getNotificationIconClass(notification.notificationType)">
            <el-icon>
              <component :is="getNotificationIcon(notification.notificationType)" />
            </el-icon>
          </div>
          <div class="notification-content">
            <h4>{{ notification.title }}</h4>
            <p>{{ notification.message }}</p>
            <span class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </header>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex'
import { 
  Bell, 
  User, 
  ArrowDown, 
  SwitchButton, 
  InfoFilled,
  Promotion,
  Clock,
  ArrowUp,
  Location,
  Check,
  Close,
  Money,
  Wallet
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import api from '@/api'

export default {
  name: 'AppHeader',
  components: {
    Bell,
    User,
    ArrowDown,
    SwitchButton,
    InfoFilled,
    Promotion,
    Clock,
    ArrowUp,
    Location,
    Check,
    Close,
    Money,
    Wallet
  },
  data() {
    return {
      isScrolled: false,
      showNotifications: false,
      notifications: [],
      unreadNotifications: 0
    }
  },
  computed: {
    ...mapState(['currentUser']),
    ...mapGetters(['isLoggedIn', 'isAdmin']),
    isMac() {
      return navigator.platform.toUpperCase().indexOf('MAC') >= 0
    }
  },
  methods: {
    ...mapActions(['logout']),
    
    handleScroll() {
      this.isScrolled = window.scrollY > 0
    },
    
    toggleNotifications() {
      this.showNotifications = !this.showNotifications
    },
    
    handleDropdownCommand(command) {
      switch (command) {
        case 'profile':
          this.$router.push('/profile')
          break
        case 'settings':
          
          break
        case 'logout':
          this.handleLogout()
          break
      }
    },
    
    async handleLogout() {
      await this.logout()
      this.$router.push('/login')
    },
    
    goToLogin() {
      this.$router.push('/login')
    },
    
    async markAllAsRead() {
      try {
        const response = await api.put('/notifications/mark-all-read')
        if (response.data.success) {
          this.notifications.forEach(notification => {
            notification.read = true
          })
          this.unreadNotifications = 0
        }
      } catch (error) {
        console.error('Error marking notifications as read:', error)
        this.$message.error('标记通知失败')
      }
    },
    
    formatNotificationTime(time) {
      return dayjs(time).format('MM/DD HH:mm')
    },
    
    async loadNotifications() {
      try {
        const response = await api.get('/notifications')
        if (response.data.success) {
          this.notifications = response.data.data.map(n => ({
            ...n,
            read: n.isRead
          }))
        }
        
        
        const countResponse = await api.get('/notifications/unread-count')
        if (countResponse.data.success) {
          this.unreadNotifications = countResponse.data.data
        }
      } catch (error) {
        console.error('Error loading notifications:', error)
        
        this.notifications = []
        this.unreadNotifications = 0
      }
    },
    
    async markNotificationAsRead(notification) {
      if (notification.read) return
      
      try {
        const response = await api.put(`/notifications/${notification.id}/mark-read`)
        if (response.data.success) {
          notification.read = true
          this.unreadNotifications = Math.max(0, this.unreadNotifications - 1)
        }
      } catch (error) {
        console.error('Error marking notification as read:', error)
      }
    },
    
    getNotificationIcon(type) {
      const iconMap = {
        'FLIGHT_REMINDER': 'Clock',
        'FLIGHT_TAKEOFF': 'ArrowUp',
        'FLIGHT_LANDING': 'Location',
        'RESCHEDULE_APPROVED': 'Check',
        'RESCHEDULE_REJECTED': 'Close',
        'REFUND_PROCESSED': 'Money',
        'PAYMENT_REQUIRED': 'Wallet',
        'REFUND_DIFFERENCE': 'Money'
      }
      return iconMap[type] || 'InfoFilled'
    },
    
    getNotificationIconClass(type) {
      const classMap = {
        'FLIGHT_REMINDER': 'icon-reminder',
        'FLIGHT_TAKEOFF': 'icon-takeoff',
        'FLIGHT_LANDING': 'icon-landing',
        'RESCHEDULE_APPROVED': 'icon-success',
        'RESCHEDULE_REJECTED': 'icon-error',
        'REFUND_PROCESSED': 'icon-money',
        'PAYMENT_REQUIRED': 'icon-payment',
        'REFUND_DIFFERENCE': 'icon-money'
      }
      return classMap[type] || 'icon-info'
    },
    
    handleKeyboardShortcuts() {
      
    }
  },
  
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
    window.addEventListener('keydown', this.handleKeyboardShortcuts)
    
    
    if (this.isLoggedIn && !this.isAdmin) {
      this.loadNotifications()
    }
  },
  
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
    window.removeEventListener('keydown', this.handleKeyboardShortcuts)
  }
}
</script>

<style scoped>
.app-header {
  height: var(--header-height);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-primary);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  transition: var(--transition-normal);
}

.app-header.scrolled {
  box-shadow: var(--shadow-md);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.header-content {
  height: 100%;
  max-width: var(--container-max-width);
  margin: 0 auto;
  padding: 0 var(--container-padding);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}


.brand-section {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.logo-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--color-primary), #3B82F6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  color: white;
  font-size: var(--icon-md);
}

.brand-name {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0;
}



.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-shrink: 0;
}

.notification-btn {
  position: relative;
}

.action-btn {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  transition: var(--transition-fast);
}

.action-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.notification-badge :deep(.el-badge__content) {
  font-size: var(--font-size-xs);
  min-width: 16px;
  height: 16px;
  line-height: 16px;
}


.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition-fast);
}

.user-dropdown-trigger:hover {
  background: var(--color-bg-hover);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--color-border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  color: var(--color-text-tertiary);
  font-size: var(--icon-sm);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.user-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
}

.user-role {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  line-height: var(--line-height-tight);
}

.dropdown-arrow {
  color: var(--color-text-tertiary);
  font-size: var(--icon-sm);
  transition: var(--transition-fast);
}

.user-dropdown-trigger:hover .dropdown-arrow {
  transform: rotate(180deg);
}



.notifications-drawer {
  z-index: 2000 !important;
}

.notifications-drawer :deep(.el-drawer) {
  z-index: 2000 !important;
}

.notifications-drawer :deep(.el-drawer__header) {
  padding: var(--space-6);
  margin-bottom: 0;
  border-bottom: 1px solid var(--color-border-primary);
}

.notifications-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notifications-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.notifications-content {
  padding: var(--space-6);
}

.no-notifications {
  text-align: center;
  padding: var(--space-12) var(--space-6);
  color: var(--color-text-tertiary);
}

.no-notifications-icon {
  font-size: var(--icon-xl);
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-4);
}

.notification-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
  border-left: 3px solid transparent;
}

.notification-item.unread {
  background: var(--color-info-light);
  border-left-color: var(--color-info);
}

.notification-item:hover {
  background: var(--color-bg-hover);
}

.notification-item {
  cursor: pointer;
}

.notification-icon {
  flex-shrink: 0;
  width: var(--icon-lg);
  height: var(--icon-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: white;
}

.notification-icon.icon-reminder {
  background: #e6a23c;
}

.notification-icon.icon-takeoff {
  background: #409eff;
}

.notification-icon.icon-landing {
  background: #67c23a;
}

.notification-icon.icon-success {
  background: #67c23a;
}

.notification-icon.icon-error {
  background: #f56c6c;
}

.notification-icon.icon-money {
  background: #909399;
}

.notification-icon.icon-payment {
  background: #e6a23c;
}

.notification-icon.icon-info {
  background: var(--color-primary);
}

.notification-content h4 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.notification-content p {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
}

.notification-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}


@media (max-width: 1023px) {
  .user-info {
    display: none;
  }
  
  .header-content {
    padding: 0 var(--space-4);
  }
}

@media (max-width: 767px) {
  .brand-name {
    font-size: var(--font-size-lg);
  }
  
  .header-actions {
    gap: var(--space-2);
  }
  
  .user-dropdown-trigger {
    padding: var(--space-1);
  }
}

</style>
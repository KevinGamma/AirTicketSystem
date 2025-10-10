<template>
  <div class="reset-page">
    <div class="page-header">
      <div class="page-title-section">
        <h1 class="page-title">
          <el-icon class="title-icon"><Refresh /></el-icon>
          数据重置管理
        </h1>
        <p class="page-description">
          管理和重置系统数据，请谨慎操作，所有重置操作都是不可逆的
        </p>
      </div>
    </div>

    
    <div class="statistics-section">
      <h2 class="section-title">当前数据统计</h2>
      <el-row :gutter="24">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon user-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.userCount }}</div>
                <div class="stat-label">用户数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon ticket-icon">
                <el-icon><Ticket /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.ticketCount }}</div>
                <div class="stat-label">机票数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon flight-icon">
                <el-icon><Location /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.flightCount }}</div>
                <div class="stat-label">航班数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon airline-icon">
                <el-icon><Promotion /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.airlineCount }}</div>
                <div class="stat-label">航空公司数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <div class="refresh-stats">
        <el-button @click="loadStatistics" :loading="loadingStats" size="small">
          <el-icon><Refresh /></el-icon>
          刷新统计
        </el-button>
      </div>
    </div>

    
    <div class="reset-section">
      <h2 class="section-title">重置选项</h2>
      
      <el-row :gutter="24">
        
        <el-col :span="12">
          <el-card class="reset-card dangerous">
            <template #header>
              <div class="card-header">
                <el-icon class="card-icon"><DeleteFilled /></el-icon>
                <span class="card-title">完全重置数据库</span>
              </div>
            </template>
            <div class="card-content">
              <p class="card-description">
                删除所有数据（保留管理员用户），包括用户、机票、航班、航空公司等所有数据，并重新初始化基础数据。
              </p>
              <div class="warning-box">
                <el-icon><Warning /></el-icon>
                <span>⚠️ 这是最危险的操作，将删除所有数据！</span>
              </div>
              <div class="card-actions">
                <el-button 
                  type="danger" 
                  @click="showResetDialog('database')"
                  :loading="resetting.database"
                  size="large"
                >
                  完全重置数据库
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        
        <el-col :span="12">
          <el-card class="reset-card warning">
            <template #header>
              <div class="card-header">
                <el-icon class="card-icon"><User /></el-icon>
                <span class="card-title">重置用户数据</span>
              </div>
            </template>
            <div class="card-content">
              <p class="card-description">
                删除所有用户数据（保留管理员），同时删除相关的机票和审批请求。
              </p>
              <div class="affects-list">
                <div class="affects-item">• 用户账户（除管理员外）</div>
                <div class="affects-item">• 相关机票记录</div>
                <div class="affects-item">• 审批请求记录</div>
              </div>
              <div class="card-actions">
                <el-button 
                  type="warning" 
                  @click="showResetDialog('users')"
                  :loading="resetting.users"
                >
                  重置用户数据
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        
        <el-col :span="12">
          <el-card class="reset-card info">
            <template #header>
              <div class="card-header">
                <el-icon class="card-icon"><Ticket /></el-icon>
                <span class="card-title">重置机票数据</span>
              </div>
            </template>
            <div class="card-content">
              <p class="card-description">
                删除所有机票记录和相关审批请求，重置航班可用座位数。
              </p>
              <div class="affects-list">
                <div class="affects-item">• 所有机票记录</div>
                <div class="affects-item">• 审批请求记录</div>
                <div class="affects-item">• 重置航班座位数</div>
              </div>
              <div class="card-actions">
                <el-button 
                  type="primary" 
                  @click="showResetDialog('tickets')"
                  :loading="resetting.tickets"
                >
                  重置机票数据
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        
        <el-col :span="12">
          <el-card class="reset-card info">
            <template #header>
              <div class="card-header">
                <el-icon class="card-icon"><Location /></el-icon>
                <span class="card-title">重置航班数据</span>
              </div>
            </template>
            <div class="card-content">
              <p class="card-description">
                删除所有航班数据和相关机票，然后重新生成新的航班计划。
              </p>
              <div class="affects-list">
                <div class="affects-item">• 所有航班记录</div>
                <div class="affects-item">• 相关机票记录</div>
                <div class="affects-item">• 重新生成月度航班</div>
              </div>
              <div class="card-actions">
                <el-button 
                  type="primary" 
                  @click="showResetDialog('flights')"
                  :loading="resetting.flights"
                >
                  重置航班数据
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        
        <el-col :span="12">
          <el-card class="reset-card warning">
            <template #header>
              <div class="card-header">
                <el-icon class="card-icon"><Promotion /></el-icon>
                <span class="card-title">重置航空公司数据</span>
              </div>
            </template>
            <div class="card-content">
              <p class="card-description">
                删除所有航空公司数据，以及相关的航班和机票，然后重新初始化。
              </p>
              <div class="affects-list">
                <div class="affects-item">• 所有航空公司</div>
                <div class="affects-item">• 相关航班记录</div>
                <div class="affects-item">• 相关机票记录</div>
              </div>
              <div class="card-actions">
                <el-button 
                  type="warning" 
                  @click="showResetDialog('airlines')"
                  :loading="resetting.airlines"
                >
                  重置航空公司数据
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    
    <el-dialog
      v-model="resetDialogVisible"
      :title="resetDialogConfig.title"
      width="500px"
      :before-close="handleDialogClose"
    >
      <div class="dialog-content">
        <div class="dialog-icon">
          <el-icon class="warning-icon" :size="48"><Warning /></el-icon>
        </div>
        <div class="dialog-text">
          <p class="dialog-message">{{ resetDialogConfig.message }}</p>
          <div class="confirmation-input">
            <p class="input-label">请输入 <strong>CONFIRM</strong> 以确认操作：</p>
            <el-input 
              v-model="confirmText" 
              placeholder="输入 CONFIRM"
              @keyup.enter="confirmReset"
            />
          </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetDialogVisible = false">取消</el-button>
          <el-button 
            type="danger" 
            @click="confirmReset"
            :disabled="confirmText !== 'CONFIRM'"
            :loading="confirmingReset"
          >
            确认重置
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { 
  Refresh, 
  User, 
  Ticket, 
  Location, 
  Promotion, 
  DeleteFilled, 
  Warning 
} from '@element-plus/icons-vue'
import api from '../../api'

export default {
  name: 'AdminReset',
  components: {
    Refresh,
    User,
    Ticket,
    Location,
    Promotion,
    DeleteFilled,
    Warning
  },
  data() {
    return {
      statistics: {
        userCount: 0,
        ticketCount: 0,
        flightCount: 0,
        airlineCount: 0,
        approvalRequestCount: 0
      },
      loadingStats: false,
      resetting: {
        database: false,
        users: false,
        tickets: false,
        flights: false,
        airlines: false
      },
      resetDialogVisible: false,
      currentResetType: '',
      confirmText: '',
      confirmingReset: false,
      resetDialogConfig: {
        title: '',
        message: ''
      }
    }
  },
  methods: {
    async loadStatistics() {
      this.loadingStats = true
      try {
        const response = await api.get('/admin/reset/statistics')
        if (response.data.success) {
          this.statistics = response.data.data
        }
      } catch (error) {
        this.$message.error('获取统计数据失败')
      } finally {
        this.loadingStats = false
      }
    },

    showResetDialog(type) {
      this.currentResetType = type
      this.confirmText = ''
      this.resetDialogVisible = true

      const configs = {
        database: {
          title: '确认完全重置数据库',
          message: '此操作将删除所有数据（保留管理员用户），包括用户、机票、航班、航空公司等，并重新初始化基础数据。此操作不可撤销！'
        },
        users: {
          title: '确认重置用户数据',
          message: '此操作将删除所有用户账户（保留管理员）和相关的机票、审批请求数据。此操作不可撤销！'
        },
        tickets: {
          title: '确认重置机票数据',
          message: '此操作将删除所有机票记录和审批请求，并重置航班座位数。此操作不可撤销！'
        },
        flights: {
          title: '确认重置航班数据',
          message: '此操作将删除所有航班和相关机票数据，然后重新生成新的航班计划。此操作不可撤销！'
        },
        airlines: {
          title: '确认重置航空公司数据',
          message: '此操作将删除所有航空公司、航班和机票数据，然后重新初始化。此操作不可撤销！'
        }
      }

      this.resetDialogConfig = configs[type]
    },

    handleDialogClose() {
      this.resetDialogVisible = false
      this.confirmText = ''
      this.currentResetType = ''
    },

    async confirmReset() {
      if (this.confirmText !== 'CONFIRM') {
        this.$message.warning('请输入 CONFIRM 以确认操作')
        return
      }

      this.confirmingReset = true
      this.resetting[this.currentResetType] = true

      try {
        const response = await api.post(`/admin/reset/${this.currentResetType}`)
        
        if (response.data.success) {
          this.$message.success(response.data.message)
          this.resetDialogVisible = false
          
          
          await this.loadStatistics()
        } else {
          this.$message.error(response.data.message || '重置操作失败')
        }
      } catch (error) {
        console.error('Reset failed:', error)
        this.$message.error('重置操作失败: ' + (error.response?.data?.message || error.message))
      } finally {
        this.confirmingReset = false
        this.resetting[this.currentResetType] = false
        this.confirmText = ''
        this.currentResetType = ''
      }
    }
  },

  async mounted() {
    await this.loadStatistics()
  }
}
</script>

<style scoped>
.reset-page {
  min-height: calc(100vh - var(--header-height));
  background: var(--color-bg-primary);
  padding: var(--space-6);
}


.page-header {
  margin-bottom: var(--space-8);
}

.page-title-section {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-error);
  margin: 0 0 var(--space-4) 0;
}

.title-icon {
  font-size: var(--icon-xl);
}

.page-description {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: 0;
  line-height: var(--line-height-normal);
}


.statistics-section {
  margin-bottom: var(--space-8);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-6) 0;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--icon-lg);
  color: white;
}

.user-icon {
  background: var(--color-primary);
}

.ticket-icon {
  background: var(--color-success);
}

.flight-icon {
  background: var(--color-info);
}

.airline-icon {
  background: var(--color-warning);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
}

.stat-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.refresh-stats {
  text-align: center;
  margin-top: var(--space-6);
}


.reset-section {
  margin-bottom: var(--space-8);
}

.reset-card {
  margin-bottom: var(--space-6);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.reset-card.dangerous {
  border-left: 4px solid var(--color-error);
}

.reset-card.warning {
  border-left: 4px solid var(--color-warning);
}

.reset-card.info {
  border-left: 4px solid var(--color-info);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.card-icon {
  font-size: var(--icon-md);
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.card-content {
  padding: var(--space-4) 0 0;
}

.card-description {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
  margin: 0 0 var(--space-4) 0;
}

.warning-box {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-error-light);
  border: 1px solid var(--color-error);
  border-radius: var(--radius-md);
  color: var(--color-error);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-4);
}

.affects-list {
  margin-bottom: var(--space-4);
}

.affects-item {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-1);
}

.card-actions {
  text-align: center;
}


.dialog-content {
  display: flex;
  gap: var(--space-4);
  align-items: flex-start;
}

.dialog-icon {
  flex-shrink: 0;
}

.warning-icon {
  color: var(--color-warning);
}

.dialog-text {
  flex: 1;
}

.dialog-message {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  line-height: var(--line-height-normal);
  margin: 0 0 var(--space-4) 0;
}

.confirmation-input {
  margin-top: var(--space-4);
}

.input-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}


@media (max-width: 1023px) {
  .reset-page {
    padding: var(--space-4);
  }

  .stat-content {
    flex-direction: column;
    text-align: center;
    gap: var(--space-2);
  }
}

@media (max-width: 767px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }

  .page-description {
    font-size: var(--font-size-base);
  }

  .dialog-content {
    flex-direction: column;
    text-align: center;
  }
}


.dark .stat-card {
  background: var(--color-bg-secondary);
}

.dark .reset-card {
  background: var(--color-bg-secondary);
}

.dark .warning-box {
  background: rgba(245, 108, 108, 0.1);
}
</style>
<template>
  <div class="payment-failed-page">
    <div class="container">
      <div class="failed-card">
        <div class="failed-icon">
          <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
        </div>

        <div class="failed-message">
          <h1>支付失败</h1>
          <p>很抱歉，您的支付未能成功完成</p>
        </div>

        <div class="error-details">
          <el-alert
            type="error"
            :closable="false"
            show-icon
          >
            <template #title>
              支付失败原因
            </template>
            <template #default>
              <div class="error-content">
                <p>• 支付超时或用户取消</p>
                <p>• 网络连接异常</p>
                <p>• 支付宝账户余额不足</p>
                <p class="sandbox-note">沙箱环境：请使用测试账号完成支付</p>
              </div>
            </template>
          </el-alert>
        </div>

        <div v-if="ticket" class="ticket-info">
          <div class="info-header">
            <h3>机票信息</h3>
            <el-tag type="warning" size="large">待支付</el-tag>
          </div>
          
          <div class="ticket-details">
            <div class="detail-row">
              <span class="label">航班号</span>
              <span class="value">{{ ticket.flight?.flightNumber }}</span>
            </div>
            <div class="detail-row">
              <span class="label">机票号</span>
              <span class="value">{{ ticket.ticketNumber }}</span>
            </div>
            <div class="detail-row">
              <span class="label">乘客姓名</span>
              <span class="value">{{ ticket.passengerName }}</span>
            </div>
            <div class="detail-row">
              <span class="label">支付金额</span>
              <span class="value price">¥{{ ticket.price }}</span>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <el-button 
            type="primary" 
            size="large"
            @click="retryPayment"
            class="retry-button"
            :disabled="!ticket"
          >
            <el-icon><Refresh /></el-icon>
            重新支付
          </el-button>
          
          <el-button 
            size="large"
            @click="goToMyTickets"
            class="tickets-button"
          >
            <el-icon><Ticket /></el-icon>
            返回机票列表
          </el-button>
          
          <el-button 
            size="large"
            @click="contactSupport"
            class="support-button"
          >
            <el-icon><Service /></el-icon>
            联系客服
          </el-button>
        </div>

        <div class="help-info">
          <div class="help-title">
            <el-icon><QuestionFilled /></el-icon>
            需要帮助？
          </div>
          <div class="help-content">
            <p>• 沙箱测试账号：fntkra7936@sandbox.com</p>
            <p>• 测试密码：111111</p>
            <p>• 确保在新窗口中完成支付流程</p>
            <p>• 如仍有问题，请联系技术支持</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { 
  CircleCloseFilled,
  Refresh,
  Ticket,
  Service,
  QuestionFilled
} from '@element-plus/icons-vue'
import api from '../api'
import dayjs from 'dayjs'

export default {
  name: 'PaymentFailed',
  components: {
    CircleCloseFilled,
    Refresh,
    Ticket,
    Service,
    QuestionFilled
  },
  data() {
    return {
      ticket: null
    }
  },
  methods: {
    async loadTicket() {
      const ticketId = this.$route.query.ticketId
      if (!ticketId) {
        this.goToMyTickets()
        return
      }

      try {
        const response = await api.get(`/tickets/${ticketId}`)
        if (response.data.success) {
          this.ticket = response.data.data
        }
      } catch (error) {
        console.error('Failed to load ticket:', error)
      }
    },

    retryPayment() {
      if (this.ticket) {
        this.$router.push(`/payment/${this.ticket.id}`)
      }
    },

    goToMyTickets() {
      this.$router.push('/my-tickets')
    },

    contactSupport() {
      ElMessage.info('客服功能开发中，请稍后再试')
    },

    formatDateTime(datetime) {
      return dayjs(datetime).format('YYYY年MM月DD日 HH:mm')
    },

    getTicketTypeText(type) {
      const typeMap = {
        'ECONOMY': '经济舱',
        'BUSINESS': '商务舱',
        'FIRST': '头等舱'
      }
      return typeMap[type] || type
    }
  },

  mounted() {
    this.loadTicket()
  }
}
</script>

<style scoped>
/* Page Layout */
.payment-failed-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.container {
  max-width: 600px;
  width: 100%;
}

/* Failed Card */
.failed-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.8s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Failed Icon */
.failed-icon {
  margin-bottom: 30px;
}

.error-icon {
  font-size: 80px;
  color: #ff4757;
  animation: shake 0.8s ease-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}

/* Failed Message */
.failed-message {
  margin-bottom: 30px;
}

.failed-message h1 {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 12px 0;
}

.failed-message p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* Error Details */
.error-details {
  margin-bottom: 30px;
  text-align: left;
}

.error-content {
  font-size: 14px;
  line-height: 1.6;
}

.error-content p {
  margin: 4px 0;
}

.sandbox-note {
  color: #1565C0;
  font-weight: 500;
  margin-top: 8px;
}

/* Ticket Info */
.ticket-info {
  background: #f8f9fa;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
  text-align: left;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.info-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.ticket-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.detail-row .label {
  font-size: 14px;
  color: #666;
}

.detail-row .value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.detail-row .value.price {
  color: #E53935;
  font-size: 16px;
  font-weight: 600;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 30px;
}

.retry-button {
  height: 56px;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tickets-button,
.support-button {
  height: 48px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Help Information */
.help-info {
  border-top: 1px solid #e0e0e0;
  padding-top: 20px;
  text-align: left;
}

.help-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.help-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.help-content p {
  margin: 4px 0;
}

/* Responsive Design */
@media (max-width: 768px) {
  .failed-card {
    padding: 30px 20px;
  }
  
  .failed-message h1 {
    font-size: 24px;
  }
  
  .ticket-details {
    gap: 8px;
  }
  
  .action-buttons {
    gap: 8px;
  }
  
  .retry-button {
    height: 48px;
    font-size: 16px;
  }
  
  .tickets-button,
  .support-button {
    height: 44px;
    font-size: 14px;
  }
}
</style>
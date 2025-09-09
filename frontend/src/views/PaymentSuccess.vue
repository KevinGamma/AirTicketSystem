end<template>
  <div class="payment-success-page">
    <div class="container">
      <div class="success-card">
        <div class="success-icon">
          <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
        </div>

        <div class="success-message">
          <h1>支付成功！</h1>
          <p>您的机票已成功支付，祝您旅途愉快</p>
        </div>

        <div v-if="ticket" class="ticket-summary">
          <div class="summary-header">
            <h3>机票信息</h3>
            <el-tag type="success" size="large">已支付</el-tag>
          </div>
          
          <div class="ticket-details">
            <div class="flight-info">
              <!-- Connecting Flight Badge -->
              <div v-if="isConnectingFlight" class="connecting-badge">
                <el-tag type="success" size="small">联程航班</el-tag>
              </div>
              
              <!-- Flight Numbers Display -->
              <div class="flight-number">
                <template v-if="isConnectingFlight">
                  {{ allFlights.map(f => f?.flightNumber).join(' + ') }}
                </template>
                <template v-else>
                  {{ ticket.flight?.flightNumber }}
                </template>
              </div>
              
              <!-- Complete Route Display -->
              <div class="route">{{ completeRoute }}</div>
              
              <!-- Flight Time Display -->
              <div class="flight-time">
                {{ formatDateTime(allFlights[0]?.departureTimeUtc) }} -
                {{ formatDateTime(allFlights[allFlights.length - 1]?.arrivalTimeUtc) }}
              </div>
              
              <!-- Detailed Flight Segments for Connecting Flights -->
              <div v-if="isConnectingFlight" class="flight-segments">
                <div 
                  v-for="(flight, index) in allFlights" 
                  :key="flight?.id || index"
                  class="segment-detail"
                >
                  <div class="segment-header">
                    <span class="segment-number">第{{ index + 1 }}段</span>
                    <span class="segment-flight">{{ flight?.flightNumber }}</span>
                  </div>
                  <div class="segment-route">
                    {{ flight?.departureAirport?.city }} 
                    {{ formatTime(flight?.departureTimeUtc) }} →
                    {{ flight?.arrivalAirport?.city }} 
                    {{ formatTime(flight?.arrivalTimeUtc) }}
                  </div>
                </div>
              </div>
            </div>
            
            <div class="passenger-details">
              <div class="detail-item">
                <span class="label">乘客姓名</span>
                <span class="value">{{ ticket.passengerName }}</span>
              </div>
              <div class="detail-item">
                <span class="label">机票号</span>
                <span class="value">{{ ticket.ticketNumber }}</span>
              </div>
              <div class="detail-item">
                <span class="label">舱位类型</span>
                <span class="value">{{ getTicketTypeText(ticket.ticketType) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">支付金额</span>
                <span class="value price">¥{{ ticket.price }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="sandbox-notice">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>
              <strong>沙箱测试支付</strong>
            </template>
            <template #default>
              这是支付宝沙箱环境的测试支付，未产生真实费用
            </template>
          </el-alert>
        </div>

        <div class="action-buttons">
          <el-button 
            type="primary" 
            size="large"
            @click="goToMyTickets"
            class="primary-action"
          >
            <el-icon><Ticket /></el-icon>
            查看我的机票
          </el-button>
          
          <el-button 
            size="large"
            @click="goToFlightSearch"
            class="secondary-action"
          >
            <el-icon><Search /></el-icon>
            继续预订
          </el-button>
        </div>

      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { 
  CircleCheckFilled,
  Ticket,
  Search
} from '@element-plus/icons-vue'
import api from '../api'
import dayjs from 'dayjs'

export default {
  name: 'PaymentSuccess',
  components: {
    CircleCheckFilled,
    Ticket,
    Search
  },
  data() {
    return {
      ticket: null
    }
  },
  computed: {
    // Check if this is a connecting flight ticket
    isConnectingFlight() {
      return this.ticket && this.ticket.connectingFlights && this.ticket.connectingFlights.length > 0
    },
    
    // Get all flights for display
    allFlights() {
      if (!this.ticket) return []
      
      const flights = [this.ticket.flight]
      if (this.isConnectingFlight) {
        flights.push(...(this.ticket.connectingFlights || []))
      }
      return flights.filter(f => f) // Remove any null/undefined flights
    },
    
    // Get complete route display
    completeRoute() {
      if (!this.ticket) return ''
      
      if (this.isConnectingFlight && this.allFlights.length > 1) {
        const cities = this.allFlights.map(f => f.departureAirport?.city).filter(Boolean)
        cities.push(this.allFlights[this.allFlights.length - 1]?.arrivalAirport?.city)
        return cities.join(' → ')
      } else {
        return `${this.ticket.flight?.departureAirport?.city} → ${this.ticket.flight?.arrivalAirport?.city}`
      }
    }
  },
  methods: {
    async loadTicket() {
      const ticketId = this.$route.query.ticketId
      if (!ticketId) {
        ElMessage.warning('缺少机票信息')
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

    goToMyTickets() {
      this.$router.push('/my-tickets')
    },

    goToFlightSearch() {
      this.$router.push('/flights')
    },

    formatDateTime(datetime) {
      return dayjs(datetime).format('YYYY年MM月DD日 HH:mm')
    },
    
    formatTime(datetime) {
      return dayjs(datetime).format('HH:mm')
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
    
    // Show success animation
    setTimeout(() => {
      const icon = document.querySelector('.check-icon')
      if (icon) {
        icon.style.animation = 'bounce 0.6s ease-out'
      }
    }, 300)
  }
}
</script>

<style scoped>
/* Page Layout */
.payment-success-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.container {
  max-width: 600px;
  width: 100%;
}

/* Success Card */
.success-card {
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

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

/* Success Icon */
.success-icon {
  margin-bottom: 30px;
}

.check-icon {
  font-size: 80px;
  color: #52c41a;
}

/* Success Message */
.success-message {
  margin-bottom: 40px;
}

.success-message h1 {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 12px 0;
}

.success-message p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* Ticket Summary */
.ticket-summary {
  background: #f8f9fa;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
  text-align: left;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.summary-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.ticket-details {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.flight-info {
  text-align: center;
  padding: 16px;
  background: white;
  border-radius: 12px;
}

.connecting-badge {
  margin-bottom: 12px;
}

.flight-segments {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.segment-detail {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  text-align: left;
}

.segment-detail:last-child {
  margin-bottom: 0;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.segment-number {
  font-size: 12px;
  color: #666;
  background: #e0e0e0;
  padding: 2px 6px;
  border-radius: 3px;
}

.segment-flight {
  font-size: 14px;
  font-weight: 600;
  color: #1565C0;
  font-family: 'Monaco', monospace;
}

.segment-route {
  font-size: 13px;
  color: #333;
  line-height: 1.3;
}

.flight-number {
  font-size: 20px;
  font-weight: 600;
  color: #1565C0;
  font-family: 'Monaco', monospace;
  margin-bottom: 8px;
}

.route {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.flight-time {
  font-size: 14px;
  color: #666;
}

.passenger-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 14px;
  color: #666;
}

.detail-item .value {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.detail-item .value.price {
  color: #E53935;
  font-size: 18px;
  font-weight: 600;
}

/* Sandbox Notice */
.sandbox-notice {
  margin-bottom: 30px;
}

/* Payment Reminder */
.payment-reminder {
  margin-bottom: 30px;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 30px;
}

.primary-action {
  height: 56px;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.secondary-action {
  height: 48px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}


/* Responsive Design */
@media (max-width: 768px) {
  .success-card {
    padding: 30px 20px;
  }
  
  .success-message h1 {
    font-size: 24px;
  }
  
  .passenger-details {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    gap: 8px;
  }
  
  .primary-action {
    height: 48px;
    font-size: 16px;
  }
  
  .secondary-action {
    height: 44px;
    font-size: 14px;
  }
}
</style>
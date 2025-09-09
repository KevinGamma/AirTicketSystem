<template>
  <div class="payment-page">
    <div class="payment-header">
      <div class="container">
        <h1 class="payment-title">机票支付</h1>
        <div class="security-info">
          <el-icon><Lock /></el-icon>
          <span>安全支付环境</span>
        </div>
      </div>
    </div>

    <div class="payment-content">
      <div class="container">
        <div class="payment-layout">
          <div class="order-summary">
            <el-card class="summary-card">
              <template #header>
                <div class="card-header">
                  <h3>订单详情</h3>
                  <el-tag type="info" size="small">沙箱环境</el-tag>
                </div>
              </template>
              
              <div v-if="ticket" class="order-details">
                <div class="flight-summary">
                  <!-- Connecting Flight Badge -->
                  <div v-if="isConnectingFlight" class="connecting-badge">
                    <el-tag type="success" size="small">联程航班</el-tag>
                  </div>
                  
                  <!-- All Flights Display -->
                  <div 
                    v-for="(flight, index) in allFlights" 
                    :key="flight?.id || index"
                    class="flight-segment"
                    :class="{ 'connecting-flight': index > 0 }"
                  >
                    <div class="flight-header">
                      <div class="flight-number">{{ flight?.flightNumber }}</div>
                      <div class="aircraft-type">{{ getAircraftDisplayName(flight?.aircraftType) }}</div>
                      <div v-if="isConnectingFlight" class="segment-label">
                        {{ index === 0 ? '第一段' : `第${index + 1}段` }}
                      </div>
                    </div>
                    
                    <div class="flight-route">
                      <div class="route-point">
                        <div class="time">{{ formatTime(flight?.departureTimeUtc) }}</div>
                        <div class="airport">{{ flight?.departureAirport?.city }}</div>
                        <div class="airport-name">{{ flight?.departureAirport?.name }}</div>
                      </div>
                      <div class="route-arrow">
                        <el-icon><Right /></el-icon>
                      </div>
                      <div class="route-point">
                        <div class="time">{{ formatTime(flight?.arrivalTimeUtc) }}</div>
                        <div class="airport">{{ flight?.arrivalAirport?.city }}</div>
                        <div class="airport-name">{{ flight?.arrivalAirport?.name }}</div>
                      </div>
                    </div>
                    
                    <!-- Layover indicator for connecting flights -->
                    <div 
                      v-if="isConnectingFlight && index < allFlights.length - 1" 
                      class="layover-indicator"
                    >
                      <div class="layover-line"></div>
                      <div class="layover-text">中转</div>
                    </div>
                  </div>
                </div>

                <div class="passenger-info">
                  <h4>乘客信息</h4>
                  <div class="info-row">
                    <span>姓名：</span>
                    <span>{{ ticket.passengerName }}</span>
                  </div>
                  <div class="info-row">
                    <span>舱位：</span>
                    <span>{{ getTicketTypeText(ticket.ticketType) }}</span>
                  </div>
                  <div class="info-row">
                    <span>座位：</span>
                    <span>{{ ticket.seatNumber || '待分配' }}</span>
                  </div>
                </div>

                <div class="price-breakdown">
                  <div class="price-row">
                    <span>机票价格</span>
                    <span>¥{{ baseTicketPrice }}</span>
                  </div>
                  <div class="price-row">
                    <span>机建燃油费</span>
                    <span>¥{{ fuelSurcharge }}</span>
                  </div>
                  <div class="price-row total">
                    <span>总计</span>
                    <span>¥{{ totalAmount }}</span>
                  </div>
                </div>

              </div>
            </el-card>
          </div>

          <div class="payment-methods">
            <el-card class="payment-card">
              <template #header>
                <h3>选择支付方式</h3>
              </template>
              
              <div class="payment-options">
                <div 
                  class="payment-option active"
                  :class="{ active: selectedPaymentMethod === 'alipay' }"
                  @click="selectedPaymentMethod = 'alipay'"
                >
                  <div class="payment-icon">
                    <img src="@/zfb.webp" alt="支付宝" />
                  </div>
                  <div class="payment-info">
                    <div class="payment-name">支付宝</div>
                    <div class="payment-desc">安全快捷，支持花呗分期</div>
                  </div>
                  <div class="payment-indicator">
                    <el-icon v-if="selectedPaymentMethod === 'alipay'"><Check /></el-icon>
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
                    <div class="notice-content">
                      <strong>沙箱测试环境</strong>
                      <p>使用测试账号：<code>wcarec3791@sandbox.com</code> 密码：<code>111111</code></p>
                      <p>此为测试环境，不会产生真实费用</p>
                    </div>
                  </template>
                </el-alert>
              </div>

              <div class="payment-actions">
                <el-button 
                  type="primary" 
                  size="large" 
                  :loading="paymentLoading"
                  :disabled="!selectedPaymentMethod"
                  @click="initiatePayment"
                  class="pay-button"
                >
                  <el-icon><CreditCard /></el-icon>
                  立即支付 ¥{{ totalAmount }}
                </el-button>
                
                <el-button 
                  size="large" 
                  @click="cancelPayment"
                  class="cancel-button"
                >
                  取消支付
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="paymentDialogVisible"
      title="支付处理中"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="payment-processing">
        <div class="processing-icon">
          <el-icon class="rotating"><Loading /></el-icon>
        </div>
        <div class="processing-text">
          <h4>{{ paymentDialogContent }}</h4>
          <p v-if="showSandboxPayment">沙箱模式：不会产生真实交易</p>
          <p v-if="paymentNumber" class="payment-number">支付单号: {{ paymentNumber }}</p>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="checkPaymentStatus">检查支付状态</el-button>
          <el-button type="primary" @click="cancelPaymentProcess">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Lock, 
  Right, 
  Check, 
  CreditCard, 
  Loading
} from '@element-plus/icons-vue'
import api from '../api'
import dayjs from 'dayjs'
import { getAircraftDisplayName } from '../utils/aircraftUtils'

export default {
  name: 'PaymentPage',
  components: {
    Lock,
    Right,
    Check,
    CreditCard,
    Loading
  },
  data() {
    return {
      ticket: null,
      selectedPaymentMethod: 'alipay',
      paymentLoading: false,
      paymentDialogVisible: false,
      paymentDialogContent: '正在创建支付订单...',
      paymentNumber: null,
      checkStatusInterval: null,
      showSandboxPayment: false,
      sandboxPaymentStep: 1
    }
  },
  computed: {
    // The ticket.price already includes fuel surcharge, so totalAmount is just ticket.price
    totalAmount() {
      return this.ticket ? this.ticket.price : 0
    },
    
    // Calculate base ticket price by subtracting fuel surcharge from total
    // Note: ticket.price from the backend already includes fuel surcharge
    baseTicketPrice() {
      if (!this.ticket) return 0
      return this.ticket.price - this.fuelSurcharge
    },
    
    // Check if this is a connecting flight ticket
    isConnectingFlight() {
      return this.ticket && this.ticket.connectingFlights && this.ticket.connectingFlights.length > 0
    },
    
    // Calculate fuel surcharge based on flight type
    // NOTE: Backend adds both fuel surcharge (70 RMB) + airport construction fee (50 RMB) = 120 RMB total per flight
    fuelSurcharge() {
      if (!this.ticket) return 0
      
      if (this.isConnectingFlight) {
        // Connecting flights: 120 RMB per flight segment (70 fuel + 50 airport construction)
        const totalFlights = 1 + (this.ticket.connectingFlights?.length || 0) // Main flight + connecting flights
        return totalFlights * 120
      } else {
        // Direct flights: 120 RMB (70 fuel + 50 airport construction)
        return 120
      }
    },
    
    // Get all flights for display
    allFlights() {
      if (!this.ticket) return []
      
      const flights = [this.ticket.flight]
      if (this.isConnectingFlight) {
        flights.push(...(this.ticket.connectingFlights || []))
      }
      return flights.filter(f => f) // Remove any null/undefined flights
    }
  },
  methods: {
    getAircraftDisplayName,
    async loadTicket() {
      const ticketId = this.$route.params.ticketId
      if (!ticketId) {
        ElMessage.error('无效的机票ID')
        this.$router.push('/my-tickets')
        return
      }

      try {
        const response = await api.get(`/tickets/${ticketId}`)
        if (response.data.success) {
          this.ticket = response.data.data

          if (this.ticket.status === 'PAID') {
            ElMessage.info('此机票已支付')
            this.$router.push('/my-tickets')
            return
          }
        }
      } catch (error) {
        ElMessage.error('加载机票信息失败')
        this.$router.push('/my-tickets')
      }
    },

    async initiatePayment() {
      if (!this.selectedPaymentMethod) {
        ElMessage.warning('请选择支付方式')
        return
      }

      try {
        this.paymentLoading = true
        
        const paymentData = {
          ticketId: this.ticket.id,
          amount: this.totalAmount,
          paymentMethod: 'ALIPAY',
          returnUrl: `${window.location.origin}/payment/return`,
          useSandbox: true
        }

        const response = await api.post('/payment/alipay/create', paymentData)
        
        if (response.data.success) {
          this.paymentNumber = response.data.data.paymentNumber

          this.paymentDialogVisible = true

          const paymentUrl = response.data.data.paymentUrl
          if (paymentUrl && paymentUrl.includes('alipay')) {
            this.paymentDialogContent = '正在跳转到支付宝沙箱支付页面...'

            setTimeout(() => {
              this.redirectToAlipayPage(paymentUrl)
            }, 1500)

            this.startPaymentStatusCheck()
          } else {
            this.simulateSandboxPayment()
          }
        }
      } catch (error) {
        ElMessage.error('创建支付失败')
      } finally {
        this.paymentLoading = false
      }
    },

    async simulateSandboxPayment() {
      this.showSandboxPayment = true
      this.sandboxPaymentStep = 1

      this.paymentDialogContent = '正在跳转到支付宝沙箱支付页面...'
      await new Promise(resolve => setTimeout(resolve, 1500))

      this.showSandboxLoginDialog()
    },

    showSandboxLoginDialog() {
      this.$confirm(
        `🔒 支付宝沙箱支付页面\n\n` +
        `📋 订单信息:\n` +
        `商品: 机票支付-${this.ticket?.flight?.flightNumber || ''}\n` +
        `金额: ¥${this.totalAmount}\n` +
        `商户: 航空票务系统\n\n` +
        `👤 请使用沙箱测试账号:\n` +
        `账号: fntkra7936@sandbox.com\n` +
        `支付密码: 111111\n\n` +
        `⚠️ 这是沙箱测试环境，不会产生真实扣费\n\n` +
        `点击"确认支付"模拟成功，点击"取消"模拟失败`,
        '支付宝沙箱支付页面', 
        {
          confirmButtonText: '确认支付 ¥' + this.totalAmount,
          cancelButtonText: '取消支付',
          type: 'info',
          showClose: false,
          closeOnClickModal: false,
          customClass: 'sandbox-payment-dialog',
          dangerouslyUseHTMLString: false
        }
      ).then(() => {
        this.processSandboxPayment()
      }).catch(() => {
        this.handlePaymentCancel()
      })
    },

    async processSandboxPayment() {
      try {
        this.paymentDialogContent = '正在验证支付信息...'
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        this.paymentDialogContent = '正在处理付款...'
        await new Promise(resolve => setTimeout(resolve, 2000))
        
        this.paymentDialogContent = '支付处理中，请稍候...'
        await new Promise(resolve => setTimeout(resolve, 1500))

        const response = await api.post(`/payment/sandbox/simulate-success`, null, {
          params: { paymentNumber: this.paymentNumber }
        })
        
        if (response.data.success) {
          this.paymentDialogContent = '支付成功！'
          await new Promise(resolve => setTimeout(resolve, 1000))
          this.handlePaymentSuccess()
        } else {
          this.handlePaymentError()
        }
      } catch (error) {
        this.handlePaymentError()
      }
    },

    handlePaymentCancel() {
      this.paymentDialogVisible = false
      this.showSandboxPayment = false
      ElMessage.warning('支付已取消')
    },

    redirectToAlipayPage(paymentUrl) {
      const newWindow = window.open('', '_blank', 'width=1000,height=700,scrollbars=yes,resizable=yes')
      
      if (newWindow) {
        try {
          newWindow.document.write(paymentUrl)
          newWindow.document.close()
          newWindow.document.title = '支付宝沙箱支付'
          this.paymentDialogContent = '请在新窗口中使用沙箱账号完成支付'
          this.$notify({
            title: '支付宝沙箱支付页面已打开',
            message: '请在新窗口中使用以下测试账号完成支付：\n账号: wcarec3791@sandbox.com\n密码: 111111\n\n支付完成后此页面会自动更新状态',
            type: 'info',
            duration: 0,
            position: 'top-right'
          })

          const checkClosed = setInterval(() => {
            if (newWindow.closed) {
              clearInterval(checkClosed)
              this.checkPaymentStatus()
            }
          }, 1000)
          
        } catch (error) {
          console.error('打开支付页面失败:', error)
          ElMessage.error('打开支付页面失败，请重试')
          this.handlePaymentError()
        }
      } else {
        ElMessage.error('无法打开支付页面，请允许浏览器弹窗或手动打开')
        this.handlePaymentError()
      }
    },

    startPaymentStatusCheck() {
      this.checkStatusInterval = setInterval(async () => {
        await this.checkPaymentStatus()
      }, 3000)
    },

    async checkPaymentStatus() {
      if (!this.paymentNumber) return

      try {
        const response = await api.get(`/payment/alipay/query/${this.paymentNumber}`)
        
        if (response.data.success) {
          const paymentStatus = response.data.data.status
          
          if (paymentStatus === 'SUCCESS') {
            this.handlePaymentSuccess()
          } else if (paymentStatus === 'FAILED' || paymentStatus === 'CANCELLED') {
            this.handlePaymentError()
          }
        } else {
          console.error('查询支付状态失败:', response.data.message)
        }
      } catch (error) {
        console.error('Check payment status failed:', error)
      }
    },

    handlePaymentSuccess() {
      this.clearStatusCheck()
      this.paymentDialogVisible = false
      
      ElMessage.success('支付成功！')
      this.$router.push({
        path: '/payment/success',
        query: { ticketId: this.ticket.id }
      })
    },

    handlePaymentError() {
      this.clearStatusCheck()
      this.paymentDialogVisible = false
      
      ElMessage.error('支付失败，请重试')
    },

    cancelPaymentProcess() {
      this.clearStatusCheck()
      this.paymentDialogVisible = false
    },

    clearStatusCheck() {
      if (this.checkStatusInterval) {
        clearInterval(this.checkStatusInterval)
        this.checkStatusInterval = null
      }
    },

    cancelPayment() {
      ElMessageBox.confirm(
        '确定要取消支付吗？',
        '取消支付',
        {
          confirmButtonText: '确定',
          cancelButtonText: '继续支付',
          type: 'warning'
        }
      ).then(() => {
        this.$router.push('/my-tickets')
      }).catch(() => {
      })
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
  },

  beforeUnmount() {
    this.clearStatusCheck()
  }
}
</script>

<style scoped>

.payment-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.payment-header {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 40px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.payment-header .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.payment-title {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin: 0;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.payment-content {
  padding: 40px 0;
}

.payment-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: start;
}

.order-summary {
  display: flex;
  flex-direction: column;
}

.summary-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.order-details {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.flight-summary {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
}

.connecting-badge {
  text-align: center;
  margin-bottom: 16px;
}

.flight-segment {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #e0e0e0;
}

.flight-segment:last-child {
  margin-bottom: 0;
}

.connecting-flight {
  border-left: 4px solid #28a745;
}

.flight-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.segment-label {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 4px 8px;
  border-radius: 4px;
}

.layover-indicator {
  text-align: center;
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

.layover-line {
  width: 60px;
  height: 2px;
  background: #ddd;
  margin: 0 auto 4px auto;
  border-radius: 1px;
}

.layover-text {
  color: #999;
  font-size: 12px;
}

.flight-number {
  font-size: 20px;
  font-weight: 600;
  color: #1565C0;
  font-family: 'Monaco', monospace;
}

.aircraft-type {
  font-size: 14px;
  color: #666;
}

.flight-route {
  display: flex;
  align-items: center;
  gap: 20px;
}

.route-point {
  flex: 1;
  text-align: center;
}

.route-point .time {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  font-family: 'Monaco', monospace;
}

.route-point .airport {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 4px 0;
}

.route-point .airport-name {
  font-size: 12px;
  color: #666;
}

.route-arrow {
  color: #1565C0;
  font-size: 20px;
}

.passenger-info h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.price-breakdown {
  border-top: 1px solid #e0e0e0;
  padding-top: 16px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.price-row.total {
  font-size: 18px;
  font-weight: 600;
  color: #E53935;
  border-top: 1px solid #e0e0e0;
  padding-top: 12px;
  margin-top: 8px;
}

.payment-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.payment-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.payment-option:hover:not(.disabled) {
  border-color: #1565C0;
  background: #f8f9ff;
}

.payment-option.active {
  border-color: #1565C0;
  background: #f0f7ff;
}

.payment-option.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.payment-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f8f9fa;
}

.payment-icon img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.payment-info {
  flex: 1;
}

.payment-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.payment-desc {
  font-size: 14px;
  color: #666;
}

.payment-indicator {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1565C0;
}

.payment-option.active .payment-indicator {
  border-color: #1565C0;
  background: #1565C0;
  color: white;
}

.sandbox-notice {
  margin-bottom: 24px;
}

.notice-content {
  font-size: 14px;
}

.notice-content p {
  margin: 4px 0;
}

.notice-content code {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', monospace;
}

.payment-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pay-button {
  height: 56px;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.cancel-button {
  height: 48px;
  font-size: 16px;
}

.payment-processing {
  text-align: center;
  padding: 20px;
}

.processing-icon {
  margin-bottom: 20px;
}

.rotating {
  animation: rotate 1s linear infinite;
  font-size: 48px;
  color: #1565C0;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.processing-text h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.processing-text p {
  margin: 4px 0;
  color: #666;
}

.sandbox-tip {
  font-family: 'Monaco', monospace;
  background: #f0f7ff;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #1565C0;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

@media (max-width: 768px) {
  .payment-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .payment-header .container {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .payment-title {
    font-size: 24px;
  }
  
  .flight-route {
    flex-direction: column;
    gap: 12px;
  }
  
  .route-arrow {
    transform: rotate(90deg);
  }
}
</style>
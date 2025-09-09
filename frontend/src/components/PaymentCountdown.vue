<template>
  <div v-if="showCountdown" :class="['payment-countdown', urgencyClass]">
    <div class="countdown-content">
      <el-icon class="countdown-icon">
        <Clock />
      </el-icon>
      <span class="countdown-text">{{ countdownDisplay }}</span>
      <span class="countdown-message">{{ paymentMessage }}</span>
    </div>
  </div>
</template>

<script>
import { Clock } from '@element-plus/icons-vue'
import { ref, onMounted, onUnmounted, computed } from 'vue'
import api from '../api'

export default {
  name: 'PaymentCountdown',
  components: {
    Clock
  },
  props: {
    ticketId: {
      type: Number,
      required: true
    },
    status: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const remainingSeconds = ref(0)
    const urgencyLevel = ref('')
    const paymentMessage = ref('')
    const countdownDisplay = ref('')
    const canPay = ref(false)
    let countdownTimer = null

    const showCountdown = computed(() => {
      return props.status === 'BOOKED' && remainingSeconds.value > 0
    })

    const urgencyClass = computed(() => {
      return `urgency-${urgencyLevel.value.toLowerCase()}`
    })

    const fetchPaymentDeadline = async () => {
      try {
        const response = await api.get(`/tickets/${props.ticketId}/payment-deadline`)
        
        if (response.data.success) {
          const data = response.data.data
          remainingSeconds.value = data.remainingSeconds || 0
          urgencyLevel.value = data.urgencyLevel || 'LOW'
          paymentMessage.value = data.message || '请在时限内完成支付'
          countdownDisplay.value = data.countdownDisplay || '00:00'
          canPay.value = data.canPay || false
        }
      } catch (error) {
        console.error('Failed to fetch payment deadline:', error)
        remainingSeconds.value = 0
      }
    }

    const updateCountdown = () => {
      if (remainingSeconds.value > 0) {
        remainingSeconds.value--
        
        const minutes = Math.floor(remainingSeconds.value / 60)
        const seconds = remainingSeconds.value % 60
        countdownDisplay.value = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
        
        if (remainingSeconds.value > 300) {
          urgencyLevel.value = 'LOW'
          paymentMessage.value = '请在时限内完成支付'
        } else if (remainingSeconds.value > 120) {
          urgencyLevel.value = 'MEDIUM'
          paymentMessage.value = '支付时间即将到期，请尽快完成支付'
        } else if (remainingSeconds.value > 0) {
          urgencyLevel.value = 'HIGH'
          paymentMessage.value = '支付时间即将到期！'
        } else {
          urgencyLevel.value = 'EXPIRED'
          paymentMessage.value = '支付时间已过期'
          canPay.value = false
        }
      } else {
        if (countdownTimer) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }
    }

    const startCountdown = () => {
      if (countdownTimer) {
        clearInterval(countdownTimer)
      }
      countdownTimer = setInterval(updateCountdown, 1000)
    }

    onMounted(async () => {
      if (props.status === 'BOOKED') {
        await fetchPaymentDeadline()
        if (remainingSeconds.value > 0) {
          startCountdown()
        }
      }
    })

    onUnmounted(() => {
      if (countdownTimer) {
        clearInterval(countdownTimer)
      }
    })

    return {
      showCountdown,
      urgencyClass,
      countdownDisplay,
      paymentMessage,
      canPay
    }
  }
}
</script>

<style scoped>
.payment-countdown {
  margin: 8px 0;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid;
  transition: all 0.3s ease;
}

.countdown-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.countdown-icon {
  font-size: 16px;
}

.countdown-text {
  font-weight: bold;
  font-size: 16px;
  font-family: monospace;
}

.countdown-message {
  font-size: 14px;
}

.urgency-low {
  background-color: #f0f9ff;
  border-color: #3b82f6;
  color: #1e40af;
}

.urgency-medium {
  background-color: #fffbeb;
  border-color: #f59e0b;
  color: #d97706;
}

.urgency-high {
  background-color: #fef2f2;
  border-color: #ef4444;
  color: #dc2626;
  animation: pulse 1s infinite;
}

.urgency-expired {
  background-color: #f3f4f6;
  border-color: #6b7280;
  color: #374151;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>
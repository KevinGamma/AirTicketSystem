<template>
  <div class="booking-page">
    
    <header class="page-header">
      <div class="container">
        <div class="header-content">
          <h1 class="page-title">航班预订</h1>
          <p class="page-subtitle">请确认航班信息并填写乘客信息</p>
        </div>
      </div>
    </header>

    
    <div v-if="loading" class="loading-container">
      <div class="container">
        <el-skeleton animated :rows="8" />
      </div>
    </div>

    
    <div v-else-if="!flight" class="error-container">
      <div class="container">
        <el-result
          icon="warning"
          title="航班信息未找到"
          sub-title="请返回重新选择航班"
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/flights')">
              返回搜索
            </el-button>
          </template>
        </el-result>
      </div>
    </div>

    
    <div v-else class="booking-content">
      <div class="container">
        <div class="booking-layout">
          
          <div class="flight-details-section">
            <div class="section-header">
              <h2>航班信息</h2>
            </div>
            
            <div class="flight-card">
              
              <div class="flight-overview" v-if="flight.isConnecting">
                <div class="overview-header">
                  <h3>行程概览</h3>
                  <div class="flight-type-badge connecting">{{ flight.segments.length }}段中转</div>
                </div>
                <div class="overview-route">
                  <div class="route-summary">
                    <span class="departure-city">{{ flight.departureAirport?.city }}</span>
                    <el-icon class="route-arrow"><ArrowRight /></el-icon>
                    <span class="arrival-city">{{ flight.arrivalAirport?.city }}</span>
                  </div>
                  <div class="journey-details">
                    <span class="total-duration">总时长: {{ calculateDuration(flight.departureTimeUtc, flight.arrivalTimeUtc) }}</span>
                    <span class="total-price">¥{{ currentSegmentPrice }}</span>
                  </div>
                </div>
              </div>

              
              <div v-for="(segment, index) in flight.segments" :key="`segment-${index}`" class="flight-segment">
                <div class="segment-header" v-if="flight.isConnecting">
                  <span class="segment-number">航段 {{ index + 1 }}</span>
                  <span class="segment-type">{{ index === 0 ? '出发' : index === flight.segments.length - 1 ? '到达' : '中转' }}</span>
                </div>
                
                <div class="flight-info">
                  <div class="airline-section">
                    <div class="airline-logo">
                      <img
                        v-if="segment.airline?.logoUrl"
                        :src="segment.airline.logoUrl"
                        :alt="segment.airline.name"
                        class="airline-logo-img"
                      />
                      <div v-else class="airline-logo-placeholder">
                        <span>{{ getAirlineShortName(segment.airline?.name) }}</span>
                      </div>
                    </div>
                    <div class="airline-details">
                      <div class="flight-number">{{ segment.flightNumber }}</div>
                      <div class="airline-name">{{ segment.airline?.name }}</div>
                    </div>
                  </div>

                  <div class="route-section">
                    <div class="departure-info">
                      <div class="time-section">
                        <div class="local-time-label" v-if="isInternationalFlight(segment.departureAirport?.city, segment.arrivalAirport?.city)">当地时间</div>
                        <div class="time">
                          {{ isInternationalFlight(segment.departureAirport?.city, segment.arrivalAirport?.city) 
                             ? formatLocalTime(segment.departureTimeUtc, segment.departureAirport?.city) 
                             : formatTime(segment.departureTimeUtc) }}
                        </div>
                      </div>
                      <div class="airport">{{ getAirportCode(segment.departureAirport) }}</div>
                      <div class="airport-name">{{ segment.departureAirport?.name }}</div>
                      <div class="date">{{ formatDate(segment.departureTimeUtc) }}</div>
                    </div>
                    
                    <div class="flight-path">
                      <div class="duration">{{ calculateDuration(segment.departureTimeUtc, segment.arrivalTimeUtc) }}</div>
                      <div class="path-visual">
                        <div class="path-line"></div>
                        <el-icon class="plane-icon"><Promotion /></el-icon>
                        <div class="path-line"></div>
                      </div>
                      <div class="flight-type">{{ flight.isConnecting ? '航段' : '直飞' }}</div>
                    </div>

                    <div class="arrival-info">
                      <div class="time-section">
                        <div class="local-time-label" v-if="isInternationalFlight(segment.departureAirport?.city, segment.arrivalAirport?.city)">当地时间</div>
                        <div class="time">
                          {{ isInternationalFlight(segment.departureAirport?.city, segment.arrivalAirport?.city) 
                             ? formatLocalTime(segment.arrivalTimeUtc, segment.arrivalAirport?.city) 
                             : formatTime(segment.arrivalTimeUtc) }}
                        </div>
                      </div>
                      <div class="airport">{{ getAirportCode(segment.arrivalAirport) }}</div>
                      <div class="airport-name">{{ segment.arrivalAirport?.name }}</div>
                      <div class="date">{{ formatDate(segment.arrivalTimeUtc) }}</div>
                    </div>
                  </div>

                  <div class="price-section" v-if="!flight.isConnecting">
                    <div class="ticket-type">{{ getTicketTypeName(ticketType) }}</div>
                    <div class="price">¥{{ currentSegmentPrice }}</div>
                  </div>
                </div>

                
                <div v-if="flight.isConnecting && index < flight.segments.length - 1" class="layover-info">
                  <div class="layover-details">
                    <el-icon class="layover-icon"><Clock /></el-icon>
                    <span class="layover-text">
                      在 {{ segment.arrivalAirport?.city }} 中转 
                      {{ calculateLayoverTime(segment.arrivalTimeUtc, flight.segments[index + 1].departureTimeUtc) }}
                    </span>
                  </div>
                </div>
              </div>

              
              <div v-if="flight.isConnecting" class="connecting-price-section">
                <div class="price-details">
                  <div class="ticket-type">{{ getTicketTypeName(ticketType) }}</div>
                  <div class="price">¥{{ currentSegmentPrice }}</div>
                </div>
              </div>
            </div>

              
              <div class="booking-summary">
                <div class="summary-item">
                  <span class="label">乘客人数:</span>
                  <span class="value">{{ passengers }}人</span>
                </div>
                <div class="summary-item">
                  <span class="label">舱位类型:</span>
                  <span class="value">{{ getTicketTypeName(ticketType) }}</span>
                </div>
                <div class="summary-item price-breakdown" v-if="baseSegmentPrice !== currentSegmentPrice">
                  <span class="label">基础价格 (经济舱):</span>
                  <span class="value">¥{{ baseSegmentPrice }}</span>
                </div>
                <div class="summary-item price-breakdown" v-if="cabinPriceMultiplier !== 1">
                  <span class="label">舱位调整 (×{{ cabinPriceMultiplier }}):</span>
                  <span class="value">¥{{ currentSegmentPrice - baseSegmentPrice }}</span>
                </div>
                <div class="summary-item price-per-person">
                  <span class="label">单人价格:</span>
                  <span class="value">¥{{ currentSegmentPrice }}</span>
                </div>
                <div class="summary-item total">
                  <span class="label">总价:</span>
                  <span class="value">¥{{ totalPrice }}</span>
                </div>
              </div>
            </div>
          </div>

          
          <div class="passenger-info-section">
            <div class="section-header">
              <h2>乘客信息</h2>
            </div>

            <el-form
              ref="bookingFormRef"
              :model="bookingForm"
              :rules="bookingRules"
              class="booking-form"
            >
              
              <div class="cabin-class-section">
                <h3>舱位选择</h3>
                <div class="cabin-options">
                  <el-radio-group v-model="ticketType" class="cabin-radio-group">
                    <el-radio-button label="ECONOMY" size="large">
                      <div class="cabin-option">
                        <div class="cabin-main">
                          <span class="cabin-name">经济舱</span>
                          <span class="cabin-description">标准座椅，基础服务</span>
                        </div>
                        <div class="cabin-price">基础价格</div>
                      </div>
                    </el-radio-button>
                    <el-radio-button label="BUSINESS" size="large">
                      <div class="cabin-option">
                        <div class="cabin-main">
                          <span class="cabin-name">高级经济舱</span>
                          <span class="cabin-description">宽敞座椅，前排座位</span>
                        </div>
                        <div class="cabin-price">+{{ Math.round((baseSegmentPrice * 1.5) - baseSegmentPrice) }}元</div>
                      </div>
                    </el-radio-button>
                    <el-radio-button label="FIRST" size="large">
                      <div class="cabin-option">
                        <div class="cabin-main">
                          <span class="cabin-name">头等舱</span>
                          <span class="cabin-description">豪华座椅，贵宾服务</span>
                        </div>
                        <div class="cabin-price">+{{ Math.round((baseSegmentPrice * 3.0) - baseSegmentPrice) }}元</div>
                      </div>
                    </el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              
              <div class="passengers-section">
                <h3>乘客详细信息</h3>
                <p class="passenger-note" v-if="passengers > 1">
                  注意：当前版本每次只能为一位乘客预订。如需为多位乘客预订，请分别进行预订。
                </p>
                <div
                  v-for="(passenger, index) in bookingForm.passengers"
                  :key="index"
                  class="passenger-card"
                >
                  <div class="passenger-header">
                    <h4>乘客信息</h4>
                  </div>
                  <div class="form-grid">
                    <el-form-item 
                      label="姓名" 
                      :prop="`passengers.${index}.name`"
                    >
                      <el-input
                        v-model="passenger.name"
                        placeholder="请输入乘客姓名"
                      />
                    </el-form-item>
                    <el-form-item 
                      label="身份证号" 
                      :prop="`passengers.${index}.idCard`"
                    >
                      <el-input
                        v-model="passenger.idCard"
                        placeholder="请输入身份证号"
                      />
                    </el-form-item>
                  </div>
                </div>
              </div>

              
              <div class="submit-section">
                <div class="terms-section">
                  <el-checkbox v-model="agreeTerms" size="large">
                    我已阅读并同意 <a href="#" @click.prevent>《购票协议》</a> 和 <a href="#" @click.prevent>《隐私政策》</a>
                  </el-checkbox>
                </div>
                
                <div class="submit-buttons">
                  <el-button size="large" @click="goBack">
                    返回修改
                  </el-button>
                  <el-button
                    type="primary"
                    size="large"
                    :loading="submitting"
                    :disabled="!agreeTerms"
                    @click="submitBooking"
                    class="submit-btn"
                  >
                    确认预订
                  </el-button>
                </div>
              </div>
            </el-form>
          </div>
        </div>
      </div>
    </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Promotion, ArrowRight, Clock } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import api from '../api'


dayjs.extend(utc)
dayjs.extend(timezone)

export default {
  name: 'BookingPage',
  components: {
    Promotion,
    ArrowRight,
    Clock
  },
  data() {
    return {
      loading: true,
      submitting: false,
      flight: null,
      passengers: 1,
      ticketType: 'ECONOMY',
      agreeTerms: false,
      bookingForm: {
        passengers: []
      },
      bookingRules: {}
    }
  },
  computed: {
    
    cabinPriceMultiplier() {
      const multipliers = {
        'ECONOMY': 1.0,
        'BUSINESS': 1.5,
        'FIRST': 3.0
      }
      return multipliers[this.ticketType] || 1.0
    },

    
    baseSegmentPrice() {
      if (!this.flight) return 0
      
      if (this.flight.isConnecting && this.flight.segments) {
        
        return this.flight.segments.reduce((total, segment) => {
          return total + (segment.price || 0)
        }, 0)
      } else {
        
        return this.flight.price || 0
      }
    },

    
    currentSegmentPrice() {
      return Math.round(this.baseSegmentPrice * this.cabinPriceMultiplier)
    },

    
    totalPrice() {
      return this.currentSegmentPrice * this.passengers
    }
  },
  methods: {
    
    getCityTimezone(city) {
      const timezoneMap = {
        '纽约': 'America/New_York',
        '洛杉矶': 'America/Los_Angeles',
        '东京': 'Asia/Tokyo',
        '首尔': 'Asia/Seoul',
        '新加坡': 'Asia/Singapore',
        
        '北京': 'Asia/Shanghai',
        '上海': 'Asia/Shanghai',
        '广州': 'Asia/Shanghai'
      };
      return timezoneMap[city] || 'Asia/Shanghai';
    },
    
    isInternationalFlight(departureCity, arrivalCity) {
      const internationalCities = ['纽约', '洛杉矶', '东京', '首尔', '新加坡'];
      return internationalCities.includes(departureCity) || internationalCities.includes(arrivalCity);
    },
    
    formatLocalTime(utcTime, city) {
      if (!utcTime || !city) return '';
      try {
        const timezone = this.getCityTimezone(city);
        return dayjs.utc(utcTime).tz(timezone).format('HH:mm');
      } catch (error) {
        console.error('Error formatting local time:', error);
        return dayjs.utc(utcTime).format('HH:mm');
      }
    },

    async loadFlightDetails() {
      try {
        this.loading = true
        const flightId = this.$route.query.flightId
        const connectingFlightIds = this.$route.query.connectingFlightIds
        const isConnecting = this.$route.query.isConnecting === 'true'
        this.passengers = parseInt(this.$route.query.passengers) || 1
        this.ticketType = 'ECONOMY' 

        if (!flightId) {
          this.flight = null
          return
        }

        if (isConnecting && connectingFlightIds) {
          
          await this.loadConnectingFlights(connectingFlightIds.split(','))
        } else {
          
          await this.loadDirectFlight(flightId)
        }
        
        this.initializePassengers()
      } catch (error) {
        console.error('Load flight details error:', error)
        this.flight = null
        ElMessage.error('获取航班信息失败')
      } finally {
        this.loading = false
      }
    },

    async loadDirectFlight(flightId) {
      const response = await api.get(`/flights/${flightId}`)
      if (response.data.success) {
        this.flight = {
          ...response.data.data,
          isConnecting: false,
          segments: [response.data.data]
        }
      } else {
        this.flight = null
        ElMessage.error(response.data.message || '获取航班信息失败')
      }
    },

    async loadConnectingFlights(flightIds) {
      try {
        
        const flightPromises = flightIds.map(id => api.get(`/flights/${id}`))
        const responses = await Promise.all(flightPromises)
        
        const segments = responses.map(response => {
          if (response.data.success) {
            return response.data.data
          }
          throw new Error('Failed to load flight segment')
        })

        if (segments.length > 0) {
          
          this.flight = {
            id: `connecting_${segments[0].id}`,
            flightNumber: segments.map(s => s.flightNumber).join(' + '),
            isConnecting: true,
            segments: segments,
            
            
            departureTimeUtc: segments[0].departureTimeUtc,
            arrivalTimeUtc: segments[segments.length - 1].arrivalTimeUtc,
            departureAirport: segments[0].departureAirport,
            arrivalAirport: segments[segments.length - 1].arrivalAirport,
            
            
            price: segments.reduce((total, segment) => total + (segment.price || 0), 0),
            
            
            airline: segments[0].airline
          }
        } else {
          throw new Error('No flight segments loaded')
        }
      } catch (error) {
        console.error('Error loading connecting flights:', error)
        this.flight = null
        ElMessage.error('获取中转航班信息失败')
      }
    },

    initializePassengers() {
      this.bookingForm.passengers = []
      for (let i = 0; i < this.passengers; i++) {
        this.bookingForm.passengers.push({
          name: '',
          idCard: ''
        })
        
        
        this.bookingRules[`passengers.${i}.name`] = [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ]
        this.bookingRules[`passengers.${i}.idCard`] = [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^\d{17}[\dXx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
        ]
      }
    },

    async submitBooking() {
      try {
        await this.$refs.bookingFormRef.validate()
        
        this.submitting = true
        
        
        
        const firstPassenger = this.bookingForm.passengers[0]
        const bookingData = {
          passengerName: firstPassenger.name,
          passengerIdNumber: firstPassenger.idCard,
          ticketType: this.ticketType
        }

        
        if (this.flight.isConnecting && this.flight.segments) {
          
          bookingData.flightId = this.flight.segments[0].id
          bookingData.connectingFlightIds = this.flight.segments.map(segment => segment.id)
        } else {
          
          bookingData.flightId = this.flight.id
        }

        const response = await api.post('/tickets/book', bookingData)
        
        if (response.data.success) {
          ElMessage.success('预订成功！正在跳转到支付页面...')
          
          setTimeout(() => {
            this.$router.push({
              path: `/payment/${response.data.data.id}`,
              query: { from: 'booking' }
            })
          }, 1500)
        } else {
          ElMessage.error(response.data.message || '预订失败，请重试')
        }
      } catch (error) {
        if (error.name !== 'ElFormValidateError') {
          console.error('Booking error:', error)
          ElMessage.error('预订失败，请重试')
        }
      } finally {
        this.submitting = false
      }
    },

    goBack() {
      this.$router.go(-1)
    },


    formatTime(datetime) {
      return dayjs(datetime).format('HH:mm')
    },

    formatDate(datetime) {
      return dayjs(datetime).format('MM-DD')
    },

    calculateDuration(departure, arrival) {
      const start = dayjs(departure)
      const end = dayjs(arrival)
      const duration = end.diff(start, 'minute')
      const hours = Math.floor(duration / 60)
      const minutes = duration % 60
      return `${hours}小时${minutes}分钟`
    },

    calculateLayoverTime(arrivalTime, nextDepartureTime) {
      const arrival = dayjs(arrivalTime)
      const departure = dayjs(nextDepartureTime)
      const duration = departure.diff(arrival, 'minute')
      const hours = Math.floor(duration / 60)
      const minutes = duration % 60
      
      if (hours > 0) {
        return `${hours}小时${minutes}分钟`
      } else {
        return `${minutes}分钟`
      }
    },

    getAirportCode(airport) {
      return airport?.code || '未知'
    },

    getAirlineShortName(name) {
      return name ? name.substring(0, 2) : '航'
    },

    getTicketTypeName(type) {
      const types = {
        'ECONOMY': '经济舱',
        'BUSINESS': '高级经济舱',
        'FIRST': '头等舱'
      }
      return types[type] || type
    }
  },

  mounted() {
    this.loadFlightDetails()
  }
}
</script>

<style scoped>
.booking-page {
  min-height: 100vh;
  background: #f5f5f5;
}


.page-header {
  padding: 2rem 0;
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
  border-bottom: 1px solid #e5e7eb;
}

.header-content {
  text-align: center;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #333333;
  margin: 0 0 0.75rem 0;
}

.page-subtitle {
  font-size: 1.125rem;
  color: #666666;
  margin: 0;
}


.loading-container,
.error-container {
  padding: 2rem 0;
}


.booking-content {
  padding: 2rem 0;
}

.booking-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  align-items: start;
}


.section-header {
  margin-bottom: 1.5rem;
}

.section-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333333;
  margin: 0;
}


.flight-details-section {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  position: sticky;
  top: 2rem;
}


.flight-overview {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e2e8f0;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.overview-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333333;
  margin: 0;
}

.flight-type-badge.connecting {
  background: #fef3c7;
  color: #d97706;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.overview-route {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.route-summary {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.departure-city,
.arrival-city {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333333;
}

.route-arrow {
  color: #3b82f6;
  font-size: 1.125rem;
}

.journey-details {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.total-duration {
  font-size: 0.875rem;
  color: #666666;
}

.total-price {
  font-size: 1.25rem;
  font-weight: 700;
  color: #3b82f6;
}


.flight-segment {
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  margin-bottom: 1rem;
  overflow: hidden;
}

.flight-segment:last-of-type {
  margin-bottom: 0;
}

.segment-header {
  background: #f3f4f6;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.segment-number {
  font-weight: 600;
  color: #333333;
}

.segment-type {
  font-size: 0.875rem;
  color: #666666;
  background: white;
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
  border: 1px solid #d1d5db;
}


.layover-info {
  background: #eff6ff;
  border-top: 1px solid #e5e7eb;
  padding: 0.75rem 1rem;
  display: flex;
  justify-content: center;
  align-items: center;
}

.layover-details {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #3b82f6;
}

.layover-icon {
  font-size: 1rem;
}

.layover-text {
  font-size: 0.875rem;
  font-weight: 500;
}


.connecting-price-section {
  background: #f8fafc;
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
  border-radius: 0 0 0.75rem 0.75rem;
  margin-top: 1rem;
}

.price-details {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.flight-card {
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  overflow: hidden;
}

.flight-info {
  padding: 1.5rem;
  display: grid;
  grid-template-columns: 200px 1fr 150px;
  gap: 1.5rem;
  align-items: center;
}


.airline-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.airline-logo {
  width: 48px;
  height: 48px;
  border-radius: 0.5rem;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
}

.airline-logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.airline-logo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3b82f6;
  color: white;
  font-weight: bold;
  font-size: 0.875rem;
}

.airline-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.flight-number {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333333;
  font-family: monospace;
}

.airline-name {
  font-size: 0.875rem;
  color: #666666;
}


.route-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.departure-info,
.arrival-info {
  text-align: center;
  min-width: 80px;
}

.time-section {
  text-align: center;
}

.local-time-label {
  font-size: 0.75rem;
  color: #666666;
  margin-bottom: 2px;
  font-weight: 500;
}

.time {
  font-size: 1.25rem;
  font-weight: bold;
  color: #333333;
  margin-bottom: 0.25rem;
}

.airport {
  font-size: 1rem;
  font-weight: 600;
  color: #333333;
  margin-bottom: 0.25rem;
}

.airport-name {
  font-size: 0.75rem;
  color: #666666;
  margin-bottom: 0.25rem;
  text-align: center;
  line-height: 1.3;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date {
  font-size: 0.75rem;
  color: #666666;
}

.flight-path {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.duration {
  font-size: 0.875rem;
  color: #666666;
  font-weight: 500;
}

.path-visual {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.path-line {
  flex: 1;
  height: 2px;
  background: #e5e7eb;
  border-radius: 1px;
  min-width: 30px;
}

.plane-icon {
  color: #3b82f6;
  font-size: 1.125rem;
  transform: rotate(90deg);
}

.flight-type {
  font-size: 0.75rem;
  color: #059669;
  font-weight: 500;
  background: #d1fae5;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
}


.price-section {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-end;
}

.ticket-type {
  font-size: 0.875rem;
  color: #666666;
  background: #f3f4f6;
  padding: 0.25rem 0.75rem;
  border-radius: 0.375rem;
}

.price {
  font-size: 1.5rem;
  font-weight: bold;
  color: #3b82f6;
}

.tax-info {
  font-size: 0.75rem;
  color: #666666;
}


.booking-summary {
  background: #f8fafc;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-item.total {
  border-top: 1px solid #e5e7eb;
  padding-top: 0.75rem;
  font-weight: 600;
  font-size: 1.125rem;
}

.summary-item .value {
  font-weight: 500;
}

.summary-item.total .value {
  color: #3b82f6;
  font-size: 1.25rem;
}

.summary-item.price-breakdown {
  font-size: 0.875rem;
  color: #666666;
  padding: 0.25rem 0;
}

.summary-item.price-breakdown .value {
  color: #f59e0b;
  font-weight: 500;
}

.summary-item.price-per-person {
  border-top: 1px solid #e5e7eb;
  padding-top: 0.75rem;
  margin-top: 0.5rem;
  font-weight: 600;
}


.passenger-info-section {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.cabin-class-section {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e5e7eb;
}

.cabin-class-section h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333333;
  margin: 0 0 1.5rem 0;
}

.cabin-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cabin-radio-group {
  display: flex;
  flex-direction: row;
  gap: 0.75rem;
  flex-wrap: wrap;
}

:deep(.cabin-radio-group .el-radio-button) {
  flex: 1;
  margin-right: 0;
  min-width: 200px;
}

:deep(.cabin-radio-group .el-radio-button__inner) {
  width: 100%;
  padding: 1rem 1.5rem;
  border-radius: 0.5rem;
  border: 2px solid #e5e7eb;
  background: white;
  transition: all 0.2s ease;
}

:deep(.cabin-radio-group .el-radio-button__inner:hover) {
  border-color: #3b82f6;
  background: #f0f9ff;
}

:deep(.cabin-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #333333;
}

.cabin-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 1rem;
}

.cabin-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  flex: 1;
}

.cabin-name {
  font-size: 1rem;
  font-weight: 600;
  color: #333333;
}

.cabin-description {
  font-size: 0.875rem;
  color: #666666;
}

.cabin-price {
  font-size: 0.875rem;
  font-weight: 600;
  color: #f59e0b;
  background: #fef3c7;
  padding: 0.25rem 0.75rem;
  border-radius: 0.375rem;
  white-space: nowrap;
}

.passengers-section {
  margin-bottom: 2rem;
}

.passengers-section h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333333;
  margin: 0 0 1rem 0;
}

.passenger-note {
  background: #fff3cd;
  color: #856404;
  padding: 0.75rem 1rem;
  border-radius: 0.375rem;
  border: 1px solid #ffeaa7;
  margin-bottom: 1rem;
  font-size: 0.875rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.passenger-card {
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.passenger-card:last-child {
  margin-bottom: 0;
}

.passenger-header h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #333333;
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}


.submit-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.terms-section {
  margin-bottom: 1.5rem;
  text-align: center;
}

.terms-section a {
  color: #3b82f6;
  text-decoration: none;
}

.terms-section a:hover {
  text-decoration: underline;
}

.submit-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.submit-btn {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 600;
}


.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}


@media (max-width: 1023px) {
  .booking-layout {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
  
  .flight-details-section {
    position: static;
  }
  
  .flight-info {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .route-section {
    justify-content: space-between;
  }
  
  .price-section {
    align-items: center;
    text-align: center;
  }
}

@media (max-width: 767px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .flight-info {
    padding: 1rem;
  }
  
  .booking-summary {
    padding: 0.75rem 1rem;
  }
  
  .passenger-card {
    padding: 1rem;
  }
  
  .submit-buttons {
    flex-direction: column;
  }
}
</style>
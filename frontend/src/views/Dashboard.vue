<template>
  <div class="dashboard-page">
    
    <section class="welcome-section">
      <div class="container">
        <div class="welcome-content">
          <h1 class="welcome-title">
            {{ $t('dashboard.welcome') }}{{ currentUser ? currentUser.fullName : $t('common.user') }}！
          </h1>
          <p class="welcome-description">
            {{ isAdmin ? $t('dashboard.adminMessage') : $t('dashboard.customerMessage') }}
          </p>
          <div class="frequent-routes" v-if="!isAdmin && frequentRoutes.length > 0">
            <span class="frequent-routes-label">常用路线：</span>
            <div class="route-chips">
              <el-button 
                v-for="route in frequentRoutes" 
                :key="route.id"
                type="primary"
                text
                size="small"
                @click="searchRoute(route)"
                class="route-chip"
              >
                {{ route.name }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    
    <section class="quick-actions-section">
      <div class="container">
        <div class="quick-actions-grid" :class="{ 'admin-layout': isAdmin }">
          <template v-if="!isAdmin">
            <div class="quick-action-card search-flights-card" @click="goTo('/flights')" tabindex="0" role="button" :aria-label="$t('dashboard.searchFlights')">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Search /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.searchFlights') }}</h3>
                <p class="card-description">{{ $t('dashboard.searchFlightsDesc') }}</p>
              </div>
            </div>

            
            <div class="quick-action-card" @click="goTo('/my-tickets')" tabindex="0" role="button" :aria-label="$t('dashboard.myTickets')">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Ticket /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.myTickets') }}</h3>
                <p class="card-description">{{ $t('dashboard.myTicketsDesc') }}</p>
                <div class="card-stats" v-if="ticketStats.total > 0">
                  <span class="stat-item">
                    <span class="stat-value">{{ ticketStats.total }}</span>
                    <span class="stat-label">张机票</span>
                  </span>
                </div>
              </div>
            </div>

            
            <div class="quick-action-card" @click="goTo('/profile')" tabindex="0" role="button" :aria-label="$t('dashboard.personalProfile')">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><User /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.personalProfile') }}</h3>
                <p class="card-description">{{ $t('dashboard.personalProfileDesc') }}</p>
              </div>
            </div>
          </template>

          
          <template v-else>
            <div class="quick-action-card" @click="goTo('/admin/statistics')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><DataAnalysis /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.adminPanel') }}</h3>
                <p class="card-description">{{ $t('dashboard.adminPanelDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/admin/flights')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Location /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.flightManagement') }}</h3>
                <p class="card-description">{{ $t('dashboard.flightManagementDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/admin/tickets')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Ticket /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.ticketManagement') }}</h3>
                <p class="card-description">{{ $t('dashboard.ticketManagementDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/admin/users')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><User /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.userManagement') }}</h3>
                <p class="card-description">{{ $t('dashboard.userManagementDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/admin/airports')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Location /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.airportManagement') }}</h3>
                <p class="card-description">{{ $t('dashboard.airportManagementDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/admin/approval-requests')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><Document /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.approvalRequests') }}</h3>
                <p class="card-description">{{ $t('dashboard.approvalRequestsDesc') }}</p>
              </div>
            </div>

            <div class="quick-action-card" @click="goTo('/profile')" tabindex="0" role="button">
              <div class="card-icon-container">
                <el-icon class="card-icon" :size="48"><User /></el-icon>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ $t('dashboard.personalProfile') }}</h3>
                <p class="card-description">{{ $t('dashboard.personalProfileDesc') }}</p>
              </div>
            </div>
          </template>
        </div>
      </div>
    </section>

    
    <section class="recent-bookings-section" v-if="!isAdmin">
      <div class="container">
        <div class="recent-bookings-card">
          <div class="section-header">
            <h3 class="section-title">{{ $t('dashboard.recentBookings') }}</h3>
            <el-button 
              text 
              type="primary" 
              @click="goTo('/my-tickets')"
              class="view-all-btn"
            >
              查看全部 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div v-if="loadingTickets" class="loading-container">
            <div v-for="i in 3" :key="i" class="booking-card-skeleton">
              <el-skeleton animated>
                <template #template>
                  <div class="skeleton-booking-card">
                    <el-skeleton-item variant="circle" style="width: 40px; height: 40px" />
                    <div class="skeleton-content">
                      <el-skeleton-item variant="text" style="width: 100px" />
                      <el-skeleton-item variant="text" style="width: 200px" />
                      <el-skeleton-item variant="text" style="width: 150px" />
                    </div>
                    <div class="skeleton-actions">
                      <el-skeleton-item variant="button" style="width: 80px; height: 32px" />
                    </div>
                  </div>
                </template>
              </el-skeleton>
            </div>
          </div>

          <div v-else-if="recentTickets.length === 0" class="empty-bookings">
            <div class="empty-illustration">
              <el-icon class="empty-icon" :size="64"><Ticket /></el-icon>
            </div>
            <h4 class="empty-title">暂无订单记录</h4>
            <p class="empty-description">您还没有预订过航班，快去搜索您心仪的航班吧！</p>
            <el-button type="primary" @click="goTo('/flights')" class="empty-action-btn">
              搜索航班
            </el-button>
          </div>

          
          <div v-else class="booking-cards-container">
            <div 
              v-for="ticket in recentTickets" 
              :key="ticket.ticketNumber"
              class="booking-card"
              @click="viewTicketDetails(ticket)"
              tabindex="0"
              role="button"
              :aria-label="`查看机票 ${getConnectingFlightNumbers(ticket)} 详情`"
            >
              <div class="booking-left">
                <div class="airline-logo">
                  <img 
                    v-if="ticket.flight?.airline?.logoUrl" 
                    :src="ticket.flight.airline.logoUrl" 
                    :alt="ticket.flight.airline.name"
                    class="airline-logo-img"
                  />
                  <div v-else class="airline-logo-placeholder">
                    <span>{{ getAirlineShortName(ticket.flight?.airline?.name) }}</span>
                  </div>
                </div>
                <div class="flight-info">
                  <div class="flight-number">{{ getConnectingFlightNumbers(ticket) }}</div>
                  <div class="route">{{ getFullRoute(ticket) }}</div>
                </div>
              </div>

              <div class="booking-middle">
                <div class="time-info">
                  <div class="departure">
                    <div class="time">{{ formatTime(getFirstFlight(ticket)?.departureTimeUtc) }}</div>
                    <div class="airport">{{ getAirportCode(getFirstFlight(ticket)?.departureAirport) }}</div>
                    <div class="terminal" v-if="getFirstFlight(ticket)?.departureTerminal">{{ getFirstFlight(ticket).departureTerminal }}</div>
                  </div>
                  <div class="flight-path">
                    <div class="path-line"></div>
                    <el-icon class="plane-icon"><Promotion /></el-icon>
                    <div class="path-line"></div>
                  </div>
                  <div class="arrival">
                    <div class="time">{{ formatTime(getLastFlight(ticket)?.arrivalTimeUtc) }}</div>
                    <div class="airport">{{ getAirportCode(getLastFlight(ticket)?.arrivalAirport) }}</div>
                    <div class="terminal" v-if="getLastFlight(ticket)?.arrivalTerminal">{{ getLastFlight(ticket).arrivalTerminal }}</div>
                    <div class="next-day" v-if="isNextDay(getFirstFlight(ticket)?.departureTimeUtc, getLastFlight(ticket)?.arrivalTimeUtc)">+1</div>
                  </div>
                </div>
                <div class="date-info">
                  {{ formatDate(getFirstFlight(ticket)?.departureTimeUtc) }}
                </div>
              </div>

              <div class="booking-right">
                <div class="status-section">
                  <div class="flight-status">
                    <el-tag 
                      :type="getFlightStatusType(ticket.flight?.status)" 
                      size="small"
                      class="status-tag"
                    >
                      <el-icon class="status-icon">
                        <component :is="getFlightStatusIcon(ticket.flight?.status)" />
                      </el-icon>
                      {{ translateFlightStatus(ticket.flight?.status) }}
                    </el-tag>
                  </div>
                  <div class="ticket-status">
                    <el-tag 
                      :type="getTicketStatusType(ticket.status)" 
                      size="small"
                      class="status-tag"
                    >
                      <el-icon class="status-icon">
                        <component :is="getTicketStatusIcon(ticket.status)" />
                      </el-icon>
                      {{ translateTicketStatus(ticket.status) }}
                    </el-tag>
                  </div>
                </div>
                <div class="actions-section">
                  <el-button 
                    size="small" 
                    type="primary"
                    @click.stop="viewTicketDetails(ticket)"
                  >
                    详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    
    <el-drawer
      v-model="ticketDetailsVisible"
      title=""
      direction="rtl"
      size="600px"
      class="ticket-details-drawer"
    >
      <template #header>
        <div class="drawer-header">
          <h3>机票详情</h3>
          <span class="ticket-number">{{ selectedTicket?.ticketNumber }}</span>
        </div>
      </template>
      
      <div v-if="selectedTicket" class="ticket-details-content">
        <div class="detail-section">
          <h4 class="section-title">航班信息</h4>
          
          
          <div class="flight-summary-card">
            <div class="flight-summary-header">
              <div class="airline-brand">
                <div class="airline-logo-large">
                  <img 
                    v-if="getFirstFlight(selectedTicket)?.airline?.logoUrl" 
                    :src="getFirstFlight(selectedTicket).airline.logoUrl" 
                    :alt="getFirstFlight(selectedTicket).airline.name"
                    class="airline-logo-img"
                  />
                  <div v-else class="airline-logo-placeholder">
                    <span>{{ getAirlineShortName(getFirstFlight(selectedTicket)?.airline?.name) }}</span>
                  </div>
                </div>
                <div>
                  <div class="flight-number-large">{{ getConnectingFlightNumbers(selectedTicket) }}</div>
                  <div class="airline-name">{{ getFirstFlight(selectedTicket)?.airline?.name || '航空公司' }}</div>
                  <div v-if="isConnectingFlight(selectedTicket)" class="connecting-indicator">
                    中转航班 ({{ selectedTicket.connectingFlights.length }}程中转)
                  </div>
                </div>
              </div>
              <div class="flight-status-large">
                <el-tag :type="getFlightStatusType(getFirstFlight(selectedTicket)?.status)" size="large">
                  {{ translateFlightStatus(getFirstFlight(selectedTicket)?.status) }}
                </el-tag>
              </div>
            </div>
            
            <div class="flight-route-detailed">
              <div class="route-endpoint departure-endpoint">
                <div class="time-large">{{ formatTime(getFirstFlight(selectedTicket)?.departureTimeUtc) }}</div>
                <div class="date">{{ formatDate(getFirstFlight(selectedTicket)?.departureTimeUtc) }}</div>
                <div class="airport-code">{{ getAirportCode(getFirstFlight(selectedTicket)?.departureAirport) }}</div>
                <div class="airport-name">{{ getFirstFlight(selectedTicket)?.departureAirport?.name }}</div>
                <div class="city-name">{{ getFirstFlight(selectedTicket)?.departureAirport?.city }}</div>
              </div>
              
              <div class="route-path-detailed">
                <div class="path-duration">
                  {{ calculateFlightDuration(getFirstFlight(selectedTicket)?.departureTimeUtc, getLastFlight(selectedTicket)?.arrivalTimeUtc) }}
                </div>
                <div class="path-line-detailed"></div>
                <el-icon class="plane-icon-large"><Promotion /></el-icon>
                <div class="path-line-detailed"></div>
              </div>
              
              <div class="route-endpoint arrival-endpoint">
                <div class="time-large">{{ formatTime(getLastFlight(selectedTicket)?.arrivalTimeUtc) }}</div>
                <div class="date">{{ formatDate(getLastFlight(selectedTicket)?.arrivalTimeUtc) }}</div>
                <div class="airport-code">{{ getAirportCode(getLastFlight(selectedTicket)?.arrivalAirport) }}</div>
                <div class="airport-name">{{ getLastFlight(selectedTicket)?.arrivalAirport?.name }}</div>
                <div class="city-name">{{ getLastFlight(selectedTicket)?.arrivalAirport?.city }}</div>
              </div>
            </div>
          </div>
          
          
          <div v-if="isConnectingFlight(selectedTicket)" class="connecting-flights-breakdown">
            <h5 class="breakdown-title">航程详情</h5>
            <div class="flight-segments">
              <div 
                v-for="(flight, index) in getAllFlights(selectedTicket)" 
                :key="flight.id"
                class="flight-segment"
              >
                <div class="segment-header">
                  <span class="segment-number">航段 {{ index + 1 }}</span>
                  <span class="segment-flight-number">{{ flight.flightNumber }}</span>
                  <el-tag 
                    :type="getFlightStatusType(flight.status)" 
                    size="small"
                  >
                    {{ translateFlightStatus(flight.status) }}
                  </el-tag>
                </div>
                <div class="segment-route">
                  <div class="segment-point">
                    <div class="segment-time">{{ formatTime(flight.departureTimeUtc) }}</div>
                    <div class="segment-airport">{{ getAirportCode(flight.departureAirport) }}</div>
                    <div class="segment-city">{{ flight.departureAirport?.city }}</div>
                  </div>
                  <div class="segment-path">
                    <div class="segment-line"></div>
                    <el-icon><Promotion /></el-icon>
                    <div class="segment-line"></div>
                  </div>
                  <div class="segment-point">
                    <div class="segment-time">{{ formatTime(flight.arrivalTimeUtc) }}</div>
                    <div class="segment-airport">{{ getAirportCode(flight.arrivalAirport) }}</div>
                    <div class="segment-city">{{ flight.arrivalAirport?.city }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">乘客信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>乘客姓名</label>
              <span>{{ selectedTicket.passengerName }}</span>
            </div>
            <div class="info-item">
              <label>身份证号</label>
              <span>{{ selectedTicket.passengerIdNumber }}</span>
            </div>
            <div class="info-item">
              <label>座位号</label>
              <span>{{ selectedTicket.seatNumber || '待分配' }}</span>
            </div>
            <div class="info-item">
              <label>舱位类型</label>
              <span>{{ getTicketTypeText(selectedTicket.ticketType) }}</span>
            </div>
          </div>
        </div>

        
        <div class="detail-section">
          <h4 class="section-title">订单信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>机票号</label>
              <span>{{ selectedTicket.ticketNumber }}</span>
            </div>
            <div class="info-item">
              <label>票价</label>
              <span class="price-large">¥{{ calculateBaseTicketPrice(selectedTicket) }}</span>
            </div>
            <div class="info-item">
              <label>预订时间</label>
              <span>{{ formatDateTime(selectedTicket.bookingTime) }}</span>
            </div>
            <div class="info-item">
              <label>支付时间</label>
              <span>{{ selectedTicket.paymentTime ? formatDateTime(selectedTicket.paymentTime) : '未支付' }}</span>
            </div>
            <div class="info-item full-width">
              <label>机票状态</label>
              <el-tag :type="getTicketStatusType(selectedTicket.status)" size="large">
                {{ translateTicketStatus(selectedTicket.status) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import dayjs from 'dayjs'
import {
  Search,
  Ticket,
  User,
  DataAnalysis,
  Location,
  Document,
  ArrowRight,
  Promotion,
  CircleCheck,
  Clock,
  Close,
  Warning,
  InfoFilled
} from '@element-plus/icons-vue'
import api from '../api'

export default {
  name: 'DashboardNew',
  components: {
    Search,
    Ticket,
    User,
    DataAnalysis,
    Location,
    Document,
    ArrowRight,
    Promotion,
    CircleCheck,
    Clock,
    Close,
    Warning,
    InfoFilled
  },
  data() {
    return {
      recentTickets: [],
      loadingTickets: false,
      frequentRoutes: [
        { id: 1, name: '北京 → 上海', from: '北京', to: '上海' },
        { id: 2, name: '上海 → 广州', from: '上海', to: '广州' }
      ],
      ticketStats: {
        total: 0,
        pending: 0,
        confirmed: 0
      },
      selectedTicket: null,
      ticketDetailsVisible: false
    }
  },
  computed: {
    ...mapState(['currentUser']),
    ...mapGetters(['isLoggedIn', 'isAdmin'])
  },
  methods: {
    goTo(path) {
      this.$router.push(path)
    },

    async loadRecentTickets() {
      this.loadingTickets = true
      try {
        const response = await api.get('/tickets/my')
        if (response.data.success) {
          this.recentTickets = response.data.data.slice(0, 3)
          this.ticketStats.total = response.data.data.length
        }
      } catch (error) {
        console.error('Failed to load recent tickets:', error)
        
        this.recentTickets = [
          {
            ticketNumber: 'T001234567',
            flight: {
              flightNumber: 'CA2345',
              airline: { name: '中国国际航空', logoUrl: null },
              departureAirport: { city: '北京', code: 'PEK', name: '北京首都国际机场' },
              arrivalAirport: { city: '上海', code: 'PVG', name: '上海浦东国际机场' },
              departureTime: '2025-08-16T09:15:00',
              arrivalTime: '2025-08-16T11:50:00',
              departureTerminal: 'T2',
              arrivalTerminal: 'T1',
              status: 'SCHEDULED'
            },
            status: 'REFUNDED',
            bookingTime: '2025-08-12T09:15:00'
          }
        ]
        this.ticketStats.total = this.recentTickets.length
      } finally {
        this.loadingTickets = false
      }
    },

    searchRoute(route) {
      this.$router.push({
        path: '/flights',
        query: {
          from: route.from,
          to: route.to
        }
      })
    },

    viewTicketDetails(ticket) {
      this.selectedTicket = ticket
      this.ticketDetailsVisible = true
    },

    handleRefund(ticket) {
      console.log('Handle refund:', ticket)
    },

    handleReschedule(ticket) {
      console.log('Handle reschedule:', ticket)
    },

    canRefund(ticket) {
      return ['PAID', 'BOOKED'].includes(ticket.status) && 
             ['SCHEDULED', 'DELAYED'].includes(ticket.flight?.status)
    },

    canReschedule(ticket) {
      return ['PAID', 'BOOKED'].includes(ticket.status) && 
             ['SCHEDULED', 'DELAYED'].includes(ticket.flight?.status)
    },

    
    formatTime(datetime) {
      return dayjs(datetime).format('HH:mm')
    },

    formatDate(datetime) {
      return dayjs(datetime).format('MM/DD')
    },

    isNextDay(departureTime, arrivalTime) {
      return dayjs(arrivalTime).date() > dayjs(departureTime).date()
    },

    getRoute(flight) {
      if (!flight) return '-'
      const departure = flight.departureAirport?.city || '未知'
      const arrival = flight.arrivalAirport?.city || '未知'
      return `${departure} → ${arrival}`
    },

    getAirportCode(airport) {
      return airport?.code || airport?.name?.slice(0, 3) || '未知'
    },

    getAirlineShortName(name) {
      if (!name) return '航'
      return name.slice(0, 2)
    },

    getFlightStatusType(status) {
      const statusTypes = {
        'SCHEDULED': 'success',
        'DELAYED': 'warning',
        'CANCELLED': 'danger',
        'DEPARTED': 'info',
        'LANDED': 'primary',
        'COMPLETED': 'info'
      }
      return statusTypes[status] || ''
    },

    getTicketStatusType(status) {
      const statusTypes = {
        'BOOKED': '',
        'PAID': 'success',
        'CHECKED_IN': 'info',
        'CANCELLED': 'danger',
        'REFUNDED': 'warning'
      }
      return statusTypes[status] || ''
    },

    getFlightStatusIcon(status) {
      const icons = {
        'SCHEDULED': 'CircleCheck',
        'DELAYED': 'Clock',
        'CANCELLED': 'Close',
        'DEPARTED': 'Promotion',
        'LANDED': 'CircleCheck',
        'COMPLETED': 'CircleCheck'
      }
      return icons[status] || 'InfoFilled'
    },

    getTicketStatusIcon(status) {
      const icons = {
        'BOOKED': 'Clock',
        'PAID': 'CircleCheck',
        'CHECKED_IN': 'CircleCheck',
        'CANCELLED': 'Close',
        'REFUNDED': 'Warning'
      }
      return icons[status] || 'InfoFilled'
    },

    translateFlightStatus(status) {
      const statusKey = status?.toLowerCase()
      const statusMap = {
        'scheduled': 'scheduled',
        'delayed': 'delayed',
        'cancelled': 'cancelled',
        'departed': 'departed',
        'landed': 'landed',
        'completed': 'completed'
      }
      return this.$t(`tickets.statuses.${statusMap[statusKey] || 'scheduled'}`)
    },

    translateTicketStatus(status) {
      const statusKey = status?.toLowerCase()
      const statusMap = {
        'booked': 'booked',
        'paid': 'paid',
        'checked_in': 'checkedIn',
        'cancelled': 'cancelled',
        'refunded': 'refunded'
      }
      return this.$t(`tickets.statuses.${statusMap[statusKey] || 'booked'}`)
    },

    formatDateTime(datetime) {
      if (!datetime) return '--'
      return dayjs(datetime).format('YYYY年MM月DD日 HH:mm')
    },

    calculateFlightDuration(departureTime, arrivalTime) {
      if (!departureTime || !arrivalTime) return '--'
      const departure = dayjs(departureTime)
      const arrival = dayjs(arrivalTime)
      const duration = arrival.diff(departure, 'minute')
      const hours = Math.floor(duration / 60)
      const minutes = duration % 60
      return hours > 0 ? `${hours}小时${minutes}分钟` : `${minutes}分钟`
    },


    getTicketTypeText(ticketType) {
      const typeMap = {
        'ECONOMY': '经济舱',
        'BUSINESS': '商务舱',
        'FIRST': '头等舱'
      }
      return typeMap[ticketType] || '经济舱'
    },

    
    isConnectingFlight(ticket) {
      return ticket && ticket.connectingFlights && ticket.connectingFlights.length > 0
    },
    
    getAllFlights(ticket) {
      if (!ticket) return []
      const flights = [ticket.flight]
      if (ticket.connectingFlights && ticket.connectingFlights.length > 0) {
        flights.push(...ticket.connectingFlights)
      }
      return flights.filter(f => f) 
    },
    
    getFirstFlight(ticket) {
      return ticket?.flight || null
    },
    
    getLastFlight(ticket) {
      if (this.isConnectingFlight(ticket)) {
        const connectingFlights = ticket.connectingFlights
        return connectingFlights[connectingFlights.length - 1]
      }
      return ticket?.flight || null
    },
    
    getFullRoute(ticket) {
      if (!ticket) return '-'
      const firstFlight = this.getFirstFlight(ticket)
      const lastFlight = this.getLastFlight(ticket)
      
      if (!firstFlight || !lastFlight) return '-'
      
      const departure = firstFlight.departureAirport?.city || '未知'
      const arrival = lastFlight.arrivalAirport?.city || '未知'
      
      if (this.isConnectingFlight(ticket)) {
        const stops = ticket.connectingFlights.length
        return `${departure} → ${arrival} (${stops}程中转)`
      }
      
      return `${departure} → ${arrival}`
    },
    
    getConnectingFlightNumbers(ticket) {
      if (!this.isConnectingFlight(ticket)) {
        return ticket.flight?.flightNumber || ''
      }
      
      const allFlights = this.getAllFlights(ticket)
      return allFlights.map(f => f.flightNumber).join(' → ')
    },
    
    
    calculateFuelSurcharge(ticket) {
      if (!ticket) return 0
      
      if (this.isConnectingFlight(ticket)) {
        
        const totalFlights = 1 + (ticket.connectingFlights?.length || 0)
        return totalFlights * 70
      } else {
        
        return 70
      }
    },
    
    
    calculateBaseTicketPrice(ticket) {
      if (!ticket) return 0
      return ticket.price - this.calculateFuelSurcharge(ticket)
    }
  },

  mounted() {
    if (!this.currentUser) {
      this.$router.push('/login')
      return
    }
    
    if (!this.isAdmin) {
      this.loadRecentTickets()
    }
  }
}
</script>

<style scoped>

.dashboard-page {
  min-height: 100vh;
  background: var(--color-bg-primary);
}


.welcome-section {
  padding: var(--space-8) 0 var(--space-6);
  background: linear-gradient(135deg, var(--color-bg-card) 0%, var(--color-bg-primary) 100%);
}

.welcome-content {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.welcome-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-4) 0;
  line-height: var(--line-height-tight);
}

.welcome-description {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-6) 0;
  line-height: var(--line-height-normal);
}

.frequent-routes {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.frequent-routes-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.route-chips {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.route-chip {
  border-radius: var(--radius-pill);
  transition: var(--transition-fast);
}


.quick-actions-section {
  padding: 0 0 var(--space-8);
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

.quick-actions-grid.admin-layout {
  grid-template-columns: repeat(3, 1fr);
}

.quick-action-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-primary);
  cursor: pointer;
  transition: var(--transition-normal);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  position: relative;
  overflow: hidden;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
  border-color: var(--color-primary);
}

.quick-action-card:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.quick-action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary), #3B82F6);
  opacity: 0;
  transition: var(--transition-fast);
}

.quick-action-card:hover::before {
  opacity: 1;
}

.card-icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--icon-xl);
  height: var(--icon-xl);
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.card-icon {
  color: var(--color-primary);
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
  line-height: var(--line-height-tight);
}

.card-description {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-3) 0;
  line-height: var(--line-height-normal);
}

.card-stats {
  display: flex;
  gap: var(--space-4);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}


.search-flights-card {
  grid-column: span 1;
}


.recent-bookings-section {
  padding: 0 0 var(--space-8);
}

.recent-bookings-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-primary);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
}


.loading-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.booking-card-skeleton {
  padding: var(--space-4);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
}

.skeleton-booking-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-actions {
  display: flex;
  gap: var(--space-2);
}


.empty-bookings {
  text-align: center;
  padding: var(--space-12) var(--space-6);
}

.empty-illustration {
  margin-bottom: var(--space-6);
}

.empty-icon {
  color: var(--color-text-tertiary);
  opacity: 0.5;
}

.empty-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-3) 0;
}

.empty-description {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-6) 0;
  line-height: var(--line-height-normal);
}

.empty-action-btn {
  padding: var(--space-3) var(--space-6);
}


.booking-cards-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.booking-card {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-4);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
  cursor: pointer;
  background: var(--color-bg-card);
}

.booking-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.booking-card:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}


.booking-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.airline-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
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
  background: var(--color-primary);
  color: white;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.flight-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.flight-number {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.route {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}


.booking-middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.time-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.departure,
.arrival {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
}

.time {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.airport {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.terminal {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.next-day {
  font-size: var(--font-size-xs);
  color: var(--color-error);
  font-weight: var(--font-weight-medium);
}

.flight-path {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 100px;
}

.path-line {
  flex: 1;
  height: 2px;
  background: var(--color-border-secondary);
  border-radius: 1px;
}

.plane-icon {
  color: var(--color-primary);
  font-size: var(--icon-sm);
  transform: rotate(90deg);
}

.date-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}


.booking-right {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: flex-end;
  flex-shrink: 0;
}

.status-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: flex-end;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-xs);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
}

.status-icon {
  font-size: var(--icon-xs);
}

.actions-section {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}


@media (max-width: 1279px) {
  .quick-actions-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-4);
  }
  
  .quick-actions-grid.admin-layout {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1023px) {
  .quick-actions-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
  }
  
  .quick-actions-grid.admin-layout {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .search-flights-card {
    grid-column: span 2;
  }
  
  .booking-card {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .booking-left,
  .booking-middle,
  .booking-right {
    align-items: stretch;
  }
  
  .time-info {
    justify-content: space-between;
  }
  
  .status-section,
  .actions-section {
    align-items: stretch;
  }
  
  .actions-section {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-2);
  }
}

@media (max-width: 767px) {
  .welcome-title {
    font-size: var(--font-size-2xl);
  }
  
  .welcome-description {
    font-size: var(--font-size-base);
  }
  
  .quick-actions-grid,
  .quick-actions-grid.admin-layout {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
  
  .search-flights-card {
    grid-column: span 1;
  }
  
  
  .booking-card {
    padding: var(--space-3);
  }
  
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  
  .frequent-routes {
    flex-direction: column;
    gap: var(--space-2);
  }
}



@keyframes cardHover {
  from { transform: translateY(0); }
  to { transform: translateY(-2px); }
}

.quick-action-card:hover {
  animation: cardHover var(--transition-fast) ease-out forwards;
}


.booking-card:focus-within {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}


@media print {
  .booking-card {
    border: 1px solid #000;
    break-inside: avoid;
  }
  
  .quick-action-card {
    break-inside: avoid;
  }
}


.ticket-details-drawer :deep(.el-drawer__header) {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border-primary);
  margin-bottom: 0;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
}

.ticket-number {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.ticket-details-content {
  padding: var(--space-4);
}

.detail-section {
  margin-bottom: var(--space-6);
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-3) 0;
  padding-bottom: var(--space-2);
  border-bottom: 2px solid var(--color-primary);
}

.flight-summary-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.flight-summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.airline-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.airline-logo-large {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
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
  background: var(--color-primary);
  color: white;
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
}

.flight-number-large {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.airline-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.flight-route-detailed {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.route-endpoint {
  text-align: center;
  flex: 1;
}

.time-large {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.date {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.airport-code {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.airport-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.city-name {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.route-path-detailed {
  flex: 1;
  text-align: center;
  margin: 0 var(--space-4);
}

.path-duration {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.path-line-detailed {
  height: 2px;
  background: var(--color-border-primary);
  margin: var(--space-2) 0;
}

.plane-icon-large {
  font-size: 24px;
  color: var(--color-primary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.info-item span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.price-large {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-success);
}


.connecting-indicator {
  font-size: var(--font-size-xs);
  color: var(--color-warning);
  font-weight: var(--font-weight-medium);
  margin-top: var(--space-1);
}

.connecting-flights-breakdown {
  margin-top: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.breakdown-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-3) 0;
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border-secondary);
}

.flight-segments {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.flight-segment {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
}

.segment-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.segment-number {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
}

.segment-flight-number {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.segment-route {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.segment-point {
  text-align: center;
  flex-shrink: 0;
}

.segment-time {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.segment-airport {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--space-1);
}

.segment-city {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.segment-path {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.segment-line {
  flex: 1;
  height: 2px;
  background: var(--color-border-secondary);
  border-radius: 1px;
}
</style>
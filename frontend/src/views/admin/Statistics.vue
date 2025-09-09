<template>
  <div class="admin-statistics">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><DataAnalysis /></el-icon>
        系统统计分析
      </h1>
      <p class="page-description">
        全面的数据分析和可视化图表，帮助您了解系统运营状况
      </p>
    </div>

    <el-row :gutter="24" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card flights-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-number">{{ stats.totalFlights || 0 }}</div>
              <div class="metric-label">总航班数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card tickets-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon><Ticket /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-number">{{ stats.totalTickets || 0 }}</div>
              <div class="metric-label">总机票数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card revenue-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-number">¥{{ formatMoney(stats.totalRevenue || 0) }}</div>
              <div class="metric-label">总收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card seats-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon><Position /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-number">{{ stats.availableSeats || 0 }}</div>
              <div class="metric-label">可用座位</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="charts-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3 class="chart-title">每日航班量变化</h3>
                <el-button-group>
                  <el-button 
                    size="small" 
                    :type="flightDays === 7 ? 'primary' : ''" 
                    @click="updateFlightChart(7)"
                  >
                    7天
                  </el-button>
                  <el-button 
                    size="small" 
                    :type="flightDays === 30 ? 'primary' : ''" 
                    @click="updateFlightChart(30)"
                  >
                    30天
                  </el-button>
                </el-button-group>
              </div>
            </template>
            <div class="chart-container">
              <v-chart 
                v-if="flightVolumeData && flightVolumeData.length > 0"
                key="flight-volume-chart"
                class="chart" 
                :option="flightVolumeOption" 
                :loading="loadingFlightChart"
                @click="onFlightChartClick"
              />
              <div v-else-if="!loadingFlightChart" class="chart-placeholder">
                暂无数据
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3 class="chart-title">航空公司航班占比</h3>
                <el-button 
                  size="small" 
                  type="primary" 
                  :loading="loadingAirlineChart"
                  @click="refreshAirlineChart"
                >
                  <el-icon><Refresh /></el-icon>
                  更新
                </el-button>
              </div>
            </template>
            <div class="chart-container">
              <v-chart 
                v-if="airlineDistributionData && airlineDistributionData.length > 0"
                :key="`airline-distribution-chart-${flightDataVersion}`"
                class="chart" 
                :option="airlineDistributionOption" 
                :loading="loadingAirlineChart"
              />
              <div v-else-if="!loadingAirlineChart" class="chart-placeholder">
                暂无数据
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3 class="chart-title">预订趋势分析</h3>
                <el-button-group>
                  <el-button 
                    size="small" 
                    :type="bookingDays === 7 ? 'primary' : ''" 
                    @click="updateBookingChart(7)"
                  >
                    7天
                  </el-button>
                  <el-button 
                    size="small" 
                    :type="bookingDays === 30 ? 'primary' : ''" 
                    @click="updateBookingChart(30)"
                  >
                    30天
                  </el-button>
                </el-button-group>
              </div>
            </template>
            <div class="chart-container">
              <v-chart 
                v-if="bookingTrendsData && bookingTrendsData.length > 0"
                key="booking-trends-chart"
                class="chart" 
                :option="bookingTrendsOption" 
                :loading="loadingBookingChart"
              />
              <div v-else-if="!loadingBookingChart" class="chart-placeholder">
                暂无数据
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <h3 class="chart-title">月度收入分析</h3>
            </template>
            <div class="chart-container">
              <v-chart 
                v-if="revenueAnalysisData && revenueAnalysisData.length > 0"
                key="revenue-analysis-chart"
                class="chart" 
                :option="revenueAnalysisOption" 
                :loading="loadingRevenueChart"
              />
              <div v-else-if="!loadingRevenueChart" class="chart-placeholder">
                暂无数据
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="24">
          <el-card class="chart-card">
            <template #header>
              <h3 class="chart-title">热门航线排行</h3>
            </template>
            <div class="chart-container">
              <v-chart 
                v-if="popularRoutesData && popularRoutesData.length > 0"
                key="popular-routes-chart"
                class="chart horizontal-chart" 
                :option="popularRoutesOption" 
                :loading="loadingRoutesChart"
              />
              <div v-else-if="!loadingRoutesChart" class="chart-placeholder">
                暂无数据
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

  </div>
</template>

<script>
import api from '../../api'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  DataAnalysis,
  Location,
  Ticket,
  Money,
  Position,
  Refresh
} from '@element-plus/icons-vue'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

export default {
  name: 'AdminStatistics',
  components: {
    VChart,
    DataAnalysis,
    Location,
    Ticket,
    Money,
    Position,
    Refresh
  },
  data() {
    return {
      stats: {},
      loading: false,
      flightDays: 30,
      bookingDays: 30,

      loadingFlightChart: false,
      loadingAirlineChart: false,
      loadingBookingChart: false,
      loadingRevenueChart: false,
      loadingRoutesChart: false,

      flightVolumeData: [],
      airlineDistributionData: [],
      bookingTrendsData: [],
      revenueAnalysisData: [],
      popularRoutesData: [],

      unsubscribe: null,
      handleStorageChange: null
    }
  },
  methods: {
    async loadStats() {
      try {
        this.loading = true
        const response = await api.get('/statistics/dashboard')
        if (response.data.success) {
          this.stats = response.data.data
        }
      } catch (error) {
        console.error('Load stats error:', error)
        ElMessage.error('加载统计数据失败')
      } finally {
        this.loading = false
      }
    },

    async loadFlightVolumeData(days = 30) {
      this.loadingFlightChart = true
      try {
        const response = await api.get(`/statistics/daily-flights?days=${days}`)
        if (response.data.success) {
          this.flightVolumeData = response.data.data
        }
      } catch (error) {
        console.error('Load flight volume error:', error)
        ElMessage.error('加载航班量数据失败')
      } finally {
        this.loadingFlightChart = false
      }
    },

    async loadAirlineDistribution() {
      console.log('[Statistics] Loading airline distribution data...')
      this.loadingAirlineChart = true
      try {
        const response = await api.get('/flights')
        if (response.data.success) {
          const flights = response.data.data
          console.log('[Statistics] Flight data loaded for airline distribution:', flights.length, 'flights')

          const airlineStats = this.calculateAirlineDistribution(flights)
          this.airlineDistributionData = airlineStats
          
          console.log('[Statistics] Airline distribution calculated:', airlineStats)
          
          // Force chart re-render by using nextTick
          await this.$nextTick()
          console.log('[Statistics] Chart data updated and DOM refreshed')
        }
      } catch (error) {
        console.error('Load airline distribution error:', error)
        ElMessage.error('加载航空公司分布数据失败')
      } finally {
        this.loadingAirlineChart = false
      }
    },

    async loadBookingTrends(days = 30) {
      this.loadingBookingChart = true
      try {
        const response = await api.get(`/statistics/booking-trends?days=${days}`)
        if (response.data.success) {
          this.bookingTrendsData = response.data.data
        }
      } catch (error) {
        console.error('Load booking trends error:', error)
        ElMessage.error('加载预订趋势数据失败')
      } finally {
        this.loadingBookingChart = false
      }
    },

    async loadRevenueAnalysis() {
      this.loadingRevenueChart = true
      try {
        const response = await api.get('/statistics/revenue-analysis')
        if (response.data.success) {
          this.revenueAnalysisData = response.data.data
        }
      } catch (error) {
        console.error('Load revenue analysis error:', error)
        ElMessage.error('加载收入分析数据失败')
      } finally {
        this.loadingRevenueChart = false
      }
    },

    async loadPopularRoutes() {
      this.loadingRoutesChart = true
      try {
        const response = await api.get('/statistics/popular-routes')
        if (response.data.success) {
          this.popularRoutesData = response.data.data
        }
      } catch (error) {
        console.error('Load popular routes error:', error)
        ElMessage.error('加载热门航线数据失败')
      } finally {
        this.loadingRoutesChart = false
      }
    },

    async updateFlightChart(days) {
      this.flightDays = days
      await this.loadFlightVolumeData(days)
    },

    async updateBookingChart(days) {
      this.bookingDays = days
      await this.loadBookingTrends(days)
    },

    onFlightChartClick(params) {
      // Handle chart click events if needed
      console.log('Flight chart clicked:', params)
    },

    formatMoney(value) {
      if (value >= 10000) {
        return (value / 10000).toFixed(1) + '万'
      }
      return value.toLocaleString()
    },


    async refreshAirlineChart() {
      console.log('[Statistics] Manual refresh button clicked for airline chart')
      await this.loadAirlineDistribution()
      // Also increment flight data version to trigger chart re-render
      this.$store.dispatch('notifyFlightDataUpdate')
      ElMessage.success('航空公司航班占比图表已更新')
    },

    calculateAirlineDistribution(flights) {
      // Airline mapping based on flight number prefixes (first 2 letters)
      const airlineMap = {
        'CA': '中国国际航空',
        'MU': '中国东方航空', 
        'CZ': '中国南方航空',
        'HU': '海南航空',
        '3U': '四川航空',
        'MF': '厦门航空',
        'FM': '上海航空',
        'HO': '吉祥航空',
        'G5': '华夏航空',
        'EU': '成都航空',
        'TV': '西藏航空',
        'DR': '瑞丽航空',
        'KN': '联合航空',
        'JR': '幸福航空',
        'OQ': '青岛航空',
        'NS': '河北航空'
      }

      // Count flights by airline prefix
      const airlineCounts = {}
      
      flights.forEach(flight => {
        if (flight.flightNumber) {
          // Extract first 2 characters as airline code
          const airlineCode = flight.flightNumber.substring(0, 2).toUpperCase()
          const airlineName = airlineMap[airlineCode] || `${airlineCode}航空`
          
          if (!airlineCounts[airlineCode]) {
            airlineCounts[airlineCode] = {
              code: airlineCode,
              name: airlineName,
              value: 0
            }
          }
          airlineCounts[airlineCode].value += 1
        }
      })

      // Convert to array format expected by the chart
      const result = Object.values(airlineCounts)
        .sort((a, b) => b.value - a.value) // Sort by flight count descending
      
      console.log('[Statistics] Airline distribution calculation:', {
        totalFlights: flights.length,
        uniqueAirlines: result.length,
        distribution: result
      })
      
      return result
    }
  },

  computed: {
    // Flight Volume Chart Option
    flightVolumeOption() {
      if (!this.flightVolumeData || this.flightVolumeData.length === 0) {
        return {}
      }
      
      return {
        title: {
          show: false
        },
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const data = params[0]
            return `${data.name}<br/>航班数量: ${data.value}班`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.flightVolumeData.map(item => item.date),
          axisLine: {
            lineStyle: {
              color: '#e4e7ed'
            }
          },
          axisLabel: {
            color: '#909399'
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: '#909399'
          },
          splitLine: {
            lineStyle: {
              color: '#f5f7fa'
            }
          }
        },
        series: [{
          name: '航班数量',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: this.flightVolumeData.map(item => item.value),
          lineStyle: {
            color: '#409EFF',
            width: 3
          },
          itemStyle: {
            color: '#409EFF'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(64, 158, 255, 0.3)'
              }, {
                offset: 1, color: 'rgba(64, 158, 255, 0.05)'
              }]
            }
          }
        }]
      }
    },

    // Airline Distribution Chart Option
    airlineDistributionOption() {
      if (!this.airlineDistributionData || this.airlineDistributionData.length === 0) {
        return {}
      }
      
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#FF6B9D', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7']
      return {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c}班 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          textStyle: {
            color: '#606266'
          }
        },
        series: [{
          name: '航班数量',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          data: this.airlineDistributionData.map((item, index) => ({
            name: item.name,
            value: item.value,
            code: item.code,
            itemStyle: {
              color: colors[index % colors.length]
            }
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          labelLine: {
            show: false
          },
          label: {
            show: true,
            position: 'inside',
            formatter: '{d}%',
            fontSize: 12,
            color: '#fff'
          }
        }]
      }
    },

    // Booking Trends Chart Option
    bookingTrendsOption() {
      if (!this.bookingTrendsData || this.bookingTrendsData.length === 0) {
        return {}
      }
      
      return {
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const bookings = params.find(p => p.seriesName === '预订数量')
            const revenue = params.find(p => p.seriesName === '收入')
            return `${bookings.name}<br/>
                    预订数量: ${bookings.value}张<br/>
                    收入: ¥${revenue.value.toFixed(2)}`
          }
        },
        legend: {
          data: ['预订数量', '收入'],
          textStyle: {
            color: '#606266'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: this.bookingTrendsData.map(item => item.date),
          axisLine: {
            lineStyle: {
              color: '#e4e7ed'
            }
          },
          axisLabel: {
            color: '#909399'
          }
        },
        yAxis: [{
          type: 'value',
          name: '预订数量',
          axisLabel: {
            color: '#909399'
          },
          splitLine: {
            lineStyle: {
              color: '#f5f7fa'
            }
          }
        }, {
          type: 'value',
          name: '收入 (¥)',
          axisLabel: {
            color: '#909399',
            formatter: '¥{value}'
          },
          splitLine: {
            show: false
          }
        }],
        series: [{
          name: '预订数量',
          type: 'bar',
          data: this.bookingTrendsData.map(item => item.bookings),
          itemStyle: {
            color: '#67C23A'
          }
        }, {
          name: '收入',
          type: 'line',
          yAxisIndex: 1,
          data: this.bookingTrendsData.map(item => item.revenue),
          lineStyle: {
            color: '#E6A23C',
            width: 3
          },
          itemStyle: {
            color: '#E6A23C'
          }
        }]
      }
    },

    // Revenue Analysis Chart Option
    revenueAnalysisOption() {
      if (!this.revenueAnalysisData || this.revenueAnalysisData.length === 0) {
        return {}
      }
      
      return {
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const data = params[0]
            return `${data.name}<br/>收入: ¥${data.value.toFixed(2)}`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: this.revenueAnalysisData.map(item => item.month),
          axisLine: {
            lineStyle: {
              color: '#e4e7ed'
            }
          },
          axisLabel: {
            color: '#909399'
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: '#909399',
            formatter: '¥{value}'
          },
          splitLine: {
            lineStyle: {
              color: '#f5f7fa'
            }
          }
        },
        series: [{
          type: 'bar',
          data: this.revenueAnalysisData.map(item => item.revenue),
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: '#409EFF'
              }, {
                offset: 1, color: '#66B2FF'
              }]
            }
          },
          emphasis: {
            itemStyle: {
              color: '#337ECC'
            }
          }
        }]
      }
    },

    // Popular Routes Chart Option
    popularRoutesOption() {
      if (!this.popularRoutesData || this.popularRoutesData.length === 0) {
        return {}
      }
      
      return {
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const data = params[0]
            return `${data.name}<br/>预订次数: ${data.value}次`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: {
            color: '#909399'
          },
          splitLine: {
            lineStyle: {
              color: '#f5f7fa'
            }
          }
        },
        yAxis: {
          type: 'category',
          data: this.popularRoutesData.map(item => item.route),
          axisLine: {
            lineStyle: {
              color: '#e4e7ed'
            }
          },
          axisLabel: {
            color: '#606266'
          }
        },
        series: [{
          type: 'bar',
          data: this.popularRoutesData.map(item => item.count),
          itemStyle: {
            color: '#F56C6C'
          },
          emphasis: {
            itemStyle: {
              color: '#F23030'
            }
          }
        }]
      }
    },
    
    // Add a computed property to watch the flight data version
    flightDataVersion() {
      const version = this.$store.getters.flightDataVersion
      console.log(`[Statistics] Computed flightDataVersion accessed: ${version}`)
      return version
    }
  },

  watch: {
    // Watch for changes in flight data version and refresh airline chart
    flightDataVersion: {
      handler(newVersion, oldVersion) {
        console.log(`[Statistics] Watcher triggered - Flight data updated: ${oldVersion} → ${newVersion}`)
        if (newVersion !== oldVersion && newVersion > 0) {
          console.log('[Statistics] Conditions met, refreshing airline distribution chart and stats')
          this.loadAirlineDistribution()
          // Also update the main stats to reflect flight count changes
          this.loadStats()
        } else {
          console.log('[Statistics] Conditions not met for refresh:', { 
            versionsDiffer: newVersion !== oldVersion, 
            newVersionPositive: newVersion > 0 
          })
        }
      },
      immediate: false,
      deep: false
    }
  },

  async mounted() {
    console.log('[Statistics] Component mounted, setting up flight data listener')
    try {
      await this.loadStats()
      
      // Load chart data with staggered delays to prevent race conditions
      await this.loadFlightVolumeData(this.flightDays)
      await this.$nextTick()
      
      await this.loadAirlineDistribution()
      await this.$nextTick()
      
      await this.loadBookingTrends(this.bookingDays)
      await this.$nextTick()
      
      await this.loadRevenueAnalysis()
      await this.$nextTick()
      
      await this.loadPopularRoutes()
      
      // Set up direct store subscription as backup
      this.unsubscribe = this.$store.subscribe((mutation) => {
        if (mutation.type === 'FLIGHT_DATA_UPDATED') {
          console.log('[Statistics] Direct store subscription detected FLIGHT_DATA_UPDATED')
          this.loadAirlineDistribution()
          this.loadStats()
        }
      })
      
      // Set up localStorage listener for cross-tab communication
      this.handleStorageChange = (event) => {
        if (event.key === 'flightDataUpdated') {
          console.log('[Statistics] localStorage flightDataUpdated event detected:', event.newValue)
          this.loadAirlineDistribution()
          this.loadStats()
        }
      }
      window.addEventListener('storage', this.handleStorageChange)
      console.log('[Statistics] localStorage event listener added')
      
    } catch (error) {
      console.error('Failed to load statistics data:', error)
      ElMessage.error('加载统计数据时发生错误')
    }
  },
  
  beforeUnmount() {
    console.log('[Statistics] Component unmounting, cleaning up subscriptions')
    // Clean up store subscription
    if (this.unsubscribe) {
      this.unsubscribe()
    }
    
    // Clean up localStorage event listener
    if (this.handleStorageChange) {
      window.removeEventListener('storage', this.handleStorageChange)
      console.log('[Statistics] localStorage event listener removed')
    }
    
    // Clean up any potential references
    this.flightVolumeData = []
    this.airlineDistributionData = []
    this.bookingTrendsData = []
    this.revenueAnalysisData = []
    this.popularRoutesData = []
  }
}
</script>

<style scoped>
.admin-statistics {
  min-height: calc(100vh - var(--header-height));
  background: var(--color-bg-primary);
  padding: var(--space-6);
}

/* Page Header */
.page-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-4) 0;
}

.title-icon {
  font-size: var(--icon-xl);
  color: var(--color-primary);
}

.page-description {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: 0;
}

/* Metrics Cards */
.metrics-row {
  margin-bottom: var(--space-8);
}

.metric-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: default;
  transition: var(--transition-fast);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.flights-card {
  border-left: 4px solid var(--color-primary);
}

.tickets-card {
  border-left: 4px solid var(--color-success);
}

.revenue-card {
  border-left: 4px solid var(--color-warning);
}

.seats-card {
  border-left: 4px solid var(--color-info);
}

.metric-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--icon-lg);
  color: white;
}

.flights-card .metric-icon {
  background: var(--color-primary);
}

.tickets-card .metric-icon {
  background: var(--color-success);
}

.revenue-card .metric-icon {
  background: var(--color-warning);
}

.seats-card .metric-icon {
  background: var(--color-info);
}

.metric-info {
  flex: 1;
}

.metric-number {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
  margin-bottom: var(--space-1);
}

.metric-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

/* Charts Section */
.charts-section {
  margin-bottom: var(--space-8);
}

.chart-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--space-6);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.chart-container {
  padding: var(--space-4) 0;
}

.chart {
  width: 100%;
  height: 300px;
}

.horizontal-chart {
  height: 400px;
}

.chart-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}


/* Responsive Design */
@media (max-width: 1023px) {
  .admin-statistics {
    padding: var(--space-4);
  }

  .page-title {
    font-size: var(--font-size-2xl);
  }

  .metric-content {
    flex-direction: column;
    text-align: center;
    gap: var(--space-2);
  }

  .chart {
    height: 250px;
  }

  .horizontal-chart {
    height: 300px;
  }
}

@media (max-width: 767px) {
  .page-header {
    margin-bottom: var(--space-6);
  }

  .metrics-row {
    margin-bottom: var(--space-6);
  }

}

/* Dark mode */
.dark .metric-card {
  background: var(--color-bg-secondary);
  border-color: var(--color-border-primary);
}

.dark .chart-card {
  background: var(--color-bg-secondary);
}


/* Loading states */
.chart[loading] {
  position: relative;
}

.chart[loading]::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border-primary);
  border-top: 3px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}
</style>
<template>
  <div class="flight-search-page">
    <!-- Header Section -->
    <header class="page-header">
      <div class="container">
        <div class="header-content">
          <div class="page-title-section">
            <h1 class="page-title">{{ $t('flights.searchTitle') }}</h1>
            <p class="page-subtitle">在这里预订您的航班。</p>
          </div>
        </div>
      </div>
    </header>

    <!-- Search Form Section -->
    <section class="search-form-section">
      <div class="container">
        <div class="search-form-card">
          <el-form 
            ref="searchFormRef" 
            :model="searchForm" 
            :rules="searchRules"
            class="flight-search-form"
            @submit.prevent="handleSearch"
          >
            <!-- Flight Options -->
            <div class="flight-options-section">
              <div class="option-group">
                <el-checkbox v-model="searchForm.includeConnecting" size="large">
                  显示中转航班
                </el-checkbox>
                <span class="option-description">包含需要中转的航班选项</span>
              </div>
            </div>

            <!-- Main Search Fields -->
            <div class="search-fields-grid">
              <!-- Origin and Destination -->
              <div class="location-row">
                <div class="location-field">
                  <label class="field-label">出发城市</label>
                  <el-form-item prop="origin">
                    <el-select
                      v-model="searchForm.origin"
                      placeholder="请选择出发城市"
                      filterable
                      clearable
                      class="location-select"
                    >
                      <!-- Domestic Cities -->
                      <el-option-group label="国内城市" v-if="groupedAirports.domestic.length > 0">
                        <el-option
                          v-for="cityGroup in groupedAirports.domestic"
                          :key="cityGroup.city"
                          :label="cityGroup.city"
                          :value="cityGroup.city"
                          class="city-option"
                        >
                          <div class="city-info">
                            <span class="city-name">{{ cityGroup.city }}</span>
                            <span class="airport-count" v-if="cityGroup.airports.length > 1">
                              {{ cityGroup.airports.length }}个机场
                            </span>
                            <span class="airport-codes">
                              {{ cityGroup.airports.map(a => a.code).join(', ') }}
                            </span>
                          </div>
                        </el-option>
                      </el-option-group>

                      <!-- International Cities -->
                      <el-option-group label="国际城市" v-if="groupedAirports.international.length > 0">
                        <el-option
                          v-for="cityGroup in groupedAirports.international"
                          :key="cityGroup.city"
                          :label="cityGroup.city"
                          :value="cityGroup.city"
                          class="city-option"
                        >
                          <div class="city-info">
                            <span class="city-name">{{ cityGroup.city }}</span>
                            <span class="airport-count" v-if="cityGroup.airports.length > 1">
                              {{ cityGroup.airports.length }}个机场
                            </span>
                            <span class="airport-codes">
                              {{ cityGroup.airports.map(a => a.code).join(', ') }}
                            </span>
                          </div>
                        </el-option>
                      </el-option-group>
                    </el-select>
                  </el-form-item>
                </div>

                <!-- Swap Button -->
                <div class="swap-button-container">
                  <el-button 
                    circle 
                    @click="swapLocations"
                    class="swap-btn"
                    :disabled="!searchForm.origin && !searchForm.destination"
                  >
                    <el-icon><Switch /></el-icon>
                  </el-button>
                </div>

                <div class="location-field">
                  <label class="field-label">目的地城市</label>
                  <el-form-item prop="destination">
                    <el-select
                      v-model="searchForm.destination"
                      placeholder="请选择目的地城市"
                      filterable
                      clearable
                      class="location-select"
                    >
                      <!-- Domestic Cities -->
                      <el-option-group label="国内城市" v-if="availableDestinations.domestic.length > 0">
                        <el-option
                          v-for="cityGroup in availableDestinations.domestic"
                          :key="cityGroup.city"
                          :label="cityGroup.city"
                          :value="cityGroup.city"
                          class="city-option"
                        >
                          <div class="city-info">
                            <span class="city-name">{{ cityGroup.city }}</span>
                            <span class="airport-count" v-if="cityGroup.airports.length > 1">
                              {{ cityGroup.airports.length }}个机场
                            </span>
                            <span class="airport-codes">
                              {{ cityGroup.airports.map(a => a.code).join(', ') }}
                            </span>
                          </div>
                        </el-option>
                      </el-option-group>

                      <!-- International Cities -->
                      <el-option-group label="国际城市" v-if="availableDestinations.international.length > 0">
                        <el-option
                          v-for="cityGroup in availableDestinations.international"
                          :key="cityGroup.city"
                          :label="cityGroup.city"
                          :value="cityGroup.city"
                          class="city-option"
                        >
                          <div class="city-info">
                            <span class="city-name">{{ cityGroup.city }}</span>
                            <span class="airport-count" v-if="cityGroup.airports.length > 1">
                              {{ cityGroup.airports.length }}个机场
                            </span>
                            <span class="airport-codes">
                              {{ cityGroup.airports.map(a => a.code).join(', ') }}
                            </span>
                          </div>
                        </el-option>
                      </el-option-group>
                    </el-select>
                  </el-form-item>
                </div>
              </div>

              <!-- Date Fields -->
              <div class="date-row">
                <div class="date-field">
                  <label class="field-label">出发日期</label>
                  <el-form-item prop="departureDate">
                    <el-date-picker
                      v-model="searchForm.departureDate"
                      type="date"
                      placeholder="选择出发日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      :disabled-date="disabledDate"
                      class="date-picker"
                    />
                  </el-form-item>
                </div>
              </div>

            </div>

            <!-- Search Button -->
            <div class="search-button-section">
              <el-button
                type="primary"
                size="large"
                @click="handleSearch"
                :loading="searching"
                class="search-btn"
              >
                <el-icon><Search /></el-icon>
                搜索航班
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </section>

    <!-- Search Results Section -->
    <section class="search-results-section" v-if="searchResults.length > 0 || searching">
      <div class="container">
        <div class="results-header">
          <h2 class="results-title">搜索结果</h2>
          <div class="results-controls">
            <div class="results-info" v-if="!searching">
              <span>找到 {{ searchResults.length }} 个航班</span>
            </div>
            <div class="sort-controls" v-if="!searching && searchResults.length > 0">
              <label class="sort-label">排序方式：</label>
              <el-select 
                v-model="sortOption" 
                placeholder="选择排序方式"
                size="small"
                style="width: 180px"
                @change="handleSortChange"
              >
                <el-option
                  label="出发时间（早到晚）"
                  value="departureTimeAsc"
                />
                <el-option
                  label="出发时间（晚到早）"
                  value="departureTimeDesc"
                />
                <el-option
                  label="价格（低到高）"
                  value="priceAsc"
                />
                <el-option
                  label="价格（高到低）"
                  value="priceDesc"
                />
              </el-select>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="searching" class="loading-state">
          <el-skeleton animated :rows="5" />
        </div>

        <!-- Results List -->
        <div v-else class="results-list">
          <div
            v-for="flight in searchResults"
            :key="flight.id"
            class="flight-result-card"
            @click="selectFlight(flight)"
          >
            <div class="flight-info">
              <div class="airline-section">
                <div class="airline-logo">
                  <img
                    v-if="flight.airline?.logoUrl"
                    :src="flight.airline.logoUrl"
                    :alt="flight.airline.name"
                    class="airline-logo-img"
                  />
                  <div v-else class="airline-logo-placeholder">
                    <span>{{ getAirlineShortName(flight.airline?.name) }}</span>
                  </div>
                </div>
                <div class="airline-details">
                  <div class="flight-number">{{ flight.flightNumber }}</div>
                  <div class="airline-name">{{ flight.airline?.name }}</div>
                </div>
              </div>

              <div class="route-section">
                <div class="departure-info">
                  <div class="time-section">
                    <div class="local-time-label" v-if="isInternationalFlight(flight.departureAirport?.city, flight.arrivalAirport?.city)">当地时间</div>
                    <div class="time">
                      {{ isInternationalFlight(flight.departureAirport?.city, flight.arrivalAirport?.city) 
                         ? formatLocalTime(flight.departureTimeUtc, flight.departureAirport?.city) 
                         : formatTime(flight.departureTimeUtc) }}
                    </div>
                  </div>
                  <div class="airport">{{ getAirportCode(flight.departureAirport) }}</div>
                  <div class="airport-name">{{ flight.departureAirport?.name }}</div>
                </div>
                
                <div class="flight-path">
                  <div class="duration">{{ calculateDuration(flight.departureTimeUtc, flight.arrivalTimeUtc) }}</div>
                  <div class="path-visual">
                    <div class="path-line"></div>
                    <el-icon class="plane-icon"><Promotion /></el-icon>
                    <div class="path-line" v-if="flight.isConnecting"></div>
                    <el-icon v-if="flight.isConnecting" class="plane-icon transfer-icon"><Promotion /></el-icon>
                    <div class="path-line"></div>
                  </div>
                  <div class="flight-type" :class="{ connecting: flight.isConnecting }">
                    {{ flight.isConnecting ? `${flight.flights ? flight.flights.length : 2}段中转` : '直飞' }}
                  </div>
                  
                  <!-- Cross-airport transfer notice -->
                  <div v-if="hasCrossAirportTransfer(flight)" class="transfer-notice">
                    <div class="transfer-warning">
                      <el-icon class="warning-icon"><Warning /></el-icon>
                      <span>需要跨机场中转</span>
                    </div>
                    <div class="transfer-details">
                      <div v-for="(transfer, index) in getCrossAirportTransfers(flight)" :key="index" class="transfer-item">
                        在{{ transfer.city }}：{{ transfer.from }} → {{ transfer.to }}
                        <span class="transfer-time" v-if="transfer.transferTime">
                          ({{ formatTransferTime(transfer.transferTime) }})
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="arrival-info">
                  <div class="time-section">
                    <div class="local-time-label" v-if="isInternationalFlight(flight.departureAirport?.city, flight.arrivalAirport?.city)">当地时间</div>
                    <div class="time-container">
                      <div class="time">
                        {{ isInternationalFlight(flight.departureAirport?.city, flight.arrivalAirport?.city) 
                           ? formatLocalTime(flight.arrivalTimeUtc, flight.arrivalAirport?.city) 
                           : formatTime(flight.arrivalTimeUtc) }}
                      </div>
                      <span class="next-day-indicator" v-if="isNextDayArrival(flight.departureTimeUtc, flight.arrivalTimeUtc)">
                        +1天
                      </span>
                    </div>
                  </div>
                  <div class="airport">{{ getAirportCode(flight.arrivalAirport) }}</div>
                  <div class="airport-name">{{ flight.arrivalAirport?.name }}</div>
                </div>
              </div>

              <div class="price-section">
                <div class="price">¥{{ flight.price }}</div>
                <div class="availability-info" v-if="flight.availableSeats !== undefined">
                  <span class="seats-label">剩余票数：</span>
                  <span class="seats-count" :class="getSeatAvailabilityClass(flight.availableSeats)">
                    {{ flight.availableSeats }}
                  </span>
                </div>
                <el-button type="primary" size="small">选择</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="!searching && searchResults.length === 0 && hasSearched" class="empty-results">
          <el-empty description="未找到符合条件的航班">
            <el-button type="primary" @click="resetSearch">重新搜索</el-button>
          </el-empty>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { 
  Search, 
  Switch, 
  Promotion,
  Warning
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import api from '../api'

// Extend dayjs with timezone plugins
dayjs.extend(utc)
dayjs.extend(timezone)

export default {
  name: 'FlightSearch',
  components: {
    Search,
    Switch,
    Promotion,
    Warning
  },
  data() {
    return {
      searchForm: {
        origin: '',
        destination: '',
        departureDate: '',
        includeConnecting: false
      },
      airports: [
        { id: 1, city: '北京', name: '北京首都国际机场', code: 'PEK' },
        { id: 2, city: '上海', name: '上海浦东国际机场', code: 'PVG' },
        { id: 3, city: '广州', name: '广州白云国际机场', code: 'CAN' },
        { id: 4, city: '深圳', name: '深圳宝安国际机场', code: 'SZX' },
        { id: 5, city: '成都', name: '成都天府国际机场', code: 'TFU' }
      ],
      searchResults: [],
      searching: false,
      hasSearched: false,
      sortOption: '',
      searchRules: {}  // Will be set in created() hook
    }
  },
  computed: {
    // Group airports by city and separate domestic/international
    groupedAirports() {
      if (!Array.isArray(this.airports) || this.airports.length === 0) {
        return { domestic: [], international: [] }
      }
      
      const domesticCities = [
        '北京', '上海', '广州', '深圳', '成都', '杭州', '南京', '西安', '重庆', '天津', 
        '武汉', '沈阳', '大连', '青岛', '厦门', '福州', '长沙', '郑州', '昆明', '乌鲁木齐', 
        '哈尔滨', '长春', '石家庄', '太原', '合肥', '南昌', '贵阳', '兰州', '银川', '西宁', 
        '拉萨', '呼和浩特', 
        // 🔧 Missing domestic cities that were incorrectly in international list
        '海口', '济南', '南宁', '珠海'
      ]
      
      const cityGroups = new Map()
      
      this.airports.forEach(airport => {
        if (!airport || !airport.city) return
        
        if (!cityGroups.has(airport.city)) {
          cityGroups.set(airport.city, {
            city: airport.city,
            airports: [],
            primaryAirport: airport, // Use first airport as primary
            isDomestic: domesticCities.includes(airport.city)
          })
        }
        
        cityGroups.get(airport.city).airports.push(airport)
      })
      
      // Separate domestic and international cities
      const domestic = []
      const international = []
      
      Array.from(cityGroups.values()).forEach(group => {
        if (group.isDomestic) {
          domestic.push(group)
        } else {
          international.push(group)
        }
      })
      
      // Sort each group by city name
      domestic.sort((a, b) => a.city.localeCompare(b.city, 'zh-CN'))
      international.sort((a, b) => a.city.localeCompare(b.city, 'zh-CN'))
      
      return { domestic, international }
    },

    // Filter out the selected origin from destination options
    availableDestinations() {
      if (!this.searchForm?.origin) {
        return this.groupedAirports
      }
      
      // Filter out the selected origin from both domestic and international
      return {
        domestic: this.groupedAirports.domestic.filter(group => group.city !== this.searchForm.origin),
        international: this.groupedAirports.international.filter(group => group.city !== this.searchForm.origin)
      }
    }
  },
  methods: {
    // Timezone utilities
    getCityTimezone(city) {
      const timezoneMap = {
        // International cities
        '纽约': 'America/New_York',
        '洛杉矶': 'America/Los_Angeles',
        '东京': 'Asia/Tokyo',
        '首尔': 'Asia/Seoul',
        '新加坡': 'Asia/Singapore',
        // All Chinese domestic cities use Asia/Shanghai
        '北京': 'Asia/Shanghai',
        '上海': 'Asia/Shanghai',
        '广州': 'Asia/Shanghai',
        '海口': 'Asia/Shanghai',
        '济南': 'Asia/Shanghai', 
        '南宁': 'Asia/Shanghai',
        '珠海': 'Asia/Shanghai'
      };
      return timezoneMap[city] || 'Asia/Shanghai';
    },
    
    isInternationalFlight(departureCity, arrivalCity) {
      const internationalCities = ['纽约', '洛杉矶', '东京', '首尔', '新加坡'];
      return internationalCities.includes(departureCity) || internationalCities.includes(arrivalCity);
    },
    
    // Check if connecting flight has cross-airport transfers
    hasCrossAirportTransfer(flight) {
      if (!flight.isConnecting || !flight.flights || flight.flights.length < 2) {
        return false;
      }
      
      for (let i = 0; i < flight.flights.length - 1; i++) {
        const currentFlight = flight.flights[i];
        const nextFlight = flight.flights[i + 1];
        
        // Validate time sequence first
        if (currentFlight.arrivalTimeUtc && nextFlight.departureTimeUtc) {
          const arrivalTime = dayjs.utc(currentFlight.arrivalTimeUtc);
          const departureTime = dayjs.utc(nextFlight.departureTimeUtc);
          
          // For connecting flights, we should trust the backend validation
          // The backend already ensures proper connection times with timezone handling
          // We'll only log extreme cases that might indicate data issues
          if (arrivalTime.isAfter(departureTime)) {
            const timeDiff = Math.abs(departureTime.diff(arrivalTime, 'hours', true));
            
            // Only flag as error if the time difference is extreme (>48 hours)
            if (timeDiff > 48) {
              console.warn('Possible data issue - very long layover:', {
                currentFlight: currentFlight.flightNumber,
                arrivalTime: arrivalTime.format(),
                nextFlight: nextFlight.flightNumber,
                departureTime: departureTime.format(),
                layoverHours: timeDiff
              });
            } else {
              console.log('Overnight connection detected:', {
                currentFlight: currentFlight.flightNumber,
                arrivalTime: arrivalTime.format(),
                nextFlight: nextFlight.flightNumber,
                departureTime: departureTime.format(),
                layoverHours: timeDiff
              });
            }
          }
        }
        
        // Check if arrival airport of current leg is different from departure airport of next leg
        // but they are in the same city
        if (currentFlight.arrivalAirport && nextFlight.departureAirport) {
          const arrivalAirportId = currentFlight.arrivalAirport.id;
          const departureAirportId = nextFlight.departureAirport.id;
          const arrivalCity = currentFlight.arrivalAirport.city;
          const departureCity = nextFlight.departureAirport.city;
          
          if (arrivalAirportId !== departureAirportId && arrivalCity === departureCity) {
            return true;
          }
        }
      }
      return false;
    },
    
    // Get cross-airport transfer details
    getCrossAirportTransfers(flight) {
      const transfers = [];
      if (!flight.isConnecting || !flight.flights || flight.flights.length < 2) {
        return transfers;
      }
      
      for (let i = 0; i < flight.flights.length - 1; i++) {
        const currentFlight = flight.flights[i];
        const nextFlight = flight.flights[i + 1];
        
        // Validate time sequence first
        if (currentFlight.arrivalTimeUtc && nextFlight.departureTimeUtc) {
          const arrivalTime = dayjs.utc(currentFlight.arrivalTimeUtc);
          const departureTime = dayjs.utc(nextFlight.departureTimeUtc);
          
          // Trust backend validation for connecting flights
          // Only skip extreme cases that indicate possible data corruption
          if (arrivalTime.isAfter(departureTime)) {
            const timeDiff = Math.abs(departureTime.diff(arrivalTime, 'hours', true));
            if (timeDiff > 48) {
              console.warn('Skipping transfer with extreme layover time:', {
                leg: i + 1,
                arrivalTime: arrivalTime.format(),
                departureTime: departureTime.format(),
                layoverHours: timeDiff
              });
              continue;
            }
          }
        }
        
        if (currentFlight.arrivalAirport && nextFlight.departureAirport) {
          const arrivalAirportId = currentFlight.arrivalAirport.id;
          const departureAirportId = nextFlight.departureAirport.id;
          const arrivalCity = currentFlight.arrivalAirport.city;
          const departureCity = nextFlight.departureAirport.city;
          
          if (arrivalAirportId !== departureAirportId && arrivalCity === departureCity) {
            // Calculate transfer time to show if adequate
            const arrivalTime = dayjs.utc(currentFlight.arrivalTimeUtc);
            const departureTime = dayjs.utc(nextFlight.departureTimeUtc);
            let transferMinutes = departureTime.diff(arrivalTime, 'minutes');
            
            // Debug: Check what's going on with the timestamps
            console.log('Transfer time calculation debug:', {
              flight1: currentFlight.flightNumber,
              arrivalUTC: currentFlight.arrivalTimeUtc,
              arrivalParsed: arrivalTime.format('YYYY-MM-DD HH:mm:ss UTC'),
              flight2: nextFlight.flightNumber,
              departureUTC: nextFlight.departureTimeUtc,
              departureParsed: departureTime.format('YYYY-MM-DD HH:mm:ss UTC'),
              rawDiffMinutes: transferMinutes,
              isNextDay: arrivalTime.isAfter(departureTime)
            });

            if (transferMinutes < 0) {
              console.warn('Negative transfer time detected - possible overnight connection');
              transferMinutes = Math.abs(transferMinutes);
            }
            
            transfers.push({
              from: currentFlight.arrivalAirport.name,
              to: nextFlight.departureAirport.name,
              city: arrivalCity,
              transferTime: Math.max(0, transferMinutes)
            });
          }
        }
      }
      return transfers;
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

    formatTransferTime(minutes) {
      if (!minutes || minutes < 0) return '时间不足';
      
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      
      if (hours === 0) {
        return `${mins}分钟`;
      } else if (mins === 0) {
        return `${hours}小时`;
      } else {
        return `${hours}小时${mins}分钟`;
      }
    },

    validateDestination(rule, value, callback) {
      try {
        if (value && value === this.searchForm.origin) {
          callback(new Error('目的地不能与出发地相同'))
        } else {
          callback()
        }
      } catch (error) {
        console.error('Error in validateDestination:', error)
        callback()
      }
    },


    disabledDate(date) {
      try {
        return date < new Date(new Date().setHours(0, 0, 0, 0))
      } catch (error) {
        console.error('Error in disabledDate:', error)
        return false
      }
    },


    handleOriginChange() {
      try {
        if (this.searchForm.destination === this.searchForm.origin) {
          this.searchForm.destination = ''
        }
        
        this.$nextTick(() => {
          if (this.$refs.searchFormRef) {
            this.$refs.searchFormRef.validateField('destination')
          }
        })
      } catch (error) {
        console.error('Error in handleOriginChange:', error)
      }
    },

    handleDestinationChange() {
      try {
        this.$nextTick(() => {
          if (this.$refs.searchFormRef) {
            this.$refs.searchFormRef.validateField('destination')
          }
        })
      } catch (error) {
        console.error('Error in handleDestinationChange:', error)
      }
    },


    swapLocations() {
      const temp = this.searchForm.origin
      this.searchForm.origin = this.searchForm.destination
      this.searchForm.destination = temp
    },

    async handleSearch() {
      try {
        await this.$refs.searchFormRef.validate()
        this.searching = true
        this.hasSearched = true
        this.sortOption = ''

        if (this.searchForm.includeConnecting) {
          await this.searchConnectingFlights()
        } else {
          await this.searchDirectFlights()
        }
      } catch (error) {
        console.error('Search error:', error)
        ElMessage.error('搜索失败，请重试')
        this.searchResults = []
      } finally {
        this.searching = false
      }
    },

    async searchDirectFlights() {
      const allCities = [...this.groupedAirports.domestic, ...this.groupedAirports.international]
      const originCityGroup = allCities.find(group => group.city === this.searchForm.origin)
      const destinationCityGroup = allCities.find(group => group.city === this.searchForm.destination)
      
      if (!originCityGroup || !destinationCityGroup) {
        ElMessage.error('请选择有效的出发地和目的地')
        return
      }

      // Use the primary airport (first airport) from each city for the search
      const originAirportId = originCityGroup.primaryAirport.id
      const destinationAirportId = destinationCityGroup.primaryAirport.id

      const response = await api.get('/flights/search', {
        params: {
          originAirportId: originAirportId,
          destinationAirportId: destinationAirportId,
          departureDate: this.searchForm.departureDate,
          passengers: 1
        }
      })

      if (response.data.success) {
        // Convert direct flights to consistent format
        this.searchResults = (response.data.data || []).map(flight => ({
          ...flight,
          isConnecting: false,
          type: 'DIRECT',
          // Ensure availableSeats is available for direct flights
          availableSeats: flight.availableSeats
        }))
        
        if (this.searchResults.length === 0) {
          ElMessage.info('未找到符合条件的直飞航班')
        }
      } else {
        ElMessage.error(response.data.message || '搜索失败')
        this.searchResults = []
      }
    },

    async searchConnectingFlights() {
      const searchRequest = {
        departureCity: this.searchForm.origin,
        arrivalCity: this.searchForm.destination,
        departureDate: this.searchForm.departureDate,
        passengers: 1,
        includeConnectingFlights: true
      }

      const response = await api.post('/flights/search/connecting', searchRequest)

      if (response.data.success) {
        console.log('Raw connecting flights from backend:', response.data.data?.length || 0);
        const uniqueFlights = new Map();
        
        (response.data.data || []).forEach(connectingFlight => {
          const isDirectFlight = connectingFlight.flights && connectingFlight.flights.length === 1
          const uniqueId = isDirectFlight 
            ? connectingFlight.flights[0].id.toString()
            : connectingFlight.flights.map(f => f.id).join('-');
          
          const flightData = {
            id: uniqueId,
            flightNumber: isDirectFlight ? connectingFlight.flights[0].flightNumber : connectingFlight.flights.map(f => f.flightNumber).join(' + '),
            price: connectingFlight.totalPrice,
            departureTimeUtc: connectingFlight.departureTimeUtc,
            arrivalTimeUtc: connectingFlight.arrivalTimeUtc,
            departureAirport: connectingFlight.flights[0].departureAirport,
            arrivalAirport: connectingFlight.flights[connectingFlight.flights.length - 1].arrivalAirport,
            airline: connectingFlight.flights[0].airline,
            isConnecting: !isDirectFlight,
            type: isDirectFlight ? 'DIRECT' : (connectingFlight.type || 'CONNECTING'),
            flights: connectingFlight.flights,
            totalDurationMinutes: connectingFlight.totalDurationMinutes,
            availableSeats: connectingFlight.availableSeats
          };
          if (!uniqueFlights.has(uniqueId)) {
            uniqueFlights.set(uniqueId, flightData);
          } else {
            console.log('Duplicate connecting flight filtered out:', uniqueId, flightData.flightNumber);
          }
        });
        
        this.searchResults = Array.from(uniqueFlights.values());
        
        console.log(`Deduplicated connecting flights: ${response.data.data?.length || 0} -> ${this.searchResults.length}`);
        
        if (this.searchResults.length === 0) {
          ElMessage.info('未找到符合条件的航班')
        }
      } else {
        ElMessage.error(response.data.message || '搜索失败')
        this.searchResults = []
      }
    },

    selectFlight(flight) {
      if (flight.isConnecting) {
        this.$router.push({
          path: '/booking',
          query: {
            flightId: flight.flights[0].id,
            connectingFlightIds: flight.flights.map(f => f.id).join(','),
            passengers: 1,
            isConnecting: 'true'
          }
        })
      } else {
        this.$router.push({
          path: '/booking',
          query: {
            flightId: flight.id,
            passengers: 1
          }
        })
      }
    },

    async loadAirports() {
      try {
        const response = await api.get('/airports')
        if (response.data && response.data.success && Array.isArray(response.data.data)) {
          this.airports = response.data.data
        } else {
          console.warn('API response format unexpected, keeping default airports')
        }
      } catch (error) {
        console.error('Failed to load airports:', error)
      }
    },

    resetSearch() {
      this.searchForm = {
        origin: '',
        destination: '',
        departureDate: '',
        includeConnecting: false
      }
      this.searchResults = []
      this.hasSearched = false
      this.sortOption = ''
      this.$refs.searchFormRef?.clearValidate()
    },

    getSeatAvailabilityClass(availableSeats) {
      if (availableSeats === 0) {
        return 'no-seats'
      } else if (availableSeats <= 5) {
        return 'low-seats'
      } else if (availableSeats <= 20) {
        return 'medium-seats'
      } else {
        return 'high-seats'
      }
    },

    formatTime(datetime) {
      return dayjs(datetime).format('HH:mm')
    },

    calculateDuration(departure, arrival) {
      const start = dayjs(departure)
      const end = dayjs(arrival)
      const duration = end.diff(start, 'minute')
      const hours = Math.floor(duration / 60)
      const minutes = duration % 60
      return `${hours}小时${minutes}分钟`
    },

    getAirportCode(airport) {
      return airport?.code || '未知'
    },

    getAirlineShortName(name) {
      return name ? name.substring(0, 2) : '航'
    },

    isNextDayArrival(departureTime, arrivalTime) {
      if (!departureTime || !arrivalTime) return false
      
      try {
        const departure = dayjs(departureTime)
        const arrival = dayjs(arrivalTime)

        return !departure.isSame(arrival, 'day')
      } catch (error) {
        console.error('Error checking next day arrival:', error)
        return false
      }
    },

    handleSortChange() {
      if (this.sortOption) {
        this.sortFlightResults()
      }
    },

    sortFlightResults() {
      if (!this.sortOption || this.searchResults.length === 0) return

      const sortedResults = [...this.searchResults]

      switch (this.sortOption) {
        case 'departureTimeAsc':
          sortedResults.sort((a, b) => {
            const timeA = new Date(a.departureTimeUtc).getTime()
            const timeB = new Date(b.departureTimeUtc).getTime()
            return timeA - timeB
          })
          break

        case 'departureTimeDesc':
          sortedResults.sort((a, b) => {
            const timeA = new Date(a.departureTimeUtc).getTime()
            const timeB = new Date(b.departureTimeUtc).getTime()
            return timeB - timeA
          })
          break

        case 'priceAsc':
          sortedResults.sort((a, b) => {
            const priceA = parseFloat(a.price) || 0
            const priceB = parseFloat(b.price) || 0
            return priceA - priceB
          })
          break

        case 'priceDesc':
          sortedResults.sort((a, b) => {
            const priceA = parseFloat(a.price) || 0
            const priceB = parseFloat(b.price) || 0
            return priceB - priceA
          })
          break

        default:
          return
      }

      this.searchResults = sortedResults
    }
  },

  created() {
    console.log('FlightSearch component created')
    try {
      // Initialize validation rules after component is created
      console.log('Setting up validation rules')
      this.searchRules = {
        origin: [
          { required: true, message: '请选择出发城市', trigger: 'change' }
        ],
        destination: [
          { required: true, message: '请选择目的地城市', trigger: 'change' },
          { validator: this.validateDestination, trigger: 'change' }
        ],
        departureDate: [
          { required: true, message: '请选择出发日期', trigger: 'change' }
        ]
      }
      console.log('Validation rules set successfully')
    } catch (error) {
      console.error('Error in created hook:', error)
    }
  },

  mounted() {
    console.log('FlightSearch component mounted')
    
    // Add global error handler for this component
    this.$nextTick(() => {
      window.addEventListener('error', (event) => {
        console.error('Global error caught:', event.error)
        console.error('Error message:', event.message)
        console.error('Error filename:', event.filename)
        console.error('Error line:', event.lineno)
        console.error('Error stack:', event.error?.stack)
      })
      
      window.addEventListener('unhandledrejection', (event) => {
        console.error('Unhandled promise rejection:', event.reason)
        console.error('Promise:', event.promise)
      })
    })
    
    try {
      this.loadAirports()
      
      // Handle query parameters from dashboard quick actions
      const { from, to } = this.$route.query
      if (from && to) {
        // Since we now work with city names directly, just set the city names
        this.$nextTick(() => {
          this.searchForm.origin = from
          this.searchForm.destination = to
        })
      }
    } catch (error) {
      console.error('Error in mounted hook:', error)
    }
  }
}
</script>

<style scoped>
/* Page Layout */
.flight-search-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* Header */
.page-header {
  padding: 2rem 0 1.5rem;
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
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

/* Search Form */
.search-form-section {
  padding: 0 0 2rem;
  margin-top: -1rem;
}

.search-form-card {
  background: #ffffff;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  padding: 2rem;
}

.flight-options-section {
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: #f8fafc;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
}

.option-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.option-description {
  font-size: 0.875rem;
  color: #64748b;
  font-style: italic;
}

/* Search Fields */
.search-fields-grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.location-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 1rem;
  align-items: end;
}

.location-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #666666;
}

.location-select {
  width: 100%;
}

.swap-button-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40px;
}

.swap-btn {
  background: #3b82f6;
  color: white;
  border: none;
  transition: all 0.2s ease;
}

.swap-btn:hover:not(:disabled) {
  background: #2563eb;
  transform: rotate(180deg);
}

/* City selection with domestic/international separation */
:deep(.el-select-group__title) {
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 2px solid #e2e8f0;
  position: relative;
  line-height: 1.4;
  min-height: 20px;
}

:deep(.el-select-group__title)::after {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #cbd5e1 50%, transparent 100%);
}

:deep(.el-select-group__wrap:not(:last-of-type)) {
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 4px;
}

:deep(.el-select-group:first-child .el-select-group__title) {
  border-top: none;
}

.city-option {
  padding: 12px 16px !important;
  border-bottom: 1px solid #f8fafc;
  transition: all 0.2s ease;
  min-height: 60px;
}

.city-option:hover {
  background: #f8fafc;
}

.city-option:last-child {
  border-bottom: none;
}

.city-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.city-name {
  font-size: 15px;
  font-weight: 600;
  color: #333333;
  line-height: 1.3;
  margin-bottom: 2px;
}

.airport-count {
  font-size: 12px;
  color: #666666;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 12px;
  align-self: flex-start;
  font-weight: 500;
}

.airport-codes {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
  font-family: 'Monaco', 'Menlo', monospace;
  letter-spacing: 0.3px;
  background: #eff6ff;
  padding: 3px 6px;
  border-radius: 4px;
  align-self: flex-start;
  line-height: 1.2;
}

/* Fix dropdown display issues */
:deep(.el-select-dropdown) {
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-select-dropdown__item) {
  padding: 0 !important;
  height: auto !important;
  min-height: 60px;
  display: flex;
  align-items: center;
  line-height: normal;
  overflow: visible;
}

:deep(.el-select-dropdown__item.hover) {
  background-color: #f0f9ff;
}

:deep(.el-select-dropdown__item.selected) {
  background-color: #eff6ff;
  color: #3b82f6;
  font-weight: 500;
}

/* Fix text cutting issues */
:deep(.el-select-dropdown__item .city-info) {
  padding: 8px 0;
  width: 100%;
}

:deep(.el-select-dropdown__item .city-name) {
  white-space: normal;
  word-wrap: break-word;
  overflow: visible;
}

/* Improve filterable input styling */
:deep(.el-select .el-input .el-input__inner) {
  font-size: 14px;
  line-height: 1.5;
  height: 40px;
}

/* Better placeholder styling */
:deep(.el-select .el-input .el-input__inner::placeholder) {
  color: #9ca3af;
  font-weight: 400;
}

/* Ensure proper group spacing */
:deep(.el-select-group__wrap) {
  margin-bottom: 0;
}

:deep(.el-select-group) {
  margin-bottom: 8px;
}

:deep(.el-select-group:last-child) {
  margin-bottom: 0;
}

/* Date Fields */
.date-row {
  display: flex;
  justify-content: flex-start;
  gap: 1rem;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.date-picker {
  width: 100%;
}

/* Passenger and Class */
.passenger-row {
  display: flex;
  justify-content: flex-start;
}

.passenger-field,
.class-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.passenger-input,
.class-select {
  width: 100%;
}

/* Search Button */
.search-button-section {
  margin-top: var(--space-8);
  text-align: center;
}

.search-btn {
  padding: var(--space-4) var(--space-8);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  gap: var(--space-2);
}

/* Search Results */
.search-results-section {
  padding: var(--space-8) 0;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.results-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333333;
  margin: 0;
  flex-shrink: 0;
}

.results-controls {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex: 1;
  justify-content: flex-end;
}

.results-info {
  color: #666666;
  font-size: 1rem;
  white-space: nowrap;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sort-label {
  font-size: 0.875rem;
  color: #666666;
  font-weight: 500;
  white-space: nowrap;
}

/* Loading State */
.loading-state {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--color-border-primary);
}

/* Flight Results */
.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.flight-result-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  cursor: pointer;
  transition: var(--transition-fast);
}

.flight-result-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-hover);
}

.flight-info {
  display: grid;
  grid-template-columns: 200px 1fr 120px;
  gap: var(--space-6);
  align-items: center;
}

/* Airline Section */
.airline-section {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.airline-logo {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border-primary);
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

.airline-details {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.flight-number {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.airline-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Route Section */
.route-section {
  display: flex;
  align-items: center;
  gap: var(--space-6);
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
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: 2px;
  font-weight: var(--font-weight-medium);
}

.time-container {
  position: relative;
  display: inline-block;
}

.time {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.next-day-indicator {
  position: absolute;
  top: -8px;
  right: -30px;
  font-size: 10px;
  font-weight: 500;
  color: #f56c6c;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 10px;
  padding: 2px 6px;
  line-height: 1;
  white-space: nowrap;
  box-shadow: 0 1px 2px rgba(245, 108, 108, 0.1);
}

.airport {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.airport-name {
  font-size: 12px;
  color: #666666;
  text-align: center;
  line-height: 1.3;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Flight Path */
.flight-path {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.duration {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.path-line {
  flex: 1;
  height: 2px;
  background: var(--color-border-secondary);
  border-radius: 1px;
  min-width: 40px;
}

.plane-icon {
  color: var(--color-primary);
  font-size: var(--icon-lg);
  transform: rotate(90deg);
}

.flight-type {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 12px;
  white-space: nowrap;
}

.flight-type:not(.connecting) {
  color: #059669;
  background: #d1fae5;
}

/* Connecting flight styling */
.flight-result-card:has(.transfer-icon) .flight-type,
.flight-type.connecting {
  color: #d97706;
  background: #fef3c7;
}

/* Cross-airport transfer notice styling */
.transfer-notice {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: 12px;
  line-height: 1.4;
}

.transfer-warning {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #dc2626;
  font-weight: 600;
  margin-bottom: 4px;
}

.warning-icon {
  font-size: 14px;
  color: #dc2626;
}

.transfer-details {
  color: #7f1d1d;
  font-weight: 500;
}

.transfer-item {
  margin: 2px 0;
}

.transfer-time {
  font-size: 11px;
  color: #059669;
  font-weight: 600;
  margin-left: 4px;
}

.path-visual {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.transfer-icon {
  color: #d97706;
  font-size: 14px;
  transform: rotate(90deg);
}

/* Price Section */
.price-section {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: flex-end;
}

.price {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.availability-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  margin: 4px 0;
}

.seats-label {
  color: #666666;
  font-weight: 500;
}

.seats-count {
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 11px;
  min-width: 20px;
  text-align: center;
}

.seats-count.no-seats {
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.seats-count.low-seats {
  color: #d97706;
  background: #fef3c7;
  border: 1px solid #fde68a;
}

.seats-count.medium-seats {
  color: #059669;
  background: #d1fae5;
  border: 1px solid #a7f3d0;
}

.seats-count.high-seats {
  color: #0f766e;
  background: #ccfbf1;
  border: 1px solid #99f6e4;
}

.tax-info {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

/* Empty Results */
.empty-results {
  text-align: center;
  padding: var(--space-12) var(--space-6);
}

/* Responsive Design */
@media (max-width: 1023px) {
  .location-row {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .swap-button-container {
    order: -1;
    height: auto;
    margin-bottom: 0.5rem;
  }
  
  .date-row,
  .passenger-class-row {
    grid-template-columns: 1fr;
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

  .results-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .results-controls {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .sort-controls {
    width: 100%;
    justify-content: flex-end;
  }
  
  .next-day-indicator {
    right: -25px;
    font-size: 9px;
    padding: 1px 5px;
  }
}

@media (max-width: 767px) {
  .search-form-card {
    padding: 1rem;
  }
  
  .page-title {
    font-size: 1.5rem;
  }
  
  .flight-result-card {
    padding: 1rem;
  }
  
  .airline-section {
    flex-direction: column;
    text-align: center;
    gap: 0.5rem;
  }
  
  .airline-logo {
    width: 40px;
    height: 40px;
  }
  
  .route-section {
    flex-direction: column;
    gap: 1rem;
  }
  
  .availability-info {
    justify-content: center;
    margin: 8px 0;
  }
  
  .flight-path {
    flex-direction: row;
    width: 100%;
  }

  .results-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .sort-controls {
    justify-content: flex-start;
  }

  .sort-controls .el-select {
    width: 100% !important;
  }
  
  .next-day-indicator {
    right: -20px;
    font-size: 8px;
    padding: 1px 4px;
    top: -6px;
  }
  
  .time-container {
    margin-right: 5px;
  }
}

/* Form validation styles */
:deep(.el-form-item.is-error .el-input__inner),
:deep(.el-form-item.is-error .el-select .el-input__inner) {
  border-color: var(--color-error);
}

:deep(.el-form-item__error) {
  font-size: var(--font-size-xs);
  color: var(--color-error);
  padding-top: var(--space-1);
}

/* Container */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}
</style>
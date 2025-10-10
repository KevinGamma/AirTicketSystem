<template>
  <div class="flight-management">
    <div class="header">
      <h2>{{ $t('admin.flightManagement') }}</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        {{ $t('admin.addFlight') }}
      </el-button>
    </div>

    <el-table :data="flights" stripe v-loading="loading">
      <el-table-column prop="flightNumber" :label="$t('admin.flightNumber')" width="150"></el-table-column>
      <el-table-column :label="$t('admin.airline')" width="200">
        <template #default="scope">
          <div v-if="scope.row.airline" style="display: flex; align-items: center;">
            <el-avatar v-if="scope.row.airline.logoUrl" :src="scope.row.airline.logoUrl" :size="24" shape="square" style="margin-right: 8px" />
            <span>{{ scope.row.airline.code }} - {{ scope.row.airline.name }}</span>
          </div>
          <span v-else style="color: #909399;">{{ $t('admin.unknownAirline') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.route')" width="250">
        <template #default="scope">
          <span v-if="scope.row.departureAirport && scope.row.arrivalAirport">
            {{ getCityName(scope.row.departureAirport.city) }} → {{ getCityName(scope.row.arrivalAirport.city) }}
          </span>
          <span v-else-if="scope.row.departureCity && scope.row.arrivalCity">
            {{ getCityName(scope.row.departureCity) }} → {{ getCityName(scope.row.arrivalCity) }}
          </span>
          <span v-else style="color: #909399;">{{ $t('admin.unknownRoute') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.departure')" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.departureTimeUtc || scope.row.departureTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="totalSeats" :label="$t('admin.totalSeats')" width="120"></el-table-column>
      <el-table-column prop="availableSeats" :label="$t('admin.available')" width="120"></el-table-column>
      <el-table-column :label="$t('admin.price')" width="100">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.aircraftType')" width="200">
        <template #default="scope">
          {{ getAircraftDisplayName(scope.row.aircraftType) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="$t('admin.status')" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.actions')" width="200">
        <template #default="scope">
          <el-button size="small" @click="editFlight(scope.row)">{{ $t('admin.edit') }}</el-button>
          <el-button
            size="small"
            type="warning"
            @click="cancelFlight(scope.row)"
            v-if="scope.row.status === 'SCHEDULED'"
          >
            {{ $t('admin.cancel') }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteFlight(scope.row)"
          >
            {{ $t('admin.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="flightForm" :rules="rules" ref="flightFormRef" label-width="140px">
        <el-form-item :label="$t('admin.airline')" prop="airlineId">
          <el-select v-model="flightForm.airlineId" :placeholder="$t('admin.selectAirline')" @change="onAirlineChange">
            <el-option
              v-for="airline in airlines"
              :key="airline.id"
              :label="`${airline.code} - ${airline.name}`"
              :value="airline.id"
            >
              <div style="display: flex; align-items: center;">
                <el-avatar v-if="airline.logoUrl" :src="airline.logoUrl" :size="20" shape="square" style="margin-right: 8px" />
                <span>{{ airline.code }} - {{ airline.name }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.flightNumber')" prop="flightNumber">
          <el-input 
            v-model="flightForm.flightNumber" 
            @blur="validateFlightNumber"
            :placeholder="getFlightNumberPlaceholder()"
            @input="handleFlightNumberInput"
          ></el-input>
          <div v-if="flightNumberError" class="form-error">{{ flightNumberError }}</div>
          <div class="form-tip">{{ $t('admin.flightNumberTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('admin.departureAirport')" prop="departureAirportId">
          <el-select v-model="flightForm.departureAirportId" :placeholder="$t('admin.selectAirport')">
            <el-option
              v-for="airport in airports"
              :key="airport.id"
              :label="`${airport.code} - ${airport.name}`"
              :value="airport.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.arrivalAirport')" prop="arrivalAirportId">
          <el-select v-model="flightForm.arrivalAirportId" :placeholder="$t('admin.selectAirport')">
            <el-option
              v-for="airport in airports"
              :key="airport.id"
              :label="`${airport.code} - ${airport.name}`"
              :value="airport.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.departureTime')" prop="departureTime">
          <el-date-picker
            v-model="flightForm.departureTime"
            type="datetime"
            :placeholder="$t('admin.selectDateTime')"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            :disabled-time="disabledTime"
          ></el-date-picker>
        </el-form-item>
        <el-form-item :label="$t('admin.arrivalTime')" prop="arrivalTime">
          <el-date-picker
            v-model="flightForm.arrivalTime"
            type="datetime"
            :placeholder="$t('admin.selectDateTime')"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            :disabled-time="disabledTime"
          ></el-date-picker>
        </el-form-item>
        <el-form-item :label="$t('admin.aircraftType')" prop="aircraftType">
          <el-select v-model="flightForm.aircraftType" :placeholder="$t('admin.selectAircraftType')" @change="onAircraftModelChange">
            <el-option
              v-for="model in aircraftModels"
              :key="model.code"
              :label="`${model.chineseName} (${model.code})`"
              :value="model.code"
            >
              <div>
                <span style="font-weight: bold;">{{ model.chineseName }}</span>
                <span style="color: #8492a6; margin-left: 8px;">{{ model.code }}</span>
              </div>
              <div style="font-size: 12px; color: #8492a6;">
                {{ $t('admin.seatsCount', { count: model.totalSeats }) }} | {{ model.description }}
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.totalSeats')" prop="totalSeats">
          <el-input-number 
            v-model="flightForm.totalSeats" 
            :min="1" 
            :max="600"
            :disabled="!!flightForm.aircraftType"
          ></el-input-number>
          <div v-if="flightForm.aircraftType" style="font-size: 12px; color: #8492a6; margin-top: 4px;">
            {{ $t('admin.autoSetSeats') }}
          </div>
        </el-form-item>
        <el-form-item :label="$t('admin.price')" prop="price">
          <el-input-number v-model="flightForm.price" :min="0" :precision="2"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('admin.cancel') }}</el-button>
        <el-button type="primary" @click="saveFlight" :loading="saving">
          {{ $t('admin.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import {Plus} from "@element-plus/icons-vue";


dayjs.extend(utc)

export default {
  name: 'FlightManagement',
  components: {Plus},
  data() {
    return {
      flights: [],
      airports: [],
      aircraftModels: [],
      airlines: [],
      loading: false,
      saving: false,
      dialogVisible: false,
      isEdit: false,
      refreshTimer: null,
      flightNumberError: null,
      flightForm: {
        flightNumber: '',
        airlineId: null,
        departureAirportId: null,
        arrivalAirportId: null,
        departureTime: '',
        arrivalTime: '',
        totalSeats: 180,
        price: 0,
        aircraftType: ''
      },
      rules: {
        airlineId: [{ required: true, message: this.$t('admin.airlineRequired'), trigger: 'change' }],
        flightNumber: [{ required: true, message: this.$t('admin.flightNumberRequired'), trigger: 'blur' }],
        departureAirportId: [{ required: true, message: this.$t('admin.departureAirportRequired'), trigger: 'change' }],
        arrivalAirportId: [{ required: true, message: this.$t('admin.arrivalAirportRequired'), trigger: 'change' }],
        departureTime: [
          { required: true, message: this.$t('admin.departureTimeRequired'), trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback()
                return
              }
              const now = new Date()
              const departureTime = new Date(value)
              if (departureTime <= now) {
                callback(new Error(this.$t('admin.pastDepartureTimeError')))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ],
        arrivalTime: [
          { required: true, message: this.$t('admin.arrivalTimeRequired'), trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback()
                return
              }
              const now = new Date()
              const arrivalTime = new Date(value)
              if (arrivalTime <= now) {
                callback(new Error(this.$t('admin.pastArrivalTimeError')))
                return
              }
              
              if (this.flightForm.departureTime && new Date(this.flightForm.departureTime) >= arrivalTime) {
                callback(new Error(this.$t('admin.invalidTimeError')))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ],
        totalSeats: [{ required: true, message: this.$t('admin.totalSeatsRequired'), trigger: 'blur' }],
        price: [{ required: true, message: this.$t('admin.priceRequired'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? this.$t('admin.editFlight') : this.$t('admin.addFlight')
    }
  },
  methods: {
    disabledDate(time) {
      
      return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
    },

    disabledTime(time) {
      const now = new Date()
      const selectedDate = new Date(time)
      
      
      if (selectedDate.toDateString() === now.toDateString()) {
        return {
          disabledHours: () => {
            const hours = []
            for (let i = 0; i < now.getHours(); i++) {
              hours.push(i)
            }
            return hours
          },
          disabledMinutes: (hour) => {
            if (hour === now.getHours()) {
              const minutes = []
              for (let i = 0; i <= now.getMinutes(); i++) {
                minutes.push(i)
              }
              return minutes
            }
            return []
          },
          disabledSeconds: (hour, minute) => {
            if (hour === now.getHours() && minute === now.getMinutes()) {
              const seconds = []
              for (let i = 0; i <= now.getSeconds(); i++) {
                seconds.push(i)
              }
              return seconds
            }
            return []
          }
        }
      }
      return {}
    },

    async loadFlights() {
      try {
        this.loading = true
        const response = await api.get('/flights')
        if (response.data.success) {
          this.flights = response.data.data
        }
      } catch (error) {
        console.log('API failed, using sample data for testing')
        
        this.flights = [
          {
            id: 1,
            flightNumber: 'CA1234',
            airline: { id: 1, code: 'CA', name: '中国国际航空', logoUrl: null },
            departureAirport: { id: 1, code: 'PEK', name: '北京首都国际机场', city: '北京' },
            arrivalAirport: { id: 2, code: 'PVG', name: '上海浦东国际机场', city: '上海' },
            departureTimeUtc: '2025-08-25T10:30:00', 
            arrivalTimeUtc: null,
            departureTime: null,
            arrivalTime: null,
            totalSeats: 180,
            availableSeats: 45,
            price: 680.00,
            aircraftType: 'B737',
            status: 'SCHEDULED'
          },
          {
            id: 2,
            flightNumber: 'MU5678',
            airline: { id: 2, code: 'MU', name: '中国东方航空', logoUrl: null },
            departureAirport: { id: 1, code: 'PEK', name: '北京首都国际机场', city: '北京' },
            arrivalAirport: { id: 2, code: 'PVG', name: '上海浦东国际机场', city: '上海' },
            departureTimeUtc: null,
            arrivalTimeUtc: null,
            departureTime: '2025-08-25 14:20:00', 
            arrivalTime: '2025-08-25 16:45:00',
            totalSeats: 160,
            availableSeats: 32,
            price: 720.00,
            aircraftType: 'A320',
            status: 'SCHEDULED'
          },
          {
            id: 3,
            flightNumber: 'CZ3456',
            airline: { id: 3, code: 'CZ', name: '中国南方航空', logoUrl: null },
            departureAirport: { id: 1, code: 'PEK', name: '北京首都国际机场', city: '北京' },
            arrivalAirport: { id: 3, code: 'CAN', name: '广州白云国际机场', city: '广州' },
            departureTimeUtc: '2025-08-26T07:45:00.123Z', 
            arrivalTimeUtc: '2025-08-26T11:30:00.456Z',
            departureTime: null,
            arrivalTime: null,
            totalSeats: 200,
            availableSeats: 28,
            price: 890.00,
            aircraftType: 'A330',
            status: 'SCHEDULED'
          },
          {
            id: 4,
            flightNumber: 'HU7788',
            airline: { id: 4, code: 'HU', name: '海南航空', logoUrl: null },
            departureAirport: { id: 4, code: 'HAK', name: '海口美兰国际机场', city: '海口' },
            arrivalAirport: { id: 2, code: 'PVG', name: '上海浦东国际机场', city: '上海' },
            departureTimeUtc: null,
            arrivalTimeUtc: null,
            departureTime: 1724571600000, 
            arrivalTime: 1724581200000,
            totalSeats: 180,
            availableSeats: 55,
            price: 980.00,
            aircraftType: 'B738',
            status: 'SCHEDULED'
          },
          {
            id: 5,
            flightNumber: '3U8889',
            airline: { id: 5, code: '3U', name: '四川航空', logoUrl: null },
            departureAirport: { id: 5, code: 'CTU', name: '成都双流国际机场', city: '成都' },
            arrivalAirport: { id: 1, code: 'PEK', name: '北京首都国际机场', city: '北京' },
            departureTimeUtc: null,
            arrivalTimeUtc: null,
            departureTime: null, 
            arrivalTime: null,
            totalSeats: 150,
            availableSeats: 38,
            price: 750.00,
            aircraftType: 'A320',
            status: 'DELAYED'
          }
        ]
        
        console.log('Loaded sample flight data:', this.flights)
      } finally {
        this.loading = false
      }
    },

    async refreshFlights() {
      try {
        const response = await api.get('/flights')
        if (response.data.success) {
          this.flights = response.data.data
        }
      } catch (error) {
        
        console.warn('Failed to refresh flights:', error)
      }
    },

    startAutoRefresh() {
      
      this.refreshTimer = setInterval(() => {
        this.refreshFlights()
      }, 30000)
    },

    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    
    async loadAirports() {
      try {
        const response = await api.get('/airports')
        if (response.data.success) {
          this.airports = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadAirportsFailed'))
      }
    },
    
    async loadAircraftModels() {
      try {
        const response = await api.get('/aircraft-models')
        if (response.data.success) {
          this.aircraftModels = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadAircraftModelsFailed'))
      }
    },
    
    async loadAirlines() {
      try {
        const response = await api.get('/airlines')
        if (response.data.success) {
          this.airlines = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadAirlinesFailed'))
      }
    },
    
    showCreateDialog() {
      this.isEdit = false
      this.flightForm = {
        flightNumber: '',
        airlineId: null,
        departureAirportId: null,
        arrivalAirportId: null,
        departureTime: '',
        arrivalTime: '',
        totalSeats: 180,
        price: 0,
        aircraftType: ''
      }
      this.flightNumberError = null
      this.dialogVisible = true
    },
    
    editFlight(flight) {
      this.isEdit = true
      this.flightForm = { 
        ...flight,
        departureTime: this.convertUtcToLocal(flight.departureTimeUtc || flight.departureTime),
        arrivalTime: this.convertUtcToLocal(flight.arrivalTimeUtc || flight.arrivalTime)
      }
      this.dialogVisible = true
    },
    
    async saveFlight() {
      try {
        await this.$refs.flightFormRef.validate()
        this.saving = true
        
        
        const convertedDepartureTime = this.convertLocalToUtc(this.flightForm.departureTime)
        const convertedArrivalTime = this.convertLocalToUtc(this.flightForm.arrivalTime)
        
        
        if (!convertedDepartureTime || !convertedArrivalTime) {
          ElMessage.error('出发时间和到达时间不能为空')
          this.saving = false
          return
        }
        
        
        const flightData = {
          flightNumber: this.flightForm.flightNumber,
          airlineId: this.flightForm.airlineId,
          departureAirportId: this.flightForm.departureAirportId,
          arrivalAirportId: this.flightForm.arrivalAirportId,
          departureTime: convertedDepartureTime,
          arrivalTime: convertedArrivalTime,
          totalSeats: this.flightForm.totalSeats,
          price: this.flightForm.price,
          aircraftType: this.flightForm.aircraftType
        }
        
        
        if (this.isEdit && this.flightForm.id) {
          flightData.id = this.flightForm.id
        }
        
        
        
        if (!this.isEdit) {
          flightData.availableSeats = this.flightForm.totalSeats
        } else {
          
          const originalFlight = this.flights.find(f => f.id === this.flightForm.id)
          if (originalFlight && originalFlight.totalSeats !== this.flightForm.totalSeats) {
            
            const bookedSeats = originalFlight.totalSeats - originalFlight.availableSeats
            flightData.availableSeats = Math.max(0, this.flightForm.totalSeats - bookedSeats)
          }
          
        }
        
        if (this.isEdit) {
          await api.put(`/flights/${this.flightForm.id}`, flightData)
          ElMessage.success(this.$t('admin.flightUpdatedSuccess'))
        } else {
          await api.post('/flights', flightData)
          ElMessage.success(this.$t('admin.flightCreatedSuccess'))
        }
        
        this.dialogVisible = false
        this.loadFlights()
        
        
        console.log('[FlightManagement] Dispatching notifyFlightDataUpdate after flight save/create')
        this.$store.dispatch('notifyFlightDataUpdate')
        
        
        const timestamp = Date.now()
        localStorage.setItem('flightDataUpdated', timestamp.toString())
        console.log('[FlightManagement] localStorage flight update event triggered:', timestamp)
      } catch (error) {
        console.error('Save flight error:', error)
        let errorMessage = this.$t('admin.saveFlightFailed')
        
        if (error.response && error.response.data && error.response.data.message) {
          const serverMessage = error.response.data.message
          
          
          if (serverMessage.includes('Duplicate entry') && serverMessage.includes('flight_number')) {
            
            const flightNumberMatch = serverMessage.match(/'([^']+)' for key 'flights\.flight_number'/)
            const flightNumber = flightNumberMatch ? flightNumberMatch[1] : this.flightForm.flightNumber
            errorMessage = this.$t('admin.duplicateFlightNumber', { flightNumber })
            

            this.flightNumberError = this.$t('admin.duplicateFlightNumberField')
          } else if (serverMessage.includes('flight.duplicate.flightNumber')) {

            const flightNumber = this.flightForm.flightNumber
            errorMessage = this.$t('admin.duplicateFlightNumber', { flightNumber })
            this.flightNumberError = this.$t('admin.duplicateFlightNumberField')
          } else if (serverMessage.includes('flight.invalidFlightNumber')) {

            errorMessage = this.$t('admin.invalidFlightNumberFormat')
            this.flightNumberError = this.$t('admin.invalidFlightNumberFormat')
          } else {

            errorMessage = errorMessage + ': ' + serverMessage
          }
        }
        
        ElMessage.error(errorMessage)
      } finally {
        this.saving = false
      }
    },
    
    async cancelFlight(flight) {
      try {
        await ElMessageBox.confirm(this.$t('admin.confirmCancelFlight'), this.$t('admin.cancelFlight'), {
          confirmButtonText: this.$t('admin.yes'),
          cancelButtonText: this.$t('admin.no'),
          type: 'warning'
        })
        
        await api.post(`/flights/${flight.id}/cancel`, this.$t('admin.adminCancelledFlight'))
        ElMessage.success(this.$t('admin.flightCancelledSuccess'))
        this.loadFlights()
        

        console.log('[FlightManagement] Dispatching notifyFlightDataUpdate after flight cancel')
        this.$store.dispatch('notifyFlightDataUpdate')
        

        const timestamp = Date.now()
        localStorage.setItem('flightDataUpdated', timestamp.toString())
        console.log('[FlightManagement] localStorage flight update event triggered:', timestamp)
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.cancelFlightFailed'))
        }
      }
    },
    
    async deleteFlight(flight) {
      try {
        await ElMessageBox.confirm(this.$t('admin.confirmDeleteFlight'), this.$t('admin.deleteFlight'), {
          confirmButtonText: this.$t('admin.delete'),
          cancelButtonText: this.$t('admin.cancel'),
          type: 'danger'
        })
        
        await api.delete(`/flights/${flight.id}`)
        ElMessage.success(this.$t('admin.flightDeletedSuccess'))
        this.loadFlights()
        

        console.log('[FlightManagement] Dispatching notifyFlightDataUpdate after flight delete')
        this.$store.dispatch('notifyFlightDataUpdate')
        

        const timestamp = Date.now()
        localStorage.setItem('flightDataUpdated', timestamp.toString())
        console.log('[FlightManagement] localStorage flight update event triggered:', timestamp)
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.deleteFlightFailed'))
        }
      }
    },
    
    getStatusType(status) {
      const statusTypes = {
        'SCHEDULED': 'success',
        'DELAYED': 'warning',
        'CANCELLED': 'danger',
        'DEPARTED': 'info',
        'LANDED': 'primary',
        'COMPLETED': 'info',
        '已起飞': 'info',
        '已降落': 'primary'
      }
      return statusTypes[status] || ''
    },

    getStatusText(status) {
      const statusTexts = {
        'SCHEDULED': this.$t('admin.statuses.scheduled'),
        'DELAYED': this.$t('admin.statuses.delayed'),
        'CANCELLED': this.$t('admin.statuses.cancelled'),
        'DEPARTED': this.$t('admin.statuses.departed'),
        'LANDED': this.$t('admin.statuses.landed'),
        'COMPLETED': this.$t('admin.statuses.completed'),
        '已起飞': '已起飞',
        '已降落': '已降落'
      }
      return statusTexts[status] || status
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) {
        return '--:--'
      }
      
      try {

        let parsed = dayjs(dateTime)
        if (parsed.isValid()) {
          return parsed.format('YYYY-MM-DD HH:mm')
        }
        

        if (typeof dateTime === 'number' || /^\d+$/.test(dateTime)) {
          parsed = dayjs(parseInt(dateTime))
          if (parsed.isValid()) {
            return parsed.format('YYYY-MM-DD HH:mm')
          }
        }
        

        const formats = [
          'YYYY-MM-DD HH:mm:ss',
          'YYYY-MM-DDTHH:mm:ss',
          'YYYY-MM-DDTHH:mm:ss.SSS',
          'YYYY-MM-DDTHH:mm:ss.SSSZ',
          'YYYY-MM-DDTHH:mm:ssZ',
          'YYYY/MM/DD HH:mm:ss',
          'DD/MM/YYYY HH:mm:ss',
          'MM/DD/YYYY HH:mm:ss',
          'YYYY-MM-DD HH:mm',
          'DD-MM-YYYY HH:mm',
          'MM-DD-YYYY HH:mm'
        ]
        
        for (const format of formats) {
          parsed = dayjs(dateTime, format)
          if (parsed.isValid()) {
            return parsed.format('YYYY-MM-DD HH:mm')
          }
        }
        
        console.warn('Unable to parse date format:', dateTime, typeof dateTime)
        return '--:--'
      } catch (error) {
        console.error('Date formatting error:', error, dateTime)
        return '--:--'
      }
    },

    convertUtcToLocal(utcTime) {
      if (!utcTime) return ''
      try {

        const utcMoment = dayjs.utc(utcTime)
        if (utcMoment.isValid()) {

          return utcMoment.local().format('YYYY-MM-DD HH:mm:ss')
        }
        return ''
      } catch (error) {
        console.error('UTC to local conversion error:', error, utcTime)
        return ''
      }
    },

    convertLocalToUtc(localTime) {
      if (!localTime || localTime === '') return null
      try {

        const localMoment = dayjs(localTime)
        if (localMoment.isValid()) {

          return localMoment.utc().toISOString()
        }
        return null
      } catch (error) {
        console.error('Local to UTC conversion error:', error, localTime)
        return null
      }
    },
    
    onAircraftModelChange(modelCode) {
      const selectedModel = this.aircraftModels.find(model => model.code === modelCode)
      if (selectedModel) {
        this.flightForm.totalSeats = selectedModel.totalSeats

        if (!this.isEdit) {
          this.flightForm.availableSeats = selectedModel.totalSeats
        }
      }
    },
    
    getAircraftDisplayName(aircraftType) {
      const model = this.aircraftModels.find(m => m.code === aircraftType)
      return model ? `${model.chineseName} (${model.code})` : aircraftType
    },
    
    getCityName(cityName) {

      if (!cityName) return ''
      

      return this.$t(`cities.${cityName}`) !== `cities.${cityName}` 
        ? this.$t(`cities.${cityName}`) 
        : cityName
    },
    
    onAirlineChange() {

      this.flightNumberError = null
      

      const selectedAirline = this.airlines.find(a => a.id === this.flightForm.airlineId)
      if (selectedAirline) {

        const existingFlightNumber = this.flightForm.flightNumber || ''
        let numericPart = ''
        
        
        const match = existingFlightNumber.match(/\d+$/)
        if (match) {
          numericPart = match[0]
        }
        
        
        this.flightForm.flightNumber = selectedAirline.code + numericPart
      }
      
      this.validateFlightNumber()
    },
    
    handleFlightNumberInput(value) {
      
      const selectedAirline = this.airlines.find(a => a.id === this.flightForm.airlineId)
      if (selectedAirline && value) {
        const airlineCode = selectedAirline.code
        
        
        if (!value.startsWith(airlineCode)) {
          
          const numericMatch = value.match(/\d+/)
          const numericPart = numericMatch ? numericMatch[0] : ''
          this.flightForm.flightNumber = airlineCode + numericPart
        } else {
          
          const allowedPattern = new RegExp(`^${airlineCode}\\d*$`)
          if (!allowedPattern.test(value)) {
            
            const numericMatch = value.slice(airlineCode.length).match(/\d+/)
            const numericPart = numericMatch ? numericMatch[0] : ''
            this.flightForm.flightNumber = airlineCode + numericPart
          }
        }
      }
    },
    
    getFlightNumberPlaceholder() {
      const selectedAirline = this.airlines.find(a => a.id === this.flightForm.airlineId)
      if (selectedAirline) {
        return `${selectedAirline.code}1234 (${this.$t('admin.flightNumberExample')})`
      }
      return this.$t('admin.flightNumberPlaceholder')
    },

    async validateFlightNumber() {
      if (!this.flightForm.flightNumber || !this.flightForm.airlineId) {
        this.flightNumberError = null
        return
      }
      
      try {
        const airline = this.airlines.find(a => a.id === this.flightForm.airlineId)
        if (!airline) return
        
        
        if (!this.flightForm.flightNumber.startsWith(airline.code)) {
          this.flightNumberError = this.$t('admin.flightNumberMustStartWith', { code: airline.code })
          return
        }
        
        
        const numericPart = this.flightForm.flightNumber.slice(airline.code.length)
        if (!numericPart || !/^\d+$/.test(numericPart)) {
          this.flightNumberError = this.$t('admin.flightNumberMustHaveNumbers')
          return
        }
        
        const response = await api.get('/airlines/validate-flight-number', {
          params: {
            flightNumber: this.flightForm.flightNumber,
            airlineCode: airline.code
          }
        })
        
        if (response.data.success && !response.data.data) {
          this.flightNumberError = this.$t('admin.invalidFlightNumberFormat', { code: airline.code })
        } else {
          this.flightNumberError = null
        }
      } catch (error) {
        console.warn('Failed to validate flight number:', error)
      }
    }
  },
  
  mounted() {
    this.loadFlights()
    this.loadAirports()
    this.loadAircraftModels()
    this.loadAirlines()
    this.startAutoRefresh()
  },

  beforeUnmount() {
    this.stopAutoRefresh()
  }
}
</script>

<style scoped>
.flight-management {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.form-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
<template>
  <div class="airport-management">
    <div class="header">
      <h2>{{ $t('admin.airportManagement') }}</h2>
      <div class="header-actions">
        <el-button type="success" @click="initializeAirports" :loading="initializing">
          <el-icon><Download /></el-icon>
          {{ $t('admin.initializeAirports') }}
        </el-button>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ $t('admin.addAirport') }}
        </el-button>
      </div>
    </div>

    <div class="filters">
      <el-input
        v-model="searchCode"
        :placeholder="$t('admin.searchByCode')"
        style="width: 200px; margin-right: 10px;"
        @keyup.enter="loadAirports"
        clearable
      />
      <el-select
        v-model="filterCountry"
        :placeholder="$t('admin.filterByCountry')"
        style="width: 200px; margin-right: 10px;"
        @change="loadAirports"
        clearable
      >
        <el-option
          v-for="country in countries"
          :key="country"
          :label="country"
          :value="country"
        ></el-option>
      </el-select>
      <el-button type="primary" @click="loadAirports">搜索</el-button>
    </div>

    <el-table :data="filteredAirports" stripe v-loading="loading">
      <el-table-column prop="code" :label="$t('admin.airportCode')" width="120">
        <template #default="scope">
          <el-tag type="primary">{{ scope.row.code }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="$t('admin.airportName')" min-width="200"></el-table-column>
      <el-table-column prop="city" :label="$t('admin.city')" width="150"></el-table-column>
      <el-table-column prop="country" :label="$t('admin.country')" width="150"></el-table-column>
      <el-table-column prop="timeZone" :label="$t('admin.timeZone')" width="180"></el-table-column>
      <el-table-column :label="$t('admin.createdAt')" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.actions')" width="200">
        <template #default="scope">
          <el-button size="small" @click="editAirport(scope.row)">{{ $t('admin.edit') }}</el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteAirport(scope.row)"
          >
            {{ $t('admin.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Airport Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="airportForm" :rules="rules" ref="airportFormRef" label-width="120px">
        <el-form-item :label="$t('admin.airportCode')" prop="code">
          <el-input 
            v-model="airportForm.code" 
            :maxlength="3"
            :placeholder="$t('admin.airportCodePlaceholder')"
            @input="handleCodeInput"
          ></el-input>
          <div class="form-tip">{{ $t('admin.airportCodeTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('admin.airportName')" prop="name">
          <el-input v-model="airportForm.name" :placeholder="$t('admin.airportNamePlaceholder')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.city')" prop="city">
          <el-input v-model="airportForm.city" :placeholder="$t('admin.cityPlaceholder')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.country')" prop="country">
          <el-input v-model="airportForm.country" :placeholder="$t('admin.countryPlaceholder')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.timeZone')" prop="timeZone">
          <el-select v-model="airportForm.timeZone" :placeholder="$t('admin.selectTimeZone')" filterable>
            <el-option
              v-for="tz in timeZones"
              :key="tz"
              :label="tz"
              :value="tz"
            ></el-option>
          </el-select>
          <div class="form-tip">{{ $t('admin.timeZoneTip') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('admin.cancel') }}</el-button>
        <el-button type="primary" @click="saveAirport" :loading="saving">
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
import { Plus, Download } from "@element-plus/icons-vue";

export default {
  name: 'AirportManagement',
  components: { Plus, Download },
  data() {
    return {
      airports: [],
      countries: [],
      loading: false,
      saving: false,
      initializing: false,
      dialogVisible: false,
      isEdit: false,
      searchCode: '',
      filterCountry: '',
      airportForm: {
        code: '',
        name: '',
        city: '',
        country: '',
        timeZone: ''
      },
      timeZones: [
        'Asia/Shanghai',
        'Asia/Beijing',
        'Asia/Urumqi',
        'Asia/Tokyo',
        'Asia/Seoul',
        'Asia/Singapore',
        'Asia/Bangkok',
        'Asia/Kuala_Lumpur',
        'Asia/Manila',
        'Asia/Jakarta',
        'Asia/Dubai',
        'Europe/London',
        'Europe/Paris',
        'Europe/Berlin',
        'Europe/Amsterdam',
        'America/New_York',
        'America/Los_Angeles',
        'America/Vancouver',
        'America/Toronto',
        'Australia/Sydney',
        'Australia/Melbourne'
      ],
      rules: {
        code: [
          { required: true, message: this.$t('admin.airportCodeRequired'), trigger: 'blur' },
          { min: 3, max: 3, message: this.$t('admin.airportCodeLength'), trigger: 'blur' },
          { pattern: /^[A-Z]{3}$/, message: this.$t('admin.airportCodeFormat'), trigger: 'blur' }
        ],
        name: [
          { required: true, message: this.$t('admin.airportNameRequired'), trigger: 'blur' }
        ],
        city: [
          { required: true, message: this.$t('admin.cityRequired'), trigger: 'blur' }
        ],
        country: [
          { required: true, message: this.$t('admin.countryRequired'), trigger: 'blur' }
        ],
        timeZone: [
          { required: true, message: this.$t('admin.timeZoneRequired'), trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? this.$t('admin.editAirport') : this.$t('admin.addAirport')
    },
    filteredAirports() {
      let filtered = this.airports
      
      if (this.searchCode) {
        filtered = filtered.filter(airport => 
          airport.code.toLowerCase().includes(this.searchCode.toLowerCase())
        )
      }
      
      if (this.filterCountry) {
        filtered = filtered.filter(airport => airport.country === this.filterCountry)
      }
      
      return filtered
    }
  },
  methods: {
    async loadAirports() {
      try {
        this.loading = true
        const response = await api.get('/airports')
        if (response.data.success) {
          this.airports = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadAirportsFailed'))
        console.error('Load airports error:', error)
      } finally {
        this.loading = false
      }
    },
    
    async loadCountries() {
      try {
        const response = await api.get('/airports/countries')
        if (response.data.success) {
          this.countries = response.data.data
        }
      } catch (error) {
        console.error('Load countries error:', error)
      }
    },
    
    showCreateDialog() {
      this.isEdit = false
      this.airportForm = {
        code: '',
        name: '',
        city: '',
        country: '',
        timeZone: 'Asia/Shanghai'
      }
      this.dialogVisible = true
    },
    
    editAirport(airport) {
      this.isEdit = true
      this.airportForm = { ...airport }
      this.dialogVisible = true
    },
    
    async saveAirport() {
      try {
        await this.$refs.airportFormRef.validate()
        this.saving = true
        
        if (this.isEdit) {
          await api.put(`/airports/${this.airportForm.id}`, this.airportForm)
          ElMessage.success(this.$t('admin.airportUpdatedSuccess'))
        } else {
          await api.post('/airports', this.airportForm)
          ElMessage.success(this.$t('admin.airportCreatedSuccess'))
        }
        
        this.dialogVisible = false
        this.loadAirports()
        this.loadCountries()
      } catch (error) {
        console.error('Save airport error:', error)
        if (error.response && error.response.data && error.response.data.message) {
          ElMessage.error(this.$t('admin.saveAirportFailed') + ': ' + error.response.data.message)
        } else {
          ElMessage.error(this.$t('admin.saveAirportFailed'))
        }
      } finally {
        this.saving = false
      }
    },
    
    async deleteAirport(airport) {
      try {
        await ElMessageBox.confirm(
          this.$t('admin.confirmDeleteAirport', { code: airport.code }), 
          this.$t('admin.deleteAirport'), 
          {
            confirmButtonText: this.$t('admin.delete'),
            cancelButtonText: this.$t('admin.cancel'),
            type: 'danger'
          }
        )
        
        await api.delete(`/airports/${airport.id}`)
        ElMessage.success(this.$t('admin.airportDeletedSuccess'))
        this.loadAirports()
        this.loadCountries()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.deleteAirportFailed'))
        }
      }
    },
    
    async initializeAirports() {
      try {
        await ElMessageBox.confirm(
          this.$t('admin.confirmInitializeAirports'), 
          this.$t('admin.initializeAirports'), 
          {
            confirmButtonText: this.$t('admin.yes'),
            cancelButtonText: this.$t('admin.no'),
            type: 'warning'
          }
        )
        
        this.initializing = true
        await api.post('/airports/initialize')
        ElMessage.success(this.$t('admin.airportsInitializedSuccess'))
        this.loadAirports()
        this.loadCountries()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Initialize airports error:', error)
          if (error.response && error.response.data && error.response.data.message) {
            ElMessage.error(this.$t('admin.initializeAirportsFailed') + ': ' + error.response.data.message)
          } else {
            ElMessage.error(this.$t('admin.initializeAirportsFailed'))
          }
        }
      } finally {
        this.initializing = false
      }
    },
    
    handleCodeInput(value) {
      // Auto-convert to uppercase
      this.airportForm.code = value.toUpperCase()
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) {
        return '--'
      }
      
      try {
        return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
      } catch (error) {
        console.error('Date formatting error:', error, dateTime)
        return '--'
      }
    }
  },
  
  mounted() {
    this.loadAirports()
    this.loadCountries()
  }
}
</script>

<style scoped>
.airport-management {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.filters {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
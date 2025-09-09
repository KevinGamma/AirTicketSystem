<template>
  <div class="ticket-management">
    <h2>{{ $t('admin.ticketManagement') }}</h2>

    <el-table :data="tickets" stripe v-loading="loading">
      <el-table-column prop="ticketNumber" :label="$t('admin.ticketNumber')" width="180"></el-table-column>
      <el-table-column :label="$t('admin.flight')" width="120">
        <template #default="scope">
          {{ scope.row.flight?.flightNumber }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.user')" width="150">
        <template #default="scope">
          {{ scope.row.user?.fullName }}
        </template>
      </el-table-column>
      <el-table-column prop="passengerName" :label="$t('admin.passenger')" width="150"></el-table-column>
      <el-table-column prop="ticketType" :label="$t('admin.type')" width="100">
        <template #default="scope">
          {{ getTicketTypeText(scope.row.ticketType) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.price')" width="100">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="$t('admin.status')" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.bookingDate')" width="150">
        <template #default="scope">
          {{ formatDate(scope.row.bookingTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.actions')" width="150">
        <template #default="scope">
          <el-button
            v-if="['BOOKED', 'PAID'].includes(scope.row.status)"
            type="warning"
            size="small"
            @click="cancelTicket(scope.row)"
          >
            {{ $t('admin.cancel') }}
          </el-button>
          <el-button
            v-if="scope.row.status === 'PAID'"
            type="danger"
            size="small"
            @click="refundTicket(scope.row)"
          >
            {{ $t('admin.refund') }}
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="deleteTicket(scope.row)"
          >
            {{ $t('admin.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import api from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

export default {
  name: 'TicketManagement',
  data() {
    return {
      tickets: [],
      loading: false
    }
  },
  methods: {
    async loadTickets() {
      try {
        this.loading = true
        const response = await api.get('/tickets')
        if (response.data.success) {
          this.tickets = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadTicketsFailed'))
      } finally {
        this.loading = false
      }
    },
    
    async cancelTicket(ticket) {
      try {
        await ElMessageBox.confirm(this.$t('admin.confirmCancelTicket'), this.$t('admin.cancelTicket'), {
          confirmButtonText: this.$t('admin.yes'),
          cancelButtonText: this.$t('admin.no'),
          type: 'warning'
        })
        
        const response = await api.post(`/tickets/${ticket.id}/cancel`, { reason: this.$t('admin.adminCancellation') })
        if (response.data.success) {
          ElMessage.success(this.$t('admin.ticketCancelledSuccess'))
          this.loadTickets()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.cancelFailed'))
        }
      }
    },
    
    async refundTicket(ticket) {
      try {
        await ElMessageBox.confirm(this.$t('admin.confirmRefundTicket'), this.$t('admin.ticketRefund'), {
          confirmButtonText: this.$t('admin.yes'),
          cancelButtonText: this.$t('admin.no'),
          type: 'warning'
        })
        
        const response = await api.post(`/tickets/${ticket.id}/refund`, { reason: this.$t('admin.adminRefund') })
        if (response.data.success) {
          ElMessage.success(this.$t('admin.refundProcessedSuccess'))
          this.loadTickets()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.refundFailed'))
        }
      }
    },
    
    async deleteTicket(ticket) {
      try {
        await ElMessageBox.confirm(
          this.$t('admin.confirmDeleteTicket', { ticketNumber: ticket.ticketNumber }), 
          this.$t('admin.deleteTicket'), 
          {
            confirmButtonText: this.$t('admin.confirmDelete'),
            cancelButtonText: this.$t('admin.cancel'),
            type: 'error',
            dangerouslyUseHTMLString: true
          }
        )
        
        const response = await api.delete(`/tickets/${ticket.id}`)
        if (response.data.success) {
          ElMessage.success(this.$t('admin.ticketDeletedSuccess'))
          this.loadTickets()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.deleteFailed'))
        }
      }
    },
    
    getStatusType(status) {
      const statusTypes = {
        'BOOKED': '',
        'PAID': 'success',
        'CHECKED_IN': 'info',
        'CANCELLED': 'danger',
        'REFUNDED': 'warning'
      }
      return statusTypes[status] || ''
    },

    getStatusText(status) {
      const statusTexts = {
        'BOOKED': this.$t('admin.statuses.booked'),
        'PAID': this.$t('admin.statuses.paid'),
        'CHECKED_IN': this.$t('admin.statuses.checkedIn'),
        'CANCELLED': this.$t('admin.statuses.cancelled'),
        'REFUNDED': this.$t('admin.statuses.refunded')
      }
      return statusTexts[status] || status
    },

    getTicketTypeText(type) {
      const typeTexts = {
        'ECONOMY': this.$t('admin.ticketTypes.economy'),
        'BUSINESS': this.$t('admin.ticketTypes.business'),
        'FIRST': this.$t('admin.ticketTypes.first')
      }
      return typeTexts[type] || type
    },
    
    formatDate(date) {
      return dayjs(date).format('YYYY-MM-DD HH:mm')
    }
  },
  
  mounted() {
    this.loadTickets()
  }
}
</script>

<style scoped>
.ticket-management {
  max-width: 1200px;
  margin: 0 auto;
}

.ticket-management h2 {
  margin-bottom: 20px;
  color: #303133;
}
</style>
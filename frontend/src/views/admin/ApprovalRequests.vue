<template>
  <div class="approval-requests">
    <h2>{{ $t('admin.approvalRequests') }}</h2>
    
    <!-- Filter Tabs -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="$t('admin.pendingRequests')" name="pending"></el-tab-pane>
      <el-tab-pane :label="$t('admin.allRequests')" name="all"></el-tab-pane>
    </el-tabs>
    
    <!-- Requests Table -->
    <el-table :data="requests" stripe v-loading="loading">
      <el-table-column :label="$t('tickets.requestType')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.requestType === 'REFUND' ? 'danger' : 'primary'">
            {{ scope.row.requestType === 'REFUND' ? $t('tickets.refund') : $t('tickets.reschedule') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('tickets.ticketNumber')" width="150">
        <template #default="scope">
          {{ scope.row.ticket?.ticketNumber }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.customer')" width="150">
        <template #default="scope">
          {{ scope.row.user?.username }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('tickets.flightNumber')" width="120">
        <template #default="scope">
          {{ scope.row.ticket?.flight?.flightNumber }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('tickets.reason')" width="200">
        <template #default="scope">
          {{ scope.row.reason }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('tickets.serviceFee')" width="100">
        <template #default="scope">
          ¥{{ scope.row.serviceFee }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('tickets.requestTime')" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.requestTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.requestStatus')" width="100">
        <template #default="scope">
          <el-tag :type="getRequestStatusType(scope.row.status)">
            {{ getRequestStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.operation')" width="200">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'PENDING'"
            type="primary"
            size="small"
            @click="showRequestDetails(scope.row)"
          >
            {{ $t('admin.process') }}
          </el-button>
          <el-button
            v-else
            type="info"
            size="small"
            @click="showRequestDetails(scope.row)"
          >
            {{ $t('admin.viewDetails') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Request Details Dialog -->
    <el-dialog v-model="requestDetailsVisible" :title="$t('admin.requestDetails')" width="800px">
      <div v-if="selectedRequest">
        <!-- Request Information -->
        <el-card style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <span>{{ $t('admin.requestInfo') }}</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <p><strong>{{ $t('tickets.requestType') }}:</strong> 
                <el-tag :type="selectedRequest.requestType === 'REFUND' ? 'danger' : 'primary'">
                  {{ selectedRequest.requestType === 'REFUND' ? $t('tickets.refund') : $t('tickets.reschedule') }}
                </el-tag>
              </p>
              <p><strong>{{ $t('admin.customer') }}:</strong> {{ selectedRequest.user?.username }}</p>
              <p><strong>{{ $t('tickets.requestTime') }}:</strong> {{ formatDateTime(selectedRequest.requestTime) }}</p>
              <p><strong>{{ $t('tickets.reason') }}:</strong> {{ selectedRequest.reason }}</p>
            </el-col>
            <el-col :span="12">
              <p><strong>{{ $t('tickets.serviceFee') }}:</strong> ¥{{ selectedRequest.serviceFee }}</p>
              <p><strong>{{ $t('tickets.status') }}:</strong> 
                <el-tag :type="getRequestStatusType(selectedRequest.status)">
                  {{ getRequestStatusText(selectedRequest.status) }}
                </el-tag>
              </p>
              <p v-if="selectedRequest.processedTime"><strong>{{ $t('admin.processedTime') }}:</strong> {{ formatDateTime(selectedRequest.processedTime) }}</p>
              <p v-if="selectedRequest.approvedByAdmin"><strong>{{ $t('admin.processedBy') }}:</strong> {{ selectedRequest.approvedByAdmin?.username }}</p>
            </el-col>
          </el-row>
          <div v-if="selectedRequest.rejectionReason" style="margin-top: 15px;">
            <p><strong>{{ $t('admin.rejectionReason') }}:</strong> {{ selectedRequest.rejectionReason }}</p>
          </div>
        </el-card>

        <!-- Ticket Information -->
        <el-card style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <span>{{ $t('tickets.ticketInfo') }}</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <p><strong>{{ $t('tickets.ticketNumber') }}:</strong> {{ selectedRequest.ticket?.ticketNumber }}</p>
              <p><strong>{{ $t('tickets.passengerName') }}:</strong> {{ selectedRequest.ticket?.passengerName }}</p>
              <p><strong>{{ $t('tickets.price') }}:</strong> ¥{{ selectedRequest.ticket?.price }}</p>
              <p><strong>{{ $t('tickets.ticketType') }}:</strong> {{ getTicketTypeText(selectedRequest.ticket?.ticketType) }}</p>
            </el-col>
            <el-col :span="12">
              <p><strong>{{ $t('tickets.flightNumber') }}:</strong> {{ selectedRequest.ticket?.flight?.flightNumber }}</p>
              <p><strong>{{ $t('tickets.route') }}:</strong> {{ getRoute(selectedRequest.ticket?.flight) }}</p>
              <p><strong>{{ $t('tickets.departureTime') }}:</strong> {{ formatDateTime(selectedRequest.ticket?.flight?.departureTimeUtc) }}</p>
              <p><strong>{{ $t('tickets.ticketStatus') }}:</strong> {{ getTicketStatusText(selectedRequest.ticket?.status) }}</p>
            </el-col>
          </el-row>
        </el-card>

        <!-- New Flight Information (for reschedule requests) -->
        <el-card v-if="selectedRequest.requestType === 'RESCHEDULE' && selectedRequest.newFlight" style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <span>{{ $t('admin.newFlightInfo') }}</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <p><strong>{{ $t('tickets.flightNumber') }}:</strong> {{ selectedRequest.newFlight?.flightNumber }}</p>
              <p><strong>{{ $t('tickets.route') }}:</strong> {{ getRoute(selectedRequest.newFlight) }}</p>
            </el-col>
            <el-col :span="12">
              <p><strong>{{ $t('tickets.departureTime') }}:</strong> {{ formatDateTime(selectedRequest.newFlight?.departureTimeUtc) }}</p>
              <p><strong>{{ $t('tickets.price') }}:</strong> ¥{{ selectedRequest.newFlight?.price }}</p>
            </el-col>
          </el-row>
        </el-card>

        <!-- Processing Actions -->
        <div v-if="selectedRequest.status === 'PENDING'" class="action-buttons">
          <h4>{{ $t('admin.processRequest') }}</h4>
          <el-button type="success" @click="approveRequest(selectedRequest.id)">
            {{ $t('admin.approve') }}
          </el-button>
          <el-button type="danger" @click="showRejectDialog(selectedRequest.id)">
            {{ $t('admin.reject') }}
          </el-button>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="requestDetailsVisible = false">{{ $t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- Reject Request Dialog -->
    <el-dialog v-model="rejectDialogVisible" :title="$t('admin.rejectRequest')" width="500px">
      <div>
        <p>{{ $t('admin.rejectConfirmMessage') }}</p>
        <el-form :model="rejectForm" label-width="120px">
          <el-form-item :label="$t('admin.rejectionReason')" required>
            <el-input
              v-model="rejectForm.reason"
              type="textarea"
              :rows="4"
              :placeholder="$t('admin.rejectionReasonPlaceholder')"
              maxlength="200"
              show-word-limit>
            </el-input>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="rejectDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button 
          type="danger" 
          @click="confirmReject"
          :disabled="!rejectForm.reason.trim()"
          :loading="processing">
          {{ $t('admin.confirmReject') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

export default {
  name: 'ApprovalRequests',
  data() {
    return {
      requests: [],
      loading: false,
      activeTab: 'pending',
      // Request details dialog
      requestDetailsVisible: false,
      selectedRequest: null,
      // Reject dialog
      rejectDialogVisible: false,
      rejectForm: {
        reason: ''
      },
      currentRequestId: null,
      processing: false
    }
  },
  methods: {
    async loadRequests() {
      try {
        this.loading = true
        const endpoint = this.activeTab === 'pending' ? '/admin/approval-requests/pending' : '/admin/approval-requests'
        const response = await api.get(endpoint)
        if (response.data.success) {
          this.requests = response.data.data
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadRequestsFailed'))
        console.error('Load requests error:', error)
      } finally {
        this.loading = false
      }
    },

    handleTabChange(tab) {
      this.activeTab = tab
      this.loadRequests()
    },

    showRequestDetails(request) {
      this.selectedRequest = request
      this.requestDetailsVisible = true
    },

    async approveRequest(requestId) {
      try {
        await ElMessageBox.confirm(
          this.$t('admin.approveConfirmMessage'), 
          this.$t('admin.approveRequest'), 
          {
            confirmButtonText: this.$t('admin.approve'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'success'
          }
        )

        this.processing = true
        const response = await api.post(`/admin/approval-requests/${requestId}/process`, {
          action: 'APPROVE'
        })
        
        if (response.data.success) {
          ElMessage.success(this.$t('admin.approveSuccess'))
          this.requestDetailsVisible = false
          this.loadRequests()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.approveFailed'))
          console.error('Approve error:', error)
        }
      } finally {
        this.processing = false
      }
    },

    showRejectDialog(requestId) {
      this.currentRequestId = requestId
      this.rejectForm.reason = ''
      this.rejectDialogVisible = true
    },

    async confirmReject() {
      if (!this.rejectForm.reason.trim()) {
        ElMessage.warning(this.$t('admin.rejectionReasonRequired'))
        return
      }

      try {
        this.processing = true
        const response = await api.post(`/admin/approval-requests/${this.currentRequestId}/process`, {
          action: 'REJECT',
          rejectionReason: this.rejectForm.reason
        })
        
        if (response.data.success) {
          ElMessage.success(this.$t('admin.rejectSuccess'))
          this.rejectDialogVisible = false
          this.requestDetailsVisible = false
          this.loadRequests()
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.rejectFailed'))
        console.error('Reject error:', error)
      } finally {
        this.processing = false
      }
    },

    getRequestStatusType(status) {
      const statusTypes = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger'
      }
      return statusTypes[status] || ''
    },

    getRequestStatusText(status) {
      const statusKey = status?.toLowerCase()
      const statusMap = {
        'pending': 'pending',
        'approved': 'approved',
        'rejected': 'rejected'
      }
      return this.$t(`tickets.requestStatuses.${statusMap[statusKey] || 'pending'}`)
    },

    getTicketStatusText(status) {
      if (!status) return this.$t('tickets.statuses.booked')
      
      const statusKey = status.toLowerCase()
      const statusMap = {
        'booked': 'booked',
        'paid': 'paid',
        'checked_in': 'checkedIn',
        'cancelled': 'cancelled',
        'refunded': 'refunded'
      }
      
      const translationKey = statusMap[statusKey]
      if (translationKey) {
        return this.$t(`tickets.statuses.${translationKey}`)
      }
      
      // Fallback: if translation key not found, return the status as-is or a default
      console.warn(`No translation found for ticket status: ${status}`)
      return this.$t('tickets.statuses.booked')
    },

    getTicketTypeText(type) {
      const typeKey = type?.toLowerCase()
      const typeMap = {
        'economy': 'economy',
        'business': 'business',
        'first': 'first'
      }
      return this.$t(`tickets.types.${typeMap[typeKey] || 'economy'}`)
    },

    getRoute(flight) {
      if (!flight) return '-'
      const departure = flight.departureAirport?.name || 'Unknown'
      const arrival = flight.arrivalAirport?.name || 'Unknown'
      return `${departure} → ${arrival}`
    },

    formatDateTime(date) {
      if (!date) return 'N/A'
      return dayjs(date).format('YYYY-MM-DD HH:mm')
    }
  },
  
  mounted() {
    this.loadRequests()
  }
}
</script>

<style scoped>
.approval-requests {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.approval-requests h2 {
  margin-bottom: 20px;
  color: #303133;
}

.card-header {
  font-weight: bold;
}

.action-buttons {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #EBEEF5;
  margin-top: 20px;
}

.action-buttons h4 {
  margin-bottom: 15px;
  color: #606266;
}

.action-buttons .el-button {
  margin: 0 10px;
}
</style>
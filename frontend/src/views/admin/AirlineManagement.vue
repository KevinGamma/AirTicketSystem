<template>
  <div class="airline-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('admin.airlineManagement') }}</span>
          <el-button type="primary" @click="showCreateDialog">
            {{ $t('admin.createAirline') }}
          </el-button>
        </div>
      </template>

      <!-- Airlines Table -->
      <el-table :data="airlines" stripe v-loading="loading">
        <el-table-column prop="code" :label="$t('admin.airlineCode')" width="100" />
        <el-table-column :label="$t('admin.airlineLogo')" width="80">
          <template #default="scope">
            <el-avatar
              v-if="scope.row.logoUrl"
              :src="scope.row.logoUrl"
              :size="40"
              shape="square"
            />
            <el-icon v-else size="40" color="#dcdfe6">
              <Picture />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('admin.airlineName')" />
        <el-table-column prop="fullName" :label="$t('admin.airlineFullName')" />
        <el-table-column :label="$t('admin.status')" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.active ? 'success' : 'danger'">
              {{ scope.row.active ? $t('admin.active') : $t('admin.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('admin.actions')" width="300">
          <template #default="scope">
            <el-button size="small" @click="viewAirline(scope.row)">
              {{ $t('admin.view') }}
            </el-button>
            <el-button size="small" type="primary" @click="editAirline(scope.row)">
              {{ $t('admin.edit') }}
            </el-button>
            <el-button
              size="small"
              :type="scope.row.active ? 'warning' : 'success'"
              @click="toggleAirlineStatus(scope.row)"
            >
              {{ scope.row.active ? $t('admin.deactivate') : $t('admin.activate') }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteAirline(scope.row)"
            >
              {{ $t('admin.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('admin.createAirline') : $t('admin.editAirline')"
      width="600px"
    >
      <el-form
        :model="airlineForm"
        :rules="airlineRules"
        ref="airlineFormRef"
        label-width="120px"
      >
        <el-form-item :label="$t('admin.airlineCode')" prop="code">
          <el-input
            v-model="airlineForm.code"
            :placeholder="$t('admin.airlineCodePlaceholder')"
            maxlength="2"
            show-word-limit
            style="text-transform: uppercase"
            @input="airlineForm.code = airlineForm.code.toUpperCase()"
          />
          <div class="form-tip">{{ $t('admin.airlineCodeTip') }}</div>
        </el-form-item>
        
        <el-form-item :label="$t('admin.airlineName')" prop="name">
          <el-input v-model="airlineForm.name" :placeholder="$t('admin.airlineNamePlaceholder')" />
        </el-form-item>
        
        <el-form-item :label="$t('admin.airlineFullName')" prop="fullName">
          <el-input v-model="airlineForm.fullName" :placeholder="$t('admin.airlineFullNamePlaceholder')" />
        </el-form-item>
        
        <el-form-item :label="$t('admin.description')">
          <el-input
            v-model="airlineForm.description"
            type="textarea"
            :rows="3"
            :placeholder="$t('admin.descriptionPlaceholder')"
          />
        </el-form-item>
        
        <el-form-item :label="$t('admin.airlineLogo')">
          <div class="logo-upload-section">
            <el-upload
              class="logo-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleLogoSuccess"
              :before-upload="beforeLogoUpload"
              accept="image/*"
            >
              <div class="logo-preview">
                <el-image
                  v-if="airlineForm.logoUrl"
                  :src="airlineForm.logoUrl"
                  fit="contain"
                  style="width: 80px; height: 80px"
                />
                <div v-else class="upload-placeholder">
                  <el-icon size="40"><Plus /></el-icon>
                  <div>{{ $t('admin.uploadLogo') }}</div>
                </div>
              </div>
            </el-upload>
            <div class="upload-tip">{{ $t('admin.logoUploadTip') }}</div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveAirline" :loading="saving">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- View Dialog -->
    <el-dialog v-model="viewDialogVisible" :title="$t('admin.airlineDetails')" width="500px">
      <el-descriptions v-if="selectedAirline" :column="1" border>
        <el-descriptions-item :label="$t('admin.airlineCode')">
          {{ selectedAirline.code }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.airlineName')">
          {{ selectedAirline.name }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.airlineFullName')">
          {{ selectedAirline.fullName }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.description')">
          {{ selectedAirline.description || $t('admin.none') }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.airlineLogo')">
          <el-image
            v-if="selectedAirline.logoUrl"
            :src="selectedAirline.logoUrl"
            fit="contain"
            style="width: 100px; height: 100px"
          />
          <span v-else>{{ $t('admin.none') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.status')">
          <el-tag :type="selectedAirline.active ? 'success' : 'danger'">
            {{ selectedAirline.active ? $t('admin.active') : $t('admin.inactive') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('admin.createdDate')">
          {{ formatDateTime(selectedAirline.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import api from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

export default {
  name: 'AirlineManagement',
  components: {
    Picture,
    Plus
  },
  data() {
    return {
      airlines: [],
      loading: false,
      saving: false,
      dialogVisible: false,
      viewDialogVisible: false,
      dialogMode: 'create',
      selectedAirline: null,
      airlineForm: {
        id: null,
        code: '',
        name: '',
        fullName: '',
        description: '',
        logoUrl: '',
        active: true
      },
      airlineRules: {
        code: [
          { required: true, message: '', trigger: 'blur' },
          { min: 2, max: 2, message: '', trigger: 'blur' },
          { pattern: /^[A-Z0-9]{2}$/, message: '', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '', trigger: 'blur' }
        ],
        fullName: [
          { required: true, message: '', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    uploadAction() {
      return `/api/airlines/${this.airlineForm.id}/logo`
    },
    uploadHeaders() {
      const token = localStorage.getItem('token')
      return {
        'Authorization': `Bearer ${token}`
      }
    }
  },
  mounted() {
    this.loadAirlines()
    this.updateValidationRules()
  },
  methods: {
    async loadAirlines() {
      this.loading = true
      try {
        const response = await api.get('/airlines/admin')
        if (response.data.success) {
          this.airlines = response.data.data
        } else {
          ElMessage.error(this.$t('admin.loadAirlinesFailed'))
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.loadAirlinesFailed'))
      } finally {
        this.loading = false
      }
    },

    showCreateDialog() {
      this.dialogMode = 'create'
      this.resetForm()
      this.dialogVisible = true
    },

    editAirline(airline) {
      this.dialogMode = 'edit'
      this.airlineForm = { ...airline }
      this.dialogVisible = true
    },

    viewAirline(airline) {
      this.selectedAirline = airline
      this.viewDialogVisible = true
    },

    resetForm() {
      this.airlineForm = {
        id: null,
        code: '',
        name: '',
        fullName: '',
        description: '',
        logoUrl: '',
        active: true
      }
      if (this.$refs.airlineFormRef) {
        this.$refs.airlineFormRef.clearValidate()
      }
    },

    async saveAirline() {
      try {
        await this.$refs.airlineFormRef.validate()
        this.saving = true

        let response
        if (this.dialogMode === 'create') {
          response = await api.post('/airlines', this.airlineForm)
        } else {
          response = await api.put(`/airlines/${this.airlineForm.id}`, this.airlineForm)
        }

        if (response.data.success) {
          ElMessage.success(
            this.dialogMode === 'create'
              ? this.$t('admin.airlineCreatedSuccess')
              : this.$t('admin.airlineUpdatedSuccess')
          )
          this.dialogVisible = false
          this.loadAirlines()
        } else {
          ElMessage.error(response.data.message || this.$t('admin.operationFailed'))
        }
      } catch (error) {
        ElMessage.error(this.$t('admin.operationFailed'))
      } finally {
        this.saving = false
      }
    },

    async toggleAirlineStatus(airline) {
      try {
        const action = airline.active ? this.$t('admin.deactivate') : this.$t('admin.activate')
        await ElMessageBox.confirm(
          this.$t('admin.confirmToggleAirlineStatus', { action, name: airline.name }),
          this.$t('admin.toggleAirlineStatus'),
          {
            confirmButtonText: this.$t('common.confirm'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'warning'
          }
        )

        const response = await api.put(`/airlines/${airline.id}/status`, null, {
          params: { active: !airline.active }
        })

        if (response.data.success) {
          ElMessage.success(this.$t('admin.airlineStatusUpdated'))
          this.loadAirlines()
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.operationFailed'))
        }
      }
    },

    async deleteAirline(airline) {
      try {
        await ElMessageBox.confirm(
          this.$t('admin.confirmDeleteAirline', { name: airline.name }),
          this.$t('admin.deleteAirline'),
          {
            confirmButtonText: this.$t('admin.confirmDelete'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'warning'
          }
        )

        const response = await api.delete(`/airlines/${airline.id}`)
        
        if (response.data.success) {
          ElMessage.success(this.$t('admin.airlineDeletedSuccess'))
          this.loadAirlines()
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(this.$t('admin.deleteAirlineFailed'))
        }
      }
    },

    beforeLogoUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isImage) {
        ElMessage.error(this.$t('admin.logoMustBeImage'))
        return false
      }
      if (!isLt2M) {
        ElMessage.error(this.$t('admin.logoSizeTooLarge'))
        return false
      }
      return true
    },

    handleLogoSuccess(response) {
      if (response.success) {
        this.airlineForm.logoUrl = response.data
        ElMessage.success(this.$t('admin.logoUploadSuccess'))
      } else {
        ElMessage.error(response.message || this.$t('admin.logoUploadFailed'))
      }
    },

    formatDateTime(dateTime) {
      return dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss')
    },

    updateValidationRules() {
      this.airlineRules.code[0].message = this.$t('admin.airlineCodeRequired')
      this.airlineRules.code[1].message = this.$t('admin.airlineCodeLength')
      this.airlineRules.code[2].message = this.$t('admin.airlineCodeFormat')
      this.airlineRules.name[0].message = this.$t('admin.airlineNameRequired')
      this.airlineRules.fullName[0].message = this.$t('admin.airlineFullNameRequired')
    }
  },
  watch: {
    '$i18n.locale'() {
      this.updateValidationRules()
    }
  }
}
</script>

<style scoped>
.airline-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.logo-upload-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.logo-uploader {
  margin-bottom: 10px;
}

.logo-preview {
  width: 120px;
  height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.logo-preview:hover {
  border-color: #409eff;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 12px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}
</style>
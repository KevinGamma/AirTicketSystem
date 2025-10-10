<template>
  <div class="user-management">
    <div class="header-section">
      <h2>{{ $t('admin.userManagement') }}</h2>
      <el-button type="primary" @click="showCreateDialog">{{ $t('admin.createUser') }}</el-button>
    </div>
    
    <el-table :data="users" stripe v-loading="loading">
      <el-table-column prop="username" :label="$t('admin.username')" width="150"></el-table-column>
      <el-table-column prop="fullName" :label="$t('admin.fullName')" width="180"></el-table-column>
      <el-table-column prop="email" :label="$t('admin.email')" width="250"></el-table-column>
      <el-table-column prop="phone" :label="$t('admin.phone')" width="150"></el-table-column>
      <el-table-column prop="role" :label="$t('admin.role')" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'success'">
            {{ getRoleText(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.createdDate')" width="150">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.actions')" width="200">
        <template #default="scope">
          <el-button size="small" @click="viewUser(scope.row)">{{ $t('admin.view') }}</el-button>
          <el-button size="small" type="warning" @click="editUser(scope.row)">{{ $t('admin.edit') }}</el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteUser(scope.row)"
            v-if="scope.row.role !== 'ADMIN'"
          >
            {{ $t('admin.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="$t('admin.userDetails')" width="500px">
      <div v-if="selectedUser">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="$t('admin.username')">{{ selectedUser.username }}</el-descriptions-item>
          <el-descriptions-item :label="$t('admin.fullName')">{{ selectedUser.fullName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('admin.email')">{{ selectedUser.email }}</el-descriptions-item>
          <el-descriptions-item :label="$t('admin.phone')">{{ selectedUser.phone || $t('admin.none') }}</el-descriptions-item>
          <el-descriptions-item :label="$t('admin.role')">
            <el-tag :type="selectedUser.role === 'ADMIN' ? 'danger' : 'success'">
              {{ getRoleText(selectedUser.role) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('admin.createdDate')">{{ formatDate(selectedUser.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('admin.close') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="formDialogVisible" :title="isEditing ? $t('admin.editUser') : $t('admin.createUser')" width="600px">
      <el-form :model="userForm" :rules="formRules" ref="userFormRef" label-width="100px">
        <el-form-item :label="$t('admin.username')" prop="username" v-if="!isEditing">
          <el-input v-model="userForm.username" :placeholder="$t('admin.enterUsername')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.password')" prop="password" v-if="!isEditing">
          <el-input type="password" v-model="userForm.password" :placeholder="$t('admin.enterPassword')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.fullName')" prop="fullName">
          <el-input v-model="userForm.fullName" :placeholder="$t('admin.enterFullName')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.email')" prop="email">
          <el-input v-model="userForm.email" :placeholder="$t('admin.enterEmail')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.phone')" prop="phone">
          <el-input v-model="userForm.phone" :placeholder="$t('admin.enterPhone')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('admin.role')" prop="role">
          <el-select v-model="userForm.role" :placeholder="$t('admin.selectRole')">
            <el-option :label="$t('admin.roles.admin')" value="ADMIN"></el-option>
            <el-option :label="$t('admin.roles.customer')" value="CUSTOMER"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ $t('admin.cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ isEditing ? $t('admin.update') : $t('admin.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '@/api'

export default {
  name: 'UserManagement',
  data() {
    return {
      users: [],
      selectedUser: null,
      loading: false,
      dialogVisible: false,
      formDialogVisible: false,
      isEditing: false,
      userForm: {
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        role: 'CUSTOMER'
      },
      formRules: {
        username: [
          { required: true, message: this.$t('admin.usernameRequired'), trigger: 'blur' },
          { min: 3, max: 50, message: this.$t('admin.usernameLength'), trigger: 'blur' }
        ],
        password: [
          { required: true, message: this.$t('admin.passwordRequired'), trigger: 'blur' },
          { min: 6, message: this.$t('admin.passwordMinLength'), trigger: 'blur' }
        ],
        fullName: [
          { required: true, message: this.$t('admin.fullNameRequired'), trigger: 'blur' }
        ],
        email: [
          { required: true, message: this.$t('admin.emailRequired'), trigger: 'blur' },
          { 
            validator: (rule, value, callback) => {
              if (!value) {
                callback()
                return
              }
              
              const emailPattern = /^[a-zA-Z0-9@._-]+$/
              if (!emailPattern.test(value)) {
                callback(new Error(this.$t('admin.emailCharactersOnly')))
                return
              }
              
              const basicEmailPattern = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
              if (!basicEmailPattern.test(value)) {
                callback(new Error(this.$t('admin.emailFormatInvalid')))
                return
              }
              callback()
            }, 
            trigger: 'blur' 
          }
        ],
        phone: [
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback()
                return
              }
              
              const phonePattern = /^\d+$/
              if (!phonePattern.test(value)) {
                callback(new Error(this.$t('admin.phoneNumbersOnly')))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ],
        role: [
          { required: true, message: this.$t('admin.roleRequired'), trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    async loadUsers() {
      try {
        this.loading = true
        const response = await api.get('/admin/users')
        
        if (response.data.success) {
          this.users = response.data.data
        } else {
          ElMessage.error(this.$t('admin.loadUsersFailed') + ': ' + response.data.message)
        }
      } catch (error) {
        console.error('Load users error:', error)
        ElMessage.error(this.$t('admin.loadUsersFailed') + ': ' + (error.response?.data?.message || error.message))
      } finally {
        this.loading = false
      }
    },
    
    viewUser(user) {
      this.selectedUser = user
      this.dialogVisible = true
    },
    
    showCreateDialog() {
      this.isEditing = false
      this.resetForm()
      this.formDialogVisible = true
    },
    
    editUser(user) {
      this.isEditing = true
      this.selectedUser = user
      this.userForm = {
        username: user.username,
        password: '',
        fullName: user.fullName,
        email: user.email,
        phone: user.phone || '',
        role: user.role
      }
      this.formDialogVisible = true
    },
    
    resetForm() {
      this.userForm = {
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        role: 'CUSTOMER'
      }
      if (this.$refs.userFormRef) {
        this.$refs.userFormRef.clearValidate()
      }
    },
    
    async submitForm() {
      try {
        await this.$refs.userFormRef.validate()
        
        let response
        
        if (this.isEditing) {
          const updateData = {
            fullName: this.userForm.fullName,
            email: this.userForm.email,
            phone: this.userForm.phone,
            role: this.userForm.role
          }
          
          response = await api.put(`/admin/users/${this.selectedUser.id}`, updateData)
        } else {
          response = await api.post('/admin/users', this.userForm)
        }
        
        if (response.data.success) {
          ElMessage.success(this.isEditing ? this.$t('admin.userUpdatedSuccess') : this.$t('admin.userCreatedSuccess'))
          this.formDialogVisible = false
          this.loadUsers()
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        console.error('Submit form error:', error)
        if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message)
        } else {
          ElMessage.error(this.$t('admin.operationFailed'))
        }
      }
    },
    
    async deleteUser(user) {
      try {
        await ElMessageBox.confirm(this.$t('admin.confirmDeleteUser'), this.$t('admin.deleteUser'), {
          confirmButtonText: this.$t('admin.delete'),
          cancelButtonText: this.$t('admin.cancel'),
          type: 'danger'
        })
        
        const response = await api.delete(`/admin/users/${user.id}`)
        
        if (response.data.success) {
          ElMessage.success(this.$t('admin.userDeletedSuccess'))
          this.loadUsers()
        } else {
          ElMessage.error(this.$t('admin.deleteUserFailed') + ': ' + response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Delete user error:', error)
          ElMessage.error(this.$t('admin.deleteUserFailed') + ': ' + (error.response?.data?.message || error.message))
        }
      }
    },
    
    formatDate(date) {
      return dayjs(date).format('YYYY-MM-DD')
    },

    getRoleText(role) {
      const roleTexts = {
        'ADMIN': this.$t('admin.roles.admin'),
        'CUSTOMER': this.$t('admin.roles.customer')
      }
      return roleTexts[role] || role
    }
  },
  
  mounted() {
    this.loadUsers()
  }
}
</script>

<style scoped>
.user-management {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-section h2 {
  margin: 0;
  color: #303133;
}
</style>
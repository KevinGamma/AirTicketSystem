<template>
  <div class="profile">
    <el-card>
      <h2>{{ $t('profile.title') }}</h2>

      <div class="avatar-section">
        <AvatarUpload 
          :avatar-url="profileForm.avatarUrl"
          @avatar-updated="onAvatarUpdated"
        />
      </div>
      
      <el-alert
        :title="$t('profile.reLoginWarning')"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;">
      </el-alert>
      <el-form :model="profileForm" :rules="rules" ref="profileFormRef" :label-width="$i18n.locale === 'en' ? '120px' : '80px'">
        <el-form-item :label="$t('auth.username')">
          <el-input v-model="profileForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item :label="$t('auth.email')" prop="email">
          <el-input v-model="profileForm.email"></el-input>
        </el-form-item>
        <el-form-item :label="$t('auth.fullName')" prop="fullName">
          <el-input v-model="profileForm.fullName"></el-input>
        </el-form-item>
        <el-form-item :label="$t('auth.phone')" prop="phone">
          <el-input v-model="profileForm.phone"></el-input>
        </el-form-item>
        <div class="saved-passenger-section">
          <h3>乘机人信息</h3>
          <p class="saved-passenger-tip">可保存一组常用乘机人信息，订票时可直接带入。</p>
        </div>
        <el-form-item label="乘机人姓名">
          <el-input v-model="profileForm.savedPassengerName" placeholder="请输入乘机人姓名"></el-input>
        </el-form-item>
        <el-form-item label="身份证号" prop="savedPassengerIdNumber">
          <el-input
            v-model="profileForm.savedPassengerIdNumber"
            placeholder="请输入身份证号"
            maxlength="18"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile" :loading="loading">
            {{ $t('profile.updateProfile') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { ElMessage } from 'element-plus'
import api from '@/api'
import AvatarUpload from '@/components/AvatarUpload.vue'

export default {
  name: 'ProfileView',
  components: {
    AvatarUpload
  },
  data() {
    return {
      profileForm: {
        username: '',
        email: '',
        fullName: '',
        phone: '',
        avatarUrl: '',
        savedPassengerName: '',
        savedPassengerIdNumber: ''
      },
      loading: false,
      rules: {
        email: [
          { required: true, message: '', trigger: 'blur' },
          { type: 'email', message: '', trigger: 'blur' }
        ],
        fullName: [
          { required: true, message: '', trigger: 'blur' }
        ],
        savedPassengerIdNumber: [
          { pattern: /^$|^\d{17}[\dXx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapState(['currentUser'])
  },
  async mounted() {
    await this.fetchCurrentUser()
    this.initForm()
    this.updateValidationRules()
  },
  methods: {
    ...mapActions(['logout']),
    
    async fetchCurrentUser() {
      try {
        const response = await api.get('/auth/me')
        if (response.data.success) {
          this.$store.commit('SET_USER', response.data.data)
        }
      } catch (error) {
        console.error('Failed to fetch current user:', error)
      }
    },
    
    initForm() {
      if (this.currentUser) {
        this.profileForm = {
          username: this.currentUser.username,
          email: this.currentUser.email,
          fullName: this.currentUser.fullName,
          phone: this.currentUser.phone || '',
          avatarUrl: this.currentUser.avatarUrl || '',
          savedPassengerName: this.currentUser.savedPassengerName || '',
          savedPassengerIdNumber: this.currentUser.savedPassengerIdNumber || ''
        }
      }
    },
    
    async updateProfile() {
      try {
        const savedPassengerName = this.profileForm.savedPassengerName.trim()
        const savedPassengerIdNumber = this.profileForm.savedPassengerIdNumber.trim()
        if ((savedPassengerName && !savedPassengerIdNumber) || (!savedPassengerName && savedPassengerIdNumber)) {
          ElMessage.error('乘机人姓名和身份证号需要同时填写')
          return
        }

        await this.$refs.profileFormRef.validate()
        this.loading = true
        
        const response = await api.put('/auth/profile', {
          email: this.profileForm.email,
          fullName: this.profileForm.fullName,
          phone: this.profileForm.phone,
          savedPassengerName,
          savedPassengerIdNumber
        })
        
        if (response.data.success) {
          ElMessage.success(this.$t('profile.updateSuccess'))
          
          this.logout()
          setTimeout(() => {
            this.$router.push('/login')
          }, 2000)
        }
        
      } catch (error) {
        ElMessage.error(error.response?.data?.message || this.$t('profile.updateError'))
      } finally {
        this.loading = false
      }
    },
    
    updateValidationRules() {
      this.rules.email[0].message = this.$t('auth.emailRequired')
      this.rules.email[1].message = this.$t('auth.emailInvalid')
      this.rules.fullName[0].message = this.$t('auth.fullNameRequired')
      this.rules.savedPassengerIdNumber[0].message = '请输入正确的身份证号'
    },

    onAvatarUpdated(updatedUser) {
      this.profileForm.avatarUrl = updatedUser.avatarUrl || ''
      this.$store.commit('SET_USER', updatedUser)
    }
  },
  
  watch: {
    '$i18n.locale'() {
      this.updateValidationRules()
    },
    currentUser: {
      handler() {
        this.initForm()
      },
      immediate: true
    }
  }
}
</script>

<style scoped>
.profile {
  max-wid1th: 600px;
  margin: 0 auto;
}

.profile h2 {
  margin-bottom: 20px;
  color: #303133;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.saved-passenger-section {
  margin-bottom: 8px;
}

.saved-passenger-section h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #303133;
}

.saved-passenger-tip {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
</style>

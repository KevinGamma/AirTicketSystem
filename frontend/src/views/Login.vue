<template>
  <div class="login-container">
    <div class="language-switch">
      <el-dropdown @command="changeLanguage">
        <el-button type="text" style="color: white;">
          <el-icon><Globe /></el-icon>
          {{ $t('system.language') }}
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh">{{ $t('system.chinese') }}</el-dropdown-item>
            <el-dropdown-item command="en">{{ $t('system.english') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div class="login-card">
      <div class="header-section">
        <h2>{{ $t('system.title') }}</h2>
        <p class="tagline">说走就走的旅行，从这里开始。</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" :label-width="$i18n.locale === 'en' ? '100px' : '80px'">
        <el-form-item :label="$t('auth.username')" prop="username">
          <el-input v-model="loginForm.username" :placeholder="$t('auth.usernameRequired')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('auth.password')" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            :placeholder="$t('auth.passwordRequired')"
            @keyup.enter="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            {{ $t('auth.loginButton') }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-row justify="center" style="width: 100%;">
            <el-button
                type="text"
                @click="goToRegister"
            >
              {{ $t('auth.goToRegister') }}
            </el-button>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { ElMessage } from 'element-plus'

export default {
  name: 'LoginView',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapState(['loading'])
  },
  watch: {
    '$i18n.locale'() {
      this.updateValidationRules()
    }
  },
  mounted() {
    this.updateValidationRules()
  },
  methods: {
    async handleLogin() {
      try {
        await this.$refs.loginFormRef.validate()
        const result = await this.$store.dispatch('login', this.loginForm)
        
        if (result.success) {
          ElMessage.success(this.$t('auth.loginSuccess'))
          this.$router.push('/dashboard')
        } else {
          ElMessage.error(result.message || this.$t('auth.loginError'))
        }
      } catch (error) {
        console.error('Login error:', error)
      }
    },
    goToRegister() {
      this.$router.push('/register')
    },
    changeLanguage(lang) {
      this.$i18n.locale = lang
      localStorage.setItem('language', lang)
      this.updateValidationRules()
    },
    updateValidationRules() {
      this.rules.username[0].message = this.$t('auth.usernameRequired')
      this.rules.password[0].message = this.$t('auth.passwordRequired')
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url('@/OIP.webp');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 400px;
  position: relative;
  z-index: 2;
}

.header-section {
  text-align: center;
  margin-bottom: 30px;
}

.login-card h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 28px;
  font-weight: 600;
}

.tagline {
  margin: 0 0 20px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.5px;
  line-height: 1.4;
  opacity: 0.85;
}

.language-switch {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.language-switch .el-button {
  color: white !important;
  font-weight: 500;
}

.language-switch .el-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>
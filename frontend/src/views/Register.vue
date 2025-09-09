<template>
  <div class="register-container">
    <div class="register-card">
      <h2>创建账户</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱地址"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="registerForm.fullName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            type="text"
            @click="goToLogin"
            style="width: 100%"
          >
            已有账户？点击登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { ElMessage } from 'element-plus'

export default {
  name: 'RegisterView',
  data() {
    return {
      registerForm: {
        username: '',
        email: '',
        fullName: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度必须为3-20个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        fullName: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少为6个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: this.validatePasswordConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapState(['loading'])
  },
  methods: {
    validatePasswordConfirm(rule, value, callback) {
      if (value !== this.registerForm.password) {
        callback(new Error('两次密码输入不一致'))
      } else {
        callback()
      }
    },
    
    async handleRegister() {
      try {
        await this.$refs.registerFormRef.validate()
        
        const userData = {
          username: this.registerForm.username,
          email: this.registerForm.email,
          fullName: this.registerForm.fullName,
          password: this.registerForm.password
        }
        
        const result = await this.$store.dispatch('register', userData)
        
        if (result.success) {
          ElMessage.success('注册成功！请登录。')
          this.$router.push('/login')
        } else {
          ElMessage.error(result.message || '注册失败')
        }
      } catch (error) {
        console.error('Registration error:', error)
      }
    },
    
    goToLogin() {
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 500px;
}

.register-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}
</style>
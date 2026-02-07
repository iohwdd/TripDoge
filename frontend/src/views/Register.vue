<template>
  <div class="register-container">
    <a-card class="register-card" :bordered="false">
      <div class="header">
        <h2 class="title">注册账号</h2>
        <p class="subtitle">加入 TripDoge，开启 AI 之旅</p>
      </div>
      
      <a-form :model="form" layout="vertical" @submit="handleSubmit">
        <a-form-item field="email" label="邮箱" :rules="[{required:true,message:'请输入邮箱'},{type:'email',message:'邮箱格式不正确'}]">
          <a-input v-model="form.email" placeholder="请输入邮箱" allow-clear />
        </a-form-item>
        
        <a-form-item field="code" label="验证码" :rules="[{required:true,message:'请输入验证码'}]">
          <a-input-search
            v-model="form.code"
            placeholder="请输入验证码"
            :button-text="codeBtnText"
            search-button
            :loading="sendingCode"
            @search="sendCode"
          />
        </a-form-item>

        <a-form-item field="password" label="密码" :rules="[{required:true,message:'请输入密码'},{minLength:6,message:'密码最少6位'}]">
          <a-input-password v-model="form.password" placeholder="请输入密码" allow-clear />
        </a-form-item>
        
        <a-form-item field="confirmPassword" label="确认密码" :rules="[{required:true,message:'请确认密码'}, {validator: validateConfirmPassword}]">
          <a-input-password v-model="form.confirmPassword" placeholder="请再次输入密码" allow-clear />
        </a-form-item>

        <a-form-item field="nickname" label="昵称" :rules="[{required:true,message:'请输入昵称'}]">
          <a-input v-model="form.nickname" placeholder="怎么称呼您" allow-clear />
        </a-form-item>
        
        <a-button type="primary" html-type="submit" long :loading="loading" class="register-btn">
          注册并登录
        </a-button>
        
        <div class="actions">
          <span class="text">已有账号？</span>
          <a-link @click="$router.push('/login')">立即登录</a-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendEmail } from '@/api/user'
import { Message } from '@arco-design/web-vue'

const router = useRouter()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const codeBtnText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s 后重试` : '发送验证码'
})

const validateConfirmPassword = (value, callback) => {
  if (value !== form.password) {
    callback('两次输入的密码不一致')
  } else {
    callback()
  }
}

const sendCode = async () => {
  if (!form.email) {
    Message.warning('请先输入邮箱')
    return
  }
  if (countdown.value > 0) return

  sendingCode.value = true
  try {
    await sendEmail({ email: form.email })
    Message.success('验证码已发送（Mock环境: 任意填写即可）')
    
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    
  } catch (error) {
    // error handled
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async ({ values, errors }) => {
  if (errors) return
  
  loading.value = true
  try {
    const { confirmPassword, ...registerData } = values
    await register(registerData)
    Message.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // error handled
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.register-card {
  width: 450px;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  margin-bottom: 20px;
}

.title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 5px 0 0;
  color: var(--color-text-3);
  font-size: 14px;
}

.register-btn {
  margin-top: 10px;
}

.actions {
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
}

.text {
  color: var(--color-text-3);
}
</style>


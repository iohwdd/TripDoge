<template>
  <div class="login-container">
    <div class="login-box">
      <div class="left-panel">
        <div class="welcome-text">
          <h1>Welcome Back!</h1>
          <p>与 AI 萌宠开启奇妙对话之旅</p>
        </div>
        <img src="https://p1-arco.byteimg.com/tos-cn-i-uwbnlip3yd/3ee5f13fb09879ecb5185e440cef6eb9.png~tplv-uwbnlip3yd-webp.webp" class="mascot-img" />
      </div>
      
      <div class="right-panel">
        <div class="header">
          <h2 class="title">登录</h2>
          <p class="subtitle">TripDoge - 您的智能伙伴</p>
        </div>
        
        <a-form :model="form" layout="vertical" @submit="handleSubmit" class="login-form">
          <a-form-item field="email" hide-label :rules="[{required:true,message:'请输入邮箱'}]">
            <a-input v-model="form.email" placeholder="邮箱" size="large" allow-clear>
              <template #prefix>
                <icon-email />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item field="password" hide-label :rules="[{required:true,message:'请输入密码'}]">
            <a-input-password v-model="form.password" placeholder="密码" size="large" allow-clear>
              <template #prefix>
                <icon-lock />
              </template>
            </a-input-password>
          </a-form-item>
          
          <a-button type="primary" html-type="submit" long size="large" :loading="loading" class="login-btn">
            进入世界
          </a-button>
          
          <div class="actions">
            <span class="text">还没有账号？</span>
            <a-link @click="$router.push('/register')">立即注册</a-link>
          </div>
          
          <div class="mock-tip">
            默认账号: 884217544@qq.com / 123123
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import { IconEmail, IconLock } from '@arco-design/web-vue/es/icon'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  email: '884217544@qq.com',
  password: '123123'
})

const handleSubmit = async ({ values, errors }) => {
  if (errors) return
  
  loading.value = true
  try {
    await userStore.login(values)
    Message.success('欢迎回来！')
    router.push('/')
  } catch (error) {
    // 错误已在 request.js 处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: var(--color-bg-1);
  background-image: radial-gradient(circle at 50% 50%, #fff8eb 0%, #ffe4b5 100%);
  padding: 20px;
}

.login-box {
  display: flex;
  width: 800px;
  height: 500px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(255, 154, 46, 0.15);
  overflow: hidden;
}

.left-panel {
  flex: 1;
  background: linear-gradient(135deg, #FF9A2E 0%, #FF7D00 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  position: relative;
  padding: 40px;
}

.welcome-text h1 {
  font-size: 32px;
  margin-bottom: 10px;
}

.welcome-text p {
  font-size: 16px;
  opacity: 0.9;
}

.mascot-img {
  width: 180px;
  margin-top: 40px;
  filter: drop-shadow(0 10px 20px rgba(0,0,0,0.2));
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-15px); }
  100% { transform: translateY(0px); }
}

.right-panel {
  flex: 1;
  padding: 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.header {
  text-align: left;
  margin-bottom: 30px;
}

.title {
  font-size: 28px;
  color: #4E342E;
  margin-bottom: 5px;
}

.subtitle {
  color: #8D6E63;
}

.login-form {
  width: 100%;
}

.login-btn {
  margin-top: 10px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(255, 154, 46, 0.4);
}

.actions {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
}

.text {
  color: var(--color-text-3);
}

.mock-tip {
  margin-top: 40px;
  text-align: center;
  font-size: 12px;
  color: #ccc;
  background: #f9f9f9;
  padding: 8px;
  border-radius: 4px;
}

/* Mobile Adaptation */
@media (max-width: 768px) {
  .login-box {
    flex-direction: column;
    width: 100%;
    height: auto;
    max-width: 400px;
  }

  .left-panel {
    display: none; /* Hide decorative panel on mobile to save space */
  }

  .right-panel {
    padding: 30px 20px;
  }

  .header {
    margin-bottom: 20px;
    text-align: center;
  }
  
  .title {
    font-size: 24px;
  }
}
</style>

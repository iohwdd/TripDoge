<template>
  <div class="home-container" :style="{ background: currentRole?.bgColor || '#fff' }">
    <!-- 动态背景层 -->
    <div class="bg-layer" :style="{ backgroundImage: currentRole?.bgImage }"></div>
    
    <div class="content-layer">
      <div v-if="loading" class="loading-box">
        <a-spin dot />
      </div>

      <template v-else>
        <!-- 顶部问候 -->
        <div class="greeting-bar">
           <h1>{{ greeting }}，{{ userStore.userInfo.nickname }}</h1>
           <p>今天想谁陪你聊聊天？</p>
        </div>

        <!-- 核心展示区 -->
        <div class="showcase-area">
          <!-- 左侧：立绘与互动 -->
          <div class="character-stage">
            <!-- 状态气泡 -->
            <transition name="pop">
               <div class="status-bubble" v-if="currentRole" :key="currentRole.id">
                 {{ currentRole.statusText }}
                 <div class="bubble-tail"></div>
               </div>
            </transition>
            
            <!-- 角色立绘 (带呼吸动画) -->
            <div class="character-img-wrapper" @click="handleChat">
              <img 
                v-if="currentRole" 
                :src="currentRole.portraitUrl" 
                class="character-img" 
                :class="{ 'animate-breath': !isSwitching }"
              />
            </div>
          </div>

          <!-- 右侧：信息卡片 -->
          <div class="info-panel">
            <transition name="slide-fade" mode="out-in">
              <div class="role-card" v-if="currentRole" :key="currentRole.id">
                <div class="card-header">
                  <h2 class="role-name">{{ currentRole.name }}</h2>
                  <a-tag :color="currentRole.themeColor" class="role-tag">
                    {{ currentRole.roleSettingLabel }}
                  </a-tag>
                </div>

                <div class="intimacy-bar">
                   <icon-heart-fill :style="{ color: '#ff4d4f', marginRight: '8px' }" />
                   <span class="label">亲密度</span>
                   <a-progress 
                     :percent="currentRole.intimacy / 100" 
                     :color="currentRole.themeColor"
                     :show-text="false"
                     style="flex: 1; margin: 0 12px;"
                     size="small"
                   />
                   <span class="value">{{ currentRole.intimacy }}</span>
                </div>

                <p class="role-desc">{{ currentRole.description }}</p>

                <div class="tags-row">
                  <span v-for="tag in currentRole.tags" :key="tag" class="mini-tag">#{{ tag }}</span>
                </div>

                <a-button 
                  type="primary" 
                  size="large" 
                  long 
                  class="chat-btn"
                  :style="{ backgroundColor: currentRole.themeColor, borderColor: currentRole.themeColor }"
                  @click="handleChat"
                >
                  <icon-message style="margin-right: 8px" />
                  开始陪伴
                </a-button>
              </div>
            </transition>
          </div>
        </div>

        <!-- 底部切换栏 -->
        <div class="role-switcher">
          <div 
            v-for="role in roles" 
            :key="role.id" 
            class="switch-item"
            :class="{ active: currentRole?.id === role.id }"
            @click="switchRole(role)"
          >
            <a-avatar :size="50" :image-url="role.avatarUrl" :style="{ border: currentRole?.id === role.id ? `3px solid ${role.themeColor}` : '3px solid transparent' }" />
            <span class="switch-name" :class="{ active: currentRole?.id === role.id }">{{ role.name }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getRoleList } from '@/api/role'
import { getIntimacy } from '@/api/intimacy'
import { IconHeartFill, IconMessage } from '@arco-design/web-vue/es/icon'
import { adaptRoleData } from '@/utils/roleAdapter'

const router = useRouter()
const userStore = useUserStore()
const roles = ref([])
const currentRole = ref(null)
const loading = ref(false)
const isSwitching = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早安'
  if (hour < 18) return '午安'
  return '晚上好'
})

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    // 使用适配器处理数据
    const mapped = res.map(adaptRoleData)
    // 补充亲密度
    const withIntimacy = await Promise.all(
      mapped.map(async (role) => {
        try {
          const data = await getIntimacy(role.id)
          if (data && typeof data.intimacy === 'number') {
            role.intimacy = data.intimacy
          }
        } catch (e) {
          // ignore intimacy fetch errors
        }
        return role
      })
    )
    roles.value = withIntimacy
    if (roles.value.length > 0) {
      currentRole.value = roles.value[0]
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const switchRole = (role) => {
  if (currentRole.value?.id === role.id) return
  isSwitching.value = true
  currentRole.value = role
  setTimeout(() => {
    isSwitching.value = false
  }, 300)
}

const handleChat = () => {
  router.push({ name: 'chat', params: { roleId: currentRole.value.id } })
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped>
.home-container {
  position: relative;
  height: 100%;
  width: 100%;
  border-radius: 16px; /* 匹配 MainLayout 的圆角 */
  overflow: hidden;
  transition: background 0.5s ease;
}

.bg-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.4; /* 柔和背景 */
  transition: background-image 0.5s ease;
  filter: blur(40px);
}

.content-layer {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px 40px;
}

.greeting-bar {
  margin-bottom: 20px;
}
.greeting-bar h1 {
  margin: 0;
  font-size: 28px;
  color: #4E342E;
}
.greeting-bar p {
  margin: 5px 0 0;
  color: #8D6E63;
  font-size: 16px;
}

.showcase-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px; /* 左右间距 */
}

/* 左侧舞台 */
.character-stage {
  position: relative;
  width: 300px;
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: flex-end;
}

.status-bubble {
  position: absolute;
  top: 20px;
  right: -20px;
  background: #fff;
  padding: 12px 20px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  font-size: 14px;
  color: #5D4037;
  z-index: 10;
  max-width: 180px;
  animation: float 3s ease-in-out infinite;
}
.bubble-tail {
  position: absolute;
  bottom: -8px;
  left: 20px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #fff;
}

.character-img-wrapper {
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: transform 0.2s;
}
.character-img-wrapper:hover {
  transform: scale(1.02);
}

.character-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 10px 20px rgba(0,0,0,0.15));
}

.animate-breath {
  animation: breath 4s ease-in-out infinite;
}

/* 右侧信息卡片 */
.info-panel {
  width: 360px;
}

.role-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  padding: 32px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.role-name {
  margin: 0;
  font-size: 32px;
  color: #4E342E;
}

.intimacy-bar {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 8px 16px;
  border-radius: 12px;
}
.intimacy-bar .label {
  font-size: 12px;
  color: #8D6E63;
}
.intimacy-bar .value {
  font-weight: bold;
  color: #4E342E;
}

.role-desc {
  color: #6D4C41;
  line-height: 1.6;
  margin-bottom: 24px;
  font-size: 15px;
}

.tags-row {
  display: flex;
  gap: 10px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.mini-tag {
  font-size: 13px;
  color: #8D6E63;
  background: rgba(255,255,255,0.6);
  padding: 4px 10px;
  border-radius: 8px;
}

.chat-btn {
  height: 48px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

/* 底部切换 */
.role-switcher {
  display: flex;
  justify-content: center;
  gap: 30px;
  padding-bottom: 20px;
}

.switch-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.3s;
  flex-shrink: 0; /* Prevent shrinking on mobile */
}
.switch-item:hover, .switch-item.active {
  opacity: 1;
  transform: translateY(-5px);
}

.switch-name {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #5D4037;
  transition: all 0.3s;
}
.switch-name.active {
  color: #4E342E;
  transform: scale(1.1);
}

/* 动画定义 */
@keyframes breath {
  0% { transform: scale(1); }
  50% { transform: scale(1.03); }
  100% { transform: scale(1); }
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
  100% { transform: translateY(0px); }
}

/* Vue Transitions */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease-out;
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.pop-enter-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.pop-enter-from {
  opacity: 0;
  transform: scale(0.8) translateY(10px);
}

/* Mobile Adaptation */
@media (max-width: 768px) {
  .content-layer {
    padding: 10px 16px;
  }
  
  .greeting-bar {
    margin-bottom: 10px;
  }
  
  .greeting-bar h1 {
    font-size: 22px;
  }
  
  .showcase-area {
    flex-direction: column;
    gap: 20px;
    margin-bottom: 20px;
    overflow-y: auto; /* Allow scrolling if too tall */
  }
  
  .character-stage {
    width: 240px;
    height: 300px;
  }
  
  .info-panel {
    width: 100%;
  }
  
  .role-card {
    padding: 20px;
  }
  
  .role-switcher {
    justify-content: flex-start;
    overflow-x: auto;
    padding: 10px;
    gap: 20px;
    /* Hide scrollbar */
    scrollbar-width: none;
    -ms-overflow-style: none;
  }
  .role-switcher::-webkit-scrollbar {
    display: none;
  }
}
</style>

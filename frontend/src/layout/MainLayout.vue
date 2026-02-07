<template>
  <a-layout class="layout-container">
    <!-- Desktop Sider -->
    <a-layout-sider
      collapsible
      breakpoint="xl"
      :width="240"
      class="layout-sider desktop-only"
      :collapsed="collapsed"
      @collapse="onCollapse"
    >
      <div class="logo">
        <img src="https://p1-arco.byteimg.com/tos-cn-i-uwbnlip3yd/3ee5f13fb09879ecb5185e440cef6eb9.png~tplv-uwbnlip3yd-webp.webp" />
        <span v-if="!collapsed" class="logo-text">TripDoge</span>
      </div>
      
      <a-menu
        :default-selected-keys="[route.name]"
        :selected-keys="[route.name]"
        @menu-item-click="handleMenuClick"
        class="custom-menu"
      >
        <a-menu-item key="roles">
          <template #icon><icon-user-group /></template>
          伙伴列表
        </a-menu-item>
        <a-menu-item key="knowledge">
          <template #icon><icon-book /></template>
          记忆养成 (知识库)
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    
    <a-layout class="main-layout-body">
      <a-layout-header class="layout-header">
        <div class="header-content">
          <h2 class="page-title">{{ pageTitle }}</h2>
          
          <a-dropdown @select="handleCommand">
            <div class="user-info">
              <a-avatar :size="36" :image-url="userInfo.avatarUrl" style="background-color: var(--primary-4);">
                <icon-user />
              </a-avatar>
              <span class="username mobile-hidden">{{ userInfo.nickname || '铲屎官' }}</span>
              <icon-down style="color: #8D6E63;" />
            </div>
            <template #content>
              <a-doption value="logout">退出登录</a-doption>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      
      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
             <div style="height: 100%;">
                <component :is="Component" />
             </div>
          </transition>
        </router-view>
      </a-layout-content>

      <!-- Mobile Bottom Nav -->
      <div class="mobile-nav mobile-only">
        <div 
          class="nav-item" 
          :class="{ active: route.name === 'roles' }"
          @click="handleMenuClick('roles')"
        >
          <icon-user-group size="20" />
          <span>伙伴</span>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: route.name === 'knowledge' }"
          @click="handleMenuClick('knowledge')"
        >
          <icon-book size="20" />
          <span>记忆</span>
        </div>
      </div>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import { IconUserGroup, IconBook, IconUser, IconDown } from '@arco-design/web-vue/es/icon'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const userInfo = computed(() => userStore.userInfo)

const pageTitle = computed(() => {
  switch (route.name) {
    case 'roles': return '选择您的 AI 伙伴'
    case 'knowledge': return '知识库养成'
    case 'chat': return '亲密互动'
    default: return ''
  }
})

const onCollapse = (val) => {
  collapsed.value = val
}

const handleMenuClick = (key) => {
  if (route.name === key) return
  router.push({ name: key })
}

const handleCommand = async (value) => {
  if (value === 'logout') {
    await userStore.logout()
    Message.success('已退出登录')
    if (route.name !== 'login') {
    router.push('/login')
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100%;
  background-color: var(--color-bg-1);
}

.layout-sider {
  background-color: #fff;
  box-shadow: 4px 0 16px rgba(255, 154, 46, 0.05);
  border-right: none;
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4E342E;
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(180deg, #fff8eb 0%, #fff 100%);
}

.logo img {
  width: 36px;
  height: 36px;
  margin-right: 10px;
}

.logo-text {
  background: linear-gradient(45deg, #FF9A2E, #FF7D00);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.custom-menu {
  margin-top: 10px;
}

.custom-menu :deep(.arco-menu-item) {
  margin: 4px 10px;
  border-radius: 12px;
  color: #5D4037;
}

.layout-header {
  height: 72px;
  background-color: transparent; /* 透明背景，显示底部的暖色bg */
  padding: 0 32px;
}

.header-content {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #4E342E;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.2s;
}

.user-info:hover {
  background-color: #FFF8EB;
  transform: translateY(-1px);
}

.username {
  margin: 0 10px;
  font-size: 14px;
  color: #5D4037;
  font-weight: 500;
}

.layout-content {
  padding: 0 32px 32px 32px;
  overflow-y: auto;
  height: calc(100vh - 72px); /* Calculate height */
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* Mobile Adaptation */
.mobile-only {
  display: none;
}

@media (max-width: 768px) {
  .desktop-only {
    display: none !important;
  }
  
  .mobile-only {
    display: flex;
  }
  
  .mobile-hidden {
    display: none;
  }

  .layout-header {
    padding: 0 16px;
    height: 60px;
  }

  .page-title {
    font-size: 18px;
  }

  .layout-content {
    padding: 0 16px 60px 16px; /* Bottom padding for nav bar */
    height: calc(100vh - 60px);
  }
  
  /* Mobile Bottom Nav */
  .mobile-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 56px;
    background: #fff;
    border-top: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-around;
    align-items: center;
    z-index: 100;
    box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  }
  
  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #999;
    font-size: 10px;
    flex: 1;
    height: 100%;
  }
  
  .nav-item.active {
    color: #FF7D00;
  }
}
</style>

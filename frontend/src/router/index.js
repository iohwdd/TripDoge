import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/Register.vue')
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/roles',
      children: [
        {
          path: 'roles',
          name: 'roles',
          component: () => import('@/views/RoleList.vue')
        },
        {
          path: 'chat/:roleId',
          name: 'chat',
          component: () => import('@/views/Chat.vue')
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/Knowledge.vue')
        }
      ]
    }
  ]
})

// 简单的路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && to.name !== 'register' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router


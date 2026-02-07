<template>
  <router-view />
</template>

<style>
body {
  margin: 0;
  background-color: var(--color-bg-1);
  color: var(--color-text-1);
  font-family: 'Nunito', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  /* 防止抖动 */
  overflow-y: hidden; 
}

#app {
  height: 100vh;
}

/* 引入字体 (可选，这里用系统字体模拟圆润感) */
</style>

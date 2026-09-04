<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-text-size-adjust: 100%;
  -webkit-touch-callout: none;
  -webkit-tap-highlight-color: transparent;
}

/* 修复 Vant Toast 背景被 van-popup 白色背景覆盖的问题 */
.van-toast {
  background: rgba(0, 0, 0, 0.7) !important;
}
</style>

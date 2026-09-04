<template>
  <router-view />
</template>

<style>
@import 'nprogress/nprogress.css';

#nprogress .bar {
  background: #409eff !important;
  height: 3px !important;
}

#nprogress .peg {
  box-shadow: 0 0 10px #409eff, 0 0 5px #409eff !important;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>

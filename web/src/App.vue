<script setup lang="ts">
import { App, ConfigProvider } from '@hmfw/ant-design'
</script>

<template>
  <ConfigProvider :theme="{ colorPrimary: '#1677ff' }">
    <App>
      <RouterView />
    </App>
  </ConfigProvider>
</template>

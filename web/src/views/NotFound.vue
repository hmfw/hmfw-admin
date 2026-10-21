<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Button, Result } from '@hmfw/ant-design'

const router = useRouter()
</script>

<template>
  <Result status="404" title="404" sub-title="抱歉，您访问的页面不存在。">
    <template #extra>
      <Button type="primary" @click="router.replace('/')">返回首页</Button>
    </template>
  </Result>
</template>

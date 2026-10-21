<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Avatar } from '@hmfw/ant-design'
import { UserOutlined } from '@hmfw/icons'
import { getAvatarPresentation } from '@/utils/avatar'

const props = defineProps<{
  nickname?: string | null
  username?: string | null
  avatarUrl?: string | null
  size?: number | 'large' | 'small'
}>()

const imageFailed = ref(false)
const presentation = computed(() => getAvatarPresentation(props))
const imageSource = computed(() =>
  imageFailed.value ? undefined : presentation.value.imageUrl,
)

watch(
  () => props.avatarUrl,
  () => {
    imageFailed.value = false
  },
)
</script>

<template>
  <Avatar
    :size="size"
    :src="imageSource"
    :icon="!imageSource && !presentation.initial ? UserOutlined : undefined"
    :style="imageSource ? undefined : { backgroundColor: presentation.backgroundColor }"
    @error="imageFailed = true"
  >
    {{ imageSource ? '' : presentation.initial }}
  </Avatar>
</template>

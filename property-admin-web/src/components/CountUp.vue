<template>
  <span ref="elRef">{{ displayValue }}</span>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const props = withDefaults(defineProps<{
  endVal: number
  duration?: number
  prefix?: string
  suffix?: string
}>(), {
  duration: 1500,
  prefix: '',
  suffix: '',
})

const elRef = ref<HTMLElement>()
const displayValue = ref(props.prefix + '0' + props.suffix)

function easeOutExpo(t: number): number {
  return t === 1 ? 1 : 1 - Math.pow(2, -10 * t)
}

function animate() {
  const startTime = performance.now()
  const startVal = 0
  const endVal = props.endVal
  const duration = props.duration

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = easeOutExpo(progress)

    const current = Math.floor(startVal + (endVal - startVal) * eased)
    displayValue.value = props.prefix + current.toLocaleString() + props.suffix

    if (progress < 1) {
      requestAnimationFrame(step)
    }
  }

  requestAnimationFrame(step)
}

let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!elRef.value) return

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          animate()
          observer?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.3 }
  )

  observer.observe(elRef.value)
})

watch(() => props.endVal, () => {
  animate()
})
</script>
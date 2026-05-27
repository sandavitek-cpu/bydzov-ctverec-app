import { ref, onMounted, onUnmounted } from 'vue'

export function useScrollAnimation() {
  const observed = ref<Element[]>([])

  let observer: IntersectionObserver | null = null

  function observe(el: Element) {
    if (!observer) return
    observer.observe(el)
    observed.value.push(el)
  }

  onMounted(() => {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible')
            observer?.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.1, rootMargin: '0px 0px -40px 0px' }
    )

    document.querySelectorAll('.fade-in-up').forEach((el) => {
      observer?.observe(el)
      observed.value.push(el)
    })
  })

  onUnmounted(() => {
    observed.value.forEach((el) => observer?.unobserve(el))
    observed.value = []
  })

  return { observe }
}

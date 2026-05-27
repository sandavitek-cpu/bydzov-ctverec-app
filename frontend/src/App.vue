<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import ToastContainer from '@/components/ToastContainer.vue'
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImpersonateModal from '@/components/ImpersonateModal.vue'

const route = useRoute()
const { isLoggedIn, impersonating, name, username, restoreFromImpersonation } = useAuth()

const showImpersonateModal = ref(false)

const isLoginPage = computed(() => route.path === '/admin/login')
const isAdminPage = computed(() => route.path.startsWith('/admin') && !isLoginPage.value)
const showAdminSidebar = computed(() => isLoggedIn.value && isAdminPage.value)
const mobileSidebarOpen = ref(false)
</script>

<template>
  <div class="min-h-screen bg-bg flex flex-col">
    <ToastContainer />

    <!-- Impersonation bar -->
    <div v-if="impersonating" class="bg-red-light border-b border-red/30 px-4 py-2 text-center text-body-sm text-text">
      <span class="font-semibold">Impersonace:</span> {{ name }} (@{{ username }})
      <button @click="restoreFromImpersonation" class="ml-3 btn-primary btn-xs">Zpět</button>
    </div>

    <AppHeader @impersonate="showImpersonateModal = true" />

    <!-- Mobile sidebar trigger -->
    <button v-if="showAdminSidebar" @click="mobileSidebarOpen = !mobileSidebarOpen"
      class="fixed bottom-4 right-4 z-50 flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-white shadow-lg md:hidden">
      <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path v-if="!mobileSidebarOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
    </button>

    <!-- Mobile sidebar overlay -->
    <div v-if="mobileSidebarOpen && showAdminSidebar" class="fixed inset-0 z-30 bg-black/30 md:hidden" @click="mobileSidebarOpen = false"></div>

    <!-- Main content area -->
    <div class="mx-auto w-full flex-1 flex flex-col" :class="isAdminPage ? 'max-w-none' : 'max-w-wide'">
      <div v-if="showAdminSidebar" class="flex flex-1">
        <AppSidebar :mobileOpen="mobileSidebarOpen" @close="mobileSidebarOpen = false" />
        <main class="flex-1 px-4 lg:px-8 py-8">
          <RouterView v-slot="{ Component, route: r }">
            <Transition name="page-fade" mode="out-in">
              <component :is="Component" :key="r.path" />
            </Transition>
          </RouterView>
        </main>
      </div>
      <main v-else class="flex-1 px-4 lg:px-8 py-8 lg:py-10" :class="isLoginPage ? 'max-w-form mx-auto w-full' : 'max-w-content mx-auto w-full'">
        <RouterView v-slot="{ Component, route: r }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" :key="r.path" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <AppFooter />

    <ImpersonateModal
      v-if="showImpersonateModal"
      @close="showImpersonateModal = false"
    />
  </div>
</template>

<style>
@keyframes slide-in {
  from { opacity: 0; transform: translateX(100%); }
  to { opacity: 1; transform: translateX(0); }
}
.animate-slide-in {
  animation: slide-in 0.3s ease-out;
}

.mobile-nav-item {
  display: block;
  padding: 0.625rem 0.75rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  border-radius: var(--radius-lg);
  transition: background-color 160ms ease-out, color 160ms ease-out;
}
.mobile-nav-item:hover,
.mobile-nav-item:active {
  background: var(--bg-alt);
  color: var(--text);
}
.mobile-nav-item.router-link-active {
  background: rgba(9,9,123,0.08);
  color: var(--primary);
}
.mobile-nav-section {
  padding: 0.5rem 0.75rem 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-soft);
}

.mobile-nav-enter-active,
.mobile-nav-leave-active {
  transition: all 0.2s ease-out;
}
.mobile-nav-enter-from,
.mobile-nav-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease-out, transform 0.2s ease-out;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.nav-link {
  position: relative;
  padding: 0.5rem 1rem;
  font-size: 14px;
  line-height: 16px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text-muted);
  text-decoration: none;
  border-radius: var(--radius-lg);
  transition: background-color 160ms ease-out, color 160ms ease-out;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  width: 0;
  height: 2px;
  background-color: var(--primary);
  border-radius: 1px;
  transition: width 200ms ease-out, left 200ms ease-out;
}
.nav-link:hover {
  background-color: var(--bg-alt);
  color: var(--text);
}
.nav-link:hover::after {
  width: 60%;
  left: 20%;
}
.nav-link-active {
  background-color: rgba(9,9,123,0.08);
  color: var(--primary);
}
.nav-link-active::after {
  width: 60%;
  left: 20%;
}
.nav-link-active:hover {
  background-color: rgba(9,9,123,0.12);
  color: var(--primary);
}
</style>

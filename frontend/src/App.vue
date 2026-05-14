<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen">
    <header class="border-b border-slate-800 bg-slate-900/80 backdrop-blur">
      <div class="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-4">
        <RouterLink to="/" class="text-lg font-semibold tracking-tight text-amber-400">
          Novobydžovský čtverec
        </RouterLink>
        <nav class="flex items-center gap-3 text-sm text-slate-400">
          <RouterLink to="/" class="hover:text-slate-200" active-class="text-amber-300">
            Domů
          </RouterLink>
          <RouterLink to="/registrace" class="hover:text-slate-200" active-class="text-amber-300">
            Přihláška
          </RouterLink>
          <RouterLink to="/rozhodci" class="hover:text-slate-200" active-class="text-amber-300">
            Rozhodčí
          </RouterLink>
          <RouterLink
            to="/admin/prihlaseni"
            class="hover:text-amber-300"
            active-class="text-amber-300"
          >
            Admin
          </RouterLink>
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-3xl px-4 py-10">
      <RouterView />
    </main>
  </div>
</template>

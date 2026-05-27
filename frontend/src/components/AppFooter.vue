<script setup lang="ts">
import logoCtverec from '@/assets/logo_ctverec.png'
import logoPrevit from '@/assets/logo_previt.png'
</script>

<template>
  <footer class="bg-surface-strong mt-12 shrink-0">
    <div class="h-0.5 w-full" style="background: linear-gradient(90deg, var(--red), var(--primary), var(--red))"></div>
    <div class="mx-auto max-w-wide px-4 lg:px-8 py-10">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 text-center sm:text-left">
        <div class="flex flex-col items-center sm:items-start">
          <div class="flex items-center gap-2 mb-3">
            <img :src="logoCtverec" alt="" class="h-7 w-auto object-contain opacity-60" />
            <span class="font-display text-lg tracking-[0.04em] text-text">Novobydžovský<br />Čtverec</span>
            <span class="text-meta text-red/60">Memoriál Elišky Junkové</span>
          </div>
          <p class="text-body-sm text-text-muted leading-relaxed">
            Klub přátel historických vozidel, z.s.
          </p>
          <p class="mt-2 text-body-sm text-text-soft leading-relaxed">
            V. Kl. Klicpery 624<br />
            504 01 Nový Bydžov
          </p>
        </div>

        <div class="flex flex-col items-center sm:items-start">
          <h4 class="text-label text-text mb-3">Kontaktní údaje</h4>
          <div class="space-y-2 text-body-sm text-text-muted">
            <p><span class="text-text-soft">IČO:</span> 26630061</p>
            <p><span class="text-text-soft">Bankovní účet:</span><br />
              <span class="font-mono text-text">1086360369/0800</span> (Česká spořitelna)
            </p>
            <a
              href="https://previt.atlassian.net/servicedesk/customer/portal/100"
              target="_blank" rel="noopener noreferrer"
              class="inline-flex items-center gap-1.5 mt-2 rounded-md border border-red/30 px-3 py-1.5 text-meta text-red no-underline hover:bg-red-light transition-colors"
              title="Helpdesk – technická podpora"
            >
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              HELPDESK
            </a>
          </div>
        </div>

        <div class="flex flex-col items-center sm:items-end justify-between">
          <div>
            <p class="text-meta text-text-soft mb-2">&copy; {{ new Date().getFullYear() }}</p>
          </div>
          <a
            href="https://pre-vit.cz"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-2 no-underline text-body-sm text-text-muted hover:text-primary transition-colors group"
            title="Previt – vývojář a správce aplikace"
          >
            <span>vytvořil</span>
            <img
              :src="logoPrevit"
              alt="Previt"
              class="h-6 w-auto object-contain opacity-60 group-hover:opacity-100 transition-opacity"
            />
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>

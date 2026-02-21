<template>
  <div class="app">
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
      <div class="container-fluid">
        <router-link to="/" class="navbar-brand">
          <i class="bi bi-book"></i> {{ t('nav.virtualBookstore') }}
        </router-link>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <!-- Cart Icon (only visible when authenticated) -->
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <router-link to="/cart" class="nav-link position-relative">
                <i class="bi bi-cart3" style="font-size: 1.2rem;"></i>
                <span 
                  v-if="cartStore.totalItems > 0" 
                  class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                  style="font-size: 0.7rem;"
                >
                  {{ cartStore.totalItems }}
                </span>
              </router-link>
            </li>
            <li class="nav-item" v-if="!authStore.isAuthenticated">
              <router-link to="/auth/login" class="nav-link">{{ t('nav.login') }}</router-link>
            </li>
            <li class="nav-item" v-if="!authStore.isAuthenticated">
              <router-link to="/auth/register" class="nav-link">{{ t('nav.register') }}</router-link>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <router-link to="/profile" class="nav-link">
                <i class="bi bi-person-circle"></i> {{ t('nav.profile') }}
              </router-link>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <span class="nav-link">{{ t('nav.welcome') }}, {{ authStore.user?.firstName || authStore.user?.username }}</span>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <button @click="logout" class="btn btn-outline-light nav-link">{{ t('nav.logout') }}</button>
            </li>
            <!-- Language Switcher -->
            <li class="nav-item">
              <LanguageSwitcher />
            </li>
          </ul>
        </div>
      </div>
    </nav>
    
    <router-view />
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/authStore'
import { useCartStore } from './stores/cartStore'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import LanguageSwitcher from './components/LanguageSwitcher.vue'

const authStore = useAuthStore()
const cartStore = useCartStore()
const router = useRouter()
const { t } = useI18n()

const logout = () => {
  authStore.logout()
  cartStore.clearCart()
  router.push('/auth/login')
}
</script>

<style>
body {
  background-color: #f5f5f5;
}
</style>

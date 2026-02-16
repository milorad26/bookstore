<template>
  <div class="app">
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
      <div class="container-fluid">
        <router-link to="/" class="navbar-brand">
          <i class="bi bi-book"></i> Virtual Bookstore
        </router-link>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item" v-if="!authStore.isAuthenticated">
              <router-link to="/auth/login" class="nav-link">Login</router-link>
            </li>
            <li class="nav-item" v-if="!authStore.isAuthenticated">
              <router-link to="/auth/register" class="nav-link">Register</router-link>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <router-link to="/profile" class="nav-link">
                <i class="bi bi-person-circle"></i> Profile
              </router-link>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <span class="nav-link">Welcome, {{ authStore.user?.username }}</span>
            </li>
            <li class="nav-item" v-if="authStore.isAuthenticated">
              <button @click="logout" class="btn btn-outline-light nav-link">Logout</button>
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
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const logout = () => {
  authStore.logout()
  router.push('/auth/login')
}
</script>

<style>
body {
  background-color: #f5f5f5;
}
</style>

<template>
  <div class="language-switcher">
    <select 
      v-model="locale"
      @change="handleLanguageChange"
      class="form-select form-select-sm language-select"
    >
      <option value="en">EN</option>
      <option value="sr">RS</option>
    </select>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const handleLanguageChange = () => {
  localStorage.setItem('locale', locale.value)
}
</script>

<style scoped>
.language-switcher {
  display: inline-block;
}

.language-select {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
  cursor: pointer;
  width: 70px;
  padding: 0.25rem 0.5rem;
}

.language-select:focus {
  background-color: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
  color: white;
  box-shadow: 0 0 0 0.25rem rgba(255, 255, 255, 0.1);
}

.language-select option {
  background-color: #343a40;
  color: white;
}
</style>

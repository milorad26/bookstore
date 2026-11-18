<template>
  <div
    v-if="totalElements > 0"
    class="d-flex flex-column flex-sm-row justify-content-between align-items-center gap-3 mt-4"
  >
    <p class="text-muted mb-0" aria-live="polite">
      {{ t('common.showingRange', { start: rangeStart, end: rangeEnd, total: totalElements, items: itemLabel }) }}
    </p>
    <nav :aria-label="t('common.pagination', { items: itemLabel })">
      <ul class="pagination mb-0">
        <li class="page-item" :class="{ disabled: page <= 1 }">
          <button class="page-link" type="button" :disabled="page <= 1" @click="$emit('change', page - 1)">
            {{ t('common.previous') }}
          </button>
        </li>
        <li class="page-item disabled" aria-current="page">
          <span class="page-link">{{ t('common.pageOf', { page, pages: totalPages }) }}</span>
        </li>
        <li class="page-item" :class="{ disabled: page >= totalPages }">
          <button class="page-link" type="button" :disabled="page >= totalPages" @click="$emit('change', page + 1)">
            {{ t('common.next') }}
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  itemLabel: { type: String, required: true }
})

defineEmits(['change'])

const { t } = useI18n()
const rangeStart = computed(() => props.totalElements === 0 ? 0 : (props.page - 1) * props.size + 1)
const rangeEnd = computed(() => Math.min(props.page * props.size, props.totalElements))
</script>

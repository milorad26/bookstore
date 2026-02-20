# Internationalization (i18n) Setup Guide

## Overview
Your application now supports **English (EN)** and **Serbian (SR)** languages using Vue I18n.

---

## 📁 Translation Files Location

All text translations are stored in these files:

```
bookstore-frontend/src/i18n/locales/
├── en.js     ← Edit English translations here
└── sr.js     ← Edit Serbian translations here
```

### How to Edit Translations

1. **Open the translation file** you want to edit:
   - `src/i18n/locales/en.js` for English
   - `src/i18n/locales/sr.js` for Serbian

2. **Find the key** you want to change. For example:
   ```javascript
   shop: {
     addToCart: 'Add to Cart',  // ← Change this text
     price: 'Price'
   }
   ```

3. **Update the text** and save the file. Changes will appear immediately when you refresh the page.

### Adding New Translations

To add a new translation key:

1. **Add it to BOTH language files** (en.js and sr.js):
   
   **In en.js:**
   ```javascript
   shop: {
     newKey: 'New Text in English'
   }
   ```
   
   **In sr.js:**
   ```javascript
   shop: {
     newKey: 'Novi Tekst na Srpskom'
   }
   ```

2. **Use it in your Vue component:**
   ```vue
   <template>
     <p>{{ t('shop.newKey') }}</p>
   </template>
   ```

---

## 🔧 How to Use i18n in Your Pages

### Step 1: Import useI18n

```vue
<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
```

### Step 2: Replace Hardcoded Text

**Before:**
```vue
<template>
  <h1>My Profile</h1>
  <button>Save</button>
  <p>Welcome, {{ username }}</p>
</template>
```

**After:**
```vue
<template>
  <h1>{{ t('profile.myProfile') }}</h1>
  <button>{{ t('common.save') }}</button>
  <p>{{ t('nav.welcome') }}, {{ username }}</p>
</template>
```

### Step 3: Handle Placeholders and Attributes

**For placeholders:**
```vue
<input :placeholder="t('login.enterUsername')" />
```

**For button text:**
```vue
<button>{{ t('common.submit') }}</button>
```

**For titles/tooltips:**
```vue
<button :title="t('common.edit')">
  <i class="bi bi-pencil"></i>
</button>
```

---

## 🎯 Examples for Common Scenarios

### Cart.vue
```vue
<script setup>
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <h2>{{ t('cart.shoppingCart') }}</h2>
  <p v-if="cart.length === 0">{{ t('cart.emptyCart') }}</p>
  <button>{{ t('cart.proceedToCheckout') }}</button>
</template>
```

### Login.vue
```vue
<script setup>
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <h2>{{ t('login.login') }}</h2>
  <input :placeholder="t('login.enterUsername')" />
  <input :placeholder="t('login.enterPassword')" />
  <button>{{ t('login.login') }}</button>
  <p>{{ t('login.noAccount') }}</p>
  <router-link to="/register">{{ t('login.registerHere') }}</router-link>
</template>
```

### Orders.vue
```vue
<script setup>
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <h2>{{ t('orders.myOrders') }}</h2>
  <p>{{ t('orders.viewHistory') }}</p>
  <button>{{ t('orders.viewDetails') }}</button>
</template>
```

---

## 🌐 Language Switcher

The language switcher is already added to the navigation bar in `App.vue`. Users can click the flag button to switch between English and Serbian.

The selected language is saved in **localStorage**, so it persists across page refreshes.

---

## ❓ Backend Changes Needed?

### **NO backend changes are required!**

The i18n implementation is **100% frontend-only**. Here's why:

1. **All translations are in the frontend** - The browser handles language switching
2. **API responses don't change** - Your backend returns the same data (book titles, prices, etc.)
3. **Only UI text changes** - Labels, buttons, messages, etc. are translated

### When you MIGHT need backend changes:

- **Storing user language preference in database** - If you want to remember each user's language choice
- **Multilingual content** - If you want book descriptions, titles, etc. in multiple languages (currently not implemented)
- **Email notifications** - If you want to send emails in the user's language

For now, the language preference is stored in **localStorage** (browser storage), which works perfectly for most use cases.

---

## 📝 Quick Checklist for Adding i18n to a Page

1. ✅ Import `useI18n` in the script section
2. ✅ Destructure `t` from `useI18n()`
3. ✅ Replace all hardcoded text with `t('key.path')` or `:attribute="t('key.path')"`
4. ✅ Make sure the translation keys exist in both `en.js` and `sr.js`
5. ✅ Test by clicking the language switcher

---

## 🔍 Translation Key Structure

The translations are organized by feature:

```javascript
{
  nav: {},        // Navigation bar
  shop: {},       // Shop/bookstore page
  cart: {},       // Shopping cart
  orders: {},     // Orders page
  profile: {},    // User profile
  login: {},      // Login page
  register: {},   // Registration page
  admin: {},      // Admin panel
  messages: {},   // Alert/notification messages
  common: {},     // Reusable buttons/labels
  status: {}      // Order statuses
}
```

---

## 💡 Tips

1. **Don't duplicate translations** - Use `common` for reusable text like "Save", "Cancel", "Close"
2. **Be consistent** - Use the same translation key structure across pages
3. **Test both languages** - Always check that text looks good in both EN and SR
4. **Keep keys descriptive** - Use meaningful names like `cart.emptyCart` instead of `cart.msg1`

---

## 🚀 Current Status

✅ **Completed:**
- i18n setup and configuration
- English and Serbian translation files
- Language switcher component in navbar
- Shop.vue page fully translated
- App.vue navigation fully translated

📝 **To Do:**
- Update remaining pages (Cart, Login, Register, Orders, Profile, etc.)
- Follow the examples in this guide to add `t()` function calls

---

## Example: Full Shop.vue Implementation

See `src/pages/Shop.vue` for a complete working example of i18n implementation.

Key points from Shop.vue:
- Import `useI18n` and extract `t` function
- Replace all text with `t('category.key')`
- Use `:placeholder="t('key')"` for input placeholders
- Use `t('key')` in JavaScript for dynamic messages

---

Need to add more languages? Just create a new file like `de.js` for German and add it to `i18n/index.js`!

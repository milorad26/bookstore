import { beforeEach, describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { createPinia, setActivePinia } from 'pinia'
import { useCartStore } from '../src/stores/cartStore.js'
import en from '../src/i18n/locales/en.js'
import sr from '../src/i18n/locales/sr.js'

const book = {
  id: 42,
  title: 'Test Book',
  author: 'Test Author',
  isbn: '1234567890',
  price: 12.5,
  stockQuantity: 2
}

describe('cart stock limits', () => {
  it('has inventory alert messages in both supported languages', () => {
    assert.match(en.books.inventoryAlertTitle, /Low-stock alert/)
    assert.match(en.books.inventoryAlertItem, /\{stock\} in stock/)
    assert.match(sr.books.inventoryAlertTitle, /Upozorenje za malu zalihu/)
    assert.match(sr.books.inventoryAlertItem, /na stanju: \{stock\}/)
  })

  beforeEach(() => {
    const values = new Map()
    globalThis.localStorage = {
      getItem: key => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, String(value)),
      removeItem: key => values.delete(key)
    }
    setActivePinia(createPinia())
  })

  it('allows adding up to available stock and rejects the next copy', () => {
    const cart = useCartStore()

    assert.deepEqual(cart.addToCart(book), { success: true })
    assert.deepEqual(cart.addToCart(book), { success: true })
    assert.deepEqual(cart.addToCart(book), { success: false, availableStock: 2 })
    assert.equal(cart.cartItems[0].quantity, 2)
  })

  it('rejects a cart quantity above stock and returns the localized-message parameters', () => {
    const cart = useCartStore()
    cart.addToCart(book)

    assert.deepEqual(cart.updateQuantity(book.id, 3), {
      success: false,
      availableStock: 2,
      title: book.title
    })
    assert.equal(cart.cartItems[0].quantity, 1)
  })

  it('has a stock-limit message in both supported languages', () => {
    assert.match(en.cart.stockLimit, /only \{stock\} available/)
    assert.match(sr.cart.stockLimit, /dostupna količina je \{stock\}/)
  })
})

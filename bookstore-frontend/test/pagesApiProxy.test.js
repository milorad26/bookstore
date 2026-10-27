import { afterEach, describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { onRequest } from '../functions/api/[[path]].js'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
})

describe('Cloudflare Pages API proxy', () => {
  it('forwards API requests and cookies while returning upstream cookies', async () => {
    let forwardedRequest
    globalThis.fetch = async request => {
      forwardedRequest = request
      return new Response('ok', {
        headers: { 'Set-Cookie': 'refreshToken=secure-token; HttpOnly; Secure; SameSite=Strict; Path=/api/auth' }
      })
    }

    const request = new Request('https://bookstore.pages.dev/api/auth/refresh?source=test', {
      method: 'POST',
      headers: { Cookie: 'refreshToken=existing-token', 'Content-Type': 'application/json' },
      body: '{}'
    })
    const response = await onRequest({
      request,
      env: { API_ORIGIN: 'https://virtual-bookstore-api.onrender.com' }
    })

    assert.equal(forwardedRequest.url, 'https://virtual-bookstore-api.onrender.com/api/auth/refresh?source=test')
    assert.equal(forwardedRequest.method, 'POST')
    assert.equal(forwardedRequest.headers.get('cookie'), 'refreshToken=existing-token')
    assert.equal(forwardedRequest.headers.get('x-forwarded-host'), 'bookstore.pages.dev')
    assert.match(response.headers.get('set-cookie'), /SameSite=Strict/)
    assert.equal(await response.text(), 'ok')
  })

  it('returns a clear configuration error when the API origin is missing', async () => {
    const response = await onRequest({
      request: new Request('https://bookstore.pages.dev/api/books'),
      env: {}
    })

    assert.equal(response.status, 500)
    assert.equal(await response.text(), 'API_ORIGIN is not configured')
  })
})

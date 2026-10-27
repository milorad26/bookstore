export async function onRequest({ request, env }) {
  if (!env.API_ORIGIN) {
    return new Response('API_ORIGIN is not configured', { status: 500 })
  }

  const incomingUrl = new URL(request.url)
  let apiOrigin
  try {
    apiOrigin = new URL(env.API_ORIGIN)
  } catch {
    return new Response('API_ORIGIN must be a valid URL', { status: 500 })
  }

  if (!['https:', 'http:'].includes(apiOrigin.protocol)) {
    return new Response('API_ORIGIN must use HTTP or HTTPS', { status: 500 })
  }

  const upstreamUrl = new URL(`${incomingUrl.pathname}${incomingUrl.search}`, apiOrigin.origin)
  const upstreamRequest = new Request(upstreamUrl, request)
  upstreamRequest.headers.set('X-Forwarded-Host', incomingUrl.host)
  upstreamRequest.headers.set('X-Forwarded-Proto', incomingUrl.protocol.slice(0, -1))

  return fetch(upstreamRequest)
}

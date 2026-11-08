const BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')
const TOKEN_KEY = 'vivabem_token'
const USER_KEY = 'vivabem_user'
export const SESSION_EXPIRED_EVENT = 'vivabem:session-expired'

function token() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setSession(auth) {
  if (!auth?.token || !auth.user) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    return
  }
  localStorage.setItem(TOKEN_KEY, auth.token)
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!token() || !raw) {
    setSession(null)
    return null
  }
  try {
    const user = JSON.parse(raw)
    if (user && typeof user === 'object') return user
  } catch {
    // Discard a malformed cached session below.
  }
  setSession(null)
  return null
}

async function request(path, options = {}) {
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...options.headers,
  }
  const savedToken = token()
  if (savedToken) headers.Authorization = `Bearer ${savedToken}`

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Não foi possível conectar à API. Verifique se o serviço está ativo e tente novamente.')
  }

  if (response.status === 401) {
    setSession(null)
    if (savedToken) window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    throw new Error(savedToken ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha incorretos.')
  }
  if (response.status === 204) return null
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.detail || data.message || `A API respondeu ${response.status}`)
  }
  return data
}

export function login(email, password) {
  return request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
}

export function register(payload) {
  return request('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) })
}

export function getMe() {
  return request('/api/me')
}

export function getAgenda(date) {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return request(`/api/agenda${query}`)
}

export function completeItem(payload) {
  return request('/api/agenda/complete', { method: 'POST', body: JSON.stringify(payload) })
}

export function createAppointment(payload) {
  return request('/api/appointments', { method: 'POST', body: JSON.stringify(payload) })
}

export function getWellbeing(date) {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return request(`/api/wellbeing${query}`)
}

export function updateWellbeing(payload) {
  return request('/api/wellbeing', { method: 'PUT', body: JSON.stringify(payload) })
}

export function chat(message) {
  return request('/api/ai/chat', { method: 'POST', body: JSON.stringify({ message }) })
}

export async function getHealth() {
  let response
  try {
    response = await fetch(`${BASE_URL}/api/health`)
  } catch {
    throw new Error('Não foi possível conectar à API.')
  }
  if (!response.ok && response.status !== 503) {
    throw new Error(`A API respondeu ${response.status}`)
  }
  return response.json()
}

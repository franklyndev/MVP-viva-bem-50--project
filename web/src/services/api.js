// Todas as chamadas à API passam por aqui. Em desenvolvimento, o Vite encaminha /api para o Spring Boot.
const BASE_URL = import.meta.env.VITE_API_URL ?? ''

export async function getHealth() {
  const response = await fetch(`${BASE_URL}/api/health`)
  if (!response.ok && response.status !== 503) {
    throw new Error(`A API respondeu ${response.status}`)
  }
  return response.json()
}

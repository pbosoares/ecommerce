const API_BASE = import.meta.env.VITE_API_URL || '/api'

export async function request(path, { token, ...options } = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })
  } catch {
    throw new Error('Não foi possível conectar à loja. Verifique se a API está ligada.')
  }
  if (!response.ok) {
    if (response.status === 401) throw new Error('Sua sessão terminou. Entre novamente para continuar.')
    if (response.status === 409) throw new Error('Este item não está disponível na quantidade escolhida.')
    if (response.status === 422) throw new Error('Não foi possível calcular o frete. Confira o peso dos produtos.')
    throw new Error(`Não foi possível concluir a operação (${response.status}).`)
  }
  if (response.status === 204) return null
  return response.json()
}

export const money = (value) => new Intl.NumberFormat('pt-BR', {
  style: 'currency', currency: 'BRL',
}).format(Number(value || 0))

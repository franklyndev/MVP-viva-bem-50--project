import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BigButton } from '../components/AppShell'
import { useAuth } from '../auth-context'
import { login, register } from '../services/api'

export default function Login() {
  const nav = useNavigate()
  const { signIn } = useAuth()
  const [mode, setMode] = useState('register')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [city, setCity] = useState('')
  const [consent, setConsent] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function toggleMode() {
    setMode(mode === 'login' ? 'register' : 'login')
    setConsent(false)
    setError('')
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const auth = mode === 'login'
        ? await login(email, password)
        : await register({ name, email, password, birthDate, city, consent })
      signIn(auth)
      nav('/inicio')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <section className="hero login-hero">
        <div className="login-card" style={{ margin: '0 auto', maxWidth: '28rem' }}>
          <p style={{ fontWeight: 600, fontSize: '1.25rem' }}>VivaBem 50+</p>
          <h1 className="title" style={{ marginTop: 12 }}>Olá! Que bom ter você aqui.</h1>
          <p style={{ marginTop: 12, fontSize: '1.125rem' }}>Sua saúde e sua rotina, organizadas com carinho.</p>
        </div>
      </section>
      <main className="login-card">
        <form onSubmit={submit} className="card login-form">
          {mode === 'register' && (
            <>
              <div className="field">
                <label htmlFor="name">Nome</label>
                <input id="name" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} />
              </div>
              <div className="field">
                <label htmlFor="birth">Data de nascimento</label>
                <input id="birth" type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} required />
              </div>
              <div className="field">
                <label htmlFor="city">Cidade</label>
                <input id="city" value={city} onChange={(e) => setCity(e.target.value)} maxLength={80} />
              </div>
            </>
          )}
          <div className="field">
            <label htmlFor="email">E-mail</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="field">
            <label htmlFor="password">Senha</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={mode === 'register' ? 8 : undefined} maxLength={72} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
          </div>
          {mode === 'register' && (
            <div className="consent-field">
              <input id="consent" type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
              <label htmlFor="consent">Concordo com o uso dos meus dados para oferecer os recursos de saúde do VivaBem 50+.</label>
            </div>
          )}
          {error && <p className="error" role="alert">{error}</p>}
          <BigButton type="submit" disabled={loading}>{loading ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}</BigButton>
          <BigButton type="button" variant="outline" onClick={toggleMode} disabled={loading}>
            {mode === 'login' ? 'Quero me cadastrar' : 'Já tenho conta'}
          </BigButton>
        </form>
        <p className="muted" style={{ marginTop: 24, textAlign: 'center' }}>Precisa de ajuda? Ligue (85) 4000-0000</p>
      </main>
    </div>
  )
}

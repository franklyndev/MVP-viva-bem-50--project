import { useEffect, useRef, useState } from 'react'
import { Mic, Send } from 'lucide-react'
import { AppShell } from '../components/AppShell'
import { useAuth } from '../auth-context'
import { chat } from '../services/api'

const chips = ['Agendar consulta', 'Dica de alimentação', 'Lembrar remédio', 'Exercício leve']

export default function Assistente() {
  const { user } = useAuth()
  const name = user?.name?.split(' ')[0] ?? ''
  const [msgs, setMsgs] = useState([{ from: 'ia', text: `Olá, ${name}! Sou a Vivi, sua assistente. Como posso ajudar hoje?` }])
  const [txt, setTxt] = useState('')
  const [ouvindo, setOuvindo] = useState(false)
  const [busy, setBusy] = useState(false)
  const [voiceError, setVoiceError] = useState('')
  const recognitionRef = useRef(null)

  useEffect(() => () => recognitionRef.current?.abort(), [])

  async function send(text) {
    const message = text.trim()
    if (!message || busy) return
    if (message.length > 500) {
      setVoiceError('A mensagem pode ter até 500 caracteres.')
      return
    }
    setVoiceError('')
    setBusy(true)
    setMsgs((m) => [...m, { from: 'eu', text: message }])
    setTxt('')
    try {
      const response = await chat(message)
      setMsgs((m) => [...m, { from: 'ia', text: response.reply }])
    } catch (err) {
      setMsgs((m) => [...m, { from: 'ia', text: err.message }])
    } finally {
      setBusy(false)
    }
  }

  function startVoice() {
    if (busy || ouvindo) return
    setVoiceError('')
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SpeechRecognition) {
      setVoiceError('O reconhecimento de voz não está disponível neste navegador. Você pode escrever sua mensagem.')
      return
    }

    const recognition = new SpeechRecognition()
    recognition.lang = 'pt-BR'
    recognition.interimResults = false
    recognition.maxAlternatives = 1
    recognition.onstart = () => setOuvindo(true)
    recognition.onresult = (event) => {
      const transcript = event.results?.[0]?.[0]?.transcript ?? ''
      if (transcript) send(transcript)
    }
    recognition.onerror = (event) => {
      setVoiceError(event.error === 'not-allowed'
        ? 'Permita o acesso ao microfone para usar a voz.'
        : 'Não consegui reconhecer sua fala. Tente novamente ou escreva a mensagem.')
      setOuvindo(false)
    }
    recognition.onend = () => {
      setOuvindo(false)
      recognitionRef.current = null
    }
    recognitionRef.current = recognition
    try {
      recognition.start()
    } catch {
      recognitionRef.current = null
      setOuvindo(false)
      setVoiceError('Não foi possível iniciar o microfone. Tente novamente.')
    }
  }

  const busyInput = busy || ouvindo

  return (
    <AppShell title="Assistente IA">
      <p className="title">Assistente Vivi</p>
      <div style={{ marginTop: 20, display: 'grid', gap: 16 }} aria-live="polite">
        {msgs.map((m, i) => (
          <p key={i} className={`bubble ${m.from}`}>{m.text}</p>
        ))}
        {busy && <p className="bubble ia" role="status">Vivi está pensando…</p>}
      </div>
      <div className="chips">
        {chips.map((c) => (
          <button key={c} type="button" className="chip" disabled={busyInput} onClick={() => send(c)}>{c}</button>
        ))}
      </div>
      <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        <button
          type="button"
          className="mic"
          aria-label={ouvindo ? 'Ouvindo sua mensagem' : 'Falar com a assistente'}
          aria-pressed={ouvindo}
          disabled={busy}
          onClick={startVoice}
        >
          <Mic size={44} aria-hidden />
        </button>
        <span style={{ fontWeight: 700 }} role="status">{ouvindo ? 'Ouvindo…' : 'Toque para falar'}</span>
      </div>
      {voiceError && <p className="error" role="alert" style={{ marginTop: 12 }}>{voiceError}</p>}
      <form onSubmit={(e) => { e.preventDefault(); send(txt) }} className="composer">
        <label htmlFor="msg" className="sr-only">Escreva sua mensagem</label>
        <input id="msg" value={txt} onChange={(e) => setTxt(e.target.value)} placeholder="Ou escreva aqui…" maxLength={500} disabled={busyInput} />
        <button type="submit" disabled={busyInput}><Send size={20} aria-hidden /> {busy ? 'Enviando…' : 'Enviar'}</button>
      </form>
    </AppShell>
  )
}

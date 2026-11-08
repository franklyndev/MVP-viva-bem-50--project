import { useEffect, useMemo, useState } from 'react'
import { Plus } from 'lucide-react'
import { AppShell, BigButton } from '../components/AppShell'
import { createAppointment, getAgenda } from '../services/api'

const dias = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

function isoDate(date) {
  const copy = new Date(date)
  copy.setMinutes(copy.getMinutes() - copy.getTimezoneOffset())
  return copy.toISOString().slice(0, 10)
}

export default function Agenda() {
  const [base] = useState(() => new Date())
  const week = useMemo(() => Array.from({ length: 7 }, (_, k) => {
    const d = new Date(base)
    d.setDate(base.getDate() - base.getDay() + k)
    return d
  }), [base])
  const [sel, setSel] = useState(base.getDay())
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ title: '', type: 'CONSULTA', time: '10:00', location: '', notes: '' })

  const selectedDate = isoDate(week[sel])

  async function load(date) {
    setError('')
    setLoading(true)
    try {
      setData(await getAgenda(date))
    } catch (err) {
      setData(null)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getAgenda(selectedDate)
      .then((result) => { if (active) setData(result) })
      .catch((err) => {
        if (active) {
          setData(null)
          setError(err.message)
        }
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selectedDate])

  async function add(event) {
    event.preventDefault()
    if (saving) return
    setError('')
    setSaving(true)
    try {
      const startsAt = new Date(`${selectedDate}T${form.time}:00`).toISOString()
      await createAppointment({
        type: form.type,
        title: form.title.trim(),
        location: form.location.trim() || null,
        notes: form.notes.trim() || null,
        startsAt,
        durationMinutes: 30,
      })
      setOpen(false)
      setForm({ title: '', type: 'CONSULTA', time: '10:00', location: '', notes: '' })
      await load(selectedDate)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell title="Minha Agenda">
      <p className="title">Minha Agenda</p>
      <p className="muted">{base.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</p>
      <div className="week">
        {week.map((d, k) => (
          <button key={k} type="button" className={`day-btn ${sel === k ? 'on' : ''}`} aria-pressed={sel === k} onClick={() => {
            if (sel === k) return
            setData(null)
            setError('')
            setLoading(true)
            setSel(k)
          }}>
            <span style={{ fontSize: '0.875rem' }}>{dias[k]}</span>
            <span style={{ fontSize: '1.25rem', display: 'block' }}>{d.getDate()}</span>
          </button>
        ))}
      </div>
      <h2 style={{ marginTop: 28 }}>{sel === base.getDay() ? 'Hoje' : dias[sel]}</h2>
      {error && <p className="error" role="alert">{error}</p>}
      {loading ? <p>Carregando agenda…</p> : data && (
        <>
          <ul className="timeline">
            {data.items.map((item) => (
              <li key={item.id} className="card item">
                <span className="time" style={{ color: 'var(--primary)' }}>{item.time}</span>
                <div style={{ minWidth: 0 }}>
                  <p className="item-title">{item.title}</p>
                  <p className="muted">{item.kind}{item.detail ? ` · ${item.detail}` : ''}</p>
                </div>
              </li>
            ))}
          </ul>
          {data.items.length === 0 && <p className="muted" style={{ marginTop: 12 }}>Nada nesta data ainda.</p>}
        </>
      )}
      <BigButton variant="accent" className="mt" style={{ marginTop: 24 }} onClick={() => setOpen(!open)} disabled={saving}>
        <Plus size={24} aria-hidden /> Adicionar lembrete
      </BigButton>
      {open && (
        <form className="card login-form" style={{ marginTop: 16 }} onSubmit={add}>
          {error && <p className="error" role="alert">{error}</p>}
          <div className="field">
            <label htmlFor="title">Título</label>
            <input id="title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={120} />
          </div>
          <div className="field">
            <label htmlFor="type">Tipo</label>
            <select id="type" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              <option value="CONSULTA">Consulta</option>
              <option value="EXAME">Exame</option>
              <option value="OUTRO">Lembrete</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="time">Horário</label>
            <input id="time" type="time" value={form.time} onChange={(e) => setForm({ ...form, time: e.target.value })} required />
          </div>
          <div className="field">
            <label htmlFor="location">Local</label>
            <input id="location" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} maxLength={160} />
          </div>
          <div className="field">
            <label htmlFor="notes">Observação</label>
            <input id="notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
          <BigButton type="submit" disabled={saving}>{saving ? 'Salvando…' : 'Salvar no banco'}</BigButton>
        </form>
      )}
    </AppShell>
  )
}

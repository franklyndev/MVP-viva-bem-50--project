import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, ChevronRight, Droplet, Footprints, Pill, Stethoscope, Utensils } from 'lucide-react'
import { AppShell } from '../components/AppShell'
import { completeItem, getAgenda } from '../services/api'

const icons = {
  Lembrete: Pill,
  Atividade: Footprints,
  Consulta: Stethoscope,
  Exame: Stethoscope,
  Refeição: Utensils,
}

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Bom dia'
  if (h < 18) return 'Boa tarde'
  return 'Boa noite'
}

function firstName(name) {
  return name?.split(' ')[0] ?? ''
}

function isoDate(date) {
  const copy = new Date(date)
  copy.setMinutes(copy.getMinutes() - copy.getTimezoneOffset())
  return copy.toISOString().slice(0, 10)
}

function recentDates() {
  const today = new Date()
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - index)
    return isoDate(date)
  })
}

function dateLabel(date, today) {
  if (date === today) return `Hoje · ${new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}`
  const yesterday = new Date(`${today}T12:00:00`)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date === isoDate(yesterday)) return `Ontem · ${new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}`
  return new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' })
}

function formatLiters(value) {
  return Number(value).toFixed(2).replace(/\.?0+$/, '').replace('.', ',')
}

export default function Inicio() {
  const [data, setData] = useState(null)
  const [agendasByDate, setAgendasByDate] = useState({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setError('')
    setLoading(true)
    try {
      const dates = recentDates()
      const agendas = await Promise.all(dates.map(async (date) => {
        try {
          return await getAgenda(date)
        } catch (err) {
          if (date === dates[0]) throw err
          return null
        }
      }))
      const loaded = Object.fromEntries(agendas.filter(Boolean).map((agenda) => [agenda.date, agenda]))
      setAgendasByDate(loaded)
      setData(loaded[dates[0]])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function toggle(item, date) {
    if (saving || !data) return
    setSaving(true)
    setError('')
    try {
      await completeItem({ source: item.source, sourceId: item.sourceId, date, done: !item.done })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading && !data) return <AppShell title="Início"><p>Carregando sua rotina…</p></AppShell>
  if (!data) return (
    <AppShell title="Início">
      <p className="error" role="alert">{error || 'Não foi possível carregar sua rotina.'}</p>
      <button type="button" className="btn btn-outline" style={{ marginTop: 16 }} onClick={() => load()}>Tentar novamente</button>
    </AppShell>
  )

  const pending = data.items.filter((item) => !item.done).length
  const next = data.nextAppointment
  const dates = recentDates()
  const today = dates[0]

  return (
    <AppShell title="Início">
      <p className="title">{greeting()}, {firstName(data.user.name)}!</p>
      <p className="muted" style={{ marginTop: 4 }}>Você tem {pending} {pending === 1 ? 'tarefa' : 'tarefas'} para hoje.</p>
      {error && <p className="error" role="alert" style={{ marginTop: 12 }}>{error}</p>}

      <h2 style={{ marginTop: 28 }}>Resumo do dia</h2>
      <div className="scroll-row">
        <div className="scroll-card hero-card">
          <p style={{ fontWeight: 600, opacity: 0.9 }}>{next ? `Próxima consulta · ${next.time}` : 'Nenhuma consulta hoje'}</p>
          <p style={{ marginTop: 4, fontSize: '1.5rem', fontWeight: 800 }}>{next ? next.title : 'Agenda livre'}</p>
          <p style={{ opacity: 0.9 }}>{next?.detail || 'Quando marcar, aparece aqui.'}</p>
        </div>
        <Link to="/bem-estar" className="scroll-card card">
          <p className="item-kind"><Droplet size={20} aria-hidden /> Hidratação</p>
          <p style={{ marginTop: 4, fontSize: '1.5rem', fontWeight: 800 }}>{formatLiters(data.wellbeing.waterLiters)} L hoje</p>
          <p className="muted" style={{ display: 'flex', alignItems: 'center' }}>Ver bem-estar <ChevronRight size={20} /></p>
        </Link>
      </div>

      <h2 style={{ marginTop: 28 }}>Linha do tempo</h2>
      <p className="muted">Acompanhe seus compromissos e atividades dia a dia.</p>
      <div style={{ marginTop: 12, display: 'grid', gap: 12 }}>
        {dates.map((date, index) => {
          const agenda = agendasByDate[date]
          const items = agenda?.items ?? []
          return (
            <details key={date} className="card timeline-day" open={index === 0}>
              <summary>
                <span>{dateLabel(date, today)}</span>
                <span className="muted">{items.length ? `${items.length} ${items.length === 1 ? 'item' : 'itens'}` : 'Nenhum compromisso ou atividade'}</span>
              </summary>
              {agenda == null ? (
                <p className="muted" style={{ padding: '0 16px 16px' }}>Não foi possível carregar este dia.</p>
              ) : items.length === 0 ? (
                <p className="muted" style={{ padding: '0 16px 16px' }}>Nada programado para este dia.</p>
              ) : (
                <ol className="timeline">
                  {items.map((item) => {
                    const Icon = icons[item.kind] ?? Footprints
                    return (
                      <li key={item.id} className={`card item ${item.done ? 'done' : ''}`}>
                        <div className="time">{item.time}</div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <p className="item-kind"><Icon size={20} aria-hidden />{item.kind}</p>
                          <p className="item-title">{item.title}</p>
                          <p className="muted">{item.detail}</p>
                        </div>
                        <button type="button" className={`done-btn ${item.done ? 'on' : ''}`} aria-pressed={item.done} disabled={saving} onClick={() => toggle(item, date)}>
                          <Check size={24} aria-hidden /><span>{saving ? 'Salvando…' : item.done ? 'Feito' : 'Concluir'}</span>
                        </button>
                      </li>
                    )
                  })}
                </ol>
              )}
            </details>
          )
        })}
      </div>
    </AppShell>
  )
}

import { useEffect, useState } from 'react'
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

export default function Inicio() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function load(date) {
    setError('')
    setLoading(true)
    try {
      setData(await getAgenda(date))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getAgenda()
      .then((result) => { if (active) setData(result) })
      .catch((err) => { if (active) setError(err.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function toggle(item) {
    if (saving || !data) return
    setSaving(true)
    setError('')
    try {
      await completeItem({ source: item.source, sourceId: item.sourceId, date: data.date, done: !item.done })
      await load(data.date)
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
          <p style={{ marginTop: 4, fontSize: '1.5rem', fontWeight: 800 }}>{data.wellbeing.waterCups} de {data.wellbeing.waterGoal} copos</p>
          <p className="muted" style={{ display: 'flex', alignItems: 'center' }}>Ver bem-estar <ChevronRight size={20} /></p>
        </Link>
      </div>

      <h2 style={{ marginTop: 28 }}>Linha do tempo</h2>
      {data.items.length === 0 ? (
        <p className="muted" style={{ marginTop: 12 }}>Nada programado para hoje.</p>
      ) : (
        <ol className="timeline">
        {data.items.map((item) => {
          const Icon = icons[item.kind] ?? Footprints
          return (
            <li key={item.id} className={`card item ${item.done ? 'done' : ''}`}>
              <div className="time">{item.time}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <p className="item-kind"><Icon size={20} aria-hidden />{item.kind}</p>
                <p className="item-title">{item.title}</p>
                <p className="muted">{item.detail}</p>
              </div>
              <button type="button" className={`done-btn ${item.done ? 'on' : ''}`} aria-pressed={item.done} disabled={saving} onClick={() => toggle(item)}>
                <Check size={24} aria-hidden /><span>{saving ? 'Salvando…' : item.done ? 'Feito' : 'Concluir'}</span>
              </button>
            </li>
          )
        })}
      </ol>
      )}
    </AppShell>
  )
}

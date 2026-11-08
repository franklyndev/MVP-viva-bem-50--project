import { useEffect, useState } from 'react'
import { Droplet, Footprints, Minus, Plus } from 'lucide-react'
import { AppShell } from '../components/AppShell'
import { getWellbeing, updateWellbeing } from '../services/api'

const humores = [
  { e: '😄', l: 'Ótimo', v: 'OTIMO' },
  { e: '🙂', l: 'Bem', v: 'BEM' },
  { e: '😐', l: 'Normal', v: 'NORMAL' },
  { e: '😔', l: 'Triste', v: 'TRISTE' },
]

export default function BemEstar() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function load() {
    setError('')
    setLoading(true)
    try {
      setData(await getWellbeing())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getWellbeing()
      .then((result) => { if (active) setData(result) })
      .catch((err) => { if (active) setError(err.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function save(patch) {
    if (saving || !data) return
    setSaving(true)
    setError('')
    try {
      const next = await updateWellbeing({ date: data.date, ...patch })
      setData(next)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading && !data) return <AppShell title="Bem-Estar"><p>Carregando hábitos…</p></AppShell>
  if (!data) return (
    <AppShell title="Bem-Estar">
      <p className="error" role="alert">{error || 'Não foi possível carregar seus hábitos.'}</p>
      <button type="button" className="btn btn-outline" style={{ marginTop: 16 }} onClick={load}>Tentar novamente</button>
    </AppShell>
  )

  return (
    <AppShell title="Bem-Estar">
      <p className="title">Bem-Estar e Hábitos</p>
      {error && <p className="error" role="alert" style={{ marginTop: 12 }}>{error}</p>}
      <div style={{ marginTop: 20, display: 'grid', gap: 16 }}>
        <section className="card" style={{ padding: 20 }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Droplet color="var(--primary)" aria-hidden /> Hidratação</h2>
          <p style={{ margin: '8px 0', fontSize: '1.125rem' }}><b>{data.waterCups}</b> de {data.waterGoal} copos</p>
          <div className="progress" role="progressbar" aria-valuenow={data.waterCups} aria-valuemax={data.waterGoal}>
            <span style={{ width: `${Math.min(100, (data.waterCups / data.waterGoal) * 100)}%` }} />
          </div>
          <div style={{ display: 'flex', gap: 12, marginTop: 16 }}>
            <button type="button" className="step" aria-label="Menos um copo" disabled={saving || data.waterCups <= 0} onClick={() => save({ waterCups: Math.max(0, data.waterCups - 1) })}><Minus /></button>
            <button type="button" className="btn btn-primary" style={{ flex: 1 }} disabled={saving || data.waterCups >= 30} onClick={() => save({ waterCups: Math.min(30, data.waterCups + 1) })}>+ Bebi um copo</button>
          </div>
        </section>
        <section className="card" style={{ padding: 20 }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Footprints color="var(--primary)" aria-hidden /> Atividade física</h2>
          <p style={{ margin: '8px 0', fontSize: '1.125rem' }}><b>{data.activityMinutes}</b> de {data.activityGoal} minutos</p>
          <div className="progress" role="progressbar" aria-valuenow={data.activityMinutes} aria-valuemax={data.activityGoal}>
            <span style={{ width: `${Math.min(100, (data.activityMinutes / data.activityGoal) * 100)}%` }} />
          </div>
          <div style={{ display: 'flex', gap: 12, marginTop: 16 }}>
            <button type="button" className="step" aria-label="Menos 5 minutos" disabled={saving || data.activityMinutes <= 0} onClick={() => save({ activityMinutes: Math.max(0, data.activityMinutes - 5) })}><Minus /></button>
            <button type="button" className="step" aria-label="Mais 5 minutos" disabled={saving || data.activityMinutes >= 600} onClick={() => save({ activityMinutes: Math.min(600, data.activityMinutes + 5) })}><Plus /></button>
          </div>
        </section>
        <section className="card" style={{ padding: 20 }}>
          <h2>Como você está hoje?</h2>
          <div className="moods">
            {humores.map((h) => (
              <button key={h.v} type="button" className={`mood ${data.mood === h.v ? 'on' : ''}`} aria-pressed={data.mood === h.v} disabled={saving} onClick={() => save({ mood: h.v })}>
                <span style={{ fontSize: '1.875rem' }} aria-hidden>{h.e}</span>
                {h.l}
              </button>
            ))}
          </div>
        </section>
      </div>
    </AppShell>
  )
}

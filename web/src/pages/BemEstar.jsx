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
  const [waterInput, setWaterInput] = useState('')
  const [activityGoalInput, setActivityGoalInput] = useState('30')

  function formatLiters(value) {
    return Number(value).toFixed(2).replace(/\.?0+$/, '').replace('.', ',')
  }

  async function load() {
    setError('')
    setLoading(true)
    try {
      const result = await getWellbeing()
      setData(result)
      setWaterInput(formatLiters(result.waterLiters))
      setActivityGoalInput(String(result.activityGoal))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getWellbeing()
      .then((result) => {
        if (active) {
          setData(result)
          setWaterInput(formatLiters(result.waterLiters))
          setActivityGoalInput(String(result.activityGoal))
        }
      })
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
      if ('waterLiters' in patch || 'waterCups' in patch) setWaterInput(formatLiters(next.waterLiters))
      if ('activityGoal' in patch) setActivityGoalInput(String(next.activityGoal))
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function saveWaterInput() {
    if (!waterInput.trim()) {
      setError('Informe quantos litros você tomou hoje.')
      return
    }
    const liters = Number(waterInput.replace(',', '.'))
    if (!Number.isFinite(liters) || liters < 0 || liters > 20) {
      setError('Informe um valor entre 0 e 20 litros.')
      return
    }
    save({ waterLiters: Number(liters.toFixed(2)) })
  }

  function saveActivityGoal() {
    const goal = Number(activityGoalInput)
    if (!Number.isInteger(goal) || goal < 1 || goal > 600) {
      setError('Informe uma meta entre 1 e 600 minutos.')
      return
    }
    save({ activityGoal: goal })
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
          <p style={{ margin: '8px 0', fontSize: '1.125rem' }}><b>{formatLiters(data.waterLiters)}</b> de {formatLiters(data.waterGoalLiters)} L hoje</p>
          <div className="progress" role="progressbar" aria-valuenow={data.waterLiters} aria-valuemax={data.waterGoalLiters}>
            <span style={{ width: `${Math.min(100, (data.waterLiters / data.waterGoalLiters) * 100)}%` }} />
          </div>
          <div className="field" style={{ marginTop: 16 }}>
            <label htmlFor="water-liters">Litros tomados hoje</label>
            <input id="water-liters" type="text" inputMode="decimal" value={waterInput} disabled={saving} onChange={(event) => setWaterInput(event.target.value)} />
          </div>
          <button type="button" className="btn btn-primary" style={{ marginTop: 12 }} disabled={saving} onClick={saveWaterInput}>{saving ? 'Salvando…' : 'Salvar hidratação'}</button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 8 }}>
            <button type="button" className="step" aria-label="Diminuir 0,25 litro" disabled={saving || data.waterLiters <= 0} onClick={() => save({ waterLiters: Math.max(0, Number((data.waterLiters - 0.25).toFixed(2))) })}><Minus /></button>
            <span>Ajustar em 0,25 L</span>
            <button type="button" className="step" aria-label="Adicionar 0,25 litro" disabled={saving || data.waterLiters >= 20} onClick={() => save({ waterLiters: Math.min(20, Number((data.waterLiters + 0.25).toFixed(2))) })}><Plus /></button>
          </div>
        </section>
        <section className="card" style={{ padding: 20 }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Footprints color="var(--primary)" aria-hidden /> Atividade física</h2>
          <p style={{ margin: '8px 0', fontSize: '1.125rem' }}><b>{data.activityMinutes}</b> de {data.activityGoal} minutos</p>
          <div className="progress" role="progressbar" aria-valuenow={data.activityMinutes} aria-valuemax={data.activityGoal}>
            <span style={{ width: `${Math.min(100, (data.activityMinutes / data.activityGoal) * 100)}%` }} />
          </div>
          <div className="field" style={{ marginTop: 16 }}>
            <label htmlFor="activity-goal">Sua meta diária em minutos</label>
            <input id="activity-goal" type="number" min="1" max="600" step="1" value={activityGoalInput} disabled={saving} onChange={(event) => setActivityGoalInput(event.target.value)} />
          </div>
          <button type="button" className="btn btn-outline" style={{ marginTop: 12 }} disabled={saving} onClick={saveActivityGoal}>{saving ? 'Salvando…' : 'Salvar meta de atividade'}</button>
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

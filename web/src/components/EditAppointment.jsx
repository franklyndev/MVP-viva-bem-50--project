import { useEffect, useState } from 'react'
import { BigButton } from './AppShell'
import { getAppointment, updateAppointment } from '../services/api'

function pad(value) {
  return String(value).padStart(2, '0')
}

// startsAt vem em UTC; o formulário mostra o dia e a hora locais.
function toLocalParts(startsAt) {
  const d = new Date(startsAt)
  return {
    date: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
    time: `${pad(d.getHours())}:${pad(d.getMinutes())}`,
  }
}

/** Janela para editar uma consulta, exame ou lembrete. Chama onSaved depois de salvar. */
export function EditAppointment({ id, onClose, onSaved }) {
  const [form, setForm] = useState(null)
  const [duration, setDuration] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let active = true
    getAppointment(id)
      .then((appointment) => {
        if (!active) return
        setDuration(appointment.durationMinutes)
        setForm({
          title: appointment.title,
          type: appointment.type,
          location: appointment.location ?? '',
          notes: appointment.notes ?? '',
          ...toLocalParts(appointment.startsAt),
        })
      })
      .catch((err) => { if (active) setError(err.message) })
    return () => { active = false }
  }, [id])

  useEffect(() => {
    function onKey(event) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function save(event) {
    event.preventDefault()
    if (saving) return
    setError('')
    setSaving(true)
    try {
      await updateAppointment(id, {
        type: form.type,
        title: form.title.trim(),
        location: form.location.trim() || null,
        notes: form.notes.trim() || null,
        startsAt: new Date(`${form.date}T${form.time}:00`).toISOString(),
        durationMinutes: duration,
      })
      await onSaved()
      onClose()
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div className="card modal" role="dialog" aria-modal="true" aria-labelledby="edit-title">
        <h2 id="edit-title">Editar compromisso</h2>
        {error && <p className="error" role="alert">{error}</p>}
        {!form ? (
          !error && <p>Carregando…</p>
        ) : (
          <form className="modal-form" onSubmit={save}>
            <div className="field">
              <label htmlFor="edit-title-input">Título</label>
              <input id="edit-title-input" autoFocus value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={120} />
            </div>
            <div className="field">
              <label htmlFor="edit-type">Tipo</label>
              <select id="edit-type" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option value="CONSULTA">Consulta</option>
                <option value="EXAME">Exame</option>
                <option value="OUTRO">Lembrete</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="edit-date">Dia</label>
              <input id="edit-date" type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
            </div>
            <div className="field">
              <label htmlFor="edit-time">Horário</label>
              <input id="edit-time" type="time" value={form.time} onChange={(e) => setForm({ ...form, time: e.target.value })} required />
            </div>
            <div className="field">
              <label htmlFor="edit-location">Local</label>
              <input id="edit-location" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} maxLength={160} />
            </div>
            <div className="field">
              <label htmlFor="edit-notes">Observação</label>
              <input id="edit-notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} maxLength={2000} />
            </div>
            <BigButton type="submit" disabled={saving}>{saving ? 'Salvando…' : 'Salvar alterações'}</BigButton>
          </form>
        )}
        <BigButton type="button" variant="outline" onClick={onClose} disabled={saving}>Cancelar</BigButton>
      </div>
    </div>
  )
}

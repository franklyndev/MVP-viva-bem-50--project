import { ShieldPlus } from 'lucide-react'
import { AppShell } from '../components/AppShell'

const contacts = [
  ['Polícia Militar', '190', 'Emergências de segurança pública'],
  ['SAMU — ambulância', '192', 'Urgências e emergências médicas'],
  ['Corpo de Bombeiros', '193', 'Incêndios, acidentes e resgates'],
  ['Polícia Rodoviária Federal', '191', 'Emergências em rodovias federais'],
  ['Polícia Civil', '197', 'Orientações e denúncias'],
  ['Defesa Civil', '199', 'Desastres e áreas de risco'],
  ['Central de Atendimento à Mulher', '180', 'Orientação e atendimento à mulher'],
]

export default function Emergencia() {
  return (
    <AppShell title="Contatos de emergência">
      <p className="title">Números úteis no Brasil</p>
      <p className="muted" style={{ marginTop: 8 }}>Consulte o serviço adequado. Os números abaixo são apenas informativos.</p>
      <div style={{ marginTop: 16, display: 'grid', gap: 12 }}>
        {contacts.map(([name, number, description]) => (
          <section className="card row emergency-contact" key={number}>
            <ShieldPlus color="var(--primary)" aria-hidden />
            <div style={{ flex: 1 }}>
              <p>{name}</p>
              <p className="muted" style={{ fontSize: '0.875rem', fontWeight: 400 }}>{description}</p>
            </div>
            <span className="emergency-number">{number}</span>
          </section>
        ))}
      </div>
    </AppShell>
  )
}

import { NavLink } from 'react-router-dom'
import { CalendarDays, Home, MessageCircleHeart, Phone, User } from 'lucide-react'

const tabs = [
  { to: '/inicio', label: 'Início', icon: Home },
  { to: '/agenda', label: 'Minha Agenda', icon: CalendarDays },
  { to: '/assistente', label: 'Assistente IA', icon: MessageCircleHeart },
  { to: '/perfil', label: 'Perfil', icon: User },
]

export function AppShell({ title, children }) {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">VivaBem 50+</span>
          <a className="sos" href="tel:192"><Phone size={20} aria-hidden /> Emergência</a>
        </div>
      </header>
      <main className="page">
        <h1 className="sr-only">{title}</h1>
        {children}
      </main>
      <nav className="tabbar" aria-label="Navegação principal">
        <ul className="nav-inner">
          {tabs.map(({ to, label, icon: Icon }) => (
            <li key={to}>
              <NavLink to={to} className={({ isActive }) => (isActive ? 'active' : undefined)}>
                <Icon size={28} aria-hidden />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}

export function BigButton({ children, variant = 'primary', ...props }) {
  return <button {...props} className={`btn btn-${variant} ${props.className ?? ''}`}>{children}</button>
}

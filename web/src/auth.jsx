import { useEffect, useMemo, useState } from 'react'
import { AuthContext } from './auth-context'
import { getStoredUser, SESSION_EXPIRED_EVENT, setSession } from './services/api'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser)

  useEffect(() => {
    function expireSession() {
      setUser(null)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, expireSession)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expireSession)
  }, [])

  const value = useMemo(() => ({
    user,
    signIn(auth) {
      setSession(auth)
      setUser(auth.user)
    },
    signOut() {
      setSession(null)
      setUser(null)
    },
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

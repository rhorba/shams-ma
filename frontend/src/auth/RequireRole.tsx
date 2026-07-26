import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import { useAuth } from './AuthContext'

export default function RequireRole({
  role,
  requireMfa = false,
  children,
}: {
  role: 'HOMEOWNER' | 'INSTALLER' | 'ADMIN'
  requireMfa?: boolean
  children: ReactNode
}) {
  const { role: currentRole, mfaEnrolled } = useAuth()

  if (!currentRole) {
    return <Navigate to="/login" replace />
  }
  if (currentRole !== role) {
    return <Alert severity="error">You don't have access to this page.</Alert>
  }
  if (requireMfa && !mfaEnrolled) {
    return <Alert severity="warning">MFA enrollment is required to access this page.</Alert>
  }
  return <>{children}</>
}

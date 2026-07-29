import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material/styles'
import { render } from '@testing-library/react'
import { theme } from './theme'
import { AuthProvider } from './auth/AuthContext'

export function AllProviders({ children, initialEntries }: { children: ReactNode; initialEntries?: string[] }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>
          <AuthProvider>{children}</AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>
  )
}

export function renderWithProviders(ui: ReactElement, initialEntries?: string[]) {
  return render(<AllProviders initialEntries={initialEntries}>{ui}</AllProviders>)
}

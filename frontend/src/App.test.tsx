import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import App from './App'
import { renderWithProviders } from './test-utils'

describe('App', () => {
  it('renders the Shams.ma heading on the home route', () => {
    renderWithProviders(<App />)
    expect(screen.getByRole('heading', { level: 1, name: /shams\.ma/i })).toBeInTheDocument()
  })

  it('renders a sign-in link when logged out', () => {
    renderWithProviders(<App />)
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument()
  })
})

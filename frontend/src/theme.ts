import { createTheme } from '@mui/material/styles'

// Brand tokens per docs/ui-shams-ma.md section 2.
export const theme = createTheme({
  palette: {
    primary: { main: '#F2A93B', dark: '#C6821F' },
    secondary: { main: '#1E7A4C' },
    error: { main: '#DC2626' },
    warning: { main: '#F59E0B' },
    success: { main: '#16A34A' },
    background: { default: '#FFFFFF', paper: '#F7F5F2' },
    text: { primary: '#1A1A1A', secondary: '#666666' },
  },
  typography: {
    fontFamily: "'Inter', system-ui, sans-serif",
  },
})

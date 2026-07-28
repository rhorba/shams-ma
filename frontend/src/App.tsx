import { Link, Route, Routes } from 'react-router-dom'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { useAuth } from './auth/AuthContext'
import LoginPage from './auth/LoginPage'
import RequireRole from './auth/RequireRole'
import InstallerDashboardPage from './features/installer/InstallerDashboardPage'
import CertificationQueuePage from './features/admin/CertificationQueuePage'
import BrowsePage from './features/browse/BrowsePage'
import RoiCalculatorPage from './features/roi/RoiCalculatorPage'
import HomeownerRequestsPage from './features/homeowner/HomeownerRequestsPage'

function Nav() {
  const { role, logout } = useAuth()
  return (
    <AppBar position="static" color="default" elevation={0}>
      <Toolbar sx={{ gap: 2 }}>
        <Typography variant="h6" component={Link} to="/" sx={{ textDecoration: 'none', color: 'inherit' }}>
          Shams.ma
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        <Button component={Link} to="/roi">
          ROI Calculator
        </Button>
        <Button component={Link} to="/browse">
          Browse
        </Button>
        {role === 'HOMEOWNER' && (
          <Button component={Link} to="/homeowner/requests">
            My requests
          </Button>
        )}
        {role === 'INSTALLER' && (
          <Button component={Link} to="/installer">
            My dashboard
          </Button>
        )}
        {role === 'ADMIN' && (
          <Button component={Link} to="/admin/certifications">
            Review queue
          </Button>
        )}
        {role ? (
          <Button onClick={logout}>Sign out</Button>
        ) : (
          <Button component={Link} to="/login">
            Sign in
          </Button>
        )}
      </Toolbar>
    </AppBar>
  )
}

function Home() {
  return (
    <main style={{ padding: 24 }}>
      <h1>Shams.ma</h1>
      <p>Solar installer marketplace.</p>
    </main>
  )
}

function App() {
  return (
    <>
      <Nav />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/roi" element={<RoiCalculatorPage />} />
        <Route path="/browse" element={<BrowsePage />} />
        <Route
          path="/homeowner/requests"
          element={
            <RequireRole role="HOMEOWNER">
              <HomeownerRequestsPage />
            </RequireRole>
          }
        />
        <Route
          path="/installer"
          element={
            <RequireRole role="INSTALLER">
              <InstallerDashboardPage />
            </RequireRole>
          }
        />
        <Route
          path="/admin/certifications"
          element={
            <RequireRole role="ADMIN" requireMfa>
              <CertificationQueuePage />
            </RequireRole>
          }
        />
      </Routes>
    </>
  )
}

export default App

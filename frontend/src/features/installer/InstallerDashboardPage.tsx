import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import CoverageZoneForm from './CoverageZoneForm'
import CertificationUploadForm from './CertificationUploadForm'
import QuoteRequestInbox from './QuoteRequestInbox'

export default function InstallerDashboardPage() {
  return (
    <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Installer dashboard</Typography>
      <QuoteRequestInbox />
      <CoverageZoneForm />
      <CertificationUploadForm />
    </Stack>
  )
}

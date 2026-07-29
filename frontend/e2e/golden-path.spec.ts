import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test, expect } from '@playwright/test'

// Full-MVP golden-path walkthrough (rule 9: video recording at project version completion).
// Account creation (homeowner/installer register, admin seeding) has no frontend UI in this
// app — registration and MFA enrollment are API-only by design (see AuthService/RegisterRequest
// comments) — so setup below goes through the API directly; only user-facing screens are driven
// through the browser and captured on video.

const API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost:8098'
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const CERT_FIXTURE = path.join(__dirname, 'fixtures', 'certification.pdf')

const RUN_ID = Date.now()
const HOMEOWNER = { email: `homeowner-${RUN_ID}@example.com`, password: 'E2eDemoPass!2026' }
const INSTALLER = { email: `installer-${RUN_ID}@example.com`, password: 'E2eDemoPass!2026' }
const HOMEOWNER_NAME = `Yasmine Alaoui ${RUN_ID}`
const INSTALLER_BUSINESS_NAME = `Solaire Atlas ${RUN_ID}`
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'E2eDemoPass!2026'

test('Shams.ma golden path: onboarding, quote, booking, payment, admin review', async ({
  page,
  request,
}) => {
  test.setTimeout(120_000)

  await test.step('seed homeowner and installer accounts via the registration API', async () => {
    const homeownerRes = await request.post(`${API_BASE_URL}/api/v1/auth/register`, {
      data: {
        email: HOMEOWNER.email,
        password: HOMEOWNER.password,
        role: 'HOMEOWNER',
        fullName: HOMEOWNER_NAME,
        phone: '+212600000001',
        addressText: 'Casablanca, Morocco',
      },
    })
    expect(homeownerRes.ok()).toBeTruthy()

    const installerRes = await request.post(`${API_BASE_URL}/api/v1/auth/register`, {
      data: {
        email: INSTALLER.email,
        password: INSTALLER.password,
        role: 'INSTALLER',
        phone: '+212600000002',
        businessName: INSTALLER_BUSINESS_NAME,
      },
    })
    expect(installerRes.ok()).toBeTruthy()
  })

  await test.step('installer: log in, set coverage zone, upload certification', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(INSTALLER.email)
    await page.getByLabel('Password').fill(INSTALLER.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'My dashboard' }).click()

    await page.getByLabel('Base address').fill('Rabat, Morocco')
    await page.getByRole('button', { name: 'Save coverage zone' }).click()
    await expect(page.getByText(/Coverage zone set/i)).toBeVisible({ timeout: 15_000 })

    await page.locator('input[type="file"]').setInputFiles(CERT_FIXTURE)
    await expect(page.getByText('PENDING')).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: 'Sign out' }).click()
  })

  await test.step('admin: approve the pending certification', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(ADMIN_EMAIL!)
    await page.getByLabel('Password').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'Review queue' }).click()

    const certRow = page.getByRole('row', { name: INSTALLER_BUSINESS_NAME })
    await expect(certRow).toBeVisible({ timeout: 15_000 })
    await certRow.getByRole('button', { name: 'Approve' }).click()
    await expect(certRow).toHaveCount(0, { timeout: 15_000 })

    await page.getByRole('button', { name: 'Sign out' }).click()
  })

  await test.step('homeowner: browse, find the verified installer, request a quote', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(HOMEOWNER.email)
    await page.getByLabel('Password').fill(HOMEOWNER.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'Browse' }).click()

    await page.getByLabel('Address').fill('Rabat, Morocco')
    await page.getByRole('button', { name: 'Search' }).click()
    const installerCard = page.locator('.MuiCard-root', { hasText: INSTALLER_BUSINESS_NAME })
    await expect(installerCard).toBeVisible({ timeout: 20_000 })

    await installerCard.getByRole('button', { name: 'Request quote' }).click()
    await expect(installerCard.getByText('Quote requested')).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: 'Sign out' }).click()
  })

  await test.step('installer: respond to the lead with a quote', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(INSTALLER.email)
    await page.getByLabel('Password').fill(INSTALLER.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'My dashboard' }).click()

    await expect(page.getByText(HOMEOWNER_NAME)).toBeVisible({ timeout: 15_000 })
    await page.getByLabel('Amount (MAD)').fill('30000')
    await page.getByRole('button', { name: 'Send quote' }).click()
    await expect(page.getByText('QUOTED')).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: 'Sign out' }).click()
  })

  await test.step('homeowner: book the quote and pay the deposit', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(HOMEOWNER.email)
    await page.getByLabel('Password').fill(HOMEOWNER.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'My requests' }).click()

    await expect(page.getByText(INSTALLER_BUSINESS_NAME)).toBeVisible({ timeout: 15_000 })
    await page.getByRole('button', { name: 'Book' }).click()

    await expect(page.getByText(/Deposit due/)).toBeVisible({ timeout: 15_000 })
    await page.getByRole('button', { name: 'Proceed to payment' }).click()
    await expect(page.getByRole('button', { name: 'Simulate successful payment' })).toBeVisible({
      timeout: 15_000,
    })
    await page.getByRole('button', { name: 'Simulate successful payment' }).click()
    await expect(page.getByText(/Payment received/)).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: 'Sign out' }).click()
  })

  await test.step('admin: review the booking/payment overview', async () => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(ADMIN_EMAIL!)
    await page.getByLabel('Password').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await page.waitForURL('/')
    await page.getByRole('link', { name: 'Bookings' }).click()

    const bookingRow = page.getByRole('row', { name: HOMEOWNER_NAME })
    await expect(bookingRow).toBeVisible({ timeout: 15_000 })
    await expect(bookingRow.getByText(INSTALLER_BUSINESS_NAME)).toBeVisible()
    await expect(bookingRow.getByText('SUCCEEDED')).toBeVisible()
  })
})

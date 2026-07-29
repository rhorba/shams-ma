import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_WEB_BASE_URL ?? 'http://localhost:5173',
    video: 'on',
    trace: 'retain-on-failure',
  },
})

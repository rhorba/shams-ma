import '@testing-library/jest-dom/vitest'

// jsdom has no ResizeObserver — MUI X-Charts measures its container with one to size
// responsively. A no-op stub is enough for tests; real browsers provide the real thing.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver

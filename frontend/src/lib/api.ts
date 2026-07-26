const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json()
    return body.message ?? body.error ?? response.statusText
  } catch {
    return response.statusText
  }
}

export interface RequestOptions {
  method?: string
  body?: BodyInit
  accessToken?: string | null
  isFormData?: boolean
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: HeadersInit = {}
  if (!options.isFormData) {
    headers['Content-Type'] = 'application/json'
  }
  if (options.accessToken) {
    headers['Authorization'] = `Bearer ${options.accessToken}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body,
  })

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorMessage(response))
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }
  return (await response.json()) as T
}

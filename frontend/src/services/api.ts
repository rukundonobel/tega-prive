import axios from 'axios'

const ACCESS_TOKEN_KEY = 'tp_access_token'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

// Content-Type is intentionally not pinned here: axios sets application/json for object
// bodies automatically, and leaves multipart/form-data to the browser for FormData uploads.
export const api = axios.create({
  baseURL: `${baseURL.replace(/\/$/, '')}/api`,
  timeout: 15000,
})

// Attach the JWT access token to every request when present.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Normalise backend errors into a predictable shape for the UI.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error?.response?.data
    const normalized = {
      message: data?.message || error?.message || 'Something went wrong. Please try again.',
      errors: data?.errors,
      status: error?.response?.status,
    }
    return Promise.reject(normalized)
  },
)

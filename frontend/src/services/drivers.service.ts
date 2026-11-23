import { api } from './api'
import type { ApiResponse, Driver, DriverDocument } from '@/types'

export interface RegisterDriverPayload {
  licenceNumber: string
  licenceExpiry?: string
}

function upload(url: string, file: File, extra?: Record<string, string>) {
  const form = new FormData()
  form.append('file', file)
  if (extra) {
    Object.entries(extra).forEach(([k, v]) => form.append(k, v))
  }
  return api.post(url, form).then((r) => r.data as ApiResponse<unknown>).then((r) => r.data)
}

export const driversService = {
  register: (payload: RegisterDriverPayload) =>
    api.post<ApiResponse<Driver>>('/drivers', payload).then((r) => r.data.data),
  me: () => api.get<ApiResponse<Driver>>('/drivers/me').then((r) => r.data.data),
  update: (payload: RegisterDriverPayload) =>
    api.put<ApiResponse<Driver>>('/drivers/me', payload).then((r) => r.data.data),
  uploadPhoto: (file: File) =>
    upload('/drivers/me/photo', file) as Promise<Driver>,
  addDocument: (type: string, file: File) =>
    api
      .post<ApiResponse<DriverDocument>>('/drivers/me/documents', makeForm({ type, file }))
      .then((r) => r.data.data),
  documents: () =>
    api.get<ApiResponse<DriverDocument[]>>('/drivers/me/documents').then((r) => r.data.data),
}

function makeForm(fields: Record<string, string | File>) {
  const form = new FormData()
  Object.entries(fields).forEach(([k, v]) => form.append(k, v))
  return form
}

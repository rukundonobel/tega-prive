import { api } from './api'
import type { ApiResponse, Vehicle, VehicleDocument, VehiclePhoto } from '@/types'

export interface VehiclePayload {
  make: string
  model: string
  manufacturingYear?: number
  colour: string
  registrationNumber: string
  category: string
  seatCapacity: number
  luggageCapacity: number
  hasAc: boolean
  description?: string
}

export const vehiclesService = {
  list: () => api.get<ApiResponse<Vehicle[]>>('/vehicles').then((r) => r.data.data),
  get: (id: string) => api.get<ApiResponse<Vehicle>>(`/vehicles/${id}`).then((r) => r.data.data),
  create: (payload: VehiclePayload) =>
    api.post<ApiResponse<Vehicle>>('/vehicles', payload).then((r) => r.data.data),
  update: (id: string, payload: VehiclePayload) =>
    api.put<ApiResponse<Vehicle>>(`/vehicles/${id}`, payload).then((r) => r.data.data),
  remove: (id: string) => api.delete<ApiResponse<void>>(`/vehicles/${id}`).then((r) => r.data.data),
  addPhoto: (id: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api
      .post<ApiResponse<VehiclePhoto>>(`/vehicles/${id}/photos`, form)
      .then((r) => r.data.data)
  },
  removePhoto: (id: string, photoId: string) =>
    api.delete<ApiResponse<void>>(`/vehicles/${id}/photos/${photoId}`).then((r) => r.data.data),
  addDocument: (id: string, type: string, file: File) => {
    const form = new FormData()
    form.append('type', type)
    form.append('file', file)
    return api
      .post<ApiResponse<VehicleDocument>>(`/vehicles/${id}/documents`, form)
      .then((r) => r.data.data)
  },
  submit: (id: string) =>
    api.post<ApiResponse<Vehicle>>(`/vehicles/${id}/submit`).then((r) => r.data.data),
}

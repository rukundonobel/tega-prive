import { api } from './api'
import type { ApiResponse, Journey, JourneySearchParams, Location } from '@/types'

export const locationsService = {
  list: () => api.get<ApiResponse<Location[]>>('/locations').then((r) => r.data.data),
}

export const journeysService = {
  search: (params: JourneySearchParams) =>
    api
      .get<ApiResponse<Journey[]>>('/journeys/search', { params })
      .then((r) => r.data.data),

  get: (id: string) => api.get<ApiResponse<Journey>>(`/journeys/${id}`).then((r) => r.data.data),

  publish: (payload: PublishJourneyPayload) =>
    api.post<ApiResponse<Journey>>('/journeys', payload).then((r) => r.data.data),

  myJourneys: () => api.get<ApiResponse<Journey[]>>('/journeys/my').then((r) => r.data.data),

  start: (id: string) => api.post<ApiResponse<Journey>>(`/journeys/${id}/start`).then((r) => r.data.data),
  complete: (id: string) => api.post<ApiResponse<Journey>>(`/journeys/${id}/complete`).then((r) => r.data.data),
  cancel: (id: string) => api.post<ApiResponse<Journey>>(`/journeys/${id}/cancel`).then((r) => r.data.data),
}

export interface PublishJourneyPayload {
  vehicleId: string
  originId: string
  destinationId: string
  departureTime: string   // ISO-8601 Instant
  estimatedArrival?: string
  pricePerSeat: number
  totalSeats: number
  pickupInstructions?: string
  dropoffInstructions?: string
  notes?: string
}

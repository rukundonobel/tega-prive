import { api } from './api'
import type { ApiResponse, Booking } from '@/types'

export const bookingsService = {
  create: (journeyId: string, seatsRequested: number) =>
    api
      .post<ApiResponse<Booking>>('/bookings', { journeyId, seatsRequested })
      .then((r) => r.data.data),

  myBookings: () =>
    api.get<ApiResponse<Booking[]>>('/bookings/my').then((r) => r.data.data),

  myBooking: (id: string) =>
    api.get<ApiResponse<Booking>>(`/bookings/my/${id}`).then((r) => r.data.data),

  cancel: (id: string, reason?: string) =>
    api
      .post<ApiResponse<Booking>>(`/bookings/my/${id}/cancel`, { reason: reason ?? '' })
      .then((r) => r.data.data),

  journeyBookings: (journeyId: string) =>
    api
      .get<ApiResponse<Booking[]>>(`/bookings/journey/${journeyId}`)
      .then((r) => r.data.data),

  declineBooking: (journeyId: string, bookingId: string) =>
    api
      .post<ApiResponse<Booking>>(`/bookings/journey/${journeyId}/${bookingId}/decline`)
      .then((r) => r.data.data),
}

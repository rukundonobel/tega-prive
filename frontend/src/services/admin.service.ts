import { api } from './api'
import type {
  ApiResponse, Driver, DriverSummary, Vehicle,
  DashboardStats, BookingSummary, ActiveJourney,
  RevenueReport, UserSummary, Complaint, Review,
  PlatformSetting, Booking, DriverPayout,
} from '@/types'
import type { CommissionRule } from '@/types'

export const adminService = {
  // ── Overview ──────────────────────────────────────────────────────────────
  stats: () => api.get<ApiResponse<DashboardStats>>('/admin/stats').then((r) => r.data.data),
  recentBookings: (limit = 20) =>
    api.get<ApiResponse<BookingSummary[]>>('/admin/bookings/recent', { params: { limit } }).then((r) => r.data.data),
  activeJourneys: () =>
    api.get<ApiResponse<ActiveJourney[]>>('/admin/journeys/active').then((r) => r.data.data),

  // ── Reports ───────────────────────────────────────────────────────────────
  revenueReport: (days = 30) =>
    api.get<ApiResponse<RevenueReport>>('/admin/reports/revenue', { params: { days } }).then((r) => r.data.data),

  // ── Drivers ───────────────────────────────────────────────────────────────
  pendingDrivers: () =>
    api.get<ApiResponse<DriverSummary[]>>('/admin/drivers').then((r) => r.data.data),
  allDrivers: () =>
    api.get<ApiResponse<DriverSummary[]>>('/admin/drivers/all').then((r) => r.data.data),
  approveDriver: (id: string) =>
    api.post<ApiResponse<Driver>>(`/admin/drivers/${id}/approve`).then((r) => r.data.data),
  rejectDriver: (id: string, reason: string) =>
    api.post<ApiResponse<Driver>>(`/admin/drivers/${id}/reject`, { reason }).then((r) => r.data.data),

  // ── Vehicles ──────────────────────────────────────────────────────────────
  pendingVehicles: () =>
    api.get<ApiResponse<Vehicle[]>>('/admin/vehicles').then((r) => r.data.data),
  getVehicle: (id: string) =>
    api.get<ApiResponse<Vehicle>>(`/admin/vehicles/${id}`).then((r) => r.data.data),
  approveVehicle: (id: string) =>
    api.post<ApiResponse<Vehicle>>(`/admin/vehicles/${id}/approve`).then((r) => r.data.data),
  rejectVehicle: (id: string, reason: string) =>
    api.post<ApiResponse<Vehicle>>(`/admin/vehicles/${id}/reject`, { reason }).then((r) => r.data.data),

  // ── Users ─────────────────────────────────────────────────────────────────
  allUsers: () =>
    api.get<ApiResponse<UserSummary[]>>('/admin/users').then((r) => r.data.data),
  suspend: (id: string) =>
    api.post<ApiResponse<UserSummary>>(`/admin/users/${id}/suspend`).then((r) => r.data.data),
  activate: (id: string) =>
    api.post<ApiResponse<UserSummary>>(`/admin/users/${id}/activate`).then((r) => r.data.data),

  // ── Bookings ──────────────────────────────────────────────────────────────
  allBookings: () =>
    api.get<ApiResponse<Booking[]>>('/admin/bookings').then((r) => r.data.data),

  // ── Complaints ────────────────────────────────────────────────────────────
  complaints: () =>
    api.get<ApiResponse<Complaint[]>>('/admin/complaints').then((r) => r.data.data),
  resolveComplaint: (id: string, resolution: string) =>
    api.post<ApiResponse<Complaint>>(`/admin/complaints/${id}/resolve`, { resolution }).then((r) => r.data.data),

  // ── Reviews ───────────────────────────────────────────────────────────────
  allReviews: () =>
    api.get<ApiResponse<Review[]>>('/admin/reviews').then((r) => r.data.data),
  hideReview: (id: string) =>
    api.post<ApiResponse<Review>>(`/admin/reviews/${id}/hide`).then((r) => r.data.data),
  showReview: (id: string) =>
    api.post<ApiResponse<Review>>(`/admin/reviews/${id}/show`).then((r) => r.data.data),

  // ── Settings ──────────────────────────────────────────────────────────────
  settings: () =>
    api.get<ApiResponse<PlatformSetting[]>>('/admin/settings').then((r) => r.data.data),
  updateSetting: (key: string, value: string) =>
    api.put<ApiResponse<PlatformSetting>>(`/admin/settings/${key}`, { value }).then((r) => r.data.data),

  // ── Commissions (via /api/commissions) ───────────────────────────────────
  commissionRules: () =>
    api.get<ApiResponse<CommissionRule[]>>('/commissions/rules').then((r) => r.data.data),
  createRule: (payload: Omit<CommissionRule, 'id' | 'effectiveFrom'>) =>
    api.post<ApiResponse<CommissionRule>>('/commissions/rules', payload).then((r) => r.data.data),
  pendingPayouts: () =>
    api.get<ApiResponse<DriverPayout[]>>('/commissions/payouts/pending').then((r) => r.data.data),
  markPayoutPaid: (id: string, ref?: string) =>
    api.post<ApiResponse<DriverPayout>>(`/commissions/payouts/${id}/paid`, null, { params: { providerReference: ref ?? '' } }).then((r) => r.data.data),
}

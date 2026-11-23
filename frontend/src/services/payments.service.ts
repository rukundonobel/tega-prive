import { api } from './api'
import type { ApiResponse, Payment, CommissionRule, DriverEarning, DriverPayout } from '@/types'

export interface InitiatePaymentPayload {
  bookingId: string
  provider: 'MTN_MOMO' | 'AIRTEL' | 'CASH'
  phoneNumber: string
}

export const paymentsService = {
  initiate: (payload: InitiatePaymentPayload) =>
    api.post<ApiResponse<Payment>>('/payments/initiate', payload).then((r) => r.data.data),

  devConfirm: (paymentId: string) =>
    api.post<ApiResponse<Payment>>(`/payments/${paymentId}/dev-confirm`).then((r) => r.data.data),

  bookingPayments: (bookingId: string) =>
    api.get<ApiResponse<Payment[]>>(`/payments/booking/${bookingId}`).then((r) => r.data.data),
}

export const commissionsService = {
  currentRule: () =>
    api.get<ApiResponse<CommissionRule>>('/commissions/rules/current').then((r) => r.data.data),

  myEarnings: () =>
    api.get<ApiResponse<DriverEarning[]>>('/commissions/earnings/my').then((r) => r.data.data),

  pendingBalance: () =>
    api.get<ApiResponse<number>>('/earnings/summary').then((r) => r.data.data),

  // Admin
  ruleHistory: () =>
    api.get<ApiResponse<CommissionRule[]>>('/commissions/rules').then((r) => r.data.data),

  driverEarnings: (driverId: string) =>
    api.get<ApiResponse<DriverEarning[]>>(`/commissions/earnings/driver/${driverId}`).then((r) => r.data.data),

  pendingPayouts: () =>
    api.get<ApiResponse<DriverPayout[]>>('/commissions/payouts/pending').then((r) => r.data.data),

  initiatePayout: (payload: { driverId: string; amount: number; provider?: string; phoneNumber?: string }) =>
    api.post<ApiResponse<DriverPayout>>('/commissions/payouts', payload).then((r) => r.data.data),

  markPaid: (payoutId: string, providerReference?: string) =>
    api.post<ApiResponse<DriverPayout>>(`/commissions/payouts/${payoutId}/paid`, null, {
      params: { providerReference: providerReference ?? '' },
    }).then((r) => r.data.data),
}

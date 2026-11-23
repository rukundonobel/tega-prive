import { api } from './api'
import type { ApiResponse } from '@/types'

export interface TrackingStateDto {
  journeyId: string
  journeyStatus: string
  latitude: number | null
  longitude: number | null
  heading: number | null
  speed: number | null
  lastUpdatedAt: number  // epoch-ms; 0 if no fix yet
  fresh: boolean
}

export const trackingService = {
  getState: (journeyId: string) =>
    api
      .get<ApiResponse<TrackingStateDto>>(`/tracking/journey/${journeyId}/state`)
      .then((r) => r.data.data),
}

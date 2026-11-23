import { api } from './api'
import type { ApiResponse, AuthTokens, User } from '@/types'

export interface RegisterPayload {
  firstName: string
  lastName: string
  email: string
  phone: string
  password: string
}

export const authService = {
  login(email: string, password: string): Promise<AuthTokens> {
    return api
      .post<ApiResponse<AuthTokens>>('/auth/login', { email, password })
      .then((r) => r.data.data)
  },
  register(payload: RegisterPayload): Promise<AuthTokens> {
    return api
      .post<ApiResponse<AuthTokens>>('/auth/register', payload)
      .then((r) => r.data.data)
  },
  refresh(refreshToken: string): Promise<AuthTokens> {
    return api
      .post<ApiResponse<AuthTokens>>('/auth/refresh', { refreshToken })
      .then((r) => r.data.data)
  },
  me(): Promise<User> {
    return api.get<ApiResponse<User>>('/auth/me').then((r) => r.data.data)
  },
}

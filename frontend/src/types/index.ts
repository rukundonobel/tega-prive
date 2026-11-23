export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'DRIVER' | 'PASSENGER'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  roles: Role[]
  status: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  user: User
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errors?: Record<string, string>
}

export interface ApiError {
  message: string
  errors?: Record<string, string>
  status?: number
}

// ---- Driver ----
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type DriverDocumentType = 'IDENTITY' | 'LICENCE' | 'PROOF_OF_ADDRESS' | 'OTHER'

export interface DriverDocument {
  id: string
  type: string
  url: string
  status: string
  rejectionReason?: string
  createdAt: string
}

export interface Driver {
  id: string
  userId: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  licenceNumber?: string
  licenceExpiry?: string
  profilePhotoUrl?: string
  verificationStatus: VerificationStatus
  verified: boolean
  ratingAvg: number
  ratingCount: number
  rejectionReason?: string
}

export interface DriverSummary {
  id: string
  userId: string
  name: string
  email: string
  licenceNumber?: string
  verificationStatus: VerificationStatus
  verified: boolean
  createdAt: string
}

// ---- Vehicle ----
export type VehicleCategory = 'SEDAN' | 'SUV' | 'VAN' | 'MINIBUS' | 'LUXURY' | 'OTHER'
export type VehicleApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED'
export type VehicleDocumentType = 'REGISTRATION' | 'INSURANCE' | 'OTHER'

export interface VehiclePhoto {
  id: string
  url: string
  position: number
  isPrimary: boolean
}

export interface VehicleDocument {
  id: string
  type: string
  url: string
  status: string
  rejectionReason?: string
}

export interface Vehicle {
  id: string
  driverId: string
  driverName: string
  make: string
  model: string
  manufacturingYear?: number
  colour: string
  registrationNumber: string
  category: VehicleCategory
  seatCapacity: number
  luggageCapacity: number
  hasAc: boolean
  description?: string
  approvalStatus: VehicleApprovalStatus
  rejectionReason?: string
  photos: VehiclePhoto[]
  documents: VehicleDocument[]
}

// ---- Location ----
export interface Location {
  id: string
  name: string
  region?: string
  country: string
  active: boolean
}

// ---- Admin types ----
export interface DashboardStats {
  totalUsers: number
  totalDrivers: number
  verifiedDrivers: number
  totalVehicles: number
  approvedVehicles: number
  totalJourneys: number
  activeJourneys: number
  totalBookings: number
  confirmedBookings: number
  totalPlatformRevenue: number
  totalDriverEarnings: number
  pendingComplaints: number
  pendingPayouts: number
}

export interface BookingSummary {
  id: string
  origin: string
  destination: string
  passengerName: string
  driverName: string
  seatsBooked: number
  totalAmount: number
  status: string
  createdAt: string
}

export interface ActiveJourney {
  journeyId: string
  driverName: string
  vehicleName: string
  registrationNumber: string
  origin: string
  destination: string
  departureTime: string
  bookedSeats: number
  totalSeats: number
  latitude?: number
  longitude?: number
  lastGpsUpdate?: string
}

export interface RevenueReport {
  periodDays: number
  grossFare: number
  platformRevenue: number
  driverEarnings: number
  bookingCount: number
}

export interface UserSummary {
  id: string
  name: string
  email: string
  phone?: string
  roles: string[]
  status: string
  createdAt: string
}

export interface Complaint {
  id: string
  complainantName: string
  complainantEmail: string
  subjectType: string
  subjectId: string
  category: string
  description: string
  status: string
  resolution?: string
  createdAt: string
}

export interface Review {
  id: string
  bookingId: string
  reviewerName: string
  driverId: string
  driverName: string
  rating: number
  comment?: string
  hidden: boolean
  createdAt: string
}

export interface PlatformSetting {
  key: string
  value: string
  description?: string
}

// ---- Journey ----
export type JourneyStatus = 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export interface Journey {
  id: string
  driverId: string
  driverName: string
  driverPhotoUrl?: string
  driverVerified: boolean
  driverRating: number
  driverRatingCount: number
  vehicleId: string
  vehicleMake: string
  vehicleModel: string
  vehicleCategory: string
  vehicleSeats: number
  vehicleHasAc: boolean
  vehiclePrimaryPhotoUrl?: string
  originId: string
  originName: string
  destinationId: string
  destinationName: string
  departureTime: string   // ISO instant
  estimatedArrival?: string
  pricePerSeat: number
  totalSeats: number
  availableSeats: number
  status: JourneyStatus
  pickupInstructions?: string
  dropoffInstructions?: string
  notes?: string
  createdAt: string
}

export interface JourneySearchParams {
  originId: string
  destinationId: string
  date: string   // YYYY-MM-DD
  seats?: number
}

// ---- Payment ----
export type PaymentStatus = 'INITIATED' | 'PENDING' | 'SUCCESSFUL' | 'FAILED' | 'REFUNDED'
export type PaymentProviderType = 'MTN_MOMO' | 'AIRTEL' | 'CASH'

export interface Payment {
  id: string
  bookingId: string
  provider: PaymentProviderType
  internalReference: string
  providerReference?: string
  amount: number
  currency: string
  phoneNumber: string
  status: PaymentStatus
  verified: boolean
  failureReason?: string
  initiatedAt: string
  verifiedAt?: string
}

// ---- Commission ----
export interface CommissionRule {
  id: string
  driverCommissionPct: number
  passengerServiceFeePct: number
  minServiceFee: number
  maxServiceFee?: number
  fixedBookingFee: number
  cancellationFeePct: number
  effectiveFrom: string
  note?: string
}

export interface DriverEarning {
  id: string
  driverId: string
  driverName: string
  bookingId: string
  grossFare: number
  commission: number
  netEarnings: number
  settlementStatus: string
  createdAt: string
}

export interface DriverPayout {
  id: string
  driverId: string
  driverName: string
  amount: number
  provider?: string
  phoneNumber?: string
  status: string
  paidAt?: string
  createdAt: string
}

// ---- Booking ----
export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'PENDING_DRIVER_APPROVAL'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'REFUNDED'
  | 'EXPIRED'

export interface Booking {
  id: string
  journeyId: string
  passengerId: string
  passengerName: string
  passengerPhone?: string
  origin: string
  destination: string
  departureTime: string
  seatsBooked: number
  status: BookingStatus
  grossFare: number
  serviceFee: number
  totalAmount: number
  paymentMethod?: string
  expiresAt?: string
  confirmedAt?: string
  cancelledAt?: string
  cancelReason?: string
  createdAt: string
}


import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import PublicLayout from '@/layouts/PublicLayout'
import DriverLayout from '@/layouts/DriverLayout'
import AdminLayout from '@/layouts/AdminLayout'
import { RequireAuth, RoleRoute } from '@/routes/guards'
import { Spinner } from '@/components/ui'
import type { Role } from '@/types'

// ── Lazy page imports ─────────────────────────────────────────────────────────
// Using lazy() means a broken page shows a component error rather than a
// white screen, and each page bundle is loaded only when first visited.

const Home          = lazy(() => import('@/pages/Home'))
const Search        = lazy(() => import('@/pages/Search'))
const Login         = lazy(() => import('@/pages/Login'))
const Register      = lazy(() => import('@/pages/Register'))
const NotFound      = lazy(() => import('@/pages/NotFound'))
const JourneyDetail = lazy(() => import('@/pages/JourneyDetail'))
const Bookings      = lazy(() => import('@/pages/Bookings'))
const BookingDetail = lazy(() => import('@/pages/BookingDetail'))
const TrackJourney  = lazy(() => import('@/pages/TrackJourney'))
const PaymentPage   = lazy(() => import('@/pages/PaymentPage'))
const Notifications = lazy(() => import('@/pages/Notifications'))

const DriverDashboard = lazy(() => import('@/pages/driver/DriverDashboard'))
const DriverProfile   = lazy(() => import('@/pages/driver/DriverProfile'))
const Vehicles        = lazy(() => import('@/pages/driver/Vehicles'))
const VehicleForm     = lazy(() => import('@/pages/driver/VehicleForm'))
const VehicleDetail   = lazy(() => import('@/pages/driver/VehicleDetail'))
const DriverJourneys  = lazy(() => import('@/pages/driver/DriverJourneys'))
const PublishJourney  = lazy(() => import('@/pages/driver/PublishJourney'))
const DriverEarnings  = lazy(() => import('@/pages/driver/DriverEarnings'))
const DriverTracking  = lazy(() => import('@/pages/driver/DriverTracking'))

const AdminOverview   = lazy(() => import('@/pages/admin/AdminOverview'))
const AdminDrivers    = lazy(() => import('@/pages/admin/AdminDrivers'))
const AdminVehicles   = lazy(() => import('@/pages/admin/AdminVehicles'))
const AdminUsers      = lazy(() => import('@/pages/admin/AdminUsers'))
const AdminBookings   = lazy(() => import('@/pages/admin/AdminBookings'))
const AdminMonitoring = lazy(() => import('@/pages/admin/AdminMonitoring'))
const AdminPayments   = lazy(() => import('@/pages/admin/AdminPayments'))
const AdminReports    = lazy(() => import('@/pages/admin/AdminReports'))
const AdminComplaints = lazy(() => import('@/pages/admin/AdminComplaints'))
const AdminReviews    = lazy(() => import('@/pages/admin/AdminReviews'))
const AdminSettings   = lazy(() => import('@/pages/admin/AdminSettings'))

// ─────────────────────────────────────────────────────────────────────────────

const DRIVER_ROLES: Role[] = ['DRIVER']
const ADMIN_ROLES: Role[]  = ['ADMIN', 'SUPER_ADMIN']

function PageLoader() {
  return (
    <div className="grid min-h-screen place-items-center bg-mist">
      <Spinner className="h-8 w-8" />
    </div>
  )
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* ── Public ── */}
        <Route element={<PublicLayout />}>
          <Route path="/"              element={<Home />} />
          <Route path="/search"        element={<Search />} />
          <Route path="/login"         element={<Login />} />
          <Route path="/register"      element={<Register />} />
          <Route path="/journeys/:id"  element={<JourneyDetail />} />
        </Route>

        {/* ── Passenger (requires auth) ── */}
        <Route element={<RequireAuth><PublicLayout /></RequireAuth>}>
          <Route path="/bookings"           element={<Bookings />} />
          <Route path="/bookings/:id"       element={<BookingDetail />} />
          <Route path="/pay/:bookingId"     element={<PaymentPage />} />
          <Route path="/track/:journeyId"   element={<TrackJourney />} />
          <Route path="/notifications"      element={<Notifications />} />
        </Route>

        {/* ── Driver portal ── */}
        <Route element={<RequireAuth><DriverLayout /></RequireAuth>}>
          <Route path="/driver"                           element={<RoleRoute roles={DRIVER_ROLES}><DriverDashboard /></RoleRoute>} />
          <Route path="/driver/profile"                   element={<DriverProfile />} />
          <Route path="/driver/vehicles"                  element={<RoleRoute roles={DRIVER_ROLES}><Vehicles /></RoleRoute>} />
          <Route path="/driver/vehicles/new"              element={<RoleRoute roles={DRIVER_ROLES}><VehicleForm /></RoleRoute>} />
          <Route path="/driver/vehicles/:id"              element={<RoleRoute roles={DRIVER_ROLES}><VehicleDetail /></RoleRoute>} />
          <Route path="/driver/vehicles/:id/edit"         element={<RoleRoute roles={DRIVER_ROLES}><VehicleForm /></RoleRoute>} />
          <Route path="/driver/journeys"                  element={<RoleRoute roles={DRIVER_ROLES}><DriverJourneys /></RoleRoute>} />
          <Route path="/driver/journeys/new"              element={<RoleRoute roles={DRIVER_ROLES}><PublishJourney /></RoleRoute>} />
          <Route path="/driver/journeys/:journeyId/track" element={<RoleRoute roles={DRIVER_ROLES}><DriverTracking /></RoleRoute>} />
          <Route path="/driver/earnings"                  element={<RoleRoute roles={DRIVER_ROLES}><DriverEarnings /></RoleRoute>} />
        </Route>

        {/* ── Admin ── */}
        <Route element={<RoleRoute roles={ADMIN_ROLES}><AdminLayout /></RoleRoute>}>
          <Route path="/admin"               element={<AdminOverview />} />
          <Route path="/admin/drivers"       element={<AdminDrivers />} />
          <Route path="/admin/vehicles"      element={<AdminVehicles />} />
          <Route path="/admin/users"         element={<AdminUsers />} />
          <Route path="/admin/bookings"      element={<AdminBookings />} />
          <Route path="/admin/monitoring"    element={<AdminMonitoring />} />
          <Route path="/admin/payments"      element={<AdminPayments />} />
          <Route path="/admin/reports"       element={<AdminReports />} />
          <Route path="/admin/complaints"    element={<AdminComplaints />} />
          <Route path="/admin/reviews"       element={<AdminReviews />} />
          <Route path="/admin/settings"      element={<AdminSettings />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  )
}

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { registerSchema, type RegisterForm } from '@/validation/auth'
import { Input, Button } from '@/components/ui'
import { useAuth } from '@/context/AuthContext'
import type { ApiError } from '@/types'

export default function Register() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })

  const onSubmit = async (data: RegisterForm) => {
    setServerError(null)
    try {
      await registerUser({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: data.phone,
        password: data.password,
      })
      navigate('/')
    } catch (e) {
      setServerError((e as ApiError).message || 'Registration failed')
    }
  }

  return (
    <div className="container-page py-16">
      <div className="mx-auto max-w-md">
        <div className="card p-8">
          <h1 className="font-display text-2xl font-semibold text-forest">Create your account</h1>
          <p className="mt-1 text-sm text-ink/60">
            Book journeys, track rides and manage your travel with TEGA PRIVÉ.
          </p>

          {serverError && (
            <div className="mt-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{serverError}</div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
            <div className="grid grid-cols-2 gap-4">
              <Input label="First name" placeholder="Claudine" error={errors.firstName?.message} {...register('firstName')} />
              <Input label="Last name" placeholder="Uwase" error={errors.lastName?.message} {...register('lastName')} />
            </div>
            <Input label="Email" type="email" placeholder="you@example.com" error={errors.email?.message} {...register('email')} />
            <Input label="Phone" placeholder="+250 7XX XXX XXX" error={errors.phone?.message} {...register('phone')} />
            <Input label="Password" type="password" placeholder="••••••••" error={errors.password?.message} {...register('password')} />
            <Input
              label="Confirm password"
              type="password"
              placeholder="••••••••"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Button type="submit" loading={isSubmitting} className="w-full">
              Create account
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-ink/60">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-gold-dark hover:text-gold">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

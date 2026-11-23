import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { loginSchema, type LoginForm } from '@/validation/auth'
import { Input, Button } from '@/components/ui'
import { useAuth } from '@/context/AuthContext'
import type { ApiError } from '@/types'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  const onSubmit = async (data: LoginForm) => {
    setServerError(null)
    try {
      await login(data.email, data.password)
      navigate('/')
    } catch (e) {
      setServerError((e as ApiError).message || 'Login failed')
    }
  }

  return (
    <div className="container-page py-16">
      <div className="mx-auto max-w-md">
        <div className="card p-8">
          <h1 className="font-display text-2xl font-semibold text-forest">Welcome back</h1>
          <p className="mt-1 text-sm text-ink/60">Sign in to your TEGA PRIVÉ account.</p>

          {serverError && (
            <div className="mt-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{serverError}</div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
            <Input
              label="Email"
              type="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" loading={isSubmitting} className="w-full">
              Sign in
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-ink/60">
            New to TEGA PRIVÉ?{' '}
            <Link to="/register" className="font-medium text-gold-dark hover:text-gold">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

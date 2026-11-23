import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}
interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[TEGA PRIVÉ] Unhandled error:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh', display: 'flex', alignItems: 'center',
          justifyContent: 'center', background: '#F5F6F4', padding: '1rem',
        }}>
          <div style={{
            background: '#fff', borderRadius: '1rem', padding: '2rem',
            maxWidth: '480px', width: '100%', boxShadow: '0 4px 24px rgba(16,28,25,0.08)',
          }}>
            <h1 style={{ color: '#101C19', fontSize: '1.25rem', fontWeight: 600, marginBottom: '0.5rem' }}>
              Something went wrong
            </h1>
            <p style={{ color: '#6b7280', fontSize: '0.875rem', marginBottom: '1rem' }}>
              The app crashed. Check the browser console for the full error.
            </p>
            <pre style={{
              background: '#F5F6F4', borderRadius: '0.5rem', padding: '0.75rem',
              fontSize: '0.75rem', color: '#dc2626', overflowX: 'auto', whiteSpace: 'pre-wrap',
            }}>
              {this.state.error?.message}
            </pre>
            <button
              onClick={() => window.location.reload()}
              style={{
                marginTop: '1rem', background: '#C5A46D', color: '#101C19',
                border: 'none', borderRadius: '0.75rem', padding: '0.5rem 1.25rem',
                cursor: 'pointer', fontWeight: 600, fontSize: '0.875rem',
              }}
            >
              Reload
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

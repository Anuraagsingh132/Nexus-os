import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
  }),
}))

// Import after mocking
import LoginPage from './page'

describe('Login Page', () => {
  it('renders login form', () => {
    render(<LoginPage />)
    expect(screen.getByText(/Welcome back/i)).toBeDefined()
    expect(screen.getByText(/Log in to your Nexus OS account/i)).toBeDefined()
  })

  it('renders email and password fields', () => {
    render(<LoginPage />)
    expect(screen.getByPlaceholderText(/john@example.com/i)).toBeDefined()
    expect(screen.getByPlaceholderText(/••••••••/i)).toBeDefined()
  })

  it('renders submit button', () => {
    render(<LoginPage />)
    expect(screen.getByRole('button', { name: /Log In/i })).toBeDefined()
  })

  it('renders signup link', () => {
    render(<LoginPage />)
    expect(screen.getByText(/Sign up/i)).toBeDefined()
  })
})

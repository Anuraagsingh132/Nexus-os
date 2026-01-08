"use client"

import { useEffect } from "react"
import { Button } from "@/components/ui/button"
import { ShieldAlert } from "lucide-react"

export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="flex h-full w-full flex-col items-center justify-center space-y-4 bg-slate-50 dark:bg-slate-900 p-8 text-center">
      <ShieldAlert className="h-12 w-12 text-red-500" />
      <h2 className="text-xl font-bold">Something went wrong!</h2>
      <p className="text-sm text-slate-500 max-w-md">
        We&apos;ve encountered an unexpected error. Please try again or contact support if the issue persists.
      </p>
      <Button onClick={() => reset()} variant="default">
        Try again
      </Button>
    </div>
  )
}

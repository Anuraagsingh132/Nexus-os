"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

interface Workspace { id: string; name: string; slug: string; }

export default function WorkspacesPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const router = useRouter()

  useEffect(() => {
    async function loadWorkspaces() {
      try {
        const res = await fetch("/api/v1/workspaces", { credentials: "include" })
        if (!res.ok) {
          if (res.status === 401 || res.status === 403) {
            router.push("/login")
            return
          }
          throw new Error("Failed to load workspaces")
        }
        const data = await res.json()
        setWorkspaces(data)
      } catch (err: unknown) {
        if (err instanceof Error) {
          setError(err.message)
        } else {
          setError(String(err))
        }
      } finally {
        setLoading(false)
      }
    }
    loadWorkspaces()
  }, [router])

  if (loading) {
    return <div className="flex h-screen items-center justify-center">Loading workspaces...</div>
  }

  return (
    <div className="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-900">
      <Card className="w-[500px]">
        <CardHeader>
          <CardTitle>Select a Workspace</CardTitle>
          <CardDescription>Choose a workspace to continue</CardDescription>
        </CardHeader>
        <CardContent>
          {error && <div className="text-red-500 mb-4">{error}</div>}
          
          {workspaces.length === 0 ? (
            <div className="text-center py-6">
              <p className="text-slate-500 mb-4">You don&apos;t belong to any workspaces yet.</p>
              <Button onClick={() => router.push("/workspaces/new")}>Create Workspace</Button>
            </div>
          ) : (
            <div className="space-y-2">
              {workspaces.map(w => (
                <Button 
                  key={w.id} 
                  variant="outline" 
                  className="w-full justify-start text-left"
                  onClick={() => {
                    localStorage.setItem("workspaceId", w.id)
                    router.push(`/dashboard`)
                  }}
                >
                  <div className="font-semibold">{w.name}</div>
                </Button>
              ))}
              <div className="pt-4 border-t mt-4">
                <Button variant="ghost" className="w-full" onClick={() => router.push("/workspaces/new")}>
                  Create New Workspace
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

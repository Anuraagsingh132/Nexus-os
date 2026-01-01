"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { apiFetch } from "@/lib/api"
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export default function NewWorkspacePage() {
  const [name, setName] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const router = useRouter()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return

    setLoading(true)
    setError("")

    try {
      const res = await apiFetch("/api/v1/workspaces", {
        method: "POST",
        headers: { 
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ name })
      })

      if (!res.ok) {
        throw new Error("Failed to create workspace")
      }

      const data = await res.json()
      if (typeof window !== "undefined") {
        localStorage.setItem("workspaceId", data.id)
      }

      router.push("/dashboard")
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

  return (
    <div className="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-900">
      <Card className="w-[400px]">
        <CardHeader>
          <CardTitle>Create Workspace</CardTitle>
          <CardDescription>Give your new workspace a name</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-sm font-medium">Workspace Name</label>
              <Input 
                name="name"
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder="Engineering Team"
                disabled={loading}
                required
                className="mt-1"
              />
            </div>
            {error && <div className="text-sm text-red-500">{error}</div>}
            <div className="flex gap-2">
              <Button type="button" variant="outline" className="flex-1" onClick={() => router.push("/workspaces")} disabled={loading}>Cancel</Button>
              <Button type="submit" className="flex-1" disabled={loading}>Create Workspace</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

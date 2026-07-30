"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { apiFetch } from "@/lib/api"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export default function NewProjectPage() {
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const router = useRouter()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return

    const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") : null
    if (!workspaceId) {
      setError("No workspace selected")
      return
    }

    setLoading(true)
    setError("")

    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/projects`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description })
      })

      if (!res.ok) {
        throw new Error("Failed to create project")
      }

      const data = await res.json()
      router.push(`/dashboard/projects/${data.id}`)
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
    <div className="p-8 max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>Create New Project</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-sm font-medium">Project Name</label>
              <Input 
                name="name"
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder="Marketing Campaign"
                disabled={loading}
                required
                className="mt-1"
              />
            </div>
            <div>
              <label className="text-sm font-medium">Description</label>
              <Input 
                name="description"
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Optional description"
                disabled={loading}
                className="mt-1"
              />
            </div>
            {error && <div className="text-sm text-red-500">{error}</div>}
            <div className="flex gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => router.push("/dashboard/projects")} disabled={loading}>Cancel</Button>
              <Button type="submit" disabled={loading}>Create Project</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

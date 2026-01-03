"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

interface Project {
  id: string
  name: string
  description: string
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    const workspaceId = localStorage.getItem("workspaceId")
    if (!workspaceId) {
      setLoading(false)
      return
    }

    fetch(`/api/v1/workspaces/${workspaceId}/projects`, { credentials: "include" })
      .then(res => res.json())
      .then(data => setProjects(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="p-8">Loading projects...</div>

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Projects</h1>
        <Button onClick={() => router.push("/dashboard/projects/new")}>New Project</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {projects.length === 0 && (
          <p className="text-slate-500">No projects yet.</p>
        )}
        {projects.map(p => (
          <Card key={p.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => router.push(`/dashboard/projects/${p.id}`)}>
            <CardHeader>
              <CardTitle className="text-lg">{p.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-slate-500 text-sm">{p.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}

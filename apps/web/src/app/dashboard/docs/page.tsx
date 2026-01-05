"use client"

import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { FileText, Plus, Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"

type Doc = {
  id: string
  title: string
  content: string
  updatedAt: string
}

export default function DocumentsPage() {
  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""
  const queryClient = useQueryClient()

  const { data: docs = [], isLoading, error } = useQuery<Doc[]>({
    queryKey: ["docs"],
    queryFn: async () => {
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/documents`)
      if (!res.ok) throw new Error("Failed to fetch docs")
      return res.json()
    },
  })

  const createDoc = useMutation({
    mutationFn: async (newDoc: Partial<Doc>) => {
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/documents`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newDoc),
      })
      if (!res.ok) throw new Error("Failed to create doc")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["docs"] })
    },
  })

  const handleCreateDoc = () => {
    createDoc.mutate({ title: "New Document", content: "## Heading\nStart typing here..." })
  }

  return (
    <div className="flex flex-col h-full bg-white dark:bg-slate-950">
      <header className="px-6 py-4 border-b flex items-center justify-between">
        <h1 className="text-2xl font-bold">Documents</h1>
        <Button onClick={handleCreateDoc} disabled={createDoc.isPending}>
          <Plus className="w-4 h-4 mr-2" /> New Document
        </Button>
      </header>
      
      <div className="p-6">
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
            <Input placeholder="Search documents..." className="pl-10" />
          </div>
          
          {isLoading ? (
            <div className="text-center text-slate-500 py-10">Loading documents...</div>
          ) : error ? (
            <div className="text-center text-destructive py-10">Error loading documents: {(error as Error).message}</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {docs.map(doc => (
                <Link key={doc.id} href={`/dashboard/docs/${doc.id}`}>
                  <Card className="cursor-pointer hover:border-indigo-500 transition-colors h-full">
                    <CardHeader className="flex flex-row items-center gap-3 pb-2">
                      <FileText className="w-6 h-6 text-indigo-500" />
                      <CardTitle className="text-base">{doc.title}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <p className="text-sm text-slate-500">
                        Updated {doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString() : "recently"}
                      </p>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

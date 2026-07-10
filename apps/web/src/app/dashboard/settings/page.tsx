"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { apiFetch } from "@/lib/api"

export default function SettingsPage() {
  const [fullName, setFullName] = useState("Admin User")
  const [email, setEmail] = useState("admin@nexusos.dev")

  const [aiMode, setAiMode] = useState("RAG")
  const [provider, setProvider] = useState("Ollama")
  const [apiKey, setApiKey] = useState("")
  const [modelName, setModelName] = useState("")
  const [loadingAi, setLoadingAi] = useState(true)

  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""

  useEffect(() => {
    if (workspaceId) {
      apiFetch(`/api/v1/workspaces/${workspaceId}/agent/settings`)
        .then(res => {
          if (!res.ok) throw new Error("Failed to fetch AI settings")
          return res.json()
        })
        .then(data => {
          setAiMode(data.mode || "RAG")
          setProvider(data.provider || "Ollama")
          setApiKey(data.apiKey || "")
          setModelName(data.modelName || "")
          setLoadingAi(false)
        })
        .catch(err => {
          console.error("Failed to fetch AI settings", err)
          setLoadingAi(false)
        })
    } else {
      setLoadingAi(false)
    }
  }, [workspaceId])

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault()
    alert("Profile saved!")
  }

  const handleSaveAI = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!workspaceId) return
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/agent/settings`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mode: aiMode, provider, apiKey, modelName })
      })
      if (!res.ok) throw new Error("Update failed")
      alert("AI Settings saved!")
    } catch (err) {
      console.error(err)
      alert("Failed to save AI settings.")
    }
  }

  return (
    <div className="flex flex-col h-full bg-slate-50 dark:bg-slate-900">
      <header className="px-6 py-4 border-b bg-white dark:bg-slate-950 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Settings</h1>
          <p className="text-sm text-slate-500">Manage your profile and preferences</p>
        </div>
      </header>
      
      <div className="flex-1 p-6 overflow-y-auto">
        <div className="max-w-3xl mx-auto space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Profile Settings</CardTitle>
              <CardDescription>Update your personal information</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSave} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="fullName">Full Name</Label>
                  <Input 
                    id="fullName" 
                    value={fullName} 
                    onChange={(e) => setFullName(e.target.value)} 
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email Address</Label>
                  <Input 
                    id="email" 
                    type="email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                    disabled
                  />
                  <p className="text-xs text-slate-500">Email cannot be changed currently.</p>
                </div>
                <Button type="submit">Save Changes</Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>AI Agent & Provider Settings</CardTitle>
              <CardDescription>Configure how your AI works in this workspace</CardDescription>
            </CardHeader>
            <CardContent>
              {loadingAi ? (
                <div className="text-sm text-slate-500">Loading AI settings...</div>
              ) : (
                <form onSubmit={handleSaveAI} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="aiMode">Mode</Label>
                    <select 
                      id="aiMode" 
                      value={aiMode} 
                      onChange={(e) => setAiMode(e.target.value)}
                      className="flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-950 focus-visible:ring-offset-2 dark:border-slate-800 dark:bg-slate-950 dark:ring-offset-slate-950"
                    >
                      <option value="RAG">RAG Q&A Only (Passive Assistant)</option>
                      <option value="Agent">Full Agent Co-Pilot (Answers & Autonomous Actions)</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="provider">Primary Provider</Label>
                    <select 
                      id="provider" 
                      value={provider} 
                      onChange={(e) => setProvider(e.target.value)}
                      className="flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-950 focus-visible:ring-offset-2 dark:border-slate-800 dark:bg-slate-950 dark:ring-offset-slate-950"
                    >
                      <option value="Ollama">Ollama (Local)</option>
                      <option value="Google Gemini">Google Gemini</option>
                      <option value="Groq">Groq</option>
                      <option value="Cerebras">Cerebras</option>
                      <option value="OpenRouter">OpenRouter</option>
                      <option value="OpenAI">OpenAI</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="apiKey">API Key (if applicable)</Label>
                    <Input 
                      id="apiKey" 
                      type="password" 
                      value={apiKey} 
                      onChange={(e) => setApiKey(e.target.value)} 
                      placeholder="sk-..."
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="modelName">Model Name</Label>
                    <Input 
                      id="modelName" 
                      type="text" 
                      value={modelName} 
                      onChange={(e) => setModelName(e.target.value)} 
                      placeholder="e.g., llama3, gemini-1.5-pro, gpt-4o"
                    />
                  </div>
                  <Button type="submit">Save AI Settings</Button>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

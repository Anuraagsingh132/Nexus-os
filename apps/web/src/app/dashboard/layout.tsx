"use client"

import Link from "next/link"
import { Folder, Inbox, Settings, MessageSquare, FileText, ShieldAlert } from "lucide-react"
import { useEffect, useState } from "react"
import { NotificationBell } from "@/components/layout/NotificationBell"
import { InviteTeamMember } from "@/components/layout/InviteTeamMember"
import { GlobalSearch } from "@/components/layout/GlobalSearch"

type Project = {
  id: string
  name: string
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const [workspaceName, setWorkspaceName] = useState("Loading...")
  const [workspaceId, setWorkspaceId] = useState<string | null>(null)
  const [projects, setProjects] = useState<Project[]>([])

  useEffect(() => {
    fetch("/api/v1/workspaces", { credentials: "include" })
      .then(res => res.json())
      .then(data => {
        if (data && data.length > 0) {
          const latest = data[data.length - 1]
          setWorkspaceName(latest.name)
          setWorkspaceId(latest.id)
          if (typeof window !== "undefined") {
            localStorage.setItem("workspaceId", latest.id)
          }
          // Fetch projects for this workspace
          fetch(`/api/v1/workspaces/${latest.id}/projects`, { credentials: "include" })
            .then(res => res.ok ? res.json() : [])
            .then(projectData => {
              if (Array.isArray(projectData)) {
                setProjects(projectData)
              }
            })
            .catch(console.error)
        } else {
          setWorkspaceName("No Workspace")
        }
      })
      .catch(() => setWorkspaceName("Error"))
  }, [])

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-900">
      <aside className="w-64 border-r border-slate-200/60 dark:border-slate-800 bg-white/80 dark:bg-slate-950/80 backdrop-blur-xl flex flex-col z-20">
        <div className="p-4 border-b border-slate-200/60 dark:border-slate-800">
          <h2 className="font-semibold text-lg text-slate-900 dark:text-slate-100 truncate">{workspaceName}</h2>
          <p className="text-sm text-slate-500">Workspace</p>
        </div>
        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          <Link href="/dashboard" className="flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md bg-slate-100 text-slate-900 dark:bg-slate-800 dark:text-white transition-colors">
            <Inbox className="w-4 h-4" /> Inbox
          </Link>
          
          <div className="pt-4 pb-2 flex items-center justify-between">
            <Link href="/dashboard/projects" className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wider hover:text-slate-900 dark:hover:text-slate-300 transition-colors">Projects</Link>
          </div>
          {projects.length > 0 ? (
            projects.map((project) => (
              <Link key={project.id} href={`/dashboard/projects/${project.id}`} className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
                <Folder className="w-4 h-4" /> {project.name}
              </Link>
            ))
          ) : (
            <p className="px-3 py-2 text-xs text-slate-400">No projects yet</p>
          )}
          
          <div className="pt-4 pb-2">
            <p className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Chat</p>
          </div>
          <Link href="/dashboard/chat/general" className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
            <MessageSquare className="w-4 h-4" /> #general
          </Link>

          <div className="pt-4 pb-2">
            <p className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Knowledge</p>
          </div>
          <Link href="/dashboard/docs" className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
            <FileText className="w-4 h-4" /> Documents
          </Link>
          <Link href="/dashboard/files" className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
            <Folder className="w-4 h-4" /> Files
          </Link>
        </nav>
        <div className="p-4 border-t border-slate-200/60 dark:border-slate-800 space-y-1">
          <Link href="/dashboard/admin" className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
            <ShieldAlert className="w-4 h-4" /> Admin
          </Link>
          <Link href="/dashboard/settings" className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-200 transition-colors">
            <Settings className="w-4 h-4" /> Settings
          </Link>
        </div>
      </aside>
      <main className="flex-1 flex flex-col relative z-10 overflow-hidden bg-slate-50 dark:bg-slate-900/50">
        <header className="h-14 flex items-center justify-between px-4 border-b border-slate-200/60 dark:border-slate-800/60 bg-white/70 dark:bg-slate-950/70 backdrop-blur-md sticky top-0 z-30 gap-4">
          <div className="flex-1 flex justify-center">
            {workspaceId && <GlobalSearch workspaceId={workspaceId} />}
          </div>
          <div className="flex items-center gap-4">
            {workspaceId && <InviteTeamMember workspaceId={workspaceId} />}
            <NotificationBell />
          </div>
        </header>
        <div className="flex-1 overflow-auto">
          {children}
        </div>
      </main>
    </div>
  )
}

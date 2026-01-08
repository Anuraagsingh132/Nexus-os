"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Activity, CheckCircle2, Users, FileText, Send } from "lucide-react"

type ActivityItem = {
  title: string
  description: string
  timestamp: string
}

export default function DashboardOverview() {
  const router = useRouter()
  const [statsData, setStatsData] = useState({
    activeProjects: 0,
    tasksCompleted: 0,
    teamMembers: 0,
    documents: 0
  })
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [activitiesLoading, setActivitiesLoading] = useState(true)
  const [aiQuery, setAiQuery] = useState("")

  useEffect(() => {
    fetch('/api/v1/admin/stats', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (data && !data.error) {
          setStatsData(data)
        }
      })
      .catch(console.error)

    fetch('/api/v1/admin/activity', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error("Failed to fetch activity")
        return res.json()
      })
      .then(data => {
        if (Array.isArray(data)) {
          setActivities(data)
        }
      })
      .catch(console.error)
      .finally(() => setActivitiesLoading(false))
  }, [])

  const handleAiSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = aiQuery.trim()
    if (!trimmed) return
    router.push(`/dashboard/chat/general?query=${encodeURIComponent(trimmed)}`)
  }

  const stats = [
    { title: "Active Projects", value: statsData.activeProjects.toString(), icon: Activity, trend: "Current total" },
    { title: "Tasks", value: statsData.tasksCompleted.toString(), icon: CheckCircle2, trend: "Current total" },
    { title: "Team Members", value: statsData.teamMembers.toString(), icon: Users, trend: "Current total" },
    { title: "Documents", value: statsData.documents.toString(), icon: FileText, trend: "Current total" },
  ]

  return (
    <div className="p-6 h-full overflow-y-auto bg-slate-50 dark:bg-slate-900">
      <div className="max-w-6xl mx-auto space-y-6">
        <header>
          <h1 className="text-3xl font-bold tracking-tight">Overview</h1>
          <p className="text-slate-500 mt-1">Here&apos;s what&apos;s happening in your workspace today.</p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {stats.map((stat, i) => {
            const Icon = stat.icon
            return (
              <Card key={i}>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium text-slate-600 dark:text-slate-300">
                    {stat.title}
                  </CardTitle>
                  <Icon className="w-4 h-4 text-slate-400" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                  <p className="text-xs text-slate-500 mt-1">{stat.trend}</p>
                </CardContent>
              </Card>
            )
          })}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="col-span-1">
            <CardHeader>
              <CardTitle>Recent Activity</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {activitiesLoading ? (
                  <p className="text-sm text-slate-400">Loading activity...</p>
                ) : activities.length === 0 ? (
                  <p className="text-sm text-slate-400">No recent activity</p>
                ) : (
                  activities.map((item, i) => (
                    <div key={i} className="flex items-start gap-4">
                      <div className="w-2 h-2 mt-2 rounded-full bg-indigo-500 shrink-0" />
                      <div>
                        <p className="text-sm text-slate-800 dark:text-slate-200">
                          <span className="font-semibold">{item.title}</span>{" "}
                          {item.description}
                        </p>
                        <p className="text-xs text-slate-500">
                          {item.timestamp ? new Date(item.timestamp).toLocaleString() : ""}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
          
          <Card className="col-span-1">
            <CardHeader>
              <CardTitle>Workspace AI</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col items-center justify-center text-center p-6 bg-gradient-to-b from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-950 border rounded-lg">
              <div className="w-12 h-12 bg-indigo-100 dark:bg-indigo-900 text-indigo-600 dark:text-indigo-400 rounded-full flex items-center justify-center mb-4">
                <span className="font-bold">AI</span>
              </div>
              <h3 className="font-semibold mb-2">Ask Nexus AI</h3>
              <p className="text-sm text-slate-500 mb-4">Search across all your projects, documents, and chat history using natural language.</p>
              <form onSubmit={handleAiSubmit} className="w-full">
                <div className="w-full bg-white dark:bg-slate-900 border rounded-md p-1 flex items-center shadow-sm focus-within:ring-2 focus-within:ring-indigo-500 transition-shadow">
                  <input
                    type="text"
                    value={aiQuery}
                    onChange={(e) => setAiQuery(e.target.value)}
                    placeholder="Ask anything..."
                    className="flex-1 text-sm ml-2 bg-transparent border-none outline-none text-slate-800 dark:text-slate-200 placeholder:text-slate-400"
                  />
                  <button
                    type="submit"
                    className="p-2 rounded-md hover:bg-indigo-50 dark:hover:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 transition-colors disabled:opacity-40"
                    disabled={!aiQuery.trim()}
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

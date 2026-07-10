"use client"

import { useEffect, useState } from "react"
import { apiFetch } from "@/lib/api"
import { Bot, CheckCircle, XCircle, Clock, AlertTriangle, ShieldQuestion, Check, X, Loader2 } from "lucide-react"

type AgentActivityStatus = 'SUCCESS' | 'FAILED' | 'PENDING_CONFIRMATION' | 'CANCELLED' | 'EXPIRED'

interface AgentActivity {
  id: string
  workspaceId: string
  requesterId: string
  requesterName?: string
  toolName: string
  toolArgs: Record<string, unknown>
  status: AgentActivityStatus
  resultSummary?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export default function AgentActivityPage() {
  const [activities, setActivities] = useState<AgentActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<AgentActivityStatus | 'ALL'>('ALL')
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""

  const fetchActivities = async () => {
    if (!workspaceId) return
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/agent/activities`)
      if (res.ok) {
        const data = await res.json()
        setActivities(data)
      }
    } catch (error) {
      console.error("Failed to fetch activities:", error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchActivities()
    const interval = setInterval(fetchActivities, 10000)
    return () => clearInterval(interval)
  }, [workspaceId])

  const handleAction = async (id: string, action: 'confirm' | 'cancel') => {
    setActionLoading(id)
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/agent/activities/${id}/${action}`, {
        method: "POST"
      })
      if (res.ok) {
        await fetchActivities()
      }
    } catch (error) {
      console.error(`Failed to ${action} activity:`, error)
    } finally {
      setActionLoading(null)
    }
  }

  const filteredActivities = activities.filter(a => filter === 'ALL' || a.status === filter)

  const StatusIcon = ({ status }: { status: AgentActivityStatus }) => {
    switch (status) {
      case 'SUCCESS': return <CheckCircle className="w-5 h-5 text-emerald-500" />
      case 'FAILED': return <AlertTriangle className="w-5 h-5 text-red-500" />
      case 'PENDING_CONFIRMATION': return <ShieldQuestion className="w-5 h-5 text-amber-500" />
      case 'CANCELLED': return <XCircle className="w-5 h-5 text-slate-400" />
      case 'EXPIRED': return <Clock className="w-5 h-5 text-slate-400" />
      default: return <Clock className="w-5 h-5 text-slate-400" />
    }
  }

  const getStatusBadgeColor = (status: AgentActivityStatus) => {
    switch (status) {
      case 'SUCCESS': return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800/30'
      case 'FAILED': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300 border-red-200 dark:border-red-800/30'
      case 'PENDING_CONFIRMATION': return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300 border-amber-200 dark:border-amber-800/30 animate-pulse'
      case 'CANCELLED': return 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300 border-slate-200 dark:border-slate-700'
      case 'EXPIRED': return 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300 border-slate-200 dark:border-slate-700'
      default: return 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300 border-slate-200 dark:border-slate-700'
    }
  }

  return (
    <div className="flex flex-col h-full bg-slate-50 dark:bg-slate-900">
      <header className="px-8 py-6 border-b border-slate-200/60 dark:border-slate-800/60 bg-white/70 dark:bg-slate-950/70 backdrop-blur-md sticky top-0 z-10">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-3 text-slate-900 dark:text-white">
              <Bot className="w-8 h-8 text-indigo-500" /> Agent Activity
            </h1>
            <p className="text-slate-500 mt-1">Monitor and manage autonomous agent actions within this workspace.</p>
          </div>
          
          <div className="flex items-center gap-2 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg">
            {(['ALL', 'PENDING_CONFIRMATION', 'SUCCESS', 'FAILED', 'CANCELLED'] as const).map(f => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all ${
                  filter === f 
                  ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm' 
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {f === 'ALL' ? 'All' : f === 'PENDING_CONFIRMATION' ? 'Pending' : f.charAt(0) + f.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </header>
      
      <div className="flex-1 overflow-y-auto p-8">
        <div className="max-w-5xl mx-auto space-y-4">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="w-8 h-8 text-indigo-500 animate-spin" />
            </div>
          ) : filteredActivities.length === 0 ? (
            <div className="bg-white/50 dark:bg-slate-900/50 backdrop-blur-xl border border-slate-200/60 dark:border-slate-800/60 rounded-2xl p-12 text-center shadow-sm">
              <Bot className="w-16 h-16 text-slate-300 dark:text-slate-700 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-2">No activities found</h3>
              <p className="text-slate-500">The agent hasn&apos;t performed any actions matching the current filter.</p>
            </div>
          ) : (
            filteredActivities.map(activity => (
              <div 
                key={activity.id} 
                className="bg-white dark:bg-slate-900 border border-slate-200/60 dark:border-slate-800/60 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-shadow"
              >
                <div className="p-5 flex items-start gap-4">
                  <div className="mt-1 flex-shrink-0">
                    <StatusIcon status={activity.status} />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <h3 className="text-lg font-semibold text-slate-900 dark:text-white truncate">
                        {activity.toolName}
                      </h3>
                      <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${getStatusBadgeColor(activity.status)}`}>
                        {activity.status.replace('_', ' ')}
                      </span>
                    </div>
                    
                    <div className="text-sm text-slate-500 mb-3 flex items-center gap-2">
                      <span>Requested by <span className="font-medium text-slate-700 dark:text-slate-300">{activity.requesterName || activity.requesterId}</span></span>
                      <span>•</span>
                      <span>{new Date(activity.createdAt).toLocaleString()}</span>
                    </div>
                    
                    {activity.resultSummary && (
                      <div className="mb-3 text-sm text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50 p-3 rounded-lg border border-slate-100 dark:border-slate-800">
                        {activity.resultSummary}
                      </div>
                    )}
                    
                    {activity.errorMessage && (
                      <div className="mb-3 text-sm text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-3 rounded-lg border border-red-100 dark:border-red-900/30">
                        {activity.errorMessage}
                      </div>
                    )}
                    
                    <div className="bg-slate-50 dark:bg-slate-950 rounded-lg p-3 overflow-x-auto border border-slate-100 dark:border-slate-800">
                      <pre className="text-xs text-slate-600 dark:text-slate-400 font-mono">
                        {JSON.stringify(activity.toolArgs, null, 2)}
                      </pre>
                    </div>
                  </div>
                </div>
                
                {activity.status === 'PENDING_CONFIRMATION' && (
                  <div className="bg-amber-50/50 dark:bg-amber-900/10 px-5 py-4 border-t border-amber-100/50 dark:border-amber-900/20 flex justify-end gap-3">
                    <button
                      onClick={() => handleAction(activity.id, 'cancel')}
                      disabled={actionLoading === activity.id}
                      className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors flex items-center gap-2 disabled:opacity-50"
                    >
                      <X className="w-4 h-4" /> Cancel
                    </button>
                    <button
                      onClick={() => handleAction(activity.id, 'confirm')}
                      disabled={actionLoading === activity.id}
                      className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-sm"
                    >
                      {actionLoading === activity.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />} 
                      Confirm Execution
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

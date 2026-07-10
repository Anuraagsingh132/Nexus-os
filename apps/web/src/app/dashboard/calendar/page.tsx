"use client"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Calendar as CalendarIcon, Video, Plus } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"

import { apiFetch } from "@/lib/api"

type Meeting = {
  id: string
  title: string
  startTime: string
  endTime: string
  videoUrl: string
}

export default function CalendarPage() {
  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""
  const queryClient = useQueryClient()

  const { data: meetings = [], isLoading, error } = useQuery<Meeting[]>({
    queryKey: ["meetings"],
    queryFn: async () => {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/meetings`)
      if (!res.ok) throw new Error("Failed to fetch meetings")
      return res.json()
    },
    enabled: !!workspaceId
  })

  const createMeeting = useMutation({
    mutationFn: async (newMeeting: Partial<Meeting>) => {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/meetings`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newMeeting),
      })
      if (!res.ok) throw new Error("Failed to create meeting")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["meetings"] })
    },
  })

  const handleSchedule = () => {
    createMeeting.mutate({
      title: "New Meeting",
      startTime: new Date().toISOString(),
      endTime: new Date(Date.now() + 3600000).toISOString(),
      videoUrl: "https://zoom.us/j/demo"
    })
  }

  return (
    <div className="flex flex-col h-full bg-white dark:bg-slate-950">
      <header className="px-6 py-4 border-b flex items-center justify-between">
        <h1 className="text-2xl font-bold">Calendar</h1>
        <Button onClick={handleSchedule} disabled={createMeeting.isPending}>
          <Plus className="w-4 h-4 mr-2" /> Schedule Meeting
        </Button>
      </header>
      
      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1 p-6 border-r flex items-center justify-center bg-slate-50 dark:bg-slate-900">
          <div className="text-center text-slate-500">
            <CalendarIcon className="w-16 h-16 mx-auto mb-4 text-slate-300" />
            <h3 className="text-lg font-medium text-slate-700 dark:text-slate-200">No events today</h3>
            <p className="mt-1">Enjoy your free time!</p>
          </div>
        </div>
        
        <aside className="w-80 bg-white dark:bg-slate-950 p-6 overflow-y-auto">
          <h2 className="font-semibold text-sm uppercase text-slate-500 tracking-wider mb-4">Upcoming</h2>
          <div className="space-y-4">
            {isLoading ? (
              <div className="text-sm text-slate-500">Loading meetings...</div>
            ) : error ? (
              <div className="text-sm text-destructive">Error: {(error as Error).message}</div>
            ) : meetings.length === 0 ? (
              <div className="text-sm text-slate-500">No upcoming meetings.</div>
            ) : meetings.map(meeting => (
              <Card key={meeting.id}>
                <CardContent className="p-4">
                  <h4 className="font-medium text-slate-800 dark:text-slate-200">{meeting.title}</h4>
                  <p className="text-sm text-slate-500 mt-1">
                    {new Date(meeting.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                    {new Date(meeting.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </p>
                  {meeting.videoUrl && (
                    <Button variant="outline" size="sm" className="w-full mt-3" onClick={() => window.open(meeting.videoUrl, '_blank')}>
                      <Video className="w-3 h-3 mr-2" /> Join Call
                    </Button>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        </aside>
      </div>
    </div>
  )
}

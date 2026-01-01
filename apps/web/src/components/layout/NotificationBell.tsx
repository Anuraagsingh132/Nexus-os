"use client"

import { useState, useEffect, useRef } from "react"
import { Bell, Info } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useStompConnection } from "@/hooks/useStompConnection"

type NotificationDto = {
  id: string
  title: string
  message: string
  isRead: boolean
  createdAt: string
}

export function NotificationBell() {
  const [notifications, setNotifications] = useState<NotificationDto[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // Fetch initial notifications
    fetch("/api/v1/notifications", { credentials: "include" })
      .then((res) => {
        if (res.ok) return res.json()
        return []
      })
      .then((data) => {
        if (Array.isArray(data)) {
          setNotifications(data)
        }
      })
      .catch(console.error)

    // Close dropdown on click outside
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener("mousedown", handleClickOutside)

    return () => {
      document.removeEventListener("mousedown", handleClickOutside)
    }
  }, [])

  const { client: stompClient } = useStompConnection()

  useEffect(() => {
    if (!stompClient) return

    const subscription = stompClient.subscribe("/user/queue/notifications", (message) => {
      if (message.body) {
        const newNotif = JSON.parse(message.body) as NotificationDto
        setNotifications((prev) => [newNotif, ...prev])
      }
    })

    return () => {
      subscription.unsubscribe()
    }
  }, [stompClient])

  useEffect(() => {
    setUnreadCount(notifications.filter((n) => !n.isRead).length)
  }, [notifications])

  const markAsRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    )
    fetch(`/api/v1/notifications/${id}/read`, {
      method: "PATCH",
      credentials: "include",
    }).catch(console.error)
  }

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    fetch("/api/v1/notifications/read-all", {
      method: "PATCH",
      credentials: "include",
    }).catch(console.error)
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <Button
        variant="ghost"
        size="icon"
        className="relative text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors rounded-full"
        onClick={() => setIsOpen(!isOpen)}
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-indigo-500"></span>
          </span>
        )}
      </Button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 lg:w-96 bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-slate-200/50 dark:border-slate-800/50 rounded-xl shadow-xl shadow-indigo-100/20 dark:shadow-none overflow-hidden z-50 animate-in slide-in-from-top-2 fade-in duration-200">
          <div className="px-4 py-3 border-b border-slate-100 dark:border-slate-800/50 flex justify-between items-center bg-slate-50/50 dark:bg-slate-950/50">
            <h3 className="font-semibold text-slate-800 dark:text-slate-100">Notifications</h3>
            {unreadCount > 0 && (
              <Button variant="ghost" size="sm" onClick={markAllAsRead} className="h-auto p-1 text-xs text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/20">
                Mark all read
              </Button>
            )}
          </div>
          <div className="max-h-[28rem] overflow-y-auto p-2 space-y-1">
            {notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-8 text-center text-slate-500">
                <Bell className="w-8 h-8 mb-2 opacity-20" />
                <p className="text-sm">No new notifications</p>
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => markAsRead(notif.id)}
                  className={`p-3 rounded-lg cursor-pointer transition-all duration-200 flex gap-3 items-start group ${
                    notif.isRead 
                      ? "hover:bg-slate-50 dark:hover:bg-slate-800/50" 
                      : "bg-indigo-50/50 dark:bg-indigo-900/20 hover:bg-indigo-50 dark:hover:bg-indigo-900/30"
                  }`}
                >
                  <div className={`mt-0.5 rounded-full p-1.5 shrink-0 ${notif.isRead ? "bg-slate-100 dark:bg-slate-800 text-slate-500" : "bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400 shadow-sm"}`}>
                    <Info className="w-4 h-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-medium truncate ${notif.isRead ? "text-slate-700 dark:text-slate-300" : "text-slate-900 dark:text-slate-100"}`}>
                      {notif.title}
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-2 leading-relaxed">
                      {notif.message}
                    </p>
                    <p className="text-[10px] text-slate-400 mt-2 font-medium">
                      {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                  {!notif.isRead && (
                    <div className="w-2 h-2 rounded-full bg-indigo-500 shadow-[0_0_8px_rgba(99,102,241,0.6)] mt-2 shrink-0"></div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}

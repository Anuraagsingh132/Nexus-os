"use client"

import { useState, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Send, Hash } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useParams } from "next/navigation"
import { useStompConnection } from "@/hooks/useStompConnection"

type ChatMessage = {
  id: string
  content: string
  author: { id: string; email: string; name?: string }
  createdAt: string
}

export default function ChatPage() {
  const params = useParams()
  const channelId = params.channelId as string
  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""
  const queryClient = useQueryClient()

  const [draft, setDraft] = useState("")
  const [errorToast, setErrorToast] = useState<string | null>(null)
  
  const { data: messages = [], isLoading } = useQuery<ChatMessage[]>({
    queryKey: ["messages", channelId],
    queryFn: async () => {
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/channels/${channelId}/messages`)
      if (!res.ok) throw new Error("Failed to fetch messages")
      return res.json()
    },
  })

  const { client: stompClient } = useStompConnection()

  useEffect(() => {
    if (!stompClient) return

    const subscription = stompClient.subscribe(`/topic/workspaces/${workspaceId}/channels/${channelId}`, (msg) => {
      const newMsg: ChatMessage = JSON.parse(msg.body)
      queryClient.setQueryData<ChatMessage[]>(["messages", channelId], (old = []) => {
        if (old.find(m => m.id === newMsg.id)) return old
        return [...old, newMsg]
      })
    })

    return () => {
      subscription.unsubscribe()
    }
  }, [channelId, queryClient, workspaceId, stompClient])

  const sendMessage = useMutation({
    mutationFn: async (content: string) => {
      if (stompClient && stompClient.connected) {
        stompClient.publish({
          destination: `/app/chat/${channelId}`,
          body: JSON.stringify({ content }),
        })
      } else {
        // Fallback to REST if WS disconnected
        const res = await fetch(`/api/v1/workspaces/${workspaceId}/channels/${channelId}/messages`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ content }),
        })
        if (!res.ok) throw new Error("Failed to send message")
      }
    },
    onMutate: async (newContent) => {
      await queryClient.cancelQueries({ queryKey: ["messages", channelId] })
      const previousMessages = queryClient.getQueryData<ChatMessage[]>(["messages", channelId])
      
      const optimisticMsg: ChatMessage = {
        id: `optimistic-${Date.now()}`,
        content: newContent,
        author: { id: "temp-user", email: "Sending..." },
        createdAt: new Date().toISOString()
      }
      
      queryClient.setQueryData<ChatMessage[]>(["messages", channelId], (old = []) => [...old, optimisticMsg])
      
      return { previousMessages }
    },
    onError: (err, newContent, context) => {
      if (context?.previousMessages) {
        queryClient.setQueryData(["messages", channelId], context.previousMessages)
      }
      setErrorToast("Failed to send message. Please try again.")
      setTimeout(() => setErrorToast(null), 3000)
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", channelId] })
    },
    onSuccess: () => {
      setDraft("")
    }
  })

  const handleSend = () => {
    if (!draft.trim()) return
    sendMessage.mutate(draft)
  }

  return (
    <div className="flex flex-col h-full bg-white dark:bg-slate-950 relative">
      {errorToast && (
        <div className="absolute top-4 right-4 bg-red-500 text-white px-4 py-2 rounded-md shadow-lg z-50 transition-opacity">
          {errorToast}
        </div>
      )}
      <header className="px-6 py-4 border-b flex items-center shadow-sm z-10">
        <Hash className="w-5 h-5 text-slate-400 mr-2" />
        <h2 className="font-semibold text-lg">{channelId === 'general' ? 'general' : channelId}</h2>
      </header>
      
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {isLoading ? (
          <div className="flex justify-center items-center h-full text-slate-500">Loading messages...</div>
        ) : messages.map((msg) => (
          <div key={msg.id} className="flex gap-4">
            <div className="w-10 h-10 rounded-full bg-indigo-100 dark:bg-indigo-900 flex items-center justify-center shrink-0">
              <span className="text-indigo-700 dark:text-indigo-300 font-medium">
                {msg.author?.email ? msg.author.email.substring(0, 2).toUpperCase() : "U"}
              </span>
            </div>
            <div>
              <div className="flex items-baseline gap-2">
                <span className="font-semibold text-sm">{msg.author?.email}</span>
                <span className="text-xs text-slate-500">{new Date(msg.createdAt).toLocaleTimeString()}</span>
              </div>
              <p className="text-slate-800 dark:text-slate-200 mt-1">{msg.content}</p>
            </div>
          </div>
        ))}
      </div>
      
      <div className="p-4 border-t bg-slate-50 dark:bg-slate-900">
        <div className="flex items-center gap-2 max-w-4xl mx-auto relative">
          <Input 
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSend() }}
            placeholder="Type a message..." 
            className="flex-1 pr-12 bg-white dark:bg-slate-950" 
            disabled={sendMessage.isPending}
          />
          <Button size="icon" onClick={handleSend} disabled={sendMessage.isPending || !draft.trim()} className="absolute right-1 top-1 bottom-1 h-auto">
            <Send className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

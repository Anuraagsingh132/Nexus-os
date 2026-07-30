"use client"

import { useEffect, useState, useMemo, useRef, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Button } from "@/components/ui/button"
import { ChevronLeft, CheckCircle2, Wifi, WifiOff, Loader2, Bot, Sparkles, X } from "lucide-react"
import { HocuspocusProvider } from '@hocuspocus/provider'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import { getWsTicket } from "@/lib/ws-ticket"

import { apiFetch } from "@/lib/api"

type Doc = {
  id: string
  title: string
  content: string
  updatedAt: string
}

const colors = ['#958DF1', '#F98181', '#FBBC88', '#FAF594', '#70CFF8', '#94FADB', '#B9F18D']
const generateRandomColor = () => colors[Math.floor(Math.random() * colors.length)]

export default function DocumentEditorPage() {
  const params = useParams()
  const router = useRouter()
  const documentId = params.documentId as string
  const [workspaceId, setWorkspaceId] = useState<string>("")
  const queryClient = useQueryClient()

  useEffect(() => {
    if (typeof window !== "undefined") {
      setWorkspaceId(localStorage.getItem("workspaceId") || "")
    }
  }, [])
  
  const [title, setTitle] = useState("Loading...")
  const [status, setStatus] = useState('connecting')
  const [isFallback, setIsFallback] = useState(false)
  const [wsTicket, setWsTicket] = useState<string | null>(null)
  const [sessionUserName, setSessionUserName] = useState<string>("Workspace User")
  const docContentRef = useRef<string | undefined>(undefined)
  
  const [askAgentOpen, setAskAgentOpen] = useState(false)
  const [askAgentQuery, setAskAgentQuery] = useState("")
  const [askAgentLoading, setAskAgentLoading] = useState(false)

  useEffect(() => {
    apiFetch('/api/v1/auth/me')
      .then(res => res.ok ? res.json() : null)
      .then(user => {
        if (user && (user.fullName || user.email)) {
          setSessionUserName(user.fullName || user.email)
        }
      })
      .catch(() => {})
  }, [])

  const { data: doc } = useQuery<Doc>({
    queryKey: ["doc", documentId],
    queryFn: async () => {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/documents/${documentId}`)
      if (!res.ok) throw new Error("Failed to fetch doc")
      return res.json()
    },
    enabled: !!workspaceId
  })

  useEffect(() => {
    if (doc) {
      setTitle(doc.title)
      docContentRef.current = doc.content
    }
  }, [doc])

  const saveTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const contentSaveTimeoutRef = useRef<NodeJS.Timeout | null>(null)

  const saveDoc = useMutation({
    mutationFn: async ({ updatedTitle, updatedContent }: { updatedTitle?: string; updatedContent?: string }) => {
      const payload: Record<string, string> = {}
      if (updatedTitle !== undefined) payload.title = updatedTitle
      if (updatedContent !== undefined) payload.content = updatedContent
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/documents/${documentId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })
      if (!res.ok) throw new Error("Failed to save doc")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["doc", documentId] })
      queryClient.invalidateQueries({ queryKey: ["docs"] })
    }
  })

  const debouncedTitleSave = useCallback((newTitle: string) => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current)
    saveTimeoutRef.current = setTimeout(() => {
      saveDoc.mutate({ updatedTitle: newTitle })
    }, 1000)
  }, [saveDoc])

  const debouncedContentSave = useCallback((newContent: string) => {
    if (contentSaveTimeoutRef.current) clearTimeout(contentSaveTimeoutRef.current)
    contentSaveTimeoutRef.current = setTimeout(() => {
      saveDoc.mutate({ updatedContent: newContent })
    }, 1000)
  }, [saveDoc])

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTitle = e.target.value
    setTitle(newTitle)
    debouncedTitleSave(newTitle)
  }

  const currentUser = useMemo(() => ({ name: sessionUserName }), [sessionUserName])

  useEffect(() => {
    if (isFallback) return

    let cancelled = false
    setWsTicket(null)
    setStatus('connecting')

    getWsTicket()
      .then((ticket) => {
        if (!cancelled) setWsTicket(ticket)
      })
      .catch(() => {
        if (!cancelled) setIsFallback(true)
      })

    return () => {
      cancelled = true
    }
  }, [documentId, isFallback])

  const provider = useMemo(() => {
    if (typeof window === 'undefined' || isFallback || !wsTicket) return null
    const p = new HocuspocusProvider({
      url: process.env.NEXT_PUBLIC_HOCUSPOCUS_URL || 'ws://localhost:1234',
      name: documentId,
      token: wsTicket,
      onStatus: ({ status }: { status: string }) => {
        setStatus(status)
      },
      onClose: () => {
        setIsFallback(true)
      },
      onSynced: () => {
        if (p.document.getXmlFragment('default').length === 0 && docContentRef.current) {
          setIsFallback(true)
        }
      }
    })
    return p
  }, [documentId, isFallback, wsTicket])

  const editor = useEditor({
    editable: true,
    content: isFallback ? doc?.content : undefined,
    onUpdate: ({ editor }) => {
      if (isFallback) {
        debouncedContentSave(editor.getHTML())
      }
    },
    extensions: [
      StarterKit.configure({ undoRedo: false }),
      ...(provider ? [
        Collaboration.configure({
          document: provider.document,
        }),
        CollaborationCaret.configure({
          provider,
          user: {
            name: currentUser.name,
            color: generateRandomColor(),
          },
        }),
      ] : []),
    ],
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none dark:prose-invert max-w-none h-full min-h-[500px]',
      },
    },
  }, [provider, isFallback, doc?.content, currentUser, debouncedContentSave])

  // Cleanup provider on unmount
  useEffect(() => {
    return () => {
      if (provider) {
        provider.destroy()
      }
    }
  }, [provider])

  const handleAskAgent = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!askAgentQuery.trim() || !editor) return
    
    setAskAgentLoading(true)
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/ai/query`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: askAgentQuery, documentId }),
      })
      if (res.ok) {
        const data = await res.json()
        if (data.answer) {
          editor.commands.insertContent(data.answer)
          setAskAgentOpen(false)
          setAskAgentQuery("")
        }
      }
    } catch (error) {
      console.error("Ask Agent failed:", error)
    } finally {
      setAskAgentLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-full bg-white dark:bg-slate-950">
      <header className="px-6 py-4 border-b flex items-center justify-between shadow-sm sticky top-0 bg-white dark:bg-slate-950 z-10">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => router.push('/dashboard/docs')}>
            <ChevronLeft className="w-5 h-5" />
          </Button>
          <input 
            value={title} 
            onChange={handleTitleChange}
            className="text-xl font-bold bg-transparent border-none outline-none focus:ring-2 focus:ring-indigo-500 rounded px-2 py-1"
          />
        </div>
        <div className="flex items-center gap-4">
          <Button 
            variant="outline" 
            size="sm" 
            className="gap-2 border-indigo-200 text-indigo-600 hover:bg-indigo-50 dark:border-indigo-900/50 dark:text-indigo-400 dark:hover:bg-indigo-900/20"
            onClick={() => setAskAgentOpen(true)}
          >
            <Bot className="w-4 h-4" /> Ask Agent
          </Button>
          <div className="flex items-center gap-2 text-sm font-medium">
            {status === 'connected' && <span className="text-green-500 flex items-center gap-1"><Wifi className="w-4 h-4" /> Connected</span>}
            {status === 'connecting' && <span className="text-yellow-500 flex items-center gap-1"><Loader2 className="w-4 h-4 animate-spin" /> Connecting...</span>}
            {status === 'disconnected' && <span className="text-red-500 flex items-center gap-1"><WifiOff className="w-4 h-4" /> Disconnected</span>}
          </div>
          <span className="text-sm text-slate-500 flex items-center gap-1">
            {saveDoc.isPending ? "Saving title..." : saveDoc.isSuccess ? <><CheckCircle2 className="w-4 h-4 text-green-500" /> Title saved</> : ""}
          </span>
        </div>
      </header>
      
      <div className="flex-1 overflow-y-auto p-8">
        {isFallback && (
          <div className="max-w-4xl mx-auto mb-4 bg-yellow-50 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200 p-4 rounded-md flex items-center justify-center text-sm font-medium border border-yellow-200 dark:border-yellow-800">
            Recovered from backup — collaborative history unavailable
          </div>
        )}
        <div className="max-w-4xl mx-auto border rounded-xl p-8 shadow-sm min-h-full bg-white dark:bg-slate-900">
          {provider || isFallback ? <EditorContent editor={editor} /> : <div className="flex justify-center items-center h-full"><Loader2 className="w-8 h-8 animate-spin text-indigo-500" /></div>}
        </div>
      </div>
      
      {askAgentOpen && (
        <div className="fixed inset-x-0 bottom-0 z-50 p-4 sm:p-6 flex justify-center pointer-events-none">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 w-full max-w-2xl pointer-events-auto flex flex-col overflow-hidden animate-in slide-in-from-bottom-10 fade-in duration-200">
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-300">
                <Sparkles className="w-4 h-4 text-indigo-500" /> Ask AI Agent
              </div>
              <button onClick={() => setAskAgentOpen(false)} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleAskAgent} className="p-4 flex gap-3">
              <input 
                type="text" 
                value={askAgentQuery}
                onChange={e => setAskAgentQuery(e.target.value)}
                placeholder="Ask agent to write, outline, or summarize..."
                className="flex-1 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 dark:text-white"
                autoFocus
                disabled={askAgentLoading}
              />
              <Button type="submit" disabled={!askAgentQuery.trim() || askAgentLoading} className="bg-indigo-600 hover:bg-indigo-700 text-white">
                {askAgentLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Send"}
              </Button>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

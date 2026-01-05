"use client"

import { useEffect, useState, useMemo, useRef, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Button } from "@/components/ui/button"
import { ChevronLeft, CheckCircle2, Wifi, WifiOff, Loader2 } from "lucide-react"
import { HocuspocusProvider } from '@hocuspocus/provider'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import { getWsTicket } from "@/lib/ws-ticket"

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
  const workspaceId = typeof window !== "undefined" ? localStorage.getItem("workspaceId") || "" : ""
  const queryClient = useQueryClient()
  
  const [title, setTitle] = useState("Loading...")
  const [status, setStatus] = useState('connecting')
  const [isFallback, setIsFallback] = useState(false)
  const [wsTicket, setWsTicket] = useState<string | null>(null)
  const docContentRef = useRef<string | undefined>(undefined)

  const { data: doc } = useQuery<Doc>({
    queryKey: ["doc", documentId],
    queryFn: async () => {
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/documents/${documentId}`)
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

  const saveDoc = useMutation({
    mutationFn: async ({ updatedTitle }: { updatedTitle: string }) => {
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/documents/${documentId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: updatedTitle }),
      })
      if (!res.ok) throw new Error("Failed to save doc title")
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

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTitle = e.target.value
    setTitle(newTitle)
    debouncedTitleSave(newTitle)
  }

  const currentUser = useMemo(() => ({ name: `User ${Math.floor(Math.random() * 1000)}` }), [])

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
    editable: !isFallback,
    content: isFallback ? doc?.content : undefined,
    extensions: provider ? [
      StarterKit.configure({ undoRedo: false }),
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
    ] : isFallback ? [StarterKit] : [],
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none dark:prose-invert max-w-none h-full min-h-[500px]',
      },
    },
  }, [provider, isFallback, doc?.content])

  // Cleanup provider on unmount
  useEffect(() => {
    return () => {
      if (provider) {
        provider.destroy()
      }
    }
  }, [provider])

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
    </div>
  )
}

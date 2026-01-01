"use client"

import { useState, useEffect } from "react"
import { Search } from "lucide-react"
import { useRouter } from "next/navigation"

export function GlobalSearch({ workspaceId }: { workspaceId: string | null }) {
  const [query, setQuery] = useState("")
  const [results, setResults] = useState<{ id: string; title: string; content?: string }[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const router = useRouter()

  useEffect(() => {
    if (!query || !workspaceId) {
      setResults([])
      return
    }

    const timer = setTimeout(() => {
      fetch(`/api/v1/workspaces/${workspaceId}/search?q=${encodeURIComponent(query)}`, { credentials: "include" })
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data)) {
            setResults(data)
          }
        })
        .catch(console.error)
    }, 300)

    return () => clearTimeout(timer)
  }, [query, workspaceId])

  return (
    <div className="relative">
      <div className="relative flex items-center">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setIsOpen(true)
          }}
          onFocus={() => setIsOpen(true)}
          placeholder="Search documents..."
          className="pl-9 pr-4 py-1.5 w-64 text-sm bg-slate-100 dark:bg-slate-800 border-none rounded-md focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-500"
        />
      </div>

      {isOpen && query && (
        <div className="absolute top-full mt-2 w-80 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-md shadow-lg overflow-hidden z-50">
          {results.length === 0 ? (
            <div className="p-3 text-sm text-slate-500 text-center">No results found</div>
          ) : (
            <ul className="max-h-64 overflow-y-auto">
              {results.map(doc => (
                <li key={doc.id}>
                  <button
                    onClick={() => {
                      setIsOpen(false)
                      router.push(`/dashboard/docs/${doc.id}`)
                    }}
                    className="w-full text-left p-3 hover:bg-slate-50 dark:hover:bg-slate-800/50 border-b border-slate-100 dark:border-slate-800 last:border-0"
                  >
                    <div className="font-medium text-sm text-slate-900 dark:text-slate-100">{doc.title}</div>
                    {doc.content && (
                      <div className="text-xs text-slate-500 mt-1 truncate">
                        {doc.content.substring(0, 60)}...
                      </div>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
      
      {/* Click outside backdrop */}
      {isOpen && query && (
        <div 
          className="fixed inset-0 z-40 bg-transparent" 
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  )
}

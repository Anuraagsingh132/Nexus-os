"use client"

import { useState, useEffect } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { FileIcon, UploadCloud, Download, Trash } from "lucide-react"
import { Badge } from "@/components/ui/badge"

import { apiFetch } from "@/lib/api"

type FileMetadata = {
  id: string
  fileName: string
  fileSize: number
  contentType: string
  createdAt: string
  ingestionStatus?: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'COMPLETE' | 'FAILED'
}

export default function FilesPage() {
  const [workspaceId, setWorkspaceId] = useState<string>("")
  const queryClient = useQueryClient()
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (typeof window !== "undefined") {
      setWorkspaceId(localStorage.getItem("workspaceId") || "")
    }
  }, [])

  const { data: files = [], isLoading, error } = useQuery<FileMetadata[]>({
    queryKey: ["files"],
    queryFn: async () => {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/files`)
      if (!res.ok) throw new Error("Failed to fetch files")
      return res.json()
    },
    enabled: !!workspaceId,
    refetchInterval: 3000
  })

  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData()
      formData.append("file", file)
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/files/upload`, {
        method: "POST",
        body: formData,
      })
      if (!res.ok) throw new Error("Upload failed")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["files"] })
    }
  })

  const retryMutation = useMutation({
    mutationFn: async (fileId: string) => {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/files/${fileId}/retry-ingestion`, {
        method: "POST",
      })
      if (!res.ok) throw new Error("Retry failed")
      return res
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["files"] })
    }
  })

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setUploading(true)
      uploadMutation.mutate(e.target.files[0], {
        onSettled: () => {
          setUploading(false)
          e.target.value = ''
        }
      })
    }
  }

  const downloadFile = async (fileId: string, fileName: string) => {
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/files/${fileId}/download`)
      if (!res.ok) throw new Error("Failed to get download URL")
      const { url } = await res.json()
      
      const a = document.createElement("a")
      a.href = url
      a.download = fileName
      a.target = "_blank"
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    } catch (err) {
      console.error(err)
      alert("Failed to download file.")
    }
  }

  const deleteFile = async (fileId: string) => {
    if (!confirm("Are you sure you want to delete this file?")) return;
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/files/${fileId}`, {
        method: "DELETE"
      })
      if (!res.ok) throw new Error("Failed to delete file")
      queryClient.invalidateQueries({ queryKey: ["files"] })
    } catch (err) {
      console.error(err)
      alert("Failed to delete file.")
    }
  }

  function formatBytes(bytes: number, decimals = 2) {
    if (!+bytes) return '0 Bytes'
    const k = 1024
    const dm = decimals < 0 ? 0 : decimals
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
  }

  return (
    <div className="flex flex-col h-full bg-slate-50 dark:bg-slate-900">
      <header className="px-6 py-4 border-b bg-white dark:bg-slate-950 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Files</h1>
          <p className="text-sm text-slate-500">Workspace storage</p>
        </div>
        <div>
          <input 
            type="file" 
            id="file-upload" 
            className="hidden" 
            onChange={handleFileUpload} 
            disabled={uploading} 
          />
          <label htmlFor="file-upload">
            <span className="inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-4 py-2 cursor-pointer">
              <UploadCloud className="w-4 h-4 mr-2" />
              {uploading ? "Uploading..." : "Upload File"}
            </span>
          </label>
        </div>
      </header>
      
      <div className="flex-1 p-6 overflow-y-auto">
        <div className="max-w-6xl mx-auto space-y-6">
          {isLoading ? (
            <div className="text-center py-12 text-slate-500">Loading files...</div>
          ) : error ? (
            <div className="text-center py-12 text-destructive">Error loading files: {(error as Error).message}</div>
          ) : files.length === 0 ? (
            <div className="text-center py-20 text-slate-500 bg-white dark:bg-slate-950 rounded-xl border border-dashed border-slate-300 dark:border-slate-800">
              <UploadCloud className="w-12 h-12 mx-auto text-slate-300 mb-4" />
              <p>No files uploaded yet.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {files.map(file => (
                <Card key={file.id} className="group overflow-hidden transition-all hover:shadow-md border-slate-200 dark:border-slate-800">
                  <div className="h-24 bg-slate-100 dark:bg-slate-800 flex items-center justify-center relative">
                    <FileIcon className="w-10 h-10 text-indigo-400" />
                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col gap-2 items-center justify-center">
                      <Button variant="secondary" size="sm" onClick={() => downloadFile(file.id, file.fileName)}>
                        <Download className="w-4 h-4 mr-2" /> Download
                      </Button>
                      {(file.ingestionStatus === 'FAILED' || file.ingestionStatus === 'PENDING') && (
                        <Button variant="default" size="sm" onClick={() => retryMutation.mutate(file.id)}>
                          {retryMutation.isPending ? "Retrying..." : "Retry Ingestion"}
                        </Button>
                      )}
                      <Button variant="destructive" size="sm" onClick={() => deleteFile(file.id)}>
                        <Trash className="w-4 h-4 mr-2" /> Delete
                      </Button>
                    </div>
                  </div>
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <h3 className="font-semibold text-sm truncate flex-1" title={file.fileName}>{file.fileName}</h3>
                      {file.ingestionStatus && (
                        <Badge 
                          variant={file.ingestionStatus === 'FAILED' ? 'destructive' : 'default'}
                          className={`text-[10px] px-1.5 py-0 h-4 uppercase ${
                            file.ingestionStatus === 'SUCCESS' || file.ingestionStatus === 'COMPLETE' ? 'bg-green-500 hover:bg-green-600' :
                            file.ingestionStatus === 'FAILED' ? '' : 'bg-yellow-500 hover:bg-yellow-600 text-yellow-950'
                          }`}
                        >
                          {file.ingestionStatus}
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 flex justify-between items-center">
                      <span>{formatBytes(file.fileSize)}</span>
                      <span>{new Date(file.createdAt).toLocaleDateString()}</span>
                    </p>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

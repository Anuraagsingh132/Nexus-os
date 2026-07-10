"use client"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { apiFetch } from "@/lib/api"

export function InviteTeamMember({ workspaceId }: { workspaceId: string }) {
  const [open, setOpen] = useState(false)
  const [email, setEmail] = useState("")
  const [status, setStatus] = useState("")

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!workspaceId) return
    setStatus("Sending...")
    try {
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/invites`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email })
      })
      if (res.ok) {
        setStatus("Invited!")
        setTimeout(() => {
          setOpen(false)
          setEmail("")
          setStatus("")
        }, 1500)
      } else {
        setStatus("Failed to invite")
      }
    } catch {
      setStatus("Error")
    }
  }

  return (
    <>
      <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
        Invite Team Member
      </Button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 p-6 rounded-lg shadow-xl w-full max-w-md border border-slate-200 dark:border-slate-800">
            <h3 className="text-lg font-semibold mb-4 text-slate-900 dark:text-slate-100">Invite Team Member</h3>
            <form onSubmit={handleInvite} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Email Address</label>
                <Input 
                  type="email" 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                  required 
                  placeholder="colleague@example.com"
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={status === "Sending..."}>
                  {status === "Sending..." ? "Sending..." : "Send Invite"}
                </Button>
              </div>
              {status && status !== "Sending..." && <p className="text-sm mt-2 text-center text-slate-600 dark:text-slate-400">{status}</p>}
            </form>
          </div>
        </div>
      )}
    </>
  )
}

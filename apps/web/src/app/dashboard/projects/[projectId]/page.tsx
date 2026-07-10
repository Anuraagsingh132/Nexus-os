"use client"

import { useEffect, useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useParams } from "next/navigation"
import { apiFetch } from "@/lib/api"
import { DragDropContext, Droppable, Draggable, DropResult } from "@hello-pangea/dnd"
import { useStompConnection } from "@/hooks/useStompConnection"

type Task = {
  id: string
  title: string
  description?: string
  status: "TODO" | "IN_PROGRESS" | "DONE"
  position: number
  assignee?: { id: string; fullName: string; email: string } | null
}

export default function ProjectBoard() {
  const params = useParams()
  const projectId = params.projectId as string
  const [workspaceId, setWorkspaceId] = useState<string | null>(null)
  const [project, setProject] = useState<{name: string} | null>(null)
  const [isAddingTask, setIsAddingTask] = useState(false)
  const queryClient = useQueryClient()

  useEffect(() => {
    const wid = localStorage.getItem("workspaceId")
    if (wid) {
      setWorkspaceId(wid)
      fetch(`/api/v1/workspaces/${wid}/projects/${projectId}`, { credentials: "include" })
        .then(res => res.json())
        .then(data => setProject(data))
    }
  }, [projectId])

  const { data: tasks = [], isLoading, error } = useQuery<Task[]>({
    queryKey: ["tasks", projectId],
    queryFn: async () => {
      if (!workspaceId) return []
      const res = await fetch(`/api/v1/workspaces/${workspaceId}/projects/${projectId}/tasks`, { credentials: "include" })
      if (!res.ok) throw new Error("Failed to fetch tasks")
      return res.json()
    },
    enabled: !!workspaceId
  })

  const { client: stompClient } = useStompConnection()

  useEffect(() => {
    if (!workspaceId || !projectId || !stompClient) return

    const subscription = stompClient.subscribe(`/topic/workspaces/${workspaceId}/projects/${projectId}/tasks`, (message) => {
      if (message.body) {
        queryClient.invalidateQueries({ queryKey: ["tasks", projectId] })
      }
    })

    return () => {
      subscription.unsubscribe()
    }
  }, [workspaceId, projectId, queryClient, stompClient])

  const createTask = useMutation({
    mutationFn: async (newTask: Partial<Task>) => {
      if (!workspaceId) throw new Error("No workspace ID")
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/projects/${projectId}/tasks`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newTask),
      })
      if (!res.ok) throw new Error("Failed to create task")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks", projectId] })
    },
  })

  const moveTask = useMutation({
    mutationFn: async ({ taskId, status, position }: { taskId: string, status: string, position: number }) => {
      if (!workspaceId) throw new Error("No workspace ID")
      const res = await apiFetch(`/api/v1/workspaces/${workspaceId}/projects/${projectId}/tasks/${taskId}/move`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status, position }),
      })
      if (!res.ok) throw new Error("Failed to move task")
      return res.json()
    },
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey: ["tasks", projectId] })
      const previousTasks = queryClient.getQueryData<Task[]>(["tasks", projectId])
      
      // Optimistic update logic
      if (previousTasks) {
        const newTasks = [...previousTasks]
        const taskIndex = newTasks.findIndex(t => t.id === variables.taskId)
        if (taskIndex > -1) {
          const [movedTask] = newTasks.splice(taskIndex, 1)
          movedTask.status = variables.status as Task["status"]
          
          // We don't recalculate all positions in the UI perfectly, we just force a refetch after success
          // But to prevent flicker, we can insert it in the right visual place for the specific column
          // Just set it to roughly the right place
          newTasks.push(movedTask)
          queryClient.setQueryData(["tasks", projectId], newTasks)
        }
      }
      return { previousTasks }
    },
    onError: (err, newTodo, context) => {
      if (context?.previousTasks) {
        queryClient.setQueryData(["tasks", projectId], context.previousTasks)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks", projectId] })
    },
  })

  const columns = [
    { id: "TODO", title: "To Do" },
    { id: "IN_PROGRESS", title: "In Progress" },
    { id: "DONE", title: "Done" },
  ]

  const onDragEnd = (result: DropResult) => {
    const { destination, source, draggableId } = result

    if (!destination) return
    if (destination.droppableId === source.droppableId && destination.index === source.index) return

    moveTask.mutate({
      taskId: draggableId,
      status: destination.droppableId,
      position: destination.index
    })
  }

  // To fix Next.js hydration issues with drag-and-drop
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])
  if (!mounted) return null

  return (
    <div className="flex flex-col h-full bg-slate-50 dark:bg-slate-900">
      <header className="px-6 py-4 border-b bg-white dark:bg-slate-950 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{project ? project.name : "Loading Project..."}</h1>
          <p className="text-sm text-slate-500">Project Board</p>
        </div>
        {!isAddingTask ? (
          <Button onClick={() => setIsAddingTask(true)}>
            <Plus className="w-4 h-4 mr-2" /> Add Task
          </Button>
        ) : (
          <form onSubmit={(e) => {
            e.preventDefault()
            const formData = new FormData(e.currentTarget)
            const title = formData.get("title") as string
            if (title) {
              createTask.mutate({ title, description: "Task details...", status: "TODO", position: tasks.filter(t => t.status === "TODO").length })
              e.currentTarget.reset()
              setIsAddingTask(false)
            }
          }} className="flex items-center gap-2">
            <input name="title" placeholder="New Task Title" className="border px-2 py-1 rounded" required />
            <Button type="button" variant="outline" onClick={() => setIsAddingTask(false)}>Cancel</Button>
            <Button type="submit" disabled={createTask.isPending}>
              Create Task
            </Button>
          </form>
        )}
      </header>
      
      {isLoading ? (
        <div className="flex items-center justify-center flex-1">Loading tasks...</div>
      ) : error ? (
        <div className="flex items-center justify-center flex-1 text-destructive">Error loading tasks: {(error as Error).message}</div>
      ) : (
        <DragDropContext onDragEnd={onDragEnd}>
          <div className="flex-1 overflow-x-auto p-6">
            <div className="flex gap-6 h-full items-start">
              {columns.map(col => (
                <div key={col.id} className="w-80 shrink-0 bg-slate-100 dark:bg-slate-800 rounded-lg p-4 flex flex-col max-h-full">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-semibold text-sm uppercase text-slate-600 dark:text-slate-300">{col.title}</h3>
                    <span className="bg-slate-200 dark:bg-slate-700 text-xs px-2 py-1 rounded-full font-medium">
                      {tasks.filter(t => t.status === col.id).length}
                    </span>
                  </div>
                  
                  <Droppable droppableId={col.id}>
                    {(provided) => (
                      <div 
                        {...provided.droppableProps} 
                        ref={provided.innerRef}
                        className="space-y-3 overflow-y-auto flex-1 pr-1 min-h-[200px]"
                      >
                        {tasks
                          .filter(t => t.status === col.id)
                          .sort((a, b) => a.position - b.position)
                          .map((task, index) => (
                            <Draggable key={task.id} draggableId={task.id} index={index}>
                              {(provided) => (
                                <div
                                  ref={provided.innerRef}
                                  {...provided.draggableProps}
                                  {...provided.dragHandleProps}
                                >
                                  <Card className="cursor-pointer hover:border-indigo-500 transition-colors shadow-sm bg-white dark:bg-slate-950">
                                    <CardContent className="p-4 flex flex-col gap-1">
                                      <p className="text-sm font-medium">{task.title}</p>
                                      {task.assignee && (
                                        <div className="flex items-center gap-2 mt-1">
                                          <div className="flex items-center justify-center w-6 h-6 rounded-full bg-indigo-100 dark:bg-indigo-900 text-indigo-700 dark:text-indigo-300 text-xs font-semibold" title={task.assignee.fullName}>
                                            {task.assignee.fullName.charAt(0).toUpperCase()}
                                          </div>
                                          <span className="text-xs text-slate-500 dark:text-slate-400 truncate">
                                            {task.assignee.fullName}
                                          </span>
                                        </div>
                                      )}
                                    </CardContent>
                                  </Card>
                                </div>
                              )}
                            </Draggable>
                        ))}
                        {provided.placeholder}
                      </div>
                    )}
                  </Droppable>
                </div>
              ))}
            </div>
          </div>
        </DragDropContext>
      )}
    </div>
  )
}

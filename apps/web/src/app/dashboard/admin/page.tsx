"use client"

import { useEffect, useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Users, HardDrive, LayoutGrid, Activity, Trash2, Ban, CheckCircle } from "lucide-react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer } from "recharts"

import { apiFetch } from "@/lib/api"

export default function AdminDashboard() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeWorkspaces: 0,
    storageUsed: 0 // in GB
  })
  const [lastRefreshed, setLastRefreshed] = useState<Date | null>(null)
  
  interface SystemHealth {
    [key: string]: string;
  }
  const [health, setHealth] = useState<SystemHealth>({})
  
  interface Alert {
    title: string;
    desc: string;
    time: string;
    color?: string;
    bg?: string;
  }
  
  interface ActivityDataPointDto {
    label: string;
    value: number;
  }
  
  interface User {
    id: string;
    email: string;
    name: string;
    status: string;
  }
  
  interface Workspace {
    id: string;
    name: string;
    ownerId: string;
    createdAt: string;
  }
  
  const [activity, setActivity] = useState<ActivityDataPointDto[]>([])
  const [activityLoading, setActivityLoading] = useState(true)
  
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [workspacesLoading, setWorkspacesLoading] = useState(true)

  useEffect(() => {
    const fetchHealth = () => {
      apiFetch('/api/v1/admin/health')
        .then(res => res.json())
        .then(data => {
          if (data && typeof data === 'object' && !data.error) {
            setHealth(data);
          }
        })
        .catch(console.error);
    };

    fetchHealth();
    const healthInterval = setInterval(fetchHealth, 30000);
    return () => clearInterval(healthInterval);
  }, []);

  const fetchUsers = () => {
    setUsersLoading(true)
    apiFetch('/api/v1/admin/users')
      .then(res => res.json())
      .then(data => {
        if (data && Array.isArray(data.items)) {
          setUsers(data.items);
        } else if (Array.isArray(data)) {
          setUsers(data);
        }
      })
      .catch(console.error)
      .finally(() => setUsersLoading(false))
  }

  const fetchWorkspaces = () => {
    setWorkspacesLoading(true)
    apiFetch('/api/v1/admin/workspaces')
      .then(res => res.json())
      .then(data => {
        if (data && Array.isArray(data.items)) {
          setWorkspaces(data.items);
        } else if (Array.isArray(data)) {
          setWorkspaces(data);
        }
      })
      .catch(console.error)
      .finally(() => setWorkspacesLoading(false))
  }

  const toggleUserStatus = (id: string, currentStatus: string) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    apiFetch(`/api/v1/admin/users/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus }),
    }).then(res => {
      if (res.ok) fetchUsers();
    }).catch(console.error);
  }

  const deleteWorkspace = (id: string) => {
    if (!confirm('Are you sure you want to delete this workspace?')) return;
    apiFetch(`/api/v1/admin/workspaces/${id}`, {
      method: 'DELETE',
    }).then(res => {
      if (res.ok) fetchWorkspaces();
    }).catch(console.error);
  }

  useEffect(() => {
    const fetchAll = () => {
      apiFetch('/api/v1/admin/stats')
        .then(res => res.json())
        .then(data => {
          if (data && !data.error) {
            setStats({
              totalUsers: data.totalUsers || 0,
              activeWorkspaces: data.activeWorkspaces || 0,
              storageUsed: data.storageUsed || 0
            })
          }
        })
        .catch(console.error)

      setActivityLoading(true);
      apiFetch('/api/v1/admin/activity')
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data)) {
            if (data.length > 0 && typeof data[0] === 'number') {
              setActivity(data.map((val, i) => ({ label: `Point ${i+1}`, value: val })))
            } else {
              setActivity(data)
            }
          }
        })
        .catch(console.error)
        .finally(() => setActivityLoading(false))

      apiFetch('/api/v1/admin/alerts')
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data)) setAlerts(data)
        })
        .catch(console.error)
        
      fetchUsers();
      fetchWorkspaces();

      setLastRefreshed(new Date())
    }

    fetchAll()
  }, [])

  return (
    <div className="p-6 h-full overflow-y-auto bg-slate-50 dark:bg-slate-900">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight bg-gradient-to-br from-slate-900 to-slate-500 dark:from-white dark:to-slate-400 bg-clip-text text-transparent">
              Admin Control Panel
            </h1>
            <p className="text-slate-500 mt-1">Platform-wide analytics and system overview.</p>
          </div>
          <div className="flex items-center gap-2 text-sm font-medium text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-800/50 px-3 py-1.5 rounded-full border border-slate-200 dark:border-slate-700/50 shadow-sm backdrop-blur-sm">
            Last refreshed: {lastRefreshed ? lastRefreshed.toLocaleTimeString() : '...'}
          </div>
        </header>

        <Tabs defaultValue="overview" className="w-full">
          <TabsList className="mb-4">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="users">Users</TabsTrigger>
            <TabsTrigger value="workspaces">Workspaces</TabsTrigger>
          </TabsList>
          
          <TabsContent value="overview" className="space-y-6">
            {/* System Health Section */}
            <Card className="border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-slate-500 dark:text-slate-400">System Health</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-4">
                  {['Database', 'Redis', 'Minio', 'Qdrant', 'Ollama'].map((service) => {
                    const status = health[service] || health[service.toLowerCase()] || 'PENDING';
                    const isUp = status === 'UP';
                    const isDown = status === 'DOWN';
                    const dotColor = isUp ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]' : isDown ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]' : 'bg-slate-400';
                    return (
                      <div key={service} className="flex items-center gap-2 bg-slate-100 dark:bg-slate-800/50 px-3 py-1.5 rounded-full border border-slate-200 dark:border-slate-700/50">
                        <div className={`w-2 h-2 rounded-full ${dotColor}`}></div>
                        <span className="text-xs font-medium text-slate-700 dark:text-slate-300">{service}</span>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <Card className="relative overflow-hidden group border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm hover:shadow-md transition-all duration-300">
                <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/5 to-purple-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <CardHeader className="flex flex-row items-center justify-between pb-2 relative z-10">
                  <CardTitle className="text-sm font-medium text-slate-500 dark:text-slate-400">
                    Total Users
                  </CardTitle>
                  <div className="p-2 bg-indigo-50 dark:bg-indigo-900/30 rounded-lg shadow-sm">
                    <Users className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                  </div>
                </CardHeader>
                <CardContent className="relative z-10">
                  <div className="flex items-baseline gap-2">
                    <div className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">{stats.totalUsers.toLocaleString()}</div>
                  </div>
                  <p className="text-xs text-slate-500 mt-2">Active accounts across platform</p>
                </CardContent>
              </Card>

              <Card className="relative overflow-hidden group border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm hover:shadow-md transition-all duration-300">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 to-cyan-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <CardHeader className="flex flex-row items-center justify-between pb-2 relative z-10">
                  <CardTitle className="text-sm font-medium text-slate-500 dark:text-slate-400">
                    Active Workspaces
                  </CardTitle>
                  <div className="p-2 bg-blue-50 dark:bg-blue-900/30 rounded-lg shadow-sm">
                    <LayoutGrid className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                  </div>
                </CardHeader>
                <CardContent className="relative z-10">
                  <div className="flex items-baseline gap-2">
                    <div className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">{stats.activeWorkspaces.toLocaleString()}</div>
                  </div>
                  <p className="text-xs text-slate-500 mt-2">Currently active instances</p>
                </CardContent>
              </Card>

              <Card className="relative overflow-hidden group border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm hover:shadow-md transition-all duration-300">
                <div className="absolute inset-0 bg-gradient-to-br from-amber-500/5 to-orange-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <CardHeader className="flex flex-row items-center justify-between pb-2 relative z-10">
                  <CardTitle className="text-sm font-medium text-slate-500 dark:text-slate-400">
                    Storage Used
                  </CardTitle>
                  <div className="p-2 bg-amber-50 dark:bg-amber-900/30 rounded-lg shadow-sm">
                    <HardDrive className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  </div>
                </CardHeader>
                <CardContent className="relative z-10">
                  <div className="flex items-baseline gap-2">
                    <div className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">{stats.storageUsed.toFixed(1)} GB</div>
                    <span className="text-xs font-medium text-slate-400 flex items-center">
                      of 500 GB
                    </span>
                  </div>
                  
                  <div className="mt-4 w-full bg-slate-100 dark:bg-slate-800/80 rounded-full h-1.5 overflow-hidden shadow-inner">
                    <div 
                      className="bg-gradient-to-r from-amber-400 to-orange-500 h-full rounded-full transition-all duration-1000 ease-out shadow-[0_0_10px_rgba(245,158,11,0.5)]"
                      style={{ width: `${Math.min((stats.storageUsed / 500) * 100, 100)}%` }}
                    ></div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Charts / Activity Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <Card className="col-span-1 lg:col-span-2 border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Activity className="w-5 h-5 text-indigo-500" />
                    System Activity
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-64 flex flex-col items-center justify-center relative overflow-hidden">
                    {activityLoading ? (
                      <Skeleton className="w-full h-full rounded-xl" />
                    ) : activity.length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={activity}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="currentColor" className="opacity-10 dark:opacity-20" />
                          <XAxis dataKey="label" stroke="currentColor" className="text-xs opacity-50 dark:opacity-40" />
                          <YAxis stroke="currentColor" className="text-xs opacity-50 dark:opacity-40" />
                          <RechartsTooltip 
                            contentStyle={{ borderRadius: '8px', backgroundColor: 'var(--tw-prose-bg, rgba(255, 255, 255, 0.9))', color: '#333' }}
                            itemStyle={{ color: '#4f46e5' }}
                          />
                          <Bar dataKey="value" fill="#6366f1" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-sm text-slate-400 border border-dashed border-slate-200 dark:border-slate-800 rounded-xl bg-slate-50/50 dark:bg-slate-900/20">
                        No activity data available
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
              
              <Card className="col-span-1 border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm">
                <CardHeader>
                  <CardTitle>Recent Alerts</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {alerts.length > 0 ? alerts.map((alert, i) => (
                      <div key={i} className="flex gap-3 group">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 mt-0.5 ${alert.bg || 'bg-slate-100'} transition-transform group-hover:scale-110`}>
                          <div className={`w-2.5 h-2.5 rounded-full bg-current ${alert.color || 'text-slate-500'}`}></div>
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-slate-800 dark:text-slate-200">{alert.title}</h4>
                          <p className="text-xs text-slate-500 dark:text-slate-400">{alert.desc}</p>
                          <p className="text-[10px] text-slate-400 mt-1 font-medium">{alert.time}</p>
                        </div>
                      </div>
                    )) : (
                      <div className="text-sm text-slate-400">No recent alerts</div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="users" className="space-y-6">
            <Card className="border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm">
              <CardHeader>
                <CardTitle>User Management</CardTitle>
              </CardHeader>
              <CardContent>
                {usersLoading ? (
                  <div className="space-y-4">
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                ) : users.length > 0 ? (
                  <div className="rounded-md border dark:border-slate-800">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Name</TableHead>
                          <TableHead>Email</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {users.map((user) => (
                          <TableRow key={user.id}>
                            <TableCell className="font-medium">{user.name || 'Unnamed User'}</TableCell>
                            <TableCell>{user.email}</TableCell>
                            <TableCell>
                              <span className={`px-2 py-1 rounded-full text-xs font-medium ${user.status === 'ACTIVE' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>
                                {user.status}
                              </span>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button 
                                variant={user.status === 'ACTIVE' ? 'destructive' : 'default'} 
                                size="sm" 
                                className="h-8"
                                onClick={() => toggleUserStatus(user.id, user.status)}
                              >
                                {user.status === 'ACTIVE' ? (
                                  <><Ban className="w-3 h-3 mr-1" /> Suspend</>
                                ) : (
                                  <><CheckCircle className="w-3 h-3 mr-1" /> Activate</>
                                )}
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                ) : (
                  <div className="text-center py-8 text-slate-500">
                    No users found
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="workspaces" className="space-y-6">
            <Card className="border-slate-200/60 dark:border-slate-800/60 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl shadow-sm">
              <CardHeader>
                <CardTitle>Workspace Management</CardTitle>
              </CardHeader>
              <CardContent>
                {workspacesLoading ? (
                  <div className="space-y-4">
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                ) : workspaces.length > 0 ? (
                  <div className="rounded-md border dark:border-slate-800">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Name</TableHead>
                          <TableHead>Owner ID</TableHead>
                          <TableHead>Created At</TableHead>
                          <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {workspaces.map((ws) => (
                          <TableRow key={ws.id}>
                            <TableCell className="font-medium">{ws.name}</TableCell>
                            <TableCell className="text-xs text-slate-500">{ws.ownerId}</TableCell>
                            <TableCell>{new Date(ws.createdAt).toLocaleDateString()}</TableCell>
                            <TableCell className="text-right">
                              <Button 
                                variant="destructive" 
                                size="sm" 
                                className="h-8"
                                onClick={() => deleteWorkspace(ws.id)}
                              >
                                <Trash2 className="w-3 h-3 mr-1" /> Delete
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                ) : (
                  <div className="text-center py-8 text-slate-500">
                    No workspaces found
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}


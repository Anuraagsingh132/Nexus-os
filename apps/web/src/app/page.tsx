"use client"

import Link from "next/link"
import { buttonVariants } from "@/components/ui/button"
import { ArrowRight, Bot, CheckCircle, MessagesSquare, FolderKanban, Sparkles } from "lucide-react"
import { motion } from "framer-motion"

export default function Home() {
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: { staggerChildren: 0.1, delayChildren: 0.2 }
    }
  }

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5 } }
  }

  return (
    <div className="min-h-full bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-50 selection:bg-indigo-500/30">
      
      {/* Hero Section */}
      <section className="relative pt-24 pb-32 overflow-hidden">
        {/* Background Gradients */}
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-100 via-slate-50 to-slate-50 dark:from-indigo-900/20 dark:via-slate-950 dark:to-slate-950"></div>
        <div className="absolute top-0 right-0 -translate-y-12 translate-x-1/3">
          <div className="w-[600px] h-[600px] bg-indigo-500/10 dark:bg-indigo-500/5 blur-[120px] rounded-full"></div>
        </div>
        <div className="absolute bottom-0 left-0 translate-y-1/3 -translate-x-1/3">
          <div className="w-[500px] h-[500px] bg-purple-500/10 dark:bg-purple-500/5 blur-[100px] rounded-full"></div>
        </div>

        <div className="max-w-6xl mx-auto px-6 text-center">
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 text-sm font-medium mb-8 border border-indigo-100 dark:border-indigo-500/20 shadow-sm"
          >
            <Sparkles className="w-4 h-4" />
            <span>Introducing Nexus OS 1.0</span>
          </motion.div>

          <motion.h1 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-5xl md:text-7xl font-extrabold tracking-tight mb-8 text-balance"
          >
            The Agent-Native <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-400 dark:to-purple-400">
              Operating System
            </span>
          </motion.h1>

          <motion.p 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-lg md:text-xl text-slate-600 dark:text-slate-400 max-w-2xl mx-auto mb-10 text-balance leading-relaxed"
          >
            Unify your projects, knowledge, and team chat in one intelligent workspace. Powered by AI agents that work alongside you to accelerate your software delivery.
          </motion.p>

          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4"
          >
            <Link 
              href="/signup" 
              className={buttonVariants({ size: "lg", className: "h-12 px-8 text-base font-medium rounded-full shadow-lg shadow-indigo-500/20 hover:shadow-indigo-500/30 transition-all" })}
            >
              Get Started for Free <ArrowRight className="ml-2 w-4 h-4" />
            </Link>
            <Link 
              href="/login" 
              className={buttonVariants({ size: "lg", variant: "outline", className: "h-12 px-8 text-base font-medium rounded-full bg-white/50 dark:bg-slate-900/50 backdrop-blur-sm border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800" })}
            >
              Sign In to Workspace
            </Link>
          </motion.div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-24 bg-white dark:bg-slate-900/50 border-t border-slate-100 dark:border-slate-800">
        <div className="max-w-6xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">Everything your team needs.</h2>
            <p className="text-slate-500 dark:text-slate-400">No more context switching. Nexus OS brings it all together.</p>
          </div>

          <motion.div 
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-100px" }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"
          >
            <motion.div variants={itemVariants} className="p-8 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 hover:shadow-xl hover:shadow-slate-200/50 dark:hover:shadow-none transition-all group">
              <div className="w-12 h-12 bg-indigo-100 dark:bg-indigo-500/20 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <FolderKanban className="w-6 h-6 text-indigo-600 dark:text-indigo-400" />
              </div>
              <h3 className="text-xl font-bold mb-3">Project Management</h3>
              <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
                Organize work with powerful Kanban boards. Track tasks, set priorities, and monitor progress in real-time.
              </p>
            </motion.div>

            <motion.div variants={itemVariants} className="p-8 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 hover:shadow-xl hover:shadow-slate-200/50 dark:hover:shadow-none transition-all group">
              <div className="w-12 h-12 bg-purple-100 dark:bg-purple-500/20 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <MessagesSquare className="w-6 h-6 text-purple-600 dark:text-purple-400" />
              </div>
              <h3 className="text-xl font-bold mb-3">Real-time Chat</h3>
              <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
                Connect instantly with your team. Dedicated channels, direct messages, and rich media sharing.
              </p>
            </motion.div>

            <motion.div variants={itemVariants} className="p-8 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 hover:shadow-xl hover:shadow-slate-200/50 dark:hover:shadow-none transition-all group">
              <div className="w-12 h-12 bg-blue-100 dark:bg-blue-500/20 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Bot className="w-6 h-6 text-blue-600 dark:text-blue-400" />
              </div>
              <h3 className="text-xl font-bold mb-3">Agentic AI</h3>
              <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
                Interact with intelligent agents that can query your documents, summarize chats, and automate your workflow.
              </p>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* Social Proof / Footer CTA */}
      <section className="py-24 relative overflow-hidden">
        <div className="absolute inset-0 bg-indigo-600 dark:bg-indigo-900"></div>
        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-10"></div>
        <div className="relative max-w-4xl mx-auto px-6 text-center text-white">
          <h2 className="text-3xl md:text-5xl font-bold mb-6 tracking-tight">Ready to transform how you work?</h2>
          <p className="text-indigo-100 text-lg mb-10 max-w-2xl mx-auto">
            Join modern software teams who are already building faster and smarter with Nexus OS.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link 
              href="/signup" 
              className={buttonVariants({ size: "lg", variant: "secondary", className: "h-14 px-8 text-indigo-900 bg-white hover:bg-slate-50 rounded-full font-semibold text-lg w-full sm:w-auto" })}
            >
              Start your free workspace
            </Link>
          </div>
          <div className="mt-10 flex items-center justify-center gap-6 text-sm text-indigo-200">
            <span className="flex items-center gap-2"><CheckCircle className="w-4 h-4" /> No credit card required</span>
            <span className="flex items-center gap-2"><CheckCircle className="w-4 h-4" /> Setup in 2 minutes</span>
          </div>
        </div>
      </section>

    </div>
  )
}

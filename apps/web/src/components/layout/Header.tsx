import Link from "next/link"
import { Button } from "@/components/ui/button"

export function Header() {
  return (
    <header className="flex items-center justify-between px-6 py-4 border-b bg-white dark:bg-slate-950">
      <div className="flex items-center gap-4">
        <Link href="/" className="font-bold text-xl tracking-tight text-indigo-600 dark:text-indigo-400">
          Nexus OS
        </Link>
      </div>
      <div className="flex items-center gap-4">
        <Link href="/login">
          <Button variant="ghost">Log in</Button>
        </Link>
        <Link href="/signup">
          <Button>Sign up</Button>
        </Link>
      </div>
    </header>
  )
}

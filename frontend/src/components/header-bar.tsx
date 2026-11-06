"use client";

import { Boxes } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { ByokSettingsDialog } from "@/components/byok-settings-dialog";
import { HistoryDrawer } from "@/components/history-drawer";

export function HeaderBar({ onOpenJob }: { onOpenJob: (jobId: string) => void }) {
  return (
    <header className="fixed inset-x-0 top-0 z-40 h-16 border-b border-line bg-canvas/80 backdrop-blur-md">
      <div className="container flex h-full max-w-[1200px] items-center justify-between">
        <a href="#top" className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-ctl bg-primary text-primary-foreground">
            <Boxes className="h-4 w-4" />
          </span>
          <span className="text-[15px] font-semibold tracking-tight">SpringDocAI</span>
          <Badge variant="outline" className="hidden sm:inline-flex">
            Spring Boot AST
          </Badge>
        </a>
        <nav className="hidden items-center gap-6 text-[13px] font-medium text-ink-2 md:flex" aria-label="Seções">
          <a href="#comparativo" className="hover:text-ink">Comparativo</a>
          <a href="#como-funciona" className="hover:text-ink">Como funciona</a>
          <a href="#amostra" className="hover:text-ink">Amostra</a>
          <a href="#privacidade" className="hover:text-ink">Privacidade</a>
        </nav>
        <div className="flex items-center gap-2">
          <HistoryDrawer onOpenJob={onOpenJob} />
          <ByokSettingsDialog />
        </div>
      </div>
    </header>
  );
}


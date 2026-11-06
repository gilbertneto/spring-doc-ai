"use client";

import { useCallback, useState } from "react";
import { History, Github, FileArchive, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { getHistory, type GenerationJob, type JobStatus } from "@/lib/api-client";

const STATUS_BADGE: Record<JobStatus, { label: string; variant: "get" | "accent" | "delete" | "neutral" }> = {
  COMPLETED: { label: "Concluído", variant: "get" },
  GENERATING_DOCS: { label: "Gerando", variant: "accent" },
  PARSING_AST: { label: "AST", variant: "accent" },
  PENDING: { label: "Pendente", variant: "neutral" },
  FAILED: { label: "Falhou", variant: "delete" },
};

export function HistoryDrawer({ onOpenJob }: { onOpenJob: (jobId: string) => void }) {
  const [open, setOpen] = useState(false);
  const [jobs, setJobs] = useState<GenerationJob[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setJobs(null);
    setError(null);
    try {
      setJobs(await getHistory());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Falha ao carregar o histórico.");
    }
  }, []);

  function onOpenChange(next: boolean) {
    setOpen(next);
    if (next) void load();
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetTrigger asChild>
        <Button variant="outline" size="sm">
          <History className="h-4 w-4" />
          Histórico
        </Button>
      </SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>Histórico de Gerações</SheetTitle>
          <SheetDescription>Últimas 20 gerações salvas. Apenas metadados e os 3 Markdowns são persistidos.</SheetDescription>
        </SheetHeader>
        <div className="flex justify-end">
          <Button variant="ghost" size="sm" onClick={() => void load()}>
            <RefreshCw className="h-3.5 w-3.5" /> Atualizar
          </Button>
        </div>
        <ScrollArea className="-mx-2 flex-1 px-2">
          {error && <p className="rounded-ctl border border-del-border bg-del-soft p-3 text-sm">{error}</p>}
          {!jobs && !error && (
            <div className="space-y-2">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-16" />
              ))}
            </div>
          )}
          {jobs && jobs.length === 0 && <p className="py-8 text-center text-sm text-ink-3">Nenhuma geração ainda.</p>}
          <ul className="space-y-2">
            {jobs?.map((job) => {
              const s = STATUS_BADGE[job.status];
              const Icon = job.sourceType === "GITHUB_URL" ? Github : FileArchive;
              return (
                <li key={job.id}>
                  <button
                    type="button"
                    onClick={() => {
                      setOpen(false);
                      onOpenJob(job.id);
                      toast.message(`Abrindo ${job.projectName}…`);
                    }}
                    className="w-full rounded-card border border-line bg-surface p-3 text-left transition-colors hover:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="flex min-w-0 items-center gap-2 text-sm font-medium">
                        <Icon className="h-4 w-4 shrink-0 text-ink-3" />
                        <span className="truncate font-mono">{job.projectName}</span>
                      </span>
                      <Badge variant={s.variant}>{s.label}</Badge>
                    </div>
                    <div className="mt-1.5 flex items-center gap-3 text-xs text-ink-3">
                      <span className="num">{new Date(job.createdAt).toLocaleString("pt-BR")}</span>
                      {job.endpointsCount !== null && <span className="num">{job.endpointsCount} endpoints</span>}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
}


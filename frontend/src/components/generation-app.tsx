"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { AstProgressStepper } from "@/components/ast-progress-stepper";
import { HeaderBar } from "@/components/header-bar";
import { HeroExtractorCard } from "@/components/hero-extractor-card";
import { MarkdownWorkspaceTabs } from "@/components/markdown-workspace-tabs";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { TooltipProvider } from "@/components/ui/tooltip";
import {
  createGithubGeneration,
  createUploadGeneration,
  getGeneration,
  type GenerationDetail,
  type GenerationJob,
} from "@/lib/api-client";
import { AlertTriangle } from "lucide-react";

const POLL_MS = 1500;
const MAX_POLL_ERRORS = 5;

function isRunning(job: GenerationDetail | null) {
  return job !== null && (job.status === "PENDING" || job.status === "PARSING_AST" || job.status === "GENERATING_DOCS");
}

export function GenerationApp({ children }: { children: ReactNode }) {
  const [job, setJob] = useState<GenerationDetail | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const workspaceRef = useRef<HTMLDivElement>(null);
  const pollErrors = useRef(0);
  const notified = useRef<string | null>(null);

  const jobId = job?.id;
  const running = isRunning(job);

  // Polling de GET /{jobId} enquanto o job não estiver terminal.
  useEffect(() => {
    if (!jobId || !running) return;
    const timer = setTimeout(async () => {
      try {
        const detail = await getGeneration(jobId);
        pollErrors.current = 0;
        setJob(detail);
      } catch (e) {
        pollErrors.current += 1;
        if (pollErrors.current >= MAX_POLL_ERRORS) {
          setError(e instanceof Error ? e.message : "Conexão perdida com o backend.");
          setJob((j) => (j ? { ...j, status: "FAILED", errorMessage: "Conexão perdida com o backend." } : j));
        } else {
          setJob((j) => (j ? { ...j } : j)); // força novo ciclo de polling
        }
      }
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [jobId, running, job]);

  // Toasts de transição de estado (uma vez por job).
  useEffect(() => {
    if (!job || notified.current === `${job.id}:${job.status}`) return;
    notified.current = `${job.id}:${job.status}`;
    if (job.status === "GENERATING_DOCS" && job.endpointsCount !== null) {
      toast.success(`AST extraída com sucesso (${job.endpointsCount} endpoints)`);
    } else if (job.status === "COMPLETED") {
      toast.success("Documentação salva e pronta para download");
    }
  }, [job]);

  const begin = useCallback(async (start: () => Promise<GenerationJob>) => {
    setSubmitting(true);
    setError(null);
    pollErrors.current = 0;
    try {
      const created = await start();
      setJob({ ...created, documents: [] });
      setTimeout(() => workspaceRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }), 100);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Erro inesperado.";
      setError(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  }, []);

  const openJob = useCallback(async (id: string) => {
    setError(null);
    try {
      setJob(await getGeneration(id));
      setTimeout(() => workspaceRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }), 100);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Não foi possível abrir o job.");
    }
  }, []);

  return (
    <TooltipProvider delayDuration={150}>
      <HeaderBar onOpenJob={openJob} />
      <main id="top" className="pt-16">
        <section className="container max-w-[768px] pb-16 pt-20 text-center">
          <Badge variant="outline" className="mx-auto rounded-full px-3 py-1 text-xs font-medium">
            Java 11–21 • Spring Boot 2 &amp; 3
          </Badge>
          <h1 className="mt-5 text-[2.5rem] font-bold leading-[1.15] tracking-[-0.03em]">
            Documentação de integração real,
            <br />
            extraída do seu código Spring.
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-[15px] text-ink-2">
            Cole um repositório ou solte um <span className="font-mono">.zip</span> e receba{" "}
            <span className="font-mono">API-REFERENCE.md</span>, <span className="font-mono">INTEGRATION-GUIDE.md</span> e{" "}
            <span className="font-mono">ARCHITECTURE.md</span>.
          </p>
          <div className="mt-8 text-left">
            <HeroExtractorCard
              busy={submitting || running}
              onGithub={(url) => begin(() => createGithubGeneration(url))}
              onUpload={(name, files) => begin(() => createUploadGeneration(name, files))}
            />
            {error && !job && (
              <Alert variant="destructive" className="mt-4">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-del" />
                <div>
                  <AlertTitle>Não foi possível iniciar a geração</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </div>
              </Alert>
            )}
          </div>
        </section>

        {job && (
          <section ref={workspaceRef} id="workspace" className="container max-w-[1024px] scroll-mt-20 space-y-4 pb-20">
            <AstProgressStepper job={job} />
            <Card className="overflow-hidden shadow-float">
              <MarkdownWorkspaceTabs documents={job.documents} generating={running} projectName={job.projectName} />
            </Card>
          </section>
        )}

        {children}
      </main>
    </TooltipProvider>
  );
}


"use client";

import { AlertTriangle, CheckCircle2, Loader2, Circle } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import type { GenerationDetail } from "@/lib/api-client";
import { cn } from "@/lib/utils";

const STEPS = ["Extraindo AST", "Gerando Doc Técnica", "Gerando Guia de Casos de Uso", "Gerando Arquitetura"] as const;

/** 0..4: quantos passos já foram concluídos (o passo atual é o índice retornado quando < 4). */
function completedSteps(job: GenerationDetail): number {
  if (job.status === "PENDING" || job.status === "PARSING_AST") return 0;
  return 1 + Math.min(job.documents.length, 3);
}

export function AstProgressStepper({ job }: { job: GenerationDetail }) {
  const done = completedSteps(job);
  const failed = job.status === "FAILED";
  const finished = job.status === "COMPLETED";
  const percent = finished ? 100 : Math.round((done / STEPS.length) * 100);

  return (
    <Card className="shadow-float">
      <CardContent className="space-y-4 p-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm font-semibold">
            {finished ? "Documentação pronta" : failed ? "Geração interrompida" : "Processando…"}{" "}
            <span className="font-mono text-xs font-normal text-ink-3">{job.projectName}</span>
          </p>
          <div className="flex items-center gap-2">
            {job.endpointsCount !== null && (
              <Badge variant="accent">
                <span className="num">{job.endpointsCount}</span>&nbsp;endpoints
              </Badge>
            )}
            {job.classesAnalyzedCount !== null && (
              <Badge variant="neutral">
                <span className="num">{job.classesAnalyzedCount}</span>&nbsp;classes
              </Badge>
            )}
          </div>
        </div>
        <Progress value={percent} aria-label="Progresso da geração" className={failed ? "[&>div]:bg-del" : undefined} />
        <ol className="grid gap-2 sm:grid-cols-4">
          {STEPS.map((label, i) => {
            const isDone = i < done;
            const isCurrent = i === done && !finished && !failed;
            return (
              <li
                key={label}
                className={cn(
                  "flex items-center gap-2 rounded-ctl border px-3 py-2 text-[13px]",
                  isDone ? "border-get-border bg-get-soft text-ink" : isCurrent ? "border-accent-border bg-accent-soft text-ink" : "border-line text-ink-3",
                )}
              >
                {isDone ? (
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-get" />
                ) : isCurrent ? (
                  <Loader2 className="h-4 w-4 shrink-0 animate-spin text-accent" />
                ) : (
                  <Circle className="h-4 w-4 shrink-0" />
                )}
                <span className="font-medium">{label}</span>
              </li>
            );
          })}
        </ol>
        {failed && (
          <Alert variant="destructive">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-del" />
            <div>
              <AlertTitle>A geração falhou</AlertTitle>
              <AlertDescription>
                {job.errorMessage ?? "Erro inesperado."}
                {job.documents.length > 0 && " Os documentos já concluídos foram preservados abaixo."}
              </AlertDescription>
            </div>
          </Alert>
        )}
      </CardContent>
    </Card>
  );
}


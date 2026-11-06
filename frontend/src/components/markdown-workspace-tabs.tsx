"use client";

import { useState } from "react";
import { Check, Copy, Download, Eye, FileCode2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { MarkdownViewer } from "@/components/markdown-viewer";
import type { DocType, GeneratedDocument } from "@/lib/api-client";
import { DOC_ORDER, downloadDocsZip } from "@/lib/download-zip";
import { cn } from "@/lib/utils";

const FILE_NAMES: Record<DocType, string> = {
  API_REFERENCE: "API-REFERENCE.md",
  INTEGRATION_GUIDE: "INTEGRATION-GUIDE.md",
  ARCHITECTURE_OVERVIEW: "ARCHITECTURE.md",
};

interface Props {
  documents: GeneratedDocument[];
  /** documentos ainda sendo gerados exibem Skeleton */
  generating?: boolean;
  projectName: string;
  scrollClassName?: string;
}

export function MarkdownWorkspaceTabs({ documents, generating = false, projectName, scrollClassName }: Props) {
  const [raw, setRaw] = useState(false);
  const [copied, setCopied] = useState<DocType | null>(null);
  const byType = new Map(documents.map((d) => [d.docType, d]));
  const complete = DOC_ORDER.every((t) => byType.has(t));

  async function copy(doc: GeneratedDocument) {
    try {
      await navigator.clipboard.writeText(doc.content);
      setCopied(doc.docType);
      setTimeout(() => setCopied(null), 1800);
    } catch {
      toast.error("Não foi possível copiar. Verifique as permissões do navegador.");
    }
  }

  async function download() {
    await downloadDocsZip(documents, projectName);
    toast.success("Pacote .zip gerado no navegador.");
  }

  return (
    <Tabs defaultValue="API_REFERENCE" className="w-full">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line bg-surface-2/60 px-3 py-2">
        <TabsList className="bg-transparent p-0">
          {DOC_ORDER.map((type, i) => (
            <TabsTrigger key={type} value={type} className="font-mono text-xs">
              {i + 1}. {FILE_NAMES[type]}
              {!byType.has(type) && generating && <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />}
            </TabsTrigger>
          ))}
        </TabsList>
        <div className="flex items-center gap-2">
          <div className="inline-flex rounded-ctl border border-line bg-surface p-0.5" role="group" aria-label="Modo de visualização">
            <button
              type="button"
              onClick={() => setRaw(false)}
              className={cn("inline-flex items-center gap-1.5 rounded-[4px] px-2.5 py-1 text-xs font-medium", !raw ? "bg-primary text-primary-foreground" : "text-ink-2")}
            >
              <Eye className="h-3.5 w-3.5" /> Preview
            </button>
            <button
              type="button"
              onClick={() => setRaw(true)}
              className={cn("inline-flex items-center gap-1.5 rounded-[4px] px-2.5 py-1 text-xs font-medium", raw ? "bg-primary text-primary-foreground" : "text-ink-2")}
            >
              <FileCode2 className="h-3.5 w-3.5" /> Raw
            </button>
          </div>
          <Button size="sm" onClick={download} disabled={documents.length === 0}>
            <Download className="h-3.5 w-3.5" />
            Baixar Pacote (.zip){!complete && documents.length > 0 ? ` · ${documents.length}/3` : ""}
          </Button>
        </div>
      </div>

      {DOC_ORDER.map((type) => {
        const doc = byType.get(type);
        return (
          <TabsContent key={type} value={type} className="m-0">
            {doc ? (
              <div>
                <div className="flex items-center justify-between border-b border-line px-4 py-2">
                  <span className="font-mono text-xs text-ink-2">{doc.fileName}</span>
                  <Tooltip open={copied === type ? true : undefined}>
                    <TooltipTrigger asChild>
                      <Button variant="outline" size="sm" onClick={() => copy(doc)}>
                        {copied === type ? <Check className="h-3.5 w-3.5 text-get" /> : <Copy className="h-3.5 w-3.5" />}
                        Copiar Markdown
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{copied === type ? "Copiado!" : "Copia o Markdown bruto"}</TooltipContent>
                  </Tooltip>
                </div>
                <ScrollArea className={cn("h-[calc(100vh-280px)] min-h-[420px]", scrollClassName)}>
                  <div className="p-6">
                    {raw ? (
                      <pre className="whitespace-pre-wrap break-words font-mono text-[13px] leading-relaxed text-ink">{doc.content}</pre>
                    ) : (
                      <MarkdownViewer content={doc.content} />
                    )}
                  </div>
                </ScrollArea>
              </div>
            ) : (
              <div className={cn("min-h-[420px] space-y-3 p-6", scrollClassName)} aria-busy={generating}>
                <p className="font-mono text-xs text-ink-3">
                  {generating ? `Gerando ${FILE_NAMES[type]}…` : `${FILE_NAMES[type]} não foi gerado.`}
                </p>
                {generating &&
                  [90, 70, 100, 55, 85, 65].map((w, i) => <Skeleton key={i} className="h-4" style={{ width: `${w}%` }} />)}
              </div>
            )}
          </TabsContent>
        );
      })}
    </Tabs>
  );
}


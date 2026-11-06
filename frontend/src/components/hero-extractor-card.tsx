"use client";

import { useRef, useState, type DragEvent } from "react";
import { AlertTriangle, FolderOpen, Github, Loader2, ShieldCheck, UploadCloud } from "lucide-react";
import { toast } from "sonner";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { filterDroppedItems, filterFileList, filterZipFile, type FilterResult } from "@/lib/client-zip-filter";
import { cn } from "@/lib/utils";

const GITHUB_RX = /^https:\/\/(www\.)?github\.com\/[\w.-]+\/[\w.-]+(\.git)?(\/tree\/[^\s?#]+)?\/?$/;

const EXAMPLES = [
  "spring-projects/spring-petclinic",
  "spring-petclinic/spring-petclinic-rest",
  "spring-guides/gs-rest-service",
];

const REASON_LABELS: Record<string, string> = {
  test: "src/test/",
  "build-output": "target/ · build/",
  "vcs-or-deps": ".git/ · node_modules/",
  "env-secret": ".env",
  binary: "imagens/binários",
  irrelevant: ".java irrelevantes",
  "too-large": "> 1 MB",
  other: "outros arquivos",
};

interface Props {
  busy: boolean;
  onGithub: (url: string) => void;
  onUpload: (projectName: string, files: Record<string, string>) => void;
}

export function HeroExtractorCard({ busy, onGithub, onUpload }: Props) {
  const [tab, setTab] = useState("github");
  const [url, setUrl] = useState("");
  const [urlError, setUrlError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [reading, setReading] = useState(false);
  const [result, setResult] = useState<FilterResult | null>(null);
  const [readError, setReadError] = useState<string | null>(null);
  const zipInput = useRef<HTMLInputElement>(null);
  const dirInput = useRef<HTMLInputElement>(null);

  function submitGithub(value = url) {
    const trimmed = value.trim();
    if (!GITHUB_RX.test(trimmed)) {
      setUrlError("Informe uma URL pública no formato https://github.com/org/repo");
      return;
    }
    setUrlError(null);
    onGithub(trimmed);
  }

  function pickExample(repo: string) {
    const full = `https://github.com/${repo}`;
    setUrl(full);
    submitGithub(full);
  }

  async function process(task: () => Promise<FilterResult>) {
    setReading(true);
    setReadError(null);
    setResult(null);
    try {
      const r = await task();
      setResult(r);
      toast.success(`${r.javaCount} classes filtradas no navegador`);
    } catch (e) {
      setReadError(e instanceof Error ? e.message : "Não foi possível ler o arquivo.");
    } finally {
      setReading(false);
    }
  }

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragging(false);
    // `items` precisa ser lido de forma síncrona antes de qualquer await.
    const items = e.dataTransfer.items;
    void process(() => filterDroppedItems(items));
  }

  const noController = result && result.javaCount > 0 && !(result.categories["Controller"] > 0);

  return (
    <Card className="shadow-float">
      <Tabs value={tab} onValueChange={setTab}>
        <div className="p-4 pb-0">
          <TabsList>
            <TabsTrigger value="github">
              <Github className="h-3.5 w-3.5" /> Colar Link do GitHub
            </TabsTrigger>
            <TabsTrigger value="zip">
              <UploadCloud className="h-3.5 w-3.5" /> Subir Arquivo .zip
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="github" className="p-4">
          <form
            className="flex flex-col gap-2 sm:flex-row"
            onSubmit={(e) => {
              e.preventDefault();
              submitGithub();
            }}
          >
            <Input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://github.com/org/repo"
              aria-label="URL do repositório GitHub"
              aria-invalid={Boolean(urlError)}
              className="h-11 font-mono"
              disabled={busy}
            />
            <Button type="submit" className="h-11 sm:px-6" disabled={busy || !url.trim()}>
              {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Gerar Documentação
            </Button>
          </form>
          {urlError && <p className="mt-2 text-[13px] text-del">{urlError}</p>}
        </TabsContent>

        <TabsContent value="zip" className="space-y-3 p-4">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={onDrop}
            className={cn(
              "flex flex-col items-center justify-center gap-2 rounded-ctl border border-dashed px-4 py-7 text-center transition-colors",
              dragging ? "border-primary bg-surface-2" : "border-line hover:bg-surface-2",
            )}
          >
            {reading ? <Loader2 className="h-6 w-6 animate-spin text-accent" /> : <UploadCloud className="h-6 w-6 text-ink-3" />}
            <p className="text-sm font-medium">
              Arraste a pasta <span className="font-mono">src/</span> ou arquivo <span className="font-mono">.zip</span>
            </p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={busy || reading} onClick={() => zipInput.current?.click()}>
                Escolher .zip
              </Button>
              <Button variant="outline" size="sm" disabled={busy || reading} onClick={() => dirInput.current?.click()}>
                <FolderOpen className="h-3.5 w-3.5" /> Escolher pasta
              </Button>
            </div>
            <input
              ref={zipInput}
              type="file"
              accept=".zip,application/zip"
              hidden
              onChange={(e) => {
                const f = e.target.files?.[0];
                e.target.value = "";
                if (f) void process(() => filterZipFile(f));
              }}
            />
            <input
              ref={dirInput}
              type="file"
              hidden
              multiple
              {...({ webkitdirectory: "" } as Record<string, string>)}
              onChange={(e) => {
                const list = Array.from(e.target.files ?? []);
                e.target.value = "";
                if (list.length) void process(() => filterFileList(list));
              }}
            />
          </div>

          {readError && (
            <Alert variant="destructive">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-del" />
              <AlertDescription className="mt-0 text-ink">{readError}</AlertDescription>
            </Alert>
          )}

          {result && (
            <div className="space-y-3 rounded-ctl border border-line bg-surface-2/60 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm">
                  <span className="num text-2xl font-bold" data-testid="java-count">{result.javaCount}</span>{" "}
                  <span className="text-ink-2">classes</span> <span className="font-mono text-xs">.java</span>{" "}
                  <span className="text-ink-2">prontas para envio</span>
                </p>
                <Badge variant={result.manifest ? "get" : "outline"}>{result.manifest ?? "sem pom.xml/build.gradle"}</Badge>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {Object.entries(result.categories).map(([cat, n]) => (
                  <Badge key={cat} variant="accent">
                    <span className="num">{n}</span>&nbsp;{cat}
                  </Badge>
                ))}
              </div>
              <p className="text-xs text-ink-3">
                <span className="num">{result.discardedCount}</span> de <span className="num">{result.totalEntries}</span> arquivos descartados no navegador:{" "}
                {Object.entries(result.discarded)
                  .filter(([, n]) => n > 0)
                  .map(([k, n]) => `${n} ${REASON_LABELS[k]}`)
                  .join(" · ") || "nenhum"}
              </p>
              {result.javaCount === 0 && (
                <Alert variant="warning">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-put" />
                  <div>
                    <AlertTitle>Nenhuma classe Java relevante encontrada</AlertTitle>
                    <AlertDescription>Envie a pasta do projeto ou um .zip que contenha <span className="font-mono">src/main/java</span>.</AlertDescription>
                  </div>
                </Alert>
              )}
              {noController && (
                <Alert variant="warning">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-put" />
                  <div>
                    <AlertTitle>Nenhum <span className="font-mono">@RestController</span> detectado</AlertTitle>
                    <AlertDescription>O backend recusará projetos sem endpoints Spring Web.</AlertDescription>
                  </div>
                </Alert>
              )}
              <Button
                className="w-full"
                disabled={busy || result.javaCount === 0}
                onClick={() => onUpload(result.projectName, result.files)}
              >
                {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                Gerar Documentação
              </Button>
            </div>
          )}
        </TabsContent>
      </Tabs>

      <CardFooter className="flex-col items-start gap-3 border-t border-line pt-4">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-ink-3">Testar com exemplo:</span>
          {EXAMPLES.map((repo) => (
            <button
              key={repo}
              type="button"
              disabled={busy}
              onClick={() => {
                setTab("github");
                pickExample(repo);
              }}
              className="rounded-full border border-line bg-surface-3/60 px-3 py-1 font-mono text-xs text-ink-2 transition-colors hover:border-primary hover:text-ink disabled:opacity-50"
            >
              {repo}
            </button>
          ))}
        </div>
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="inline-flex cursor-default items-center gap-1.5 text-xs text-ink-3">
              <ShieldCheck className="h-3.5 w-3.5 text-get" /> Código processado em RAM e descartado
            </span>
          </TooltipTrigger>
          <TooltipContent>O código-fonte nunca é gravado em disco nem no banco de dados.</TooltipContent>
        </Tooltip>
      </CardFooter>
    </Card>
  );
}

"use client";

import { useState } from "react";
import { KeyRound, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { loadByok, saveByok, type ByokSettings } from "@/lib/api-client";
import { cn } from "@/lib/utils";

const PROVIDERS: { id: ByokSettings["provider"]; label: string }[] = [
  { id: "openai", label: "OpenAI" },
  { id: "gemini", label: "Gemini" },
  { id: "other", label: "Outro (compatível OpenAI)" },
];

export function ByokSettingsDialog() {
  const [open, setOpen] = useState(false);
  const [provider, setProvider] = useState<ByokSettings["provider"]>("openai");
  const [apiKey, setApiKey] = useState("");
  const [hasKey, setHasKey] = useState(false);

  function onOpenChange(next: boolean) {
    if (next) {
      const saved = loadByok();
      setProvider(saved?.provider ?? "openai");
      setApiKey(saved?.apiKey ?? "");
      setHasKey(Boolean(saved?.apiKey));
    }
    setOpen(next);
  }

  function save() {
    saveByok({ provider, apiKey });
    setHasKey(Boolean(apiKey.trim()));
    toast.success(apiKey.trim() ? "Chave salva neste navegador." : "Chave removida.");
    setOpen(false);
  }

  function clear() {
    saveByok(null);
    setApiKey("");
    setHasKey(false);
    toast.success("Chave removida.");
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <Tooltip>
        <TooltipTrigger asChild>
          <DialogTrigger asChild>
            <Button variant="ghost" size="icon" aria-label="Configurar API Key (BYOK)" className="relative">
              <KeyRound className="h-4 w-4" />
              {hasKey && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-get" />}
            </Button>
          </DialogTrigger>
        </TooltipTrigger>
        <TooltipContent>API Key (BYOK)</TooltipContent>
      </Tooltip>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>API Key própria (BYOK)</DialogTitle>
          <DialogDescription>
            Opcional. Sem chave, o backend usa a chave padrão do servidor ou o modo Mock.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4">
          <div className="grid gap-1.5">
            <span className="text-[13px] font-medium">Provedor</span>
            <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Provedor">
              {PROVIDERS.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  role="radio"
                  aria-checked={provider === p.id}
                  onClick={() => setProvider(p.id)}
                  className={cn(
                    "rounded-ctl border px-3 py-1.5 text-[13px] font-medium",
                    provider === p.id ? "border-primary bg-primary text-primary-foreground" : "border-line text-ink-2 hover:border-primary",
                  )}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>
          <div className="grid gap-1.5">
            <label htmlFor="byok-key" className="text-[13px] font-medium">
              API Key
            </label>
            <Input
              id="byok-key"
              type="password"
              autoComplete="off"
              placeholder="sk-…"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="font-mono"
            />
          </div>
          <p className="flex items-start gap-2 rounded-ctl bg-surface-2 p-3 text-xs text-ink-2">
            <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-get" />
            A chave fica apenas no <span className="font-mono">localStorage</span> deste navegador e é enviada no header{" "}
            <span className="font-mono">X-LLM-API-Key</span> a cada geração. O servidor não a grava nem a registra em logs.
          </p>
          <div className="flex justify-end gap-2">
            {hasKey && (
              <Button variant="ghost" onClick={clear}>
                Remover
              </Button>
            )}
            <Button onClick={save}>Salvar</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}


import { ArrowRight, Cpu, FileText, Filter, ShieldCheck, Database, MemoryStick, Trash2, TriangleAlert, CheckCircle2 } from "lucide-react";
import { Badge, HttpMethodBadge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";

export function ComparisonSection() {
  return (
    <section id="comparativo" className="container max-w-[1100px] scroll-mt-24 py-20">
      <h2 className="text-2xl font-semibold tracking-[-0.02em]">Swagger cru vs. documentação contextual</h2>
      <p className="mt-2 max-w-2xl text-ink-2">
        Uma lista de rotas não explica a ordem das chamadas nem as regras escondidas nos services. A AST revela ambas.
      </p>
      <div className="mt-8 grid gap-5 md:grid-cols-2">
        <Card className="border-line bg-surface-2/70 p-5 opacity-80">
          <div className="mb-4 flex items-center justify-between">
            <span className="text-sm font-semibold text-ink-2">Swagger genérico</span>
            <Badge variant="delete"><TriangleAlert className="mr-1 h-3 w-3" />sem contexto</Badge>
          </div>
          <div className="flex items-center gap-2 font-mono text-sm">
            <HttpMethodBadge method="POST" /> /api/v1/faturamento
          </div>
          <pre className="mt-4 overflow-x-auto rounded-ctl border border-line bg-surface p-3 font-mono text-[13px] text-ink-2">{`{
  "cliente": 0,
  "valor": 0,
  "tipo": "string"
}`}</pre>
          <p className="mt-3 text-[13px] text-ink-3">Apenas tipos primitivos. Qual cliente? Qual tipo é válido? O que vem antes?</p>
        </Card>

        <Card className="border-primary p-5">
          <div className="mb-4 flex items-center justify-between">
            <span className="text-sm font-semibold">Documentação contextual</span>
            <Badge variant="get"><CheckCircle2 className="mr-1 h-3 w-3" />regras extraídas</Badge>
          </div>
          <div className="flex items-center gap-2 font-mono text-sm">
            <HttpMethodBadge method="POST" /> /api/v1/faturamento
          </div>
          <ul className="mt-4 space-y-2 text-[13px] text-ink-2">
            <li>
              <b className="text-ink">Pré-requisito:</b> o pedido deve estar <span className="font-mono">CONFIRMADO</span> (
              <span className="font-mono">POST /api/v1/pedidos/{"{id}"}/confirmar</span>).
            </li>
            <li>
              <b className="text-ink">Regra do Service:</b> <span className="font-mono">FaturamentoService.emitir</span> lança{" "}
              <span className="font-mono">PedidoNaoConfirmadoException</span> → <span className="font-mono">422</span>.
            </li>
          </ul>
          <pre className="mt-4 overflow-x-auto rounded-ctl border border-line bg-surface-2 p-3 font-mono text-[13px]">{`{
  "pedidoId": 1001,        // @NotNull — id do pedido confirmado
  "tipo": "NFE",           // @Pattern(NFE|NFSE)
  "valor": 1590.90         // @Positive
}`}</pre>
        </Card>
      </div>
    </section>
  );
}

const ANNOTATIONS = ["@RestController", "@Service", "@Entity", "@Valid"];

export function PipelineSection() {
  const steps = [
    {
      icon: Filter,
      title: "1. Triagem no navegador",
      body: (
        <>
          <p className="text-[13px] text-ink-2">Descartado antes do upload:</p>
          <p className="mt-1 font-mono text-xs text-del line-through decoration-del/40">src/test/ · target/ · .git/ · .env · *.jar</p>
          <p className="mt-2 text-[13px] text-ink-2">Enviado:</p>
          <p className="mt-1 font-mono text-xs text-get">*Controller.java · *Service.java · *Dto.java · pom.xml</p>
        </>
      ),
    },
    {
      icon: Cpu,
      title: "2. Extração AST (JavaParser)",
      body: (
        <>
          <p className="text-[13px] text-ink-2">Análise sintática real — não um prompt genérico. Reconhece:</p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {ANNOTATIONS.map((a) => (
              <Badge key={a} variant="accent">{a}</Badge>
            ))}
          </div>
        </>
      ),
    },
    {
      icon: FileText,
      title: "3. Síntese em 3 camadas",
      body: (
        <ul className="space-y-1 font-mono text-xs text-ink-2">
          <li>API-REFERENCE.md</li>
          <li>INTEGRATION-GUIDE.md</li>
          <li>ARCHITECTURE.md</li>
        </ul>
      ),
    },
  ];
  return (
    <section id="como-funciona" className="container max-w-[1100px] scroll-mt-24 pb-20">
      <h2 className="text-2xl font-semibold tracking-[-0.02em]">Como funciona por baixo do capô</h2>
      <div className="mt-8 grid items-stretch gap-3 md:grid-cols-[1fr_auto_1fr_auto_1fr]">
        {steps.map((s, i) => (
          <div key={s.title} className="contents">
            <Card className="p-5">
              <s.icon className="h-5 w-5 text-accent" />
              <h3 className="mt-3 text-[18px] font-semibold">{s.title}</h3>
              <div className="mt-2">{s.body}</div>
            </Card>
            {i < steps.length - 1 && (
              <div className="hidden items-center md:flex" aria-hidden>
                <span className="w-6 border-t border-dashed border-ink-3" />
                <ArrowRight className="h-4 w-4 text-ink-3" />
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

export function TrustSection() {
  const points = [
    { icon: ShieldCheck, title: "Triagem local", text: "A filtragem acontece no seu navegador; testes, segredos e binários nunca saem da máquina." },
    { icon: MemoryStick, title: "Processamento em RAM", text: "O código é lido como stream em memória. Nenhum .java é gravado em disco." },
    { icon: Database, title: "Só o resultado é salvo", text: "Persistimos apenas metadados do job e os 3 Markdowns finais." },
  ];
  return (
    <section id="privacidade" className="container max-w-[1024px] scroll-mt-24 pb-20">
      <div className="rounded-card border border-line bg-surface-2 p-6">
        <div className="grid gap-6 md:grid-cols-3">
          {points.map((p) => (
            <div key={p.title}>
              <p.icon className="h-5 w-5 text-ink" />
              <h3 className="mt-2 text-[15px] font-semibold">{p.title}</h3>
              <p className="mt-1 text-[13px] text-ink-2">{p.text}</p>
            </div>
          ))}
        </div>
        <div className="mt-6 flex flex-wrap items-center gap-2 font-mono text-xs text-ink-2" aria-label="Ciclo de vida do código">
          <Badge variant="outline">Browser Filter</Badge>
          <ArrowRight className="h-3.5 w-3.5" />
          <Badge variant="outline">RAM Parser (Stateless)</Badge>
          <ArrowRight className="h-3.5 w-3.5" />
          <Badge variant="outline"><Trash2 className="mr-1 h-3 w-3" />Immediate Wipe</Badge>
        </div>
      </div>
    </section>
  );
}

export function FooterSection() {
  return (
    <footer className="border-t border-line py-8">
      <div className="container flex max-w-[1200px] flex-wrap items-center justify-between gap-2 text-[13px] text-ink-3">
        <span>© SpringDocAI — documentação a partir da AST do seu Spring Boot.</span>
        <span className="font-mono">Java 11–21 · Spring Boot 2 &amp; 3</span>
      </div>
    </footer>
  );
}


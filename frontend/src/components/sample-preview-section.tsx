"use client";

import { MarkdownWorkspaceTabs } from "@/components/markdown-workspace-tabs";
import { SAMPLE_DOCUMENTS } from "@/lib/sample-docs";

export function SamplePreviewSection() {
  return (
    <section id="amostra" className="container max-w-[1024px] scroll-mt-24 pb-20">
      <h2 className="text-2xl font-semibold tracking-[-0.02em]">Folheie os 3 artefatos</h2>
      <p className="mt-2 text-ink-2">Exemplo ilustrativo do que é gerado para um projeto de pedidos.</p>
      <div className="mt-8 overflow-hidden rounded-card border border-line bg-surface shadow-float">
        <div className="flex items-center gap-1.5 border-b border-line bg-surface-2 px-3 py-2" aria-hidden>
          <span className="h-2.5 w-2.5 rounded-full bg-surface-3" />
          <span className="h-2.5 w-2.5 rounded-full bg-surface-3" />
          <span className="h-2.5 w-2.5 rounded-full bg-surface-3" />
          <span className="ml-2 font-mono text-xs text-ink-3">docs/</span>
        </div>
        <MarkdownWorkspaceTabs documents={SAMPLE_DOCUMENTS} projectName="exemplo" scrollClassName="h-[460px] min-h-0" />
      </div>
    </section>
  );
}


"use client";

import { useEffect, useState } from "react";

let counter = 0;

export function MermaidDiagram({ chart }: { chart: string }) {
  const [svg, setSvg] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setSvg(null);
    setFailed(false);
    (async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          theme: "neutral",
          securityLevel: "strict",
          fontFamily: "Inter Variable, Inter, system-ui, sans-serif",
        });
        const { svg: out } = await mermaid.render(`mmd-${++counter}`, chart);
        if (!cancelled) setSvg(out);
      } catch {
        if (!cancelled) setFailed(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [chart]);

  if (failed) {
    return (
      <pre>
        <code>{chart}</code>
      </pre>
    );
  }
  if (!svg) {
    return <div className="my-3 h-24 animate-pulse rounded-ctl bg-surface-2" aria-label="Renderizando diagrama" />;
  }
  return (
    <div
      className="my-3 flex justify-center overflow-x-auto rounded-ctl border border-line bg-surface p-4"
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
}


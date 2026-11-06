import JSZip from "jszip";
import type { GeneratedDocument } from "./api-client";

/** Gera o pacote .zip com os 3 arquivos .md inteiramente no navegador. */
export async function downloadDocsZip(documents: GeneratedDocument[], projectName: string): Promise<void> {
  const zip = new JSZip();
  documents.forEach((d) => zip.file(d.fileName, d.content));
  const blob = await zip.generateAsync({ type: "blob" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${projectName.replace(/[^\w.-]+/g, "-") || "docs"}-docs.zip`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export const DOC_ORDER = ["API_REFERENCE", "INTEGRATION_GUIDE", "ARCHITECTURE_OVERVIEW"] as const;


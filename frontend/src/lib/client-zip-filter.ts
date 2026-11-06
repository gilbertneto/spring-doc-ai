import JSZip from "jszip";

/**
 * Triagem 100% client-side (ADR-003). Lê um .zip ou pasta direto na memória do navegador e mantém
 * apenas `.java` relevantes + manifesto de build. Nada é enviado antes do usuário confirmar.
 */

export interface RawEntry {
  /** caminho relativo com "/" */
  path: string;
  size: number;
  read: () => Promise<string>;
}

export type DiscardReason = "test" | "build-output" | "vcs-or-deps" | "env-secret" | "binary" | "irrelevant" | "too-large" | "other";

export interface FilterResult {
  /** caminho relativo -> conteúdo, pronto para `POST /upload` */
  files: Record<string, string>;
  javaCount: number;
  manifest: string | null;
  totalEntries: number;
  discardedCount: number;
  discarded: Record<DiscardReason, number>;
  /** quantos .java por categoria (Controller, Service, ...) */
  categories: Record<string, number>;
  /** nome sugerido do projeto */
  projectName: string;
}

export const MAX_FILE_BYTES = 1024 * 1024;
const IGNORED_DIRS: [string, DiscardReason][] = [
  ["src/test/", "test"],
  ["target/", "build-output"],
  ["build/", "build-output"],
  ["out/", "build-output"],
  ["node_modules/", "vcs-or-deps"],
  [".git/", "vcs-or-deps"],
  [".idea/", "vcs-or-deps"],
  [".gradle/", "vcs-or-deps"],
];
const MANIFESTS = new Set(["pom.xml", "build.gradle", "build.gradle.kts"]);
const BINARY_EXT = /\.(png|jpe?g|gif|svg|ico|webp|pdf|jar|war|class|zip|gz|tar|exe|dll|so|bin|woff2?|ttf|mp4|mp3)$/i;

const NAME_HINT = /(Controller|Resource|Service|Dto|DTO|Request|Response|Record|Entity|Model|Exception|Advice|Handler)\.java$/;
const CONTENT_HINTS = [
  /@RestController/, /@Controller\b/, /@RequestMapping/, /@Service\b/, /@Entity\b/, /@Document\b/,
  /@ControllerAdvice/, /@RestControllerAdvice/, /@Data\b/, /@Value\b/, /\brecord\s+[A-Z]\w*\s*[(<]/,
  /jakarta\.validation|javax\.validation/, /extends\s+\w*(Exception|Controller)\b/,
];

function normalize(path: string): string {
  return path.replace(/\\/g, "/").replace(/^\.?\//, "");
}

/** Remove a pasta raiz única (ex.: "meu-projeto/src/...") quando todos os caminhos a compartilham. */
function stripCommonRoot(entries: RawEntry[]): RawEntry[] {
  if (entries.length === 0) return entries;
  const first = entries[0].path.split("/")[0];
  const shared = entries.every((e) => e.path.includes("/") && e.path.split("/")[0] === first);
  if (!shared || first === "src") return entries;
  return entries.map((e) => ({ ...e, path: e.path.slice(first.length + 1) }));
}

function discardReason(path: string): DiscardReason | null {
  const p = "/" + path;
  const name = path.split("/").pop() ?? path;
  if (name === ".env" || name.startsWith(".env.")) return "env-secret";
  for (const [dir, reason] of IGNORED_DIRS) {
    if (p.includes("/" + dir)) return reason;
  }
  if (BINARY_EXT.test(name)) return "binary";
  return null;
}

function category(path: string, content: string): string {
  if (/@RestController|@Controller\b/.test(content)) return "Controller";
  if (/@Service\b/.test(content)) return "Service";
  if (/@Entity\b|@Document\b/.test(content)) return "Entity";
  if (/Exception|@ControllerAdvice|@RestControllerAdvice/.test(path + content.slice(0, 600))) return "Exception";
  if (/\brecord\s+[A-Z]/.test(content)) return "Record";
  return "DTO/Model";
}

export async function filterEntries(rawEntries: RawEntry[], fallbackName = "projeto"): Promise<FilterResult> {
  const entries = stripCommonRoot(rawEntries.map((e) => ({ ...e, path: normalize(e.path) })));
  const discarded = { test: 0, "build-output": 0, "vcs-or-deps": 0, "env-secret": 0, binary: 0, irrelevant: 0, "too-large": 0, other: 0 } as Record<DiscardReason, number>;
  const files: Record<string, string> = {};
  const categories: Record<string, number> = {};
  let manifestPath: string | null = null;
  let manifestDepth = Infinity;
  let manifestContent: string | null = null;
  let javaCount = 0;

  for (const entry of entries) {
    const reason = discardReason(entry.path);
    if (reason) {
      discarded[reason]++;
      continue;
    }
    const name = entry.path.split("/").pop() ?? "";
    const isJava = name.endsWith(".java");
    const isManifest = MANIFESTS.has(name);
    if (!isJava && !isManifest) {
      discarded.other++;
      continue;
    }
    if (entry.size > MAX_FILE_BYTES) {
      discarded["too-large"]++;
      continue;
    }
    const content = await entry.read();
    if (isManifest) {
      const depth = entry.path.split("/").length;
      if (depth < manifestDepth) {
        manifestDepth = depth;
        manifestPath = entry.path;
        manifestContent = content;
      }
      continue;
    }
    if (name === "package-info.java" || name === "module-info.java") {
      discarded.irrelevant++;
      continue;
    }
    if (!NAME_HINT.test(name) && !CONTENT_HINTS.some((rx) => rx.test(content))) {
      discarded.irrelevant++;
      continue;
    }
    files[entry.path] = content;
    javaCount++;
    const cat = category(entry.path, content);
    categories[cat] = (categories[cat] ?? 0) + 1;
  }

  if (manifestPath && manifestContent !== null) {
    files[manifestPath] = manifestContent;
  }
  const discardedCount = Object.values(discarded).reduce((a, b) => a + b, 0);
  return {
    files,
    javaCount,
    manifest: manifestPath,
    totalEntries: entries.length,
    discardedCount,
    discarded,
    categories,
    projectName: fallbackName,
  };
}

/** Lê um .zip na memória do navegador. */
export async function filterZipFile(file: File): Promise<FilterResult> {
  const zip = await JSZip.loadAsync(file);
  const entries: RawEntry[] = [];
  zip.forEach((relativePath, zipEntry) => {
    if (zipEntry.dir) return;
    const size = (zipEntry as unknown as { _data?: { uncompressedSize?: number } })._data?.uncompressedSize ?? 0;
    entries.push({ path: relativePath, size, read: () => zipEntry.async("string") });
  });
  return filterEntries(entries, file.name.replace(/\.zip$/i, ""));
}

/** Lê arquivos de uma pasta escolhida via `<input webkitdirectory>`. */
export async function filterFileList(files: File[]): Promise<FilterResult> {
  const entries: RawEntry[] = files.map((f) => ({
    path: (f as File & { webkitRelativePath?: string }).webkitRelativePath || f.name,
    size: f.size,
    read: () => f.text(),
  }));
  const root = entries[0]?.path.split("/")[0] ?? "projeto";
  return filterEntries(entries, root);
}

type FsEntry = { isFile: boolean; isDirectory: boolean; name: string; fullPath: string };
type FsFileEntry = FsEntry & { file: (cb: (f: File) => void, err: (e: unknown) => void) => void };
type FsDirEntry = FsEntry & {
  createReader: () => { readEntries: (cb: (e: FsEntry[]) => void, err: (e: unknown) => void) => void };
};

async function walk(entry: FsEntry, out: RawEntry[]): Promise<void> {
  if (entry.isFile) {
    const file = await new Promise<File>((res, rej) => (entry as FsFileEntry).file(res, rej));
    out.push({ path: entry.fullPath.replace(/^\//, ""), size: file.size, read: () => file.text() });
    return;
  }
  if (!entry.isDirectory) return;
  // Pastas ignoradas nem são percorridas (evita ler node_modules/.git inteiros).
  if (["node_modules", ".git", "target", "build", ".idea", ".gradle"].includes(entry.name)) return;
  const reader = (entry as FsDirEntry).createReader();
  for (;;) {
    const batch = await new Promise<FsEntry[]>((res, rej) => reader.readEntries(res, rej));
    if (batch.length === 0) break;
    for (const child of batch) await walk(child, out);
  }
}

/** Trata arrastar-e-soltar de pasta ou .zip. */
export async function filterDroppedItems(items: DataTransferItemList): Promise<FilterResult> {
  const first = items[0]?.getAsFile();
  if (items.length === 1 && first && /\.zip$/i.test(first.name)) {
    return filterZipFile(first);
  }
  const entries: RawEntry[] = [];
  let rootName = "projeto";
  for (let i = 0; i < items.length; i++) {
    const entry = (items[i] as DataTransferItem & { webkitGetAsEntry?: () => FsEntry | null }).webkitGetAsEntry?.();
    if (entry) {
      if (i === 0) rootName = entry.name;
      await walk(entry, entries);
    }
  }
  if (entries.length === 0) throw new Error("Nenhum arquivo legível foi encontrado. Arraste uma pasta ou um arquivo .zip.");
  return filterEntries(entries, rootName);
}


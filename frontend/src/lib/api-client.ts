export type JobStatus = "PENDING" | "PARSING_AST" | "GENERATING_DOCS" | "COMPLETED" | "FAILED";
export type SourceType = "GITHUB_URL" | "ZIP_UPLOAD";
export type DocType = "API_REFERENCE" | "INTEGRATION_GUIDE" | "ARCHITECTURE_OVERVIEW";

export interface GenerationJob {
  id: string;
  projectName: string;
  sourceType: SourceType;
  repositoryUrl: string | null;
  status: JobStatus;
  endpointsCount: number | null;
  classesAnalyzedCount: number | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface GeneratedDocument {
  docType: DocType;
  fileName: string;
  content: string;
}

export interface GenerationDetail extends GenerationJob {
  documents: GeneratedDocument[];
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code: string,
  ) {
    super(message);
  }
}

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const KEY_STORAGE = "springdocai.byok";

export interface ByokSettings {
  provider: "openai" | "gemini" | "other";
  apiKey: string;
}

export function loadByok(): ByokSettings | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(KEY_STORAGE);
    return raw ? (JSON.parse(raw) as ByokSettings) : null;
  } catch {
    return null;
  }
}

export function saveByok(settings: ByokSettings | null): void {
  if (settings && settings.apiKey.trim()) {
    window.localStorage.setItem(KEY_STORAGE, JSON.stringify({ ...settings, apiKey: settings.apiKey.trim() }));
  } else {
    window.localStorage.removeItem(KEY_STORAGE);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const byok = loadByok();
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (byok?.apiKey) headers["X-LLM-API-Key"] = byok.apiKey;
  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, { ...init, headers: { ...headers, ...(init?.headers ?? {}) } });
  } catch {
    throw new ApiError("Não foi possível conectar ao backend (http://localhost:8080). Ele está em execução?", 0, "NETWORK");
  }
  if (!res.ok) {
    let body: { error?: string; message?: string } = {};
    try {
      body = await res.json();
    } catch {
      /* corpo vazio */
    }
    throw new ApiError(body.message ?? `Erro HTTP ${res.status}`, res.status, body.error ?? "HTTP_ERROR");
  }
  return (await res.json()) as T;
}

export function createUploadGeneration(projectName: string, files: Record<string, string>) {
  return request<GenerationJob>("/api/v1/generations/upload", {
    method: "POST",
    body: JSON.stringify({ projectName, files }),
  });
}

export function createGithubGeneration(repositoryUrl: string) {
  return request<GenerationJob>("/api/v1/generations/github", {
    method: "POST",
    body: JSON.stringify({ repositoryUrl }),
  });
}

export function getGeneration(jobId: string) {
  return request<GenerationDetail>(`/api/v1/generations/${jobId}`);
}

export function getHistory() {
  return request<GenerationJob[]>("/api/v1/generations/history");
}


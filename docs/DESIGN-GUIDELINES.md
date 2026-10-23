# DESIGN-GUIDELINES: Sistema Visual e Guia de Componentes

## 1. Direção Estética (Clean Modern Light Mode)
Inspirado na precisão visual de **Linear, Resend (Light) e Vercel Geist**:
- **Filosofia:** Superfícies brancas e cinza-gelo extremamente limpas, uso de bordas de `1px` nítidas no lugar de sombras pesadas, alto contraste tipográfico e destaque para elementos técnicos (rotas HTTP, badges de anotações Java e blocos de código).
- **Tema:** Exclusivamente **Light Mode** por padrão (otimizado para leitura prolongada de documentação técnica).

---

## 2. Paleta de Cores (Hex & Tokens)

### Superfícies e Fundos
- **Background Principal (Canvas):** `#FAFAFA` (Zinc 50 — evita o cansaço visual do branco puro no fundo geral)
- **Superfície Elevada (Cards, Containers, Workspace):** `#FFFFFF` (Branco puro)
- **Superfície Secundária (Code Blocks, Dropzone Hover, Table Headers):** `#F4F4F5` (Zinc 100)
- **Superfície Sutil de Destaque (Badges neutras, Pílulas):** `#E4E4E7` (Zinc 200)

### Textos
- **Texto Primário (Títulos, Corpo importante):** `#09090B` (Zinc 950)
- **Texto Secundário (Subtítulos, Descrições, Labels):** `#52525B` (Zinc 600)
- **Texto Muted (Placeholders, Metadados, Timestamps):** `#A1A1AA` (Zinc 400)

### Bordas e Divisores
- **Borda Padrão de Card/Input:** `#E4E4E7` (Zinc 200)
- **Borda em Foco / Hover Ativo:** `#18181B` (Zinc 900)

### Ações e Semântica (Acentos Técnicos & Verbos HTTP)
- **Primária (Botões principais, Indicadores ativos):** `#18181B` (Zinc 900) com texto `#FAFAFA`
- **Acento Técnico (Links, Seleção, Progresso AST):** `#2563EB` (Blue 600) — Fundo suave: `#EFF6FF`
- **Sucesso / HTTP `GET` / `200 OK`:** `#16A34A` (Green 600) — Fundo badge: `#F0FDF4` — Borda: `#BBF7D0`
- **Criação / HTTP `POST` / `201 Created`:** `#2563EB` (Blue 600) — Fundo badge: `#EFF6FF` — Borda: `#BFDBFE`
- **Atualização / HTTP `PUT` & `PATCH`:** `#D97706` (Amber 600) — Fundo badge: `#FFFBEB` — Borda: `#FDE68A`
- **Remoção / Erro / HTTP `DELETE` / `4xx-5xx`:** `#DC2626` (Red 600) — Fundo badge: `#FEF2F2` — Borda: `#FECACA`

---

## 3. Tipografia
- **Fonte Sans-Serif (UI, Títulos e Prosa do Markdown):** `Inter` ou `Geist Sans`
  - **Hero H1:** `2.5rem` (40px), `font-weight: 700`, `letter-spacing: -0.03em`, `line-height: 1.15`
  - **Section H2:** `1.5rem` (24px), `font-weight: 600`, `letter-spacing: -0.02em`
  - **Card Title / H3:** `1.125rem` (18px), `font-weight: 600`
  - **Body (Leitura da Documentação):** `0.9375rem` (15px), `font-weight: 400`, `line-height: 1.65`
  - **Small / Captions:** `0.8125rem` (13px), `font-weight: 500`
- **Fonte Monoespaçada (Rotas HTTP, Anotações `@RestController`, Código e Payloads):** `JetBrains Mono` ou `Geist Mono`
  - Aplicada obrigatoriamente em URLs de endpoints, nomes de classes Java, blocos de código JSON/cURL e contadores numéricos (`font-variant-numeric: tabular-nums`).

---

## 4. Espaçamento, Radius e Sombras
- **Grid & Container:** Container principal de `max-width: 1200px` com padding lateral de `24px` (mobile) a `48px` (desktop). Escala base de espaçamento em múltiplos de `4px` (`4, 8, 12, 16, 24, 32, 48, 64px`).
- **Border Radius:**
  - Cards principais e painéis de documentação: `10px` (`rounded-xl` suave)
  - Botões, Inputs e Code Blocks: `6px` (`rounded-md`)
  - Badges de método HTTP e anotações: `4px` (`rounded`)
- **Sombras (Minimalistas):**
  - **Cards padrão:** `0 1px 2px 0 rgba(0, 0, 0, 0.03)` combinada com borda sólida `1px solid #E4E4E7`.
  - **Input Box Principal (Hero) / Modais:** `0 10px 30px -10px rgba(0, 0, 0, 0.06), 0 1px 3px 0 rgba(0, 0, 0, 0.04)`.

---

## 5. Guia Prático de Componentes `shadcn/ui`

| Componente `shadcn/ui` | Onde e Como Usar no Projeto |
| :--- | :--- |
| **`Tabs` (`TabsList`, `TabsTrigger`, `TabsContent`)** | **1.** No Card de entrada do Hero para alternar entre *"Colar Link do GitHub"* e *"Subir Arquivo .zip"*.<br>**2.** No painel principal de resultados para navegar entre os 3 arquivos gerados (`API-REFERENCE.md`, `INTEGRATION-GUIDE.md`, `ARCHITECTURE.md`). |
| **`Card` (`CardHeader`, `CardContent`, `CardFooter`)** | Container do extrator no Hero, blocos de resumo da AST (mostrando total de `Controllers`, `DTOs`, `Entities` lidos) e moldura do visualizador de Markdown. |
| **`Input` + `Button`** | Barra de inserção da URL do repositório GitHub e ações primárias ("Gerar Documentação", "Baixar .zip", "Copiar"). |
| **`Badge`** | Essencial para exibir verbos HTTP (`GET`, `POST`, `PUT`, `DELETE`) com as cores semânticas da paleta e para destacar anotações extraídas (`@RestController`, `@Valid`, `@Transactional`). |
| **`Progress` + `Skeleton`** | Durante o processamento do `JavaParser` e geração da LLM. Exibir barra de progresso segmentada em 4 passos + `Skeleton` imitando linhas de documentação enquanto o texto está sendo gerado. |
| **`ScrollArea`** | Envolver a área de leitura dos 3 documentos Markdown (`height: calc(100vh - 220px)`) para manter o cabeçalho das abas e botões de download sempre fixos e visíveis no topo. |
| **`Accordion`** | Na visualização da AST extraída (caso o usuário queira inspecionar quais endpoints foram detectados em cada `Controller` antes ou depois da geração). |
| **`Sheet` (Drawer Lateral)** | Acessado pelo botão "Histórico" no Header: abre uma gaveta lateral direita listando os `GenerationJobs` salvos no PostgreSQL/H2 com data, nome do projeto, quantidade de endpoints e botão para reabrir os 3 arquivos. |
| **`Dialog`** | Modal limpo para configuração opcional de *Bring Your Own Key (BYOK)* (seleção do provedor OpenAI/Gemini + campo de API Key salvo no `localStorage`). |
| **`Alert`** | Exibição de avisos importantes de validação (ex: quando o `.zip` enviado não contém nenhuma classe anotada com `@RestController` ou quando ocorre erro de limite de tokens). |
| **`Tooltip`** | Explicações rápidas sobre privacidade ("Código processado em RAM e descartado") e feedback visual de "Copiado!" nos botões de código. |
| **`Sonner` (Toast)** | Notificações discretas no canto inferior direito: *"24 classes filtradas no navegador"*, *"AST extraída com sucesso (42 endpoints)"*, *"Documentação salva e pronta para download"*. |
# PRD: Product Requirements Document — SpringDocAI

## 1. Visão Geral
O **SpringDocAI** é uma plataforma onde o usuário fornece o link de um repositório público do GitHub ou faz upload da pasta `src/main/java` (em `.zip`) de um projeto Java/Spring Boot. O frontend realiza a triagem inicial dos arquivos no navegador, envia apenas classes relevantes para o backend Spring Boot, que extrai a árvore sintática (AST) via **JavaParser**, aciona uma LLM com prompts encadeados e persiste os 3 documentos Markdown resultantes via **Spring Data JPA** (PostgreSQL em produção e H2 em desenvolvimento/testes).

## 2. Personas
1. **Carlos (Tech Lead de ERP Legado):** Tem uma API monolítica em Spring Boot com 80 endpoints sem documentação atualizada. Precisa entregar até sexta-feira um guia de integração para um novo parceiro comercial consumir os fluxos de Pedido e Faturamento. Tem receio de vazar o código da empresa.
2. **Marina (Dev Backend Pleno/Sênior):** Acabou de assumir a manutenção de um microsserviço Spring Boot mal documentado e precisa entender rapidamente a arquitetura, entidades e casos de uso sem ler classe por classe.

## 3. User Stories
- **US01 (Input via GitHub URL):** Como usuário, quero colar a URL de um repositório público do GitHub para que o sistema baixe e analise apenas a estrutura Java/Spring sem que eu precise clonar o projeto.
- **US02 (Upload Local Seguro com Filtro Client-Side):** Como Tech Lead de código privado, quero arrastar um arquivo `.zip` ou pasta do meu projeto para o navegador, visualizando quais arquivos `.java` serão enviados antes do upload, garantindo que `.jar`, `.env`, testes e imagens nunca saiam da minha máquina.
- **US03 (Acompanhamento de Progresso):** Como usuário, quero ver em tempo real em qual etapa o processamento está (`Extraindo AST`, `Gerando Doc Técnica`, `Gerando Guia de Casos de Uso`, `Gerando Arquitetura`) para saber que o sistema não travou.
- **US04 (Visualização e Download):** Como usuário, quero ler os 3 documentos gerados em abas organizadas com syntax highlighting e baixar um pacote `.zip` com os 3 arquivos `.md` ou copiá-los individualmente com um clique.
- **US05 (Histórico Persistido):** Como usuário, quero acessar as documentações geradas anteriormente a partir de um histórico salvo no banco de dados para não gastar tokens gerando novamente o mesmo projeto.

## 4. Requisitos Funcionais (RF)

### 4.1 Frontend (Client-Side First)
- **RF01 - Validação e Descompactação em Memória (Client-Side):** Ao receber um `.zip` no dropzone, o browser (via `JSZip`) deve varrer a estrutura localmente e descartar tudo que não seja `.java` ou `pom.xml`/`build.gradle`. Deve ignorar pastas `src/test/`, `target/`, `build/`, `.git/` e classes que não pertençam às camadas de Controller, Service, DTO/Record, Entity/Model ou ExceptionHandler.
- **RF02 - Configuração de API Key (Opcional BYOK):** Permitir que o usuário insira sua chave de API (OpenAI/Gemini/Anthropic) armazenada apenas no `localStorage` do navegador e enviada via Header HTTP seguro (`X-LLM-API-Key`) para a requisição em andamento.
- **RF03 - Painel de Preview Triplo:** Renderizar os resultados em 3 abas (`API-REFERENCE.md`, `INTEGRATION-GUIDE.md`, `ARCHITECTURE.md`) com alternância entre visualização renderizada (Markdown) e código bruto (Raw).

### 4.2 Backend (Spring Boot + JavaParser + Spring AI)
- **RF04 - Ingestão Stateless de Código:** Receber o payload de arquivos `.java` filtrados (ou baixar o tarball do GitHub em `InputStream` de memória) sem gravar arquivos temporários permanentes em disco.
- **RF05 - Extrator AST Cirúrgico (`JavaParser`):**
  - Identificar classes anotadas com `@RestController`, `@Controller`, `@RequestMapping`.
  - Extrair verbos HTTP (`@GetMapping`, `@PostMapping`, etc.), paths, `@PathVariable`, `@RequestParam`, `@RequestBody` e `@ResponseStatus`.
  - Resolver os tipos de entrada e saída (DTOs/Records), extraindo seus atributos e anotações do Jakarta/Bean Validation (`@NotNull`, `@Size`, `@Pattern`, `@Email`, etc.).
  - Rastrear dependências injetadas no Controller (`@Autowired` ou construtor) que sejam `@Service`, extraindo as assinaturas dos métodos públicos chamados, Javadocs existentes e exceções explícitas (`throw new ...`) para capturar as regras de negócio.
  - Extrair classes `@Entity` e `@Table` (atributos e relacionamentos `@OneToMany`, `@ManyToOne`, etc.) para compor o diagrama/visão de domínio.
- **RF06 - Orquestração de Prompts em 3 Etapas:**
  - Transformar a AST extraída em um JSON enxuto (`ProjectAstSummary`).
  - **Prompt 1 (`API-REFERENCE.md`):** Gera tabela de rotas, headers, payloads JSON de exemplo baseados nos DTOs, validações obrigatórias e possíveis códigos de erro HTTP baseados nas exceções dos Services.
  - **Prompt 2 (`INTEGRATION-GUIDE.md`):** Analisa a relação semântica entre os endpoints e Services para criar fluxos de casos de uso passo a passo (ex: "Fluxo de Onboarding de Cliente", "Fluxo de Fechamento de Pedido") com exemplos `cURL`.
  - **Prompt 3 (`ARCHITECTURE.md`):** Gera visão geral da stack (extraída do `pom.xml`/`build.gradle`), divisão de pacotes, camadas e diagrama Mermaid.js das entidades (`@Entity`).

## 5. Requisitos Não-Funcionais (RNF)
- **RNF01 - Zero Retenção de Código-Fonte:** Nenhuma linha de código-fonte bruto enviado pelo usuário pode ser salva no banco de dados ou em logs. Apenas o resumo métrico e os 3 textos Markdown finais são persistidos.
- **RNF02 - Limite de Payload e Chunking:** Se o JSON da AST ultrapassar 60.000 tokens, o backend deve agrupar os Controllers por domínio/pacote e processar a documentação técnica em lotes (batches), consolidando o Markdown ao final.
- **RNF03 - Tempo de Resposta e Assincronismo:** A geração deve utilizar processamento assíncrono (`@Async` ou `SseEmitter`) para evitar HTTP Timeout (504) em projetos com mais de 30 endpoints.

## 6. Camada de Persistência (Spring Data JPA: PostgreSQL + H2)
*(Substitui integrações BaaS externas por controle nativo via Spring Data JPA)*

### 6.1 Configuração de Ambientes (`Spring Profiles`)
- **Perfil `dev` e `test` (`application-dev.yml` / `application-test.yml`):**
  - Banco de dados **H2 Database** em memória (`jdbc:h2:mem:springdocai;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`).
  - Console H2 habilitado em `/h2-console` para inspeção rápida durante o desenvolvimento e execução instantânea de testes com `@DataJpaTest` no JUnit 5.
- **Perfil `prod` (`application-prod.yml`):**
  - Banco de dados **PostgreSQL 16+**.
  - Gerenciamento de migrações de schema com **Flyway** (`src/main/resources/db/migration/V1__create_tables.sql`).

### 6.2 Modelo de Entidades JPA
1. **`GenerationJob` (`@Entity` - Tabela `generation_jobs`):**
   - `id` (`UUID`, `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)`)
   - `projectName` (`String`, nullable = false) — Nome do repo ou arquivo `.zip`.
   - `sourceType` (`Enum`: `GITHUB_URL`, `ZIP_UPLOAD`)
   - `repositoryUrl` (`String`, nullable = true)
   - `status` (`Enum`: `PENDING`, `PARSING_AST`, `GENERATING_DOCS`, `COMPLETED`, `FAILED`)
   - `endpointsCount` (`Integer`) — Total de endpoints mapeados pelo JavaParser.
   - `classesAnalyzedCount` (`Integer`) — Total de classes lidas na AST.
   - `errorMessage` (`String`, columnDefinition = `TEXT`, nullable = true)
   - `createdAt` (`LocalDateTime`, `@CreationTimestamp`)
   - `completedAt` (`LocalDateTime`, nullable = true)
2. **`GeneratedDocument` (`@Entity` - Tabela `generated_documents`):**
   - `id` (`UUID`, `@Id`)
   - `job` (`@ManyToOne(fetch = FetchType.LAZY)`, `JoinColumn(name = "job_id")`)
   - `docType` (`Enum`: `API_REFERENCE`, `INTEGRATION_GUIDE`, `ARCHITECTURE_OVERVIEW`)
   - `fileName` (`String`) — Ex: `API-REFERENCE.md`.
   - `content` (`String`, `@Column(columnDefinition = "TEXT")`, nullable = false) — Conteúdo Markdown completo.

### 6.3 Repositórios Spring Data
- `GenerationJobRepository extends JpaRepository<GenerationJob, UUID>`
  - `List<GenerationJob> findTop20ByOrderByCreatedAtDesc();`
- `GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID>`
  - `List<GeneratedDocument> findByJobId(UUID jobId);`

## 7. Edge Cases (Casos Limite)
- **EC01 - Repositório sem Anotações Spring Web:** O usuário sobe um projeto Java puro ou biblioteca sem `@RestController`/`@Controller`. O sistema deve abortar antes de chamar a LLM e retornar um aviso claro: *"Nenhum endpoint Spring Web (@RestController) foi encontrado neste projeto."*
- **EC02 - Uso de Lombok e Records Java 17+:** O `JavaParser` deve identificar anotações `@Data`, `@Getter`, `@Value` e `record` do Java para deduzir corretamente os campos do JSON mesmo quando não há métodos `get/set` explícitos no código.
- **EC03 - Herança de Rotas e Genéricos:** Controllers que herdam rotas de uma classe base abstrata ou retornam `ResponseEntity<ApiResponse<MeuDTO>>`. O extrator AST deve desempacotar o tipo genérico interno (`MeuDTO`) para documentar os campos reais do payload.
- **EC04 - Queda ou Rate Limit da API da LLM:** Se a geração do 2º ou 3º documento falhar por Rate Limit (HTTP 429), o backend aplica *Exponential Backoff* (até 3 tentativas via Spring Retry). Se persistir, marca o `GenerationJob` como `FAILED` preservando os documentos que já foram concluídos na transação.

## 8. Critérios de Aceitação (Definition of Done)
- [ ] Upload de um `.zip` contendo um projeto Spring Boot com 30 endpoints filtra os arquivos no frontend em menos de 2 segundos.
- [ ] O parser AST extrai com 100% de precisão o método HTTP, a URL completa (concatenando `@RequestMapping` da classe + método) e os campos dos DTOs de Request/Response.
- [ ] Os 3 documentos `.md` são gerados, exibidos em abas no frontend e salvos nas tabelas `generation_jobs` e `generated_documents` via Spring Data JPA.
- [ ] Testes de repositório (`@DataJpaTest` com H2) e testes unitários do extrator AST (`JavaParserServiceTest` com JUnit 5) passam com cobertura mínima de 80% na camada de extração.
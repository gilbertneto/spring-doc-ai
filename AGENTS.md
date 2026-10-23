# AGENTS.md — Diretrizes Globais e Roteiro de Execução (SpringDocAI)

Este arquivo define as regras arquiteturais, restrições técnicas inegociáveis, o protocolo de documentação contínua (SDD Vivo) e o plano de execução em 4 missões para os agentes autônomos do **Google Antigravity**.

## 📚 1. Fontes da Verdade (Context Anchors)

Antes de planejar, criar *Artifacts* ou modificar qualquer código, leia obrigatoriamente os documentos de especificação na pasta `/docs`:

* **`@docs/BRIEF.md`**: Visão executiva do produto, diferencial da extração AST vs. Swagger comum e métricas de sucesso.

* **`@docs/PRD.md`**: Requisitos funcionais (RF01 a RF06), requisitos não-funcionais (RNF01 a RNF03), modelo de entidades **Spring Data JPA** (`GenerationJob` e `GeneratedDocument`), edge cases (EC01 a EC04) e critérios de aceitação.

* **`@docs/MVP-SCOPE.md`**: Matriz MoSCoW e lista estrita do que **fica fora** do MVP (Anti-Escopo).

* **`@docs/LANDING-PAGE-SPEC.md`**: Arquitetura de informação seção a seção da Landing Page / Workspace e hierarquia de CTAs.

* **`@docs/DESIGN-GUIDELINES.md`**: Paleta de cores Hex (Light Mode), tipografia (`Inter` + `JetBrains Mono`) e mapeamento de componentes `shadcn/ui`.

* **`@docs/SDD.md`**: **Software Design Document (Documento Vivo)**. Contém o desenho técnico real implementado, diagramas, decisões arquiteturais (ADRs) e o registro cronológico de alterações.

## 📝 2. Protocolo Obrigatório de Documentação Viva (`@docs/SDD.md`)

> ⚠️ **REGRA DE BLOQUEIO (DEFINITION OF DONE):** Nenhuma tarefa, missão, refatoração ou correção de bug é considerada concluída até que o arquivo `@docs/SDD.md` seja atualizado refletindo o estado exato do código.

Sempre que você (Agente) criar, alterar ou remover qualquer classe, endpoint, componente, configuração de banco ou dependência:

1. **Antes de codificar:** Consulte `@docs/SDD.md` para respeitar os contratos, padrões de pacotes e decisões arquiteturais (ADRs) vigentes.

2. **Após codificar e testar (Obrigatório no mesmo turno):** Edite `@docs/SDD.md` atualizando:

   * **Catálogo de Componentes/Classes:** Adicione ou atualize a assinatura, responsabilidade e dependências da classe/componente modificado.

   * **Contratos de Dados e Endpoints:** Se uma rota REST, DTO ou Entidade JPA mudou, atualize imediatamente a especificação correspondente no SDD.

   * **ADRs (Architecture Decision Records):** Se precisou tomar uma decisão técnica durante a implementação (ex: escolha de biblioteca auxiliar, estratégia de retry, tratamento de erro), adicione um novo bloco `ADR-XXX` na seção de decisões do SDD.

   * **Changelog Técnico de Implementação:** Adicione uma entrada no final do `@docs/SDD.md` contendo: Data/Missão, Arquivos Criados/Modificados, Resumo Técnico da Mudança e Status dos Testes (`mvn test` / `npm run build`).

## 🔒 3. Restrições Técnicas Inegociáveis (Hard Rules)

1. **Estrutura de Monorepo:**

   * `/backend`: **Java 21 + Spring Boot 3.x + Maven**.

   * `/frontend`: **Next.js (App Router) + React + TypeScript + Tailwind CSS + `shadcn/ui` + `lucide-react` + `jszip`**.

   * `/docs`: Documentação oficial de descoberta, especificação e SDD vivo.

2. **Privacidade Zero-Retention para Código-Fonte (Crítico):**

   * **NUNCA** grave arquivos `.java` ou pacotes `.zip` enviados pelo usuário em disco local (`File`, `Files.write`, `/tmp`) nem em tabelas do banco de dados.

   * Todo o fluxo de leitura de arquivos (seja via upload filtrado do cliente ou download de tarball de repositório público do GitHub) deve operar **100% em memória RAM** (`InputStream`, `ByteArrayInputStream`, `Map<String, String>`).

   * Apenas metadados da execução e os 3 textos Markdown finais (`API-REFERENCE.md`, `INTEGRATION-GUIDE.md`, `ARCHITECTURE.md`) são persistidos no banco.

3. **Camada de Persistência (Spring Data JPA Dual-Profile):**

   * **Perfil `dev` e `test` (`application-dev.yml` / `application-test.yml`):** Uso obrigatório do **H2 Database** em memória com modo de compatibilidade PostgreSQL (`jdbc:h2:mem:springdocai;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`) e console habilitado em `/h2-console`.

   * **Perfil `prod` (`application-prod.yml`):** Uso obrigatório do **PostgreSQL 16+** com versionamento de schema gerenciado pelo **Flyway** (`src/main/resources/db/migration/V1__create_tables.sql`).

4. **Extrator Sintático Cirúrgico (`JavaParser`):**

   * Nunca envie classes Java brutas inteiras para a LLM.

   * Utilize o `javaparser-core` configurado para `JAVA_21` para extrair apenas a árvore sintática resumida (`ProjectAstSummary`), cobrindo `@RestController`, `@Controller`, `@RequestMapping` (e derivados), `@Service` (assinaturas públicas + exceções lançadas `throw new`), `@Entity` e DTOs/Records com validações (`jakarta.validation`).

   * Trate obrigatoriamente os Edge Cases **EC01** (erro amigável se o projeto tiver 0 endpoints Spring Web), **EC02** (suporte a Lombok `@Data`/`@Value` e `record` do Java 17+) e **EC03** (desempacotar tipos genéricos como `ResponseEntity<ApiResponse<MeuDTO>>`).

5. **Arquitetura Client-Side First no Frontend:**

   * Quando o usuário selecionar ou arrastar um arquivo `.zip` no navegador, utilize **`JSZip` no cliente** para filtrar apenas arquivos `.java` relevantes e `pom.xml`/`build.gradle` antes do upload.

   * Ignore imediatamente no cliente pastas `src/test/`, `target/`, `build/`, `node_modules/`, `.git/`, imagens e binários.

6. **Fidelidade Visual (`DESIGN-GUIDELINES.md`):**

   * Tema estritamente **Light Mode** inspirado em Linear / Resend / Vercel (`#FAFAFA` no canvas principal, `#FFFFFF` nos cards e superfícies, `#09090B` no texto primário, `#E4E4E7` nas bordas de `1px`).

   * Fonte monoespaçada (`JetBrains Mono` ou `Geist Mono`) obrigatória em todas as rotas HTTP, badges de anotações Java, blocos JSON/cURL e contadores numéricos.

7. **Política de Verificação, Testes e SDD:**

   * Antes de dar qualquer missão como concluída:

     1. Execute os testes no terminal integrado (`mvn clean test` em `/backend` e `npm run build` em `/frontend`).

     2. Atualize o arquivo `@docs/SDD.md` com tudo o que foi construído ou modificado.

## 🗂️ 4. Estrutura de Diretórios Esperada

```
springdoc-ai/
├── AGENTS.md
├── .antigravity/
│   └── rules.md (opcional: espelho ou link para AGENTS.md)
├── docs/
│   ├── BRIEF.md
│   ├── PRD.md
│   ├── MVP-SCOPE.md
│   ├── LANDING-PAGE-SPEC.md
│   ├── DESIGN-GUIDELINES.md
│   └── SDD.md  <-- DOCUMENTO VIVO ATUALIZADO PELO AGENTE A CADA MUDANÇA
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/springdocai/
│       │   │   ├── SpringDocAiApplication.java
│       │   │   ├── ast/
│       │   │   │   ├── dto/ProjectAstSummary.java
│       │   │   │   └── service/JavaParserAstExtractorService.java
│       │   │   ├── config/CorsConfig.java
│       │   │   ├── controller/GenerationController.java
│       │   │   ├── domain/
│       │   │   │   ├── entity/GenerationJob.java
│       │   │   │   ├── entity/GeneratedDocument.java
│       │   │   │   └── enums/ (SourceType, JobStatus, DocumentType)
│       │   │   ├── repository/
│       │   │   │   ├── GenerationJobRepository.java
│       │   │   │   └── GeneratedDocumentRepository.java
│       │   │   └── service/
│       │   │       ├── GithubTarballInMemoryService.java
│       │   │       ├── LlmPromptOrchestratorService.java
│       │   │       └── GenerationWorkflowService.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/V1__create_tables.sql
│       └── test/java/com/springdocai/
│           ├── ast/JavaParserAstExtractorServiceTest.java
│           └── repository/GenerationJobRepositoryTest.java
└── frontend/
    ├── package.json
    ├── tailwind.config.ts
    └── src/
        ├── app/
        │   ├── layout.tsx
        │   └── page.tsx
        ├── components/
        │   ├── ui/ (shadcn/ui components)
        │   ├── header-bar.tsx
        │   ├── hero-extractor-card.tsx
        │   ├── ast-progress-stepper.tsx
        │   ├── markdown-workspace-tabs.tsx
        │   ├── byok-settings-dialog.tsx
        │   └── history-drawer.tsx
        └── lib/
            ├── client-zip-filter.ts
            └── api-client.ts


```

## 🚀 5. Roteiro de Execução em 4 Missões (Mission Control)

Execute a construção do projeto seguindo sequencialmente as 4 missões abaixo. Ao finalizar cada missão, atualize `@docs/SDD.md` e gere um *Artifact* de validação.

### Missão 1: Fundação do Backend, Persistência Dual (H2/Postgres) e Extrator AST

* **Objetivo:** Criar `/backend`, configurar `pom.xml`, perfis `dev` (H2) e `prod` (PostgreSQL + Flyway), entidades JPA, repositórios e o extrator `JavaParserAstExtractorService`.

* **Critério de Aceite da Missão 1:**

  * `GenerationJobRepositoryTest` (`@DataJpaTest` com H2) e `JavaParserAstExtractorServiceTest` passando 100% no `mvn test`.

  * **Atualização obrigatória do `@docs/SDD.md`:** Documentar o schema JPA implementado, as regras de extração AST e registrar a entrega da Missão 1 no Changelog Técnico do SDD.

### Missão 2: Ingestão em Memória, Orquestração dos 3 Prompts LLM e API REST

* **Objetivo:** Implementar a ingestão *stateless* (via lista de arquivos filtrados pelo cliente ou download em RAM do tarball de repositório público do GitHub), o serviço `LlmPromptOrchestratorService` (suportando header `X-LLM-API-Key` para BYOK ou variável de ambiente padrão) e o `GenerationController`.

* **Critério de Aceite da Missão 2:**

  * Todos os testes unitários e de controller passando no `mvn test`.

  * **Atualização obrigatória do `@docs/SDD.md`:** Documentar os contratos exatos dos endpoints REST criados (`/api/v1/generations/...`), a estrutura dos 3 prompts de sistema e registrar a Missão 2 no Changelog Técnico do SDD.

### Missão 3: Frontend Client-First (`shadcn/ui` + Triagem `JSZip`)

* **Objetivo:** Criar `/frontend` seguindo rigorosamente `@docs/LANDING-PAGE-SPEC.md` e `@docs/DESIGN-GUIDELINES.md`.

* **Critério de Aceite da Missão 3:**

  * `npm run build` executando sem erros de TypeScript ou lint.

  * **Atualização obrigatória do `@docs/SDD.md`:** Documentar a árvore de componentes React criados, a lógica do filtro client-side (`client-zip-filter.ts`) e registrar a Missão 3 no Changelog Técnico do SDD.

### Missão 4: Verificação Ponta a Ponta no Browser Integrado

* **Objetivo:** Levantar `/backend` (porta `8080`, perfil `dev`) e `/frontend` (porta `3000`), abrir o navegador integrado do Antigravity e validar o fluxo completo de ponta a ponta.

* **Critério de Aceite da Missão 4:**

  * Simular uma geração completa pela UI, confirmar a renderização das 3 abas de documentação e atualizar o `@docs/SDD.md` marcando o status do MVP como `VERIFIED_END_TO_END`.
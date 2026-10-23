# Regras Globais do Projeto SpringDocAI

Sempre consulte a pasta `/docs` antes de planejar ou executar tarefas:
- Visão e Produto: `@docs/BRIEF.md` e `@docs/MVP-SCOPE.md`
- Arquitetura Backend, JPA e AST: `@docs/PRD.md`
- UI, Componentes e Paleta Light Mode: `@docs/LANDING-PAGE-SPEC.md` e `@docs/DESIGN-GUIDELINES.md`

## Restrições Técnicas Inegociáveis:
1. Monorepo dividido em `/backend` (Java 21 + Spring Boot 3 + Maven) e `/frontend` (Next.js/React + Tailwind + shadcn/ui + JSZip).
2. Persistência: Spring Data JPA com H2 em memória no perfil `dev`/`test` e PostgreSQL + Flyway no perfil `prod`.
3. Zero-Retention de Código: NUNCA salve arquivos `.java` enviados pelo usuário em disco ou no banco de dados. Todo o parse do `JavaParser` ocorre em memória RAM.
4. Sempre rode os testes via terminal integrado (`mvn test` no backend / `npm run build` no frontend) antes de concluir uma missão.
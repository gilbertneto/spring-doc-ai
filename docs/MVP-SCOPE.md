# MVP-SCOPE: Definição de Escopo e Corte de Gordura

## 1. Matriz de Priorização (MoSCoW)

### MUST HAVE (Entra obrigatoriamente no MVP)
- **M1 - Input Duplo Descomplicado:** Campo para colar URL de repositório público do GitHub + Dropzone para upload de `.zip`/pasta com pré-filtragem client-side (no navegador) de arquivos `.java` e `pom.xml`/`build.gradle`.
- **M2 - Engine AST para Java/Spring Boot:** Extrator baseado em `JavaParser` focado em `@RestController`, `@RequestMapping` (e variantes `@GetMapping`, `@PostMapping`, etc.), `@Service`, `@Entity`, DTOs/Records e anotações do `jakarta.validation`.
- **M3 - Geração dos 3 Artefatos Core via LLM:**
  1. `API-REFERENCE.md`
  2. `INTEGRATION-GUIDE.md`
  3. `ARCHITECTURE.md` (incluindo diagrama Mermaid.js básico de entidades e camadas).
- **M4 - Persistência Dual com Spring Data JPA:** Configuração pronta com **H2** (perfil `dev`/`test`) e **PostgreSQL** (perfil `prod`) salvando apenas metadados do job e os 3 Markdowns gerados.
- **M5 - Workspace de Leitura e Exportação:** Tela com visualizador de Markdown em 3 abas, botão "Copiar Markdown" individual e botão "Baixar Pacote (.zip)" gerado direto no cliente.

### SHOULD HAVE (Importante, entra se não atrasar a entrega principal)
- **S1 - Streaming de Status (SSE ou Polling Curto):** Feedback visual passo a passo na tela mostrando quantas classes `@RestController` e `@Entity` foram encontradas antes da LLM terminar de responder.
- **S2 - Suporte a BYOK (Bring Your Own Key):** Modal simples de configurações no client onde o usuário pode colar sua própria chave da OpenAI ou Gemini caso queira rodar projetos grandes sem limite.
- **S3 - Histórico Recente na Sidebar/Drawer:** Listagem das últimas gerações salvas no banco (via `GenerationJobRepository`) para consulta rápida.

### COULD HAVE (Desejável para pós-lançamento imediato)
- **C1 - Exportação de Collection Postman/Insomnia:** Aproveitar a AST extraída para gerar também um JSON importável no Postman junto com os 3 arquivos `.md`.
- **C2 - Leitura de `ControllerAdvice` (`@ExceptionHandler`):** Mapear o tratamento global de erros do Spring para documentar o formato exato do payload de erro padrão da API.

---

## 2. O que FICA FORA do MVP (Anti-Escopo / Viagem)
- ❌ **Suporte a outras linguagens/frameworks:** Nada de Node.js, Python, Go, C# ou mesmo outros frameworks Java (Quarkus/Micronaut) neste momento. O parser é 100% otimizado para anotações do Spring Boot.
- ❌ **Autenticação OAuth com GitHub/GitLab/Bitbucket:** Não haverá botão "Conectar conta do GitHub para listar repos privados" no MVP. Repositórios privados são resolvidos fazendo upload do `.zip` da pasta `src`.
- ❌ **Portal Hospedado Multi-Tenant (SaaS estilo ReadMe.io):** O sistema não vai hospedar subdomínios públicos para clientes finais (`api.empresa.com`). A entrega final do MVP são os arquivos Markdown.
- ❌ **Execução/Teste de Endpoints ao Vivo (Try It Out):** Não haverá cliente HTTP embutido na documentação gerada para disparar requisições contra servidores reais.
- ❌ **Armazenamento de Código-Fonte em Disco ou Banco:** O código Java nunca é salvo no PostgreSQL/H2 nem em buckets S3.

---

## 3. Hipóteses de Validação do MVP
1. **Hipótese de Eficiência da AST (H1):** Extrair apenas a estrutura declarativa dos `Controllers`, `DTOs`, `Entities` e as assinaturas/exceções dos `Services` reduz o tamanho do prompt em mais de 75% em relação ao código bruto, mantendo contexto suficiente para a LLM deduzir as regras de negócio.
2. **Hipótese de Valor do Caso de Uso (H2):** Desenvolvedores que integram ERPs valorizam mais o arquivo `INTEGRATION-GUIDE.md` (que explica a ordem lógica das chamadas: ex. *Autenticar -> Consultar Estoque -> Criar Pedido*) do que a listagem alfabética de endpoints de um Swagger comum.
3. **Hipótese de Atrito Zero no Upload (H3):** A filtragem client-side (mostrando ao usuário que apenas arquivos `.java` limpos estão subindo e que nada de código fica salvo no banco) elimina a objeção de usar a ferramenta em projetos legados fechados.

---

## 4. Métricas do MVP
- **Taxa de Conclusão do Parser:** `%` de repositórios Spring Boot válidos que passam pelo `JavaParser` sem estourar exceção de sintaxe (Meta: maior que 95% para Java 11 a 21).
- **Tempo Médio de Geração (TTFD - Time to First Doc):** Tempo entre o clique em "Gerar Documentação" e a renderização dos 3 arquivos na tela (Meta: menor que 90s para projetos de até 40 endpoints).
- **Taxa de Download/Cópia:** `%` de jobs concluídos onde o usuário efetivamente clica em "Baixar .zip" ou "Copiar Markdown" (Meta: maior que 70% — indica que o conteúdo gerado teve qualidade percebida).
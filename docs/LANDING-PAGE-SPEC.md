# LANDING-PAGE-SPEC: Estrutura e Arquitetura de Conversão
> ⚠️ **Nota:** Especificação estritamente estrutural e visual (sem copywriting final). Todo o fluxo prioriza o conceito *Product-Led*, onde a própria dobra principal já funciona como entrada para a ferramenta.

## 1. Visão Geral das Seções
1. **Header (Navegação Minimalista)**
2. **Hero Section + Interactive Input Box (Acima da Dobra)**
3. **Seção Comparativa "Antes vs. Depois" (Swagger Cru vs. Documentação Contextual)**
4. **Seção "Como Funciona por Baixo do Capô" (Pipeline AST em 3 Passos)**
5. **Seção de Amostra dos 3 Artefatos (Preview Interativo em Abas)**
6. **Banner de Privacidade & Segurança para Código Legado**
7. **Footer Enxuto**

---

## 2. Detalhamento por Seção

### Seção 1: Header
- **Objetivo:** Navegação rápida, acesso ao histórico local/banco e configuração de API Key sem distrair do fluxo principal.
- **Layout:** Barra fixa no topo (`height: 64px`), `backdrop-blur-md`, fundo translúcido claro, container centralizado (`max-width: 1200px`) com 3 áreas: Logo à esquerda, links de âncora no centro, ações à direita.
- **Elementos Visuais:**
  - Ícone geométrico minimalista + Nome do Produto + Badge sutil `Spring Boot AST`.
  - Botão secundário (Ghost/Outline) para abrir o Drawer de "Histórico de Gerações".
  - Botão de ícone (Engrenagem/Chave) para abrir o Modal de "API Key (BYOK)".

### Seção 2: Hero Section + Interactive Input Box
- **Objetivo:** Permitir que o usuário inicie a geração imediatamente em menos de 10 segundos (colando link do GitHub ou soltando o `.zip`).
- **Layout:** Centralizado, coluna única (`max-width: 768px`), espaçamento vertical generoso (`padding-top: 80px`).
- **Elementos Visuais:**
  - Badge superior em pílula com indicador de versão/suporte (`Java 11–21 • Spring Boot 2 & 3`).
  - Headline principal de alto impacto (2 linhas) + Subheadline curta explicando a geração dos 3 arquivos `.md`.
  - **Componente Central (Input Card Unificado):**
    - Caixa elevada com borda sutil e sombra suave contendo duas abas de entrada rápida:
      - **Aba 1 (Padrão - Prioridade):** Input de texto largo para URL do GitHub (`https://github.com/org/repo`) com botão primário de ação acoplado à direita.
      - **Aba 2:** Área de Dropzone compacta (`Arraste a pasta src/ ou arquivo .zip`) com contador instantâneo de arquivos `.java` detectados pelo filtro client-side.
    - Linha inferior dentro do card com 3 "Pílulas de Exemplo Rápido" (repositórios Spring Boot públicos conhecidos de exemplo para o usuário testar com 1 clique caso não tenha um repo em mãos agora).

### Seção 3: Comparativo "Antes vs. Depois"
- **Objetivo:** Mostrar visualmente por que um Swagger/OpenAPI sem contexto falha em integrações de ERP e como o guia gerado resolve o problema.
- **Layout:** Grid de 2 colunas lado a lado (`50% / 50%` no desktop, empilhado no mobile), dentro de um container de `1100px`.
- **Elementos Visuais:**
  - **Card Esquerdo (O Problema):** Visual acinzentado/opaco mostrando uma rota genérica `POST /api/v1/faturamento` onde só aparecem tipos primitivos sem explicação de regra de negócio. Badge vermelho sutil de alerta.
  - **Card Direito (A Solução):** Visual nítido com borda destacada mostrando o trecho equivalente gerado pela ferramenta: explicação das validações extraídas do `Service`, pré-requisito de chamada anterior e payload comentado. Badge verde/escuro de destaque.

### Seção 4: Pipeline Técnico (Como Funciona)
- **Objetivo:** Gerar autoridade técnica para desenvolvedores Java/Spring explicando que não é um "prompt genérico", mas sim análise sintática real (AST).
- **Layout:** Grid horizontal de 3 cards conectados por setas tracejadas sutis.
- **Elementos Visuais:**
  - **Card 1 (Triagem Client-Side):** Ícone de filtro/navegador + lista visual do que é descartado no browser vs. arquivos `.java` selecionados.
  - **Card 2 (Extração AST via JavaParser):** Ícone de árvore sintática + badges de anotações reconhecidas (`@RestController`, `@Service`, `@Entity`, `@Valid`).
  - **Card 3 (Síntese em 3 Camadas):** Ícone dos 3 arquivos `.md` finais prontos para download.

### Seção 5: Preview Interativo dos 3 Documentos
- **Objetivo:** Deixar o visitante folhear um exemplo real dos 3 arquivos gerados antes mesmo de subir o próprio código.
- **Layout:** Container estilo janela de IDE/Editor em Light Mode (`max-width: 1024px`) com barra de abas superior.
- **Elementos Visuais:**
  - 3 Abas clicáveis: `1. API-REFERENCE.md`, `2. INTEGRATION-GUIDE.md`, `3. ARCHITECTURE.md`.
  - Conteúdo interno com formatação Markdown impecável: tabelas de endpoints, blocos de código `cURL` e JSON em fonte monoespaçada, e um diagrama de arquitetura renderizado via Mermaid.js na terceira aba.

### Seção 6: Bloco de Confiança e Arquitetura Stateless
- **Objetivo:** Quebrar a objeção de segurança de Tech Leads e donos de sistemas legados privados.
- **Layout:** Card horizontal único de largura total (`max-width: 1024px`), fundo levemente contrastante (`#F4F4F5`), dividido em 3 pontos objetivos com ícones de escudo/memória/banco.
- **Elementos Visuais:**
  - Indicador visual claro do ciclo de vida do código: `Browser Filter` -> `RAM Parser (Stateless)` -> `Immediate Wipe`.

---

## 3. Hierarquia de CTAs (Calls to Action)
1. **CTA Primário (Foco Total):** Botão de submissão dentro do Input Card no Hero (Ação imediata: iniciar extração da URL ou do `.zip`). Estilo sólido de alto contraste (`#18181B` com texto `#FAFAFA`).
2. **CTA Secundário (Ativação sem fricção):** Pílulas de "Testar com repositório Spring de exemplo" logo abaixo do input principal (para quem quer ver funcionando em 1 clique).
3. **CTA Terciário (Pós-geração na Workspace):** Botão primário "Baixar Pacote (.zip com 3 arquivos)" no topo direito do painel de resultados + botão secundário "Copiar Markdown" dentro de cada aba.
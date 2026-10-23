# BRIEF: SpringDocAI (Gerador Inteligente de Documentação para Legados Spring Boot)

## 1. Problema
Integrar sistemas corporativos (como ERPs e backbones legados em Java/Spring Boot) é um gargalo porque o Swagger/OpenAPI tradicional quando existe apenas lista rotas e tipos primitivos, omitindo regras de negócio escondidas nos Services, validações complexas e a ordem correta de chamada dos endpoints.

## 2. Solução
Uma aplicação web client-first combinada com um motor de extração sintática (AST via JavaParser) em Spring Boot que lê repositórios públicos do GitHub ou pacotes `.zip` pré-filtrados no navegador, extrai a estrutura real das camadas (`Controllers`, `DTOs`, `Services` e `Entities`) e utiliza LLMs com prompts estruturados para gerar 3 artefatos Markdown prontos para uso:
1. `API-REFERENCE.md` (Documentação técnica detalhada com payloads e regras de validação).
2. `INTEGRATION-GUIDE.md` (Guia prático de casos de uso e fluxos passo a passo de integração).
3. `ARCHITECTURE.md` (Visão geral da arquitetura, módulos e modelo de domínio).

## 3. Público-Alvo
- **Primário:** Tech Leads, Arquitetos e Desenvolvedores Backend Sênior responsáveis por sistemas legados em Java/Spring Boot (Software Houses, ERPs, Fintechs) que precisam fornecer documentação de integração clara para parceiros ou novos desenvolvedores.
- **Secundário:** Desenvolvedores Java que desejam documentar projetos existentes rapidamente sem poluir o código-fonte com dezenas de anotações `@Schema` e `@Operation` do Swagger.

## 4. Diferencial Competitivo
- **Contexto de Negócio vs. Rota Crua:** Diferente do SpringDoc/Swagger que apenas reflete assinaturas HTTP, a ferramenta cruza o `Controller` com as assinaturas e exceções do `Service` e `Entity` para explicar *como* e *em qual sequência* consumir a API.
- **Economia de Tokens via AST Cirúrgica:** Não envia código-fonte bruto inteiro para a LLM. O backend filtra a Árvore Sintática Abstrata (AST), reduzindo o consumo de tokens em até 80% e evitando alucinações em projetos com mais de 50 endpoints.
- **Privacidade Zero-Retention para Código:** O código-fonte enviado via `.zip` é pré-filtrado no cliente, processado exclusivamente em memória RAM no servidor e descartado imediatamente após a extração da AST.

## 5. Modelo de Negócio
- **Modo BYOK (Bring Your Own Key) / Self-Hosted:** Uso gratuito informando a própria API Key (OpenAI, Anthropic ou Google Gemini) na interface client-side.
- **SaaS Freemium / Créditos (Evolução):** Cota gratuita para repositórios de até 25 endpoints; cobrança por pacote de gerações ou assinatura mensal para projetos corporativos maiores (acima de 50 endpoints) utilizando infraestrutura de LLM gerenciada.

## 6. Métricas de Sucesso (North Star & KPIs)
- **Taxa de Utilidade Direta:** Pelo menos 85% dos endpoints e fluxos gerados nos 3 arquivos `.md` não exigem reescrita manual antes de serem compartilhados com terceiros.
- **Performance de Processamento:** Tempo total de ponta a ponta (extração AST + geração LLM) inferior a 120 segundos para projetos de até 50 endpoints.
- **Redução de Payload (Eficiência AST):** Taxa de compressão de tokens superior a 70% entre o tamanho original dos arquivos `.java` e o JSON estruturado enviado ao prompt.
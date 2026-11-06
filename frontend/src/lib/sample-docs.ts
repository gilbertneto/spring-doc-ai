/** Exemplo estático dos 3 artefatos (seção "Preview Interativo"). Conteúdo ilustrativo. */
import type { GeneratedDocument } from "@/lib/api-client";

const API_REFERENCE = `# API Reference

Total de endpoints: \`3\` em \`1\` controller(s).

## OrderController

Base path: \`/api/v1/orders\`

### \`POST /api/v1/orders\`

Cria um pedido a partir do carrinho. **Pré-requisito:** o cliente deve existir (\`POST /api/v1/customers\`).

**Request body:** \`CreateOrderRequest\`

| Campo | Tipo | Validações |
| :--- | :--- | :--- |
| \`customerId\` | \`Long\` | \`@NotNull\` |
| \`items\` | \`List<OrderItemDto>\` | \`@NotEmpty\` \`@Valid\` |
| \`couponCode\` | \`String\` | \`@Size(max = 20)\` |

~~~json
{
  "customerId": 42,
  "items": [{ "sku": "ABC-123", "quantity": 2 }],
  "couponCode": "BLACK10"
}
~~~

**Resposta:** \`OrderDto\` — status \`201 CREATED\`

### \`GET /api/v1/orders/{id}\`

Busca um pedido pelo id.

| Parâmetro | Tipo | Origem | Obrigatório |
| :--- | :--- | :--- | :--- |
| \`id\` | \`Long\` | PATH_VARIABLE | sim |

## Códigos de erro

| Exceção | Status HTTP sugerido | Origem |
| :--- | :--- | :--- |
| \`OrderNotFoundException\` | 404 | OrderService.find |
| \`IllegalStateException("Estoque insuficiente")\` | 422 | OrderService.create |
`;

const INTEGRATION_GUIDE = `# Guia de Integração

URL base de exemplo: \`http://localhost:8080\`

## Caso de uso: Fechamento de pedido

1. **Cadastrar o cliente** — \`POST /api/v1/customers\`

~~~bash
curl -X POST "http://localhost:8080/api/v1/customers" \\
  -H "Content-Type: application/json" \\
  -d '{"name":"ACME Ltda","document":"12345678000199"}'
~~~

2. **Criar o pedido** com o \`id\` retornado no passo 1 — \`POST /api/v1/orders\`

~~~bash
curl -X POST "http://localhost:8080/api/v1/orders" \\
  -H "Content-Type: application/json" \\
  -d '{"customerId":42,"items":[{"sku":"ABC-123","quantity":2}]}'
~~~

3. **Consultar o pedido** — \`GET /api/v1/orders/{id}\`

> Erros esperados: \`404\` se o pedido não existir; \`422\` se não houver estoque.

## Boas práticas

- Trate respostas \`4xx\` como erros de contrato/validação e \`5xx\` com retentativa.
`;

const ARCHITECTURE = `# Arquitetura

## Stack

Build: \`MAVEN\`

- \`spring-boot-starter-web\`
- \`spring-boot-starter-data-jpa\`
- \`spring-boot-starter-validation\`

## Fluxo entre camadas

~~~mermaid
flowchart LR
    OrderController --> OrderService
    CustomerController --> CustomerService
    OrderService --> OrderRepository
~~~

## Modelo de domínio

~~~mermaid
erDiagram
    Order {
        Long id
        Customer customer
    }
    Customer {
        Long id
        String name
    }
    Order }o--|| Customer : customer
~~~
`;

export const SAMPLE_DOCUMENTS: GeneratedDocument[] = [
  { docType: "API_REFERENCE", fileName: "API-REFERENCE.md", content: API_REFERENCE },
  { docType: "INTEGRATION_GUIDE", fileName: "INTEGRATION-GUIDE.md", content: INTEGRATION_GUIDE },
  { docType: "ARCHITECTURE_OVERVIEW", fileName: "ARCHITECTURE.md", content: ARCHITECTURE },
];


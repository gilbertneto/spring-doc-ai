package com.springdocai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.ast.dto.ProjectAstSummary.ControllerInfo;
import com.springdocai.ast.dto.ProjectAstSummary.EndpointInfo;
import com.springdocai.ast.dto.ProjectAstSummary.EntityInfo;
import com.springdocai.ast.dto.ProjectAstSummary.FieldInfo;
import com.springdocai.ast.dto.ProjectAstSummary.ParameterInfo;
import com.springdocai.ast.dto.ProjectAstSummary.TypeInfo;
import com.springdocai.domain.enums.DocumentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback determinístico (modo MOCK): gera os 3 documentos localmente a partir do
 * {@link ProjectAstSummary}, sem chamar nenhuma LLM. Usado quando não há chave de API.
 */
@Component
public class MockDocumentGenerator {

    private static final String MOCK_BANNER =
            "> ⚠️ Documento gerado em **modo MOCK** (nenhuma chave de LLM foi informada). "
                    + "O conteúdo é montado de forma determinística a partir da AST extraída.\n\n";
    private static final Pattern COLLECTION = Pattern.compile("^(?:List|Set|Collection|Iterable)<(.+)>$");
    private static final Pattern PATH_VAR = Pattern.compile("\\{[^}]+}");

    private final ObjectMapper objectMapper;

    public MockDocumentGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(DocumentType type, ProjectAstSummary summary) {
        return switch (type) {
            case API_REFERENCE -> apiReference(summary);
            case INTEGRATION_GUIDE -> integrationGuide(summary);
            case ARCHITECTURE_OVERVIEW -> architecture(summary);
        };
    }

    // ------------------------------------------------------------------ API-REFERENCE

    private String apiReference(ProjectAstSummary s) {
        Map<String, TypeInfo> dtos = dtoIndex(s);
        StringBuilder sb = new StringBuilder("# API Reference\n\n").append(MOCK_BANNER);
        sb.append("Total de endpoints: `").append(s.totalEndpoints()).append("` em `")
                .append(s.controllers().size()).append("` controller(s).\n\n");

        for (ControllerInfo c : s.controllers()) {
            sb.append("## ").append(c.name()).append("\n\nBase path: `").append(c.basePath()).append("`\n\n");
            for (EndpointInfo e : c.endpoints()) {
                sb.append("### `").append(e.httpMethod()).append(' ').append(e.path()).append("`\n\n");
                if (e.javadoc() != null) {
                    sb.append(e.javadoc()).append("\n\n");
                }
                if (!e.parameters().isEmpty()) {
                    sb.append("| Parâmetro | Tipo | Origem | Obrigatório |\n| :--- | :--- | :--- | :--- |\n");
                    for (ParameterInfo p : e.parameters()) {
                        sb.append("| `").append(p.name()).append("` | `").append(p.type()).append("` | ")
                                .append(p.kind()).append(" | ").append(p.required() ? "sim" : "não").append(" |\n");
                    }
                    sb.append('\n');
                }
                if (e.requestBodyType() != null) {
                    sb.append("**Request body:** `").append(e.requestBodyType()).append("`\n\n");
                    appendFieldTable(sb, dtos.get(baseName(e.requestBodyType())));
                    sb.append("```json\n").append(json(sample(e.requestBodyType(), dtos, 0))).append("\n```\n\n");
                }
                sb.append("**Resposta:** `").append(e.responseBodyType()).append("` — status `")
                        .append(e.responseStatus() == null ? "OK" : e.responseStatus()).append("`\n\n");
                if (!"void".equalsIgnoreCase(e.responseBodyType()) && !"Void".equals(e.responseBodyType())) {
                    sb.append("```json\n").append(json(sample(e.responseBodyType(), dtos, 0))).append("\n```\n\n");
                }
            }
        }

        sb.append("## Códigos de erro\n\n");
        List<String[]> errors = errorRows(s);
        if (errors.isEmpty()) {
            sb.append("Nenhuma exceção explícita foi encontrada nos services.\n");
        } else {
            sb.append("| Exceção | Status HTTP sugerido | Origem |\n| :--- | :--- | :--- |\n");
            for (String[] row : errors) {
                sb.append("| `").append(row[0]).append("` | ").append(row[1]).append(" | ").append(row[2]).append(" |\n");
            }
        }
        return sb.toString();
    }

    private void appendFieldTable(StringBuilder sb, TypeInfo type) {
        if (type == null) {
            return;
        }
        sb.append("| Campo | Tipo | Validações |\n| :--- | :--- | :--- |\n");
        for (FieldInfo f : type.fields()) {
            sb.append("| `").append(f.name()).append("` | `").append(f.type()).append("` | ")
                    .append(f.validations().isEmpty() ? "—" : "`" + String.join("` `", f.validations()) + "`")
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private List<String[]> errorRows(ProjectAstSummary s) {
        List<String[]> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        s.services().forEach(svc -> svc.methods().forEach(m -> m.exceptions().forEach(ex -> {
            String name = ex.contains("(") ? ex.substring(0, ex.indexOf('(')) : ex;
            String origin = svc.name() + "." + methodName(m.signature());
            if (seen.add(name + "|" + origin)) {
                rows.add(new String[]{ex, String.valueOf(inferStatus(name)), origin});
            }
        })));
        return rows;
    }

    static int inferStatus(String exceptionName) {
        String n = exceptionName.toLowerCase();
        if (n.contains("notfound")) return 404;
        if (n.contains("conflict") || n.contains("duplicate") || n.contains("alreadyexists")) return 409;
        if (n.contains("unauthorized") || n.contains("authentication")) return 401;
        if (n.contains("forbidden") || n.contains("accessdenied")) return 403;
        if (n.contains("illegalargument") || n.contains("invalid") || n.contains("validation")
                || n.contains("badrequest")) return 400;
        if (n.contains("business") || n.contains("unprocessable") || n.contains("illegalstate")) return 422;
        return 500;
    }

    private static String methodName(String signature) {
        int paren = signature.indexOf('(');
        String head = paren < 0 ? signature : signature.substring(0, paren);
        return head.substring(head.lastIndexOf(' ') + 1);
    }

    // ------------------------------------------------------------------ INTEGRATION-GUIDE

    private String integrationGuide(ProjectAstSummary s) {
        Map<String, TypeInfo> dtos = dtoIndex(s);
        StringBuilder sb = new StringBuilder("# Guia de Integração\n\n").append(MOCK_BANNER);
        sb.append("URL base de exemplo: `http://localhost:8080`\n\n");

        for (ControllerInfo c : s.controllers()) {
            sb.append("## Fluxo: ").append(c.name()).append("\n\n");
            List<EndpointInfo> ordered = new ArrayList<>(c.endpoints());
            ordered.sort(Comparator.comparingInt(e -> verbOrder(e.httpMethod())));
            int step = 1;
            for (EndpointInfo e : ordered) {
                sb.append(step++).append(". **").append(e.methodName()).append("** — `")
                        .append(e.httpMethod()).append(' ').append(e.path()).append("`\n\n");
                sb.append("```bash\n").append(curl(e, dtos)).append("\n```\n\n");
            }
        }
        sb.append("## Boas práticas\n\n- Trate respostas `4xx` como erros de contrato/validação e `5xx` com retentativa.\n"
                + "- Respeite as validações dos DTOs descritas na API Reference.\n");
        return sb.toString();
    }

    private static int verbOrder(String verb) {
        return switch (verb.split(",")[0]) {
            case "POST" -> 0;
            case "GET", "ALL" -> 1;
            case "PUT", "PATCH" -> 2;
            case "DELETE" -> 3;
            default -> 4;
        };
    }

    private String curl(EndpointInfo e, Map<String, TypeInfo> dtos) {
        String verb = e.httpMethod().split(",")[0];
        if ("ALL".equals(verb)) {
            verb = "GET";
        }
        String path = PATH_VAR.matcher(e.path()).replaceAll("1");
        List<String> query = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        for (ParameterInfo p : e.parameters()) {
            if ("REQUEST_PARAM".equals(p.kind()) && p.required()) {
                query.add(p.name() + "=valor");
            } else if ("REQUEST_HEADER".equals(p.kind()) && p.required()) {
                headers.add(p.name() + ": valor");
            }
        }
        StringBuilder sb = new StringBuilder("curl -X ").append(verb).append(" \"http://localhost:8080")
                .append(path).append(query.isEmpty() ? "" : "?" + String.join("&", query)).append('"');
        headers.forEach(h -> sb.append(" \\\n  -H \"").append(h).append('"'));
        if (e.requestBodyType() != null) {
            sb.append(" \\\n  -H \"Content-Type: application/json\" \\\n  -d '")
                    .append(compactJson(sample(e.requestBodyType(), dtos, 0))).append('\'');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ ARCHITECTURE

    private String architecture(ProjectAstSummary s) {
        StringBuilder sb = new StringBuilder("# Arquitetura\n\n").append(MOCK_BANNER);

        sb.append("## Stack\n\n");
        if (s.build() == null || s.build().buildTool() == null) {
            sb.append("Manifesto de build não informado.\n\n");
        } else {
            sb.append("Build: `").append(s.build().buildTool()).append("`\n\n");
            s.build().dependencies().stream().limit(25).forEach(d -> sb.append("- `").append(d).append("`\n"));
            sb.append('\n');
        }

        sb.append("## Pacotes\n\n");
        if (s.packages() == null || s.packages().isEmpty()) {
            sb.append("não informado\n\n");
        } else {
            s.packages().forEach(p -> sb.append("- `").append(p).append("`\n"));
            sb.append('\n');
        }

        sb.append("## Camadas\n\n| Camada | Quantidade |\n| :--- | :--- |\n")
                .append("| Controllers | ").append(s.controllers().size()).append(" |\n")
                .append("| Services | ").append(s.services().size()).append(" |\n")
                .append("| DTOs / Records | ").append(s.dtos().size()).append(" |\n")
                .append("| Entidades | ").append(s.entities().size()).append(" |\n\n");

        sb.append("## Fluxo entre camadas\n\n```mermaid\nflowchart LR\n");
        for (ControllerInfo c : s.controllers()) {
            if (c.injectedServices().isEmpty()) {
                sb.append("    ").append(id(c.name())).append("\n");
            }
            for (String svc : c.injectedServices()) {
                sb.append("    ").append(id(c.name())).append(" --> ").append(id(svc)).append('\n');
            }
        }
        sb.append("```\n\n## Modelo de domínio\n\n");
        if (s.entities().isEmpty()) {
            sb.append("Nenhuma entidade `@Entity` encontrada.\n");
        } else {
            sb.append("```mermaid\n").append(erDiagram(s.entities())).append("```\n");
        }
        return sb.toString();
    }

    private String erDiagram(List<EntityInfo> entities) {
        Set<String> names = new LinkedHashSet<>();
        entities.forEach(e -> names.add(e.name()));
        StringBuilder sb = new StringBuilder("erDiagram\n");
        StringBuilder rels = new StringBuilder();
        for (EntityInfo e : entities) {
            sb.append("    ").append(id(e.name())).append(" {\n");
            for (FieldInfo f : e.fields()) {
                sb.append("        ").append(id(f.type())).append(' ').append(id(f.name())).append('\n');
                String target = innerType(f.type());
                if (!names.contains(target)) {
                    continue;
                }
                for (String ann : f.jpaAnnotations()) {
                    String card = relationship(ann);
                    if (card != null) {
                        rels.append("    ").append(id(e.name())).append(' ').append(card).append(' ')
                                .append(id(target)).append(" : ").append(id(f.name())).append('\n');
                    }
                }
            }
            sb.append("    }\n");
        }
        return sb.append(rels).toString();
    }

    private static String relationship(String annotation) {
        if (annotation.startsWith("@ManyToOne")) return "}o--||";
        if (annotation.startsWith("@OneToMany")) return "||--o{";
        if (annotation.startsWith("@OneToOne")) return "||--||";
        if (annotation.startsWith("@ManyToMany")) return "}o--o{";
        return null;
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, TypeInfo> dtoIndex(ProjectAstSummary s) {
        Map<String, TypeInfo> map = new LinkedHashMap<>();
        s.dtos().forEach(d -> map.put(d.name(), d));
        return map;
    }

    private static String baseName(String type) {
        int lt = type.indexOf('<');
        return (lt < 0 ? type : type.substring(0, lt)).trim();
    }

    private static String innerType(String type) {
        Matcher m = Pattern.compile("<(.+)>").matcher(type);
        return m.find() ? m.group(1).trim() : type.trim();
    }

    private static String id(String raw) {
        return raw.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private Object sample(String type, Map<String, TypeInfo> dtos, int depth) {
        String t = type.trim();
        Matcher m = COLLECTION.matcher(t);
        if (m.matches()) {
            return List.of(sample(m.group(1), dtos, depth));
        }
        if (t.startsWith("Map<")) {
            return Map.of("chave", "valor");
        }
        if (t.endsWith("[]")) {
            return List.of(sample(t.substring(0, t.length() - 2), dtos, depth));
        }
        switch (t) {
            case "String", "CharSequence":
                return "string";
            case "Long", "long", "Integer", "int", "Short", "short", "Byte", "byte":
                return 1;
            case "Double", "double", "Float", "float", "BigDecimal":
                return 1.5;
            case "Boolean", "boolean":
                return true;
            case "UUID":
                return "3fa85f64-5717-4562-b3fc-2c963f66afa6";
            case "LocalDate":
                return "2025-01-31";
            case "LocalDateTime", "Instant", "OffsetDateTime", "ZonedDateTime", "Date":
                return "2025-01-31T12:00:00";
            default:
                break;
        }
        TypeInfo dto = dtos.get(baseName(t));
        if (dto != null && depth < 3) {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (FieldInfo f : dto.fields()) {
                obj.put(f.name(), sample(f.type(), dtos, depth + 1));
            }
            return obj;
        }
        return "string";
    }

    private String json(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String compactJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}


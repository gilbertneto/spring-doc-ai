package com.springdocai.ast.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.ast.dto.ProjectAstSummary.BuildInfo;
import com.springdocai.ast.dto.ProjectAstSummary.ControllerInfo;
import com.springdocai.ast.dto.ProjectAstSummary.EndpointInfo;
import com.springdocai.ast.dto.ProjectAstSummary.EntityInfo;
import com.springdocai.ast.dto.ProjectAstSummary.FieldInfo;
import com.springdocai.ast.dto.ProjectAstSummary.ParameterInfo;
import com.springdocai.ast.dto.ProjectAstSummary.ServiceInfo;
import com.springdocai.ast.dto.ProjectAstSummary.ServiceMethodInfo;
import com.springdocai.ast.dto.ProjectAstSummary.TypeInfo;
import com.springdocai.ast.exception.NoEndpointsFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrator sintático cirúrgico (RF05). Opera 100% em memória: recebe um mapa
 * {@code caminho -> conteúdo} e nunca grava nada em disco (Zero-Retention).
 */
@Service
public class JavaParserAstExtractorService {

    public static final String NO_ENDPOINTS_MESSAGE =
            "Nenhum endpoint Spring Web (@RestController) foi encontrado neste projeto.";

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of("RestController", "Controller");
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of("Entity", "Document");
    private static final Set<String> LOMBOK_ANNOTATIONS = Set.of(
            "Data", "Value", "Getter", "Setter", "Builder", "AllArgsConstructor",
            "NoArgsConstructor", "RequiredArgsConstructor", "ToString", "EqualsAndHashCode");
    private static final Map<String, String> MAPPING_ANNOTATIONS = Map.of(
            "GetMapping", "GET",
            "PostMapping", "POST",
            "PutMapping", "PUT",
            "DeleteMapping", "DELETE",
            "PatchMapping", "PATCH",
            "RequestMapping", "ALL");
    private static final Map<String, String> PARAMETER_KINDS = Map.of(
            "PathVariable", "PATH_VARIABLE",
            "RequestParam", "REQUEST_PARAM",
            "RequestBody", "REQUEST_BODY",
            "RequestHeader", "REQUEST_HEADER");
    private static final Set<String> VALIDATION_ANNOTATIONS = Set.of(
            "NotNull", "NotBlank", "NotEmpty", "Size", "Pattern", "Email", "Min", "Max",
            "Positive", "PositiveOrZero", "Negative", "NegativeOrZero", "DecimalMin", "DecimalMax",
            "Digits", "Past", "PastOrPresent", "Future", "FutureOrPresent", "AssertTrue",
            "AssertFalse", "Valid", "Null");
    private static final Set<String> JPA_ANNOTATIONS = Set.of(
            "Id", "Column", "OneToMany", "ManyToOne", "OneToOne", "ManyToMany",
            "JoinColumn", "JoinTable", "Enumerated", "GeneratedValue", "Embedded");
    /** Tipos genéricos que NÃO são envelopes: mantidos como estão (ex.: List&lt;OrderDto&gt;). */
    private static final Set<String> COLLECTION_LIKE = Set.of(
            "List", "Set", "Collection", "Iterable", "Page", "Slice", "Map", "Flux", "Stream");
    private static final Set<String> DTO_SUFFIXES_LOWER = Set.of("dto", "request", "response");
    private static final Pattern XML_ARTIFACT = Pattern.compile("<artifactId>\\s*([^<\\s]+)\\s*</artifactId>");
    private static final Pattern GRADLE_DEP = Pattern.compile("['\"]([\\w.\\-]+:[\\w.\\-]+)(?::[^'\"]*)?['\"]");

    /**
     * @param files mapa {@code caminho relativo -> conteúdo} (somente .java e pom.xml/build.gradle)
     * @throws NoEndpointsFoundException EC01, quando não há nenhum endpoint Spring Web
     */
    public ProjectAstSummary extract(Map<String, String> files) {
        JavaParser parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

        List<TypeDeclaration<?>> types = new ArrayList<>();
        String manifestName = null;
        String manifestContent = null;

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String path = entry.getKey().replace('\\', '/');
            String content = entry.getValue();
            if (content == null || path.contains("src/test/")) {
                continue;
            }
            if (path.endsWith(".java")) {
                ParseResult<CompilationUnit> result = parser.parse(content);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    types.addAll(cu.findAll(ClassOrInterfaceDeclaration.class));
                    types.addAll(cu.findAll(RecordDeclaration.class));
                }
            } else if (manifestName == null && (path.endsWith("pom.xml")
                    || path.endsWith("build.gradle") || path.endsWith("build.gradle.kts"))) {
                manifestName = path;
                manifestContent = content;
            }
        }

        Map<String, TypeDeclaration<?>> index = new LinkedHashMap<>();
        for (TypeDeclaration<?> td : types) {
            index.putIfAbsent(td.getNameAsString(), td);
        }

        Set<String> serviceNames = new HashSet<>();
        for (TypeDeclaration<?> td : types) {
            if (hasAnnotation(td, Set.of("Service"))) {
                serviceNames.add(td.getNameAsString());
                if (td instanceof ClassOrInterfaceDeclaration cd) {
                    cd.getImplementedTypes().forEach(t -> serviceNames.add(t.getNameAsString()));
                }
            }
        }

        // Controllers + endpoints
        List<ControllerInfo> controllers = new ArrayList<>();
        Set<String> referencedTypes = new LinkedHashSet<>();
        int totalEndpoints = 0;
        for (TypeDeclaration<?> td : types) {
            if (td instanceof ClassOrInterfaceDeclaration cd && hasAnnotation(cd, CONTROLLER_ANNOTATIONS)) {
                ControllerInfo info = buildController(cd, index, serviceNames, referencedTypes);
                controllers.add(info);
                totalEndpoints += info.endpoints().size();
            }
        }

        // EC01
        if (totalEndpoints == 0) {
            throw new NoEndpointsFoundException(NO_ENDPOINTS_MESSAGE);
        }

        // Services
        List<ServiceInfo> services = new ArrayList<>();
        for (TypeDeclaration<?> td : types) {
            if (td instanceof ClassOrInterfaceDeclaration cd && hasAnnotation(cd, Set.of("Service"))) {
                services.add(buildService(cd));
            }
        }

        // Entities
        List<EntityInfo> entities = new ArrayList<>();
        for (TypeDeclaration<?> td : types) {
            if (hasAnnotation(td, ENTITY_ANNOTATIONS)) {
                entities.add(new EntityInfo(td.getNameAsString(), tableName(td),
                        lombokAnnotations(td), fields(td)));
            }
        }

        // DTOs / Records
        Set<String> dtoNames = resolveReferencedTypes(referencedTypes, index);
        List<TypeInfo> dtos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TypeDeclaration<?> td : types) {
            String name = td.getNameAsString();
            if (isControllerOrServiceOrEntity(td)) {
                continue;
            }
            if ((dtoNames.contains(name) || isDtoCandidate(td)) && seen.add(name)) {
                dtos.add(new TypeInfo(name, td instanceof RecordDeclaration ? "RECORD" : "CLASS",
                        lombokAnnotations(td), fields(td)));
            }
        }

        BuildInfo build = buildInfo(manifestName, manifestContent);
        return new ProjectAstSummary(build, controllers, services, dtos, entities,
                totalEndpoints, types.size());
    }

    // ---------------------------------------------------------------- controllers

    private ControllerInfo buildController(ClassOrInterfaceDeclaration cd,
                                           Map<String, TypeDeclaration<?>> index,
                                           Set<String> serviceNames,
                                           Set<String> referencedTypes) {
        String basePath = firstOrEmpty(classPaths(cd, index, new HashSet<>()));

        List<MethodDeclaration> methods = new ArrayList<>();
        Set<String> signatures = new HashSet<>();
        collectMethods(cd, index, methods, signatures, new HashSet<>());

        List<EndpointInfo> endpoints = new ArrayList<>();
        for (MethodDeclaration m : methods) {
            endpoints.addAll(buildEndpoints(m, basePath, referencedTypes));
        }

        return new ControllerInfo(cd.getNameAsString(), basePath.isEmpty() ? "/" : normalize(basePath),
                injectedServices(cd, serviceNames), endpoints);
    }

    /** Resolve o path de classe, herdando do ancestral quando a classe não declara (EC03). */
    private List<String> classPaths(ClassOrInterfaceDeclaration cd,
                                    Map<String, TypeDeclaration<?>> index, Set<String> visited) {
        if (!visited.add(cd.getNameAsString())) {
            return List.of();
        }
        for (AnnotationExpr a : cd.getAnnotations()) {
            if ("RequestMapping".equals(simpleName(a))) {
                List<String> paths = attrValues(a, false, "value", "path");
                if (!paths.isEmpty()) {
                    return paths;
                }
            }
        }
        for (ClassOrInterfaceType parent : cd.getExtendedTypes()) {
            TypeDeclaration<?> p = index.get(parent.getNameAsString());
            if (p instanceof ClassOrInterfaceDeclaration pcd) {
                List<String> paths = classPaths(pcd, index, visited);
                if (!paths.isEmpty()) {
                    return paths;
                }
            }
        }
        return List.of();
    }

    private void collectMethods(ClassOrInterfaceDeclaration cd, Map<String, TypeDeclaration<?>> index,
                                List<MethodDeclaration> out, Set<String> signatures, Set<String> visited) {
        if (!visited.add(cd.getNameAsString())) {
            return;
        }
        for (MethodDeclaration m : cd.getMethods()) {
            String sig = m.getNameAsString() + m.getParameters().stream()
                    .map(p -> p.getType().asString()).toList();
            if (signatures.add(sig)) {
                out.add(m);
            }
        }
        for (ClassOrInterfaceType parent : cd.getExtendedTypes()) {
            TypeDeclaration<?> p = index.get(parent.getNameAsString());
            if (p instanceof ClassOrInterfaceDeclaration pcd) {
                collectMethods(pcd, index, out, signatures, visited);
            }
        }
    }

    private List<EndpointInfo> buildEndpoints(MethodDeclaration m, String basePath, Set<String> referencedTypes) {
        AnnotationExpr mapping = null;
        for (AnnotationExpr a : m.getAnnotations()) {
            if (MAPPING_ANNOTATIONS.containsKey(simpleName(a))) {
                mapping = a;
                break;
            }
        }
        if (mapping == null) {
            return List.of();
        }

        String mappingName = simpleName(mapping);
        String httpMethod = MAPPING_ANNOTATIONS.get(mappingName);
        if ("RequestMapping".equals(mappingName)) {
            List<String> verbs = attrValues(mapping, true, "method").stream()
                    .map(JavaParserAstExtractorService::afterLastDot).toList();
            if (!verbs.isEmpty()) {
                httpMethod = String.join(",", verbs);
            }
        }

        List<String> paths = attrValues(mapping, false, "value", "path");
        if (paths.isEmpty()) {
            paths = List.of("");
        }

        List<ParameterInfo> parameters = new ArrayList<>();
        String requestBodyType = null;
        for (Parameter p : m.getParameters()) {
            for (AnnotationExpr a : p.getAnnotations()) {
                String kind = PARAMETER_KINDS.get(simpleName(a));
                if (kind == null) {
                    continue;
                }
                List<String> names = attrValues(a, false, "value", "name");
                String name = names.isEmpty() ? p.getNameAsString() : names.get(0);
                parameters.add(new ParameterInfo(name, p.getType().asString(), kind, isRequired(a)));
                if ("REQUEST_BODY".equals(kind)) {
                    requestBodyType = p.getType().asString();
                    collectTypeNames(p.getType(), referencedTypes);
                }
                break;
            }
        }

        Type returnType = m.getType();
        String unwrapped = unwrap(returnType);
        collectTypeNames(unwrapType(returnType), referencedTypes);

        String status = null;
        for (AnnotationExpr a : m.getAnnotations()) {
            if ("ResponseStatus".equals(simpleName(a))) {
                List<String> values = attrValues(a, false, "value", "code");
                if (!values.isEmpty()) {
                    status = afterLastDot(values.get(0));
                }
            }
        }

        String javadoc = m.getJavadoc().map(j -> j.getDescription().toText().trim())
                .filter(s -> !s.isEmpty()).orElse(null);

        List<EndpointInfo> result = new ArrayList<>();
        for (String path : paths) {
            result.add(new EndpointInfo(httpMethod, joinPaths(basePath, path), m.getNameAsString(),
                    parameters, requestBodyType, returnType.asString(), unwrapped, status, javadoc));
        }
        return result;
    }

    private boolean isRequired(AnnotationExpr a) {
        if (a instanceof NormalAnnotationExpr n) {
            for (MemberValuePair pair : n.getPairs()) {
                if ("required".equals(pair.getNameAsString())
                        && pair.getValue() instanceof BooleanLiteralExpr b && !b.getValue()) {
                    return false;
                }
                if ("defaultValue".equals(pair.getNameAsString())) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> injectedServices(ClassOrInterfaceDeclaration cd, Set<String> serviceNames) {
        Set<String> found = new LinkedHashSet<>();
        for (FieldDeclaration f : cd.getFields()) {
            addIfService(f.getElementType().asString(), serviceNames, found);
        }
        for (ConstructorDeclaration c : cd.getConstructors()) {
            for (Parameter p : c.getParameters()) {
                addIfService(p.getType().asString(), serviceNames, found);
            }
        }
        return new ArrayList<>(found);
    }

    private void addIfService(String typeName, Set<String> serviceNames, Set<String> out) {
        if (serviceNames.contains(typeName) || typeName.endsWith("Service")) {
            out.add(typeName);
        }
    }

    // ---------------------------------------------------------------- types / generics (EC03)

    /** Representação textual do tipo de payload desempacotado. */
    private String unwrap(Type type) {
        return unwrapType(type).asString();
    }

    /**
     * Desempacota envelopes genéricos ({@code ResponseEntity<ApiResponse<MeuDTO>>} -> {@code MeuDTO}),
     * preservando coleções ({@code List<MeuDTO>}).
     */
    private Type unwrapType(Type type) {
        Type current = type;
        while (current instanceof ClassOrInterfaceType c
                && c.getTypeArguments().isPresent()
                && !c.getTypeArguments().get().isEmpty()
                && !COLLECTION_LIKE.contains(c.getNameAsString())) {
            current = c.getTypeArguments().get().get(0);
        }
        return current;
    }

    private void collectTypeNames(Type type, Set<String> out) {
        if (type instanceof ClassOrInterfaceType c) {
            out.add(c.getNameAsString());
        }
        for (ClassOrInterfaceType nested : type.findAll(ClassOrInterfaceType.class)) {
            out.add(nested.getNameAsString());
        }
    }

    /** Fecho transitivo dos tipos referenciados pelos endpoints (via campos). */
    private Set<String> resolveReferencedTypes(Set<String> seeds, Map<String, TypeDeclaration<?>> index) {
        Set<String> resolved = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            String name = queue.poll();
            TypeDeclaration<?> td = index.get(name);
            if (td == null || !resolved.add(name)) {
                continue;
            }
            Set<String> next = new LinkedHashSet<>();
            if (td instanceof RecordDeclaration rd) {
                rd.getParameters().forEach(p -> collectTypeNames(p.getType(), next));
            } else {
                td.getFields().forEach(f -> f.getVariables().forEach(v -> collectTypeNames(v.getType(), next)));
            }
            queue.addAll(next);
        }
        return resolved;
    }

    private boolean isDtoCandidate(TypeDeclaration<?> td) {
        if (td instanceof RecordDeclaration) {
            return true;
        }
        String name = td.getNameAsString().toLowerCase();
        for (String suffix : DTO_SUFFIXES_LOWER) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return hasAnnotation(td, Set.of("Data", "Value"));
    }

    private boolean isControllerOrServiceOrEntity(TypeDeclaration<?> td) {
        return hasAnnotation(td, CONTROLLER_ANNOTATIONS)
                || hasAnnotation(td, Set.of("Service"))
                || hasAnnotation(td, ENTITY_ANNOTATIONS);
    }

    // ---------------------------------------------------------------- services

    private ServiceInfo buildService(ClassOrInterfaceDeclaration cd) {
        List<ServiceMethodInfo> methods = new ArrayList<>();
        for (MethodDeclaration m : cd.getMethods()) {
            if (!m.isPublic()) {
                continue;
            }
            Set<String> exceptions = new LinkedHashSet<>();
            m.getThrownExceptions().forEach(t -> exceptions.add(t.asString()));
            for (ThrowStmt stmt : m.findAll(ThrowStmt.class)) {
                if (stmt.getExpression() instanceof ObjectCreationExpr oce) {
                    String label = oce.getType().getNameAsString();
                    if (!oce.getArguments().isEmpty()
                            && oce.getArguments().get(0) instanceof StringLiteralExpr lit) {
                        label += "(\"" + lit.asString() + "\")";
                    }
                    exceptions.add(label);
                }
            }
            String javadoc = m.getJavadoc().map(j -> j.getDescription().toText().trim())
                    .filter(s -> !s.isEmpty()).orElse(null);
            methods.add(new ServiceMethodInfo(m.getDeclarationAsString(true, true, true),
                    javadoc, new ArrayList<>(exceptions)));
        }
        return new ServiceInfo(cd.getNameAsString(), methods);
    }

    // ---------------------------------------------------------------- DTO / entity fields

    private List<FieldInfo> fields(TypeDeclaration<?> td) {
        List<FieldInfo> result = new ArrayList<>();
        if (td instanceof RecordDeclaration rd) {
            for (Parameter p : rd.getParameters()) {
                result.add(new FieldInfo(p.getNameAsString(), p.getType().asString(),
                        filter(p.getAnnotations(), VALIDATION_ANNOTATIONS),
                        filter(p.getAnnotations(), JPA_ANNOTATIONS)));
            }
            return result;
        }
        for (FieldDeclaration f : td.getFields()) {
            if (f.isStatic()) {
                continue;
            }
            List<String> validations = filter(f.getAnnotations(), VALIDATION_ANNOTATIONS);
            List<String> jpa = filter(f.getAnnotations(), JPA_ANNOTATIONS);
            f.getVariables().forEach(v ->
                    result.add(new FieldInfo(v.getNameAsString(), v.getType().asString(), validations, jpa)));
        }
        return result;
    }

    private List<String> filter(List<AnnotationExpr> annotations, Set<String> allowed) {
        return annotations.stream()
                .filter(a -> allowed.contains(simpleName(a)))
                .map(a -> a.toString().replaceAll("\\s+", " "))
                .toList();
    }

    private List<String> lombokAnnotations(TypeDeclaration<?> td) {
        return td.getAnnotations().stream()
                .map(JavaParserAstExtractorService::simpleName)
                .filter(LOMBOK_ANNOTATIONS::contains)
                .map(n -> "@" + n)
                .toList();
    }

    private String tableName(TypeDeclaration<?> td) {
        for (AnnotationExpr a : td.getAnnotations()) {
            String n = simpleName(a);
            if ("Table".equals(n) || "Document".equals(n)) {
                List<String> names = attrValues(a, false, "name", "value", "collection");
                if (!names.isEmpty()) {
                    return names.get(0);
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- build manifest

    private BuildInfo buildInfo(String name, String content) {
        if (name == null) {
            return new BuildInfo(null, List.of());
        }
        Set<String> deps = new LinkedHashSet<>();
        if (name.endsWith("pom.xml")) {
            Matcher m = XML_ARTIFACT.matcher(content);
            while (m.find()) {
                deps.add(m.group(1));
            }
            return new BuildInfo("MAVEN", new ArrayList<>(deps));
        }
        Matcher m = GRADLE_DEP.matcher(content);
        while (m.find()) {
            deps.add(m.group(1));
        }
        return new BuildInfo("GRADLE", new ArrayList<>(deps));
    }

    // ---------------------------------------------------------------- helpers

    private static boolean hasAnnotation(TypeDeclaration<?> td, Set<String> names) {
        return td.getAnnotations().stream().anyMatch(a -> names.contains(simpleName(a)));
    }

    private static String simpleName(AnnotationExpr a) {
        return afterLastDot(a.getNameAsString());
    }

    private static String afterLastDot(String s) {
        return s.substring(s.lastIndexOf('.') + 1);
    }

    private static String firstOrEmpty(List<String> list) {
        return list.isEmpty() ? "" : list.get(0);
    }

    /**
     * @param normalOnly quando true ignora o valor de membro único (usado para atributos nomeados como {@code method})
     */
    private static List<String> attrValues(AnnotationExpr a, boolean normalOnly, String... names) {
        List<Expression> exprs = new ArrayList<>();
        if (!normalOnly && a instanceof SingleMemberAnnotationExpr s) {
            exprs.add(s.getMemberValue());
        } else if (a instanceof NormalAnnotationExpr n) {
            List<String> wanted = Arrays.asList(names);
            for (MemberValuePair pair : n.getPairs()) {
                if (wanted.contains(pair.getNameAsString())) {
                    exprs.add(pair.getValue());
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (Expression e : exprs) {
            if (e instanceof ArrayInitializerExpr arr) {
                arr.getValues().forEach(v -> out.add(literal(v)));
            } else {
                out.add(literal(e));
            }
        }
        return out;
    }

    private static String literal(Expression e) {
        return e instanceof StringLiteralExpr s ? s.asString() : e.toString();
    }

    private static String joinPaths(String base, String path) {
        return normalize(base + "/" + path);
    }

    private static String normalize(String path) {
        String p = ("/" + path).replaceAll("/+", "/");
        return p.length() > 1 && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }
}

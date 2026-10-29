package com.springdocai.ast.dto;

import java.util.List;

/**
 * Árvore sintática enxuta do projeto (somente estrutura declarativa, nunca código bruto).
 */
public record ProjectAstSummary(
        BuildInfo build,
        List<ControllerInfo> controllers,
        List<ServiceInfo> services,
        List<TypeInfo> dtos,
        List<EntityInfo> entities,
        int totalEndpoints,
        int classesAnalyzed,
        List<String> packages) {

    /** Informações do manifesto de build (pom.xml / build.gradle). */
    public record BuildInfo(String buildTool, List<String> dependencies) {
    }

    public record ControllerInfo(
            String name,
            String basePath,
            List<String> injectedServices,
            List<EndpointInfo> endpoints) {
    }

    /**
     * @param responseType     tipo declarado, ex.: {@code ResponseEntity<ApiResponse<OrderDto>>}
     * @param responseBodyType tipo de payload desempacotado (EC03), ex.: {@code OrderDto}
     */
    public record EndpointInfo(
            String httpMethod,
            String path,
            String methodName,
            List<ParameterInfo> parameters,
            String requestBodyType,
            String responseType,
            String responseBodyType,
            String responseStatus,
            String javadoc) {
    }

    /** @param kind PATH_VARIABLE | REQUEST_PARAM | REQUEST_BODY | REQUEST_HEADER */
    public record ParameterInfo(String name, String type, String kind, boolean required) {
    }

    public record ServiceInfo(String name, List<ServiceMethodInfo> methods) {
    }

    public record ServiceMethodInfo(String signature, String javadoc, List<String> exceptions) {
    }

    /** @param kind CLASS | RECORD */
    public record TypeInfo(String name, String kind, List<String> lombokAnnotations, List<FieldInfo> fields) {
    }

    public record EntityInfo(String name, String tableName, List<String> lombokAnnotations, List<FieldInfo> fields) {
    }

    /**
     * @param validations    anotações jakarta.validation, ex.: {@code @Size(min = 3)}
     * @param jpaAnnotations anotações JPA (relacionamentos, @Id, @Column...)
     */
    public record FieldInfo(String name, String type, List<String> validations, List<String> jpaAnnotations) {
    }
}

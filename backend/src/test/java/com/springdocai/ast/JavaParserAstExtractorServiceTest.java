package com.springdocai.ast;

import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.ast.dto.ProjectAstSummary.ControllerInfo;
import com.springdocai.ast.dto.ProjectAstSummary.EndpointInfo;
import com.springdocai.ast.dto.ProjectAstSummary.FieldInfo;
import com.springdocai.ast.dto.ProjectAstSummary.TypeInfo;
import com.springdocai.ast.exception.NoEndpointsFoundException;
import com.springdocai.ast.service.JavaParserAstExtractorService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class JavaParserAstExtractorServiceTest {

    private final JavaParserAstExtractorService extractor = new JavaParserAstExtractorService();

    private static final String CONTROLLER = """
            package com.acme.web;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/v1/orders")
            public class OrderController {

                private final OrderService orderService;

                public OrderController(OrderService orderService) {
                    this.orderService = orderService;
                }

                /** Busca um pedido pelo id. */
                @GetMapping("/{id}")
                public ResponseEntity<ApiResponse<OrderDto>> findById(@PathVariable("id") Long id,
                                                                      @RequestParam(required = false) String expand) {
                    return ResponseEntity.ok(new ApiResponse<>(orderService.find(id)));
                }

                @PostMapping
                @ResponseStatus(HttpStatus.CREATED)
                public ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
                    return ResponseEntity.ok(orderService.create(request));
                }

                @RequestMapping(value = "/all", method = RequestMethod.GET)
                public List<OrderDto> list() {
                    return List.of();
                }
            }
            """;

    private static final String SERVICE = """
            package com.acme.service;

            @Service
            public class OrderService {

                /** Cria um pedido. */
                public OrderDto create(CreateOrderRequest request) {
                    if (request == null) {
                        throw new IllegalArgumentException("Pedido inválido");
                    }
                    return null;
                }

                public OrderDto find(Long id) throws OrderNotFoundException {
                    throw new OrderNotFoundException();
                }

                private void helper() {}
            }
            """;

    private static final String API_RESPONSE = """
            package com.acme.web;

            public record ApiResponse<T>(T data) {}
            """;

    private static final String RECORD_DTO = """
            package com.acme.web;

            import jakarta.validation.constraints.*;

            public record CreateOrderRequest(
                    @NotBlank @Size(min = 3, max = 50) String customer,
                    @NotNull @Positive Integer quantity) {}
            """;

    private static final String LOMBOK_DTO = """
            package com.acme.web;

            import lombok.Data;

            @Data
            public class OrderDto {
                private Long id;
                private String customer;
                private static final String IGNORED = "x";
            }
            """;

    private static final String ENTITY = """
            package com.acme.domain;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "orders")
            public class Order {
                @Id
                private Long id;

                @ManyToOne
                private Customer customer;
            }
            """;

    private static final String POM = """
            <project>
              <dependencies>
                <dependency><artifactId>spring-boot-starter-web</artifactId></dependency>
              </dependencies>
            </project>
            """;

    private Map<String, String> sampleProject() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("src/main/java/com/acme/web/OrderController.java", CONTROLLER);
        files.put("src/main/java/com/acme/service/OrderService.java", SERVICE);
        files.put("src/main/java/com/acme/web/ApiResponse.java", API_RESPONSE);
        files.put("src/main/java/com/acme/web/CreateOrderRequest.java", RECORD_DTO);
        files.put("src/main/java/com/acme/web/OrderDto.java", LOMBOK_DTO);
        files.put("src/main/java/com/acme/domain/Order.java", ENTITY);
        files.put("pom.xml", POM);
        return files;
    }

    @Test
    void extractsControllerEndpointsWithFullPaths() {
        ProjectAstSummary summary = extractor.extract(sampleProject());

        assertThat(summary.totalEndpoints()).isEqualTo(3);
        assertThat(summary.controllers()).hasSize(1);

        ControllerInfo controller = summary.controllers().get(0);
        assertThat(controller.name()).isEqualTo("OrderController");
        assertThat(controller.basePath()).isEqualTo("/api/v1/orders");
        assertThat(controller.injectedServices()).containsExactly("OrderService");

        EndpointInfo get = endpoint(controller, "findById");
        assertThat(get.httpMethod()).isEqualTo("GET");
        assertThat(get.path()).isEqualTo("/api/v1/orders/{id}");
        assertThat(get.javadoc()).isEqualTo("Busca um pedido pelo id.");
        assertThat(get.parameters()).extracting("name", "kind", "required")
                .containsExactly(
                        tuple("id", "PATH_VARIABLE", true),
                        tuple("expand", "REQUEST_PARAM", false));

        EndpointInfo post = endpoint(controller, "create");
        assertThat(post.httpMethod()).isEqualTo("POST");
        assertThat(post.path()).isEqualTo("/api/v1/orders");
        assertThat(post.responseStatus()).isEqualTo("CREATED");
        assertThat(post.requestBodyType()).isEqualTo("CreateOrderRequest");

        EndpointInfo list = endpoint(controller, "list");
        assertThat(list.httpMethod()).isEqualTo("GET");
        assertThat(list.path()).isEqualTo("/api/v1/orders/all");
    }

    @Test
    void ec03_unwrapsNestedGenericResponseTypes() {
        ProjectAstSummary summary = extractor.extract(sampleProject());
        ControllerInfo controller = summary.controllers().get(0);

        EndpointInfo get = endpoint(controller, "findById");
        assertThat(get.responseType()).isEqualTo("ResponseEntity<ApiResponse<OrderDto>>");
        assertThat(get.responseBodyType()).isEqualTo("OrderDto");

        assertThat(endpoint(controller, "create").responseBodyType()).isEqualTo("OrderDto");
        assertThat(endpoint(controller, "list").responseBodyType()).isEqualTo("List<OrderDto>");
    }

    @Test
    void extractsServicePublicMethodsAndThrownExceptions() {
        ProjectAstSummary summary = extractor.extract(sampleProject());

        assertThat(summary.services()).hasSize(1);
        var service = summary.services().get(0);
        assertThat(service.name()).isEqualTo("OrderService");
        assertThat(service.methods()).hasSize(2);

        var create = service.methods().stream()
                .filter(m -> m.signature().contains("create")).findFirst().orElseThrow();
        assertThat(create.javadoc()).isEqualTo("Cria um pedido.");
        assertThat(create.exceptions()).containsExactly("IllegalArgumentException(\"Pedido inválido\")");

        var find = service.methods().stream()
                .filter(m -> m.signature().contains("find")).findFirst().orElseThrow();
        assertThat(find.exceptions()).contains("OrderNotFoundException");
    }

    @Test
    void ec02_extractsRecordsWithJakartaValidationsAndLombokClasses() {
        ProjectAstSummary summary = extractor.extract(sampleProject());

        TypeInfo request = dto(summary, "CreateOrderRequest");
        assertThat(request.kind()).isEqualTo("RECORD");
        FieldInfo customer = request.fields().get(0);
        assertThat(customer.name()).isEqualTo("customer");
        assertThat(customer.validations()).containsExactly("@NotBlank", "@Size(min = 3, max = 50)");
        assertThat(request.fields().get(1).validations()).containsExactly("@NotNull", "@Positive");

        TypeInfo orderDto = dto(summary, "OrderDto");
        assertThat(orderDto.kind()).isEqualTo("CLASS");
        assertThat(orderDto.lombokAnnotations()).containsExactly("@Data");
        assertThat(orderDto.fields()).extracting(FieldInfo::name).containsExactly("id", "customer");
    }

    @Test
    void extractsEntitiesAndBuildManifest() {
        ProjectAstSummary summary = extractor.extract(sampleProject());

        assertThat(summary.entities()).hasSize(1);
        var entity = summary.entities().get(0);
        assertThat(entity.name()).isEqualTo("Order");
        assertThat(entity.tableName()).isEqualTo("orders");
        assertThat(entity.fields().get(0).jpaAnnotations()).containsExactly("@Id");
        assertThat(entity.fields().get(1).jpaAnnotations()).containsExactly("@ManyToOne");

        assertThat(summary.build().buildTool()).isEqualTo("MAVEN");
        assertThat(summary.build().dependencies()).contains("spring-boot-starter-web");
        assertThat(summary.classesAnalyzed()).isEqualTo(6);
    }

    @Test
    void inheritsBasePathAndMethodsFromAbstractController() {
        String base = """
                @RequestMapping("/base")
                public abstract class BaseController {
                    @GetMapping("/ping")
                    public String ping() { return "pong"; }
                }
                """;
        String child = """
                @RestController
                public class ChildController extends BaseController {
                    @GetMapping("/child")
                    public String child() { return "c"; }
                }
                """;
        ProjectAstSummary summary = extractor.extract(Map.of(
                "src/main/java/BaseController.java", base,
                "src/main/java/ChildController.java", child));

        assertThat(summary.controllers().get(0).endpoints())
                .extracting(EndpointInfo::path)
                .containsExactlyInAnyOrder("/base/child", "/base/ping");
    }

    @Test
    void ec01_failsGracefullyWhenThereAreNoSpringWebEndpoints() {
        Map<String, String> files = Map.of(
                "src/main/java/Util.java", "public class Util { public static int one() { return 1; } }",
                "pom.xml", POM);

        assertThatThrownBy(() -> extractor.extract(files))
                .isInstanceOf(NoEndpointsFoundException.class)
                .hasMessage("Nenhum endpoint Spring Web (@RestController) foi encontrado neste projeto.");
    }

    @Test
    void ignoresUnparseableFilesAndTestSources() {
        Map<String, String> files = new LinkedHashMap<>(sampleProject());
        files.put("src/main/java/Broken.java", "public class {{{ not java");
        files.put("src/test/java/FakeControllerTest.java", """
                @RestController
                public class FakeController { @GetMapping("/fake") public String f() { return ""; } }
                """);

        ProjectAstSummary summary = extractor.extract(files);

        assertThat(summary.totalEndpoints()).isEqualTo(3);
        assertThat(summary.controllers()).hasSize(1);
    }

    private static EndpointInfo endpoint(ControllerInfo controller, String methodName) {
        return controller.endpoints().stream()
                .filter(e -> e.methodName().equals(methodName)).findFirst().orElseThrow();
    }

    private static TypeInfo dto(ProjectAstSummary summary, String name) {
        return summary.dtos().stream().filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }
}

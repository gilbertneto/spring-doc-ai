package com.springdocai.testsupport;

import java.util.LinkedHashMap;
import java.util.Map;

/** Projeto Spring de exemplo (em memória) usado pelos testes de workflow/controller. */
public final class SampleProjects {

    private SampleProjects() {
    }

    public static Map<String, String> orderProject() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("src/main/java/com/acme/web/OrderController.java", """
                package com.acme.web;

                @RestController
                @RequestMapping("/api/v1/orders")
                public class OrderController {
                    private final OrderService orderService;

                    public OrderController(OrderService orderService) { this.orderService = orderService; }

                    /** Busca um pedido. */
                    @GetMapping("/{id}")
                    public ResponseEntity<ApiResponse<OrderDto>> findById(@PathVariable Long id) {
                        return ResponseEntity.ok(new ApiResponse<>(orderService.find(id)));
                    }

                    @PostMapping
                    @ResponseStatus(HttpStatus.CREATED)
                    public ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
                        return ResponseEntity.ok(orderService.create(request));
                    }

                    @DeleteMapping("/{id}")
                    public void delete(@PathVariable Long id) { }
                }
                """);
        files.put("src/main/java/com/acme/service/OrderService.java", """
                package com.acme.service;

                @Service
                public class OrderService {
                    public OrderDto create(CreateOrderRequest request) {
                        throw new IllegalArgumentException("Pedido inválido");
                    }
                    public OrderDto find(Long id) {
                        throw new OrderNotFoundException("Pedido não encontrado");
                    }
                }
                """);
        files.put("src/main/java/com/acme/web/ApiResponse.java",
                "package com.acme.web; public record ApiResponse<T>(T data) {}");
        files.put("src/main/java/com/acme/web/CreateOrderRequest.java", """
                package com.acme.web;

                public record CreateOrderRequest(@NotBlank @Size(min = 3) String customer, @NotNull Integer quantity) {}
                """);
        files.put("src/main/java/com/acme/web/OrderDto.java", """
                package com.acme.web;

                @Data
                public class OrderDto {
                    private Long id;
                    private String customer;
                }
                """);
        files.put("src/main/java/com/acme/domain/Order.java", """
                package com.acme.domain;

                @Entity
                @Table(name = "orders")
                public class Order {
                    @Id private Long id;
                    @ManyToOne private Customer customer;
                }
                """);
        files.put("src/main/java/com/acme/domain/Customer.java", """
                package com.acme.domain;

                @Entity
                public class Customer {
                    @Id private Long id;
                    private String name;
                }
                """);
        files.put("pom.xml", "<project><dependencies><dependency><artifactId>spring-boot-starter-web</artifactId>"
                + "</dependency></dependencies></project>");
        return files;
    }

    /** Projeto sem nenhum endpoint (EC01). */
    public static Map<String, String> noEndpointsProject() {
        return Map.of("src/main/java/Util.java", "public class Util { }");
    }
}


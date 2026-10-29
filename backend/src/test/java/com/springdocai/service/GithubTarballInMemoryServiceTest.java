package com.springdocai.service;

import com.springdocai.service.GithubTarballInMemoryService.GithubRepoRef;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubTarballInMemoryServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private final GithubTarballInMemoryService service = new GithubTarballInMemoryService();

    @Test
    void parsesPublicGithubUrls() {
        assertThat(service.parse("https://github.com/acme/demo"))
                .isEqualTo(new GithubRepoRef("acme", "demo", null));
        assertThat(service.parse("https://github.com/acme/demo.git/"))
                .isEqualTo(new GithubRepoRef("acme", "demo", null));
        assertThat(service.parse("https://github.com/acme/demo/tree/feature/x"))
                .isEqualTo(new GithubRepoRef("acme", "demo", "feature/x"));
    }

    @Test
    void rejectsInvalidUrls() {
        for (String bad : new String[]{null, "", "http://github.com/a/b", "https://gitlab.com/a/b",
                "https://github.com/onlyowner", "file:///etc/passwd", "https://evil.com/github.com/a/b"}) {
            assertThatThrownBy(() -> service.parse(bad)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void readZipKeepsOnlyProductionJavaAndRootManifestInMemory() throws IOException {
        byte[] zip = zip(Map.of(
                "demo-abc123/pom.xml", "<project/>",
                "demo-abc123/src/main/java/com/acme/A.java", "class A {}",
                "demo-abc123/src/test/java/com/acme/ATest.java", "class ATest {}",
                "demo-abc123/target/classes/B.java", "class B {}",
                "demo-abc123/node_modules/x/C.java", "class C {}",
                "demo-abc123/module/pom.xml", "<project/>",
                "demo-abc123/README.md", "# readme",
                "demo-abc123/logo.png", "binary"));

        Map<String, String> files = service.readZip(new ByteArrayInputStream(zip), true);

        assertThat(files).containsOnlyKeys("pom.xml", "src/main/java/com/acme/A.java");
        assertThat(files.get("src/main/java/com/acme/A.java")).isEqualTo("class A {}");
    }

    @Test
    void readZipWithoutStripRootKeepsPaths() throws IOException {
        Map<String, String> files = service.readZip(
                new ByteArrayInputStream(zip(Map.of("src/main/java/A.java", "class A {}"))), false);

        assertThat(files).containsOnlyKeys("src/main/java/A.java");
    }

    @Test
    void invalidZipFailsWithIngestionException() {
        // Cabeçalho local de ZIP truncado
        byte[] corrupted = {0x50, 0x4b, 0x03, 0x04, 0x01, 0x02};
        assertThatThrownBy(() -> service.readZip(new ByteArrayInputStream(corrupted), true))
                .isInstanceOf(GithubIngestionException.class);
    }

    @Test
    void downloadsZipballFromGithubApiIntoMemory() throws IOException {
        byte[] zip = zip(Map.of("demo-1/src/main/java/A.java", "class A {}"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/repos/acme/demo/zipball", ex -> {
            ex.sendResponseHeaders(200, zip.length);
            ex.getResponseBody().write(zip);
            ex.close();
        });
        server.createContext("/repos/acme/missing/zipball", ex -> {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        });
        server.createContext("/repos/acme/limited/zipball", ex -> {
            ex.sendResponseHeaders(403, -1);
            ex.close();
        });
        server.start();
        GithubTarballInMemoryService svc = new GithubTarballInMemoryService(
                HttpClient.newHttpClient(), "http://127.0.0.1:" + server.getAddress().getPort());

        assertThat(svc.download("https://github.com/acme/demo")).containsOnlyKeys("src/main/java/A.java");
        assertThatThrownBy(() -> svc.download("https://github.com/acme/missing"))
                .isInstanceOf(GithubIngestionException.class).hasMessageContaining("não encontrado");
        assertThatThrownBy(() -> svc.download("https://github.com/acme/limited"))
                .isInstanceOf(GithubIngestionException.class).hasMessageContaining("Limite");
    }
}


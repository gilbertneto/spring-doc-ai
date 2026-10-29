package com.springdocai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Baixa um repositório público do GitHub e o lê 100% em memória (Zero-Retention): o arquivo
 * compactado é consumido como {@link InputStream} e nunca é gravado em disco.
 * <p>
 * Usa o endpoint {@code /zipball} da API do GitHub (formato ZIP, lido via {@link ZipInputStream}).
 */
@Service
public class GithubTarballInMemoryService {

    static final String DEFAULT_API_URL = "https://api.github.com";
    private static final Pattern GITHUB_URL = Pattern.compile(
            "^https://(?:www\\.)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?"
                    + "(?:/tree/([^?#]+?))?/?(?:[?#].*)?$");
    private static final long MAX_FILE_BYTES = 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;

    /** Referência a um repositório (e branch opcional). */
    public record GithubRepoRef(String owner, String repo, String branch) {
    }

    private final HttpClient httpClient;
    private final String apiBaseUrl;

    @Autowired
    public GithubTarballInMemoryService() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build(), DEFAULT_API_URL);
    }

    GithubTarballInMemoryService(HttpClient httpClient, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
    }

    /** @throws IllegalArgumentException se a URL não for de um repositório público do GitHub */
    public GithubRepoRef parse(String url) {
        Matcher m = GITHUB_URL.matcher(url == null ? "" : url.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "URL inválida. Informe um repositório público no formato https://github.com/{owner}/{repo}.");
        }
        return new GithubRepoRef(m.group(1), m.group(2), m.group(3));
    }

    /** Baixa o repositório e devolve apenas os arquivos relevantes ({@code caminho -> conteúdo}). */
    public Map<String, String> download(String url) {
        GithubRepoRef ref = parse(url);
        URI uri = URI.create(apiBaseUrl + "/repos/" + ref.owner() + "/" + ref.repo() + "/zipball"
                + (ref.branch() != null ? "/" + ref.branch() : ""));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SpringDocAI")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new GithubIngestionException("Não foi possível acessar o GitHub.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GithubIngestionException("Download do repositório interrompido.", e);
        }

        try (InputStream body = response.body()) {
            int status = response.statusCode();
            if (status == 404) {
                throw new GithubIngestionException("Repositório não encontrado ou não é público.");
            }
            if (status == 403 || status == 429) {
                throw new GithubIngestionException(
                        "Limite de requisições do GitHub atingido. Tente novamente em instantes.");
            }
            if (status != 200) {
                throw new GithubIngestionException("O GitHub respondeu HTTP " + status + ".");
            }
            return readZip(body, true);
        } catch (IOException e) {
            throw new GithubIngestionException("Falha ao ler o repositório baixado.", e);
        }
    }

    /**
     * Lê um ZIP em memória mantendo só {@code .java} de produção e o manifesto de build da raiz.
     *
     * @param stripRoot remove a pasta raiz única (padrão do zipball do GitHub)
     */
    public Map<String, String> readZip(InputStream in, boolean stripRoot) {
        Map<String, String> files = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            int entries = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName().replace('\\', '/');
                if (stripRoot) {
                    int slash = path.indexOf('/');
                    path = slash < 0 ? path : path.substring(slash + 1);
                }
                if (!isRelevant(path)) {
                    continue;
                }
                byte[] bytes = readLimited(zis);
                if (bytes == null) {
                    continue; // arquivo acima do limite: ignorado
                }
                total += bytes.length;
                if (total > MAX_TOTAL_BYTES) {
                    throw new GithubIngestionException("O repositório excede o limite de 50 MB de código relevante.");
                }
                files.put(path, new String(bytes, StandardCharsets.UTF_8));
            }
            if (entries == 0) {
                throw new GithubIngestionException("Arquivo compactado inválido ou vazio.");
            }
        } catch (IOException e) {
            throw new GithubIngestionException("Arquivo compactado inválido ou corrompido.", e);
        }
        return files;
    }

    static boolean isRelevant(String path) {
        String p = "/" + path;
        if (p.contains("/src/test/") || p.contains("/target/") || p.contains("/build/")
                || p.contains("/node_modules/") || p.contains("/.git/")) {
            return false;
        }
        if (path.endsWith(".java")) {
            return true;
        }
        return path.equals("pom.xml") || path.equals("build.gradle") || path.equals("build.gradle.kts");
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long size = 0;
        int read;
        boolean tooLarge = false;
        while ((read = in.read(buffer)) != -1) {
            size += read;
            if (size > MAX_FILE_BYTES) {
                tooLarge = true;
                continue; // drena sem acumular
            }
            out.write(buffer, 0, read);
        }
        return tooLarge ? null : out.toByteArray();
    }
}

package com.sausage.site;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpServer;

public final class SiteBuilder {
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\R(.*?)\\R---\\R?", Pattern.DOTALL);

    private SiteBuilder() {
    }

    public static void build(Path projectRoot) throws IOException {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        Path outputRoot = normalizedProjectRoot.resolve("target");
        cleanOutputDirectory(outputRoot);
        Files.createDirectories(outputRoot);

        List<Path> markdownFiles = Files.walk(normalizedProjectRoot)
                .filter(path -> path.toString().endsWith(".md"))
                .filter(path -> !path.startsWith(outputRoot))
                .filter(path -> !path.startsWith(normalizedProjectRoot.resolve("target")))
                .filter(path -> !path.startsWith(normalizedProjectRoot.resolve("lib")))
                .filter(path -> !path.startsWith(normalizedProjectRoot.resolve("data")))
                .sorted()
                .toList();

        for (Path markdownFile : markdownFiles) {
            SiloContext silo = resolveSiloContext(normalizedProjectRoot, markdownFile);
            Page page = parsePage(markdownFile);
            String baseUrl = resolveBaseUrl(normalizedProjectRoot, markdownFile);
            String relativeInput = silo.contentRoot().relativize(markdownFile).toString();
            String outputRelative = relativeInput.replaceFirst("\\.md$", ".html");
            Path outputFile = silo.outputRoot().resolve(outputRelative);
            Files.createDirectories(outputFile.getParent());

            StringBuilder cssLinks = new StringBuilder();
            StringBuilder jsTags = new StringBuilder();

            for (String libraryName : page.libraries()) {
                copyLibraryAssets(silo.siloRoot(), outputFile.getParent(), libraryName);
                List<Path> cssFiles = filesInLibraryOutput(outputFile.getParent(), libraryName, "css");
                for (Path cssFile : cssFiles) {
                    String href = outputFile.getParent().relativize(cssFile).toString().replace('\\', '/');
                    cssLinks.append("<link rel=\"stylesheet\" href=\"" + href + "\">\n");
                }
                List<Path> jsFiles = filesInLibraryOutput(outputFile.getParent(), libraryName, "js");
                for (Path jsFile : jsFiles) {
                    String src = outputFile.getParent().relativize(jsFile).toString().replace('\\', '/');
                    jsTags.append("<script src=\"" + src + "\"></script>\n");
                }
            }

            for (String dataName : page.data()) {
                copyDataBundle(silo.siloRoot(), outputFile.getParent(), dataName);
            }

            String renderedBody = renderMarkdown(page.body());
            StringBuilder appMounts = new StringBuilder();
            for (String appName : page.apps()) {
                appMounts.append("<div data-app=\"" + appName + "\"></div>\n");
            }

            String title = page.metadata().getOrDefault("title", markdownFile.getFileName().toString().replaceFirst("\\.md$", ""));
            String metadataHead = buildHeadMetadata(page.metadata());

            String html = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "  <meta charset=\"UTF-8\">\n"
                    + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                    + "  <base href=\"" + escapeHtml(baseUrl) + "\">\n"
                    + "  <title>" + escapeHtml(title) + "</title>\n"
                    + metadataHead
                    + cssLinks
                    + "</head>\n"
                    + "<body>\n"
                    + "  <main class=\"container\">\n"
                    + renderedBody
                    + appMounts
                    + "  </main>\n"
                    + jsTags
                    + "</body>\n"
                    + "</html>\n";

            Files.writeString(outputFile, html, StandardCharsets.UTF_8);
        }

        System.out.println("Built site to " + outputRoot.toAbsolutePath());
    }

    public static void preview(Path projectRoot) throws Exception {
        build(projectRoot);
        Path outputRoot = projectRoot.resolve("target");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            Path file = outputRoot.resolve(requestPath.substring(1).replace('/', FileSystems.getDefault().getSeparator().charAt(0)));
            if (file.toString().equals(outputRoot.toString()) || requestPath.equals("/")) {
                file = outputRoot.resolve("index.html");
            }
            if (Files.isDirectory(file)) {
                file = file.resolve("index.html");
            }
            if (Files.exists(file) && !Files.isDirectory(file)) {
                String contentType = guessContentType(file);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                byte[] body = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Preview server running at http://localhost:8080");
        System.out.println("Press Ctrl+C to stop.");
        Thread.currentThread().join();
    }

    private static String resolveBaseUrl(Path projectRoot, Path markdownFile) throws IOException {
        Path current = markdownFile.toAbsolutePath().normalize().getParent();
        if (current == null) {
            current = projectRoot.toAbsolutePath().normalize();
        }

        while (current != null) {
            Path siteConfig = current.resolve("site.config.json");
            if (Files.exists(siteConfig)) {
                String json = Files.readString(siteConfig, StandardCharsets.UTF_8);
                Matcher matcher = Pattern.compile("\\\"baseUrl\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
                if (matcher.find()) {
                    String baseUrl = matcher.group(1).replace("\\/", "/").replace("\\\"", "\"");
                    return normalizeBaseUrl(baseUrl);
                }
                return "/";
            }
            if (current.equals(projectRoot.toAbsolutePath().normalize())) {
                break;
            }
            current = current.getParent();
        }

        Path fallbackConfig = projectRoot.resolve("site.config.json");
        if (Files.exists(fallbackConfig)) {
            String json = Files.readString(fallbackConfig, StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\\\"baseUrl\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
            if (matcher.find()) {
                String baseUrl = matcher.group(1).replace("\\/", "/").replace("\\\"", "\"");
                return normalizeBaseUrl(baseUrl);
            }
        }
        return "/";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "/";
        }
        String normalized = baseUrl.trim();
        if (!normalized.startsWith("/")) {
            if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                if (!normalized.endsWith("/")) {
                    normalized += "/";
                }
                return normalized;
            }
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/") && !normalized.contains("://")) {
            normalized += "/";
        }
        return normalized;
    }

    private static String buildHeadMetadata(Map<String, String> metadata) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key.equals("title") || key.equals("slug") || key.equals("libraries") || key.equals("data") || key.equals("apps") || key.equals("draft")) {
                continue;
            }
            out.append("  <meta name=\"" + escapeHtml(key) + "\" content=\"" + escapeHtml(entry.getValue()) + "\">\n");
        }
        return out.toString();
    }

    private static List<Path> filesInLibraryOutput(Path outputDirectory, String libraryName, String type) {
        Path libraryDir = outputDirectory.resolve("lib").resolve(libraryName);
        if (!Files.exists(libraryDir)) {
            return List.of();
        }
        try {
            Path typeDir = libraryDir.resolve(type);
            Path scanRoot = Files.exists(typeDir) ? typeDir : libraryDir;
            return Files.walk(scanRoot)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(type.equals("css") ? ".css" : ".js"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static void copyLibraryAssets(Path siloRoot, Path outputDirectory, String libraryName) throws IOException {
        Path libraryRoot = siloRoot.resolve("lib").resolve(libraryName);
        Path outputLibraryDir = outputDirectory.resolve("lib").resolve(libraryName);

        if (Files.exists(libraryRoot)) {
            copyDirectory(libraryRoot, outputLibraryDir);
            return;
        }

        Path legacyCssRoot = siloRoot.resolve("lib").resolve("css").resolve(libraryName);
        Path legacyJsRoot = siloRoot.resolve("lib").resolve("js").resolve(libraryName);

        if (Files.exists(legacyCssRoot)) {
            copyDirectory(legacyCssRoot, outputLibraryDir.resolve("css"));
        }
        if (Files.exists(legacyJsRoot)) {
            copyDirectory(legacyJsRoot, outputLibraryDir.resolve("js"));
        }
    }

    private static void copyDataBundle(Path siloRoot, Path outputDirectory, String dataName) throws IOException {
        Path dataRoot = siloRoot.resolve("data").resolve(dataName);
        if (!Files.exists(dataRoot)) {
            return;
        }
        copyDirectory(dataRoot, outputDirectory.resolve("data").resolve(dataName));
    }

    private static void cleanOutputDirectory(Path outputRoot) throws IOException {
        if (!Files.exists(outputRoot)) {
            return;
        }
        try (var stream = Files.walk(outputRoot)) {
            var paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                if (!path.equals(outputRoot)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path targetPath = target.resolve(relative.toString().replace('\\', '/'));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Page parsePage(Path markdownFile) throws IOException {
        String content = Files.readString(markdownFile, StandardCharsets.UTF_8);
        String body = content;
        Map<String, String> metadata = new HashMap<>();
        Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String frontMatter = matcher.group(1);
            body = content.substring(matcher.end());
            for (String line : frontMatter.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("- ")) {
                    continue;
                }
                int idx = trimmed.indexOf(':');
                if (idx > 0) {
                    String key = trimmed.substring(0, idx).trim();
                    String value = trimmed.substring(idx + 1).trim();
                    if (value.isEmpty()) {
                        metadata.put(key, "");
                    } else {
                        metadata.put(key, value.replace("\"", ""));
                    }
                }
            }
            List<String> libs = new ArrayList<>();
            List<String> data = new ArrayList<>();
            List<String> apps = new ArrayList<>();
            List<String> current = null;
            String currentKey = null;
            for (String line : frontMatter.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("- ")) {
                    if (current != null) {
                        current.add(trimmed.substring(2).trim());
                    }
                    continue;
                }
                int idx = trimmed.indexOf(':');
                if (idx > 0) {
                    currentKey = trimmed.substring(0, idx).trim();
                    String value = trimmed.substring(idx + 1).trim();
                    if (value.isEmpty()) {
                        if ("libraries".equals(currentKey)) {
                            current = libs;
                        } else if ("data".equals(currentKey)) {
                            current = data;
                        } else if ("apps".equals(currentKey)) {
                            current = apps;
                        } else {
                            current = null;
                        }
                    } else {
                        current = null;
                    }
                }
            }
            metadata.put("libraries", String.join(",", libs));
            metadata.put("data", String.join(",", data));
            metadata.put("apps", String.join(",", apps));
        }

        List<String> libraries = new ArrayList<>();
        if (metadata.containsKey("libraries")) {
            String raw = metadata.get("libraries");
            if (!raw.isBlank()) {
                for (String item : raw.split(",")) {
                    String val = item.trim();
                    if (!val.isEmpty()) {
                        libraries.add(val);
                    }
                }
            }
        }

        List<String> data = new ArrayList<>();
        if (metadata.containsKey("data")) {
            String raw = metadata.get("data");
            if (!raw.isBlank()) {
                for (String item : raw.split(",")) {
                    String val = item.trim();
                    if (!val.isEmpty()) {
                        data.add(val);
                    }
                }
            }
        }

        List<String> apps = new ArrayList<>();
        if (metadata.containsKey("apps")) {
            String raw = metadata.get("apps");
            if (!raw.isBlank()) {
                for (String item : raw.split(",")) {
                    String val = item.trim();
                    if (!val.isEmpty()) {
                        apps.add(val);
                    }
                }
            }
        }

        if (libraries.isEmpty()) {
            libraries = List.of();
        }
        if (data.isEmpty()) {
            data = List.of();
        }
        if (apps.isEmpty()) {
            apps = List.of();
        }

        return new Page(body, metadata, libraries, data, apps);
    }

    private static String renderMarkdown(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        return HtmlRenderer.builder().build().render(document);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String guessContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".csv")) return "text/csv; charset=utf-8";
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return "application/yaml; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static SiloContext resolveSiloContext(Path projectRoot, Path markdownFile) {
        Path file = markdownFile.toAbsolutePath().normalize();
        Path project = projectRoot.toAbsolutePath().normalize();
        Path relative = project.relativize(file);
        Path siloRoot = project;
        Path contentRoot = project;

        int srcIndex = findSegmentIndex(relative, "src");
        if (srcIndex >= 0) {
            Path subPath = relative.subpath(0, srcIndex);
            siloRoot = project.resolve(subPath).normalize();
            contentRoot = project.resolve(subPath).resolve("src").normalize();
        } else {
            Path current = file.getParent();
            Path bestRoot = project;
            while (current != null && current.startsWith(project)) {
                if (Files.exists(current.resolve("site.config.json"))
                        || Files.exists(current.resolve("lib"))
                        || Files.exists(current.resolve("data"))) {
                    bestRoot = current;
                }
                if (current.equals(project)) {
                    break;
                }
                current = current.getParent();
            }
            siloRoot = bestRoot;
            if (siloRoot.equals(project)) {
                contentRoot = project.resolve("src");
                if (!Files.exists(contentRoot)) {
                    contentRoot = project;
                }
            } else {
                contentRoot = siloRoot.resolve("src");
                if (!Files.exists(contentRoot)) {
                    contentRoot = siloRoot;
                }
            }
        }

        String siloName = resolveSiloName(project, siloRoot);
        Path outputRoot = project.resolve("target").resolve(siloName).normalize();
        return new SiloContext(siloRoot, contentRoot, outputRoot);
    }

    private static int findSegmentIndex(Path path, String segmentName) {
        if (path == null) {
            return -1;
        }
        for (int i = 0; i < path.getNameCount(); i++) {
            if (segmentName.equals(path.getName(i).toString())) {
                return i;
            }
        }
        return -1;
    }

    private static String resolveSiloName(Path projectRoot, Path siloRoot) {
        Path relative = projectRoot.toAbsolutePath().normalize().relativize(siloRoot.toAbsolutePath().normalize());
        String relativeName = relative.toString().replace('\\', '/');
        if (relativeName.isBlank() || relativeName.equals(".")) {
            return "main";
        }
        return relativeName;
    }

    public record Page(String body, Map<String, String> metadata, List<String> libraries, List<String> data, List<String> apps) {
    }

    private record SiloContext(Path siloRoot, Path contentRoot, Path outputRoot) {
    }
}

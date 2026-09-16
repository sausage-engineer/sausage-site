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
        Path contentRoot = projectRoot.resolve("src");
        if (!Files.exists(contentRoot)) {
            contentRoot = projectRoot;
        }

        Path outputRoot = projectRoot.resolve("target");
        Files.createDirectories(outputRoot);

        Path libraryRoot = projectRoot.resolve("lib");
        List<Path> markdownFiles = Files.walk(contentRoot)
                .filter(path -> path.toString().endsWith(".md"))
                .filter(path -> !path.startsWith(outputRoot))
                .filter(path -> !path.startsWith(libraryRoot))
                .sorted()
                .toList();

        for (Path markdownFile : markdownFiles) {
            Page page = parsePage(markdownFile);
            String relativeInput = contentRoot.relativize(markdownFile).toString();
            String outputRelative = relativeInput.replaceFirst("\\.md$", ".html");
            Path outputFile = outputRoot.resolve(outputRelative);
            Files.createDirectories(outputFile.getParent());

            StringBuilder cssLinks = new StringBuilder();
            StringBuilder jsTags = new StringBuilder();

            for (String libraryName : page.libraries()) {
                copyLibraryAssets(projectRoot, outputFile.getParent(), libraryName);
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

    private static String buildHeadMetadata(Map<String, String> metadata) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key.equals("title") || key.equals("slug") || key.equals("libraries") || key.equals("apps") || key.equals("draft")) {
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

    private static void copyLibraryAssets(Path projectRoot, Path outputDirectory, String libraryName) throws IOException {
        Path libraryRoot = projectRoot.resolve("lib").resolve(libraryName);
        Path outputLibraryDir = outputDirectory.resolve("lib").resolve(libraryName);

        if (Files.exists(libraryRoot)) {
            copyDirectory(libraryRoot, outputLibraryDir);
            return;
        }

        Path legacyCssRoot = projectRoot.resolve("lib").resolve("css").resolve(libraryName);
        Path legacyJsRoot = projectRoot.resolve("lib").resolve("js").resolve(libraryName);

        if (Files.exists(legacyCssRoot)) {
            copyDirectory(legacyCssRoot, outputLibraryDir.resolve("css"));
        }
        if (Files.exists(legacyJsRoot)) {
            copyDirectory(legacyJsRoot, outputLibraryDir.resolve("js"));
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
        if (apps.isEmpty()) {
            apps = List.of();
        }

        return new Page(body, metadata, libraries, apps);
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
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    public record Page(String body, Map<String, String> metadata, List<String> libraries, List<String> apps) {
    }
}

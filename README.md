# sausage-site

A Java 25 static site generator that turns Markdown files into simple HTML pages.

## Quick start

1. Build the application:
   ```bash
   mvn package
   ```

2. Build a site from a project directory:
   ```bash
   java -jar target/sausage-site-0.1.0.jar build demo-site
   ```

3. Preview locally:
   ```bash
   java -jar target/sausage-site-0.1.0.jar preview demo-site
   ```

The build output defaults to `<project-root>/target`.

A top-level `site.json` file can configure site-level defaults. For now, the only supported key is `baseUrl`, which is emitted as a `<base href="...">` tag in the HTML `<head>`. If omitted, the app treats the base as "/".

```json
{
  "baseUrl": "/docs/"
}
```

## Project layout

- `src/` contains Markdown content files (optional; the root can also be used directly)
- `lib/` contains top-level library folders such as `bootstrap/`
- `data/` contains top-level data bundles such as `site-data/` or `content/` that hold static assets and JSON payloads copied into the generated site the same way libraries are
- `site.json` contains top-level site settings; for now, only `baseUrl` is used
- `target/` is the generated static site output, with imported libraries copied into `target/lib/<library-name>/` and imported data copied into `target/data/<bundle-name>/`

Each Markdown file may include front matter like:

```yaml
---
title: Welcome
libraries:
  - bootstrap
data:
  - site-data
apps:
  - todo
---
```

This produces HTML with Bootstrap asset links and `data-app` mount points that JavaScript can target. When the page imports `bootstrap` or `site-data`, the corresponding folder is copied into `target/lib/bootstrap/` or `target/data/site-data/` before the page is generated.

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

## Project layout

- `src/` contains Markdown content files (optional; the root can also be used directly)
- `library/` contains named asset libraries such as Bootstrap
- `target/` is the generated static site output

Each Markdown file may include front matter like:

```yaml
---
title: Welcome
libraries:
  - bootstrap
apps:
  - todo
---
```

This produces HTML with Bootstrap asset links and `data-app` mount points that JavaScript can target.

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

The site is treated as a collection of top-level silos. Each silo is a self-contained sub-site rooted at its own top-level folder, such as `pwa/my-app/`, `docs/`, or `marketing/`. Each silo can have its own `lib/` and `data/` directories, and the base URL for that silo is the URL path of that top-level folder. This keeps multiple PWAs or static sub-sites isolated from each other while allowing each one to carry the assets it needs.

Markdown files at the project root belong to a special `main` silo. They resolve `lib` and `data` references against a root-level `main/lib` and `main/data` layout, and they compile into `target/main/...` with the root config's base URL. This makes the root landing page behave like any other silo without creating a shared global asset registry.

A directory may contain a `site.config.json` file to configure that subtree. This works like a parent/child inheritance model: a page inherits the nearest ancestor config, and a child directory can override it with its own `site.config.json`.

For now, the only supported key is `baseUrl`, which is emitted as a `<base href="...">` tag in the HTML `<head>`. If omitted, the app treats the base as "/". In silo mode, the recommended value is the folder path of the silo itself, such as `/my-app/`.

```json
{
  "baseUrl": "/my-app/"
}
```

## Project layout

- `my-app/` or another top-level folder is a silo root; each silo is a standalone sub-site
- each silo may contain its own `lib/` with top-level library folders such as `bootstrap/`
- each silo may contain its own `data/` with top-level data bundles such as `site-data/` or `content/`
- `site.config.json` configures a directory and its subfolders; nearest config wins
- `target/` is the generated static site output, with imported libraries copied into `target/<silo>/lib/<library-name>/` and imported data copied into `target/<silo>/data/<bundle-name>/`

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

This produces HTML with Bootstrap asset links and `data-app` mount points that JavaScript can target. When the page imports `bootstrap` or `site-data`, the corresponding folder is copied into the current silo's output before the page is generated, so each silo carries its own self-contained bundle of CSS, JS, and data.

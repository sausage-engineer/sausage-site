# Sausage Site Specification

## 1. Overview

Sausage Site is a Java 25 static site generator for building content-driven websites from standalone Markdown pages. It is designed for developers and content authors who want fast, dependable publishing without a server runtime, database, or CMS backend.

The product generates a complete static site that can be deployed to any static host, such as GitHub Pages, Netlify, Cloudflare Pages, or any web server that serves HTML files. The application should be implemented as a Java 25 command-line tool with a simple, repeatable build and preview workflow. A popular Java Markdown library such as Flexmark is the preferred implementation choice for parsing and rendering Markdown content.

The primary site structure is a collection of top-level siloed sub-sites. Each silo is a self-contained sub-site rooted at a top-level folder under `src/`, such as `src/my-app/`, `src/docs/`, or `src/showcase/`. Each page within a silo references only the bundles it needs from the shared package registries in the project root: `lib/` and `data/`. The project root holds the package repositories that supply named libraries and data bundles; the generated site copies selected bundles into `target/lib/<library-name>/` and `target/data/<bundle-name>/` inside the silo output. This design supports collections of PWAs or static sub-sites that are isolated, independently deployable, and able to pull in shared assets without duplicating their source trees inside each silo.

Markdown files at the project root are treated as belonging to a special `main` silo under `src/`. Their `lib` and `data` references resolve against the root-level package repositories `lib/` and `data/`, and their generated output is placed under `target/main/...`. This lets the project homepage or shared content live alongside the silo folders while still keeping a single shared asset registry.

Each page is a single Markdown file. The project root contains package repositories: `lib/` stores top-level library folders such as `bootstrap/`, `fontawesome/`, or any other asset bundle, while `data/` stores top-level data bundles such as `site-data/`, `content/`, or any other structured dataset. A page's front matter identifies which named libraries and data bundles should be copied into the page's output directory and linked or included in the generated HTML. When a page imports a library or data bundle, the entire corresponding folder is copied into the output path for the current silo, preserving the bundle's internal structure. There are no HTML templates or variable substitution features in the initial product. Each generated page is simply the rendered Markdown body placed into an HTML document that includes the configured library links and script tags.

## 2. Problem Statement

Many small sites and personal projects need a simple way to publish content quickly without managing a backend system. Existing solutions are often either too heavy, too opinionated, or too difficult to customize.

Sausage Site should provide a minimal but robust workflow:
- single-page Markdown content files
- predictable build output
- named library bundles for CSS and JavaScript assets
- easy local preview
- static deployment to common hosting providers

## 3. Product Goals

- Generate static HTML from standalone Markdown page files
- Support Markdown-based authoring with front matter
- Allow per-page asset selection from a top-level library registry
- Allow per-page data bundle selection from a top-level data registry
- Keep the build process deterministic and fast
- Provide a local preview workflow for development
- Produce deployable output with no runtime server requirements
- Avoid template engines and variable replacement in the initial product

## 4. Technology Constraints

The implementation must satisfy the following technical constraints:
- The application must be implemented in Java 25.
- The project should be built as a command-line application, not a web server or long-running daemon.
- Markdown content should be parsed and rendered using a reputable Java Markdown library, with Flexmark as the preferred default choice.
- The frontend for the initial version should use Bootstrap as the default UI library and asset bundle.
- The application should generate static HTML, CSS, and other static assets without requiring a runtime server.
- The build and preview workflow should be deterministic, repeatable, and suitable for local development and CI usage.

## 5. Non-Goals

The initial version will not include:
- user authentication or authorization
- database-backed content management
- server-side rendering
- HTML template systems or layout inheritance
- variable replacement or page interpolation at build time
- online visual CMS editing
- plugin marketplace or ecosystem at v1
- dynamic forms or comments systems
- multi-tenant hosting

## 6. Target Users

### 6.1 Developer publisher
A developer who wants to ship a site for their project, portfolio, blog, or documentation with minimal infrastructure.

### 6.2 Content author
A writer or editor who wants to create pages and posts in a simple, low-friction format with minimal tooling.

### 6.3 Site maintainer
A person responsible for configuration, asset library management, navigation, and deployment but not a full-stack app developer.

## 7. Core User Stories

- As a content author, I can write a post in Markdown so it renders as a page.
- As a site owner, I can configure site metadata such as title, description, and navigation.
- As a developer, I can choose which named library bundles are included on a page.
- As a maintainer, I can preview the site locally before deploying.
- As a publisher, I can build a static output directory suitable for deployment.
- As a user, I can browse a site with clean, semantic HTML and minimal JavaScript.

## 8. Functional Requirements

### 8.1 Site Layout
The app uses a fixed root layout with four canonical top-level directories: `src/`, `lib/`, `data/`, and `target/`. `src/` contains one or more silo folders and the Markdown pages they own. `lib/` and `data/` act as shared package repositories that can be referenced by any silo during the build. `target/` is generated output and contains the selected library and data packages copied into the appropriate silo output path.

By default, the generated output should be written to a `target` directory under the input project root. The application should treat `src/` as the canonical source root when present and otherwise allow direct page discovery from the project root.

#### 8.1.1 Siloed Sub-Site Model
The website is intended to be a collection of independent top-level silos. Each silo represents a distinct deployable sub-site, such as a PWA, a docs surface, or a marketing landing page, and sits at its own top-level folder under `src/`. The project root has four canonical top-level directories: `src/`, `lib/`, `data/`, and `target/`. `src/` contains the silo folders and markdown pages, while `lib/` and `data/` act like package repositories from which any silo may pull shared bundles during the build. `target/` is generated output and contains the selected library and data packages copied into the appropriate silo output path.

Markdown files at the project root are treated as part of a special `main` silo under `src/`. They may reference `lib` and `data` bundles from the shared repositories at the root, even though they are not under a nested subfolder. The `main` silo uses the predictable output path `target/main/...`, which keeps the site landing page or shared content consistent with the rest of the silo model without creating per-silo source copies of asset packages.

This model keeps each sub-site self-contained at output time: the build must resolve libraries and data bundles from the shared package repositories only, copy them into the current silo's output folder, and keep the generated HTML scoped to that silo's output path. Source trees for silos stay under `src/`; the `lib` and `data` directories are never created within each silo's source tree. They are repository-like package stores consumed as part of the build process.

This is especially useful for multi-PWA deployments where each app should have its own copy of all assets it requires at runtime, but where the authoring and package management stay centralized in root-level `lib` and `data` repositories. A silo may still contain nested pages and child directories under `src/<silo>/`; the top-level silo folder is the logical boundary for the URL and asset ownership.

### 8.2 Content Authoring
Each page must be authored as a single Markdown file. Each content item may include front matter metadata such as:
- title
- slug
- date
- author
- tags
- category
- draft status
- libraries: a list of named library bundles to include on that page
- data: a list of named data bundles to include on that page
- apps: a list of app names that should be mounted in the generated HTML

Example front matter:
```yaml
---
title: Welcome
slug: welcome
libraries:
  - bootstrap
data:
  - site-data
apps:
  - todo
  - calendar
---
```

The markdown file is the complete source for one page. There are no page templates, no shared layout wrappers, and no variable interpolation in the initial version.

### 8.3 Page Generation and HTML Composition
The app must generate a standalone HTML document for each Markdown page. Generated items should include:
- rendered body content from the Markdown file
- stylesheet link tags for each selected library CSS bundle
- script tags for each selected library JavaScript bundle
- a copy of each selected data bundle under `target/<silo>/data/<bundle-name>/` for browser or client-side access
- app mount points for each named app in the front matter, represented as HTML div elements that JavaScript can target
- optional metadata in the document head when provided by front matter
- a generated filename and URL based on the page's slug or path

Each app in the `apps` front matter must be converted into a placeholder div in the HTML output, so JavaScript can find and hydrate the app by name. A simple convention is to emit `div` elements with a `data-app` attribute such as `<div data-app="todo"></div>`. The app placeholders should be inserted into the page body alongside the rendered Markdown content.

The resulting HTML document must be composed from the Markdown-rendered body plus the configured library asset references and data bundle copies, without requiring HTML templates. Every generated page must include a `<base href="/<silo-name>/">` tag in the document head so that local asset paths can remain silo-scoped and stable regardless of page depth. For example, a page at `/my-app/subfolder/page.html` can reference `lib/bootstrap/css/bootstrap.css` without any `../` path logic because the browser resolves it against `/my-app/`.

### 8.4 Library Registry
The project must support a root-level `lib` directory that contains library folders. Each library is a self-contained folder, such as `lib/bootstrap/` or `lib/fontawesome/`. The folder may contain nested CSS, JavaScript, fonts, images, or other assets in any structure it needs.

Bootstrap should be provided as a default library bundle in the initial product, and page front matter may reference it by name in the same way as any other library. When a page lists a named library in front matter, the build process must copy the entire corresponding library folder into `target/<silo>/lib/<library-name>/` and add the appropriate HTML link/script tags to the generated document.

### 8.5 Data Registry
The project must support a root-level `data` directory that contains data bundles intended for static content assets and structured payloads. Each data bundle is a self-contained folder, such as `data/site-data/` or `data/content/`, and may contain JSON files, CSV files, YAML files, images, icons, or other static assets that are intended to be served alongside the site. The directory should behave the same way as `lib`: a named bundle is resolved from the shared root `data` repository and copied into `target/<silo>/data/<bundle-name>/` when a page requests it in front matter.

When a page lists a named data bundle in front matter, the build process must copy the corresponding folder into `target/<silo>/data/<bundle-name>/` and make it available to the generated page using the same bundle naming convention as the library registry. Data bundles should be optional, page-scoped, and easy to wire into JavaScript or client-side applications without requiring a backend. The primary purpose of this folder is to hold static assets and `.json`-style data files that are consumed by the generated site at runtime.

### 8.6 Collections and Taxonomy
The app must support grouping content into logical collections such as:
- posts
- pages
- tags
- categories

This may be implemented as a basic collection model with automatic listing generation.

### 8.7 Asset Handling
The app must support copying static assets into the generated site, including:
- images
- CSS
- JavaScript
- fonts
- favicon files

Asset copying must respect page-local source, library configuration, and data bundle configuration so each page receives the assets named in its front matter.

### 8.8 Build and Preview
The app must provide at least two core commands:
- build: generate static output
- preview: run a local development server and rebuild on source changes

### 8.9 Deployment Readiness
The generated site must be deployable without requiring a running application server. Output should be static HTML/CSS/JS assets suitable for hosting on static providers.

## 9. Non-Functional Requirements

### 9.1 Performance
- builds should complete quickly for small and medium-sized sites
- local preview should respond promptly to source changes
- generated output should be lightweight and easy to serve

### 9.2 Reliability
- build output should be deterministic for the same input
- invalid content should fail clearly with actionable errors
- site generation should avoid silently dropping content

### 9.3 Simplicity
- configuration should be easy to read and edit
- defaults should be sensible
- the CLI should expose a minimal number of commands

### 9.4 Portability
- the project should run on common developer machines
- generated output should be portable across static hosting providers

## 10. Proposed Architecture

### 10.1 High-Level Components
- CLI entry points
- Configuration loader
- Content parser
- Front matter and metadata handling
- Markdown renderer
- Library asset resolver
- Data bundle resolver
- Asset pipeline
- Site builder
- Preview server

### 10.2 Suggested Runtime Model
The app should follow a simple build-centric architecture:
1. load configuration
2. discover page Markdown files
3. parse metadata and content
4. resolve page-specific library folders from the `lib` directory
5. resolve page-specific data bundles from the `data` directory
6. copy the selected library folders into `target/lib/<library-name>/` and the selected data bundles into `target/data/<bundle-name>/`
7. render Markdown into HTML
8. assemble final page HTML without templates
9. write generated output to the project `target` directory by default

This model keeps the project understandable and reduces operational complexity.

## 11. Data Model

### 11.1 SiteLayout
- srcRoot: string
- libRoot: string
- dataRoot: string
- outputRoot: string
- siloName: string

The project uses a fixed directory layout: `src/` stores silo content, `lib/` and `data/` are package repositories, and `target/` holds generated output. There is no per-directory configuration file in the initial product.

### 11.2 ContentItem
- id: string
- type: page | post
- title: string
- slug: string
- date: ISO date
- author: string
- summary: string
- rawContent: string
- metadata: object
- tags: array
- category: string
- draft: boolean
- libraries: array of strings
- data: array of strings
- apps: array of strings

### 11.3 LibraryBundle
- name: string
- path: string
- type: css | js | asset
- files: array of file paths

### 11.4 DataBundle
- name: string
- path: string
- kind: json | csv | yaml | text | asset
- files: array of file paths

### 11.5 Asset
- sourcePath: string
- targetPath: string
- kind: image | css | js | font | data | other

### 11.6 PageRenderResult
- htmlBody: string
- cssLinks: array of href values
- jsSources: array of src values
- dataPaths: array of bundle paths
- appMounts: array of app names or placeholder selectors
- outputPath: string

## 12. User Experience Flow

### 12.1 Initial Setup
1. Create a project directory
2. Create `src/`, `lib/`, and `data/` directories
3. Add content files under top-level silo folders in `src/`
4. Add library folders under the top-level `lib` directory
5. Add data bundles under the top-level `data` directory
6. Run build
7. Verify output is generated in `<project-root>/target`

### 12.2 Local Preview
1. Run preview command
2. Start local server
3. Watch relevant Markdown, library, and data files for changes
4. Rebuild site automatically
5. Refresh browser to inspect output

### 12.3 Deployment
1. Run production build
2. Upload generated static output to a static hosting provider
3. Configure domain or custom path if needed

## 13. Acceptance Criteria

The MVP will be considered successful if:
- a user can create a root `src/`, `lib/`, and `data/` layout
- a single Markdown file renders into a standalone HTML page
- a page can declare one or more named libraries from the `lib` directory
- a page can declare one or more named data bundles from the `data` directory
- the build command copies selected library folders into `target/<silo>/lib/<library-name>/` and selected data bundles into `target/<silo>/data/<bundle-name>/`
- the build command emits the correct HTML link/script tags for libraries and serves data bundles alongside the output
- the build output is written to `<project-root>/target` by default
- a preview command serves the site locally
- the output is deployable to a static hosting provider
- invalid content yields a clear error

## 14. Minimum Viable Product (MVP)

The MVP should prioritize the smallest useful, reliable version of the product:
- single-file Markdown page support
- simple front matter metadata
- library-based asset selection from the top-level `lib` directory
- static HTML generation without templates
- local preview server
- asset copying

This is enough to validate the product’s value before expanding features.

## 15. Roadmap

### Phase 1: MVP
- project scaffold
- root `src/lib/data/target` layout validation
- Markdown rendering
- library asset pipeline
- build command
- preview command

### Phase 2: Publishing Experience
- tags and categories
- better pagination and collection pages
- default public library bundles
- improved asset handling

### Phase 3: Scale and Flexibility
- richer metadata handling
- external data or content sources
- deployment integrations
- optional advanced asset or theme conventions

## 16. Risks and Constraints

- scope creep from trying to support too many features early
- over-engineered asset or styling conventions too early in the project
- unclear content model if metadata requirements are not standardized
- inconsistent behavior between build and preview workflows

Mitigation:
- keep v1 focused on a small, clean feature set
- document config and content conventions clearly
- make preview and build share the same core rendering pipeline

## 17. Success Metrics

For the initial product, success will be measured by:
- ability to produce a valid static site in a single build command
- ease of creating and editing content files
- total setup time under a short onboarding flow
- minimal configuration required for basic use

## 18. Summary

Sausage Site should be a simple, dependable static site generator focused on clarity, fast builds, and easy deployment. The initial version should solve the core publishing workflow without introducing runtime complexity or backend management.

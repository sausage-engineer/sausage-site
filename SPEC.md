# Sausage Site Specification

## 1. Overview

Sausage Site is a Java 25 static site generator for building content-driven websites from standalone Markdown pages. It is designed for developers and content authors who want fast, dependable publishing without a server runtime, database, or CMS backend.

The product generates a complete static site that can be deployed to any static host, such as GitHub Pages, Netlify, Cloudflare Pages, or any web server that serves HTML files. The application should be implemented as a Java 25 command-line tool with a simple, repeatable build and preview workflow. A popular Java Markdown library such as Flexmark is the preferred implementation choice for parsing and rendering Markdown content.

Each page is a single Markdown file. A top-level `library` directory contains named subdirectories such as `css`, `js`, and other asset groups. A page's front matter identifies which named libraries should be copied into the page's output directory and linked or included in the generated HTML. There are no HTML templates or variable substitution features in the initial product. Each generated page is simply the rendered Markdown body placed into an HTML document that includes the configured library links and script tags.

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

### 8.1 Site Configuration
The app must support a site configuration file containing:
- site name
- base URL
- description
- default language
- navigation links
- output directory
- source directories
- optional default asset libraries

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

Example front matter:
```yaml
---
title: Welcome
slug: welcome
libraries:
  - bootstrap
---
```

The markdown file is the complete source for one page. There are no page templates, no shared layout wrappers, and no variable interpolation in the initial version.

### 8.3 Page Generation and HTML Composition
The app must generate a standalone HTML document for each Markdown page. Generated items should include:
- rendered body content from the Markdown file
- stylesheet link tags for each selected library CSS bundle
- script tags for each selected library JavaScript bundle
- optional metadata in the document head when provided by front matter
- a generated filename and URL based on the page's slug or path

The resulting HTML document must be composed from the Markdown-rendered body plus the configured library asset references, without requiring HTML templates.

### 8.4 Library Registry
The project must support a top-level `library` directory that contains named asset bundles. Each bundle may be organized by type, including:
- `library/css/<name>/...`
- `library/js/<name>/...`
- additional asset folders as needed

Bootstrap should be provided as a default library bundle in the initial product, and page front matter may reference it by name in the same way as any other library. When a page lists a named library in front matter, the build process must copy the corresponding files into the output directory for that page and add the appropriate HTML link/script tags to the generated document.

### 8.5 Collections and Taxonomy
The app must support grouping content into logical collections such as:
- posts
- pages
- tags
- categories

This may be implemented as a basic collection model with automatic listing generation.

### 8.6 Asset Handling
The app must support copying static assets into the generated site, including:
- images
- CSS
- JavaScript
- fonts
- favicon files

Asset copying must respect page-local source and library configuration so each page receives the assets named in its front matter.

### 8.7 Build and Preview
The app must provide at least two core commands:
- build: generate static output
- preview: run a local development server and rebuild on source changes

### 8.8 Deployment Readiness
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
- Asset pipeline
- Site builder
- Preview server

### 10.2 Suggested Runtime Model
The app should follow a simple build-centric architecture:
1. load configuration
2. discover page Markdown files
3. parse metadata and content
4. resolve page-specific library asset bundles from the `library` directory
5. copy selected assets to the output directory and compute HTML tag references
6. render Markdown into HTML
7. assemble final page HTML without templates
8. write generated output to a build directory

This model keeps the project understandable and reduces operational complexity.

## 11. Data Model

### 11.1 SiteConfig
- name: string
- baseUrl: string
- description: string
- outputDir: string
- sourceDir: string
- nav: array of links
- defaultLibraries: array of strings

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

### 11.3 LibraryBundle
- name: string
- path: string
- type: css | js | asset
- files: array of file paths

### 11.4 Asset
- sourcePath: string
- targetPath: string
- kind: image | css | js | font | other

### 11.5 PageRenderResult
- htmlBody: string
- cssLinks: array of href values
- jsSources: array of src values
- outputPath: string

## 12. User Experience Flow

### 12.1 Initial Setup
1. Create a project directory
2. Add configuration file
3. Add content files
4. Add library bundles under the top-level `library` directory
5. Run build

### 12.2 Local Preview
1. Run preview command
2. Start local server
3. Watch relevant Markdown, library, and configuration files for changes
4. Rebuild site automatically
5. Refresh browser to inspect output

### 12.3 Deployment
1. Run production build
2. Upload generated static output to a static hosting provider
3. Configure domain or custom path if needed

## 13. Acceptance Criteria

The MVP will be considered successful if:
- a user can create a basic site configuration
- a single Markdown file renders into a standalone HTML page
- a page can declare one or more named libraries from the `library` directory
- the build command copies selected library assets into the output directory and emits the correct HTML link/script tags
- a preview command serves the site locally
- the output is deployable to a static hosting provider
- invalid configuration or content yields a clear error

## 14. Minimum Viable Product (MVP)

The MVP should prioritize the smallest useful, reliable version of the product:
- single-file Markdown page support
- simple front matter metadata
- library-based asset selection from the top-level `library` directory
- static HTML generation without templates
- local preview server
- asset copying

This is enough to validate the product’s value before expanding features.

## 15. Roadmap

### Phase 1: MVP
- project scaffold
- config loading
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

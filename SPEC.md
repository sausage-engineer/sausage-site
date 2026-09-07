# Sausage Site Specification

## 1. Overview

Sausage Site is a Java 25 static site generator for building content-driven websites from simple source files and templates. It is designed for developers and content authors who want fast, dependable publishing without a server runtime, database, or CMS backend.

The product generates a complete static site that can be deployed to any static host, such as GitHub Pages, Netlify, Cloudflare Pages, or any web server that serves HTML files. The application should be implemented as a Java 25 command-line tool with a simple, repeatable build and preview workflow. A popular Java Markdown library such as Flexmark is the preferred implementation choice for parsing and rendering Markdown content.

## 2. Problem Statement

Many small sites and personal projects need a simple way to publish content quickly without managing a backend system. Existing solutions are often either too heavy, too opinionated, or too difficult to customize.

Sausage Site should provide a minimal but robust workflow:
- content authored in Markdown files
- predictable build output
- reusable templates
- easy local preview
- static deployment to common hosting providers

## 3. Product Goals

- Generate static HTML from source content
- Support Markdown-based authoring with front matter
- Allow site-wide configuration and custom layouts
- Keep the build process deterministic and fast
- Provide a local preview workflow for development
- Produce deployable output with no runtime server requirements

## 4. Technology Constraints

The implementation must satisfy the following technical constraints:
- The application must be implemented in Java 25.
- The project should be built as a command-line application, not a web server or long-running daemon.
- Markdown content should be parsed and rendered using a reputable Java Markdown library, with Flexmark as the preferred default choice.
- The application should generate static HTML, CSS, and other static assets without requiring a runtime server.
- The build and preview workflow should be deterministic, repeatable, and suitable for local development and CI usage.

## 5. Non-Goals

The initial version will not include:
- user authentication or authorization
- database-backed content management
- server-side rendering
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
A person responsible for configuration, templates, navigation, and deployment but not a full-stack app developer.

## 7. Core User Stories

- As a content author, I can write a post in Markdown so it renders as a page.
- As a site owner, I can configure site metadata such as title, description, and navigation.
- As a developer, I can reuse templates across pages and posts.
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
- optional theme or template settings

### 8.2 Content Authoring
The app must support content authored in Markdown. Each content item may include front matter metadata such as:
- title
- slug
- date
- author
- tags
- category
- draft status

### 8.3 Page and Post Generation
The app must generate pages and posts from source content into HTML output. Generated items should include:
- rendered body content
- metadata in page headers
- canonical URLs or slugs
- listing pages for collections

### 8.4 Collections and Taxonomy
The app must support grouping content into logical collections such as:
- posts
- pages
- tags
- categories

This may be implemented as a basic collection model with automatic listing generation.

### 8.5 Layouts and Partials
The app must support reusable layouts and partials so pages can share headers, footers, sidebars, and wrappers.

### 8.6 Asset Handling
The app must support copying static assets into the generated site, including:
- images
- CSS
- JavaScript
- fonts
- favicon files

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
- Renderer
- Template engine
- Asset pipeline
- Site builder
- Preview server

### 10.2 Suggested Runtime Model
The app should follow a simple build-centric architecture:
1. load configuration
2. read source content files
3. parse metadata and content
4. transform content to renderable data structures
5. render pages and collections through templates
6. copy static assets
7. write generated output to a build directory

This model keeps the project understandable and reduces operational complexity.

## 11. Data Model

### 11.1 SiteConfig
- name: string
- baseUrl: string
- description: string
- outputDir: string
- sourceDir: string
- nav: array of links
- theme: optional string

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

### 11.3 Layout
- name: string
- templatePath: string
- variables: object

### 11.4 Asset
- sourcePath: string
- targetPath: string
- kind: image | css | js | font | other

## 12. User Experience Flow

### 12.1 Initial Setup
1. Create a project directory
2. Add configuration file
3. Add content files
4. Add layout templates
5. Run build

### 12.2 Local Preview
1. Run preview command
2. Start local server
3. Watch relevant content and templates for changes
4. Rebuild site automatically
5. Refresh browser to inspect output

### 12.3 Deployment
1. Run production build
2. Upload generated static output to a static hosting provider
3. Configure domain or custom path if needed

## 13. Acceptance Criteria

The MVP will be considered successful if:
- a user can create a basic site configuration
- a Markdown file renders into HTML output
- the build command produces static files in an output directory
- a preview command serves the site locally
- pages can reuse templates and shared layout structure
- the output is deployable to a static hosting provider
- invalid configuration or content yields a clear error

## 14. Minimum Viable Product (MVP)

The MVP should prioritize the smallest useful, reliable version of the product:
- Markdown-based content support
- simple front matter metadata
- single site configuration
- basic templating
- static build output
- local preview server
- asset copying

This is enough to validate the product’s value before expanding features.

## 15. Roadmap

### Phase 1: MVP
- project scaffold
- config loading
- Markdown rendering
- template system
- build command
- preview command

### Phase 2: Publishing Experience
- tags and categories
- better pagination and collection pages
- custom themes
- improved asset handling

### Phase 3: Scale and Flexibility
- optional advanced templating features
- richer metadata handling
- external data or content sources
- deployment integrations

## 16. Risks and Constraints

- scope creep from trying to support too many features early
- over-engineered templating too early in the project
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

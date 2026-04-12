# Go Language Support — Implementation Plan

## Current State

The Go module supports **parse + print + GetObject + InstallRecipes + Reset** over RPC. All 15 integration tests pass (helloWorld, structs, slices, interfaces, for/range loops, switch, channels, maps, etc.). The recipe bundle resolver infrastructure (`GolangRecipeBundleResolver`/`GolangRecipeBundleReader`) is wired up on the Java side with stub handling on the Go side.

## What's Left

### 1. Go Server: GetMarketplace Handler
The Go RPC server needs to handle `GetMarketplace` requests so Java can discover what recipes the Go process has available. Python/JS servers return a `GetMarketplaceResponse` containing recipe descriptors organized by category.

### 2. Go Server: PrepareRecipe Handler
Handle `PrepareRecipe` requests to instantiate a recipe with options. This is how the Java host asks the Go process to create a configured recipe instance ready for execution.

### 3. Go Server: Visit Handler
Handle `Visit` requests — the core recipe execution path. Java sends a tree ID and a prepared recipe ID; the Go server applies the visitor and returns the modified tree. This requires bidirectional RPC (Go calls back to Java's `GetObject` to fetch the tree, applies the visitor, then Java calls `GetObject` to get the result).

### 4. Go Server: Generate Handler
Handle `Generate` requests for recipes that create new source files rather than modifying existing ones.

### 5. Go Server: ParseProject Handler
Handle `ParseProject` for bulk project parsing. Java sends a project directory path; Go discovers and parses all `.go` files, resolving the module structure (`go.mod`). Python/JS have 3 overloads supporting exclusion patterns and relative path configuration.

### 6. Go Server: TraceGetObject Handler
Handle `TraceGetObject` to toggle verbose RPC message tracing for debugging.

### 7. Go Server: InstallRecipes — Actual Implementation
The current `InstallRecipes` handler is a stub. It needs to:
- For local paths: discover and load Go recipe plugins from a local module
- For package specs: fetch a Go module from a Git repository, build it, and load its recipes
- Return accurate `recipesInstalled` count and resolved `version`

### 8. Go Recipe Framework
Build the Go-side recipe/visitor infrastructure so Go recipes can be authored:
- Recipe interface/struct (name, description, options, visitor factory)
- Visitor base types for Go AST traversal
- Recipe registration/discovery mechanism
- Marketplace integration (expose registered recipes via GetMarketplace)

### 9. Java Client: parseProject()
Add `parseProject()` methods to `GoRewriteRpc` (following Python's 3 overloads pattern) that send `ParseProject` RPC requests and return parsed source files with appropriate markers.

### 10. Java Client: Builder Enhancements
Add builder options to `GoRewriteRpc.Builder`:
- `environment(Map<String, String>)` — environment variables for the Go subprocess
- `workingDirectory(Path)` — working directory for the Go subprocess
- `metricsCsv(Path)` — metrics output
- `recipeInstallDir(Path)` — where installed recipe modules live

### 11. Java Client: resetCurrent()
Add static `resetCurrent()` convenience method (mirrors Python/JS pattern).

## Priority Order

The most impactful order for enabling end-to-end recipe execution:

1. **Go Recipe Framework** (#8) — foundation for everything else
2. **GetMarketplace** (#1) + **PrepareRecipe** (#2) — recipe discovery
3. **Visit** (#3) — recipe execution
4. **InstallRecipes actual impl** (#7) — load recipes from Git
5. **parseProject** (#5, #9) — project-level tooling
6. **Generate** (#4) — new file generation
7. **Builder/client polish** (#10, #11, #6) — ergonomics

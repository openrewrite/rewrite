# Authoring Go recipes

A short reference for the patterns recipe authors need that aren't
obvious from the rest of the codebase. Snippets here are pulled from
real tests so they stay accurate as the API evolves.

## A recipe in 30 lines

A Go-native recipe embeds `recipe.Base`, names itself, and returns a
`TreeVisitor` from `Editor()`:

```go
package golang

import (
    "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
    "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
    "github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type RenameXToFlag struct{ recipe.Base }

func (r *RenameXToFlag) Name() string        { return "org.openrewrite.golang.test.RenameXToFlag" }
func (r *RenameXToFlag) DisplayName() string { return "Rename x to flag" }
func (r *RenameXToFlag) Description() string { return "Test recipe." }

func (r *RenameXToFlag) Editor() recipe.TreeVisitor {
    return visitor.Init(&renameXVisitor{})
}

type renameXVisitor struct{ visitor.GoVisitor }

func (v *renameXVisitor) VisitIdentifier(ident *tree.Identifier, _ any) tree.J {
    if ident.Name == "x" {
        c := *ident
        c.Name = "flag"
        return &c
    }
    return ident
}
```

Two patterns that come up everywhere:

- `visitor.Init(...)` — sets the `Self` field on the embedded
  `GoVisitor` so virtual dispatch works. Always use it.
- Return a fresh value, never mutate in place. Recipes get the same
  cu reference as their parent visitor; in-place mutation breaks
  no-change detection (and makes recipe diffs confusing to debug).

## Test wrappers — `goProject(...)`

Multi-file Go recipes test against a project layout that mirrors what
shows up in real codebases: a `go.mod` plus one or more `.go` files,
all SIBLINGS inside a project directory (not nested). The Go and Java
test harnesses both expose this:

**Go side** (`pkg/test/spec.go`):

```go
spec := test.NewRecipeSpec().WithRecipe(&MyRecipe{})
spec.RewriteRun(t,
    test.GoProject("foo",
        test.GoMod(`
            module example.com/foo

            go 1.22

            require github.com/google/uuid v1.6.0
        `),
        test.GoSum(`
            github.com/google/uuid v1.6.0 h1:...
            github.com/google/uuid v1.6.0/go.mod h1:...
        `),
        test.Golang(`package main

func main() {}
`).WithPath("main.go"),
    ),
)
```

**Java side** (`Assertions.goProject`):

```java
rewriteRun(
    spec -> spec.recipe(new MyRecipe()),
    goProject("foo",
        goMod("module example.com/foo\n\ngo 1.22\n"),
        go("package main\n\nfunc main() {}\n")
    )
);
```

What `goProject(...)` does:

1. Tags every child source with a `tree.GoProject` marker — recipes
   that need to know "is this file part of project X?" read it.
2. Parses any `goMod(...)` sibling and attaches a
   `tree.GoResolutionResult` marker holding requires / replaces /
   excludes / retracts. A sibling `GoSum(...)` populates
   `ResolvedDependencies` on the same marker.
3. On the Java side, files are written to a temp dir before parsing
   so the on-disk vendor walker can resolve relative paths. On the
   Go side, the test harness threads the same data through
   `parser.ProjectImporter` in memory.

## Reading module info from a recipe

`tree.GoResolutionResult` lives on the sibling `go.mod` source in a
multi-file project. Recipes that need module-level information walk
the input set looking for the marker:

```go
import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

func modulePath(cu *tree.CompilationUnit) string {
    for _, m := range cu.Markers.Entries {
        if mrr, ok := m.(tree.GoResolutionResult); ok {
            return mrr.ModulePath
        }
    }
    return ""
}
```

For project-aware parses (`handleParseProject` and Java's
`parseWithProject`), the marker is attached to **every**
`Go.CompilationUnit` so the lookup is local — no scanning siblings
needed. For test-harness parses, only the `goMod(...)` source carries
the marker (mirrors how Maven recipes read `MavenResolutionResult` off
the sibling `pom.xml`).

The shared imports primitives in `pkg/recipe/golang/internal/imports.go`
expose this as `internal.FindModulePath(cu)` for recipes inside the
package (Add/Remove/OrderImports use it for grouping).

## Cursor message map

Visitors traverse depth-first; sometimes a child needs to leave a
breadcrumb for an ancestor (or vice versa). The cursor exposes a
small message map for this:

```go
v.Cursor().PutMessage("foundError", true)

// Later, in some ancestor visit:
if v.Cursor().GetNearestMessage("foundError") == true {
    // ...
}
```

Available methods on `*visitor.Cursor`:

| Method | What it does |
|---|---|
| `PutMessage(key, value)` | Store on this cursor's frame. |
| `GetMessage(key)` | Read from this frame only. |
| `GetNearestMessage(key)` | Walk parents looking for `key`; return first hit. |
| `GetNearestMessageOrDefault(key, default)` | Same, with a fallback. |
| `PollNearestMessage(key)` | Walk parents like `GetNearestMessage`, then **delete** the message from the frame it was found on. |
| `PutMessageOnFirstEnclosing(key, value, predicate)` | Walk up to the first ancestor matching predicate, store there. |

All match the Java `Cursor.putMessage` / `getNearestMessage` semantics.

## Module context for type attribution

Type attribution depth depends on how the parser is constructed:

| Construction | Stdlib | Intra-project | Third-party requires | Vendored sources |
|---|---|---|---|---|
| `parser.NewGoParser()` (default) | ✓ | (single file only) | ✗ | ✗ |
| `goparser.NewProjectImporter(modulePath, fallback)` + `AddSource(...)` | ✓ | ✓ (cross-file) | stub (typed) | ✗ |
| `+ SetProjectRoot(rootDir)` | ✓ | ✓ | stub | ✓ if `<rootDir>/vendor/<path>/` exists |
| `+ AddModule(path, cachePath, version)` | ✓ | ✓ | ✓ from `$GOMODCACHE` | ✓ (wins) |
| `+ AddReplace(old, new, version)` | ✓ | ✓ | redirected to `new` (vendor or local) | ✓ |

A module reachable through none of these still resolves to a typed
stub — a package with the right path and an empty scope — so an import
of it names something even though its symbols do not.

For Java-side parsing through RPC, build the parser with
`GolangParser.builder().module("...").goMod(content).build()`. The
server reconstructs a `ProjectImporter` from those plus sibling .go
files, and uses `RelativeTo` (when set) as the project root for the
vendor walker.

The `parseProject` RPC handles this automatically — every `.go` file
under the project root resolves against its closest-ancestor `go.mod`.
Multi-module repos (root + nested submodules) are honored.

## Shipped export data

A template is type-checked as a file belonging to no module, against
whatever `importer.Default()` can load. That covers the stdlib and
nothing else, so `Imports("github.com/pkg/errors")` yields a call with
no signature — `matcher.IsResolved` is false, and any recipe that later
reads the template's output sees an unattributed tree.

A recipe module closes that gap by carrying the compiler export data
for the packages its templates import. Generate it where those packages
resolve, and commit what lands:

```
go run github.com/openrewrite/rewrite/rewrite-go/cmd/goexportdata \
    -o internal/jsonv2exportdata encoding/json/v2 encoding/json/jsontext
```

The output directory can sit anywhere in the module; its name becomes
the generated package's name, or pass `-pkg` when the two should
differ. Name it for what it carries rather than `exportdata`, which
collides with this package when a test calls `Verify`. A module can generate as many of these as it likes — one per
dependency it templates against, say — and a template draws on however
many it needs, since sets accumulate the way `Imports` does.

That writes one blob per import path plus an `embed.FS` shim, which a
recipe consumes through the `ExportData` option:

```go
r := template.NewRecipe(
    template.RecipeName("com.example.MarshalIndentToV2"),
    template.WithCaptures(v, pre, ind),
    template.WithBefore(fmt.Sprintf(`json.MarshalIndent(%s, %s, %s)`, v, pre, ind),
        template.Imports("encoding/json")),
    template.WithAfter(fmt.Sprintf(`json.Marshal(%s, jsontext.WithIndent(%s), jsontext.WithIndentPrefix(%s))`, v, ind, pre),
        template.Imports("encoding/json/v2", "encoding/json/jsontext"),
        template.ExportData(jsonv2exportdata.FS),
        template.SourceImports("encoding/json/v2", "encoding/json/jsontext")),
)
```

`ExportData` on `WithAfter` is what makes the emitted code carry real
types, and that is what lets `RemoveUnusedImports` tell the superseded
`encoding/json` from the `encoding/json/v2` replacing it — the two bind
the same `json` qualifier, so without attribution the old import reads
as still in use. On `WithBefore` it bears only on shape: a generic
function instantiation parses as a call carrying type arguments once
attributed and as an index expression otherwise, so set it there only
when the source is parsed the same way. The builder form takes the same
option:

```go
tmpl := template.ExpressionTemplate(`jsontext.WithIndent("  ")`).
    Imports("encoding/json/jsontext").
    ExportData(jsonv2exportdata.FS).
    Build()
```

Blobs are a few percent of the archive `go build` produces — 99 KB for
`encoding/json/jsontext`, 10 KB for `github.com/pkg/errors` — because
the generator keeps only the export data and drops the object code. A
package's own dependencies need no blobs of their own: the types they
contribute to its API travel inside the one blob that names them.

Nothing about a blob is pinned to the machine that made it. The
GOOS/GOARCH and toolchain version recorded in it are not checked on
read; only the binary export format is, and it changes rarely. That
format reads backward only — a toolchain decodes its own version or
older — so generate blobs with a toolchain no newer than the oldest one
expected to read them. A blob a toolchain cannot read leaves the
template where shipping none would: it still applies, carrying no
types.

That is harmless on its own and is not harmless with `SourceImports`.
Dropping a superseded import needs attribution naming the path that
replaced it, so an unattributed after-template emits both and the file
stops compiling — the same output an un-rewritten reference produces,
below. Since losing attribution is silent, a module that ships blobs
should assert on its own:

```go
func TestExportDataIsReadable(t *testing.T) {
    require.NoError(t, exportdata.Verify(jsonv2exportdata.FS, jsonv2exportdata.Paths...))
}
```

`exportdata` there is this package; `jsonv2exportdata` is the generated
one.

Regenerate when that test fails, or when the shipped package's API
moves and the templates should follow.

One limit is worth knowing before reaching for `SourceImports` here. A
template rewrites one expression, but swapping a package under a
preserved qualifier is a whole-file decision. Where any un-rewritten
reference to the old package survives, both imports are emitted and
bind the same name, which does not compile:

```go
import (
	"encoding/json"     // an untouched json.Marshal still references it
	"encoding/json/v2"  // the rewritten call references this
)
```

Each half is individually right, and no alias helps, since the emitted
text says `json.`. Only a recipe that has established whole-file
coverage may swap a package this way. Omitting `SourceImports` leaves
the template doing node replacement alone — fully attributed, imports
untouched — which composes with a hand-written file-level gate.

## Composing import edits — `ImportService` and `DoAfterVisit`

Most refactor recipes don't ONLY rewrite imports — they edit a method
body, then need to add an import as a follow-up. Composing this with
the `AddImport` recipe directly is awkward (you'd have to nest visitors
or mutate the tree twice). The canonical pattern uses two pieces:

1. `recipe.Service[T](sourceFile)` — fetch a registered service by type.
2. `GoVisitor.DoAfterVisit(visitor)` — queue a follow-up visitor; the
   recipe runner drains the queue once the main visit returns.

Together they let a recipe queue an import as a side-effect:

```go
import (
    "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
    "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
    "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
    "github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type ReplaceTimeSinceCall struct{ recipe.Base }

func (r *ReplaceTimeSinceCall) Editor() recipe.TreeVisitor {
    return visitor.Init(&replaceTimeSinceVisitor{})
}

type replaceTimeSinceVisitor struct{ visitor.GoVisitor }

func (v *replaceTimeSinceVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
    mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*tree.MethodInvocation)
    if !looksLikeTimeSince(mi) {
        return mi
    }
    // ... rewrite mi to xerrors-style call ...

    // Side-effect: queue an `import "xerrors"` to land after the main
    // visit completes. The harness drains the queue automatically.
    golang.MaybeAddImport(v, "xerrors", nil, false /* unconditional */)
    return mi
}
```

`MaybeAddImport(v, path, alias, onlyIfReferenced)` is the convenience
form for add-import side effects. It queues the same `AddImport` visitor
and de-duplicates equivalent pending requests before the after-visit
drain runs.

`ImportService` exposes four visitors:

| Method | Returns a visitor that... |
|---|---|
| `AddImportVisitor(path, alias, onlyIfReferenced)` | Adds `import [alias] "path"`. No-op when already present. |
| `RemoveImportVisitor(path)` | Deletes any import with the matching path. |
| `RemoveUnusedImportsVisitor()` | Drops imports the file doesn't reference. |
| `OrderImportsVisitor()` | Sorts into stdlib / third-party / local groups. |

Each visitor is queueable via `DoAfterVisit` OR applicable directly via
`v.Visit(cu, ctx)`. After-visits can themselves queue more after-visits
(the runner drains transitively).

**Service registration** happens at package `init()` time. As long as
your recipe imports `pkg/recipe/golang` (which most do), services are
registered before any test or RPC dispatch runs. Looking up a missing
service panics with a clear message.

## The names types carry

Every type is attributed under the name Go spells it with, so a pattern
and a signature share one vocabulary:

| Go | attributed as |
|---|---|
| `int`, `int32`, `uint8`, `float64`, `complex128`, `string`, `bool` | `JavaType.Class` with that name |
| `byte`, `rune` | their own names, kept apart from `uint8` and `int32` |
| an untyped constant | `untyped int`, `untyped string`, ... |
| `any`, `interface{}` | `any` |
| `map[K]V`, `chan T` | `map` / `chan` parameterized over K, V |
| a builtin call (`append`, `len`, ...) | a method of `builtin` |
| `unsafe.Pointer` | `unsafe.Pointer` |
| an anonymous `struct{...}` / `interface{...}` | members, but no name |

`byte` and `uint8` are one Go type spelled two ways, as are `rune` and
`int32`; compare two types for identity with `matcher.IsSameGoType`
rather than by name. `matcher.IsInt`, `IsNumeric`, `IsString` and
`IsBool` classify without caring which spelling arrived.

The one exception is a literal, which carries a `JavaType.Primitive`
keyword instead: `J.Literal#type` is declared `JavaType.Primitive` on
the Java side and `withType` drops anything else, and the LST
deserializer reads the keyword to reconstruct the value. The `matcher`
predicates above read both vocabularies, so a literal answers them the
same way a variable of its type does.

## Asserting types in tests

Two helpers exist on both sides for common type assertions:

**Go** (`pkg/test/expect_type.go`):

```go
import . "github.com/openrewrite/rewrite/rewrite-go/pkg/test"

ExpectType(t, cu, "p", "main.Point")          // any type, by its Go name
ExpectType(t, cu, "x", "int32")               // basic types included
ExpectMethodType(t, cu, "Println", "fmt")      // method's declaring FQN
```

**Java** (`org.openrewrite.golang.Assertions`):

```java
expectType(cu, "p", "main.Point");
expectType(cu, "x", "int32");
expectMethodType(cu, "Println", "fmt");
```

Both walk the tree, find the first identifier (or method) matching the
name, and assert on its attributed type. Throw `AssertionError` with a
descriptive message on mismatch — drop them straight into an
`afterRecipe(cu -> ...)` lambda.

## Variadic captures

A capture marked `Variadic(min, max)` stands for a run of list elements
rather than one subtree, so one pattern covers a call of any arity
(`-1` for `max` means unlimited):

```go
args := template.Expr("args").Variadic(0, -1)
before := template.Expression(fmt.Sprintf("assert.Equal(%s)", args)).Captures(args).Build()
after := template.ExpressionTemplate(fmt.Sprintf("require.Equal(%s)", args)).Captures(args).Build()
```

`MatchResult.GetList(name)` reads the run back, and `BindList` supplies
one by hand for `Instantiate`:

```go
tmpl.Instantiate(template.NewMatchResult().
    Bind(recv, tIdent).
    BindList(args, template.Elems(coreArgs, msgArgs)))
```

`Elems` is there because Go converts neither `[]java.Expression` nor
`[]java.Statement` to the `[]java.J` `BindList` takes, and a recipe
holds one of those. It concatenates any number of element slices, so
a leading single node goes in as `Elems([]java.Expression{recv}, ...)`.

A run of any length is a binding, zero included, so an empty one
leaves the call reading as `require.Equal(t)`. A run outside the
capture's bounds is not, and `Instantiate` returns nil — the same
bounds the match path enforces.

Two rules bound what this covers. **A list splits at one variadic
capture**: with one, every other pattern element takes one candidate
element, so the run's length is arithmetic and the captures after it
match positionally. A list holding two variadic captures leaves the
split undetermined, and the match fails.
**A run needs a list to expand into**: a call's arguments or a block's
statements. A variadic placeholder anywhere else has nowhere to go, and
`Apply` returns nil rather than emit the placeholder — as it does for
any capture left unbound.

## Surface boundaries

When in doubt about what pattern to use:

- **Refactoring an expression / statement / declaration** — write a
  `recipe.Recipe` with a `GoVisitor` Editor.
- **Pattern → template rewrite** — use `template.Rewrite(before, after)`
  from `pkg/template`. See `pkg/template/PARITY-AUDIT.md` for the full
  surface against `JavaTemplate`.
- **Adding / removing / reordering imports** — use the existing
  recipes in `pkg/recipe/golang/`: `AddImport`, `RemoveImport`,
  `RemoveUnusedImports`, `OrderImports`. They share primitives
  in `pkg/recipe/golang/internal/imports.go`.
- **Asserting on the parsed LST in a test** — `goProject(...)` for the
  setup, `expectType` / `expectMethodType` for the assertion, and
  `afterRecipe(cu -> ...)` for the callback.

## Where to look next

- `PLAN.md` — what's shipped vs. what's open work.
- `pkg/template/PARITY-AUDIT.md` — GoTemplate vs. JavaTemplate surface.
- `test/testdata/printer-corpus/README.md` — printer fidelity corpus
  (run with `make parity`).
- `test/import_recipes_test.go` — worked examples for the Add/Remove/
  OrderImports recipes.
- `test/cross_package_generics_test.go` — what to expect from
  cross-package type attribution.
- `pkg/exportdata` — the importer, the packer, and `Verify`.

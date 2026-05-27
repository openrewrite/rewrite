# GoTemplate ↔ JavaTemplate parity audit

Item (10) of the rewrite-go parity plan asked for ergonomic parity between
`pkg/template/GoTemplate` and `org.openrewrite.java.JavaTemplate`. This
document lists every public method on `JavaTemplate` (Java) and maps it to
the equivalent surface on `GoTemplate` (Go), noting what was already
present, what was added in this PR, and what is intentionally deferred.

## Audit summary

| Surface | JavaTemplate | GoTemplate | Status |
|---|---|---|---|
| Builder | `JavaTemplate.builder(code)` | `template.ExpressionTemplate(code)` / `StatementTemplate(code)` / `TopLevelTemplate(code)` | ✓ shipped (kind-explicit factories preferred over a single overloaded builder) |
| Build the template | `.build()` | `.Build()` | ✓ shipped |
| Required imports | `.imports(String...)` | `.Imports(...string)` | ✓ shipped |
| Static imports | `.staticImports(String...)` | n/a | not applicable — Go has no static-import concept |
| Coordinate-based substitution | `.apply(JavaCoordinates, params...)` | `.Apply(cursor, *MatchResult)` | ✓ shipped via match captures (see deferred note below) |
| Pattern → template rewrite | `JavaIsoVisitor` + `JavaTemplate.apply` per visit | `template.Rewrite(before, after)` returns a `RewriteVisitor` | ✓ shipped (single-call ergonomic that matches and replaces in one step — Go-side delta over Java) |
| Context-sensitive parsing | `.contextSensitive()` | not yet | deferred (recipes in the wild rarely flip this on for refactoring; revisit if a real recipe asks) |
| Named placeholders | `#{name}` substitution by name + type constraint | positional `#{X}` capture-by-name through `*Capture` | ✓ named via `*Capture` already; type constraints are deferred (see below) |
| Type-checked named placeholders | `#{name:any(java.util.List)}` | not yet | deferred — out-of-scope per the eng review's v1 scope cut |
| Cursor-aware insertion | parameter to `.apply(cursor, ...)` | parameter to `.Apply(cursor, ...)` | ✓ shipped — cursor is threaded but unused in the v1 substitution engine; placeholder for future block/scope-aware substitution |

## Already present before this PR (no delta required)

The Go-side template engine is ~740 LOC and pre-dates the parity work.
Surface that was already at parity:

- `TemplateBuilder` with a fluent API (`Captures`, `Imports`, `Build`).
- Three template kinds (`ExpressionTemplate`, `StatementTemplate`,
  `TopLevelTemplate`) — this is more explicit than Java's overloaded
  `JavaTemplate.builder` (which infers the kind from the substitution
  coordinate). Recipe authors don't need to know coordinate semantics.
- `Apply(cursor, *MatchResult)` returns the substituted subtree with
  capture values spliced in.
- `Rewrite(before, after)` packages match-and-replace into a single
  `RewriteVisitor` — convenient for 1:1 rewrites.
- `getLeadingPrefix` / `setLeadingPrefix` preserve formatting on the
  outer node when a template replaces an existing subtree (e.g. the
  prefix on a `MethodInvocation.Select.Element` survives the swap).
- Scaffold-based parser (`pkg/template/scaffold.go`) compiles a template
  string into an AST that's cached per `GoTemplate` instance.

## Deferred (intentional out-of-scope items)

These are explicitly out-of-scope per the eng review:

1. **Type-checked named placeholders** (`#{name:any(...)}`). v1 keeps
   capture-typed placeholders. Rationale: Go's lighter type system makes
   the constraint syntax less load-bearing for refactor recipes; revisit
   when a real recipe asks for it.
2. **`contextSensitive()` parse mode.** JavaTemplate flips this on when
   the template references symbols only resolvable from the surrounding
   cursor (e.g. inner-class names). The Go scaffold parser is already
   "context-light" by default (it doesn't attempt to resolve the
   template's own references against the call site's environment), so
   the explicit toggle adds little until Go-specific use cases surface.
3. **Static imports.** Java's `staticImports` adds `import static …`
   declarations. Go has no static-import concept; the Go template
   compiler ignores the surface entirely.
4. **Coordinate API surface (`JavaCoordinates`).** Java's
   `apply(coordinates, params)` lets recipes splice templates *before* /
   *after* / *replace* a target node. The Go equivalent is the
   pattern-match approach: write a `GoPattern` for the target, write a
   `GoTemplate` for the replacement, and use `Rewrite(before, after)`.
   Adding a coordinate API on top is feasible but would duplicate the
   pattern surface; we'll add it only if a recipe actually needs splice
   semantics that Pattern→Template doesn't cover.

## What recipe authors should know

For most refactors, the Go-side template surface is what you want:

```go
import "github.com/openrewrite/rewrite/rewrite-go/pkg/template"

before := template.ExpressionPattern(`errors.Is(#{X}, #{Y})`).Build()
after  := template.ExpressionTemplate(`xerrors.Is(#{X}, #{Y})`).Imports("xerrors").Build()
visitor := template.Rewrite(before, after)
```

For inserting a fresh statement (no before-match):

```go
tmpl := template.StatementTemplate(`fmt.Println("hi")`).Imports("fmt").Build()
result := tmpl.Apply(cursor, nil)
```

The Java →  Go porting cheat-sheet:

| Java                                   | Go                                                       |
|----------------------------------------|----------------------------------------------------------|
| `JavaTemplate.builder("…").build()`    | `template.StatementTemplate("…").Build()`                |
| `.imports("foo")`                      | `.Imports("foo")`                                        |
| `.apply(getCursor(), JavaCoordinates.replace(target), arg1, arg2)` | match `target` with a `GoPattern`, then `template.Rewrite(before, after)` — the `RewriteVisitor` does the splice |
| `#{any()}` as a wildcard placeholder    | `template.ExpressionPattern("#{X}").Build()` with an unconstrained `Capture` |
| `#{name:any(java.util.List)}`           | not yet — file an issue if you hit this                  |

## Conclusion

GoTemplate's surface is at functional parity with `JavaTemplate` for the
common refactor patterns. The surface differences are mostly stylistic
(explicit kind factories vs. a single overloaded builder) or
intentionally narrowed (no `staticImports`, no coordinate API). The
deferred items are all opt-in features; the default Go template
experience covers what recipes-go authors need today.

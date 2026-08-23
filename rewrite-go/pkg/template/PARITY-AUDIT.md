# rewrite-go template and matcher parity audit

Two audits live here. The first maps `pkg/template/GoTemplate` against
`org.openrewrite.java.JavaTemplate`. The second maps `pkg/template/GoPattern`
— the AST matcher — against the JavaScript and Python matchers, which are the
only other implementations of the same pattern surface.

## GoTemplate ↔ JavaTemplate

Item (10) of the rewrite-go parity plan asked for ergonomic parity between
`pkg/template/GoTemplate` and `org.openrewrite.java.JavaTemplate`. This
document lists every public method on `JavaTemplate` (Java) and maps it to
the equivalent surface on `GoTemplate` (Go), noting what was already
present, what was added in this PR, and what is intentionally deferred.

### Audit summary

| Surface | JavaTemplate | GoTemplate | Status |
|---|---|---|---|
| Builder | `JavaTemplate.builder(code)` | `template.ExpressionTemplate(code)` / `StatementTemplate(code)` / `TopLevelTemplate(code)` | ✓ shipped (kind-explicit factories preferred over a single overloaded builder) |
| Build the template | `.build()` | `.Build()` | ✓ shipped |
| Parse-context imports | `.imports(String...)` | `.Imports(...string)` | ✓ shipped |
| Static imports | `.staticImports(String...)` | n/a | not applicable — Go has no static-import concept |
| Coordinate-based substitution | `.apply(JavaCoordinates, params...)` | `.Apply(cursor, *MatchResult)` | ✓ shipped via match captures (see deferred note below) |
| Pattern → template rewrite | `JavaIsoVisitor` + `JavaTemplate.apply` per visit | `template.Rewrite(before, after)` returns a `RewriteVisitor` | ✓ shipped (single-call ergonomic that matches and replaces in one step — Go-side delta over Java) |
| Context-sensitive parsing | `.contextSensitive()` | not yet | deferred (recipes in the wild rarely flip this on for refactoring; revisit if a real recipe asks) |
| Named placeholders | `#{name}` substitution by name + type constraint | positional `#{X}` capture-by-name through `*Capture` | ✓ named via `*Capture` already; type constraints are deferred (see below) |
| Type-checked named placeholders | `#{name:any(java.util.List)}` | `Capture.WithType("time.Duration")` | ✓ shipped — assignability, with interface satisfaction computed from the two method sets since Go declares none |
| Cursor-aware insertion | parameter to `.apply(cursor, ...)` | parameter to `.Apply(cursor, ...)` | ✓ shipped — the cursor names the node being replaced, from which `Apply` takes the leading whitespace, whether the result needs parenthesizing against the expression around it, and the level to indent to |
| Variadic placeholders | n/a | `Capture.Variadic(min, max)` matched by `GoPattern`, expanded by `GoTemplate` | ✓ shipped — Go-side delta over Java. `JavaTemplate` has no variadic placeholder; `Substitutions.maybeExpandVarargsNewArray` flattens a captured varargs `J.NewArray` back into an argument list, which is a Java-language repair rather than a capture kind |

### Already present before this PR (no delta required)

The Go-side template engine is ~740 LOC and pre-dates the parity work.
Surface that was already at parity:

- `TemplateBuilder` with a fluent API (`Captures`, `Imports`, `Build`).
- Three template kinds (`ExpressionTemplate`, `StatementTemplate`,
  `TopLevelTemplate`) — this is more explicit than Java's overloaded
  `JavaTemplate.builder` (which infers the kind from the substitution
  coordinate). Recipe authors don't need to know coordinate semantics.
- `Apply(cursor, *MatchResult)` returns the substituted subtree with
  capture values spliced in, placed at the site the cursor names.
- `Rewrite(before, after)` packages match-and-replace into a single
  `RewriteVisitor` — convenient for 1:1 rewrites.
- `getLeadingPrefix` / `setLeadingPrefix` carry the replaced node's own
  leading whitespace onto the replacement (e.g. the prefix on a
  `MethodInvocation.Select.Element` survives the swap), which is the first
  of the steps `Apply` takes to place a result.
- Scaffold-based parser (`pkg/template/scaffold.go`) compiles a template
  string into an AST that's cached per `GoTemplate` instance.

### Deferred (intentional out-of-scope items)

These are explicitly out-of-scope per the eng review:

1. **`contextSensitive()` parse mode.** JavaTemplate flips this on when
   the template references symbols only resolvable from the surrounding
   cursor (e.g. inner-class names). The Go scaffold parser is already
   "context-light" by default (it doesn't attempt to resolve the
   template's own references against the call site's environment), so
   the explicit toggle adds little until Go-specific use cases surface.
2. **Static imports.** Java's `staticImports` adds `import static …`
   declarations. Go has no static-import concept; the Go template
   compiler ignores the surface entirely.
3. **Coordinate API surface (`JavaCoordinates`).** Java's
   `apply(coordinates, params)` lets recipes splice templates *before* /
   *after* / *replace* a target node. The Go equivalent is the
   pattern-match approach: write a `GoPattern` for the target, write a
   `GoTemplate` for the replacement, and use `Rewrite(before, after)`.
   Adding a coordinate API on top is feasible but would duplicate the
   pattern surface; we'll add it only if a recipe actually needs splice
   semantics that Pattern→Template doesn't cover.

### What recipe authors should know

For most refactors, the Go-side template surface is what you want:

```go
import "github.com/openrewrite/rewrite/rewrite-go/pkg/template"

x, y := template.Expr("x"), template.Expr("y")
before := template.Expression(fmt.Sprintf(`errors.Is(%s, %s)`, x, y)).Captures(x, y).Build()
after  := template.ExpressionTemplate(fmt.Sprintf(`xerrors.Is(%s, %s)`, x, y)).Imports("xerrors").Build()
visitor := template.Rewrite(before, after)
```

For inserting a fresh statement (no before-match):

```go
tmpl := template.StatementTemplate(`fmt.Println("hi")`).Imports("fmt").Build()
result := tmpl.Apply(cursor, nil)
```

`Imports(...)` is parse context only, matching `JavaTemplate.imports(...)`.
Declarative template recipes that should edit source imports must say so
explicitly:

```go
template.WithAfter(`strconv.Itoa(#{N})`, template.Imports("strconv"), template.SourceImports("strconv"))
```

The Java →  Go porting cheat-sheet:

| Java                                   | Go                                                       |
|----------------------------------------|----------------------------------------------------------|
| `JavaTemplate.builder("…").build()`    | `template.StatementTemplate("…").Build()`                |
| `.imports("foo")`                      | `.Imports("foo")`                                        |
| `.apply(getCursor(), JavaCoordinates.replace(target), arg1, arg2)` | match `target` with a `GoPattern`, then `template.Rewrite(before, after)` — the `RewriteVisitor` does the splice |
| `#{any()}` as a wildcard placeholder    | a `*Capture` interpolated into the pattern string, which prints as its placeholder |
| `#{name:any(java.util.List)}`           | `template.Expr("x").WithType("time.Duration")`            |

### Conclusion

GoTemplate's surface is at functional parity with `JavaTemplate` for the
common refactor patterns. The surface differences are mostly stylistic
(explicit kind factories vs. a single overloaded builder) or
intentionally narrowed (no `staticImports`, no coordinate API). The
deferred items are all opt-in features; the default Go template
experience covers what recipes-go authors need today.

## GoPattern ↔ JavaScript and Python matchers

`GoPattern` matches an LST subtree against a parsed pattern and binds
placeholder identifiers to what they matched. The equivalent surfaces are
`rewrite-javascript/rewrite/src/javascript/templating/` (~6300 LOC, with
`comparator.ts` at 1422) and
`rewrite-python/rewrite/src/rewrite/python/template/` (~3000 LOC, with
`comparator.py` at 537).

### The structural comparison is built differently in Go

Python's `PythonComparatorVisitor._compare_fields` iterates
`dataclasses.fields()`. JavaScript's matcher iterates `Object.keys(j)`. Both
skip identity and formatting fields by name and recurse into whatever remains,
so neither can omit a node kind or a field: coverage is a property of the walk
rather than of a table someone maintains.

Go instead dispatched on a 65-case type switch in `matchProperties`. Three
classes of defect followed from that, all of them measured against the parser
rather than inferred from the source.

**Kinds the switch never reached.** Taking the 78-case dispatch in
`visitor.GoVisitor.Visit` as the model's enumeration, seven reachable kinds
were absent: `golang.DeclarationBlock`, `golang.ExpressionStatement`,
`golang.StatementExpression`, `java.ParameterizedType`, `java.TypeParameter`,
`java.TypeParameters` and `java.Annotation`. A pattern naming one never
matched — `const ( A = 1 )` and `Foo[string]` matched nothing at all. Six
further kinds, `golang.GoMod`/`GoModBlock`/`GoModDirective`/`GoModValue` and
`golang.GoSum`/`GoSumLine`, are unreachable by construction: a pattern is
parsed from Go source by `parser.NewGoParser`, which produces neither.

**Fields the cases forgot.** A case that compares some of a node's children
and ignores the rest reports a match the source does not support:

The audit corpus finds thirteen such pairs; five stand for the rest:

| pattern | candidate | matched |
|---|---|---|
| `func F(y bool) {…}` | `func F(x int) {…}` | yes |
| `func F() string {…}` | `func F() int {…}` | yes |
| `func (a *A) F() {…}` | `func (b *B) F() {…}` | yes |
| `func G[T any]() {…}` | `func G[U comparable]() {…}` | yes |
| ``F int `json:"a"` `` | ``F int `json:"b"` `` | yes |

`java.MethodDeclaration` compared `Name` and `Body` only, leaving
`Parameters`, `ReturnType`, `TypeParameters` and `LeadingAnnotations`
unread, and `golang.MethodDeclaration` compared `Declaration` and never
`Receiver`. A false match is worse than a missing one, because the recipe
rewrites on it.

**A nil child read as a present one.** `type I interface{ M() }` crashed the
match. An interface method has no body, and `MethodDeclaration.Body` is a
`*java.Block`, so the nil arrives at a parameter of interface type as a
non-nil `java.J` holding a nil pointer; the guard against a missing child
tests the interface and lets it through. `match_result.go` carries an
`isNilTree` helper for the same hazard on the binding side.

**Markers, which the peers can afford to ignore and Go cannot.** JavaScript
and Python drop the marker collection wholesale, and their models let them:
nothing that decides what the source says lives there. Go's does. `x = 1`
matched `x := 1` and `var x = 1` matched `const x = 1`, because `:=` and
`const` are `golang.ShortVarDecl` and `golang.ConstDecl` rather than tree
structure. Rewriting the first of those drops a declaration.

### Design

Comparison is a reflective field walk over a per-`reflect.Type` plan, built
once and cached. Each exported field resolves to one rule:

| rule | applies to |
|---|---|
| skip | `uuid.UUID`, `java.Space`, `java.Markers` (compared separately, below), and `golang.CompilationUnit`'s `SourcePath` / `CharsetBomMarked` / `EOF` / `PackageDecl` / `Imports` |
| type slot | any field whose type implements `java.JavaType` |
| padded | `RightPadded[T]` / `LeftPadded[T]` / `Container[T]`, recursing on `Element`/`Elements` and ignoring `Before`/`After` |
| node | anything implementing `java.J`, including the by-value `ForControl` and `ForEachControl` embeds |
| slice | element-wise, with variadic run absorption for `[]RightPadded[T]` |
| scalar | `==`, covering strings, bools, ints, `DeclKind`, `ChanDir`, the operator enums and `Literal.Value` |

The padded rule keys on field name, so it holds for every instantiation of `T`
without enumerating them, and `LeftPadded[Space]` degenerates to a skip.

A recipe's visitor method calls `Match` once per node it has narrowed to, so
the candidate usually shares the pattern's kind and the reject on concrete
type never fires. `Identifier`, `MethodInvocation`, `FieldAccess`, `Binary`
and `Block` therefore read their own fields, which costs 50ns against the
walk's 180ns on that call, and 248ns against 1172ns once arguments nest —
`BenchmarkMatchSameKindMiss` beside `BenchmarkWalkSameKindMiss` measures both.
`TestFastPathKindsHaveTheFieldsItReads` names the fields each of the five
holds, so one added to the model fails a test rather than going uncompared.
Everything else goes through the walk, so a new node kind needs no work.

The variadic run is the one thing written twice, over a typed list here and a
reflected one in the walk. Handing the typed list to the reflected
implementation costs the hand-written comparisons the speed they exist for —
a deep match goes from 2.3us back to 8.3us — so
`TestFastPathAgreesWithWalkOnVariadicRuns` compares the two on what they match
and on the run each absorbs instead.

`TestFastPathAgreesWithWalk` is what makes the hand-written comparisons safe:
the walk reaches every field by construction, so it is the answer they are
held to, under each matching mode, on what they bind as well as what they
answer.

Mutating their clauses one at a time, twelve of seventeen fail a test. Of the
five that do not: an identifier's annotations are a slot Go source never fills
and only the RPC peer sends; a call's method type is settled first by the
declaring-type comparison; an identifier's variable slot is redundant with the
name and type beside it; and the type slots on `FieldAccess` and `Binary` want
a fixture the corpus does not attribute.

Generating the comparison per node type from the model would give the same
speed without the hand-written half. It is the principled successor to both
the walk and the fast path, and wants its own change: there is no model
generator in the tree today.

Explicit handling survives ahead of the walk where a node's meaning is not its
fields: placeholder binding, `java.Literal` (its source text and its type),
`java.Empty` (present or not), variadic runs, and the FQN-based
`java.MethodInvocation` comparison described below.

**Which markers count.** Six decide what the source says and are compared:
`ShortVarDecl`, `ConstDecl`, `VarKeyword`, `InterfaceMethod` and `Builtin` by
presence, and `StructTag` by the source of the `*java.Literal` it carries.
`Builtin` separates a call to a predeclared function from one to a
user-defined function of the same name, and the printer never reads it, so
what reaches the output is the wrong test to apply. The rest carry layout —
`Semicolon`, `TrailingComma`, `GroupedSpec`, `GroupedImport`, `ImportBlock`,
`StructTagQuote`, `ChanDirMarker`, `ImplicitForClauses`, `TypeSwitchGuard` —
or describe the project rather than the file: `GoProject`,
`GoResolutionResult`.

`TestEveryMarkerIsClassified` reads the marker types back out of both tree
packages and fails on one neither list names, so a marker cannot join the
ignored set unexamined. A `StructTag` reaches a Go-parsed tree only from the
RPC peer, this parser reading a tag into `LeadingAnnotations` instead; the
`json:"a"` against `json:"b"` row above is that field, not the marker.

### Reading an expression rather than its text

Two spellings of one expression match. Parentheses are read through, and a
literal compares by what it evaluates to:

| pattern | candidate | matches |
|---|---|---|
| `true` | `(true)` | yes — the tree says how an expression groups, parentheses only how it was written |
| `(a + b) * c` | `a + b*c` | no — the grouping differs, and reading through parentheses leaves it differing |
| `1` | `0x1` | yes — one literal written twice |
| `"x"` | `` `x` `` | yes |
| `1` | `1.0` | no — an integer and a float are different constants |

A capture binds what it found, parentheses included, so a replacement keeps
the source's own spelling. Parentheses are read through only where two kinds
disagree, which keeps the cost off the path where they agree; a pattern is
never printed, so its own root is unwrapped once when it is parsed.

`true`, `false` and `nil` are predeclared identifiers in Go rather than
literals, so they are `java.Identifier` and the value comparison never sees
them — `(true)` against `true` is the parentheses half alone.

**The line this draws.** Compare values the parser already computed; never
compute new ones. `sameLiteralValue` reads the `Value` the parser filled, so
`0x1` against `1` evaluates nothing. Reading through parentheses is a
projection with no arithmetic, and it is a rule no normalising pass could
apply anyway, since a capture has to bind the parentheses it found.

`2 + 4` against `6`, `!true` against `false`, and `1` against `1.0` — which
Go's untyped constants do make equal — sit on the far side of that line,
needing the program evaluated. Each is a constant-folding rule and the set of
them is a pass, not a special case.

When that step comes it may not be a folding pass at all: `go/types` has
already folded every constant expression, and `types.Info.Types[expr].Value`
holds the result. Recording it on the model and reading it here covers all
three cases uniformly, for a value slot plus RPC registration and a few lines
in the comparator — against re-implementing Go's untyped-constant arithmetic
in an LST visitor that would own its own correctness surface.

### Type attribution

Structural comparison is the default and the only mode the peers offer:
`comparator.ts` compares no types at all, and Python layers
`PythonSemanticComparator` on top of its structural visitor. Go follows
Python, with `PatternBuilder.TypeMatching`:

| mode | one side unattributed | both attributed |
|---|---|---|
| `TypeMatchingOff` (default) | type slots unread | type slots unread |
| `TypeMatchingLenient` | matches | compared |
| `TypeMatchingStrict` | fails | compared |

Python's attribution comes from `ty` and is often partially absent, which
makes its lenient default close to off in practice. Go's comes from `go/types` and is either complete for a package or
absent for all of it, so the two modes diverge sharply and the default has to
be the behaviour recipes already depend on.

Comparison mirrors Python's `_compare_types`: two nils are equal, one nil
defers to the mode, two primitives compare by the intersection of
`matcher.GoTypeNames` — not `matcher.IsSameGoType`, which answers false for
literal keywords by design — two method types compare by name and declaring
type FQN, two variables by name and the type they hold, and anything else by
`matcher.GetFullyQualifiedName`, with an empty FQN on either side deferring to
the mode.

A variable's owner is left out of that comparison on purpose. A pattern
declares its variables in a package of its own, so comparing owners would
have a pattern naming a variable answer to no source at all — and, since a
`JavaTypeVariable` carries no fully qualified name, would report every such
comparison as an attribution that is missing.

When both sides of a `java.MethodInvocation` resolve **and both receivers name
a package**, the declaring type FQN, the method name and the call's explicit
type arguments are compared and the receiver is skipped, so a `fmt.Println`
pattern still matches source that imported `fmt` under an alias while
`slices.Clip[[]int]` still differs from `slices.Clip[[]string]`. This is the Go reading of the case Python's
comparator documents for `os.path.join` against a bare `join`.

The receiver has to name a package for this to be sound. Skipping it wherever
the declaring types agree would read `one.WriteString` and `two.WriteString`
as the same call, and would leave a capture written in receiver position
unbound while still reporting a match — the placeholder then prints into the
source. Attribution tells the two apart: a value's identifier carries the
variable it reads, and a package name carries none, being no value.

### The type a capture declares

`Capture.WithType` is the one type comparison an author writes by hand, so it
is read whatever the mode says about the slots around it — including
`TypeMatchingOff`, where nothing else is. The mode keeps the question it
answers everywhere else: a candidate carrying no attribution binds under
`TypeMatchingLenient` and is refused under the other two.

The constraint is the type the scaffold preamble resolved the placeholder to,
not the name it was written with. That is what lets an interface be satisfied
structurally: Go declares no `implements`, and `mapNamed` accordingly leaves
`Interfaces` empty, so `error` is answered from the candidate's method set and
a `*MyErr` carrying `Error() string` matches where `io.EOF` does.
`matcher.IsAssignableToType` is that relation — identity first, then a
literal's keyword answering for the class of Go types it names, `byte`/`uint8`
folded, and the method set last. A name the scaffold cannot resolve fails the parse rather
than silently constraining nothing, and a declared type on anything but an
expression capture panics, an expression being the only thing that carries one.

The type filters what a match binds, and not what `MatchResult.Bind` does:
`Instantiate` splices the node an author names, so `WithType("string")` bound
to an `int` literal emits `errors.New(42)` and says nothing. This is
`JavaTemplate`'s split too — `Substitutions.substituteTypedPattern` reads the
declared type to generate the substitution stub and never compares the
parameter against it — and it is what keeps a hand-built node with no
attribution bindable, which a recipe supplying a runtime-computed literal
needs.

A run absorbed by a variadic capture is held to the type element by element,
and a pattern that is a placeholder alone reaches the comparator to be held
there too — its kind rejects nothing, so the type is all that is left.

Satisfaction is as exact as the attributed model, which falls short of Go in
four ways doc/recipe-authoring.md lists under "Typed captures" — each a
property of the type mapper rather than of this comparison, and the first two
pinned by `capture_type_test.go` so a model that grows past them fails there.

Java is the only peer with the same surface, and Go follows it:
`JavaTemplateSemanticallyEqual` resolves `#{name:any(FQN)}` to a `JavaType`
when the template is parsed and matches with
`TypeUtils.isAssignableTo(…, INFER)`, with no mode to switch it off and
`isPseudoType` refusing an unattributed candidate. It also refuses to bind a
type rather than an expression, which is the rule the panic states earlier.
JavaScript parses a `typeConstraint` out of its placeholder identifiers in
`utils.ts` and its comparator never reads it; Python has no typed capture at
all. Both offer a predicate instead — see "Capture constraints" below.

**Degraded attribution.** A type-comparing match against a package whose
attribution is incomplete returns false, which reads exactly like source that
does not match. The comparator therefore counts the comparisons that were
inconclusive for want of attribution, and `GoPattern.Explain` reports the
count with the path of the node where it happened, so the two cases are
distinguishable. The stronger behaviour — refusing rather than answering
false when the package is known to be partially attributed — waits on the
`PartialTypeAttribution` marker, which is not yet in the tree.

### Attribution context for the pattern itself

JavaScript's `PatternOptions.context` takes declarations that are prepended to
the pattern before parsing, so the pattern gets attributed. Go arrives at the
same place from two directions that already existed: `Imports` puts import
declarations in the scaffold, and `ExportData` supplies the importer that
resolves them, so `Expression("fmt.Println(\"x\")").Imports("fmt")` already
yields a resolved `MethodType` whose declaring type is `fmt`. `Context` adds
what those two cannot express — type aliases, local declarations, and any
other top-level Go the pattern needs to read against.

### Capture constraints, and why Go does not get them

Both peers let a capture carry a predicate: Python's runs over the captured
value after the match, JavaScript's also receives the cursor and the captures
bound so far. Go does not, and should not yet.

A Go recipe is compiled Go. An author wanting "capture only if it is a
`time.Duration`" writes the check against the match result, with the type
system behind it:

```go
if m := pat.Match(node, cursor); m != nil &&
    matcher.IsOfClassType(matcher.TypeOfExpression(m.Get("d")), "time.Duration") {
```

A template literal is the only surface a JavaScript or Python pattern author
has, which is why those implementations need the predicate inside the capture.
The declarative Go surface — `template.WithAfter` and friends — is
configuration and could not carry a closure regardless. Enforcing
`Capture.WithType` covers the declarative subset that is actually expressible.

### Where Go leads

| capability | Go | JavaScript | Python |
|---|---|---|---|
| Variadic capture | a bounded run anywhere in a list, via `Capture.Variadic(min, max)` | marker-based, whole list | whole argument list only |
| Match-free template instantiation | `MatchResult.Bind` / `BindList` | — | — |
| Type-attribution comparison | opt-in, three modes | — | on by default, lenient only |
| Type-constrained capture | `WithType` filtering a match, assignable, interfaces satisfied structurally | parsed and never read | — |

### Enforcement

`TestMatcherDistinctness` holds the audit. A corpus of fixtures spans every
kind `visitor.GoVisitor.Visit` dispatches; each must match itself, and no
fixture may match a different one. Distinctness is the property the rows above
violated, and counting covered kinds would have caught none of them. The kinds
that cannot be reached from Go source are asserted unreachable rather than
passed over.

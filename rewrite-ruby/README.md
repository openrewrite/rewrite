# rewrite-ruby

OpenRewrite support for Ruby source code. Parses Ruby into a Lossless Semantic Tree (LST) that reuses the Java `J` model wherever possible, adding Ruby-specific `Rb` types and markers only when necessary.

## Parser front end

The parser is [Prism](https://github.com/ruby/prism), run standalone. `RubyParser` hands the source bytes to `org.ruby_lang.prism.wasm.Prism` and loads the serialized result with `Loader`; there is no `org.jruby.Ruby` runtime. The `jruby-base` dependency is kept only because it supplies and pins the Prism artifacts (`jruby-prism` → `prism-parser-api`/`prism-parser-wasm`). Compile against `org.ruby_lang.prism.*` — the same classes appear relocated as `org.jruby.internal.prism.*` inside `jruby-complete`, which this module never depends on.

Three things shape `RubyParserVisitor`:

- **Byte offsets.** Every Prism node carries `startOffset`/`length` as offsets into the source *bytes*, while the LST and the printer work on the decoded `String`. `PrismSource` owns both and translates between them, which is what makes non-ASCII source round-trip. `prefix(node)` consumes whitespace up to a node's start and asserts the cursor landed exactly there, so a desync fails with file and offset context instead of corrupting everything downstream.
- **No comments, no token positions.** `ParseResult` has no comment array and the node classes carry no `nameLoc`/`operatorLoc`/`openingLoc`, so whitespace, comments and keywords are re-lexed from the source with a linear `int cursor` anchored on node offsets. `peekWhitespace` is the only backtracking primitive.
- **Heredocs live outside their node's span.** A heredoc node covers only its `<<~ID` opener. Openers are queued as they are seen and their bodies claimed the first time the cursor crosses a newline (a line holding nothing but the terminator closes one, indented only for `<<~`/`<<-`), then folded into the tree by a final pass keyed by node id. `Rb.Heredoc` holds the opener and terminator as their own fields and the printer replays the body at the first newline it emits after the opener, so whitespace rewriting cannot strand it.

Prism is asked for `partialScript` (so fragments with a top-level `next`/`break`/`return` parse), not `mainScript` (so a shebang naming something other than ruby is just a comment), and is always handed newline-terminated bytes (it will not close a heredoc whose terminator ends the file).

This module targets Java 21, unlike most of the repo. JRuby 10 ships class file version 65 and cannot load below Java 21, so `build.gradle.kts` nulls the `--release 8` that `org.openrewrite.build.java-base` sets.

Any Ruby syntax the visitor has not been taught reaches `defaultVisit`, which throws with the Prism node name. Nothing maps to `J.Unknown`.

## Ruby-specific LST types (`Rb.*`)

| Ruby syntax | LST element | Example(s) | Why an `Rb` type? |
|---|---|---|---|
| Source file | `Rb.CompilationUnit` | any `.rb` file | Ruby files are a list of arbitrary statements. `J.CompilationUnit` holds only imports and class declarations. |
| Array literal, implicit array | `Rb.Array` | `[1, 2]`, `a, b = 1, 2` | `J.NewArray` models a typed Java array creation. Ruby arrays are literals with optional brackets, so the same type also carries bracket-less target and argument lists. |
| Hash literal | `Rb.Hash`, `Rb.Hash.KeyValue` | `{a: 1, "b" => 2}` | Java has no hash literal, and the two separators (`:` label vs `=>` rocket) are part of the syntax. |
| Symbol | `Rb.Symbol` | `:name`, `:"with space"`, `%s(sym)` | An interned name with several delimiter spellings; no `J` equivalent. |
| Block | `Rb.Block` | `each { \|x\| ... }`, `each do \|x\| ... end` | A block argument is part of the call's syntax, not an ordinary argument, and its `{}`/`do…end` form and `\|params\|` list have no `J` counterpart. |
| Block argument | `Rb.BlockArgument` | `&blk`, `&:to_s`, `def f(&nil)` | The `&` prefix converting a proc to a block argument, or refusing one. |
| Splat | `Rb.Splat` | `*args`, `**opts`, `**nil` | Spreading an array or a hash into a list, with the operator distinguishing which. |
| `%w`/`%i` array | `Rb.DelimitedArray` | `%w[a b]`, `%i[a b]`, `%W(#{x})` | The elements are whitespace-separated with no commas, and the delimiter is part of the literal. |
| Interpolated string, regex, `%x` | `Rb.ComplexString` | `"hi #{name}"`, `/ab#{c}/i`, `` `ls` `` | These have internal structure (literal parts plus real expressions) and regex options, so they cannot be a `J.Literal`. |
| Heredoc | `Rb.Heredoc` | `<<~SQL ... SQL` | The value lives outside the node's own span and the terminator has to round-trip. |
| Ruby-only binary operators | `Rb.Binary` | `a <=> b`, `2 ** 8`, `1..5`, `1...5`, `s =~ /a/`, `s !~ /a/`, `r === x`, `if a..b`, `"a" "b"`, `in :x \| :y` | `J.Binary.Type` has no `<=>`, `**`, range, flip-flop, match, `===`, implicit concatenation or pattern-alternation operator. |
| Ruby-only unary operators | `Rb.Unary` | `defined?(x)`, `in ^expected` | `J.Unary.Type` has neither `defined?` nor the pattern pin `^`. |
| `\|\|=` / `&&=` | `Rb.AssignmentOperation` | `x \|\|= 1`, `@memo &&= v` | `J.AssignmentOperation.Type` has no short-circuiting assignment operators. |
| Multiple assignment | `Rb.MultipleAssignment` | `a, b = 1, 2`, `a, (b, c) = ...` | Several targets bind simultaneously from several initializers; `J.Assignment` is one-to-one. |
| Sub-array index | `Rb.SubArrayIndex` | `a[1, 2]` | An index written as start and length, which `J.ArrayDimension` cannot hold. |
| Rational / imaginary literal | `Rb.NumericDomain` | `3r`, `2i` | A numeric literal with a domain suffix. |
| Module | `Rb.Module` | `module Api::V1 ... end` | Not a class: it has no superclass and prints `module`. Its name is an `Expression` so a compact name keeps every segment. |
| Eigenclass | `Rb.OpenEigenclass` | `class << self ... end` | Reopening an object's singleton class has no `J` shape. |
| Singleton method | `Rb.ClassMethod` | `def self.create`, `def Point.sum` | A method definition qualified by a receiver, which `J.MethodDeclaration` cannot express. |
| `alias` | `Rb.Alias` | `alias new old`, `alias :new :old`, `alias $new $old` | A declaration of its own. |
| `undef` | `Rb.Undef` | `undef foo`, `undef :foo, :bar` | Removes method definitions; a list of names with no `J` equivalent. |
| `begin`/`rescue`/`else`/`ensure` | `Rb.Rescue` | `begin ... rescue => e ... end`, `x = f rescue nil` | Wraps a `J.Try` because Ruby's rescue can also borrow the enclosing `def`/block/class `end`, or be a postfix modifier. |
| `begin` block | `Rb.Begin` | `begin ... end` (no rescue) | A grouping expression rather than a `try`. |
| `BEGIN`/`END` | `Rb.PreExecution`, `Rb.PostExecution` | `BEGIN { ... }`, `END { ... }` | Program-lifecycle hooks with no `J` statement. |
| `yield` | `Rb.Yield` | `yield`, `yield(a, b)` | Calls the block passed to the enclosing method; not a `J` statement. |
| `break`/`next` with a value | `Rb.Break`, `Rb.Next` | `break 1`, `next unless x` | `J.Break`/`J.Continue` carry only a label, not a value. |
| `redo`/`retry` | `Rb.Redo`, `Rb.Retry` | `redo`, `retry` | No `J` equivalent. |
| One-line pattern match | `Rb.BooleanCheck`, `Rb.RightwardAssignment` | `h in {a: 1}`, `h => {a:}` | Same shape, different semantics: `in` yields a boolean, `=>` binds or raises. |
| Pattern binding | `Rb.PatternBinding` | `in Integer => n` | Binds a match to a name. Mirrors `Rb.RightwardAssignment`'s token with the operands the other way round, so it is a separate type. |
| Pattern guard | `Rb.PatternGuard` | `in [x, y] if x > y` | `J.Case` has no guard slot, so the guard stands in its expression container. |
| Deconstruct pattern | `Rb.StructPattern` | `in Point[..5, ..5]`, `in Point(x: ..5)` | A constant followed by a bracketed or parenthesized sub-pattern. |
| Expression in statement position | `Rb.ExpressionStatement`, `Rb.StatementExpression` | `1 + 1` as a statement, `(if c then a else b end)` as a value | Ruby lets expressions and statements stand in for each other; these wrap rather than duplicating every `J` type. |
| Expression in type position | `Rb.ExpressionTypeTree` | `class Point < Struct.new(:x, :y)` | A superclass computed by an expression, where `J` expects a `TypeTree`. |

## Ruby-specific markers

| Ruby syntax | LST element + marker | Example(s) | Why a marker? |
|---|---|---|---|
| `unless` | `J.If` / `Rb.PatternGuard` + `Unless` | `unless x then a end`, `in [x] unless x.zero?` | Structurally an `if` with the condition inverted in spelling only. |
| Statement modifiers | `J.If` + `IfModifier`, `J.WhileLoop` + `WhileModifier` | `do_it if cond`, `step while more?` | The same conditional written with the body first. |
| `until` | `J.WhileLoop` + `Until` | `until done? ... end` | A `while` printed with the opposite keyword. |
| `and` / `or` / `not` | `J.Binary`, `J.Unary` + `EnglishOperator` | `a and b`, `not x` | Spelled-out forms of `&&`, `\|\|` and `!`. |
| Safe navigation | `J.MethodInvocation`, `J.FieldAccess` + `SafeNavigation` | `obj&.m`, `user&.profile = p`, `config&.timeout \|\|= 30` | A `.` printed as `&.`. |
| `::` as a call separator | `J.MethodInvocation` + `Colon2` | `Nokogiri::XML(body)` | The same call as `Nokogiri.XML(body)`, spelled differently. |
| Optional `then` / `do` | `J.ControlParentheses`, `JContainer` + `ExplicitThen`, `ExplicitDo` | `if c then a end`, `while c do a end` | Keywords Ruby allows but does not require. |
| `case ... in` | `J.Case` + `PatternCase` | `case x in Integer then ... end` | A `J.Case` printed with `in` instead of `when`. |
| Explicit `begin` around a rescue | `Rb.Rescue` + `ExplicitBegin` | `begin ... rescue ... end` | Distinguishes a rescue with its own `begin` from one borrowing a `def`/block/class `end`. |
| `rescue` modifier | `Rb.Rescue` + `RescueModifier` | `value = Integer(s) rescue nil` | A postfix rescue on a single expression. |
| Keyword argument | `J.VariableDeclarations` + `KeywordArgument` | `def f(a:, b: 1)` | A parameter printed `name:` rather than as a plain name. |
| Keyword rest argument | `J.VariableDeclarations` + `KeywordRestArgument` | `def f(**kwargs)` | A varargs parameter printed with `**` rather than `*`. |

Markers from `org.openrewrite.java.marker` are reused rather than duplicated: `Semicolon` for a `;` statement separator, `TrailingComma` for a trailing `,` in a list, `OmitParentheses` for a bracket-less array/argument/parameter list or a brace-less hash, `OmitBraces` for an endless method body and for `#@ivar` interpolation, and `ImplicitReturn` for a method's last expression.

## Standard `J` mappings (no Ruby-specific type needed)

| Ruby syntax | LST element | Example(s) |
|---|---|---|
| Class declaration | `J.ClassDeclaration` | `class Point < Base ... end` |
| Method definition | `J.MethodDeclaration` | `def f(a, b = 1) ... end`, `def pi = 3.14` |
| Method invocation | `J.MethodInvocation` | `obj.m(a)`, `puts x`, `a + b` written as `a.+(b)` |
| Identifier | `J.Identifier` | `x`, `@ivar`, `@@cvar`, `$gvar`, `CONST`, `_1`, `it` |
| Literal | `J.Literal` | `42`, `3.14`, `"hi"`, `true`, `nil` |
| Binary expression | `J.Binary` | `a + b`, `x == y`, `a && b` |
| Unary expression | `J.Unary` | `!flag`, `-x`, `~bits` |
| Assignment | `J.Assignment` | `x = 1`, `@x = 1` |
| Compound assignment | `J.AssignmentOperation` | `x += 1` |
| Index access | `J.ArrayAccess` | `a[0]`, `h[:k]` |
| Constant path | `J.MemberReference` | `Foo::Bar`, `::TopLevel` |
| Attribute access | `J.FieldAccess` | `obj.attr = 1` |
| Parenthesized expression | `J.Parentheses` | `(a + b)` |
| Conditional | `J.If` | `if c ... elsif c2 ... else ... end` |
| Ternary | `J.Ternary` | `c ? a : b` |
| `case`/`when` and `case`/`in` | `J.Switch`, `J.Case` | `case x when 1 then ... end` |
| While loop | `J.WhileLoop` | `while c ... end`, `until c ... end` |
| For loop | `J.ForEachLoop` | `for i in 0..5 ... end` |
| Lambda | `J.Lambda` | `-> (x) { x }`, `-> { it * 2 }` |
| Return | `J.Return` | `return 42`, and every method's last expression |
| Block / body | `J.Block` | a `def`, `class` or block body |
| Variable declaration | `J.VariableDeclarations` | parameters, rescue exception names |
| Empty | `J.Empty` | an omitted hash value (`{x:}`), a bare `;` |

## Printing differences

Even when Ruby maps to the same `J` type as Java, `RubyPrinter` adjusts the output:

| Feature | Java syntax | Ruby syntax |
|---|---|---|
| Class body | `class Foo { }` | `class Foo` … `end` |
| Method body | `void f() { }` | `def f` … `end` |
| Superclass | `class A extends B` | `class A < B` |
| Conditional | `if (c) { } else { }` | `if c` … `else` … `end` |
| `else if` | `else if (c)` | `elsif c` |
| Loop | `while (c) { }` | `while c` … `end` |
| For-each | `for (T x : xs)` | `for x in xs` |
| Switch label | `case 1:` | `when 1` / `in 1` |
| Continue | `continue` | `next` |
| Statement terminator | `;` required | `;` optional, and a separator rather than a terminator |
| Argument list | `f(a, b)` required | `f a, b` also legal (`OmitParentheses`) |
| Endless method | — | `def f = expr` (`OmitBraces` on the body block) |

## Building and testing

```bash
./gradlew :rewrite-ruby:test                                                  # the whole suite
./gradlew :rewrite-ruby:test --tests "org.openrewrite.ruby.tree.CaseTest"     # one class
./gradlew :rewrite-ruby:build                                                 # tests + license + javadoc
```

Keep fixtures inline as `ruby("...")` text blocks rather than `.rb` files on disk: a `.rb` file inside a source set gets a `#` license header stamped by `licenseFormat`, which breaks round-trip assertions. `rewriteRun(ruby("..."))` with no expected output is the assertion that every byte of the source landed in exactly one prefix, suffix or literal value.

`RubyCorpusTest` measures the parser against real code. It is skipped unless `-Druby.corpus.dir` is set:

```bash
./gradlew :rewrite-ruby:test --tests "org.openrewrite.ruby.RubyCorpusTest" \
  -Druby.corpus.dir=/path/to/a/ruby/checkout \
  -Druby.corpus.report=/tmp/corpus.txt
```

It prints a parse rate plus a histogram of failure causes, and alongside the report writes `<report>.failures` (one `cause<TAB>path` line per file) and `<report>.messages` (the full message per file), since the histogram keeps only one sample per cause.

See `CLAUDE.md` for the mapping conventions this module holds itself to, and `doc/adr/0012-ruby-parsing-via-jruby.md` for the parser front-end decision.

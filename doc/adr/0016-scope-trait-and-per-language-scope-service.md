# 16. Name resolution as a `Scope` trait over a per-language service

## Status

Proposed

## Context

Recipes keep asking the same questions about names. Is this name taken where I want to declare a
variable? Which declaration does this identifier refer to? Is this variable ever reassigned? Would
renaming it, or moving this block, make some name read something else?

Each recipe answered them on its own. `VariableNameUtils` walked the whole file per call.
`RenameVariable` kept a stack of scopes. The template generator had its own test for whether a
branch completes normally, to know which `instanceof` pattern variables are in scope. Recipe modules
carried more copies. The copies disagreed, and most compared variables by type attribution, which
cannot tell apart two locals of one name and type declared in sibling blocks.

The JavaScript module already has a scope model that answers these questions from the tree alone.
Languages built on `J` (Kotlin, Scala, Groovy) share most of Java's rules and part from them in a
few places each.

## Decision

Add one API in `rewrite-java` that answers from the position of a name in the tree, and let each
language supply the rules.

`Scope` is what recipes use. `Scope.at(cursor)` is the innermost scope at a position, and answers
what is visible there (`lookup`, `visible`, `isVisible`), whether a new name is free (`isFree`,
`freeName`) and whether a scope's names would clash if they were in scope elsewhere
(`collidesAcross`). Three static queries take a cursor and need no scope: `bindingOf` a declaration,
the bindings `declaredWithin` a tree, and those `referencedWithin` it from outside.

`Scope.Binding` is one declared name. It knows its `Kind` (local, parameter, member, inherited,
imported) and `Namespace` (variable, method, type), its `references()`, whether it
`isEffectivelyFinal()`, and whether it `canRenameTo` another name. Two bindings are equal when they
come from the same declaration, so a binding is a stable key where attribution is not.

`ScopeService` holds the rules: which trees open a scope, what each binds, where a binding is
visible, which namespace an identifier is read in, which words are reserved. A source file hands out
its language's service through `SourceFile.service(ScopeService.class)`, as it does for formatting.
The Java service implements the Java Language Specification's rules, including flow scoping of
pattern variables. The Kotlin, Scala and Groovy services extend it and override only where their
language differs, such as an implicit `it`, pattern variables in a `case`, or a script's variables.

`VariableAccess`, the existing trait for one use of a variable, is the other end of the relation. It
resolves to its `Scope.Binding`, and `Scope` asks it which uses are writes.

Bindings and references are computed once per scope and kept on the source file's cursor for the
traversal, keyed by tree identity. An edited copy of a tree is a different key, so it is computed
again.

## Consequences

Recipes get one answer to each question, and it is the same in every language that supplies a
service. Several recipes became more correct when they moved over, since the shared rules cover
cases their own did not.

A language without a service runs on Java's rules, which are wrong for it in places. Adding a `J`
language now includes writing its `ScopeService`.

The API answers for the tree its cursor holds. A visitor that renames or declares several names in
one pass sees none of its own edits, and keeps track of the names it has handed out.

Type names are bound only in part. A type of the same package, one brought in by an on-demand
import, and an inherited member type the file never names are not bound, because the type model
cannot list them cheaply. No type binding therefore does not mean a simple type name is free.

`isEffectivelyFinal` counts assignments per path through the tree. It is not a data-flow analysis.
An analysis that is one can filter the same streams of bindings and references, without `Scope`
knowing about it.

The API is marked incubating, so its shape can still change.

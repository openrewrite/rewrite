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
carried more copies. The copies disagreed, and most compared variables by type attribution, whose
`equals` and `TypeUtils.isOfType` take two locals of one name and type declared in sibling blocks of
a method for the same variable.

The JavaScript module already has a scope model that answers these questions from the tree alone.
Languages built on `J` (Kotlin, Scala, Groovy) share most of Java's rules and part from them in a
few places each.

## Decision

Add one API in `rewrite-java` that answers from the position of a name in the tree, and let each
language supply the rules.

`Scope` is what recipes use. `Scope.at(cursor)` is the innermost scope at a position, and answers
what is visible there (`lookup`, `visible`, `isVisible`), whether code there may refer to a binding
in reach (`canReference`), whether a new name is free (`isFree`, `freeName`) and whether the scope's
statements could stand elsewhere with every name meaning what it means now (`canMoveBefore`,
`canMoveAfter`). Static queries take a cursor and need no scope:
`bindingOf` a declaration, the bindings `declaredWithin` a tree, those `referencedWithin` it from
outside, whether an expression would read the same if evaluated earlier in its block
(`canEvaluateBefore`), and whether a lambda written around some code could capture what it reads and
assigns (`lambdaCanCapture`).

A recipe that moves code asks `canMoveBefore`, `canMoveAfter`, `canEvaluateBefore` or
`lambdaCanCapture`, since each takes several rules to answer and recipes that put the answer
together themselves left one out.
`cursorTo` gives the cursor of a tree the visitor has not reached, with every tree between, because
a cursor that leaves one out hides what that tree binds.

`Scope.Binding` is one declared name. It knows its `Kind` (local, parameter, member, inherited,
imported) and `Namespace` (variable, method, type), the tree that declares it, its `references()`,
whether it `isEffectivelyFinal()`, and whether it `canRenameTo` another name. Two bindings are equal
when they come from the same declaration, so a binding is a stable key where attribution is not.

`ScopeService` holds the rules: which trees open a scope, what each binds, where a binding is
visible, which namespace an identifier is read in, which trees are a static context, which words are
reserved. A source file hands out
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

A binding is one name in one scope. A field has a binding in its own class, another in each class
that inherits it, and none where it is selected from an instance of a class that is not around the
access. Across classes a field is identified by its `JavaType.Variable`.

Type names are bound only in part. A type of the same package, one brought in by an on-demand
import, and an inherited member type the file never names are not bound, because the type model
cannot list them cheaply. No type binding therefore does not mean a simple type name is free.

Kotlin's implicit `it` is bound by every lambda that declares no parameters. A lambda with a receiver or with
no parameter has no `it`, so inside one the name resolves to that lambda and not to the one around it.

`lambdaCanCapture` applies Java's rule in every language, so it says no for a capture that Kotlin,
Scala or Groovy would allow.

`canReference` asks each language what a static context is. It applies Java's rules for capturing a
local, as `lambdaCanCapture` does, for a forward reference to a field, and for an enum's static field
read while a constant is constructed, in every language. Groovy and Kotlin refuse such a read too,
though not in the same places as Java. Scala allows it, so there `canReference` says no for a read
the language accepts. A Kotlin `init` block is an instance initializer to these rules. In a Kotlin
enum, `canReference` refuses every member of the companion object while an entry is constructed and
in an entry's arguments, a `const val` and a function too, as Kotlin does. It allows one in an object
expression there, which Kotlin refuses. Kotlin allows a read in a lambda, as of a companion member in
`by lazy { }` or of a property declared after an `init` block, where `canReference` refuses it. An
enum constant's body binds none of the members it inherits from its enum, so `canReference` refuses a
field of the enum there that the body could read.

`canEvaluateBefore` looks at names and assignments only. It does not see a method called in between
that changes a field or an object the expression reads, nor what the expression itself changes.

`canMoveBefore` and `canMoveAfter` look for `this` only in code that leaves a class body. A Kotlin
lambda with a receiver gives `this` another meaning too, which they do not see.

`isEffectivelyFinal` counts assignments per path through the tree. It is not a data-flow analysis.
An analysis that is one can filter the same streams of bindings and references, without `Scope`
knowing about it.

The API is marked incubating, so its shape can still change.

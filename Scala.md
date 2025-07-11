# Scala Language Support Implementation Plan

This document tracks the implementation plan for adding Scala language support to OpenRewrite.

## Overview

This plan outlines the steps needed to implement a Scala parser and AST model that integrates with OpenRewrite's Lossless Semantic Tree (LST) framework.

### Architecture Approach

As a JVM-based language, Scala's LST implementation will:
- Define an `S` interface that extends from `J` (Java's LST interface)
- Reuse common JVM constructs from the J model (classes, methods, fields, etc.)
- Add Scala-specific constructs to the S interface (pattern matching, traits, implicits, etc.)
- Follow the established pattern used by Groovy (`G extends J`) and Kotlin (`K extends J`)

### Composition Pattern

When implementing Scala-specific LST elements, we will use composition of J elements rather than duplication. This ensures that Java-focused recipes can still operate on Scala code by accessing the composed J elements. For example:

- A Scala pattern match might compose a `J.Switch` internally
- Scala's `for` comprehension could compose `J.ForEachLoop` elements
- Implicit parameters might compose `J.VariableDeclarations`

This composition approach maximizes recipe reusability across all JVM languages.

## Implementation Phases

### Phase 1: Parser Implementation (First Priority)
- [x] Create `ScalaParser` class implementing `Parser` interface.
- [x] Integrate with Scala 3 compiler (dotty.tools.dotc)
- [x] Implement builder pattern with classpath/dependency management
- [x] Set up compiler configuration and error handling

The parser is the entry point and must be implemented first, following patterns from `GroovyParser` and `KotlinParser`.

### Phase 2: Parser Visitor Implementation (Second Priority)
- [x] Create `ScalaParserVisitor` implementing Scala compiler's AST visitor
- [x] Map Scala compiler AST elements to S/J LST model
- [ ] Handle all Scala language constructs (in progress)
- [x] Preserve formatting, comments, and type information
- [ ] Implement visitor methods for each Scala AST node type (in progress)

The Parser Visitor bridges the Scala compiler's internal AST with OpenRewrite's LST model.

### Phase 3: Visitor Infrastructure Skeleton
- [x] Create `ScalaVisitor` extending `JavaVisitor`
  - [x] Override `isAcceptable()` and `getLanguage()`
  - [x] Add skeleton visit methods for future S elements
- [x] Create `ScalaIsoVisitor` extending `JavaIsoVisitor`
  - [x] Override methods to provide type-safe transformations
- [x] Create `ScalaPrinter` extending `ScalaVisitor`
  - [x] Implement LST to source code conversion
  - [x] Create inner `ScalaJavaPrinter` for J elements
- [ ] Create supporting classes: `SSpace`, `SLeftPadded`, `SRightPadded`, `SContainer`
  - [ ] Define Location enums for Scala-specific formatting

This infrastructure must be in place before implementing LST elements.

### Phase 4: Testing Infrastructure
- [x] Create `Assertions.java` class with `scala()` methods
- [x] Implement parse-only overload for round-trip testing
- [x] Implement before/after overload for transformation testing
- [x] Configure ScalaParser with appropriate classpath
- [x] Create `org.openrewrite.scala.tree` test package

The Assertions class is the foundation for all Scala LST testing. Each LST element gets a test in `org.openrewrite.scala.tree` that uses `rewriteRun()` with `scala()` assertions to verify parse → print → parse idempotency.

### Phase 5: Core LST Infrastructure
- [x] Create `rewrite-scala` module structure
- [x] Define `S` interface extending `J`
- [x] Implement Scala-specific AST classes in S
- [x] Design composition strategy for Scala constructs using J elements
- [ ] Write unit tests for each LST element in tree package (in progress)

### Phase 6: Advanced Language Features
- [ ] Add type attribution support from compiler
- [ ] Handle Scala-specific features (implicits, traits, pattern matching, etc.)
- [ ] Implement formatting preservation for Scala syntax
- [ ] Support Scala 2 vs Scala 3 differences

### Phase 7: Testing & Validation
- [ ] Create comprehensive test suite beyond tree tests
- [ ] Implement Scala TCK (Technology Compatibility Kit)
- [ ] Validate LST round-trip accuracy
- [ ] Performance benchmarking

### Phase 8: Recipe Support
- [ ] Implement common Scala refactoring recipes
- [ ] Create Scala-specific visitor utilities
- [ ] Document recipe development patterns

## Technical Considerations

### Key Scala Features to Support
- Pattern matching
- Implicit conversions and parameters
- Traits and mixins
- Case classes and objects
- Higher-kinded types
- Macros (Scala 2 vs Scala 3)
- Extension methods
- Union and intersection types (Scala 3)

### Integration Points
- Compatibility with existing Java recipes where applicable
- Interoperability with mixed Java/Scala codebases
- Build tool integration (SBT, Maven, Gradle)

## LST Element Mapping Plan

When implementing the Scala LST model, we'll map elements progressively from simple to complex. This approach allows us to build a solid foundation and test each element thoroughly before moving to more complex constructs.

### Phase 1: Basic Literals and Identifiers
These are the atomic building blocks of any Scala program:

1. **S.Literal** (compose J.Literal)
   - Integer literals: `42`, `0xFF`
   - Long literals: `42L`
   - Float literals: `3.14f`
   - Double literals: `3.14`
   - Boolean literals: `true`, `false`
   - Character literals: `'a'`
   - String literals: `"hello"`
   - Multi-line strings: `"""hello""""`
   - Null literal: `null`
   - Symbol literals: `'symbol` (Scala 2)

2. **S.Identifier** (compose J.Identifier)
   - Simple identifiers: `x`, `value`
   - Backtick identifiers: `` `type` ``
   - Operator identifiers: `+`, `::`, `=>`

### Phase 2: Basic Expressions
Building on literals and identifiers:

3. **S.Assignment** (compose J.Assignment)
   - Simple assignment: `x = 5`
   - Compound assignment: `x += 1`

4. **S.Binary** (compose J.Binary)
   - Arithmetic: `a + b`, `x * y`
   - Comparison: `a > b`, `x == y`
   - Logical: `a && b`, `x || y`
   - Infix method calls: `list map func`

5. **S.Unary** (compose J.Unary)
   - Prefix: `!flag`, `-x`, `+y`
   - Postfix: `x!` (custom operators)

6. **S.Parentheses** (compose J.Parentheses)
   - Grouping: `(a + b) * c`

### Phase 3: Method Invocations and Access
7. **S.MethodInvocation** (compose J.MethodInvocation)
   - Standard calls: `obj.method(args)`
   - Operator calls: `a + b` (desugared to `a.+(b)`)
   - Apply method: `obj(args)`
   - Infix notation: `list map func`

8. **S.FieldAccess** (compose J.FieldAccess)
   - Simple access: `obj.field`
   - Chained access: `obj.inner.field`

### Phase 4: Collections and Sequences
9. **S.NewArray** (compose J.NewArray)
   - Array creation: `Array(1, 2, 3)`
   - Type annotations: `Array[Int](1, 2, 3)`

10. **S.CollectionLiteral** (new S-specific)
    - List literals: `List(1, 2, 3)`
    - Set literals: `Set(1, 2, 3)`
    - Map literals: `Map("a" -> 1, "b" -> 2)`
    - Tuples: `(1, "two", 3.0)`

### Phase 5: Type System Elements
11. **S.TypeReference** (compose J.ParameterizedType/J.Identifier)
    - Simple types: `Int`, `String`
    - Parameterized types: `List[Int]`
    - Compound types: `A with B`
    - Refined types: `{ def foo: Int }`
    - Higher-kinded types: `F[_]`

12. **S.TypeParameter** (compose J.TypeParameter)
    - Simple: `[T]`
    - Bounded: `[T <: Upper]`, `[T >: Lower]`
    - Context bounds: `[T: TypeClass]`
    - View bounds: `[T <% Viewable]` (Scala 2)

### Phase 6: Variable and Value Declarations
13. **S.VariableDeclarations** (compose J.VariableDeclarations)
    - Val declarations: `val x = 5`
    - Var declarations: `var y = 10`
    - Lazy vals: `lazy val z = compute()`
    - Pattern declarations: `val (a, b) = tuple`
    - Type annotations: `val x: Int = 5`

### Phase 7: Control Flow
14. **S.If** (compose J.If)
    - If expressions: `if (cond) expr1 else expr2`
    - If statements: `if (cond) doSomething()`

15. **S.WhileLoop** (compose J.WhileLoop)
    - While loops: `while (cond) { ... }`
    - Do-while loops: `do { ... } while (cond)`

16. **S.ForLoop** (new S-specific)
    - For comprehensions: `for (x <- list) yield x * 2`
    - Multiple generators: `for (x <- xs; y <- ys) yield (x, y)`
    - Guards: `for (x <- list if x > 0) yield x`
    - Definitions: `for (x <- list; y = x * 2) yield y`

17. **S.Match** (new S-specific, may compose J.Switch)
    - Pattern matching: `x match { case 1 => "one" case _ => "other" }`
    - Type patterns: `case x: String => x.length`
    - Constructor patterns: `case Person(name, age) => name`
    - Guards: `case x if x > 0 => "positive"`

### Phase 8: Function Definitions
18. **S.Lambda** (compose J.Lambda)
    - Simple lambdas: `x => x + 1`
    - Multi-parameter: `(x, y) => x + y`
    - Block lambdas: `x => { val y = x * 2; y + 1 }`
    - Placeholder syntax: `_ + 1`

19. **S.MethodDeclaration** (compose J.MethodDeclaration)
    - Def methods: `def foo(x: Int): Int = x + 1`
    - Generic methods: `def bar[T](x: T): T = x`
    - Multiple parameter lists: `def curry(x: Int)(y: Int): Int`
    - Implicit parameters: `def baz(x: Int)(implicit y: Int): Int`
    - Default parameters: `def qux(x: Int = 0): Int`

### Phase 9: Class and Object Definitions
20. **S.ClassDeclaration** (compose J.ClassDeclaration)
    - Classes: `class Foo(x: Int) { ... }`
    - Case classes: `case class Person(name: String, age: Int)`
    - Abstract classes: `abstract class Base { ... }`
    - Sealed classes: `sealed class Option[+T]`

21. **S.Trait** (new S-specific)
    - Traits: `trait Drawable { def draw(): Unit }`
    - Trait mixins: `class Circle extends Shape with Drawable`
    - Self types: `trait A { self: B => ... }`

22. **S.Object** (new S-specific)
    - Singleton objects: `object Util { ... }`
    - Companion objects: `object Person { ... }`
    - Case objects: `case object Empty`

### Phase 10: Advanced Scala Features
23. **S.Import** (compose J.Import)
    - Simple imports: `import scala.collection.mutable`
    - Wildcard imports: `import scala.collection._`
    - Selective imports: `import scala.collection.{List, Set}`
    - Renaming imports: `import java.util.{List => JList}`

24. **S.Package** (compose J.Package)
    - Package declarations: `package com.example`
    - Package objects: `package object utils { ... }`

25. **S.Implicit** (new S-specific)
    - Implicit vals: `implicit val ord: Ordering[Int]`
    - Implicit defs: `implicit def strToInt(s: String): Int`
    - Implicit classes: `implicit class RichInt(x: Int) { ... }`

26. **S.Given** (new S-specific, Scala 3)
    - Given instances: `given Ordering[Int] = ...`
    - Using clauses: `def sort[T](list: List[T])(using Ordering[T])`
    - Extension methods: `extension (x: Int) def times(f: => Unit): Unit`

### Testing Strategy
Each LST element will have comprehensive tests in `org.openrewrite.scala.tree`:
- Parse-only tests to verify round-trip accuracy
- Tests for all syntax variations
- Tests for formatting preservation
- Tests for type attribution (when available)

### Implementation Notes
- Start with Phase 1 and complete all testing before moving to Phase 2
- Each element should preserve all original formatting and comments
- Use composition of J elements wherever possible for recipe compatibility
- Document any Scala-specific formatting in Location enums
- Consider Scala 2 vs Scala 3 syntax differences in each element

## Implementation Progress

### Current Status (As of Jul 11, 2025)

We have successfully completed the foundational infrastructure and are making good progress on LST element implementation:

#### Completed LST Elements ✅
1. **Literals** (13/13 tests passing) - All literal types including strings, numbers, booleans, characters, null, and symbols
2. **Identifiers** (8/8 tests passing) - Simple, backtick, and operator identifiers  
3. **Assignments** (8/8 tests passing) - Simple, compound, field, array, and tuple destructuring assignments
4. **Parentheses** (10/10 tests passing) - Expression grouping and nesting
5. **Variable Declarations** (12/12 tests passing) - val, var, lazy val, with modifiers and type annotations
   - Note: Currently preserved as Unknown nodes for formatting accuracy

#### In Progress 🟨
1. **Binary Operations** (19/20 tests passing) - One failing test for infix method calls with dots
2. **Unary Operations** (5/7 tests passing) - Issues with postfix operators and prefix method calls causing expression duplication

#### Not Started Yet ❌
1. **Compilation Units** (4/9 tests passing) - Need to implement proper package/import handling
2. Method invocations, control flow, classes, traits, objects, pattern matching, etc.

### Key Technical Decisions Made
- Using Unknown nodes to preserve formatting for unimplemented constructs
- Wrapping bare expressions in object wrappers for valid Scala syntax
- Updated assignment tests to use object blocks since Scala doesn't allow top-level assignments
- Implemented multi-line detection in isSimpleExpression to avoid inappropriate wrapping

### Next Steps
1. Fix expression duplication issues in binary/unary operations
2. Implement proper compilation unit structure (packages/imports)
3. Continue with method invocations and control flow constructs

## Notes

This plan will evolve as we progress through the implementation.
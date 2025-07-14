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

### Current Status (As of Jul 14, 2025)

We have successfully completed the foundational infrastructure and are making excellent progress on LST element implementation. Currently at **92.8% test passing rate (211/227 tests)**.

#### Completed LST Elements ✅
These elements are fully mapped to J model classes without J.Unknown:
1. **Literals** (13/13 tests passing) - Maps to J.Literal
2. **Identifiers** (8/8 tests passing) - Maps to J.Identifier  
3. **Assignments** (7/8 tests passing) - Maps to J.Assignment and J.AssignmentOperation
   - ✅ Simple assignment: `x = 5` - Maps to J.Assignment
   - ✅ Compound assignments: `x += 5` - Maps to J.AssignmentOperation
   - ❌ Tuple destructuring: `(a, b) = (3, 4)` - Parse error (needs special handling)
4. **Binary Operations** (20/20 tests passing) - Maps to J.Binary
5. **Field Access** (8/8 tests passing) - Maps to J.FieldAccess
6. **Method Invocations** (12/12 tests passing) - Maps to J.MethodInvocation
7. **Control Flow - If** (partial) - Maps to J.If
8. **Control Flow - While** (partial) - Maps to J.WhileLoop
9. **Control Flow - Block** (partial) - Maps to J.Block
10. **Classes** (partial) - Maps to J.ClassDeclaration
11. **Objects** (partial) - Maps to J.ClassDeclaration with SObject marker
12. **New Class** (6/6 tests passing) - Maps to J.NewClass
13. **Return Statements** (6/6 tests passing) - Maps to J.Return
14. **Throw Statements** (8/8 tests passing) - Maps to J.Throw
15. **Parameterized Types** (8/10 tests passing) - Maps to J.ParameterizedType
16. **Compilation Units** (12/12 tests passing) - Maps to S.CompilationUnit

#### Using J.Unknown (Need Proper Mapping) ⚠️
These elements have passing tests but rely on J.Unknown:
1. **Variable Declarations** (12/12 tests passing)
   - Currently preserved as Unknown nodes - needs J.VariableDeclarations mapping
2. **Unary Operations** (7/7 tests passing)
   - Currently preserved as Unknown nodes - needs J.Unary mapping
3. **Parentheses** (10/10 tests passing)
   - Currently preserved as Unknown nodes - needs J.Parentheses mapping
4. **Imports** (8/8 tests passing)
   - Currently preserved as Unknown nodes - needs J.Import mapping
5. **Type Cast** (7/8 tests passing)
   - Currently preserved as Unknown nodes - needs J.TypeCast mapping
6. **Try-Catch-Finally** (8/8 tests passing)
   - Currently preserved as Unknown nodes - needs J.Try mapping
7. **For Comprehensions** (part of control flow tests)
   - Preserved as Unknown with ScalaForLoop marker - complex Scala-specific syntax

#### Not Started Yet ❌
1. Traits, pattern matching, J.ArrayAccess, J.Lambda, etc.

### Important Implementation Principles

#### J.Unknown Usage Policy
- **J.Unknown is NOT progress**: Having passing tests with J.Unknown nodes is not considered a completed implementation
- **Partial mappings are acceptable**: For complex Scala-specific constructs (like for-comprehensions), it's okay to map parts to J model and preserve complex parts as J.Unknown with markers
- **Completion criteria**: An LST element is only considered "done" when it has no J.Unknown nodes in the mapping (except for documented edge cases)
- **Incremental approach**: Start with J.Unknown to get tests passing, then replace with proper J mappings

#### Current Priority
Replace existing J.Unknown implementations with proper J model mappings:
1. J.VariableDeclarations for val/var declarations
2. J.Unary for unary operations
3. J.Parentheses for parenthesized expressions
4. J.Import for import statements
5. J.TypeCast for asInstanceOf operations
6. J.Try for try-catch-finally blocks

### Key Technical Decisions Made
- Using Unknown nodes to preserve formatting for unimplemented constructs (temporary)
- Wrapping bare expressions in object wrappers for valid Scala syntax
- Updated assignment tests to use object blocks since Scala doesn't allow top-level assignments
- Implemented multi-line detection in isSimpleExpression to avoid inappropriate wrapping
- Fixed expression duplication by excluding postfix operators from wrapping and handling unary operators in Select nodes
- Fixed comment handling by updating Space.format to properly extract comments from whitespace
- Fixed infixWithDot issue by preserving parentheses as Unknown nodes
- Fixed package duplication by properly updating cursor position after package declaration
- Decided to keep imports as Unknown nodes for now after encountering double printing issues with J.Import

### Incremental Implementation Lessons Learned
When attempting to implement import mapping to J.Import, we encountered issues with imports being processed twice (once as J.Import and once as J.Unknown), resulting in double printing. Investigation revealed:

1. **Multiple visitor calls**: The Scala compiler's AST structure for imports causes the visitor to be called multiple times for the same import statement
2. **Incomplete field access**: When parsing `import scala.collection.mutable`, only `scala.collection` was being captured in the J.Import
3. **Cursor management complexity**: Managing the cursor position to prevent duplicate source consumption proved challenging
4. **Debug findings**:
   - Import expression was a Select node with name "collection" and qualifier "scala"
   - The full path "mutable" was not being captured in the field access construction
   - Both J.Import and J.Unknown were being added, causing double printing

This reinforced the importance of:
1. Understanding the compiler's AST structure thoroughly before implementation
2. Starting with simple cases that clearly map to existing LST elements
3. Using J.Unknown for complex cases to preserve formatting while keeping tests passing
4. Adding support gradually as patterns emerge and issues are understood
5. Not trying to handle all variations at once

**Future approach**: Now that Select nodes map to J.FieldAccess, we need to:
1. Resolve the cursor management issues preventing proper source consumption
2. Fix the multiple visitor calls for the same import statement
3. Ensure the J.Import properly captures the complete field access without duplication

### Next Steps
1. Implement classes, traits, and objects
2. Add pattern matching support
3. Circle back to imports once we better understand the cursor management patterns
4. Eventually create S.Import for Scala-specific import syntax (multi-select, aliases)
5. Create S.ForComprehension for Scala's complex for loops with generators and guards

## Prioritized Implementation List (Easiest to Hardest)

Based on analysis of available J model classes and Scala language constructs, here's the prioritized implementation order:

### Easy Wins (Map directly to existing J model)
1. **J.Assignment** ✅ - Simple variable reassignment: `x = 5` (Implemented)
   - Simple assignments come through as `Assign` nodes
2. **J.AssignmentOperation** ✅ - Compound assignments: `x += 5` (Implemented)
   - Compound assignments come through as `InfixOp` nodes with operators ending in `=`
3. **J.NewClass** ✅ - Object instantiation: `new MyClass(args)` (Implemented)
   - `new` expressions come through as `New` nodes
   - Constructor calls with arguments come through as `Apply(New(...), args)`
4. **J.Return** ✅ - Return statements in methods: `return value` (Implemented)
   - Return statements come through as `Return` nodes
   - Handles both void returns (`return`) and value returns (`return expr`)
5. **J.Throw** ✅ - Exception throwing: `throw new Exception("error")` (Implemented)
   - Throw statements come through as `Throw` nodes
   - Handles any expression that evaluates to a Throwable
6. **J.ParameterizedType** ✅ (8/10 tests) - Generic types: `List[String]`, `Map[K, V]`
   - Implemented in `visitAppliedTypeTree` method
   - 8/10 tests passing - basic parameterized types work
   - TODO: Fix trait handling and variance annotations (+T, -T)

### Moderate Complexity (Straightforward mapping with some nuances)
7. **J.TypeCast** - Type casting: `x.asInstanceOf[String]`
8. **J.InstanceOf** - Type checking: `x.isInstanceOf[String]`
9. **J.Try** - Try-catch-finally blocks
10. **J.ArrayAccess** - Array/collection indexing: `arr(0)`

### Higher Complexity (Requires careful handling)
11. **J.Lambda** - Function literals: `(x: Int) => x + 1`
12. **J.Annotation** - Annotations: `@deprecated`, `@tailrec`
13. **J.MemberReference** - Method references: `List.apply _`
14. **J.NewArray** - Array creation: `Array(1, 2, 3)`
15. **J.Ternary** - Inline if-else expressions (less common in Scala)

### Complex Scala-Specific (May need custom S types)
16. **Pattern Matching** - Requires J.Switch/Case or custom S.Match
17. **For Comprehensions** - Complex desugaring to map/flatMap
18. **Implicit Parameters** - Scala 2 implicits
19. **Given/Using** - Scala 3 contextual abstractions
20. **Extension Methods** - Scala 3 extension syntax

## Important Design Decisions

### LST Model Language Choice (Java vs Scala)

During implementation, we made a critical decision to implement the LST model classes in Java rather than Scala, following the established pattern used by Kotlin (K.java) and Groovy (G.java).

#### Initial Approach
We initially implemented S.scala and S.CompilationUnit in Scala, thinking it would be more idiomatic for Scala support.

#### Issues Encountered
1. **Non-idiomatic Scala code**: The LST pattern requires many getters, setters, and wither methods that look unnatural in Scala
2. **Lombok-style patterns**: The immutability pattern with `@With` annotations and builder methods is Java-centric
3. **Cross-language complexity**: Mixed Java/Scala compilation added unnecessary complexity

#### Final Decision: Move to Java
We migrated S interface and S.CompilationUnit to Java for the following reasons:

1. **Consistency**: Follows the proven pattern of K.java (Kotlin) and G.java (Groovy)
2. **Simplicity**: Avoids mixed-language compilation issues
3. **Lombok support**: Can use `@RequiredArgsConstructor`, `@With`, `@Getter` for cleaner code
4. **Cross-language compatibility**: Java beans work well from both Java and Scala

#### Key Implementation Details
- Used `@RequiredArgsConstructor` to generate constructor with all final fields
- Maintained `@Nullable` annotations instead of Scala Options for cross-language compatibility  
- Used JRightPadded for lists to preserve formatting
- Followed exact field ordering from K.CompilationUnit as a template

#### Benefits of This Approach
1. **Java developers** get familiar Java code with standard patterns
2. **Scala developers** can still use the classes idiomatically through `@BeanProperty` and implicit conversions
3. **Performance** is optimal with no wrapper overhead
4. **Maintainability** is improved by following established patterns

This decision reinforces that the LST model is language-agnostic infrastructure that should be implemented in Java, while language-specific visitor logic can still be implemented in the target language where it makes sense.

## Notes

This plan will evolve as we progress through the implementation.
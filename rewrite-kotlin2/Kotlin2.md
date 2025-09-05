# Kotlin 2 Language Support Implementation Plan

This document tracks the implementation plan for adding Kotlin 2 language support to OpenRewrite.

## K2 Compiler Architecture Changes Summary

### Why KotlinTreeParserVisitor Needs Major Refactoring

The Kotlin 2.0 release introduced the **K2 compiler** with a completely rewritten frontend that fundamentally changes how Kotlin code is parsed and analyzed. This necessitates significant refactoring of OpenRewrite's `KotlinTreeParserVisitor`.

#### Key Architectural Shifts:

1. **From PSI+BindingContext to FIR**
   - **K1 (Old)**: Used PSI trees with a separate BindingContext (huge hash maps) to store semantic information
   - **K2 (New)**: Uses FIR (Frontend Intermediate Representation) with semantic information embedded directly in the tree structure
   - **Impact**: KotlinTreeParserVisitor can no longer rely on BindingContext lookups; must interact with FIR APIs

2. **New Compilation Pipeline**
   - **K1**: PSI → Resolution → BindingContext → Backend
   - **K2**: PSI → RawFIR → Multiple FIR Resolution Phases → Resolved FIR → Backend
   - **Impact**: Type resolution and semantic analysis happen through FIR transformations, not descriptor-based APIs

3. **Performance Improvements**
   - Up to **94% faster compilation** (e.g., Anki-Android: 57.7s → 29.7s)
   - Achieved through unified data structure, better cache utilization, elimination of redundant lookups
   - Tree traversal is more efficient than hash map lookups in BindingContext

4. **API Changes**
   - Old descriptor-based APIs deprecated
   - New FIR extension points: `FirDeclarationGenerationExtension`, `FirStatusTransformerExtension`, etc.
   - Compiler plugins must be rewritten using new FIR Plugin API

5. **What This Means for OpenRewrite**
   - Existing `KotlinTreeParserVisitor` extends `KtVisitor<J, ExecutionContext>` and uses K1 APIs
   - Must create new visitor that works with FIR representations
   - Need to map FIR elements to OpenRewrite's LST model instead of PSI+BindingContext
   - Type attribution must come from FIR's resolved phases, not BindingContext

## Overview

This plan outlines the steps needed to implement a Kotlin 2 parser and AST model that integrates with OpenRewrite's Lossless Semantic Tree (LST) framework, accounting for the fundamental architectural changes in the K2 compiler.

### Architecture Approach

As a JVM-based language, Kotlin 2's LST implementation will:
- Define a `K2` interface that extends from `J` (Java's LST interface)
- Reuse common JVM constructs from the J model (classes, methods, fields, etc.)
- Leverage the existing `K` interface patterns for consistency
- Add Kotlin 2-specific constructs to the K2 interface (context receivers, value classes, etc.)
- Follow the established pattern used by existing Kotlin (`K extends J`), Groovy (`G extends J`), and Scala (`S extends J`)

### Composition Pattern

When implementing Kotlin 2-specific LST elements, we will use composition of J elements rather than duplication. This ensures that Java-focused recipes can still operate on Kotlin 2 code by accessing the composed J elements. For example:

- Kotlin 2's context receivers might compose `J.MethodDeclaration` parameters
- Value classes could compose `J.ClassDeclaration` with special markers
- Sealed interfaces might compose `J.ClassDeclaration` with sealed modifiers

This composition approach maximizes recipe reusability across all JVM languages.

## Implementation Phases

### Phase 1: Parser Implementation (First Priority)
- [ ] Create `Kotlin2Parser` class implementing `Parser` interface
- [ ] Integrate with Kotlin 2 compiler toolchain (K2 frontend)
- [ ] Implement builder pattern with classpath/dependency management
- [ ] Set up compiler configuration and error handling
- [ ] Support both Kotlin 1.x and 2.x language features

The parser is the entry point and must be implemented first, following patterns from `KotlinParser`, `GroovyParser`, and `ScalaParser`.

### Phase 2: Parser Visitor Implementation (Second Priority)
- [ ] Create `Kotlin2ParserVisitor` implementing Kotlin 2 compiler's AST visitor
- [ ] Map Kotlin 2 compiler AST elements to K2/J LST model
- [ ] Handle all Kotlin 2 language constructs
- [ ] Preserve formatting, comments, and type information
- [ ] Implement visitor methods for each Kotlin 2 AST node type
- [ ] Support backward compatibility with Kotlin 1.x features

The Parser Visitor bridges the Kotlin 2 compiler's internal AST with OpenRewrite's LST model.

### Phase 3: Visitor Infrastructure Skeleton
- [ ] Create `Kotlin2Visitor` extending `JavaVisitor`
    - [ ] Override `isAcceptable()` and `getLanguage()`
    - [ ] Add skeleton visit methods for future K2 elements
- [ ] Create `Kotlin2IsoVisitor` extending `JavaIsoVisitor`
    - [ ] Override methods to provide type-safe transformations
- [ ] Create `Kotlin2Printer` extending `Kotlin2Visitor`
    - [ ] Implement LST to source code conversion
    - [ ] Create inner `Kotlin2JavaPrinter` for J elements
- [ ] Create supporting classes: `K2Space`, `K2LeftPadded`, `K2RightPadded`, `K2Container`
    - [ ] Define Location enums for Kotlin 2-specific formatting

This infrastructure must be in place before implementing LST elements.

### Phase 4: Testing Infrastructure
- [ ] Create `Assertions.java` class with `kotlin2()` methods
- [ ] Implement parse-only overload for round-trip testing
- [ ] Implement before/after overload for transformation testing
- [ ] Configure Kotlin2Parser with appropriate classpath
- [ ] Create `org.openrewrite.kotlin2.tree` test package

The Assertions class is the foundation for all Kotlin 2 LST testing. Each LST element gets a test in `org.openrewrite.kotlin2.tree` that uses `rewriteRun()` with `kotlin2()` assertions to verify parse → print → parse idempotency.

### Phase 5: Core LST Infrastructure
- [ ] Create `rewrite-kotlin2` module structure
- [ ] Define `K2` interface extending `J`
- [ ] Implement Kotlin 2-specific AST classes in K2
- [ ] Design composition strategy for Kotlin 2 constructs using J elements
- [ ] Write unit tests for each LST element in tree package

### Phase 6: Advanced Kotlin 2 Features
- [ ] Add type attribution support from K2 compiler
- [ ] Handle Kotlin 2-specific features (context receivers, value classes, sealed interfaces, etc.)
- [ ] Implement formatting preservation for Kotlin 2 syntax
- [ ] Support migration paths from Kotlin 1.x to 2.x

### Phase 7: Testing & Validation
- [ ] Create comprehensive test suite beyond tree tests
- [ ] Implement Kotlin 2 TCK (Technology Compatibility Kit)
- [ ] Validate LST round-trip accuracy
- [ ] Performance benchmarking against existing KotlinParser

### Phase 8: Recipe Support
- [ ] Implement common Kotlin 2 refactoring recipes
- [ ] Create Kotlin 2-specific visitor utilities
- [ ] Document recipe development patterns
- [ ] Migration recipes from Kotlin 1.x to 2.x

## Technical Considerations

### Key Kotlin 2 Features to Support
- **Context receivers**: New language feature for contextual programming
- **Value classes**: Inline classes with improved semantics
- **Sealed interfaces**: Extension of sealed classes concept
- **Definitely non-nullable types**: Enhanced type system
- **Smart casts improvements**: Better flow analysis
- **Opt-in requirement for builder inference**: Enhanced type inference
- **Underscore operator for type arguments**: Improved generic syntax
- **Callable references improvements**: Enhanced functional programming

### Integration Points
- **Backward compatibility** with existing Kotlin recipes where applicable
- **Interoperability** with mixed Kotlin 1.x/2.x codebases
- **Build tool integration** (Gradle Kotlin DSL, Maven)
- **IDE support** for language server features

## LST Element Mapping Plan

The Kotlin 2 LST model will largely reuse the existing K interface patterns while adding K2-specific extensions. We'll map elements progressively from simple to complex.

### Phase 1: Reuse Existing K Elements (80% compatibility)
Most existing Kotlin 1.x constructs will work directly:

1. **K2.Literal** (reuse K.Literal)
    - All existing literal types from Kotlin 1.x
    - Enhanced string templates in Kotlin 2

2. **K2.Identifier** (reuse K.Identifier)
    - Standard identifiers with enhanced Unicode support
    - Context receiver names

3. **K2.Binary/Unary/Assignment** (reuse K.Binary, K.Unary, K.Assignment)
    - All existing operators continue to work
    - Enhanced smart cast support

### Phase 2: Enhanced Existing Elements
4. **K2.ClassDeclaration** (extend K.ClassDeclaration)
    - Add support for sealed interfaces
    - Enhanced value class semantics
    - Context receiver parameters

5. **K2.MethodDeclaration** (extend K.MethodDeclaration)
    - Context receiver parameters
    - Enhanced suspend function support
    - Improved builder inference

### Phase 3: New Kotlin 2 Elements
6. **K2.ContextReceiver** (new K2-specific)
    - Context receiver declarations: `context(Context)`
    - Context receiver usage in method signatures
    - Context receiver constraints

7. **K2.ValueClass** (new K2-specific, compose K.ClassDeclaration)
    - Value class declarations with improved semantics
    - Inline class compatibility
    - Boxing/unboxing optimizations

8. **K2.SealedInterface** (new K2-specific, compose J.ClassDeclaration)
    - Sealed interface declarations
    - Implementation tracking
    - Pattern matching improvements

### Phase 4: Advanced Type System
9. **K2.DefinitelyNonNullableType** (new K2-specific)
    - Type annotations for definitely non-nullable types
    - Integration with smart casts
    - Null safety improvements

10. **K2.UnderscoreTypeArgument** (new K2-specific)
    - Underscore operator for type arguments: `List<_>`
    - Type inference improvements
    - Generic variance handling

### Testing Strategy
Each LST element will have comprehensive tests in `org.openrewrite.kotlin2.tree`:
- Parse-only tests to verify round-trip accuracy
- Tests for all syntax variations
- Tests for formatting preservation
- Tests for type attribution (when available)
- Migration tests from Kotlin 1.x equivalent constructs

### Implementation Notes
- Start with Phase 1 (reuse) and complete all testing before moving to Phase 2
- Each element should preserve all original formatting and comments
- Use composition of J/K elements wherever possible for recipe compatibility
- Document any Kotlin 2-specific formatting in Location enums
- Maintain backward compatibility with existing Kotlin 1.x recipes

## Implementation Progress

### Current Status (As of [Date])
[To be updated as work progresses]

### Completed Infrastructure ✅
[To be filled in as components are completed]

### In Progress 🚧
[Current work items]

### Known Issues 🐛
[Issues discovered during implementation]

## Key Technical Decisions

### Reuse vs. Extension Strategy
- **Reuse existing K elements** where Kotlin 2 doesn't introduce breaking changes
- **Extend K elements** for enhanced features that maintain backward compatibility
- **Create new K2 elements** only for genuinely new Kotlin 2 language constructs
- **Maintain composition pattern** to ensure Java recipe compatibility

### Compiler Integration Strategy
- **Leverage K2 frontend** for improved type inference and analysis
- **Maintain K1 compatibility** during transition period
- **Support mixed codebases** with both Kotlin 1.x and 2.x files
- **Progressive migration path** from existing KotlinParser

### Performance Considerations
- **Shared type cache** between K and K2 parsers
- **Incremental compilation** support where possible
- **Memory efficiency** through element reuse
- **Parallel processing** of mixed Kotlin versions

## Prioritized Implementation List (Easiest to Hardest)

### Easy Wins (Direct reuse from K)
1. **K2.Literal** ✅ (Reuse K.Literal)
2. **K2.Identifier** ✅ (Reuse K.Identifier)
3. **K2.Binary/Unary** ✅ (Reuse K.Binary/K.Unary)
4. **K2.MethodInvocation** ✅ (Reuse K.MethodInvocation)

### Moderate Complexity (Extensions of K)
5. **K2.ClassDeclaration** (Extend K.ClassDeclaration for sealed interfaces)
6. **K2.MethodDeclaration** (Extend K.MethodDeclaration for context receivers)
7. **K2.Property** (Extend K.Property for enhanced semantics)

### Higher Complexity (New K2 elements)
8. **K2.ContextReceiver** (New language feature)
9. **K2.ValueClass** (Enhanced value semantics)
10. **K2.SealedInterface** (New sealed construct)

### Complex Type System Features
11. **K2.DefinitelyNonNullableType** (Advanced type system)
12. **K2.UnderscoreTypeArgument** (Type inference improvements)
13. **K2.SmartCastExpression** (Enhanced flow analysis)

## Migration Strategy

### From Existing KotlinParser
- **Gradual migration**: Support both parsers during transition
- **Feature parity**: Ensure K2 parser supports all K parser features
- **Recipe compatibility**: Existing recipes continue to work
- **Performance testing**: Validate K2 parser performance

### User Migration Path
- **Opt-in basis**: Users can choose when to migrate to K2 parser
- **Clear documentation**: Migration guide with benefits and considerations
- **Fallback support**: Ability to use K1 parser for unsupported features
- **Validation tools**: Recipes to validate successful migration

## Notes

This plan will evolve as we progress through the implementation. The key insight from the Scala implementation is that starting with proper infrastructure (parser, visitor, testing) and then incrementally building LST elements leads to the most robust implementation.

The major advantage of Kotlin 2 support is that we can reuse most of the existing Kotlin 1.x infrastructure, making this implementation significantly faster than starting from scratch.
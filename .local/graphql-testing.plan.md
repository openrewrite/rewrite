# GraphQL Module Testing Plan

## Review Summary

### Completed Tasks
1. ✅ Analyzed existing test coverage in rewrite-graphql module
2. ✅ Created comprehensive test plan identifying gaps
3. ✅ Implemented edge cases test suite (GraphQlParserEdgeCasesTest.java)
4. ✅ Implemented complex features test suite (GraphQlParserComplexFeaturesTest.java)
5. ✅ Implemented schema extensions test suite (GraphQlParserExtensionsTest.java)

### Test Files Created
- `/src/test/java/org/openrewrite/graphql/GraphQlParserEdgeCasesTest.java` - 29 tests covering edge cases
- `/src/test/java/org/openrewrite/graphql/GraphQlParserComplexFeaturesTest.java` - 15 tests covering advanced features
- `/src/test/java/org/openrewrite/graphql/GraphQlParserExtensionsTest.java` - 14 tests covering schema extensions

### Testing Improvements Added
1. **Edge Cases Coverage**:
   - Empty documents and whitespace handling
   - Unicode and escaped characters
   - Deeply nested structures
   - Boundary value testing
   - Special formatting cases

2. **Complex Features Coverage**:
   - Multiple operations in single document
   - Complex variable defaults
   - Fragment variables
   - Type conditions on unions/interfaces
   - Introspection queries
   - Relay connection patterns
   - Apollo Federation directives

3. **Extensions Coverage**:
   - All GraphQL extension types (schema, type, interface, union, enum, input)
   - Multiple extensions of same type
   - Extension ordering and dependencies
   - Repeatable directives
   - All directive locations

### Note on Test Execution
The build system requires Java 11 which is not currently available in the environment. To run these tests:
1. Install Java 11 using your package manager or SDKMAN
2. Run: `./gradlew :rewrite-graphql:test`

# GraphQL Module Testing Plan

## Current Test Coverage Analysis

### Well-Covered Areas
1. **Basic Operations**: Queries, mutations, subscriptions
2. **Type Definitions**: Objects, interfaces, unions, enums, inputs, scalars
3. **Schema Features**: Schema definitions, directives, fragments
4. **Value Types**: All GraphQL value types (string, int, float, boolean, null, enum, list, object)
5. **Advanced Features**: Variables, aliases, descriptions, comments, nested types
6. **Recipe Testing**: FindTypeUsages recipe has comprehensive tests

### Missing Test Coverage

#### 1. Edge Cases and Error Handling
- [ ] Empty GraphQL documents
- [ ] Invalid syntax (parser error recovery)
- [ ] Deeply nested selection sets
- [ ] Circular fragment references
- [ ] Very large documents (performance tests)
- [ ] Unicode and special characters in strings
- [ ] Escaped characters in strings

#### 2. Complex Query Features
- [ ] Variable default values with complex types
- [ ] Multiple operations in single document
- [ ] Anonymous operations alongside named ones
- [ ] Fragment variables (experimental feature)
- [ ] Type conditions on unions/interfaces

#### 3. Schema Extension Features
- [ ] Schema extensions (`extend schema`)
- [ ] Type extensions (`extend type`, `extend interface`, etc.)
- [ ] Multiple directive applications (repeatable directives)
- [ ] Directive on all possible locations

#### 4. Formatting and Whitespace
- [ ] Preservation of unusual formatting
- [ ] Comments in various positions
- [ ] Multi-line strings with indentation
- [ ] Trailing commas handling
- [ ] Mixed line endings (CRLF vs LF)

#### 5. Parser Robustness
- [ ] Recovery from syntax errors
- [ ] Partial parsing of invalid documents
- [ ] Memory efficiency with large files
- [ ] Performance benchmarks

#### 6. Integration Tests
- [ ] Multiple GraphQL files in same project
- [ ] Cross-file type references
- [ ] Visitor pattern edge cases
- [ ] Recipe composition

#### 7. Real-World Scenarios
- [ ] GitHub GraphQL API schema parsing
- [ ] Apollo Federation directives
- [ ] Relay-style pagination
- [ ] Custom scalar parsing
- [ ] Introspection queries

## Implementation Tasks

### Phase 1: Core Functionality Tests
1. **Edge Cases Test Suite** (`GraphQlParserEdgeCasesTest.java`)
   - Empty documents
   - Unicode handling
   - Escaped characters
   - Invalid syntax recovery

2. **Complex Features Test Suite** (`GraphQlParserComplexFeaturesTest.java`)
   - Multiple operations
   - Variable defaults
   - Fragment variables
   - Type conditions

3. **Schema Extensions Test Suite** (`GraphQlParserExtensionsTest.java`)
   - All extension types
   - Repeatable directives
   - Directive locations

### Phase 2: Quality and Performance Tests
4. **Formatting Test Suite** (`GraphQlFormattingTest.java`)
   - Whitespace preservation
   - Comment positioning
   - Line ending handling

5. **Performance Test Suite** (`GraphQlParserPerformanceTest.java`)
   - Large file parsing
   - Memory usage
   - Benchmark comparisons

### Phase 3: Integration and Real-World Tests
6. **Integration Test Suite** (`GraphQlIntegrationTest.java`)
   - Multi-file projects
   - Visitor combinations
   - Recipe interactions

7. **Real-World Examples** (`GraphQlRealWorldTest.java`)
   - Popular GraphQL schemas
   - Common patterns
   - Framework-specific features

## Success Metrics
- All tests pass consistently
- No memory leaks or performance regressions
- Parser handles invalid input gracefully
- Formatting is preserved correctly
- Real-world schemas parse successfully

## Files to Create/Modify
1. `/src/test/java/org/openrewrite/graphql/GraphQlParserEdgeCasesTest.java` (new)
2. `/src/test/java/org/openrewrite/graphql/GraphQlParserComplexFeaturesTest.java` (new)
3. `/src/test/java/org/openrewrite/graphql/GraphQlParserExtensionsTest.java` (new)
4. `/src/test/java/org/openrewrite/graphql/GraphQlFormattingTest.java` (new)
5. `/src/test/java/org/openrewrite/graphql/GraphQlParserPerformanceTest.java` (new)
6. `/src/test/java/org/openrewrite/graphql/GraphQlIntegrationTest.java` (new)
7. `/src/test/java/org/openrewrite/graphql/GraphQlRealWorldTest.java` (new)
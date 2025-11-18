# 7. JavaScript templating engine enhancements

Date: 2025-11-02

## Status

Proposed

## Context

The JavaScript templating engine provides pattern matching and template replacement capabilities for OpenRewrite JavaScript recipes. It enables recipes to match code patterns and generate replacement code through a declarative API using template literals.

Current limitations:
- Captures match any node structurally, with no way to add runtime constraints
- No convenient way to extract properties from captured nodes
- Configuration option `imports` is misleadingly named (accepts any declarations)
- No support for dynamic code generation based on match results
- Template literals only - no programmatic construction API
- Strict type matching prevents patterns from working without full type attribution
- Using `capture()` in templates without patterns is semantically confusing
- No way to match variable-length sequences (e.g., "any statements between these two statements")

This ADR proposes nine enhancements to address these limitations and make the templating engine more expressive and flexible.

### Summary of Enhancements

| # | Feature                                      | Status         |
|---|----------------------------------------------|----------------|
| 1 | Property Access on Captures                  | ✅ Implemented |
| 2 | Capture Constraints                          | ✅ Implemented |
| 3 | Rename `imports` to `context`                | ✅ Implemented |
| 4 | Pre-parsing Code Interpolation with `code()` | 📝 Proposed    |
| 5 | Builder API for Dynamic Construction         | ✅ Implemented |
| 6 | Lenient Type Matching in Comparator          | ✅ Implemented |
| 7 | `param()` for Template-Only Parameters       | ✅ Implemented |
| 8 | Variadic Captures for Sequence Matching      | ✅ Implemented |
| 9 | `any()` for Non-Capturing Pattern Matching   | 📝 Proposed    |

## Decision

We will implement the following enhancements to the JavaScript templating engine:

### 1. Property Access on Captures

**Status**: ✅ Implemented

Allow accessing properties of captured nodes directly in templates using natural JavaScript syntax, eliminating the need to manually extract properties in visitor code.

**API:**

```typescript
const method = capture<J.MethodInvocation>('method');
const pat = pattern`foo(${method})`;

// Access properties in templates
const tmpl = template`bar(${method.name})`;

// Deep property access and array indexing
const firstArg = method.arguments.elements[0].element;
const tmpl2 = template`baz(${firstArg})`;
```

**Rationale:**
- Reduces boilerplate in visitor methods for extracting node properties
- Provides natural JavaScript syntax (same as accessing properties on any object)
- Type-safe with TypeScript generics providing IDE autocomplete
- Supports complex navigation patterns (deep chaining, array access)

**Implementation:**
- `CaptureValue` class encapsulates property path resolution
- Property access on `Capture` returns `CaptureValue` via Proxy interception
- `CaptureValue.resolve()` navigates the property path on the matched node at runtime
- Supports deep chaining (`method.name.simpleName`) and array access (`elements[0]`)
- Capture objects can be used directly as Map keys (normalized to string keys internally)
- Added `getName()` method to Capture interface for explicit string name access
- Internal `CAPTURE_NAME_SYMBOL` prevents Proxy interference with implementation

### 2. Constrained Captures

**Status**: ✅ Implemented

Add runtime validation to captures, allowing patterns to specify constraints that matched nodes must satisfy beyond structural matching.

**API:**

```typescript
// Define a capture with a constraint
const arg = capture<J.Literal>('arg', {
    constraint: (node) => typeof node.value === 'number' && node.value > 10
});

// Use in a structural pattern - constraint adds semantic validation
const pat = pattern`processData(${arg})`;
// Structurally matches: processData(<any expression>)
// Constraint validates: expression must be a numeric literal > 10
// Final result: only matches processData(42), processData(3.14), etc.

// Compose constraints using and(), or(), not()
const evenNumber = capture<J.Literal>('num', {
    constraint: and(
        (node) => typeof node.value === 'number',
        (node) => (node.value as number) % 2 === 0
    )
});
```

**Rationale:**
- Currently captures match based on pattern structure alone - a bare `pattern`${capture('x')}`` matches ANY expression
- No way to add semantic constraints (type checks, value validation) without manual validation in visitor code
- Constraints co-located with capture definitions improve code organization
- Reusable constraint functions can be packaged with captures
- Mirrors existing `configure()` API pattern used on Pattern objects

**Important: How Constraints Work**

Constraints provide **semantic validation** after **structural matching**:

1. **Structural matching** (pattern AST determines what can match):
   - `pattern`processData(${arg})`` structurally matches any call to `processData` with one argument
   - The argument can be any expression (identifier, literal, call, binary operation, etc.)

2. **Constraint validation** (runtime filter after structural match):
   - Constraint function receives the matched node: `(expr) => expr instanceof J.Literal && typeof expr.value === 'number'`
   - Returns `true`: match succeeds, capture is bound
   - Returns `false`: match fails, pattern returns `undefined`

**Best Practices:**

```typescript
// ✅ GOOD: Structural pattern + semantic constraint
const numArg = capture<J.Literal>('arg', {
    constraint: (node) => typeof node.value === 'number'
});
pattern`process(${numArg})`; // Matches process(42), rejects process("text")

// ⚠️ LESS EFFICIENT: Bare capture with constraint
const invocation = capture<J.MethodInvocation>('invocation', {
    constraint: (node) => node instanceof J.MethodInvocation
});
pattern`${invocation}`; // Structurally matches ANY expression, then filters

// ✅ BETTER: Use structural context when possible
pattern`${invocation}()`; // Structurally requires method invocation
```

**Type Safety Note:** The TypeScript generic `<T>` provides IDE autocomplete but doesn't enforce runtime types. Always include explicit type checks in your constraint function:

```typescript
// TypeScript type is just for autocomplete
const method = capture<J.MethodInvocation>('method', {
    // Must still check instanceof at runtime
    constraint: (node) =>
        node instanceof J.MethodInvocation && node.name.simpleName === 'foo'
});
```

**Implementation:**
- `capture()` function accepts optional `CaptureOptions<T>` parameter with `constraint` field
- Constraint function receives the matched node and returns boolean
- Composition functions `and()`, `or()`, `not()` allow combining constraints
- Pattern matching evaluates constraints in `Matcher.handleCapture()` after structural matching succeeds
- Failed constraints cause pattern match to fail (return no match)
- Constraints stored internally using `CAPTURE_CONSTRAINT_SYMBOL` to avoid Proxy interference
- TypeScript generic `<T>` on Capture provides type-safe constraint function parameter

**Test Coverage:**
- 15 comprehensive tests in `test/javascript/templating/capture-constraints.test.ts`
- Tests cover simple constraints, composition functions, and complex patterns

### 3. Rename `imports` to `context`

**Status**: ✅ Implemented

Rename the `imports` configuration option to `context` to accurately reflect that it accepts any declarations, not just import statements.

**API:**

```typescript
// Current API
const pat = pattern`forwardRef(${capture('name')})`
    .configure({
        imports: [`import { forwardRef } from 'react'`]  // Misleading name
    });

// Proposed API
const pat = pattern`forwardRef(${capture('name')})`
    .configure({
        context: [`import { forwardRef } from 'react'`]  // Clearer intent
    });
```

**Rationale:**
- `imports` option accepts any declarations (types, functions, constants), not just imports
- Name `context` better communicates purpose: providing surrounding context for type attribution
- More intuitive for users adding type/function declarations
- Reduces confusion about what can be provided

**Examples:**

```typescript
const pat = pattern`foo(${capture('x')})`
    .configure({
        context: [
            `type MyType = { value: number }`,        // Type declaration
            `const foo = (x: MyType) => x`,           // Function declaration
            `const bar = (x: string) => x`            // Another function
        ]
    });
```

**Migration Path:**
- Keep `imports` as deprecated alias for backward compatibility
- Emit deprecation warning when `imports` is used
- Update all documentation and examples to use `context`
- Remove `imports` alias in next major version

### 4. Pre-parsing Code Interpolation with `code()`

**Status**: 📝 Proposed

Add a `code()` function for dynamic code generation before template parsing, enabling templates to generate code based on matched patterns.

**API:**

```typescript
// String form - simple interpolation
code(str: string): CodeFragment

// Function form - dynamic generation with context
code(fn: (ctx: CodeContext) => string): CodeFragment

interface CodeContext {
    captures: Map<string, J>;      // Matched captures from pattern
    cursor: Cursor;                // Current cursor position
    tree: J;                       // Node being transformed
}
```

**Rationale:**
- Clear timing distinction: `code()` evaluates pre-parsing, captures evaluate post-parsing
- Enables dynamic code structure generation based on match results
- Function form receives context with captures for intelligent generation
- Supports both simple string interpolation and complex conditional logic

**Examples:**

```typescript
// Simple string interpolation
const tmpl = template`function foo() { ${code('return 42')} }`;

// Dynamic generation based on captures
const pat = pattern`foo(${capture('args')})`;
const tmpl = template`
    function wrapper() {
        ${code(ctx => {
            const args = ctx.captures.get('args') as J.MethodInvocation;
            const count = args.arguments.elements.length;
            return Array.from({length: count}, (_, i) =>
                `const arg${i} = arguments[${i}];`
            ).join('\n');
        })}
        return originalFn.apply(this, arguments);
    }
`;

// Conditional generation
const tmpl = template`
    ${code(ctx => ctx.captures.get('needsValidation')
        ? 'if (typeof x !== "number") throw new Error("Invalid");'
        : ''
    )}
    return x * 2;
`;
```

**Implementation:**
- `code()` returns `CodeFragment` marker object
- Template engine detects CodeFragments during template string construction
- String form: direct interpolation into template before parsing
- Function form: calls function with CodeContext, then interpolates result
- Evaluation happens during template.apply(), before AST parsing

**Timing:**
```
code():    construct → evaluate code() → concat strings → parse → substitute captures
capture:   construct → insert placeholder → parse → substitute captures
```

### 5. Builder API for Dynamic Template Construction

**Status**: ✅ Implemented

Add fluent builder API for programmatic template and pattern construction when structure isn't known at compile time.

**API:**

```typescript
class TemplateBuilder {
    code(str: string): this;        // Add static string part
    param(value: TemplateParameter): this;  // Add parameter
    build(): Template;
}

class PatternBuilder {
    code(str: string): this;        // Add static string part
    capture(value: Capture | string): this;  // Add capture
    build(): Pattern;
}

// Factory methods
Template.builder(): TemplateBuilder
Pattern.builder(): PatternBuilder
```

**Rationale:**
- Build templates programmatically when structure unknown at compile time
- Conditionally add parts based on runtime logic
- Compose templates from reusable fragments
- Generate repetitive patterns with loops (cleaner than string concatenation)

**Examples:**

```typescript
// Conditional construction
const builder = Template.builder().code('function foo(x) {');
if (needsValidation) {
    builder.code('if (typeof x !== "number") throw new Error("Invalid");');
}
builder.code('return x * 2; }');
const tmpl = builder.build();

// Loop-based pattern generation
const builder = Pattern.builder().code('myFunction(');
for (let i = 0; i < argCount; i++) {
    if (i > 0) builder.code(', ');
    builder.capture(capture(`arg${i}`));
}
builder.code(')');
const pat = builder.build();

// Composition from fragments
function createWrapper(innerBody: Capture): Template {
    return Template.builder()
        .code('function wrapper() { try { ')
        .param(innerBody)
        .code(' } catch(e) { console.error(e); } }')
        .build();
}
```

**Implementation:**
- Builder accumulates string parts and parameters separately
- `build()` creates synthetic TemplateStringsArray compatible with template literal API
- Delegates to existing `template()`/`pattern()` functions (no logic duplication)
- Methods return `this` for chaining

**Distinction from `code()` function:**
- Builder's `.code()`: Static string at build time
- `code()` function (Feature 4): Dynamic generation at apply time

```typescript
// Build time (builder)
Template.builder().code('return 42;').build();

// Apply time (code function)
template`function foo() { ${code(ctx => '...')} }`;
```

### 6. Lenient Type Matching in Comparator

**Status**: ✅ Implemented

Add option for lenient type matching in comparator, allowing patterns without full type attribution to still match structurally equivalent code.

**Current Workaround**: The `PatternMatchingComparator` class (which extends `JavaScriptSemanticComparatorVisitor`) includes targeted lenient matching for specific node types by overriding individual visitor methods:
- **Variable declarations**: Patterns with untyped variables match typed variables by adding a wildcard type capture (templating.ts:171-188)
- **Method declarations**: Patterns with untyped functions/methods match typed functions/methods by adding a wildcard return type capture (templating.ts:190-207)

This approach works but requires implementing a new override for each AST node type that has optional type annotations. It's a temporary solution that should be generalized.

**Problem**: Currently, when parsing untyped JavaScript/TypeScript code, many type-related fields are set to `undefined`:
- `J.VariableDeclarations.typeExpression` is `undefined` for `const x = 1`
- `J.MethodDeclaration.returnTypeExpression` is `undefined` for `function f() { }`
- Similar for parameter types, expression types, etc.

When comparing a pattern AST (often untyped) against target code AST (may be typed), the comparator sees:
- Pattern: `typeExpression = undefined`
- Target: `typeExpression = J.Identifier("string")`
- Result: No match (fields differ)

**Proposed Solutions**:

**Option A: Parser-Level Solution**
Change the parser to always populate type-related fields, even when types are unknown:
```typescript
// Instead of:
const x = 1;  // typeExpression: undefined

// Parser would create:
const x = 1;  // typeExpression: J.Identifier("unknown") or special Unknown type
```

**Benefits:**
- Simpler comparator logic - always has type objects to compare
- No need for special cases in visitor methods
- Type fields never `undefined`, reducing null checks throughout codebase
- Could introduce a special `JavaType.Unknown` type to represent missing attribution

**Drawbacks:**
- Changes LST structure and parser behavior (potentially breaking)
- Every AST node would be slightly larger (extra type objects)
- May complicate other visitors that check for `undefined` to detect untyped code
- Need to audit all code that checks `typeExpression === undefined`

**Option B: Comparator-Level Solution (Current + Enhancement)**
Add a `lenientTypeMatching` option to `JavaScriptSemanticComparatorVisitor`:
```typescript
constructor(options?: { lenientTypeMatching?: boolean })
```

The base class would handle type field comparisons generically:
- When comparing any field that's a type annotation (`typeExpression`, `returnTypeExpression`, etc.)
- If `lenientTypeMatching` is enabled and either side is `undefined`, allow the match
- Eliminates need for per-node-type overrides in `PatternMatchingComparator`

**Benefits:**
- No parser changes required
- Backward compatible - opt-in behavior
- Comparator logic centralized in one place
- Can enable/disable per pattern

**Drawbacks:**
- Comparator becomes more complex
- Need to identify which fields are "type fields" vs regular fields
- Still requires special handling, just in one place instead of many

**Recommendation**: Pursue Option B first (comparator-level) as it's less invasive. Option A (parser-level) could be considered for a future major version if we want to guarantee type objects are always present.

**Trade-offs Summary**:
- **Parser approach (Option A)**: Simpler comparator, but changes LST structure and has broader impact
- **Comparator approach (Option B)**: Preserves parser behavior, but requires smart type field detection in comparator

**API:**

```typescript
interface ComparatorOptions {
    lenientTypeMatching?: boolean;  // Default: false (strict)
}

class JavaScriptSemanticComparatorVisitor {
    constructor(
        ignoreLocation: boolean,
        options?: ComparatorOptions
    ) { }

    private compareTypes(type1: JavaType | null, type2: JavaType | null): boolean {
        if (this.lenientTypeMatching && (!type1 || !type2)) {
            return true;  // Allow match if either side missing type
        }
        return this.strictTypeComparison(type1, type2);
    }
}

// Pattern integration
const pat = pattern`foo(${capture('x')})`
    .configure({
        context: [`const foo = (x: any) => x`],
        lenientTypeMatching: true
    });
```

**Rationale:**
- Simple patterns work without full type setup (lower barrier to entry)
- Quick prototyping without complete dependency configuration
- Incremental adoption: start with structural matching, add types later
- Development workflow: iterate faster with lenient mode, tighten for production

**Lenient Mode Behavior:**
- Missing types on either side: allow match
- Structural equivalence still required (names, node types must match)
- Affects: method types, variable types, expression types, return types

**Examples:**

```typescript
// Quick prototyping without dependencies
const quickPat = pattern`forwardRef(${capture('comp')})`
    .configure({ lenientTypeMatching: true });

// Partial context (no full type info)
const partialPat = pattern`${capture('fn')}(${capture('arg')})`
    .configure({
        context: [`const someFunc = (x) => x`],  // Untyped
        lenientTypeMatching: true
    });

// Development → production workflow
const devPat = pattern`foo(${capture('x')})`
    .configure({ lenientTypeMatching: true });  // Fast iteration

const prodPat = pattern`foo(${capture('x')})`
    .configure({
        context: [
            `import { Foo } from './types'`,
            `declare function foo(x: Foo): void`
        ],
        dependencies: { './types': '^1.0.0' }
        // lenientTypeMatching: false (default) for strict validation
    });
```

**Implementation Plan (Option B - Recommended):**

1. **Add configuration to base comparator**:
   ```typescript
   class JavaScriptSemanticComparatorVisitor extends JavaVisitor<J> {
       constructor(
           protected readonly ignoreLocation: boolean = true,
           protected readonly lenientTypeMatching: boolean = false
       ) { }
   }
   ```

2. **Override field comparison to handle type fields**:
   - Identify which fields are type-related (naming convention: `*Type*`, `typeExpression`, `returnTypeExpression`, etc.)
   - When comparing these fields, if `lenientTypeMatching` is enabled and either is `undefined`, return `true` (match)
   - Keep strict comparison for non-type fields

3. **Update PatternMatchingComparator**:
   - Remove the specific `visitVariableDeclarations()` and `visitMethodDeclaration()` overrides
   - Enable `lenientTypeMatching` in constructor: `super(true, true)`
   - The base class now handles all type field comparisons

4. **Update Pattern configuration**:
   ```typescript
   const pat = pattern`foo(${capture('x')})`
       .configure({
           context: [`const foo = (x: any) => x`],
           lenientTypeMatching: true  // Pass to comparator
       });
   ```

**Implementation Considerations**:
- Need to determine which fields should be treated as "type fields"
- Could use field name conventions or maintain an explicit list
- May need special handling for nested type structures
- Should log when lenient matching occurs for debugging

### 7. `param()` for Template-Only Parameters

**Status**: ✅ Implemented

Add `param()` function as an alternative to `capture()` for use in templates that don't involve pattern matching, making the intent clearer.

**API:**

```typescript
// For templates used with patterns - use capture()
const method = capture<J.MethodInvocation>('method');
const pat = pattern`foo(${method})`;
const tmpl = template`bar(${method.name})`;

// For standalone templates - use param() for clarity
const node = param<J.Identifier>('nodeName');
const tmpl = template`function ${node}() { return 42; }`;
await tmpl.apply(cursor, someTree, new Map([['nodeName', identifierNode]]));
```

**Rationale:**
- `capture()` implies pattern matching context, but templates can be used standalone
- `param()` more clearly indicates simple parameter substitution without matching
- Reduces cognitive load: users don't wonder "what am I capturing?"
- Aligns terminology with common template systems (parameters, not captures)
- Both functions work identically - difference is semantic clarity

**When to Use Which:**

```typescript
// ✅ GOOD: Use capture() with patterns
const expr = capture('expr');
const pat = pattern`foo(${expr})`;
const tmpl = template`bar(${expr})`;  // Makes sense - captured from pattern

// ✅ GOOD: Use param() for standalone templates
const value = param('value');
const tmpl = template`return ${value} * 2;`;
await tmpl.apply(cursor, node, new Map([['value', someExpr]]));

// ⚠️ CONFUSING: capture() in standalone template
const value = capture('value');  // "Capturing" what? No pattern here!
template`return ${value} * 2;`

// ✅ ALSO GOOD: param() works with Builder API
const value = param('value');
const tmpl = Template.builder()
    .code('return ')
    .param(value)
    .code(' * 2;')
    .build();
```

**Implementation:**
- `param()` returns distinct `TemplateParam<T>` type (not `Capture<T>`)
- `TemplateParam` is simpler - no Proxy overhead, no property access support
- Both work in templates through unified template processing
- Type signature: `param<T = any>(name?: string): TemplateParam<T>`
- Type safety: Distinct types allow future enhancements specific to each use case

**Key Differences from Capture:**
- `TemplateParam`: Simple name-only parameter (no property access)
- `Capture`: Proxy-wrapped to support property access (e.g., `capture('x').name.simpleName`)
- Both handled identically by template engine during substitution
- Performance: `TemplateParam` avoids Proxy overhead

**Considerations:**

Can you mix in the same template?
```typescript
const c = capture('fromPattern');
const p = param('standalone');
template`${c} + ${p}`  // Both work technically
```

Decision: Yes, both work (unified template processing), but document that:
- Use `capture()` when template is used with a pattern
- Use `param()` when template is standalone
- Prefer consistency - don't mix unnecessarily (can be confusing)
- If you need property access, use `capture()` everywhere

### 8. Variadic Captures for Sequence Matching

**Status**: ✅ Implemented

Add support for matching variable-length sequences of nodes using variadic captures. Variadic captures can match zero or more nodes in function/method arguments and other sequence contexts.

**API:**

```typescript
// Capture zero or more arguments
const args = capture('args', { variadic: true });
const pat = pattern`foo(${args})`;

// Matches: foo(), foo(1), foo(1, 2), foo(1, 2, 3, ...)

// Capture first arg, rest are variadic
const first = capture('first');
const rest = capture('rest', { variadic: true });
const pat2 = pattern`foo(${first}, ${rest})`;

// Matches: foo(1), foo(1, 2), foo(1, 2, 3, ...)
// Use in template to add an argument while preserving others
const tmpl = template`foo(${first}, "newArg", ${rest})`;
```

**Rationale:**

Many refactoring scenarios require matching and transforming code with variable-length sequences:
- Matching function calls with any number of arguments
- Adding/removing/reordering arguments while preserving others
- Capturing "the rest" of arguments after specific ones
- Working with functions that have optional trailing parameters

Without variadic captures, patterns can only match fixed-length sequences, making it impossible to write flexible refactoring recipes.

**Variadic Options:**

```typescript
interface VariadicOptions {
    // Separator between elements (default: ', ' for arguments)
    separator?: string;

    // Minimum number of nodes to match (default: 0)
    min?: number;

    // Maximum number of nodes to match (default: unlimited)
    max?: number;
}

// Create a variadic capture
capture('args', { variadic: true })                    // Simple form with defaults
capture('args', { variadic: { separator: '; ' } })     // Custom separator
capture('args', { variadic: { min: 1, max: 3 } })      // With bounds
```

**How It Works:**

1. **Zero-or-more matching**: Variadic captures match zero or more nodes in a sequence
2. **Greedy matching**: Captures as many nodes as possible while allowing subsequent patterns to match
3. **Separator handling**: Separators are automatically inserted/removed during template application
4. **Empty matches**: When matching zero nodes, the capture binds to an empty array `[]`

**Pattern Styles:**

```typescript
// Match all arguments (zero or more)
const args = capture('args', { variadic: true });
pattern`foo(${args})`;
// Matches: foo(), foo(1), foo(1, 2), ...

// Match first + rest (one or more total)
const first = capture('first');
const rest = capture('rest', { variadic: true });
pattern`foo(${first}, ${rest})`;
// Matches: foo(1), foo(1, 2), foo(1, 2, 3), ...
// Note: Comma is part of the pattern, so foo() won't match
```

**Examples:**

```typescript
// Example 1: Match all arguments (including zero)
const args = capture('args', { variadic: true });
const pat = pattern`foo(${args})`;

// Matches:
// foo()            → args=[]
// foo(1)           → args=[1]
// foo(1, 2, 3)     → args=[1, 2, 3]

// Use in template:
const tmpl = template`bar(${args})`;
// foo()      → bar()
// foo(1, 2)  → bar(1, 2)

// Example 2: Required first argument, rest optional
const first = capture('first');
const rest = capture('rest', { variadic: true });
const pat2 = pattern`foo(${first}, ${rest})`;

// Matches:
// foo(1)           → first=1, rest=[]
// foo(1, 2)        → first=1, rest=[2]
// foo(1, 2, 3, 4)  → first=1, rest=[2, 3, 4]
// foo()            → NO MATCH (first is required)

// Add an argument while preserving others:
const tmpl2 = template`bar(${first}, "newArg", ${rest})`;
// foo(1)       → bar(1, "newArg")
// foo(1, 2, 3) → bar(1, "newArg", 2, 3)

// Example 3: Variadic with min/max bounds
const callback = capture('callback');
const options = capture('options', { variadic: { max: 1 } });
const pat3 = pattern`addEventListener('click', ${callback}, ${options})`;

// Matches:
// addEventListener('click', handler)              → callback=handler, options=[]
// addEventListener('click', handler, {once:true}) → callback=handler, options=[{once:true}]
// addEventListener('click', handler, a, b)        → NO MATCH (too many args)

// Example 4: Custom separator
const items = capture('items', { variadic: { separator: '; ' } });
const pat4 = pattern`process(${items})`;
// Matches process(...) and captures args separated by semicolons

// Example 5: Variadic with constraints (validates the entire array)
const numericArgs = capture<J.Literal>('args', {
    variadic: true,
    constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
});
const pat5 = pattern`sum(${numericArgs})`;
// Matches: sum(), sum(1), sum(1, 2, 3)
// Rejects: sum("text"), sum(1, "text", 3)

// Example 6: Variadic constraint - check relationships between elements
const sortedArgs = capture<J.Literal>('args', {
    variadic: true,
    constraint: (nodes) => {
        // Ensure all are numbers and in ascending order
        if (nodes.length < 2) return true;
        for (let i = 1; i < nodes.length; i++) {
            if (typeof nodes[i-1].value !== 'number' || typeof nodes[i].value !== 'number') {
                return false;
            }
            if ((nodes[i-1].value as number) > (nodes[i].value as number)) {
                return false;
            }
        }
        return true;
    }
});
const pat6 = pattern`process(${sortedArgs})`;
// Matches: process(1, 2, 3), process(1, 1, 2)
// Rejects: process(3, 1, 2), process(1, "x", 3)
```

**Implementation:**

- `capture()` accepts `variadic` option (boolean or `VariadicOptions` object)
- Variadic captures work in function/method call arguments
- Greedy matching: captures as many nodes as possible
- Separator handling: automatically inserted/removed during template application
- Type safety: `VariadicCapture<T>` type extends array type `T[]`
- Empty matches bind to empty array `[]`
- Min/max bounds supported for constraining match count
- **Constraints**: For variadic captures, the constraint function receives the entire array `T[]` (not individual elements), enabling validation of:
  - All elements meeting a criterion (`.every()`, `.some()`)
  - Array length requirements
  - Relationships between elements (ordering, uniqueness, etc.)
  - Specific positions (first/last elements)

**Test Coverage:**
- 6 test files covering variadic capture functionality
- Tests include basic matching, expansion, markers, array proxy behavior, matching algorithms, and constraints
- Constraint tests cover array-level validation, relationship validation, and combination with min/max bounds

**Future Enhancements:**
Variadic captures currently work in function arguments. Future phases could extend support to:
- Statement sequences in blocks
- Array literal elements
- Other sequence contexts

**Future Phase Examples - Statements and Arrays (Not Yet Implemented):**

```typescript
// Example 7: Statement sequences (PHASE 2)
const before = capture('before');
const middle = ellipsis('middle');
const after = capture('after');
const pat = pattern`
    console.log(${before});
    ${middle}
    console.log(${after});
`;

const tmpl = template`
    console.log(${after});  // Swap order
    ${middle}               // Keep middle as-is (flattened when inserted)
    console.log(${before});
`;

// Example 8: Try-catch patterns (PHASE 2)
const errorHandler = capture('handler');
const pat = pattern`
    try {
        ${ellipsis()}  // Match any statements, don't capture
    } catch (e) {
        ${errorHandler}
    }
`;

// Example 9: Multiple ellipses in one pattern (PHASE 2)
const before = ellipsis('before');
const target = capture('target');
const after = ellipsis('after');

const pat = pattern`
    function foo() {
        ${before}
        if (${target}) { return; }
        ${after}
    }
`;

// Example 10: Array element ellipsis (PHASE 3)
const first = capture('first');
const rest = ellipsis('rest');
const pat = pattern`[${first}, ${rest}]`;

// Example 11: Concise alias (all phases)
const pat = pattern`
    try {
        ${___()}
    } catch (e) {
        console.error(e);
    }
`;
```

**Rationale:**
- Essential for real-world refactoring patterns (not just toy examples)
- Common in other pattern matching systems (Semgrep uses `...`, Rust uses `..`, etc.)
- Enables matching code with variable structure
- Makes patterns more flexible and powerful
- Natural extension of existing capture concept

**Phase 1 Implementation Strategy - Function Arguments:**

**1. AST Context Detection:**

The parser needs to detect when an ellipsis appears in a function argument list context:

```typescript
// Pattern: foo(${first}, ${rest})
// Where rest is an Ellipsis

class TemplateProcessor {
    private buildTemplateString(): string {
        // Detect argument list context by tracking parentheses depth
        // and whether we're in a function call or method invocation

        for (let i = 0; i < this.captures.length; i++) {
            const capture = this.captures[i];
            if (capture instanceof EllipsisImpl && this.isInArgumentListContext()) {
                // Create ellipsis placeholder for arguments
                result += PlaceholderUtils.createEllipsis(capture.name);
            } else if (capture instanceof EllipsisImpl) {
                throw new Error('Ellipsis only supported in function arguments (Phase 1)');
            } else {
                result += PlaceholderUtils.createCapture(capture.name);
            }
        }
    }
}
```

**2. Argument List Matching Algorithm:**

For Phase 1, we only need to handle `J.MethodInvocation.arguments` (a list structure):

```typescript
/**
 * Matches pattern arguments against target arguments, handling ellipses.
 *
 * Phase 1: Only operates on J.MethodInvocation.arguments
 */
private async matchArgumentsWithEllipsis(
    patternArgs: PatternArgument[],  // Mix of Capture and Ellipsis
    targetArgs: JRightPadded<Expression>[],
    bindings: Map<string, J | J[]>
): Promise<boolean> {
    let patternIdx = 0;
    let targetIdx = 0;

    while (patternIdx < patternArgs.length) {
        const patternArg = patternArgs[patternIdx];

        if (patternArg.isEllipsis) {
            const ellipsis = patternArg.ellipsis!;

            // Determine how many target args this ellipsis should consume
            const remainingPatternArgs = patternArgs.length - patternIdx - 1;
            const remainingTargetArgs = targetArgs.length - targetIdx;
            const maxCapture = remainingTargetArgs - remainingPatternArgs;

            // Validate min/max constraints
            if (ellipsis.options?.min && maxCapture < ellipsis.options.min) {
                return false;  // Not enough args to satisfy minimum
            }

            const captureCount = ellipsis.options?.max
                ? Math.min(maxCapture, ellipsis.options.max)
                : maxCapture;

            if (captureCount < 0) {
                return false;  // Not enough arguments for remaining pattern
            }

            // Capture the arguments
            const captured = targetArgs
                .slice(targetIdx, targetIdx + captureCount)
                .map(rp => rp.element);

            // Apply constraints
            if (ellipsis.options?.constraint) {
                for (const arg of captured) {
                    if (!ellipsis.options.constraint(arg)) {
                        return false;
                    }
                }
            }

            if (ellipsis.options?.sequenceConstraint &&
                !ellipsis.options.sequenceConstraint(captured)) {
                return false;
            }

            // Store capture (empty array if no name = anonymous ellipsis)
            if (ellipsis.name) {
                bindings.set(ellipsis.name, captured);
            }

            targetIdx += captureCount;
            patternIdx++;
        } else {
            // Regular capture - must match exactly one argument
            if (targetIdx >= targetArgs.length) {
                return false;  // Pattern expects more args than target has
            }

            const matched = await this.matchNode(
                patternArg.node,
                targetArgs[targetIdx].element
            );

            if (!matched) {
                return false;
            }

            // Store regular capture
            if (patternArg.capture?.name) {
                bindings.set(patternArg.capture.name, targetArgs[targetIdx].element);
            }

            targetIdx++;
            patternIdx++;
        }
    }

    // All pattern arguments matched - but target may have extra args
    return targetIdx === targetArgs.length;
}
```

**3. Template Argument Substitution:**

When applying templates, ellipsis captures (which are arrays) need to be expanded:

```typescript
override async visitMethodInvocation(
    invocation: J.MethodInvocation,
    bindings: Map<string, J | J[]>
): Promise<J | undefined> {
    const newArgs: JRightPadded<Expression>[] = [];

    for (const arg of invocation.arguments.elements) {
        const marker = findEllipsisMarker(arg);

        if (marker) {
            // This argument position has an ellipsis marker
            const captured = bindings.get(marker.name);

            if (Array.isArray(captured)) {
                // Expand array into individual arguments
                for (const expr of captured) {
                    newArgs.push(JRightPadded.build(expr as Expression));
                }
            } else if (captured) {
                // Single value (shouldn't happen for ellipsis, but handle gracefully)
                newArgs.push(JRightPadded.build(captured as Expression));
            }
            // If captured is undefined and marker has no name (anonymous), skip
        } else {
            // Regular argument - keep as-is or substitute
            const result = await this.visit(arg.element, bindings);
            if (result) {
                newArgs.push(JRightPadded.build(result as Expression));
            }
        }
    }

    return produce(invocation, draft => {
        draft.arguments = produce(draft.arguments, argsDraft => {
            argsDraft.elements = newArgs;
        });
    });
}
```

**4. Type Safety:**

Update type system to distinguish ellipsis captures:

```typescript
class MatchResult {
    // Overload for Ellipsis - always returns array
    get<T>(capture: Ellipsis<T>): T[];
    get(capture: Ellipsis<any> | string): J[];

    // Overload for regular Capture - returns single value
    get<T>(capture: Capture<T>): T | undefined;
    get(capture: Capture | string): J | undefined;

    // Implementation handles both
    get(capture: Capture | Ellipsis | string): J | J[] | undefined {
        const key = typeof capture === 'string'
            ? capture
            : capture.getName();

        const value = this.bindings.get(key);

        // Runtime check: if it's an array, it was captured by ellipsis
        return value;
    }
}
```

**Phase 1 Limitations:**

- ❌ Ellipsis NOT supported in statement blocks
- ❌ Ellipsis NOT supported in array literals
- ❌ Ellipsis NOT supported in object literals
- ✅ Ellipsis ONLY supported in function/method call arguments
- ✅ Multiple ellipses per pattern supported (but must be in argument lists)

**General Implementation Strategy (All Phases):**

**1. Parser Detection:**
```typescript
class TemplateProcessor {
    private buildTemplateString(): string {
        for (let i = 0; i < this.captures.length; i++) {
            const capture = this.captures[i];
            if (capture instanceof EllipsisImpl) {
                result += PlaceholderUtils.createEllipsis(capture.name);
            } else {
                result += PlaceholderUtils.createCapture(capture.name);
            }
        }
    }
}
```

**2. AST Marker:**
```typescript
class EllipsisMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.EllipsisMarker';
    readonly id = randomId();

    constructor(
        public readonly name?: string,
        public readonly options?: EllipsisOptions<any>
    ) {}
}
```

**3. Sequence Matching Algorithm:**

Use greedy matching with backtracking:

```typescript
/**
 * Matches pattern sequence against target sequence where pattern may contain ellipses.
 *
 * Pattern: [A, ellipsis, B]
 * Target:  [A, X, Y, Z, B]
 * Result:  Match! ellipsis captures [X, Y, Z]
 *
 * Uses greedy matching: ellipsis captures as much as possible while still
 * allowing subsequent pattern elements to match.
 */
private matchSequenceWithEllipsis(
    patternElements: PatternElement[],
    targetElements: J[],
    bindings: Map<string, J | J[]>
): boolean {
    let patternIdx = 0;
    let targetIdx = 0;

    while (patternIdx < patternElements.length) {
        const patternElem = patternElements[patternIdx];

        if (patternElem.isEllipsis) {
            // Find the next non-ellipsis pattern element
            const nextPattern = patternElements[patternIdx + 1];

            if (!nextPattern) {
                // Ellipsis at end - captures rest of target
                const captured = targetElements.slice(targetIdx);
                if (patternElem.name) {
                    bindings.set(patternElem.name, captured);
                }
                return true;
            }

            // Find where nextPattern matches in remaining target
            let matchPos = -1;
            for (let i = targetIdx; i < targetElements.length; i++) {
                if (await this.matchNode(nextPattern.node, targetElements[i])) {
                    matchPos = i;
                    break;
                }
            }

            if (matchPos === -1) {
                return false; // Next pattern element not found
            }

            // Ellipsis captures everything before matchPos
            const captured = targetElements.slice(targetIdx, matchPos);

            // Validate constraints
            if (!this.validateEllipsisConstraints(patternElem, captured)) {
                return false;
            }

            if (patternElem.name) {
                bindings.set(patternElem.name, captured);
            }

            targetIdx = matchPos;
            patternIdx++;
        } else {
            // Regular pattern element - must match current target
            if (targetIdx >= targetElements.length) {
                return false;
            }

            if (!await this.matchNode(patternElem.node, targetElements[targetIdx])) {
                return false;
            }

            targetIdx++;
            patternIdx++;
        }
    }

    // All pattern elements matched, but target may have extra elements
    return targetIdx === targetElements.length;
}
```

**4. Statement List Flattening:**

When inserting ellipsis into blocks, flatten arrays:

```typescript
override async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
    const newStatements = [];

    for (const stmt of block.statements) {
        const result = await this.visit(stmt.element, p);

        if (Array.isArray(result)) {
            // Ellipsis returned multiple statements - flatten
            for (const item of result) {
                newStatements.push(JRightPadded.build(item));
            }
        } else if (result) {
            newStatements.push(JRightPadded.build(result));
        }
    }

    return produce(block, draft => {
        draft.statements = newStatements;
    });
}
```

**Type System:**

```typescript
// Ellipsis captures return arrays
const middle = ellipsis<J.Statement>('middle');
const match = await pat.match(node);

if (match) {
    const stmts: J.Statement[] = match.get(middle); // Type is array
}

// Regular captures return single nodes
const target = capture<J.Identifier>('target');
const match2 = await pat.match(node);

if (match2) {
    const id: J.Identifier = match2.get(target); // Type is single node
}
```

Update `MatchResult.get()` to handle both:
```typescript
class MatchResult {
    get<T>(capture: Capture<T> | string): T | undefined;
    get<T>(capture: Ellipsis<T> | string): T[] | undefined;
    get(capture: Capture | Ellipsis | string): J | J[] | undefined {
        // Runtime discrimination based on capture type
    }
}
```

**Phased Rollout Plan:**

| Phase | Context | Priority | Complexity | Deliverable |
|-------|---------|----------|------------|-------------|
| **Phase 1** | Function/Method Arguments | ⭐️ High | 🟢 Medium | `foo(${first}, ${rest})` |
| **Phase 2** | Statement Sequences | ⭐️ High | 🟡 High | `{ ${before}; stmt; ${after}; }` |
| **Phase 3** | Array Literals | Medium | 🟢 Medium | `[${first}, ${rest}]` |
| **Phase 4** | Object Literals | Low | 🔴 Very High | `{${before}, key: val, ${after}}` |

**Phase 1 Success Criteria:**
- ✅ Ellipsis works in function call arguments (any position)
- ✅ Multiple ellipses per argument list supported
- ✅ Template substitution expands ellipsis captures correctly
- ✅ Type system distinguishes `Capture<T>` vs `Ellipsis<T>`
- ✅ Clear error messages when ellipsis used outside argument context
- ✅ Comprehensive test coverage (10+ test cases)
- ✅ Documentation with real-world examples

**Edge Cases (All Phases):**

1. **Multiple ellipses in sequence**: Need greedy matching that doesn't consume too much
2. **Adjacent ellipses**: `pattern`${ellipsis('a')}${ellipsis('b')}`` - Should this be an error or use min/max to disambiguate?
3. **Empty matches**: Allowed by default (`min: 0`)
4. **Performance**: Backtracking can be expensive - consider caching and limiting depth
5. **Phase 1 specific**: Last argument ellipsis is simpler (greedy to end)

## Consequences

### Property Access on Captures (Proposed)

**Positive:**
1. **Reduced boilerplate**: No need to manually extract properties in visitor code
2. **Natural syntax**: Uses familiar JavaScript property access and array indexing
3. **Type-safe**: TypeScript generics provide autocomplete for property paths
4. **Flexible**: Supports deep chaining and complex property navigation

**Negative:**
1. **Learning curve**: New concept of CaptureValue that users need to understand
2. **Proxy complexity**: Internal implementation uses Proxy which can be tricky to debug

**Mitigation strategies:**
- Provide clear documentation with examples of property access patterns
- Add TypeScript types for better IDE support and autocomplete
- Include debugging utilities to inspect CaptureValue resolution
- Document Proxy behavior for advanced users who need to understand internals

### Constrained Captures (Proposed)

**Positive:**
1. **More precise patterns**: Captures can validate node types and properties without additional visitor code
2. **Better code organization**: Constraints are co-located with capture definitions
3. **Reusable constraints**: Common patterns can be packaged with their constraints
4. **API consistency**: Using `configure()` matches the existing Pattern API design
5. **Type safety**: TypeScript generic parameter on capture provides autocomplete for constraint function
6. **Composable**: Constraints can be built from reusable predicate functions

**Negative:**
1. **Complexity**: Adds another concept to the templating API
2. **Performance**: Constraint evaluation adds runtime overhead to pattern matching
3. **Debugging**: Failed constraints may be harder to debug than explicit visitor checks

**Mitigation strategies:**
- Provide clear documentation with common constraint patterns
- Add debug logging for constraint evaluation failures
- Keep constraint evaluation efficient (avoid expensive operations)
- Consider caching constraint results if the same node is checked multiple times

### Rename `imports` to `context` (Proposed)

**Positive:**
1. **Clearer intent**: Name accurately reflects that any declarations can be provided
2. **Better discoverability**: Users more likely to understand they can add type/function declarations
3. **Reduced confusion**: No longer implies only import statements are allowed
4. **Backward compatible**: Keeping `imports` as alias ensures no breaking changes initially

**Negative:**
1. **Migration effort**: Existing code using `imports` needs to be updated
2. **Documentation churn**: All examples and docs need updating
3. **Temporary API duplication**: Both names supported during migration period

**Mitigation strategies:**
- Implement as non-breaking change with deprecation warning
- Provide clear migration guide in release notes
- Use automated tooling to help migrate codebases
- Set clear timeline for eventual removal of `imports` alias

### Pre-parsing Code Interpolation with `code()` (Proposed)

**Positive:**
1. **Clear semantics**: Distinct from capture substitution with obvious timing (pre vs post parse)
2. **Powerful generation**: Can generate complex, dynamic code structures based on matched nodes
3. **Access to context**: Function form receives captures and cursor for intelligent generation
4. **Flexible**: Both simple string interpolation and complex function-based generation
5. **Composable**: Can be combined with captures for sophisticated transformations

**Negative:**
1. **Complexity**: Introduces two-phase evaluation model (pre-parsing and post-parsing)
2. **Error handling**: Errors in code generation happen at different time than parse errors
3. **Debugging difficulty**: Generated code may be harder to debug since it's dynamic
4. **Performance**: Function evaluation adds overhead, especially if expensive operations in generator
5. **Type safety challenges**: Generated code is strings, hard to validate correctness

**Mitigation strategies:**
- Provide clear documentation explaining timing and use cases
- Add validation that generated code is well-formed (syntax check before parsing)
- Include source maps or debug info linking generated code to generator function
- Add warnings for expensive operations in generator functions
- Provide helper utilities for common code generation patterns
- Consider caching generated code when possible

### Builder API for Dynamic Template Construction (Proposed)

**Positive:**
1. **Programmatic flexibility**: Build templates/patterns dynamically when structure unknown at compile time
2. **Conditional construction**: Easy to add/remove parts based on runtime logic
3. **Composability**: Build templates from reusable fragments and functions
4. **Loop-friendly**: Natural way to generate repetitive patterns
5. **No string concatenation**: Cleaner than manually building template strings
6. **Type-safe**: Builder methods provide better IDE support than string building

**Negative:**
1. **Verbosity**: Builder API more verbose than template literal syntax
2. **Two APIs**: Users need to learn both template literals and builder pattern
3. **Naming confusion**: Builder's `.code()` vs `code()` function have different purposes
4. **Less readable**: Template literals are more concise and easier to read for simple cases
5. **Learning curve**: Builder pattern adds another concept to learn

**Trade-offs:**
- Template literals: Best for static, compile-time-known patterns (most common case)
- Builder API: Best for dynamic, runtime-determined patterns (advanced use cases)

**Mitigation strategies:**
- Document clear guidance on when to use each approach
- Provide examples showing both styles side-by-side
- Clarify the distinction between builder's `.code()` (build-time) and `code()` function (apply-time)
- Consider renaming builder's `.code()` to `.text()` or `.fragment()` to reduce confusion
- Ensure builder API delegates to template literals internally (no code duplication)

### Lenient Type Matching in Comparator (Proposed)

**Positive:**
1. **Practical patterns**: Simple patterns work without full type setup, lowering barrier to entry
2. **Quick prototyping**: Test patterns without complete dependency configuration
3. **Incremental adoption**: Start with structural matching, add type validation later
4. **Backward compatible**: Existing patterns with partial types continue to work
5. **Flexible matching**: Useful when structural equivalence is sufficient
6. **Development workflow**: Faster iteration during development, stricter checks in production

**Negative:**
1. **Reduced precision**: May match unintended nodes if types aren't validated
2. **False positives**: Pattern may match structurally similar but semantically different code
3. **Debugging complexity**: Harder to diagnose why a pattern matched incorrectly
4. **Hidden bugs**: Type mismatches may go undetected until runtime
5. **Inconsistent behavior**: Same pattern behaves differently with/without lenient flag
6. **Documentation burden**: Need clear guidance on when lenient mode is appropriate

**Trade-offs:**
- **Strict mode (default)**: Precise, type-safe matching but requires complete type attribution
- **Lenient mode**: More flexible, easier to use but potentially less accurate

**Mitigation strategies:**
- Default to strict mode; lenient is opt-in to maintain safety by default
- Add clear documentation explaining trade-offs and when to use each mode
- Provide validation tooling to detect patterns that match too broadly in lenient mode
- Include warnings when lenient matching succeeds but types don't align
- Recommend transitioning from lenient to strict as patterns mature
- Add debug logging showing what would have failed in strict mode
- Consider "strict development, lenient production" or vice versa depending on use case

### `param()` for Template-Only Parameters (Proposed)

**Positive:**
1. **Clearer intent**: `param()` clearly indicates template parameter substitution, not pattern capture
2. **Reduced confusion**: Users don't wonder "what am I capturing?" when no pattern exists
3. **Better semantics**: Aligns with common template system terminology (parameters)
4. **Self-documenting**: Code intent is clearer when reading template-only usage
5. **No learning burden**: Simple alias - users who know `capture()` immediately understand `param()`
6. **Backward compatible**: `capture()` continues to work everywhere

**Negative:**
1. **Two names for same thing**: API surface increases with synonym
2. **Inconsistency risk**: Users might mix `param()` and `capture()` inconsistently
3. **Documentation burden**: Need to explain when to use which
4. **Potential confusion**: New users might wonder about the difference
5. **Edge cases**: What about templates sometimes used with patterns, sometimes standalone?

**Trade-offs:**
- **Semantic clarity vs API simplicity**: Clearer intent but more to learn
- **With patterns**: `capture()` makes sense (you're capturing from pattern)
- **Without patterns**: `param()` makes sense (you're substituting parameters)
- **Mixed usage**: Either works, but one or the other is more semantically appropriate

**Mitigation strategies:**
- Clear documentation: "Use `param()` for standalone templates, `capture()` with patterns"
- IDE hints: Could add JSDoc suggesting `param()` when template not used with pattern
- Code examples: Show both use cases side-by-side in documentation
- Linter rule: Suggest `param()` when template variable never used in pattern
- Keep implementation simple: Just an alias, no runtime behavior difference
- Accept that some mixing will occur: Both work, document best practices but don't enforce

### Ellipsis Patterns for Sequence Matching (Proposed)

**Positive:**
1. **Real-world patterns**: Enables matching patterns that actually occur in codebases (variable-length sequences)
2. **Powerful matching**: Can express complex patterns like "find try blocks with specific error handling regardless of body"
3. **Flexible**: Works in multiple contexts (statements, arguments, array elements)
4. **Natural extension**: Fits naturally into existing capture/pattern model
5. **Industry standard**: Similar to features in Semgrep, Rust patterns, Python unpacking, etc.
6. **Composable**: Can combine multiple ellipses with regular captures for sophisticated patterns
7. **Constrained matching**: Options for min/max and custom constraints on sequences
8. **Type-safe**: TypeScript generics provide array types for ellipsis captures

**Negative:**
1. **Complexity**: Adds significant complexity to pattern matching algorithm (backtracking, ambiguity)
2. **Performance**: Sequence matching with multiple ellipses can be expensive (O(n*m) or worse)
3. **Ambiguity**: Adjacent ellipses or multiple ellipses can have ambiguous matches
4. **Learning curve**: Another concept for users to understand beyond basic captures
5. **Edge cases**: Many edge cases to handle (empty sequences, multiple matches, greedy vs minimal)
6. **Implementation effort**: Requires changes to parser, comparator, and template application
7. **Debugging difficulty**: Failed matches with ellipses harder to diagnose than simple captures
8. **Type discrimination**: `MatchResult.get()` must handle both single values and arrays

**Trade-offs:**

**Greedy vs Minimal Matching:**
- **Greedy** (proposed): Ellipsis captures as much as possible
  - Pro: Intuitive for most use cases
  - Con: May not match what user expects in some cases
- **Minimal**: Ellipsis captures as little as possible
  - Pro: More predictable in some patterns
  - Con: Less intuitive, requires more explicit constraints

**Multiple Ellipses:**
- **Allow with constraints**: Use min/max to disambiguate
  - Pro: Maximum flexibility
  - Con: Complex, easy to create ambiguous patterns
- **Restrict**: Only one ellipsis per sequence
  - Pro: Simpler implementation and mental model
  - Con: Less expressive

**Anonymous Ellipsis:**
- **Allow** (proposed): `${ellipsis()}` matches but doesn't capture
  - Pro: Useful for "don't care" matching
  - Con: Might be confusing (why not just omit?)
- **Disallow**: All ellipses must be named
  - Pro: Explicit capture intent
  - Con: Forces capturing even when not needed

**Recommendation**: Start with greedy matching and single ellipsis per context, then expand based on user feedback.

**Mitigation strategies:**
1. **Performance**:
   - Add performance warnings for patterns with multiple ellipses
   - Consider caching partial match results
   - Limit backtracking depth with configurable timeout
   - Profile and optimize hot paths

2. **Ambiguity**:
   - Document greedy matching semantics clearly
   - Provide examples of common ambiguous patterns and how to resolve them
   - Consider warning when pattern has multiple valid matches
   - Add validation that detects obviously ambiguous patterns (adjacent unconstrained ellipses)

3. **Debugging**:
   - Add detailed logging showing how ellipses matched (what was captured)
   - Provide visualization of sequence matching steps
   - Include match diagnostics showing which elements were consumed by each ellipsis
   - Show alternative matches when pattern is ambiguous

4. **Learning curve**:
   - Provide comprehensive examples from simple to complex
   - Document common patterns (cookbook style)
   - Add interactive tutorial showing ellipsis behavior
   - Compare to similar features in other tools (Semgrep, etc.)

5. **Type safety**:
   - Provide clear TypeScript types distinguishing Capture (returns single) from Ellipsis (returns array)
   - Add runtime checks to prevent incorrect usage
   - Document type discrimination in `MatchResult.get()`

6. **Edge cases**:
   - Clearly document behavior for empty sequences (allowed by default)
   - Show how to use `min` constraint to require non-empty
   - Explain how multiple ellipses are disambiguated
   - Provide examples of all edge cases with expected behavior

**Implementation phases:**

**Phase 1: Basic ellipsis (single, unconstrained)**
- Single ellipsis per sequence context
- No constraints (matches 0 or more)
- Works in statement sequences only
- Greedy matching

**Phase 2: Constraints**
- Add min/max options
- Add element and sequence constraints
- Add validation for ambiguous patterns

**Phase 3: Multiple ellipses**
- Allow multiple ellipses in same sequence
- Implement disambiguation via constraints
- Add performance optimizations

**Phase 4: Extended contexts**
- Support in function arguments
- Support in array elements
- Future: object properties, class members

**Priority**: **High** - Essential for production-quality refactoring patterns. Without this, many real-world patterns cannot be expressed.

### `any()` for Non-Capturing Pattern Matching (Proposed)

**Positive:**
1. **Clear intent**: `any()` clearly communicates "match but don't capture" vs misleading use of `capture()` with unused names
2. **Semantic correctness**: Distinguishes matching (pattern concern) from binding (template concern)
3. **Code clarity**: Reading `pattern\`foo(${any()})\`` is more intuitive than `pattern\`foo(${capture('unused')})\``
4. **Prevents errors**: Type system can prevent accidentally using non-capturing matches in templates
5. **Memory efficiency**: Pattern matcher doesn't need to store bindings for anonymous matches
6. **Flexible validation**: Can apply constraints without the overhead of capturing
7. **Natural extension**: Works with all existing features (variadic, constraints) seamlessly

**Negative:**
1. **API surface increase**: Another function for users to learn and remember
2. **Overlapping functionality**: Technically `capture()` can do everything `any()` does (with unused name)
3. **Implementation complexity**: Requires tracking capturing vs non-capturing state throughout the system
4. **Naming discussion**: "any" might imply "any type" rather than "any value without capturing"
5. **Edge case handling**: Need to ensure non-capturing placeholders work correctly in all contexts
6. **Documentation burden**: Need to explain when to use `capture()` vs `any()`

**Trade-offs:**

**Semantic Clarity vs API Simplicity:**
- **With `any()`**: More functions to learn, but intent is explicit
- **Without `any()`**: Simpler API surface, but using `capture('_')` is semantically misleading

**Memory vs Clarity:**
- **Storing unused bindings**: Wastes memory but simplifies implementation
- **Tracking capturing flag**: More complex but more efficient

**Type Safety:**
- **Prevent `any()` in templates**: TypeScript error if you try (good!)
- **Allow `any()` in templates**: Runtime error or silent no-op (bad!)

**Mitigation strategies:**

1. **Clear documentation**:
   - Side-by-side examples showing `capture()` vs `any()`
   - Decision flowchart: "Do you need the value? → Yes: capture() / No: any()"
   - Common patterns cookbook

2. **Type safety**:
   - Make `any()` return distinct `Any<T>` type that templates reject
   - Provide clear TypeScript error messages
   - Add JSDoc warnings about template usage

3. **Implementation**:
   - Reuse `Capture` infrastructure with `capturing: false` flag
   - Minimal code duplication - same overloads, same options
   - Unified placeholder parsing with different prefix (`__anon_` vs `__capt_`)

4. **Naming alternatives** (considered but not chosen):
   - `wildcard()` / `Wildcard<T>` - emphasizes "matches anything"
   - `anonymous()` / `Anonymous<T>` - emphasizes "has no name"
   - `match()` / `Match<T>` - emphasizes matching without capturing
   - `_` (function) - traditional placeholder symbol (but may conflict with lodash)
   - `ignore()` / `Ignore<T>` - emphasizes not caring about the value
   - **Chosen: `any()` / `Any<T>`** - semantic parallel with TypeScript's `any` type (permissive at this position)

5. **Best practices**:
   - Lint rule: Suggest `any()` when `capture()` used but value never referenced
   - IDE hints: Show "This capture is never used" warning
   - Migration: Automated refactoring to convert unused captures to `any()`

6. **Performance**:
   - Skip binding step for non-capturing matches
   - Early return in constraint validation if match fails
   - Don't allocate map entry for anonymous captures

**Alternative Considered: Using Same `Capture<T>` Type**

Instead of separate `Any<T>` type, `any()` could return `Capture<T>` with internal flag:

```typescript
// Considered: same type
export function any<T>(options?: CaptureOptions<T>): Capture<T>;
// Runtime flag prevents binding, but type system can't prevent template usage

// Chosen: separate type
export function any<T>(options?: CaptureOptions<T>): Any<T>;
// Type system prevents template usage at compile time
```

**Pros of same type**: Simpler type system, less to learn
**Cons of same type**: No compile-time safety, errors only at runtime

**Decision**: Use separate `Any<T>` type for compile-time safety.

### 9. `any()` for Non-Capturing Pattern Matching

**Status**: ✅ Implemented

Add `any()` function for matching AST nodes without binding them to a name, providing clearer semantic intent for wildcard patterns compared to using `capture()` with an unused name.

**API:**

```typescript
// Match any expression without capturing it
const pat = pattern`foo(${any()})`;

// Match multiple arguments without capturing them
const pat2 = pattern`bar(${any()}, ${any()})`;

// Variadic any - match zero or more without capturing
const rest = any({ variadic: true });
const pat3 = pattern`baz(${capture('first')}, ${rest})`;

// With constraints - validate but don't capture
const numericArg = any<J.Literal>({
    constraint: (node) => typeof node.value === 'number'
});
const pat4 = pattern`process(${numericArg})`;
```

**Rationale:**

When writing patterns, you often need to match nodes without caring about their specific values:
- Matching method calls regardless of arguments: `foo(${any()})`
- Matching structure without needing access to all parts: `if (${any()}) { ${capture('body')} }`
- Validating presence/type without binding: `validate(${any({ constraint: isNumber })})`

Using `capture()` for this is semantically misleading:
```typescript
// ⚠️ CONFUSING: Creating a capture but never using it
const ignored = capture('ignored');
pattern`foo(${ignored})`;  // Why capture if you never reference it?

// ✅ CLEAR: Explicitly states "match anything here"
pattern`foo(${any()})`;    // Intent is obvious
```

**When to Use Which:**

```typescript
// Use capture() when you need to reference the value
const arg = capture('arg');
const pat = pattern`foo(${arg})`;
const tmpl = template`bar(${arg})`; // Using captured value

// Use any() when you just need structural matching
const pat2 = pattern`foo(${any()})`;  // Match foo with any argument
const tmpl2 = template`bar(42)`;      // Replacement doesn't use the argument

// Use any() with constraints for validation without binding
const numericArg = any<J.Literal>({
    constraint: (node) => typeof node.value === 'number'
});
const pat3 = pattern`process(${numericArg})`;
// Matches: process(42), process(3.14)
// Rejects: process("text")
// But doesn't bind the value
```

**Variadic Support:**

`any()` supports the same variadic options as `capture()`:

```typescript
// Match function with any number of arguments
const args = any({ variadic: true });
pattern`foo(${args})`;
// Matches: foo(), foo(1), foo(1, 2, 3), ...

// Match first arg + any remaining args
const first = capture('first');
const rest = any({ variadic: true });
pattern`bar(${first}, ${rest})`;
// Matches: bar(1), bar(1, 2), bar(1, 2, 3), ...
// Only binds 'first', rest are matched but not captured

// Variadic with constraints
const numericArgs = any<J.Literal>({
    variadic: true,
    constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
});
pattern`sum(${numericArgs})`;
// Matches: sum(1, 2, 3) - all arguments must be numbers
// But doesn't capture the arguments
```

**Comparison with Capture:**

| Feature | `capture()` | `any()` |
|---------|-------------|---------|
| **Purpose** | Match and bind value | Match without binding |
| **Use in patterns** | ✅ Yes | ✅ Yes |
| **Use in templates** | ✅ Yes (substitutes value) | ❌ No (nothing to substitute) |
| **Property access** | ✅ Yes (`capture.name`) | ❌ No (no value to access) |
| **Constraints** | ✅ Yes | ✅ Yes |
| **Variadic** | ✅ Yes | ✅ Yes |
| **Semantic meaning** | "Capture this value" | "Match anything here" |

**Implementation:**

- `any()` returns distinct `Any<T>` type (not `Capture<T>`)
- Uses same `__capt_` prefix internally as `capture()` (distinction is in type system only)
- Pattern matcher validates constraints but doesn't store binding (controlled by internal `capturing: false` flag)
- Template substitution ignores non-capturing placeholders
- Type system prevents `any()` result from being used in templates (compile-time safety)
- Same signature as `capture()` including all options (variadic, constraints)

**Implementation Details:**

```typescript
// Separate interface for non-capturing matches
export interface Any<T> {
    getName(): string;
    isVariadic(): boolean;
    getVariadicOptions(): VariadicOptions | undefined;
    getConstraint?(): ((node: T) => boolean) | undefined;
}

// Function signature - returns Any<T>, not Capture<T>
export function any<T = any>(
    options?: CaptureOptions<T>
): Any<T> & T;

export function any<T = any>(
    options: { variadic: true | VariadicOptions; constraint?: (nodes: T[]) => boolean }
): Any<T[]> & T[];

// Internal implementation
class CaptureImpl {
    constructor(
        name: string,
        options?: CaptureOptions<T>,
        private readonly capturing: boolean = true  // New field
    ) { }
}

// Factory functions
export function capture<T>(name?: string, options?: CaptureOptions<T>): Capture<T> {
    const captureName = name || `unnamed_${capture.nextUnnamedId++}`;
    return new CaptureImpl(captureName, options, true) as Capture<T>;  // capturing = true
}

export function any<T>(options?: CaptureOptions<T>): Any<T> {
    const anonName = `anon_${any.nextAnonId++}`;
    return new CaptureImpl(anonName, options, false) as Any<T>;  // capturing = false
}
```

**Placeholder Generation:**

```typescript
class PlaceholderUtils {
    static createCapture(name: string, capturing: boolean = true): string {
        const prefix = capturing ? '__capt_' : '__anon_';
        return `${prefix}${name}__`;
    }
}
```

**Pattern Matching:**

```typescript
class Matcher {
    private handleCapture(pattern: J, target: J, captureInfo: CaptureInfo): boolean {
        // Validate node matches (structural + constraints)
        const matches = this.matchNode(pattern, target);
        if (!matches) return false;

        // Only bind if this is a capturing placeholder
        if (captureInfo.capturing) {
            this.bindings.set(captureInfo.name, target);
        }

        return true;
    }
}
```

**Type Safety:**

```typescript
// Prevent any() from being used in templates via type system
const wildcard = any();
const pat = pattern`foo(${wildcard})`;  // ✅ OK in pattern

const tmpl = template`bar(${wildcard})`;  // ❌ TypeScript error
// Error: Type 'Any<any>' is not assignable to template parameter
// Templates only accept 'Capture<T>' or 'TemplateParam<T>'
```

**Examples:**

```typescript
// Example 1: Match structure without caring about arguments
const findFooCalls = pattern`foo(${any()})`;
// Matches: foo(1), foo("x"), foo(obj), ...
// Use case: Finding all calls to foo() regardless of arguments

// Example 2: Match with validation but don't capture
const hasNumericArg = any<J.Literal>({
    constraint: (node) => typeof node.value === 'number'
});
const validatePattern = pattern`validate(${hasNumericArg})`;
// Matches: validate(42), validate(3.14)
// Rejects: validate("text")
// Use case: Ensuring argument type without needing the value

// Example 3: Capture first arg, ignore rest
const first = capture('first');
const rest = any({ variadic: true });
const pat = pattern`process(${first}, ${rest})`;
const tmpl = template`processModified(${first})`;
// Input:  process(1, 2, 3)
// Output: processModified(1)
// Use case: Dropping extra arguments

// Example 4: Complex pattern with mixed capture/any
const target = capture('target');
const pat = pattern`
    if (${any()}) {
        ${target}
    }
`;
// Matches if statements, captures body but not condition
// Use case: Finding specific statement patterns regardless of condition

// Example 5: Variadic with mixed capture and any
const method = capture('method');
const leadingArgs = any({ variadic: { max: 2 } });
const callback = capture('callback');
const pat = pattern`addEventListener(${method}, ${leadingArgs}, ${callback})`;
// Matches various addEventListener signatures
// Captures: event type and callback
// Ignores: optional middleware arguments
```

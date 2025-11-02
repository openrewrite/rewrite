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

This ADR proposes eight enhancements to address these limitations and make the templating engine more expressive and flexible.

### Summary of Enhancements

| # | Feature | Status |
|---|---------|--------|
| 1 | Property Access on Captures | 📝 Proposed |
| 2 | Constrained Captures | 📝 Proposed |
| 3 | Rename `imports` to `context` | 📝 Proposed |
| 4 | Pre-parsing Code Interpolation with `code()` | 📝 Proposed |
| 5 | Builder API for Dynamic Construction | 📝 Proposed |
| 6 | Lenient Type Matching in Comparator | 📝 Proposed |
| 7 | `param()` for Template-Only Parameters | 📝 Proposed |
| 8 | Ellipsis Patterns for Sequence Matching | 📝 Proposed |

## Decision

We will implement the following enhancements to the JavaScript templating engine:

### 1. Property Access on Captures

**Status**: 📝 Proposed

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

**Status**: 📝 Proposed

Add runtime validation to captures, allowing patterns to specify constraints that matched nodes must satisfy beyond structural matching.

**API:**

```typescript
// Define a capture with a constraint
const arg = capture<J.Expression>('arg')
    .configure({
        constraint: (expr: J.Expression) =>
            expr instanceof J.Literal && typeof expr.value === 'number'
    });

// Use in a structural pattern - constraint adds semantic validation
const pat = pattern`processData(${arg})`;
// Structurally matches: processData(<any expression>)
// Constraint validates: expression must be a numeric literal
// Final result: only matches processData(42), processData(3.14), etc.
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
const numArg = capture('arg').configure({
    constraint: (node) => node instanceof J.Literal && typeof node.value === 'number'
});
pattern`process(${numArg})`; // Matches process(42), rejects process("text")

// ⚠️ LESS EFFICIENT: Bare capture with constraint
const invocation = capture('invocation').configure({
    constraint: (node) => node instanceof J.MethodInvocation
});
pattern`${invocation}`; // Structurally matches ANY expression, then filters

// ✅ BETTER: Use structural context when possible
pattern`${invocation}()`; // Structurally requires method invocation
```

**Type Safety Note:** The TypeScript generic `<T>` provides IDE autocomplete but doesn't enforce runtime types. Always include explicit type checks in your constraint function:

```typescript
// TypeScript type is just for autocomplete
const method = capture<J.MethodInvocation>('method')
    .configure({
        // Must still check instanceof at runtime
        constraint: (node: J.MethodInvocation) =>
            node instanceof J.MethodInvocation && node.name.simpleName === 'foo'
    });
```

**Implementation:**
- `configure()` method on Capture accepts options object with `constraint` function
- Constraint function receives the matched node and returns boolean
- Pattern matching evaluates constraints after structural matching succeeds
- Failed constraints cause pattern match to fail (return no match)
- Constraints stored on Capture object and evaluated during pattern matching phase
- TypeScript generic on Capture provides type-safe constraint function

**API Consistency:**

```typescript
// Pattern configuration (existing)
pattern`forwardRef(${capture('name')})`
    .configure({ imports: [...], dependencies: {...} });

// Capture configuration (new)
capture('invocation')
    .configure({ constraint: node => ... });
```

### 3. Rename `imports` to `context`

**Status**: 📝 Proposed

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

**Status**: 📝 Proposed

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

**Status**: 📝 Proposed

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

**Status**: 📝 Proposed

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
tmpl.apply(cursor, someTree, new Map([['nodeName', identifierNode]]));
```

**Rationale:**
- `capture()` implies pattern matching context, but templates can be used standalone
- `param()` more clearly indicates simple parameter substitution without matching
- Reduces cognitive load: users don't wonder "what am I capturing?"
- Aligns terminology with common template systems (parameters, not captures)
- Both functions work identically - difference is semantic clarity

**Comparison:**

```typescript
// Using capture() in standalone template - semantically unclear
const value = capture('value');
template`return ${value} * 2;`
    .apply(cursor, node, new Map([['value', someExpr]]));

// Using param() in standalone template - clearer intent
const value = param('value');
template`return ${value} * 2;`
    .apply(cursor, node, new Map([['value', someExpr]]));

// capture() still appropriate with patterns
const value = capture('value');
const pat = pattern`foo(${value})`;  // Pattern context makes "capture" make sense
const tmpl = template`bar(${value})`;
```

**Implementation:**
- `param()` is an alias for `capture()` - identical implementation
- Both return same `Capture` type, work interchangeably
- No runtime distinction - purely semantic naming
- Builder API could also offer `.param()` alongside `.capture()` for templates
- Type signature: `param<T = any>(name?: string): Capture<T> & T`

**Considerations:**

Should we allow mixing in the same template?
```typescript
const c = capture('fromPattern');
const p = param('standalone');
template`${c} + ${p}`  // Both work, but semantically confusing
```

Decision: Allow mixing (they're the same type), but document that:
- Use `capture()` when template is used with a pattern
- Use `param()` when template is standalone
- Don't mix both in the same template (confusing)

### 8. Ellipsis Patterns for Sequence Matching

**Status**: 📝 Proposed

Add support for matching variable-length sequences of nodes (statements, arguments, array elements) using ellipsis captures.

**Problem Statement:**

Current patterns can only match fixed-length sequences at specific positions. There's no way to:
- Match "any number of statements between two specific statements"
- Match "any arguments after the first two"
- Match "any number of elements in an array"
- Capture variable-length sequences

This is critical for real-world refactoring scenarios like:
- Finding try-catch blocks with specific setup/teardown regardless of body contents
- Matching function calls with variable arguments
- Finding loops that contain specific operations among other statements

**API:**

```typescript
/**
 * Creates an ellipsis capture that matches zero or more nodes in a sequence.
 *
 * @param name Optional name for the capture. If not provided, matches but doesn't capture.
 * @returns An Ellipsis capture that can match sequences
 */
export function ellipsis<T = any>(name?: string): Ellipsis<T>;

// Alternative concise alias (three underscores)
export const ___ = ellipsis;

interface Ellipsis<T = any> {
    name?: string;
    getName(): string | undefined;

    // Configuration for constraints
    configure(options: EllipsisOptions<T>): this;
}

interface EllipsisOptions<T> {
    // Minimum number of nodes to match (default: 0)
    min?: number;

    // Maximum number of nodes to match (default: unlimited)
    max?: number;

    // Constraint that each matched node must satisfy
    constraint?: (node: J) => boolean;

    // Constraint that the entire sequence must satisfy
    sequenceConstraint?: (nodes: J[]) => boolean;
}
```

**Examples:**

```typescript
// Example 1: Basic statement sequence matching
const middle = ellipsis('middle');
const pat = pattern`
    console.log("start");
    ${middle}
    console.log("end");
`;

// Matches:
// console.log("start"); console.log("end");
// console.log("start"); doSomething(); console.log("end");
// console.log("start"); doX(); doY(); doZ(); console.log("end");

// Example 2: Function arguments with ellipsis
const first = capture('first');
const rest = ellipsis('rest');
const pat = pattern`foo(${first}, ${rest})`;

// Matches: foo(1), foo(1, 2), foo(1, 2, 3, 4)

// Example 3: Anonymous ellipsis (match but don't capture)
const errorHandler = capture('handler');
const pat = pattern`
    try {
        ${ellipsis()}  // Match any statements, don't capture
    } catch (e) {
        ${errorHandler}
    }
`;

// Example 4: Constrained ellipsis
const middle = ellipsis('middle').configure({
    min: 1,
    sequenceConstraint: (nodes) => nodes.length > 0
});

// Example 5: Using ellipsis in templates
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

// Example 6: Concise alias
const pat = pattern`
    try {
        ${___()}
    } catch (e) {
        console.error(e);
    }
`;

// Example 7: Multiple ellipses in one pattern
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

// Example 8: Array element ellipsis
const first = capture('first');
const rest = ellipsis('rest');
const pat = pattern`[${first}, ${rest}]`;
```

**Rationale:**
- Essential for real-world refactoring patterns (not just toy examples)
- Common in other pattern matching systems (Semgrep uses `...`, Rust uses `..`, etc.)
- Enables matching code with variable structure
- Makes patterns more flexible and powerful
- Natural extension of existing capture concept

**Implementation Strategy:**

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

**Edge Cases:**

1. **Multiple ellipses in sequence**: Need greedy matching that doesn't consume too much
2. **Adjacent ellipses**: `pattern`${ellipsis('a')}${ellipsis('b')}`` - Should this be an error or use min/max to disambiguate?
3. **Ellipsis contexts**: Works for statement sequences, function arguments, array elements (object properties - future?)
4. **Empty matches**: Allowed by default (`min: 0`)
5. **Performance**: Backtracking can be expensive - consider caching and limiting depth

**Applicable Contexts:**
- Statement sequences in blocks (most common)
- Function/method arguments
- Array elements
- Future: Object properties, class members

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

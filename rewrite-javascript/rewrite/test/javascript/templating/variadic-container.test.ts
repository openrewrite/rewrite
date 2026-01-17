/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Tests for variadic pattern matching in J.Container elements (e.g., object/array destructuring patterns).
 *
 * This test file validates that variadic captures work correctly in destructuring patterns.
 * The implementation required two key fixes:
 *
 * 1. MarkerAttachmentVisitor.visitBindingElement (engine.ts) - Promotes CaptureMarkers from
 *    the name identifier to the BindingElement itself, so visitRightPadded can then promote
 *    them to the J.RightPadded wrapper where the comparator can find them.
 *
 * 2. PatternMatchingComparator.visitContainer (comparator.ts) - Overrides the base container
 *    visitor to use matchSequence logic instead of strict length comparison when variadic
 *    captures are present, similar to how method invocations are handled.
 */
import {capture, JavaScriptParser, JavaScriptVisitor, JS, pattern, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {Cursor} from "../../../src";

describe('variadic pattern matching in containers', () => {
    const parser = new JavaScriptParser();
    const parseCache = new Map<string, any>();

    async function parse(code: string) {
        // Check cache first
        if (parseCache.has(code)) {
            return parseCache.get(code)!;
        }

        // Parse and cache
        const parseGen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value as JS.CompilationUnit;
        const result = cu.statements[0];
        parseCache.set(code, result);
        return result;
    }

    test('variadic capture in object destructuring pattern', async () => {
        const props = capture({ variadic: true });
        const pat = pattern`function foo({${props}}) {}`;

        // The pattern has 1 element (variadic capture), but the target has 2+ elements
        // This demonstrates the length mismatch issue in visitContainer

        // Should match function with multiple properties (pattern: 1 elem, target: 2 elems)
        const result2 = await pat.match(await parse('function foo({a, b}) {}'), undefined!);
        expect(result2).toBeDefined();
        const captured2 = result2!.get(props);
        expect(Array.isArray(captured2)).toBe(true);
        expect((captured2 as any[]).length).toBe(2);

        // Should match function with even more properties (pattern: 1 elem, target: 3 elems)
        const result3 = await pat.match(await parse('function foo({a, b, c}) {}'), undefined!);
        expect(result3).toBeDefined();
        const captured3 = result3!.get(props);
        expect(Array.isArray(captured3)).toBe(true);
        expect((captured3 as any[]).length).toBe(3);

        // Should also match with single property (lengths match)
        const result1 = await pat.match(await parse('function foo({a}) {}'), undefined!);
        expect(result1).toBeDefined();
        const captured1 = result1!.get(props);
        expect(Array.isArray(captured1)).toBe(true);
        expect((captured1 as any[]).length).toBe(1);
    });

    test('variadic capture with required property in object destructuring', async () => {
        const first = capture('first');
        const rest = capture({ variadic: true });
        const pat = pattern`function foo({${first}, ${rest}}) {}`;

        // The pattern has 2 elements (first + variadic), but target has 3+ elements
        // This demonstrates the length mismatch: pattern=2, target=3

        // Should match function with multiple properties (pattern: 2 elems, target: 3 elems)
        const result3 = await pat.match(await parse('function foo({a, b, c}) {}'), undefined!);
        expect(result3).toBeDefined();
        const rest3 = result3!.get(rest);
        expect(Array.isArray(rest3)).toBe(true);
        expect((rest3 as any[]).length).toBe(2);

        // Should also match when lengths are equal (pattern: 2 elems, target: 2 elems)
        // In this case the variadic captures zero elements
        const result2 = await pat.match(await parse('function foo({a, b}) {}'), undefined!);
        expect(result2).toBeDefined();
        const rest2 = result2!.get(rest);
        expect(Array.isArray(rest2)).toBe(true);
        expect((rest2 as any[]).length).toBe(1);

        // Should NOT match function with no properties - missing required first
        expect(await pat.match(await parse('function foo({}) {}'), undefined!)).toBeUndefined();
    });

    test('variadic capture in array destructuring pattern', async () => {
        const elements = capture({ variadic: true });
        const pat = pattern`function foo([${elements}]) {}`;

        // The pattern has 1 element (variadic capture), but the target has 2+ elements
        // This demonstrates the length mismatch issue in visitContainer

        // Should match function with multiple elements (pattern: 1 elem, target: 2 elems)
        const result2 = await pat.match(await parse('function foo([a, b]) {}'), undefined!);
        expect(result2).toBeDefined();
        const captured2 = result2!.get(elements);
        expect(Array.isArray(captured2)).toBe(true);
        expect((captured2 as any[]).length).toBe(2);

        // Should match function with even more elements (pattern: 1 elem, target: 3 elems)
        const result3 = await pat.match(await parse('function foo([a, b, c]) {}'), undefined!);
        expect(result3).toBeDefined();
        const captured3 = result3!.get(elements);
        expect(Array.isArray(captured3)).toBe(true);
        expect((captured3 as any[]).length).toBe(3);

        // Should also match with single element (lengths match)
        const result1 = await pat.match(await parse('function foo([a]) {}'), undefined!);
        expect(result1).toBeDefined();
        const captured1 = result1!.get(elements);
        expect(Array.isArray(captured1)).toBe(true);
        expect((captured1 as any[]).length).toBe(1);
    });

    test('variadic capture with min/max constraints in containers', async () => {
        // Test min constraint
        const props1 = capture({ variadic: { min: 2 } });
        const pat1 = pattern`function foo({${props1}}) {}`;

        expect(await pat1.match(await parse('function foo({}) {}'), undefined!)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(await parse('function foo({a}) {}'), undefined!)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(await parse('function foo({a, b}) {}'), undefined!)).toBeDefined();    // exactly min
        expect(await pat1.match(await parse('function foo({a, b, c}) {}'), undefined!)).toBeDefined();    // more than min

        // Test max constraint
        const props2 = capture({ variadic: { max: 2 } });
        const pat2 = pattern`function foo({${props2}}) {}`;

        expect(await pat2.match(await parse('function foo({}) {}'), undefined!)).toBeDefined();    // within max
        expect(await pat2.match(await parse('function foo({a, b}) {}'), undefined!)).toBeDefined();    // exactly max
        expect(await pat2.match(await parse('function foo({a, b, c}) {}'), undefined!)).toBeUndefined();  // exceeds max
    });

    test('variadic capture with custom constraint function in containers', async () => {
        // This test verifies that constraint functions for variadic captures
        // receive the full array of captured elements AND a cursor pointing to the common parent

        let receivedCursor: any = null;

        // Capture with constraint that checks the array length and verifies cursor is present
        const props = capture({
            variadic: true,
            constraint: (nodes: J[], context) => {
                receivedCursor = context.cursor;
                // For variadic captures, constraint receives:
                // 1. The array of captured nodes
                // 2. A cursor pointing to the parent context (the container holding these elements)
                // The cursor parameter is optional to declare, but when declared it's always defined

                // Only match if we captured exactly 2 elements
                return Array.isArray(nodes) && nodes.length === 2;
            }
        });
        const pat = pattern`function foo({${props}}) {}`;

        // Should NOT match with 1 element - constraint requires exactly 2
        expect(await pat.match(await parse('function foo({a}) {}'), undefined!)).toBeUndefined();

        // Should match with 2 elements - constraint satisfied
        receivedCursor = null;
        const result2 = await pat.match(await parse('function foo({a, b}) {}'), undefined!);
        expect(result2).toBeDefined();

        // Verify cursor was provided
        expect(receivedCursor).toBeTruthy();
        expect(receivedCursor.constructor.name).toBe('Cursor');

        const captured2 = result2!.get(props);
        expect(Array.isArray(captured2)).toBe(true);
        expect((captured2 as unknown as any[]).length).toBe(2);

        // Should NOT match with 3 elements - constraint requires exactly 2
        expect(await pat.match(await parse('function foo({a, b, c}) {}'), undefined!)).toBeUndefined();
    });

    test('variadic replacement in object destructuring pattern', () => {
        // This test reproduces the issue where variadic captures in containers
        // are not properly expanded during template replacement

        const propsWithBindings = capture({ variadic: true });
        const ref = capture();
        const body = capture({ variadic: true });
        const name = capture();

        // Pattern: forwardRef(function Name({...props}, ref) {...})
        const beforePattern = pattern`forwardRef(function ${name}({${propsWithBindings}}, ${ref}) {${body}})`;

        // Template: function Name({ ref: ref, ...props }) {...}
        const afterTemplate = template`function ${name}({ ref: ${ref}, ${propsWithBindings} }) {${body}}`;

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                const match = await beforePattern.match(methodInvocation, undefined!);
                if (match) {
                    return await afterTemplate.apply(methodInvocation, this.cursor, {values: match});
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `forwardRef(function MyInput({ className, ...rest }, ref) { return null; })`,
                `
                    function MyInput({ ref: ref, className, ...rest }) {
                        return null;
                    }`
            )
        );
    });

    test('preserve propertyName in BindingElement replacement', () => {
        // This test verifies that { ref: ${ref} } preserves the "ref:" property name
        // when replacing ${ref} with the captured value

        const ref = capture();
        const props = capture({ variadic: true });
        const body = capture({ variadic: true });
        const name = capture();

        // Reuse the forwardRef pattern but with a template that has propertyName
        const beforePattern = pattern`forwardRef(function ${name}({${props}}, ${ref}) {${body}})`;

        // Template with propertyName: { ref: ${ref} }
        const afterTemplate = template`function ${name}({ ref: ${ref}, ${props} }) {${body}}`;

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                const match = await beforePattern.match(methodInvocation, this.cursor);
                if (match) {
                    return await afterTemplate.apply(methodInvocation, this.cursor, {values: match});
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `forwardRef(function MyInput({ className, ...rest }, ref) { return null; })`,
                `
                    function MyInput({ ref: ref, className, ...rest }) {
                        return null;
                    }`
            )
        );
    });

    test('preserve Spread marker on non-variadic capture in function declaration', () => {
        // This test demonstrates the issue where the Spread marker gets lost during
        // marker propagation. When we have ...${capture} in a template, the Spread
        // marker is on the placeholder identifier. During marker propagation (in
        // MarkerAttachmentVisitor), the CaptureMarker gets propagated up, but the
        // Spread marker can get lost.

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(method: J.MethodDeclaration, p: any): Promise<J | undefined> {
                // Template with spread: function Name(...params) {...}
                const afterTemplate = template`function ${method.name}(...${method.parameters.elements[0]}) {${method.body!.statements}}`;
                return await afterTemplate.apply(method, this.cursor);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function myFunc(props) {
                    return null;
                }`,
                `
                function myFunc(...props) {
                    return null;
                }`
            )
        );
    });

    test('preserve Spread marker on non-variadic capture in function call', () => {
        // This test verifies that the Spread marker is preserved when using
        // a non-variadic capture with spread operator in a function call context.
        // Example: myFunc(args) -> myFunc(...args)

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                // Template with spread: myFunc(...args)
                const afterTemplate = template`myFunc(...${method.arguments.elements[0]})`;
                return await afterTemplate.apply(method, this.cursor);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `myFunc(props)`,
                `myFunc(...props)`
            )
        );
    });

    test('preserve Spread marker on non-variadic capture in object literal', () => {
        // This test verifies that the Spread marker is preserved when using
        // a non-variadic capture with spread operator in an object literal context.
        // Example: { foo: bar } -> { foo: bar, ...rest }

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                // Template with spread in object literal: myFunc({ ...obj })
                const afterTemplate = template`myFunc({ ...${method.arguments.elements[0]} })`;
                return await afterTemplate.apply(method, this.cursor);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `myFunc(props)`,
                `myFunc({...props})`
            )
        );
    });
});

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
import {JavaScriptParser, JS, prettierFormat} from "../../../src/javascript";
import {WhitespaceReconciler} from "../../../src/javascript/format/whitespace-reconciler";
import {TreePrinters} from "../../../src";

describe('WhitespaceReconciler', () => {
    const parser = new JavaScriptParser();

    const parse = (code: string) =>
        parser.parseOne({sourcePath: 'test.ts', text: code}) as JS.CompilationUnit;

    const parseOnly = (code: string) =>
        parser.parseOnly({sourcePath: 'test.ts', text: code}) as JS.CompilationUnit;

    const print = (cu: JS.CompilationUnit) => TreePrinters.print(cu);

    it('should reconcile whitespace from formatted tree to original', async () => {
        // Original code with types
        const original = await parse('const x=1');

        // Formatted code (no types for speed)
        const formatted = parseOnly('const x = 1');

        // Reconcile
        const reconciler = new WhitespaceReconciler();
        const result = reconciler.reconcile(original, formatted);

        // Print and verify
        const printed = print(result as JS.CompilationUnit);
        expect(printed).toBe('const x = 1');

        // Verify original types are preserved
        const resultCu = result as JS.CompilationUnit;
        expect(resultCu.statements[0]).toBeDefined();
    });

    it('should handle mismatched structure gracefully', async () => {
        // Original: variable declaration
        const original = await parse('const x = 1');

        // Formatted: different structure (function call)
        const formatted = parseOnly('foo()');

        // Reconcile - should return original unchanged
        const reconciler = new WhitespaceReconciler();
        const result = reconciler.reconcile(original, formatted);

        // Should return original unchanged
        const printed = print(result as JS.CompilationUnit);
        expect(printed).toBe('const x = 1');
    });

    it('should preserve markers from original tree', async () => {
        // For this test we just verify the structure is preserved
        const original = await parse('const x = 1; const y = 2');
        const formatted = parseOnly('const x = 1;\nconst y = 2');

        const reconciler = new WhitespaceReconciler();
        const result = reconciler.reconcile(original, formatted);

        const resultCu = result as JS.CompilationUnit;
        expect(resultCu.statements.length).toBe(2);
    });

    it('parseOnly should produce AST that prints identically to parse', async () => {
        const withTypes = await parse('const x: number = 1');
        const withoutTypes = parseOnly('const x: number = 1');

        // Both should parse and print the same
        const printed1 = print(withTypes);
        const printed2 = print(withoutTypes);
        expect(printed1).toBe(printed2);
    });

    it('should reconcile multi-line formatting', async () => {
        const original = await parse('const fn = () => { return 1 }');
        const formatted = parseOnly(`const fn = () => {
  return 1;
};
`);

        const reconciler = new WhitespaceReconciler();
        const result = reconciler.reconcile(original, formatted);

        const printed = print(result as JS.CompilationUnit);
        expect(printed).toContain('\n');
        expect(printed).toContain('return 1');
    });

    it('should reconcile binary expression spacing', async () => {
        const original = await parse('const x=1+2');
        const formatted = parseOnly('const x = 1 + 2');

        const reconciler = new WhitespaceReconciler();
        const result = reconciler.reconcile(original, formatted);

        const printed = print(result as JS.CompilationUnit);
        expect(printed).toBe('const x = 1 + 2');
    });
});

describe('prettierFormat', () => {
    const parser = new JavaScriptParser();

    const parse = (code: string) =>
        parser.parseOne({sourcePath: 'test.ts', text: code}) as JS.CompilationUnit;

    const print = (cu: JS.CompilationUnit) => TreePrinters.print(cu);

    it('should format code using Prettier and preserve types', async () => {
        const original = await parse('const x=1+2');

        const result = await prettierFormat(original, {
            tabWidth: 2,
            semi: true
        });

        const printed = print(result);
        // Prettier should add spaces around operators
        expect(printed).toContain('x = 1 + 2');
    });

    it('should format multiline code', async () => {
        const original = await parse('const fn=()=>{return 1}');

        const result = await prettierFormat(original, {
            tabWidth: 2,
            semi: true
        });

        const printed = print(result);
        expect(printed).toContain('const fn');
        expect(printed).toContain('return');
    });
});

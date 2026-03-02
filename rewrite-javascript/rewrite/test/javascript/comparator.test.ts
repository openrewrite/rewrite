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
import {JavaScriptComparatorVisitor} from '../../src/javascript/comparator';
import {J} from '../../src/java';
import {JavaScriptParser, JS} from '../../src/javascript';

describe('JavaScriptComparatorVisitor', () => {
    const parser = new JavaScriptParser();
    const comparator = new JavaScriptComparatorVisitor();

    // Helper function to parse code and get the AST
    async function parse(code: string): Promise<JS.CompilationUnit> {
        const parseGenerator = parser.parse({text: code, sourcePath: 'test.ts'});
        return (await parseGenerator.next()).value as JS.CompilationUnit;
    }

    // Helper function to get the first statement from a compilation unit
    function getFirstStatement(cu: J): J {
        const jscu = cu as JS.CompilationUnit;
        return jscu.statements[0];
    }

    test('identical literals match', async () => {
        const ast1 = await parse('42;');
        const ast2 = await parse('42;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(true);
    });

    test('different literals do not match', async () => {
        const ast1 = await parse('42;');
        const ast2 = await parse('43;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('identical identifiers match', async () => {
        const ast1 = await parse('foo;');
        const ast2 = await parse('foo;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(true);
    });

    test('different identifiers do not match', async () => {
        const ast1 = await parse('foo;');
        const ast2 = await parse('bar;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('identical binary expressions match', async () => {
        const ast1 = await parse('1 + 2;');
        const ast2 = await parse('1 + 2;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(true);
    });

    test('binary expressions with different operators do not match', async () => {
        const ast1 = await parse('1 + 2;');
        const ast2 = await parse('1 - 2;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('binary expressions with different operands do not match', async () => {
        const ast1 = await parse('1 + 2;');
        const ast2 = await parse('1 + 3;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('identical blocks match', async () => {
        const ast1 = await parse('{ const a = 1; const b = 2; }');
        const ast2 = await parse('{ const a = 1; const b = 2; }');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(true);
    });

    test('blocks with different statements do not match', async () => {
        const ast1 = await parse('{ const a = 1; const b = 2; }');
        const ast2 = await parse('{ const a = 1; const b = 3; }');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('blocks with different number of statements do not match', async () => {
        const ast1 = await parse('{ const a = 1; const b = 2; }');
        const ast2 = await parse('{ const a = 1; }');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('identical compilation units match', async () => {
        const ast1 = await parse('const a = 1;\nconst b = 2;');
        const ast2 = await parse('const a = 1;\nconst b = 2;');

        expect(await comparator.compare(ast1, ast2)).toBe(true);
    });

    test('compilation units with different statements do not match', async () => {
        const ast1 = await parse('const a = 1;\nconst b = 2;');
        const ast2 = await parse('const a = 1;\nconst b = 3;');

        expect(await comparator.compare(ast1, ast2)).toBe(false);
    });

    test('compilation units with different number of statements do not match', async () => {
        const ast1 = await parse('const a = 1;\nconst b = 2;');
        const ast2 = await parse('const a = 1;');

        expect(await comparator.compare(ast1, ast2)).toBe(false);
    });

    test('nodes of different kinds do not match', async () => {
        const ast1 = await parse('1;');
        const ast2 = await parse('foo;');

        const stmt1 = getFirstStatement(ast1);
        const stmt2 = getFirstStatement(ast2);

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('complex expressions match when identical', async () => {
        const ast1 = await parse('function foo(a, b) { return a + b * (c - d); }');
        const ast2 = await parse('function foo(a, b) { return a + b * (c - d); }');

        expect(await comparator.compare(ast1, ast2)).toBe(true);
    });

    test('complex expressions do not match when different', async () => {
        const ast1 = await parse('function foo(a, b) { return a + b * (c - d); }');
        const ast2 = await parse('function foo(a, b) { return a + b * (c + d); }');

        expect(await comparator.compare(ast1, ast2)).toBe(false);
    });
});
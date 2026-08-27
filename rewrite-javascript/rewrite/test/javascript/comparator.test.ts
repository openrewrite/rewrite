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
import {JavaScriptParser, JS, sourceFileCache} from '../../src/javascript';

describe('JavaScriptComparatorVisitor', () => {
    const parser = new JavaScriptParser({sourceFileCache});
    const comparator = new JavaScriptComparatorVisitor();

    // Helper function to parse code and get the AST
    async function parse(code: string): Promise<JS.CompilationUnit> {
        const parseGenerator = parser.parse({text: code, sourcePath: 'test.ts'});
        return (await parseGenerator.next()).value as JS.CompilationUnit;
    }

    // Helper function to get the first statement from a compilation unit
    function getFirstStatement(cu: J): J {
        const jscu = cu as JS.CompilationUnit;
        return jscu.statements[0].element;
    }

    test('structurally identical trees match', async () => {
        const src = 'function foo(a, b) { return a + b * (c - d); }';
        expect(await comparator.compare(await parse(src), await parse(src))).toBe(true);
    });

    test('nodes of different kinds do not match', async () => {
        const stmt1 = getFirstStatement(await parse('1;'));
        const stmt2 = getFirstStatement(await parse('foo;'));

        expect(await comparator.compare(stmt1, stmt2)).toBe(false);
    });

    test('a differing scalar property does not match', async () => {
        const literals = [getFirstStatement(await parse('42;')), getFirstStatement(await parse('43;'))];
        expect(await comparator.compare(literals[0], literals[1])).toBe(false);

        const identifiers = [getFirstStatement(await parse('foo;')), getFirstStatement(await parse('bar;'))];
        expect(await comparator.compare(identifiers[0], identifiers[1])).toBe(false);

        // The operator is left-padded rather than a plain field, so it takes its own comparison path
        const operators = [getFirstStatement(await parse('1 + 2;')), getFirstStatement(await parse('1 - 2;'))];
        expect(await comparator.compare(operators[0], operators[1])).toBe(false);
    });

    test('lists of differing length do not match', async () => {
        expect(await comparator.compare(
            await parse('const a = 1;\nconst b = 2;'),
            await parse('const a = 1;')
        )).toBe(false);
    });

    test('a difference nested deep in the tree does not match', async () => {
        expect(await comparator.compare(
            await parse('function foo(a, b) { return a + b * (c - d); }'),
            await parse('function foo(a, b) { return a + b * (c + d); }')
        )).toBe(false);
    });
});

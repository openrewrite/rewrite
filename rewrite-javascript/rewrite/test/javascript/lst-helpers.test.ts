/*
 * Copyright 2026 the original author or authors.
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
import {
    amdBlockOf,
    functionExpressionOf,
    hasEsmSyntax,
    isAssignmentTarget,
    isCommonJs,
    isDirective,
    isFunctionOrClassExpression,
    isLeftmostChild,
    isObjectLiteral,
    isTypeScript,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    leftmostChild,
    leftmostExpression,
    prologueLength,
    withFactoryBody,
    withStringValue
} from "../../src/javascript";
import {isTextComment, J} from "../../src/java";
import {TreePrinters} from "../../src/print";
import {Cursor} from "../../src/tree";

const parser = new JavaScriptParser();

async function parse(source: string, sourcePath = 'test.js'): Promise<JS.CompilationUnit> {
    return (await parser.parse({text: source, sourcePath}).next()).value as JS.CompilationUnit;
}

function initializerOf(cu: JS.CompilationUnit, index = 0): J {
    const declaration = cu.statements[index].element as J.VariableDeclarations;
    return declaration.variables[0].element.initializer!.element;
}

function expressionOf(cu: JS.CompilationUnit, index = 0): J {
    const statement = cu.statements[index].element;
    return statement.kind === JS.Kind.ExpressionStatement ? (statement as JS.ExpressionStatement).expression : statement;
}

async function assignmentTargets(source: string): Promise<string[]> {
    const targets: string[] = [];
    const record = async (cursor: Cursor, node: J) => {
        if (isAssignmentTarget(cursor)) {
            targets.push((await TreePrinters.printer(cursor).print(node)).trim());
        }
    };
    await new class extends JavaScriptVisitor<undefined> {
        override async visitAssignment(assignment: J.Assignment, p: undefined): Promise<J | undefined> {
            await record(this.cursor, assignment);
            return super.visitAssignment(assignment, p);
        }

        override async visitNewArray(newArray: J.NewArray, p: undefined): Promise<J | undefined> {
            await record(this.cursor, newArray);
            return super.visitNewArray(newArray, p);
        }

        override async visitFieldAccess(fieldAccess: J.FieldAccess, p: undefined): Promise<J | undefined> {
            await record(this.cursor, fieldAccess);
            return super.visitFieldAccess(fieldAccess, p);
        }
    }().visit(await parse(source), undefined);
    return targets;
}

describe('isObjectLiteral', () => {
    test('an object literal is one and a constructor call is not', async () => {
        // given
        const cu = await parse(`const a = {x: 1};\nconst b = new Foo();`);

        // when
        const answers = [isObjectLiteral(initializerOf(cu, 0)), isObjectLiteral(initializerOf(cu, 1))];

        // then
        expect(answers).toEqual([true, false]);
    });
});

describe('isFunctionOrClassExpression and functionExpressionOf', () => {
    test('a function expression is unwrapped, a class expression is not, an arrow is neither', async () => {
        // given
        const cu = await parse(`const f = function named() {};\nconst C = class {};\nconst g = () => 1;`);
        const [fn, cls, arrow] = [0, 1, 2].map(i => initializerOf(cu, i));

        // when
        const expressions = [fn, cls, arrow].map(isFunctionOrClassExpression);
        const unwrapped = [fn, cls, arrow].map(functionExpressionOf);

        // then
        expect(expressions).toEqual([true, true, false]);
        expect((unwrapped[0]?.name as J.Identifier).simpleName).toBe('named');
        expect(unwrapped.slice(1)).toEqual([undefined, undefined]);
    });
});

describe('leftmostExpression, leftmostChild and isLeftmostChild', () => {
    test('the left spine leads to the token the expression starts with', async () => {
        // given
        const cu = await parse(`a.b().c + 1;`);
        const binary = expressionOf(cu) as J.Binary;

        // when
        const leftmost = leftmostExpression(binary);
        const child = leftmostChild(binary);

        // then
        expect((leftmost as J.Identifier).simpleName).toBe('a');
        expect(child?.id).toBe(binary.left.id);
        expect(isLeftmostChild(binary, binary.left)).toBe(true);
        expect(isLeftmostChild(binary, binary.right)).toBe(false);
    });

    test('a prefix unary starts with a token of its own', async () => {
        // given
        const cu = await parse(`-a;`);

        // when
        const child = leftmostChild(expressionOf(cu));

        // then
        expect(child).toBeUndefined();
    });
});

describe('hasEsmSyntax and isCommonJs', () => {
    test('syntax decides an ambiguous extension and the extension decides the rest', async () => {
        // given
        const esm = await parse(`import a from 'a';`);
        const commonJs = await parse(`function f() { return require('a'); }`);
        const mjsWithoutSyntax = await parse(`const a = 1;`, 'test.mjs');
        const cjs = await parse(`const a = 1;`, 'test.cjs');

        // when
        const answers = [esm, commonJs, mjsWithoutSyntax, cjs].map(cu => [hasEsmSyntax(cu), isCommonJs(cu)]);

        // then
        expect(answers).toEqual([[true, false], [false, true], [false, false], [false, true]]);
    });

    test('a top-level `for await` or `await using` is module syntax, one in a function is not', async () => {
        // given
        const sources = [
            `for await (const x of xs) {}`,
            `await using r = open();`,
            `async function f() { for await (const x of xs) {} await using r = open(); }`
        ];

        // when
        const answers = await Promise.all(sources.map(async source => hasEsmSyntax(await parse(source))));

        // then
        expect(answers).toEqual([true, true, false]);
    });
});

describe('isDirective and prologueLength', () => {
    test('a file and a function body each open with their prologue', async () => {
        // given
        const cu = await parse(`#!/usr/bin/env node\n'use strict';\n"use client";\n('x');\nfunction f() { 'use strict'; g(); }`);
        const body = (cu.statements[4].element as J.MethodDeclaration).body!;

        // when
        const fileLength = prologueLength(cu);
        const bodyLength = prologueLength(body);

        // then
        expect(fileLength).toBe(3);
        expect(bodyLength).toBe(1);
        expect(isDirective(cu.statements[3].element)).toBe(false);
    });
});

describe('withStringValue', () => {
    test('keeps single quotes and escapes what the quotes need', async () => {
        // given
        const literal = expressionOf(await parse(`'a';`)) as J.Literal;

        // when
        const updated = withStringValue(literal, `it's\\a\nb`);

        // then
        expect(updated.value).toBe(`it's\\a\nb`);
        expect(updated.valueSource).toBe(`'it\\'s\\\\a\\nb'`);
        expect(literal.value).toBe('a');
    });

    test('falls back to double quotes', async () => {
        // given
        const literal = expressionOf(await parse('`a`;')) as J.Literal;

        // when
        const updated = withStringValue(literal, 'b');

        // then
        expect(updated.valueSource).toBe('"b"');
    });
});

describe('isTextComment', () => {
    test('a line comment is a text comment', async () => {
        // given
        const cu = await parse(`f();\n// note\ng();`);

        // when
        const comments = cu.statements[1].element.prefix.comments;

        // then
        expect(comments.map(isTextComment)).toEqual([true]);
    });
});

describe('withFactoryBody', () => {
    test('edits a function factory body in place', async () => {
        // given
        const cu = await parse(`define(['a'], function (a) {\n    a();\n});`);
        const call = expressionOf(cu) as J.MethodInvocation;
        const block = amdBlockOf(call)!;

        // when
        const updated = withFactoryBody(call, block, body => ({...body, statements: []}));

        // then
        const edited: JS.CompilationUnit = {...cu, statements: [{...cu.statements[0], element: updated}]};
        expect(await TreePrinters.print(edited)).toBe(`define(['a'], function (a) {\n});`);
    });

    test('leaves an arrow factory without braces alone', async () => {
        // given
        const cu = await parse(`define(['a'], (a) => a);`);
        const call = expressionOf(cu) as J.MethodInvocation;
        const edit = vi.fn((body: J.Block) => body);

        // when
        const updated = withFactoryBody(call, amdBlockOf(call)!, edit);

        // then
        expect(updated).toBe(call);
        expect(edit).not.toHaveBeenCalled();
    });
});

describe('isAssignmentTarget', () => {
    test('assignment targets and their destructuring parts are, declarations and values are not', async () => {
        // when
        const targets = await assignmentTargets(
            `x.y = 1;\n[a = a || b] = list;\nfor ([c = 1] of lists) {}\nconst [d = 1] = list;\nf([e = 1]);`
        );

        // then
        expect(targets).toEqual(['x.y', '[a = a || b]', 'a = a || b', 'c = 1']);
    });
});

describe('isTypeScript', () => {
    test('reads the extension of a path or a file', async () => {
        // given
        const cu = await parse(`const a = 1;`, 'src/a.mts');

        // when
        const answers = [cu, 'a.tsx', 'a.d.ts', 'a.cts', 'a.js', 'a.mjs'].map(isTypeScript);

        // then
        expect(answers).toEqual([true, true, true, true, false, false]);
    });
});

// noinspection JSUnusedLocalSymbols

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
import {RecipeSpec} from "../../src/test";
import {JavaScriptParser, JavaScriptVisitor, JS, typescript} from "../../src/javascript";
import {J} from "../../src/java";
import {ParseErrorKind} from "../../src/parse-error";


test("comments", () =>
    new RecipeSpec().rewriteRun({
        //language=typescript
        ...typescript(`
            /*1*/
            const /*2*/ x /*3*/ = /*4*/ 10;/*5*/
            /*6*/
            const y = 5 /*7*/; /*8*/
        `),
        afterRecipe: async (cu: JS.CompilationUnit) => {
            let commentCount = 0;
            const checkSpaces = new class extends JavaScriptVisitor<void> {
                public override async visitSpace(space: J.Space, p: void): Promise<J.Space> {
                    const ret = await super.visitSpace(space, p);
                    expect(ret.whitespace).not.toContain("/*");
                    commentCount += ret.comments.length;
                    return ret;
                }
            }
            await checkSpaces.visit(cu, undefined);
            expect(commentCount).toBe(8);
        }
    }));

describe('parseOnly', () => {
    test('parses basic TypeScript code', async () => {
        const parser = new JavaScriptParser();
        const result = await parser.parseOnly({
            sourcePath: 'test.ts',
            text: 'const x: number = 1 + 2;'
        });

        expect(result.kind).toBe(JS.Kind.CompilationUnit);
        const cu = result as JS.CompilationUnit;
        expect(cu.statements.length).toBe(1);
    });

    test('parses JSX code', async () => {
        const parser = new JavaScriptParser();
        const result = await parser.parseOnly({
            sourcePath: 'test.tsx',
            text: 'const element = <div className="test">Hello</div>;'
        });

        expect(result.kind).toBe(JS.Kind.CompilationUnit);
    });

    test('returns ParseExceptionResult for syntax errors', async () => {
        const parser = new JavaScriptParser();
        const result = await parser.parseOnly({
            sourcePath: 'test.ts',
            text: 'const x = {;'  // Invalid syntax
        });

        expect(result.kind).toBe(ParseErrorKind);
    });

    test('parses without type attribution', async () => {
        const parser = new JavaScriptParser();
        const result = await parser.parseOnly({
            sourcePath: 'test.ts',
            text: `
                interface User { name: string; }
                const user: User = { name: "test" };
            `
        });

        expect(result.kind).toBe(JS.Kind.CompilationUnit);
        const cu = result as JS.CompilationUnit;
        // Types are not attributed in parseOnly
        expect(cu.statements.length).toBe(2);
    });

    test('preserves source path', async () => {
        const parser = new JavaScriptParser();
        const result = await parser.parseOnly({
            sourcePath: 'src/components/Button.tsx',
            text: 'export const Button = () => <button />;'
        });

        expect(result.kind).toBe(JS.Kind.CompilationUnit);
        const cu = result as JS.CompilationUnit;
        expect(cu.sourcePath).toBe('src/components/Button.tsx');
    });
});

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
import {
    bindingNames,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    namesDeclaredIn,
    namesInScope,
    Scope,
    scopeOf,
    sourceFileCache
} from "../../src/javascript";
import {J} from "../../src/java";
import {Cursor} from "../../src/tree";

const parser = new JavaScriptParser({sourceFileCache});

async function parse(source: string, sourcePath = 'test.ts'): Promise<JS.CompilationUnit> {
    return (await parser.parse({text: source, sourcePath}).next()).value as JS.CompilationUnit;
}

/** The position where the source calls `anchor()`. */
async function cursorAtAnchor(source: string, sourcePath?: string): Promise<Cursor> {
    const found: Cursor[] = [];
    await new class extends JavaScriptVisitor<undefined> {
        override async visitMethodInvocation(method: J.MethodInvocation, p: undefined): Promise<J | undefined> {
            if ((method.name as J.Identifier)?.simpleName === 'anchor') {
                found.push(this.cursor);
            }
            return super.visitMethodInvocation(method, p);
        }
    }().visit(await parse(source, sourcePath), undefined);
    expect(found).toHaveLength(1);
    return found[0];
}

async function scopeAtAnchor(source: string, sourcePath?: string): Promise<Scope> {
    return scopeOf(await cursorAtAnchor(source, sourcePath));
}

async function namesAtAnchor(source: string, sourcePath?: string): Promise<string[]> {
    return [...namesInScope(await cursorAtAnchor(source, sourcePath))].sort();
}

describe('scopeOf', () => {
    test('a module binds every form of import', async () => {
        expect(await namesAtAnchor(`
            import def from 'a';
            import {named, other as renamed} from 'b';
            import * as ns from 'c';
            import 'd';
            anchor();
        `)).toEqual(['def', 'named', 'ns', 'renamed']);
    });

    test('a module binds what its declarations name', async () => {
        expect(await namesAtAnchor(`
            const c = 1;
            let l = 2;
            var v = 3, w = 4;
            function f() {}
            class K {}
            namespace N {}
            type T = string;
            const {r} = require('m');
            anchor();
        `)).toEqual(['K', 'N', 'T', 'c', 'f', 'l', 'r', 'v', 'w']);
    });

    test('a var reaches the anchor from a sibling block, a let does not', async () => {
        const names = await namesAtAnchor(`
            function f() {
                if (x) {
                    var hoisted = 1;
                    let confined = 2;
                }
                anchor();
            }
        `);
        expect(names).toContain('hoisted');
        expect(names).not.toContain('confined');
    });

    test('a block binds its declarations wherever the anchor sits in it', async () => {
        // `const` binds its whole block, so referencing it above its declaration is a temporal dead
        // zone error rather than a reference to whatever the name means outside.
        expect(await namesAtAnchor(`
            function f() {
                anchor();
                const later = 1;
            }
        `)).toContain('later');
    });

    test("a switch's cases share one scope", async () => {
        expect(await namesAtAnchor(`
            switch (x) {
                case 1:
                    let s = 1;
                    break;
                case 2:
                    anchor();
            }
        `)).toContain('s');
    });

    test('a function binds its parameters, destructured and rest included', async () => {
        expect(await namesAtAnchor(`
            function f(plain, {prop, other: renamed, nested: {deep}}, [first], ...rest) {
                anchor();
            }
        `)).toEqual(['deep', 'f', 'first', 'plain', 'prop', 'renamed', 'rest']);
    });

    test('an arrow function binds its parameters', async () => {
        expect(await namesAtAnchor(`const f = (a, {b}) => anchor();`)).toEqual(['a', 'b', 'f']);
    });

    test('a catch binds its parameter in its block alone', async () => {
        expect(await namesAtAnchor(`try {} catch ({message}) { anchor(); }`)).toContain('message');

        expect(await namesAtAnchor(`try {} catch (err) {} anchor();`)).not.toContain('err');
    });

    test('a loop binds what its control declares', async () => {
        expect(await namesAtAnchor(`for (let i = 0; i < 2; i++) { anchor(); }`)).toContain('i');

        expect(await namesAtAnchor(`for (const [key, value] of pairs) { anchor(); }`))
            .toEqual(expect.arrayContaining(['key', 'value']));

        expect(await namesAtAnchor(`for (const prop in obj) { anchor(); }`)).toContain('prop');
    });

    test('a class binds its own name, not its members', async () => {
        // Only a class expression's name is bound solely inside the class — the block holding a
        // declaration binds its name too.
        const names = await namesAtAnchor(`
            const C = class K {
                member() { anchor(); }
            };
        `);
        expect(names).toContain('K');
        expect(names).not.toContain('member');
    });

    test('a nested function keeps its declarations to itself', async () => {
        const names = await namesAtAnchor(`
            function outer() {
                function inner(param) {
                    var hidden = 1;
                }
                anchor();
            }
        `);
        expect(names).toEqual(['inner', 'outer']);
    });

    test('a function expression binds the name it gives itself, and only there', async () => {
        expect(await namesAtAnchor(`const f = function named() { anchor(); };`)).toContain('named');

        expect(await namesAtAnchor(`function f() { (function named() {})(); anchor(); }`)).not.toContain('named');
    });

    test('a scope reaches through the JSX between it and the cursor', async () => {
        // A handler prop is where a template most often lands in a component, and JSX is all the
        // cursor crosses to get from the lambda binding `evt` out to the module.
        expect(await namesAtAnchor(
            `import R from 'r';\nfunction App() { return <button onClick={(evt) => anchor()}>x</button>; }`,
            'test.tsx'
        )).toEqual(['App', 'R', 'evt']);
    });

    test('a binding pattern names the property each name reads', async () => {
        const cu = await parse(`const {plain, other: renamed, nested: {deep}, ...rest} = o;`);
        const declaration = cu.statements[0].element as J.VariableDeclarations;
        expect(bindingNames(declaration.variables[0].element.name)).toEqual([
            {name: 'plain', member: 'plain'},
            {name: 'renamed', member: 'other'},
            // Neither reads a property of the object the pattern destructures.
            {name: 'deep'},
            {name: 'rest'}
        ]);
    });

    test('declares answers for a name any enclosing scope binds', async () => {
        const scope = await scopeAtAnchor(`import {merge} from 'm';\nfunction f(param) { anchor(); }`);
        expect(scope.declares('merge')).toBe(true);
        expect(scope.declares('param')).toBe(true);
        expect(scope.declares('absent')).toBe(false);
    });

    test('declaringScope names the innermost scope binding a name, not merely one that does', async () => {
        const shadowed = await scopeAtAnchor(
            `import B from 'a/B';\nfunction inner() { { var B = 1; } anchor(); }`);
        // A `var` reaches the whole function, so the function binds it, not the block holding it.
        expect(shadowed.declaringScope('B')?.kind).toBe(J.Kind.MethodDeclaration);
        expect(shadowed.declaringScope('absent')).toBeUndefined();

        const reachable = await scopeAtAnchor(`import B from 'a/B';\nfunction inner() { anchor(); }`);
        expect(reachable.declaringScope('B')?.kind).toBe(JS.Kind.CompilationUnit);
    });

    test('a var belongs to the function it sits in, a let to the block', async () => {
        const hoisting = await scopeAtAnchor(`function f(p) { var p = 1; anchor(); }`);
        expect(hoisting.declaringScope('p')?.kind).toBe(J.Kind.MethodDeclaration);

        const blockScoped = await scopeAtAnchor(`function f(p) { let p = 1; anchor(); }`);
        expect(blockScoped.declaringScope('p')?.kind).toBe(J.Kind.Block);
    });
});

describe('namesDeclaredIn', () => {
    test('a name declared in any scope is a name the file has', async () => {
        expect([...namesDeclaredIn(await parse(`
            import imported from 'm';
            const top = 1;
            function fn(param) {
                if (c) { var nested = 2; }
                try {} catch (caught) {}
                for (const [element] of pairs) {}
                return (inline) => inline;
            }
        `))].sort()).toEqual(['caught', 'element', 'fn', 'imported', 'inline', 'nested', 'param', 'top']);
    });

    test('a class member is reached through an instance, so its name is not the file\'s', async () => {
        // What a member's own code declares is still a name the file has.
        expect([...namesDeclaredIn(await parse(`
            class K {
                merge(param) { const local = 1; }
                field = (bound) => bound;
            }
        `))].sort()).toEqual(['K', 'bound', 'local', 'param']);
    });
});

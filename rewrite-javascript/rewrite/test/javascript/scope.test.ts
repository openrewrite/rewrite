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
    isReference,
    namesDeclaredIn,
    namesDeclaredWithin,
    namesReferencedWithin,
    namesUsedWithin,
    resolve,
    Scope,
    scopeOf,
    sourceFileCache,
    walk
} from "../../src/javascript";
import {J, NameTree} from "../../src/java";
import {Cursor} from "../../src/tree";
import {randomId} from "../../src/uuid";

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
    const names = new Set<string>();
    scopeOf(await cursorAtAnchor(source, sourcePath)).walk(scope => {
        for (const name of scope.names()) {
            names.add(name);
        }
        return true;
    });
    return [...names].sort();
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

    test('a namespace hoists the functions and vars in its body to itself, not to the file', async () => {
        const inside = await scopeAtAnchor('namespace NS { function f() {} var v = 1; anchor(); }');
        expect(inside.declaringScope('f')?.kind).toBe(JS.Kind.NamespaceDeclaration);
        expect(inside.declaringScope('v')?.kind).toBe(JS.Kind.NamespaceDeclaration);

        const outside = await scopeAtAnchor('namespace NS { function f() {} }\nanchor();');
        expect(outside.declares('f')).toBe(false);
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

    test('a function or class expression binds the name it gives itself, and only there', async () => {
        expect(await namesAtAnchor(`const f = function named() { anchor(); };`)).toContain('named');

        expect(await namesAtAnchor(`function f() { (function named() {})(); anchor(); }`)).not.toContain('named');

        expect(await namesAtAnchor(`const C = class Named { m() { anchor(); } };`)).toContain('Named');

        expect(await namesAtAnchor(`const C = class Named {}; anchor();`)).not.toContain('Named');
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

    test('a value reference is shadowed by a value, not by a type of the same name', async () => {
        const scope = await scopeAtAnchor(`
            import type {Imported} from 'm';
            import {type Specified} from 'm';
            interface Object {}
            type Math = {};
            interface Merged {}
            const Merged = 1;
            anchor();
        `);
        // A binding still collides with a type of the same name, so every kind counts unless asked otherwise.
        expect(scope.declares('Object')).toBe(true);
        expect(scope.declares('Object', 'type')).toBe(true);
        // An import binds its name with every meaning: `typeof Imported` reads it as a value from a type.
        expect(['Imported', 'Specified', 'Object', 'Math'].filter(name => scope.declares(name, 'value')))
            .toEqual(['Imported', 'Specified']);
        expect(scope.declares('Merged', 'value') && scope.declares('Merged', 'type')).toBe(true);
        expect(scope.declares('Object', 'namespace')).toBe(false);
        expect(scope.declaringScope('Object', 'value')).toBeUndefined();
    });

    test('an object or type literal binds none of its members, around it or inside it', async () => {
        expect(await namesAtAnchor(`const o = { foo() { anchor(); }, get bar() { return 1; } };`))
            .toEqual(['o']);
        expect(await namesAtAnchor(`
            function f() {
                g({ method() {} });
                type T = { property: string; signature(): void };
                anchor();
            }
        `)).toEqual(['T', 'f']);
    });

    test('a var belongs to the function it sits in, a let to the block', async () => {
        const hoisting = await scopeAtAnchor(`function f(p) { var p = 1; anchor(); }`);
        expect(hoisting.declaringScope('p')?.kind).toBe(J.Kind.MethodDeclaration);

        const blockScoped = await scopeAtAnchor(`function f(p) { let p = 1; anchor(); }`);
        expect(blockScoped.declaringScope('p')?.kind).toBe(J.Kind.Block);
    });
});

describe('Scope', () => {
    test('each scope binds its own names, out to the compilation unit', async () => {
        const bound: string[][] = [];
        (await scopeAtAnchor('const top = 1;\nfunction fn(param) { const local = 2; anchor(); }'))
            .walk(scope => (bound.push([...scope.names()].sort()), true));

        expect(bound).toEqual([['local'], ['param'], ['fn', 'top']]);
    });

    test('walking outward visits the innermost scope first and stops where the caller says', async () => {
        const visited: string[][] = [];
        (await scopeAtAnchor('const top = 1;\nfunction fn(param) { const local = 2; anchor(); }'))
            .walk(scope => (visited.push([...scope.names()]), visited.length < 2));

        expect(visited).toEqual([['local'], ['param']]);
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

    test('an object or type literal member is reached through a value, so its name is not the file\'s', async () => {
        expect([...namesDeclaredIn(await parse(`
            const o = {
                undefined() { const local = 1; },
                get getter() { return 1; }
            };
            let t: { undefined: string; signature(): void };
        `))].sort()).toEqual(['local', 'o', 't']);
    });

    test('a type parameter is a name that shadows, so the file declares it', async () => {
        expect([...namesDeclaredIn(await parse(`
            import {Node} from 'm';
            function sortKeys<Bound extends Node>(node: Bound) {}
            class Holder<Owned> {}
        `))].sort()).toEqual(['Bound', 'Holder', 'Node', 'Owned', 'node', 'sortKeys'].sort());
    });
});

describe('namesReferencedWithin', () => {
    test('a value hides only value reads of its name, a type only type reads', async () => {
        // A name declared in the kind its use does not read reaches past it, one in the same kind does not.
        const fn = (await parse(`
            function f() {
                const Cast = 1, Satisfied = 1, Argument = 1, Implemented = 1, Asserted = 1;
                const Aliased = 1, Indexed = 1, ClassIndexed = 1, Bound = 1, Key = 1;
                type Computed = 1;
                const SameValue = 1;
                interface SameType {}
                interface Queried {}
                x as Cast;
                x satisfies Satisfied;
                g<Argument>();
                class K implements Implemented {}
                <Asserted>x;
                let q: typeof Queried;
                type A = Aliased;
                interface I { [key: number]: Indexed }
                function h<P extends Bound>() {}
                class C { [k: string]: ClassIndexed }
                let m: { [Computed]: Key };
                SameValue;
                let s: SameType;
                function isBound(subject: unknown): subject is Bound { return true; }
            }
        `)).statements[0].element;

        const referenced = [...namesReferencedWithin(fn)];
        expect(referenced).toEqual(expect.arrayContaining([
            'Aliased', 'Argument', 'Asserted', 'Bound', 'Cast', 'ClassIndexed', 'Computed', 'Implemented',
            'Indexed', 'Key', 'Queried', 'Satisfied'
        ]));
        expect(referenced).not.toContain('SameValue');
        expect(referenced).not.toContain('SameType');
        expect(referenced).not.toContain('subject');
    });

    test('a type parameter binds across the declaration carrying it, for type reads alone', async () => {
        const statements = (await parse(`
            function f<Fn>(x: Fn): Fn { let y: Fn; return x; }
            class K<Cls> extends Base<Cls> implements I<Cls> { x: Cls; m(): Cls { return this.x; } }
            type A<Alias> = Alias[];
            const g = <Arrow,>(x: Arrow): Arrow => x;
            type F = <FnType>(x: FnType) => FnType;
            type M<Keys extends string> = {[Mapped in Keys as \`x\${Mapped}\`]: Mapped};
            type U<T> = T extends Promise<infer Inferred> ? Inferred : never;
            type V<T> = T extends Promise<infer Hidden> ? never : Hidden;
            type W = Checked extends Promise<infer Checked> ? never : never;
            type P = (Param: any) => Param is Foo;
            const h = (Pred: unknown): Pred is string => true;
            function w<Value>() { return Value; }
        `)).statements;

        const referenced = [...namesReferencedWithin(statements)];
        for (const name of ['Fn', 'Cls', 'Alias', 'Arrow', 'FnType', 'Keys', 'Mapped', 'Inferred', 'Param', 'Pred']) {
            expect(referenced).not.toContain(name);
        }
        // An `infer` name reaches neither the check type nor the false branch, and a type parameter
        // binds no value.
        expect(referenced).toEqual(expect.arrayContaining(['Hidden', 'Checked', 'Value']));
    });

    test('a binding pattern names what it binds, and reads only its defaults and computed keys', async () => {
        // Asked of the statements alone, so nothing binds the pattern's names and a read of one would show.
        const statements = (await parse(`
            const {Defaulted = Fallback, [Computed]: Keyed, ...Rest} = source;
            const [First, ...Others] = list;
        `)).statements;

        const referenced = [...namesReferencedWithin(statements)];
        expect(referenced).toEqual(expect.arrayContaining(['Fallback', 'Computed', 'source', 'list']));
        for (const name of ['Defaulted', 'Keyed', 'Rest', 'First', 'Others']) {
            expect(referenced).not.toContain(name);
        }
    });

    test('a qualified type name reads a namespace, a space of its own beside values and types', async () => {
        // A namespace sits in a namespace or module, so the outer one here is what a function is elsewhere.
        const ns = (await parse(`
            namespace Outer {
                interface Iface {}
                type Alias = 1;
                const Value = 1;
                class Klass {}
                namespace Hidden { export type T = 1 }
                namespace TypesOnly { export type T = 1; interface I {} const enum CE { A } namespace Inner { type U = 1 } }
                namespace Deep { namespace Inner { export const v = 1 } }
                namespace Dotted.Inner { export const v = 1 }
                namespace Qualified.Inner { export type T = 1 }
                enum Enum { A }
                let a: Iface.T;
                let b: Alias.T;
                let c: Value.T;
                let d: Klass.T;
                let e: Hidden.T;
                TypesOnly.go();
                let g: Enum.A;
                Deep.go();
                Dotted.go();
            }
        `)).statements[0].element;

        const referenced = [...namesReferencedWithin(ns)];
        expect(referenced).toEqual(expect.arrayContaining(['Iface', 'Alias', 'Value', 'Klass', 'TypesOnly']));
        expect(referenced).not.toContain('Hidden');
        expect(referenced).not.toContain('Enum');
        expect(referenced).not.toContain('Deep');
        expect(referenced).not.toContain('Dotted');
        // A dotted declaration names its root, as much as a plain one.
        expect(referenced).not.toContain('Qualified');
    });
});

describe('namesUsedWithin', () => {
    test('a subtree spells what it reads, not only what it declares', async () => {
        const fn = (await parse('const outer = 1;\nfunction fn() { return ambient(outer); }')).statements[1].element;

        expect([...namesUsedWithin(fn)].sort()).toEqual(['ambient', 'fn', 'outer']);
        expect([...namesDeclaredWithin(fn)].sort()).toEqual(['fn']);
    });

    test('a subtree that is absent holds no names', async () => {
        // What a caller reaches these with: a method declared without a body.
        expect([namesUsedWithin(undefined).size, namesDeclaredWithin(undefined).size]).toEqual([0, 0]);
    });
});

describe('walk', () => {
    test('visits LST nodes only, so a cyclic type graph never traps it', async () => {
        // A method returning its own class makes the type graph cyclic; the LST it hangs off is not.
        const cu = await parse('class A { m(): A { return this; } }\nconst a = new A(); a.m().m();');
        const offTree: unknown[] = [];
        const seen = new Set<unknown>();
        let visited = 0;
        walk(cu, node => {
            visited++;
            seen.add(node);
            // Padding carries markers but no id, and a JavaType carries neither.
            if (!('id' in node && 'markers' in node)) {
                offTree.push(node.kind);
            }
            return true;
        });

        expect(offTree).toEqual([]);
        expect(visited).toBe(seen.size);
        expect(seen.size).toBeGreaterThan(10);
    });
});

describe('resolve', () => {
    test('a read resolves to the scope binding what it reads, so a nearer binding of another kind hides nothing', async () => {
        const source = `
            import {Imported} from 'm';
            namespace NS { export type Foo = 1 }
            function f<T>(a: T): T {
                interface Imported {}
                let n: NS.Foo;
                let t: T;
                const o = {Imported};
                return o.Imported ? Imported.go(a) : a;
            }
            function g() { const Imported = 1; let y: Imported; Imported: for (;;) { break Imported; } }
            type U<X> = X extends Promise<infer I> ? I : I;
            let i: import('other').Imported;
            import a = NS.Foo;
            export {NS};
        `;
        const cu = JS.Kind.CompilationUnit;
        // In order: the specifier, the interface, the shorthand, the property key, the receiver, the
        // const, the type, the label, the break and the qualified import type. Four read a binding
        // the file reaches.
        expect(await resolvedAt(source, 'Imported'))
            .toEqual([undefined, undefined, cu, undefined, cu, undefined, cu, undefined, undefined, undefined]);
        // The declaration, the qualifier in a type, the right-hand side of an import alias, and an
        // export clause, which reads a name with whatever meaning it has.
        expect(await resolvedAt(source, 'NS')).toEqual([undefined, cu, cu, cu]);
        expect((await resolvedAt(source, 'T')).sort()).toEqual([...Array(3).fill(J.Kind.MethodDeclaration), undefined]);
        // An `infer` name reaches the true branch alone, so the false branch reads past it to nothing.
        expect(await resolvedAt(source, 'I')).toEqual([undefined, JS.Kind.ConditionalType, undefined]);
    });

    test('asked from a call, the callee\'s receiver resolves as it would from its own position', async () => {
        const receiverAt = async (source: string) => {
            const answers: (string | undefined)[] = [];
            await new class extends JavaScriptVisitor<undefined> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: undefined): Promise<J | undefined> {
                    const select = method.select?.element;
                    if (select?.kind === J.Kind.Identifier) {
                        answers.push(resolve(this.cursor, select as J.Identifier)?.kind);
                    }
                    return super.visitMethodInvocation(method, p);
                }
            }().visit(await parse(source), undefined);
            return answers;
        };
        expect(await receiverAt('Object.assign({}, o);')).toEqual([undefined]);
        expect(await receiverAt('function f() { const Object = {}; Object.assign({}, o); }')).toEqual([J.Kind.Block]);
        expect(await receiverAt('interface Object {}\nObject.assign({}, o);')).toEqual([undefined]);
    });

    test('an identifier the tree does not hold is refused, not read as a global', async () => {
        const cu = await parse('const x = 1; use(x);');
        const call = cu.statements[1].element as J.MethodInvocation;
        const read = call.arguments.elements[0].element as J.Identifier;
        const cursor = new Cursor(call, new Cursor(cu));

        expect(resolve(cursor, read)).toBe(cu);
        // A copy keeps its id, so it still stands for the tree's identifier; a fresh id stands for nothing in it.
        expect(resolve(cursor, {...read})).toBe(cu);
        expect(() => resolve(cursor, {...read, id: randomId()})).toThrow(/not in the tree/);
    });

    test('an identifier standing at two positions is refused, not resolved as the last of them', async () => {
        const cu = await parse('use(x); use(x);');
        const second = cu.statements[1].element as J.MethodInvocation;
        const shared = (cu.statements[0].element as J.MethodInvocation).arguments.elements[0];
        const doubled = {
            ...cu,
            statements: [cu.statements[0], {
                ...cu.statements[1],
                element: {...second, arguments: {...second.arguments, elements: [shared]}}
            }]
        } as JS.CompilationUnit;

        expect(() => resolve(new Cursor(doubled), shared.element as J.Identifier)).toThrow(/two positions/);
    });
});

/** The kind of the scope `resolve` answers for each occurrence of `name` in `source`, from the identifier's own cursor. */
async function resolvedAt(source: string, name: string): Promise<(string | undefined)[]> {
    const answers: (string | undefined)[] = [];
    await new class extends JavaScriptVisitor<undefined> {
        override async visitIdentifier(identifier: J.Identifier, p: undefined): Promise<J | undefined> {
            if (identifier.simpleName === name) {
                answers.push(resolve(this.cursor, identifier)?.kind);
            }
            return identifier;
        }
        protected override async visitTypeName<N extends NameTree>(nameTree: N, p: undefined): Promise<N> {
            return await this.visit(nameTree, p) as N;
        }
    }().visit(await parse(source), undefined);
    expect(answers.length).toBeGreaterThan(0);
    return answers;
}

/** What `isReference` answers for each occurrence of `target` in `source`. */
async function targetsReference(source: string, sourcePath?: string): Promise<boolean[]> {
    const answers: boolean[] = [];
    await new class extends JavaScriptVisitor<undefined> {
        override async visitIdentifier(identifier: J.Identifier, p: undefined): Promise<J | undefined> {
            if (identifier.simpleName === 'target') {
                answers.push(isReference(this.cursor, identifier));
            }
            return identifier;
        }
    }().visit(await parse(source, sourcePath), undefined);
    expect(answers.length).toBeGreaterThan(0);
    return answers;
}

describe('isReference', () => {
    test('a name its parent introduces, or one from a namespace of its own, is not a reference', async () => {
        for (const source of ['const o = {target: 1};', 'o.target;', 'target: while (c) { break target; }']) {
            expect(await targetsReference(source)).not.toContain(true);
        }

        expect(await targetsReference('<E target="v"/>;', 'test.tsx')).not.toContain(true);
    });

    test('a read, and the callee of a call selecting nothing, are references', async () => {
        for (const source of ['use(target);', 'target();']) {
            expect(await targetsReference(source)).not.toContain(false);
        }
    });

    test('a type position reads alike to a value, and an import declares what it spells', async () => {
        expect(await targetsReference('let v: target;')).toEqual([true]);
        expect(await targetsReference("import {target, a as target2} from 'm';\nexport {target as published};"))
            .toEqual([false, true]);
    });

    test('a shorthand property answers as the name it is, though it also reads the binding', async () => {
        expect(await targetsReference('const o = {target};')).toEqual([false]);
    });

    test('a computed key and a default value read from within a naming slot', async () => {
        expect(await targetsReference('const {[target]: a, b = target} = o;')).toEqual([true, true]);
    });
});

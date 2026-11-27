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
import * as fs from "node:fs";
import * as os from "node:os";
import * as path from "node:path";
import {JavaScriptParser, JavaScriptVisitor, JS} from "../../src/javascript";
import {J, Type} from "../../src/java";

interface Identities {
    calls: Record<string, string[]>;
    declarations: Record<string, string[]>;
}

function identity(method: Type.Method | undefined): string {
    if (!method) {
        return '∅';
    }
    const static_ = method.flags & Type.Flag.Static ? ' static' : '';
    return `${Type.FullyQualified.getFullyQualifiedName(method.declaringType)}#${method.name}${static_}`;
}

async function identities(files: Record<string, string>): Promise<Identities> {
    const project = fs.mkdtempSync(path.join(os.tmpdir(), "rewrite-source-declaration-identity-"));
    const found: Identities = {calls: {}, declarations: {}};
    try {
        for (const [file, text] of Object.entries(files)) {
            const target = path.join(project, file);
            fs.mkdirSync(path.dirname(target), {recursive: true});
            fs.writeFileSync(target, text);
        }
        const parser = new JavaScriptParser({relativeTo: project});
        const inputs = Object.keys(files).filter(f => /\.[cm]?[jt]s$/.test(f)).map(f => path.join(project, f));
        for await (const sourceFile of parser.parse(...inputs)) {
            const calls: string[] = found.calls[sourceFile.sourcePath] = [];
            const declarations: string[] = found.declarations[sourceFile.sourcePath] = [];
            await new class extends JavaScriptVisitor<void> {
                protected override async visitMethodInvocation(method: J.MethodInvocation, p: void) {
                    calls.push(identity(method.methodType));
                    return super.visitMethodInvocation(method, p);
                }

                protected override async visitFunctionCall(functionCall: JS.FunctionCall, p: void) {
                    calls.push(identity(functionCall.methodType));
                    return super.visitFunctionCall(functionCall, p);
                }

                protected override async visitMethodDeclaration(method: J.MethodDeclaration, p: void) {
                    declarations.push(identity(method.methodType));
                    return super.visitMethodDeclaration(method, p);
                }

                protected override async visitArrowFunction(arrowFunction: JS.ArrowFunction, p: void) {
                    declarations.push(identity(arrowFunction.methodType));
                    return super.visitArrowFunction(arrowFunction, p);
                }
            }().visit(sourceFile, undefined);
        }
    } finally {
        fs.rmSync(project, {recursive: true, force: true});
    }
    return found;
}

describe('a call into parsed source is attributed as the declaration it calls is', () => {

    test('a module re-exported with module.exports = require() is called as the function it exports', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "express", version: "1.0.0", main: "index.js"}),
            "index.js": `module.exports = require('./lib/express');`,
            "lib/express.js": `exports = module.exports = createApplication;\nfunction createApplication() {}`,
            "test/app.js": `var express = require('../');\nexpress();\nrequire('../')();`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["lib/express.js"]).toEqual(["lib/express#createApplication static"]);
        expect(calls["test/app.js"].filter(call => !call.endsWith("#require"))).toEqual([
            "lib/express#createApplication static",
            "lib/express#createApplication static"
        ]);
    });

    test('an exported arrow function is declared and called on its file under its variable name', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "ky", version: "1.0.0", type: "module"}),
            "tsconfig.json": JSON.stringify({compilerOptions: {module: "nodenext", moduleResolution: "nodenext", strict: true}}),
            "source/index.ts": `export const helper = (x: number) => x + 1;\n[1].map(n => n);`,
            "test/main.ts": `import {helper} from '../source/index.js';\nhelper(1);`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["source/index.ts"]).toEqual(["source/index#helper static", "∅"]);
        expect(calls["test/main.ts"]).toEqual(["source/index#helper static"]);
    });

    test('a default-exported variable holding a callable is called under its own name', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "ky", version: "1.0.0", type: "module"}),
            "tsconfig.json": JSON.stringify({compilerOptions: {module: "nodenext", moduleResolution: "nodenext", strict: true}}),
            "source/index.ts": `type KyInstance = (input: string) => void;\nconst createInstance = (): KyInstance => () => {};\nconst ky = createInstance();\nexport default ky;`,
            "test/main.ts": `import ky from '../source/index.js';\nky('a');`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["source/index.ts"]).toEqual(["source/index#createInstance static", "∅"]);
        expect(calls["source/index.ts"]).toEqual(["source/index#createInstance static"]);
        expect(calls["test/main.ts"]).toEqual(["source/index#ky static"]);
    });

    test('an arrow function imported through a tsconfig paths alias is called on its file', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "zustand", version: "1.0.0"}),
            "tsconfig.json": JSON.stringify({
                compilerOptions: {
                    baseUrl: ".", module: "esnext", moduleResolution: "bundler", strict: true,
                    paths: {"zustand/*": ["./src/*.ts"]}
                }
            }),
            "src/vanilla.ts": `type CreateStore = <T>(init: T) => T;\nexport const createStore = (<T>(init: T): T => init) as CreateStore;\nexport const plain = <T>(init: T): T => init;`,
            "tests/basic.test.ts": `import {createStore, plain} from 'zustand/vanilla';\ncreateStore(1);\nplain(2);`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/vanilla.ts"]).toEqual(["src/vanilla#createStore static", "src/vanilla#plain static"]);
        expect(calls["tests/basic.test.ts"]).toEqual(["src/vanilla#createStore static", "src/vanilla#plain static"]);
    });

    test('a function expression is named by the variable or the export it is assigned to', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "lib", version: "1.0.0"}),
            "lib/fns.js": `const bound = function () {};\nexports.exported = function () {};\nexports.arrow = () => {};\nmodule.exports.named = function own() {};`,
            "test/fns.js": `const {exported, arrow, named} = require('../lib/fns');\nexported();\narrow();\nnamed();`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["lib/fns.js"]).toEqual([
            "lib/fns#bound static",
            "lib/fns#exported static",
            "lib/fns#arrow static",
            "lib/fns#own static"
        ]);
        expect(calls["test/fns.js"].filter(call => !call.endsWith("#require"))).toEqual([
            "lib/fns#exported static",
            "lib/fns#arrow static",
            "lib/fns#own static"
        ]);
    });

    test('an anonymous default export is declared and called as its file\'s default', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "lib", version: "1.0.0", type: "module"}),
            "tsconfig.json": JSON.stringify({compilerOptions: {module: "nodenext", moduleResolution: "nodenext", strict: true}}),
            "src/arrow.ts": `export default (x: number) => x;`,
            "src/fn.ts": `export default function (x: number) { return x; }`,
            "src/named.ts": `export default function named(x: number) { return x; }`,
            "test/main.ts": `import arrow from '../src/arrow.js';\nimport fn from '../src/fn.js';\nimport named from '../src/named.js';\narrow(1);\nfn(2);\nnamed(3);`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/arrow.ts"]).toEqual(["src/arrow#<default> static"]);
        expect(declarations["src/fn.ts"]).toEqual(["src/fn#<default> static"]);
        expect(declarations["src/named.ts"]).toEqual(["src/named#named static"]);
        expect(calls["test/main.ts"]).toEqual(["src/arrow#<default> static", "src/fn#<default> static", "src/named#named static"]);
    });

    test('a function assigned to module.exports or export = is declared and called as its file\'s default', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "lib", version: "1.0.0"}),
            "lib/fn.js": `module.exports = function () {};`,
            "lib/arrow.js": `exports = module.exports = () => {};`,
            "lib/own.js": `module.exports = function own() {};`,
            "src/equals.ts": `export = (x: number) => x;`,
            "test/main.js": `const fn = require('../lib/fn');\nconst arrow = require('../lib/arrow');\nconst own = require('../lib/own');\nfn();\narrow();\nown();`,
            "test/equals.ts": `import equals = require('../src/equals');\nequals(1);`
        };

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["lib/fn.js"]).toEqual(["lib/fn#<default> static"]);
        expect(declarations["lib/arrow.js"]).toEqual(["lib/arrow#<default> static"]);
        expect(declarations["lib/own.js"]).toEqual(["lib/own#own static"]);
        expect(declarations["src/equals.ts"]).toEqual(["src/equals#<default> static"]);
        expect(calls["test/main.js"].filter(call => !call.endsWith("#require"))).toEqual([
            "lib/fn#<default> static",
            "lib/arrow#<default> static",
            "lib/own#own static"
        ]);
        expect(calls["test/equals.ts"].at(-1)).toEqual("src/equals#<default> static");
    });

    test('a call of a function a local variable holds is declared on its file, as a nested function declaration is', async () => {
        // given
        const files = {
            "package.json": JSON.stringify({name: "lib", version: "1.0.0", type: "module"}),
            "tsconfig.json": JSON.stringify({compilerOptions: {module: "nodenext", moduleResolution: "nodenext", strict: true}}),
            "src/local.ts": `export function outer(cb: () => void) {\n    const arrow = () => 1;\n    const fn = function () {};\n    function nested() {}\n    arrow();\n    fn();\n    nested();\n    cb();\n}`
        };

        // when
        const {calls} = await identities(files);

        // then
        expect(calls["src/local.ts"].slice(0, 3)).toEqual(["src/local#arrow", "src/local#fn", "src/local#nested"]);
        expect(calls["src/local.ts"][3]).not.toMatch(/^src\/local#/);
    });
});

describe('a function implementing a member of a named type is declared on that member, as a call through the type is', () => {
    const project = (files: Record<string, string>) => ({
        "package.json": JSON.stringify({name: "lib", version: "1.0.0", type: "module"}),
        "tsconfig.json": JSON.stringify({compilerOptions: {module: "nodenext", moduleResolution: "nodenext", strict: true}}),
        ...files
    });

    test('a local variable\'s function is declared on its file under the variable\'s name, as a call of it is', async () => {
        // given
        const files = project({
            "src/local.ts": `export function outer() {\n    const arrow = () => 1;\n    const cast = (() => 2) as () => number;\n    const typed: () => number = function () { return 3; };\n    arrow();\n    cast();\n    typed();\n    [1].map(n => n);\n}`
        });

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/local.ts"]).toEqual([
            "src/local#outer static",
            "src/local#arrow",
            "src/local#cast",
            "src/local#typed",
            "∅"
        ]);
        expect(calls["src/local.ts"].slice(0, 3)).toEqual(["src/local#arrow", "src/local#cast", "src/local#typed"]);
    });

    test('a function assigned to a property of an aliased type is declared on the alias, through mapped types and an intersection with any', async () => {
        // given
        const files = project({
            "src/types.ts": `export type Inst = { (input: string): number; create(x: string): Inst; extend: (x: string) => Inst };`,
            "src/index.ts": `import type {Inst} from './types.js';\ntype Mutable<T> = {-readonly [K in keyof T]: T[K]};\nconst createInstance = (): Inst => {\n    const inst: Partial<Mutable<Inst>> = {};\n    inst.create = (x: string) => createInstance();\n    const loose: Partial<Inst> & any = {};\n    loose.extend = (x: string) => createInstance();\n    return inst as Inst;\n};\nexport default createInstance();`,
            "test/main.ts": `import inst from '../src/index.js';\ninst.create('a');\ninst.extend('b');`
        });

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/types.ts"]).toEqual(["∅", "src/types.Inst#create"]);
        expect(declarations["src/index.ts"]).toEqual([
            "src/index#createInstance static",
            "src/types.Inst#create",
            "src/types.Inst#extend"
        ]);
        expect(calls["test/main.ts"]).toEqual(["src/types.Inst#create", "src/types.Inst#extend"]);
    });

    test('a method or a property function of an object literal is declared on its contextual generic interface, also through a cast', async () => {
        // given
        const files = project({
            "src/types.ts": `export interface StoreApi<T> { setState(v: T): void; getState: () => T }`,
            "src/index.ts": `import type {StoreApi} from './types.js';\nexport const literalStore = <T>(t: T): StoreApi<T> => ({ setState(v) {}, getState: () => t });\nexport const castStore = () => ({ setState(v: number) {}, getState: function () { return 1; } }) as StoreApi<number>;`,
            "test/main.ts": `import {literalStore, castStore} from '../src/index.js';\nliteralStore(1).setState(2);\nliteralStore(1).getState();\ncastStore().setState(3);\ncastStore().getState();`
        });

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/types.ts"]).toEqual(["src/types.StoreApi#setState"]);
        expect(declarations["src/index.ts"]).toEqual([
            "src/index#literalStore static",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState",
            "src/index#castStore static",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState"
        ]);
        expect(calls["test/main.ts"].filter(call => !call.startsWith("src/index#"))).toEqual([
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState"
        ]);
    });

    test('a local function implements the member its annotation indexes, or the one a contextually typed object literal holds it as', async () => {
        // given
        const files = project({
            "src/types.ts": `export interface StoreApi<T> { setState(v: T): void; getState: () => T }`,
            "src/vanilla.ts": `import type {StoreApi} from './types.js';\nexport const createStore = <T>(initial: T): StoreApi<T> => {\n    let state = initial;\n    const setState: StoreApi<T>['setState'] = (v) => { state = v; };\n    const getState = () => state;\n    const api: StoreApi<T> = {setState, getState};\n    return api;\n};`,
            "test/main.ts": `import {createStore} from '../src/vanilla.js';\nconst s = createStore(1);\ns.setState(2);\ns.getState();\nconst {setState, getState} = createStore(1);\nsetState(3);\ngetState();`
        });

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/vanilla.ts"]).toEqual([
            "src/vanilla#createStore static",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState"
        ]);
        expect(calls["test/main.ts"].filter(call => !call.startsWith("src/vanilla#"))).toEqual([
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState"
        ]);
    });

    test('a local function implements the member a typed object literal holds it as, wherever an untyped literal also holds it', async () => {
        // given
        const files = project({
            "src/types.ts": `export interface StoreApi { setState(v: number): void; getState: () => number }`,
            "src/vanilla.ts": `import type {StoreApi} from './types.js';\nexport const createStore = (): StoreApi => {\n    const setState = (v: number) => {};\n    const getState = () => 1;\n    const debug = {setState};\n    const api: StoreApi = {setState, getState};\n    const trace = {getState};\n    return api;\n};`
        });

        // when
        const {declarations} = await identities(files);

        // then
        expect(declarations["src/vanilla.ts"]).toEqual([
            "src/vanilla#createStore static",
            "src/types.StoreApi#setState",
            "src/types.StoreApi#getState"
        ]);
    });

    test('a local function assigned to a property of a named type implements that member', async () => {
        // given
        const files = project({
            "src/types.ts": `export type Inst = { create(x: string): Inst };`,
            "src/index.ts": `import type {Inst} from './types.js';\nexport const createInstance = (): Inst => {\n    const inst: Partial<Inst> = {};\n    const create = (x: string) => createInstance();\n    inst.create = create;\n    return inst as Inst;\n};`
        });

        // when
        const {declarations} = await identities(files);

        // then
        expect(declarations["src/index.ts"]).toEqual([
            "src/index#createInstance static",
            "src/types.Inst#create"
        ]);
    });

    test('a member of a type literal typing a property of a named type is declared on the type and the property', async () => {
        // given
        const files = project({
            "src/types.ts": `export interface Store { persist: { rehydrate: () => void } }\nexport type WithPersist<S> = S extends object ? { persist: { hasHydrated(): boolean } } : never;`,
            "src/index.ts": `import type {Store, WithPersist} from './types.js';\nexport const store = (): Store => ({ persist: { rehydrate() {} } });\nexport const persisted = () => {\n    const api = {};\n    (api as WithPersist<{}>).persist = { hasHydrated: () => true };\n    return api as WithPersist<{}>;\n};`,
            "test/main.ts": `import {store, persisted} from '../src/index.js';\nstore().persist.rehydrate();\npersisted().persist.hasHydrated();`
        });

        // when
        const {calls, declarations} = await identities(files);

        // then
        expect(declarations["src/types.ts"]).toEqual(["src/types.WithPersist.persist#hasHydrated"]);
        expect(declarations["src/index.ts"]).toEqual([
            "src/index#store static",
            "src/types.Store.persist#rehydrate",
            "src/index#persisted static",
            "src/types.WithPersist.persist#hasHydrated"
        ]);
        expect(calls["test/main.ts"].filter(call => !call.startsWith("src/index#"))).toEqual([
            "src/types.Store.persist#rehydrate",
            "src/types.WithPersist.persist#hasHydrated"
        ]);
    });

    test('an object literal with no contextual type, or one a library declares, keeps its members on its file', async () => {
        // given
        const files = project({
            "src/plain.ts": `export const plain = () => ({ use() {}, arrow: () => 1 });\nexport const iterator: Iterator<number> = { next() { return {done: true, value: 0}; } };`
        });

        // when
        const {declarations} = await identities(files);

        // then
        expect(declarations["src/plain.ts"]).toEqual([
            "src/plain#plain static",
            "src/plain#use",
            "src/plain#arrow",
            "src/plain#next"
        ]);
    });
});

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
});

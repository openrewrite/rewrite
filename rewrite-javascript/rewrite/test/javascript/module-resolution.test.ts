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
import * as fs from "node:fs";
import * as os from "node:os";
import * as path from "node:path";
import {JavaScriptParser, JavaScriptVisitor} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {SourceFile} from "../../src";

async function parseProject(files: Record<string, string>, entry: string): Promise<SourceFile> {
    const project = fs.mkdtempSync(path.join(os.tmpdir(), "rewrite-module-resolution-"));
    try {
        for (const [file, text] of Object.entries(files)) {
            const target = path.join(project, file);
            fs.mkdirSync(path.dirname(target), {recursive: true});
            fs.writeFileSync(target, text);
        }
        const parser = new JavaScriptParser({relativeTo: project});
        for await (const parsed of parser.parse(path.join(project, entry))) {
            return parsed;
        }
        throw new Error("parser yielded nothing");
    } finally {
        fs.rmSync(project, {recursive: true, force: true});
    }
}

async function firstInvocationReturnType(sourceFile: SourceFile): Promise<Type | undefined> {
    let found: Type | undefined;
    const finder = new class extends JavaScriptVisitor<void> {
        protected override async visitMethodInvocation(method: J.MethodInvocation, p: void) {
            found ??= method.methodType?.returnType;
            return super.visitMethodInvocation(method, p);
        }
    };
    await finder.visit(sourceFile, undefined);
    return found;
}

test("types published only through exports conditions are attributed", async () => {
    const exportsOnly = await parseProject({
        "node_modules/exports-only/package.json": JSON.stringify({
            name: "exports-only",
            version: "1.0.0",
            exports: {".": {types: "./idx.d.ts", default: "./idx.js"}}
        }),
        "node_modules/exports-only/idx.d.ts": `export declare function greet(): string;`,
        "node_modules/exports-only/idx.js": `exports.greet = () => "";`,
        "uses.ts": `import {greet} from "exports-only";\ngreet();`
    }, "uses.ts");
    expect(await firstInvocationReturnType(exportsOnly)).toEqual(Type.Primitive.String);

    // Node16 would match this condition against the importing file's own format and find nothing.
    const importConditionOnly = await parseProject({
        "node_modules/import-condition-only/package.json": JSON.stringify({
            name: "import-condition-only",
            version: "1.0.0",
            type: "module",
            exports: {import: {types: "./idx.d.ts", default: "./idx.js"}}
        }),
        "node_modules/import-condition-only/idx.d.ts": `export declare function greet(): string;`,
        "node_modules/import-condition-only/idx.js": `export const greet = () => "";`,
        "uses.ts": `import {greet} from "import-condition-only";\ngreet();`
    }, "uses.ts");
    expect(await firstInvocationReturnType(importConditionOnly)).toEqual(Type.Primitive.String);
});

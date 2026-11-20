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
import {JavaScriptParser, JavaScriptVisitor} from "../../src/javascript";
import {J, Type} from "../../src/java";

async function methodTypes(text: string, sourcePath: string): Promise<Type.Method[]> {
    const found: Type.Method[] = [];
    await new class extends JavaScriptVisitor<void> {
        protected override async visitMethodDeclaration(method: J.MethodDeclaration, p: void) {
            expect(method.methodType).toBeDefined();
            found.push(method.methodType!);
            return super.visitMethodDeclaration(method, p);
        }
    }().visit(await new JavaScriptParser().parseOne({text, sourcePath}), undefined);
    return found;
}

describe("a class expression", () => {
    test("declares its methods, as it declares its constructor", async () => {
        // As a bundler writes a class: assigned to a variable of its name and exported apart from it.
        const [constructor, rpc] = await methodTypes(`
var PostgrestClient = class PostgrestClient {
    constructor(url) { this.url = url; }
    rpc(fn, args = {}) { return this.url + fn; }
};
exports.PostgrestClient = PostgrestClient;
`, "dist/index.cjs");
        expect(rpc.name).toBe("rpc");
        expect(Type.FullyQualified.getFullyQualifiedName(rpc.declaringType)).toBe("PostgrestClient");
        expect(rpc.declaringType).toBe(constructor.declaringType);
    });

    test("with no name of its own declares its methods too", async () => {
        const [save] = await methodTypes(`export const Saver = class { save(): void {} };`, "saver.ts");
        expect(save.declaringType).not.toBe(Type.unknownType);
    });
});

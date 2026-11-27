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

async function methodTypes(text: string): Promise<Type.Method[]> {
    const found: Type.Method[] = [];
    await new class extends JavaScriptVisitor<void> {
        protected override async visitMethodDeclaration(method: J.MethodDeclaration, p: void) {
            expect(method.methodType).toBeDefined();
            found.push(method.methodType!);
            return super.visitMethodDeclaration(method, p);
        }
    }().visit(await new JavaScriptParser().parseOne({text, sourcePath: "accessors.ts"}), undefined);
    return found;
}

describe("accessor method types", () => {
    test("a getter and a setter are methods named after their property", async () => {
        const [getter, setter] = await methodTypes(`
export class Box {
    get size(): number { return 1; }
    set size(value: number) {}
}
`);
        expect(getter).toMatchObject({name: "size", parameterNames: [], returnType: Type.Primitive.Double});
        expect(setter).toMatchObject({name: "size", parameterNames: ["value"], parameterTypes: [Type.Primitive.Double]});
        expect(Type.FullyQualified.getFullyQualifiedName(getter.declaringType)).toBe("accessors.Box");
        expect(getter.flags & Type.Flag.Static).toBeFalsy();
    });

    test("a static accessor is static", async () => {
        const [getter] = await methodTypes(`export class Box { static get size() { return 1; } }`);
        expect(getter.flags & Type.Flag.Static).toBeTruthy();
    });

    test("an accessor is declared by a class expression", async () => {
        const [getter] = await methodTypes(`export const Box = class Named { get size() { return 1; } };`);
        expect(getter.name).toBe("size");
        expect(Type.FullyQualified.getFullyQualifiedName(getter.declaringType)).toContain("Named");
    });

    test("an accessor is declared by an object literal", async () => {
        const [getter] = await methodTypes(`export const box = { get size() { return 1; } };`);
        expect(getter.name).toBe("size");
        expect(Type.FullyQualified.getFullyQualifiedName(getter.declaringType)).toBe(Type.OBJECT_TYPE_NAME);
    });

    test("a private accessor keeps the name it is written with", async () => {
        const [getter] = await methodTypes(`export class Box { get #size() { return 1; } }`);
        expect(getter.name).toBe("#size");
    });
});

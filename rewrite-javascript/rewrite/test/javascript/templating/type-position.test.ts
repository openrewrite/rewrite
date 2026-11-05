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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, JavaScriptVisitor, JS, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

/** Replaces every `Old` identifier with `code`, as a recipe replacing a deprecated type does. */
function replaceOld(code: TemplateStringsArray) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitIdentifier(identifier: J.Identifier, p: any): Promise<J | undefined> {
            const visited = await super.visitIdentifier(identifier, p) as J.Identifier;
            return visited.simpleName === "Old" ? await template(code).apply(visited, this.cursor) ?? visited : visited;
        }
    });
}

/** Replaces each type argument `t` but a union with `${t} | null`, as a recipe making a ref nullable does. */
function nullableTypeArguments() {
    const t = capture('t');
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        protected override async postVisit(tree: J, p: any): Promise<J | undefined> {
            const parent = this.cursor.parentTree()?.value as J;
            return parent?.kind === J.Kind.ParameterizedType && tree.kind !== JS.Kind.Union &&
                (parent as J.ParameterizedType).typeParameters?.elements.some(e => e.element.id === tree.id) ?
                await template`${t} | null`.apply(tree, this.cursor, {values: {t: tree}}) : tree;
        }
    });
}

describe("template replacing a type", () => {
    const spec = new RecipeSpec();

    test("a union replacing a type annotation is a union type", () => {
        spec.recipe = replaceOld`number | string`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: Old;
                function f(x: Old): Old { return x; }
            `,
            `
                let a: number | string;
                function f(x: number | string): number | string { return x; }
            `
        ));
    });

    test("a union replacing a type argument of an import type, a type query or a call is a union type", () => {
        spec.recipe = replaceOld`number | string`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: import('react').RefObject<Old>;
                let b: typeof f<Old>;
                let c = handlers[key]<Old>();
            `,
            `
                let a: import('react').RefObject<number | string>;
                let b: typeof f<number | string>;
                let c = handlers[key]<number | string>();
            `
        ));
    });

    test("a union replacing an index type, a key remapping or a tuple rest is a union type", () => {
        spec.recipe = replaceOld`number | string`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: T[Old];
                type B = {[K in keyof T as Old]: T[K]};
                let c: [boolean, ...Old];
            `,
            `
                let a: T[number | string];
                type B = {[K in keyof T as number | string]: T[K]};
                let c: [boolean, ...number | string];
            `
        ));
    });

    test("a union is parenthesized where its slot binds tighter", () => {
        spec.recipe = replaceOld`number | string`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: Old[];
                let b: keyof Old;
                let c: Old & {id: number};
                let d: Old["length"];
            `,
            `
                let a: (number | string)[];
                let b: keyof (number | string);
                let c: (number | string) & {id: number};
                let d: (number | string)["length"];
            `
        ));
    });

    test("a union replacing a union member joins that union", () => {
        spec.recipe = replaceOld`number | string`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: Old | null;
                let b: Array<Old | undefined>;
            `,
            `
                let a: number | string | null;
                let b: Array<number | string | undefined>;
            `
        ));
    });

    test("a function type in a union is parenthesized", () => {
        spec.recipe = replaceOld`() => void`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: Old | null;
                let b: Old;
            `,
            `
                let a: (() => void) | null;
                let b: () => void;
            `
        ));
    });

    test("a function, conditional or constructor type substituted into a union is parenthesized", () => {
        spec.recipe = nullableTypeArguments();
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: RefObject<() => void>;
                let b: RefObject<A extends B ? C : D>;
                let c: RefObject<new () => X>;
            `,
            `
                let a: RefObject<(() => void) | null>;
                let b: RefObject<(A extends B ? C : D) | null>;
                let c: RefObject<(new () => X) | null>;
            `
        ));
    });

    test("an optional tuple element keeps its `?` after the type a template splices back in", () => {
        const name = capture('name');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitIdentifier(identifier: J.Identifier, p: any): Promise<J | undefined> {
                const bare = identifier.simpleName === "ReactElement" &&
                    this.cursor.parentTree()?.value.kind !== J.Kind.ParameterizedType;
                return bare ? await template`${name}<any>`.apply(identifier, this.cursor, {values: {name: identifier}}) : identifier;
            }
        });
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: [string, ReactElement?];
            `,
            `
                let a: [string, ReactElement<any>?];
            `
        ));
    });

    test("an implements or extends entry is still built as the parser builds one", () => {
        spec.recipe = replaceOld`Base<string>`;
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                class A implements Old {}
                interface I extends Old {}
            `,
            `
                class A implements Base<string> {}
                interface I extends Base<string> {}
            `
        ));
    });

    test("a comment inside the replaced type stays", () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitFieldAccess(fieldAccess: J.FieldAccess, p: any): Promise<J | undefined> {
                const visited = await super.visitFieldAccess(fieldAccess, p) as J.FieldAccess;
                return visited.name.element.simpleName === "Old"
                    ? await template`number | string`.apply(visited, this.cursor) ?? visited
                    : visited;
            }
        });
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                let a: R./* deprecated */Old;
            `,
            `
                let a: /* deprecated */number | string;
            `
        ));
    });
});

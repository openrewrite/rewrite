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
import {fromVisitor, RecipeSpec, SourceSpec} from "../../src/test";
import {Autodetect} from "../../src/javascript/autodetect";
import {replaceMarkerByKind} from "../../src/markers";
import {
    ChangeImport, isReference, JavaScriptVisitor, JS, maybeBind, MaybeBindOptions, maybeRebind, MaybeRebindOptions,
    maybeUnbind, moduleBindings, resolve, typescript
} from "../../src/javascript";
import {J} from "../../src/java";

/** A declaration file, where `declare module` blocks are usually written. */
function declarations(before: string, after?: string): SourceSpec<JS.CompilationUnit> {
    return {...typescript(before, after), path: 'types/my-widget.d.ts'};
}

/** Stands in for the style marker the project parser attaches to the compilation units it produces. */
async function withDetectedStyles(cu: JS.CompilationUnit): Promise<JS.CompilationUnit> {
    const detector = Autodetect.detector();
    await detector.sample(cu);
    return {...cu, markers: replaceMarkerByKind(cu.markers, detector.build())};
}

/** Binds a `Thing` that reads nothing the file binds, writing the name the binding answers with. */
function bindThing(options: Partial<MaybeBindOptions> = {}, bound: {name?: string} = {}) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitIdentifier(identifier: J.Identifier, p: any): Promise<J | undefined> {
            if (identifier.simpleName !== 'Thing' || !isReference(this.cursor, identifier) ||
                resolve(this.cursor, identifier) !== undefined) {
                return super.visitIdentifier(identifier, p);
            }
            bound.name = maybeBind(this, {module: 'lib', member: 'Thing', typeOnly: true, ...options});
            return bound.name === undefined || bound.name === 'Thing' ? identifier : {...identifier, simpleName: bound.name} as J.Identifier;
        }
    });
}

/** Rebinds from the cursor standing on each `declare module`, as a recipe visiting one would. */
function rebindInDeclarations(options: MaybeRebindOptions, bound: {name?: string} = {}) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitNamespaceDeclaration(declaration: JS.NamespaceDeclaration, p: any): Promise<J | undefined> {
            bound.name = maybeRebind(this, options);
            return declaration;
        }
    });
}

describe('maybeBind inside declare module', () => {
    test('binds inside the block of a script, where an import at the top would turn the declaration into an augmentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                export function f(): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Thing} from 'lib';
                export function f(): Thing;
            }
            `
        ));
    });

    test('reuses the import the block already has, under its alias', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Thing as Widget} from 'lib';
                export function f(): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Thing as Widget} from 'lib';
                export function f(): Widget;
            }
            `
        ));
    });

    test('a type specifier in the block answers a type-only request', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import {type Thing as Kind, other} from 'lib';
                export function f(value: typeof other): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import {type Thing as Kind, other} from 'lib';
                export function f(value: typeof other): Kind;
            }
            `
        ));
    });

    test('merges into the block\'s import of the same module', () => {
        const spec = new RecipeSpec();
        // An import of a module that resolves nowhere hides an unresolved reference from `onlyIfReferenced`.
        spec.recipe = bindThing({onlyIfReferenced: false});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type { Other } from 'lib';
                export function f(value: Other): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type { Other, Thing } from 'lib';
                export function f(value: Other): Thing;
            }
            `
        ));
    });

    test('goes after the block\'s imports, at their indentation and in their quotes and brace spacing', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun({
            ...declarations(
                `
                declare module "my-widget" {
                  import { Base } from "base";

                  export interface Widget extends Base { thing: Thing }
                }
                `,
                `
                declare module "my-widget" {
                  import { Base } from "base";
                  import type { Thing } from "lib";

                  export interface Widget extends Base { thing: Thing }
                }
                `
            ),
            beforeRecipe: withDetectedStyles
        });
    });

    test('takes the first statement\'s line, leaving its comment with it', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                /** Makes a thing. */
                export function f(): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Thing} from 'lib';
                /** Makes a thing. */
                export function f(): Thing;
            }
            `
        ));
    });

    test('two blocks bind independently, each under the name it asks for', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Thing} from 'lib';
                export function f(): Thing;
            }
            declare module 'other-widget' {
                export function g(): Thing;
            }
            declare module 'third-widget' {
                export function h(): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Thing} from 'lib';
                export function f(): Thing;
            }
            declare module 'other-widget' {
                import type {Thing} from 'lib';
                export function g(): Thing;
            }
            declare module 'third-widget' {
                import type {Thing} from 'lib';
                export function h(): Thing;
            }
            `
        ));
    });

    test('steers clear of a name the file\'s top level declares, which the block reads', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitIdentifier(identifier: J.Identifier, p: any): Promise<J | undefined> {
                if (identifier.simpleName !== 'Placeholder') {
                    return super.visitIdentifier(identifier, p);
                }
                const name = maybeBind(this, {module: 'lib', member: 'Thing', typeOnly: true, onlyIfReferenced: false});
                return {...identifier, simpleName: name} as J.Identifier;
            }
        });
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare const Thing: number;
            declare module 'my-widget' {
                export function f(): Placeholder;
            }
            `,
            `
            declare const Thing: number;
            declare module 'my-widget' {
                import type {Thing as Thing_1} from 'lib';
                export function f(): Thing_1;
            }
            `
        ));
    });

    test('refuses a relative module, which an ambient module declaration cannot import', async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {name: 'unset'};
        spec.recipe = bindThing({module: './lib'}, bound);
        //language=typescript
        await spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                export function f(): Thing;
            }
            `
        ));
        expect(bound.name).toBeUndefined();
    });

    test('a module binds at its top, since its declare module is an augmentation, which takes no import', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(typescript(
            `
            import type {Box} from 'box';

            declare module 'box' {
                interface Box { thing: Thing }
            }
            `,
            `
            import type {Box} from 'box';
            import type {Thing} from 'lib';

            declare module 'box' {
                interface Box { thing: Thing }
            }
            `
        ));
    });

    test('an .mts source is a module whatever it holds, so its declare module binds at the top', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun({
            ...typescript(
                `
                declare module 'box' {
                    interface Box { thing: Thing }
                }
                `,
                `
                import type {Thing} from 'lib';

                declare module 'box' {
                    interface Box { thing: Thing }
                }
                `
            ),
            ext: 'mts'
        });
    });

    test('a declare global block binds at the top of its module', () => {
        const spec = new RecipeSpec();
        spec.recipe = bindThing();
        //language=typescript
        return spec.rewriteRun(typescript(
            `
            export {};

            declare global {
                interface Window { thing: Thing }
            }
            `,
            `
            import type {Thing} from 'lib';

            declare global {
                interface Window { thing: Thing }
            }
            `
        ));
    });

    test('moduleBindings reads the block\'s imports, which bind with imports whatever the script holds', async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string, binding?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitNamespaceDeclaration(declaration: JS.NamespaceDeclaration, p: any): Promise<J | undefined> {
                const bindings = moduleBindings(this);
                seen.moduleSystem = bindings.moduleSystem;
                seen.binding = bindings.bindingOf('lib');
                return declaration;
            }
        });
        //language=typescript
        await spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import * as lib from 'lib';
                export const thing: lib.Thing;
            }
            `
        ));
        expect(seen).toEqual({moduleSystem: 'esm', binding: 'lib'});
    });
});

describe('maybeUnbind inside declare module', () => {
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            maybeUnbind(this, {module: 'lib', member: 'Gone'});
            return super.visitJsCompilationUnit(cu, p);
        }
    });

    test('drops the statement with its last specifier, and marks nothing a module', () =>
        //language=typescript
        spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type { Gone } from 'lib';
                export function f(value: string): void;
            }
            `,
            `
            declare module 'my-widget' {
                export function f(value: string): void;
            }
            `
        ))
    );

    test('keeps the other specifiers, their line breaks and trailing comma', () =>
        //language=typescript
        spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import {
                    Gone,
                    Kept,
                } from 'lib';
                export const kept: Kept;
            }
            `,
            `
            declare module 'my-widget' {
                import {
                    Kept,
                } from 'lib';
                export const kept: Kept;
            }
            `
        ))
    );

    test('a block\'s import is used by its own body alone', () =>
        //language=typescript
        spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Gone} from 'lib';
                export const gone: Gone;
            }
            declare module 'other-widget' {
                import type {Gone} from 'lib';
                export const other: string;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Gone} from 'lib';
                export const gone: Gone;
            }
            declare module 'other-widget' {
                export const other: string;
            }
            `
        ))
    );

    test('a module that loses its last import stays one, so its declare module stays an augmentation', () =>
        //language=typescript
        spec.rewriteRun(typescript(
            `
            import type {Gone} from 'lib';

            declare module 'box' {
                interface Box { size: number }
            }
            `,
            `
            declare module 'box' {
                interface Box { size: number }
            }

            export {};
            `
        ))
    );
});

describe('maybeRebind inside declare module', () => {
    test('ChangeImport renames a named import inside the block, with the references to it', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'lib', oldMember: 'Old', newModule: 'lib', newMember: 'New'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import { Old, other } from 'lib';
                export const w: Old<number>;
            }
            `,
            `
            declare module 'my-widget' {
                import { New, other } from 'lib';
                export const w: New<number>;
            }
            `
        ));
    });

    test('ChangeImport moves a block\'s import to another module in place', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'old-lib', oldMember: 'Thing', newModule: 'new-lib'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Thing} from 'old-lib';
                export function f(): Thing;
            }
            `,
            `
            declare module 'my-widget' {
                import type {Thing} from 'new-lib';
                export function f(): Thing;
            }
            `
        ));
    });

    test('ChangeImport adds the replacement inside the block, in place of the specifier it removed', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'lib', oldMember: 'Old', newModule: 'new-lib', newMember: 'New'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import { Old, other } from 'lib';
                export const w: Old<typeof other>;
            }
            `,
            `
            declare module 'my-widget' {
                import { other } from 'lib';
                import { New } from 'new-lib';
                export const w: New<typeof other>;
            }
            `
        ));
    });

    test('ChangeImport merges into the block\'s import of the target, dropping the emptied one', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'lib', oldMember: 'Old', newModule: 'new-lib', newMember: 'New'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import { Old } from 'lib';
                import { Other } from 'new-lib';
                export const w: Old<Other>;
            }
            `,
            `
            declare module 'my-widget' {
                import { New, Other } from 'new-lib';
                export const w: New<Other>;
            }
            `
        ));
    });

    test('ChangeImport rebinds each block on its own, under the name each one leaves free', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'lib', oldMember: 'Old', newModule: 'lib', newMember: 'New'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Old} from 'lib';
                export const w: Old;
            }
            declare module 'other-widget' {
                import type {Old} from 'lib';
                export interface New { old: Old }
            }
            `,
            `
            declare module 'my-widget' {
                import type {New} from 'lib';
                export const w: New;
            }
            declare module 'other-widget' {
                import type {New as Old} from 'lib';
                export interface New { old: Old }
            }
            `
        ));
    });

    test('a reference outside the block reads something else, and keeps its name', () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({oldModule: 'lib', oldMember: 'Old', newModule: 'lib', newMember: 'New'});
        //language=typescript
        return spec.rewriteRun(declarations(
            `
            declare const Old: number;
            declare module 'my-widget' {
                import type {Old} from 'lib';
                export const w: Old;
            }
            `,
            `
            declare const Old: number;
            declare module 'my-widget' {
                import type {New} from 'lib';
                export const w: New;
            }
            `
        ));
    });

    test('refuses a relative target, which an ambient module declaration cannot import', async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {name: 'unset'};
        spec.recipe = rebindInDeclarations({from: {module: 'lib', member: 'Old'}, to: {module: './local', member: 'Old'}}, bound);
        //language=typescript
        await spec.rewriteRun(declarations(
            `
            declare module 'my-widget' {
                import type {Old} from 'lib';
                export const w: Old;
            }
            `
        ));
        expect(bound.name).toBeUndefined();
    });
});

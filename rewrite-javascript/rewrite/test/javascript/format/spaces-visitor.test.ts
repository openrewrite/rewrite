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
import {BlankLinesStyle, IntelliJ, JavaScriptParser, SpacesStyle, typescript} from "../../../src/javascript";
import {AutoformatVisitor, SpacesVisitor} from "../../../src/javascript/format";
import {Draft, produce} from "immer";
import {MarkersKind, NamedStyles, randomId, Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function spaces(customizer?: StyleCustomizer<SpacesStyle>): SpacesStyle {
    return customizer
        ? produce(IntelliJ.TypeScript.spaces(), draft => customizer(draft))
        : IntelliJ.TypeScript.spaces();
}

describe('SpacesVisitor', () => {
    const spec = new RecipeSpec()

    test('space before function declaration parentheses', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces(draft => {
            draft.beforeParentheses.functionDeclarationParentheses = true;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                    interface K {
                        m(): number;
                    }
                `,
                `
                    interface K {
                        m (): number;
                    }
                `)
            // @formatter:on
        )
    });

    test('spaces after export or import', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                export{MyPreciousClass} from'./my-precious-class';
                export type{MyOtherClass} from'./my-other-class';
                import{delta,gamma,zeta}from'delta.js';
                import{b}from'qux.js';
                import*as foo from'foo.js';
                import a from'baz.js';
                import'module-without-export.js';
                import type{Models} from'../models';
                `,
                `
                export {MyPreciousClass} from './my-precious-class';
                export type {MyOtherClass} from './my-other-class';
                import {delta, gamma, zeta} from 'delta.js';
                import {b} from 'qux.js';
                import * as foo from 'foo.js';
                import a from 'baz.js';
                import 'module-without-export.js';
                import type {Models} from '../models';
                `
                // @formatter:on
            ));
    });

    test('ES6 Import Export braces', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces(draft => {
            draft.within.es6ImportExportBraces = true;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                export{MyPreciousClass} from'./my-precious-class';
                import{delta,gamma,zeta}from'delta.js';
                import no from 'change.js';
                `,
                `
                export { MyPreciousClass } from './my-precious-class';
                import { delta, gamma, zeta } from 'delta.js';
                import no from 'change.js';
                `
                // @formatter:on
            ));
    });

    test('await', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                async function fetchData(): Promise<string> {
                    const response = await fetch('https://api.example.com/data');
                    return response.json().name;
                }
                `
                // @formatter:on
            ));
    });

    test('types', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            type Values={
                [key:string]:string;
            }
            `,
                `
            type Values = {
                [key: string]: string;
            }
            `)
            // @formatter:on
        )});

    test('space around assignment operator: true', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces(draft => {
            draft.aroundOperators.assignment = true;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`const x=3; class A {m() { this.x=4; }}`,
                `const x = 3; class A {m() { this.x = 4; }}`)
            // @formatter:on
        )});

    test('space around assignment operator: false', () => {
        spec.recipe = fromVisitor(new SpacesVisitor(spaces(draft => {
            draft.aroundOperators.assignment = false;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`const x = 3; class A {m() { this.x = 4; }}`,
                `const x=3; class A {m() { this.x=4; }}`)
            // @formatter:on
        )});
});

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
import {BlankLinesStyle, IntelliJ, JavaScriptParser, typescript} from "../../../src/javascript";
import {AutoformatVisitor, BlankLinesVisitor} from "../../../src/javascript/format";
import {Draft, produce} from "immer";
import {MarkersKind, NamedStyles, randomId, Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function blankLines(customizer: StyleCustomizer<BlankLinesStyle>): BlankLinesStyle {
    return produce(IntelliJ.TypeScript.blankLines(), draft => customizer(draft));
}

describe('BlankLinesVisitor', () => {
    const spec = new RecipeSpec()

    test('classes', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.aroundClass = 1;
            draft.minimum.afterImports = 1;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                import { readFileSync } from 'fs';
                class A {
                }
                class B {
                } class C {}
                `,
                `
                import { readFileSync } from 'fs';

                class A {
                }

                class B {
                }

                 class C {}
                `),
            // TODO the space before `class C` seems excessive, not sure if it's the BlankLinkesVisitor's responsibility though
            // @formatter:on
        )
    });

    test('fields', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.aroundField = 3;
            draft.keepMaximum.inCode = 3;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                 class A {
    
    
                   field1 = 1;
                   field2 = 2;
                   m() {
                   }
                 }
                `,
                `
                 class A {
                   field1 = 1;
                 
                
                
                   field2 = 2;
                
                
                
                   m() {
                   }
                 }
                `)
            // @formatter:on
        );
    });

    test('class with no imports', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.afterImports = 7;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                 class A {
                 }
                `)
            // @formatter:on
        );
    });
});

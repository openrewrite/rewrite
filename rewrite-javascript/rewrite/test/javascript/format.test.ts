/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {fromVisitor, RecipeSpec} from "../../src/test";
import {IntelliJ, JavaScriptParser, SpacesStyle, typescript} from "../../src/javascript";
import {AutoformatVisitor} from "../../src/javascript/format";
import {Draft, produce} from "immer";
import {MarkersKind, NamedStyles, randomId, Style} from "../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function spaces(customizer: StyleCustomizer<SpacesStyle>): NamedStyles {
    return {
        displayName: "", name: "", tags: [],
        kind: MarkersKind.NamedStyles,
        id: randomId(),
        styles: [produce(IntelliJ.TypeScript.spaces(), customizer)]
    }
}

describe('AutoformatVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new AutoformatVisitor(IntelliJ.TypeScript.spaces()));

    test('no spaces before function declaration parentheses', () => {
        let styles = spaces(draft => {
            draft.beforeParentheses.functionDeclarationParentheses = true;
        });
        return spec.rewriteRun({
            // @formatter:off
            //language=typescript
            ...typescript(`
                    interface K {
                        m (): number;
                    }
                `,
                `
                    interface K {
                        m(): number;
                    }
                `),
            // @formatter:on
            parser: _ => new JavaScriptParser({styles: [styles]})
        })
    });

    test('everything', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                    class K{
                        m () :number{
                            this.m( );
                            return 1;
                        }
                    }
                    const a=1;
                    if(1>0){
                        console.log  (   "true"  );
                    }
                `,
                `
                    class K {
                        m(): number {
                            this.m();
                            return 1;
                        }
                    }
                    const a = 1;
                    if (1 > 0) {
                        console.log("true");
                    }
                `)
            // @formatter:on
        )
    });
});

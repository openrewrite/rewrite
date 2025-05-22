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

function spaces(customizer: StyleCustomizer<SpacesStyle>): SpacesStyle {
    return produce(IntelliJ.TypeScript.spaces(), draft => customizer(draft));
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
});

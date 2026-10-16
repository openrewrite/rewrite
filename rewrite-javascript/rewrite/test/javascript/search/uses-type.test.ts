// noinspection JSUnusedLocalSymbols

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
import {typescript} from "../../../src/javascript";
import {UsesType} from "../../../src/javascript/search";
import {findTypes, usesType} from "../../../src/javascript/preconditions";

describe('UsesType visitor', () => {
    test('should find exact type match', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesType("Array"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `const arr = [1, 2]`,
                `const /*~~>*/arr = /*~~>*/[1, 2]`
            )
        );
    });

    test('should match with glob pattern', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesType("*romise"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `const p = Promise.resolve("data")`,
                `const /*~~>*/p = /*~~>*/Promise.resolve("data")`
            )
        );
    });

    test('usesType matches a subtype, findTypes only the type itself, as in Java', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(usesType('Base').localVisitor!);
        //language=typescript
        await spec.rewriteRun(
            typescript(
                `class Base {}\nclass Leaf extends Base {}\nnew Leaf()`,
                `/*~~>*/class /*~~>*/Base {}\n/*~~>*/class /*~~>*/Leaf extends /*~~>*/Base {}\nnew /*~~>*/Leaf()`
            )
        );

        spec.recipe = fromVisitor(findTypes('Base').localVisitor!);
        //language=typescript
        await spec.rewriteRun(
            typescript(
                `class Base {}\nclass Leaf extends Base {}\nnew Leaf()`,
                `/*~~>*/class /*~~>*/Base {}\nclass Leaf extends /*~~>*/Base {}\nnew Leaf()`
            )
        );
    });

    test('usesType with checkAssignability also matches types reached only through a called method, as Java\'s HasType', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(usesType('Number', true).localVisitor!);
        //language=typescript
        await spec.rewriteRun(
            typescript(
                `declare const n: number;\nn.toFixed(2);\nn.toFixed?.(2);`,
                `declare const n: number;\n/*~~>*/n.toFixed(2);\n/*~~>*/n.toFixed?.(2);`
            )
        );

        spec.recipe = fromVisitor(usesType('Number').localVisitor!);
        //language=typescript
        await spec.rewriteRun(
            typescript(`declare const n: number;\nn.toFixed(2);\nn.toFixed?.(2);`)
        );
    });

    test('usesType matches a primitive by its keyword, as Java\'s HasType', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(usesType('String').localVisitor!);
        //language=typescript
        await spec.rewriteRun(
            typescript(
                `const s = "a"`,
                `const /*~~>*/s = /*~~>*/"a"`
            )
        );
    });
});

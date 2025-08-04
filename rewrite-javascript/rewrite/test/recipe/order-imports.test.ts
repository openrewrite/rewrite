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
import {describe} from "@jest/globals";
import {RecipeSpec} from "../../src/test";
import {OrderImports} from "../../src/recipe/order-imports";
import {typescript} from "../../src/javascript";

describe("order-imports", () => {
    const spec = new RecipeSpec()
    spec.recipe = new OrderImports();

    test("basic", () => {
        return spec.rewriteRun(
            typescript(
            `
            import {gamma, delta} from 'delta.js';
            import {beta as bet, alpha,} from 'alpha.js';
            import {b} from 'qux.js';
            import * as foo from 'foo.js';
            import * as bar from 'bar.js';
            import a from 'baz.js';
            import 'module-without-export.js';
            `,
            `
            import 'module-without-export.js';
            import * as bar from 'bar.js';
            import * as foo from 'foo.js';
            import {alpha, beta as bet,} from 'alpha.js';
            import {delta, gamma} from 'delta.js';
            import a from 'baz.js';
            import {b} from 'qux.js';
            `)
        )
    })
});

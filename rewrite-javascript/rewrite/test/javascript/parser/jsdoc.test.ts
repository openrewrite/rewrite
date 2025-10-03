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

import {RecipeSpec} from "../../../src/test";
import {javascript} from "../../../src/javascript";

test("invalid JSDoc with index signature syntax should not cause parse failure", () =>
    new RecipeSpec().rewriteRun({
        //language=javascript
        ...javascript(`
            // noinspection JSCommentMatchesSignature,JSValidateJSDoc,JSClosureCompilerSyntax

            /**
             * @param { Route } route
             * @param { Array<string|string[]> | Array<SidebarGroup> | [link: string]: SidebarConfig } config
             * @returns { base: string, config: SidebarConfig }
             */
            export function resolveMatchingConfig(regularPath, config) {
                return {}
            }
        `)
    }));

test("valid JSDoc with Record syntax", () =>
    new RecipeSpec().rewriteRun({
        //language=javascript
        ...javascript(`
            // noinspection JSCommentMatchesSignature,JSValidateJSDoc,JSClosureCompilerSyntax

            /**
             * @param { Route } route
             * @param { Array<string|string[]> | Array<SidebarGroup> | Record<string, SidebarConfig> } config
             * @returns {{ base: string, config: SidebarConfig }}
             */
            export function resolveMatchingConfig(regularPath, config) {
                return {}
            }
        `)
    }));

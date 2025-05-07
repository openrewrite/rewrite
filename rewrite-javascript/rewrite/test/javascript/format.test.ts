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

import {RecipeSpec, fromVisitor} from "../../src/test";
import {typescript} from "../../src/javascript";
import {AutoformatVisitor} from "../../src/javascript/format";
import {IntelliJ} from "../../src/javascript/style";

describe('AutoformatVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new AutoformatVisitor(IntelliJ.TypeScript.spaces()));

    test('spaces', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(`
                class K{
                    m () :number{
                        return   x;
                    }
                }
                const a=1;
                if(1>0){
                    console. log ( "true"  );
                }
                `,
            `
                class K {
                    m(): number {
                        return x;
                    }
                }
                const a = 1;
                if (1 > 0) {
                    console.log("true");
                }
                `)
        )});
});

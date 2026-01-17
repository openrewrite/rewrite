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
import {RecipeSpec} from "../../../src/test";
import {JS, javascript} from "../../../src/javascript";

describe('shebang', () => {
    const spec = new RecipeSpec();

    test('shebang at beginning of file', () => spec.rewriteRun({
        //language=javascript
        ...javascript(
            `
                #!/usr/bin/env node
                console.log("Hello, world!");
                `),
        afterRecipe: (cu: JS.CompilationUnit) => {
            const firstStatement = cu.statements[0];
            expect(firstStatement.kind).toBe(JS.Kind.Shebang);
        }
    }));

    test('shebang with blank line after', () => spec.rewriteRun(
        //language=javascript
        javascript(
            `#!/usr/bin/env node

const fs = require('fs');
`)));

    test('shebang followed by comment', () => spec.rewriteRun(
        //language=javascript
        javascript(
            `#!/usr/bin/env node

/* eslint-disable no-console */

const FD = { STDIN: 0, STDOUT: 1 };
`)));
});

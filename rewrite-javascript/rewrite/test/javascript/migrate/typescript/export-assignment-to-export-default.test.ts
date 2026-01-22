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
import {RecipeSpec} from "../../../../src/test";
import {ExportAssignmentToExportDefault} from "../../../../src/javascript/migrate/typescript/export-assignment-to-export-default";
import {typescript} from "../../../../src/javascript";

describe("export-assignment-to-export-default", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ExportAssignmentToExportDefault();

    test("exportIdentifier", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const myFunction = () => {
                    console.log("Hello");
                };
                export = myFunction;
                `,
                `
                const myFunction = () => {
                    console.log("Hello");
                };
                export default myFunction;
                `
            )
        );
    });

    test("exportClass", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                class MyClass {
                    constructor() {
                        this.value = 42;
                    }
                }
                export = MyClass;
                `,
                `
                class MyClass {
                    constructor() {
                        this.value = 42;
                    }
                }
                export default MyClass;
                `
            )
        );
    });

    test("exportFunction", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function myFunction() {
                    return "Hello World";
                }
                export = myFunction;
                `,
                `
                function myFunction() {
                    return "Hello World";
                }
                export default myFunction;
                `
            )
        );
    });

    test("exportObjectLiteral", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                export = {
                    foo: "bar",
                    baz: 42
                };
                `,
                `
                export default {
                    foo: "bar",
                    baz: 42
                };
                `
            )
        );
    });

    test("doesNotChangeExportDefault", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const myFunction = () => {
                    console.log("Hello");
                };
                export default myFunction;
                `
            )
        );
    });

    test("multipleExportsOnlyConvertsExportEquals", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                export const helper = () => "helper";

                class MyClass {
                    value: number = 42;
                }

                export = MyClass;
                `,
                `
                export const helper = () => "helper";

                class MyClass {
                    value: number = 42;
                }

                export default MyClass;
                `
            )
        );
    });
});

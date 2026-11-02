/*
 * Copyright 2026 the original author or authors.
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
import {javascript, typescript} from "../../../src/javascript";
import {IsVendoredOrBundled} from "../../../src/javascript/search";

describe('IsVendoredOrBundled visitor', () => {
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new IsVendoredOrBundled());

    test.each([
        "vendor/jquery.js",
        "src/vendor/lib.js",
        "node_modules/lodash/index.js",
        "bower_components/angular/angular.js",
        "dist/index.js",
        "packages/core/dist/index.mjs",
        "public/app.min.js",
        "lib/index.min.mjs",
        "public/app.bundle.js",
        "static/vendor.bundle.cjs"
    ])('marks vendored or bundled path %s', async path => {
        await spec.rewriteRun({
            ...javascript(`const a = 1;`, `/*~~>*/const a = 1;`),
            path
        });
    });

    test.each([
        "index.js",
        "src/index.js",
        "build/webpack.config.js",
        "src/vendors.js",
        "src/distance.js",
        "src/minify.js",
        "src/bundle.js"
    ])('leaves own source path %s alone', async path => {
        await spec.rewriteRun({
            ...javascript(`const a = 1;`),
            path
        });
    });

    test('marks TypeScript in a vendored directory', async () => {
        await spec.rewriteRun({
            ...typescript(`const a: number = 1;`, `/*~~>*/const a: number = 1;`),
            path: "vendor/lib.ts"
        });
    });

    test('marks a trailing sourceMappingURL', async () => {
        await spec.rewriteRun({
            ...javascript(
                `const a = 1;\n//# sourceMappingURL=index.js.map\n`,
                `/*~~>*/const a = 1;\n//# sourceMappingURL=index.js.map\n`
            ),
            path: "lib/index.js"
        });
    });

    test('marks a legacy trailing sourceMappingURL', async () => {
        await spec.rewriteRun({
            ...javascript(
                `const a = 1;\n//@ sourceMappingURL=index.js.map\n`,
                `/*~~>*/const a = 1;\n//@ sourceMappingURL=index.js.map\n`
            ),
            path: "lib/index.js"
        });
    });

    test('ignores a sourceMappingURL that is not trailing', async () => {
        await spec.rewriteRun({
            ...javascript(`//# sourceMappingURL=index.js.map\nconst a = 1;\n`),
            path: "lib/index.js"
        });
    });
});

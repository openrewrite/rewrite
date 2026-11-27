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
import {builtinModules} from "node:module";
import * as packageName from "../../src/javascript/package-name";

type PackageName = typeof packageName;

/**
 * The query string makes this a separate instance of package-name.
 * The instance the other test files in this worker share keeps the real `module`.
 */
async function withoutIsBuiltin(): Promise<PackageName> {
    vi.doMock("module", async importOriginal => ({...await importOriginal<object>(), isBuiltin: undefined}));
    try {
        const specifier = "../../src/javascript/package-name?without-isBuiltin";
        return await import(/* @vite-ignore */ specifier) as PackageName;
    } finally {
        vi.doUnmock("module");
    }
}

describe("package-name", () => {
    test("names built-ins without module.isBuiltin as with it", async () => {
        const fallback = await withoutIsBuiltin();
        const names = [...builtinModules.filter(name => !name.startsWith("node:")), "test"];
        const answers = (lib: PackageName) => names.map(name =>
            [lib.moduleName(`node:${name}`), lib.spelledLike(name, "node:url")]);
        expect(answers(fallback)).toEqual(answers(packageName));
    });
});

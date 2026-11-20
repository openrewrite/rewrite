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
import {upgradeDependencyVersions} from "../../src/javascript";

describe("upgradeDependencyVersions", () => {
    const GLOBAL_KEY = Symbol.for("org.openrewrite.rpc.RewriteRpc.global");
    let saved: unknown;
    beforeEach(() => {
        saved = (globalThis as any)[GLOBAL_KEY];
        delete (globalThis as any)[GLOBAL_KEY];
    });
    afterEach(() => {
        (globalThis as any)[GLOBAL_KEY] = saved;
    });

    test.each([undefined, false, true])("prepares one Java batch with recovery=%s", async reconcileNpmPeerDependencies => {
        const versions = {"@angular/*": "16.x", "typescript": "5.0.x"};
        const recipe = await upgradeDependencyVersions({versions, reconcileNpmPeerDependencies});
        expect(recipe.name).toBe("org.openrewrite.javascript.UpgradeDependencyVersions");
        const descriptor = await recipe.descriptor();
        expect(Object.fromEntries(descriptor.options.map(o => [o.name, o.value])))
            .toEqual({versions, reconcileNpmPeerDependencies});
        expect(descriptor.recipeList).toEqual([]);
    });
});

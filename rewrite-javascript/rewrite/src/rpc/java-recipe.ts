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
import {RewriteRpc} from "./rewrite-rpc";
import type {RpcRecipe} from "./recipe";

/**
 * Prepare a Java recipe via the active {@link RewriteRpc} connection.
 *
 * Routes a `PrepareRecipe` request to whichever side hosts the Java
 * implementation:
 *  - in production, the Java host that spawned the TS server (`dist/rpc/server.js`);
 *  - in tests, the JVM spawned via `JavaRpcTestServer` (see
 *    `@openrewrite/rewrite/test/java-rpc`).
 *
 * The returned {@link RpcRecipe} extends `Recipe`, so it can be used directly
 * as a recipe — e.g. assigned to `RecipeSpec.recipe` — or composed inside a
 * larger TS recipe (as the editor of a `check(usesType(...), ...)`
 * precondition, or as a step in `recipeList()`).
 *
 * Top-level convenience functions for specific Java recipes (e.g.
 * `addDependency`, `changeType`) sit naturally on top of this primitive:
 *
 * ```ts
 * export function addDependency(options: AddDependencyOptions): Promise<Recipe> {
 *     return prepareJavaRecipe("org.openrewrite.javascript.AddDependency", options);
 * }
 * ```
 *
 * There is no in-process fallback by design: a Java recipe's editor is the
 * single source of truth — we don't reimplement editing recipes in TS. For
 * preconditions that need a graceful no-RPC fallback, use the {@link RecipeRef}
 * pattern via `usesType` / `usesMethod`, which carries a `localVisitor`.
 *
 * @throws Error when no active {@link RewriteRpc} connection is registered.
 */
export async function prepareJavaRecipe(
    id: string,
    options?: Record<string, any>,
): Promise<RpcRecipe> {
    const rpc = RewriteRpc.get();
    if (!rpc) {
        throw new Error(
            `Cannot prepare Java recipe "${id}": no active RewriteRpc connection.\n` +
            "  • Tests: spawn one via JavaRpcTestServer.start() — see " +
            "@openrewrite/rewrite/test/java-rpc.\n" +
            "  • Production: the Java host provides one automatically when it " +
            "loads the TS recipe artifact."
        );
    }
    return rpc.prepareRecipe(id, options);
}

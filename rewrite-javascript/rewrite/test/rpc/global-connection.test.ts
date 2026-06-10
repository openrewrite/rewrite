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
import {RewriteRpc} from "../../src/rpc/rewrite-rpc";

/**
 * Regression tests for gh-7968: the active {@link RewriteRpc} connection must be
 * shared across *every copy* of the `@openrewrite/rewrite` module, not just the
 * host's. A recipe package that bundles the library (or resolves it from its own
 * `node_modules`) loads a separate copy of this class, so a `static` field would
 * be invisible to it — `prepareJavaRecipe` would then fail with "no active
 * RewriteRpc connection", which `InstallRecipes` masks as the misleading
 * "Ensure the constructor can be called without any arguments".
 *
 * Storing the connection on `globalThis` under a `Symbol.for` key — which
 * resolves to the same symbol in any module copy — keeps it visible everywhere.
 */
describe("RewriteRpc active connection", () => {
    // The key every module copy resolves to via Symbol.for.
    const GLOBAL_KEY = Symbol.for("org.openrewrite.rpc.RewriteRpc.global");

    const saved = (globalThis as any)[GLOBAL_KEY];
    afterEach(() => {
        (globalThis as any)[GLOBAL_KEY] = saved;
    });

    test("set() stores the connection on globalThis under the shared Symbol.for key", () => {
        const fake = {iAm: "connection"} as unknown as RewriteRpc;
        RewriteRpc.set(fake);
        // A separately-loaded module copy reads this exact slot through its own
        // (distinct) RewriteRpc class object — Symbol.for guarantees the same key.
        expect((globalThis as any)[GLOBAL_KEY]).toBe(fake);
    });

    test("get() sees a connection set by another module copy", () => {
        const fake = {iAm: "connection-from-other-copy"} as unknown as RewriteRpc;
        // Simulate the host copy publishing the active connection.
        (globalThis as any)[GLOBAL_KEY] = fake;
        expect(RewriteRpc.get()).toBe(fake);
    });
});

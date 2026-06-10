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
/**
 * Vitest fixture for tests that need to drive Java recipes via a spawned
 * `org.openrewrite.maven.rpc.JavaRewriteRpc` process. Provides a worker-scoped
 * JVM (one process per vitest worker) wrapped in a test-scoped fixture that
 * resets accumulated state before each test.
 *
 * Typical use:
 * ```
 * import {testJavaRpc, describeJavaRpc} from "@openrewrite/rewrite/test/java-rpc";
 *
 * describeJavaRpc("AddDependency", () => {
 *     testJavaRpc("adds a dep to package.json", async ({javaRpc}) => {
 *         const spec = new RecipeSpec();
 *         spec.recipe = await javaRpc.rpc.prepareRecipe(
 *             "org.openrewrite.javascript.AddDependency", {name: "lodash", version: "^4.17.21"},
 *         );
 *         await spec.rewriteRun(packageJson(before, after));
 *     });
 * });
 * ```
 *
 * The classpath is resolved via {@link findTestClasspath}; the suite is skipped
 * (via {@link describeJavaRpc}) when no classpath has been configured.
 */
import {describe, type SuiteAPI, test as base} from "vitest";
import {findTestClasspath, JavaRpcTestServer} from "../rpc/java-rpc-client";

export {findTestClasspath, JavaRpcTestServer} from "../rpc/java-rpc-client";
export type {JavaRpcOptions} from "../rpc/java-rpc-client";

interface JavaRpcFixtures {
    /**
     * Internal worker-scoped fixture holding the spawned JVM. Test code should
     * use {@link javaRpc} instead — it depends on this fixture but adds a
     * per-test reset.
     */
    _javaRpcServer: JavaRpcTestServer;
    /**
     * The {@link JavaRpcTestServer} for this test. State on both the Java
     * side and the TS-side {@link RewriteRpc} is reset before the test runs,
     * so each test gets a clean slate even though the JVM persists across
     * tests within the same vitest worker.
     */
    javaRpc: JavaRpcTestServer;
}

/**
 * Vitest test API extended with a {@link JavaRpcTestServer} fixture. The JVM
 * is spawned once per worker; state is reset before every test.
 */
export const testJavaRpc = base.extend<JavaRpcFixtures>({
    _javaRpcServer: [
        async ({}, use) => {
            const server = await JavaRpcTestServer.start();
            try {
                await use(server);
            } finally {
                await server.dispose();
            }
        },
        {scope: "worker"},
    ],
    javaRpc: async ({_javaRpcServer}, use) => {
        await _javaRpcServer.reset();
        await use(_javaRpcServer);
    },
});

/**
 * `describe` wrapper that skips the suite when the Java RPC test classpath has
 * not been configured (env var or generated file). Use in place of `describe`
 * for any suite that uses {@link testJavaRpc}.
 *
 * Run `./gradlew :rewrite-javascript:generateTestClasspath` to enable, or set
 * the `REWRITE_JAVASCRIPT_CLASSPATH` environment variable.
 */
export const describeJavaRpc: SuiteAPI =
    findTestClasspath() ? describe : (describe.skip as unknown as SuiteAPI);

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
package org.openrewrite.python;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openrewrite.python.rpc.PythonRewriteRpc;

/**
 * JUnit 5 extension that gives each test a clean Python RPC channel.
 * <p>
 * Tests share one Python process, because starting one takes longer than most tests do.
 * A failed test can leave a request half-finished, so after one the process is shut down instead of reset.
 * <p>
 * Usage:
 * <pre>
 * {@code @ExtendWith(PythonRpcResetExtension.class)}
 * class MyPythonTest implements RewriteTest {
 *     // tests...
 * }
 * </pre>
 */
public class PythonRpcResetExtension implements BeforeEachCallback, AfterEachCallback {
    private static boolean factoryConfigured = false;

    @Override
    public void beforeEach(ExtensionContext context) {
        // Configure the factory on first use.
        // The python version can be overridden via system property (e.g., by the py2CompatibilityTest suite),
        // and the build names the log and traces every RPC message to it only under -PverboseTests.
        if (!factoryConfigured) {
            String pythonVersion = System.getProperty("rewrite.python.version", "3");
            PythonRewriteRpc.setFactory(
              PythonRewriteRpc.builder()
                .pythonVersion(pythonVersion)
                .traceRpcMessages(Boolean.getBoolean("rewrite.python.traceRpcMessages"))
                .log(java.nio.file.Paths.get(System.getProperty("rewrite.python.rpcLog", "build/python-rpc.log")))
            );
            factoryConfigured = true;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            PythonRewriteRpc.shutdownCurrent();
            return;
        }
        try {
            PythonRewriteRpc.resetCurrent();
        } catch (RuntimeException e) {
            PythonRewriteRpc.shutdownCurrent();
        }
    }
}

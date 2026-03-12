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
 * JUnit 5 extension that ensures each test gets an isolated Python RPC channel.
 * <p>
 * This follows the same pattern as JavaScript tests, where the RPC process is
 * shut down after each test to ensure complete isolation. This prevents any
 * state leakage between tests and ensures each test starts with a fresh RPC channel.
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
        // Configure the factory with tracing on first use.
        // The python version can be overridden via system property (e.g., by the py2CompatibilityTest suite).
        if (!factoryConfigured) {
            String pythonVersion = System.getProperty("rewrite.python.version", "3");
            PythonRewriteRpc.setFactory(
              PythonRewriteRpc.builder()
                .pythonVersion(pythonVersion)
                .traceRpcMessages()
                .log(java.nio.file.Paths.get("build/python-rpc.log"))
            );
            factoryConfigured = true;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Shutdown after each test to ensure complete isolation
        // This matches the JavaScript test pattern
        PythonRewriteRpc.shutdownCurrent();
    }
}

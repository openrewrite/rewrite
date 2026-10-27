/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class JavaRewriteRpcTest {

    @Test
    void exitsWhenStdinCloses(@TempDir Path tempDir) throws Exception {
        Path stderr = tempDir.resolve("stderr.txt");
        Process server = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-Duser.home=" + tempDir,
                "-cp", System.getProperty("java.class.path"),
                WithNonDaemonThread.class.getName())
                .redirectError(stderr.toFile())
                .start();
        JsonRpc client = new JsonRpc(new HeaderDelimitedMessageHandler(
                server.getInputStream(), server.getOutputStream())).bind();
        try {
            String[] languages = client.send(JsonRpcRequest.newRequest("GetLanguages"))
                    .get(60, SECONDS).getResult(String[].class);
            assertThat(languages).contains("org.openrewrite.xml.tree.Xml$Document");

            server.getOutputStream().close();
            assertThat(server.waitFor(30, SECONDS)).as("exited after stdin closed").isTrue();
            assertThat(server.exitValue()).isZero();
        } catch (Exception | AssertionError e) {
            // The server's stack trace is only in its stderr file, which the temp directory's cleanup deletes
            throw new AssertionError("Server stderr:\n" + new String(Files.readAllBytes(stderr)), e);
        } finally {
            client.shutdown();
            server.destroyForcibly();
        }
    }

    /**
     * Runs the server next to a non-daemon thread, standing in for one that recipe code might leave running.
     */
    public static class WithNonDaemonThread {
        public static void main(String[] args) {
            new Thread(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignored) {
                }
            }).start();
            JavaRewriteRpc.main(args);
        }
    }
}

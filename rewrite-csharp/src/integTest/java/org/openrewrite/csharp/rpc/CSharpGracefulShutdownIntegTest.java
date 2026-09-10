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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Closing the server's stdin is how the Java host asks it to shut down, and the exit
 * status is the only signal it gets back.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpGracefulShutdownIntegTest {

    @Test
    void serverExitsCleanlyWhenStdinCloses(@TempDir Path tempDir) throws Exception {
        // --metrics-csv is what wraps the message handler, and only the wrapper has to
        // forward the buffer-release callback; without the flag nothing is under test.
        Process server = new ProcessBuilder("dotnet", locateTool().toString(),
          "--metrics-csv=" + tempDir.resolve("metrics.csv"))
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
        try {
            // One consumed message is enough to leave bytes in the buffer, and it also
            // guarantees the server is listening rather than racing an EOF at startup.
            request(server, "GetLanguages");

            server.getOutputStream().close();

            assertThat(server.waitFor(60, TimeUnit.SECONDS))
              .as("server should exit on its own once stdin closes")
              .isTrue();
            assertThat(server.exitValue())
              .as("an aborted process reports 134, not 0")
              .isZero();
        } finally {
            server.destroyForcibly();
        }
    }

    /** The tool assembly produced by the {@code csharpBuild} Gradle task this suite depends on. */
    private static Path locateTool() {
        Path base = Paths.get(System.getProperty("user.dir"));
        for (Path csharpDir : new Path[]{base.resolve("csharp"), base.resolve("rewrite-csharp/csharp")}) {
            Path tool = csharpDir.resolve("OpenRewrite.Tool/bin/Debug/net10.0/OpenRewrite.Tool.dll");
            if (Files.exists(tool)) {
                return tool.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Could not find OpenRewrite.Tool.dll; run the csharpBuild task");
    }

    /** Sends a parameterless JSON-RPC request and drains the reply off the header-delimited stream. */
    private static void request(Process server, String method) throws IOException {
        byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":{}}")
          .getBytes(StandardCharsets.UTF_8);
        OutputStream out = server.getOutputStream();
        out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();

        InputStream in = server.getInputStream();
        int contentLength = -1;
        for (String line = readLine(in); ; line = readLine(in)) {
            if (line.isEmpty()) {
                if (contentLength < 0) {
                    throw new IOException("Reply had no Content-Length header");
                }
                break;
            }
            if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }
        for (int read = 0; read < contentLength; ) {
            long skipped = in.skip(contentLength - read);
            if (skipped <= 0) {
                throw new IOException("Reply body truncated");
            }
            read += (int) skipped;
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int c = in.read(); c != '\n'; c = in.read()) {
            if (c == -1) {
                throw new IOException("Server closed stdout before replying");
            }
            if (c != '\r') {
                line.append((char) c);
            }
        }
        return line.toString();
    }
}

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
package org.openrewrite.golang;

import org.openrewrite.golang.internal.GoExecutor;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.rpc.GoRpcSourceExtractor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The RPC engine built the way a host builds it, from the sources packaged with the module. It is built
 * once for all the test classes of a JVM.
 */
public final class BundledEngine {
    private static Path sources;

    private BundledEngine() {
    }

    /**
     * @return The directory the engine's sources were extracted to and built in.
     */
    public static synchronized Path sources() throws Exception {
        if (sources == null) {
            Path dir = Files.createTempDirectory("rewrite-go-engine");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(dir)));
            GoRpcSourceExtractor.extractTo(dir);
            String go = GoExecutor.GO.find();
            assertThat(go).as("the go toolchain builds the RPC engine").isNotNull();
            GoExecutor.RunResult build = GoExecutor.GO.run(dir, go, emptyMap(),
                    "build", "-o", "rewrite-go-rpc", "./cmd/rpc");
            assertThat(build.isSuccess()).as(build.getStderr()).isTrue();
            sources = dir;
        }
        return sources;
    }

    /**
     * Has {@link GoRewriteRpc#getOrStart()} start this engine on the calling thread.
     */
    public static void use() throws Exception {
        GoRewriteRpc.setFactory(GoRewriteRpc.builder().goBinaryPath(sources().resolve("rewrite-go-rpc")));
    }

    private static void delete(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(reverseOrder()).forEach(file -> file.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

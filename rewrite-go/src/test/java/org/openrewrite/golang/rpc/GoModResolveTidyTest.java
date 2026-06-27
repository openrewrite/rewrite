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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.internal.modgraph.GoModFile;
import org.openrewrite.golang.internal.modgraph.GoImports;
import org.openrewrite.golang.rpc.GoRewriteRpc.GoModResolveTidyRequest;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the exact code path the CLI invokes for the Go go.mod recipe: the
 * {@code GoModResolveTidy} RPC handler ({@link GoRewriteRpc#resolveTidy}), which
 * builds the cache+proxy source from the request and runs the pure-Java
 * resolver. Validates the direct/indirect result against {@code go mod tidy},
 * fetching dependency metadata over the proxy via {@link HttpUrlConnectionSender}
 * — the same HttpSender path the host would use.
 */
class GoModResolveTidyTest {

    @Test
    @SuppressWarnings("unchecked")
    void handlerResolvesPruningCompletenessLikeGoModTidy(@TempDir Path dir) throws Exception {
        assumeTrue(hasGo(), "needs the go toolchain + network");
        // gin: the case that genuinely exercises go1.17+ pruning-completeness
        // promotion (kr/text, go.uber.org/mock) through the resolver.
        write(dir, "go.mod", "module example.com/ginapp\n\ngo 1.25.0\n\nrequire github.com/gin-gonic/gin v1.10.0\n");
        write(dir, "main.go", "package main\n\nimport _ \"github.com/gin-gonic/gin\"\n\nfunc main() {}\n");
        runGo(dir, "mod", "tidy");

        String tidied = new String(Files.readAllBytes(dir.resolve("go.mod")), StandardCharsets.UTF_8);
        GoModFile golden = GoModFile.parse(tidied);
        Set<String> goldenDirect = new TreeSet<>();
        Set<String> goldenIndirect = new TreeSet<>();
        for (GoModFile.Require r : golden.requires()) {
            (r.indirect ? goldenIndirect : goldenDirect).add(r.path);
        }
        assumeTrue(!goldenIndirect.isEmpty(), "go mod tidy produced nothing (offline?)");

        GoModResolveTidyRequest req = new GoModResolveTidyRequest();
        req.goMod = tidied;
        req.mainImports = scanMainImports(dir);
        req.modulePath = "example.com/ginapp";
        req.separateIndirect = true;
        req.goproxy = runGo(dir, "env", "GOPROXY").trim();
        req.gomodcache = null;

        Map<String, Object> out = GoRewriteRpc.resolveTidy(req, new HttpUrlConnectionSender());

        assertThat((Boolean) out.get("complete")).as("complete").isTrue();
        Map<String, String> direct = (Map<String, String>) out.get("direct");
        Map<String, String> indirect = (Map<String, String>) out.get("indirect");
        assertThat(new TreeSet<>(direct.keySet())).as("direct").isEqualTo(goldenDirect);
        assertThat(new TreeSet<>(indirect.keySet())).as("indirect").isEqualTo(goldenIndirect);
    }

    private static List<String> scanMainImports(Path dir) throws Exception {
        Set<String> imports = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.getFileName().toString().endsWith(".go"))::iterator) {
                imports.addAll(GoImports.parse(new String(Files.readAllBytes(f), StandardCharsets.UTF_8)));
            }
        }
        return new ArrayList<>(imports);
    }

    private static boolean hasGo() {
        try {
            return new ProcessBuilder("go", "version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void write(Path dir, String name, String content) throws Exception {
        Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String runGo(Path dir, String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "go";
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true);
        pb.environment().put("GOFLAGS", "-mod=mod");
        Process p = pb.start();
        String out;
        try (InputStream in = p.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            out = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
        p.waitFor();
        return out;
    }
}

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
package org.openrewrite.golang.internal.modgraph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Validates the Java pruned-MVS resolver against the real {@code go} toolchain:
 * the build list it computes (fetching every dependency go.mod from the GOPROXY
 * via {@link HttpUrlConnectionSender}) must agree with {@code go list -m all}.
 * The toolchain is used only as the golden — the resolver never execs.
 */
class ResolverTest {

    @Test
    void buildListMatchesGoListMAll(@TempDir Path dir) throws Exception {
        assumeTrue(hasGo(), "needs the go toolchain + network");

        write(dir, "go.mod", "module example.com/resolvertest\n\ngo 1.21\n\nrequire github.com/spf13/cobra v1.8.0\n");
        write(dir, "main.go", "package main\n\nimport _ \"github.com/spf13/cobra\"\n\nfunc main() {}\n");
        runGo(dir, "mod", "tidy");

        Map<String, String> golden = listModVersions(dir); // path -> version, sans main module
        assumeTrue(!golden.isEmpty(), "go list produced nothing (offline?)");

        byte[] goMod = Files.readAllBytes(dir.resolve("go.mod"));
        String goproxy = runGo(dir, "env", "GOPROXY").trim();
        ModSource src = new ProxySource(goproxy, new HttpUrlConnectionSender());

        ResolveResult res = Resolver.resolve(goMod, src);
        assertThat(res.complete()).as("expected a complete resolution").isTrue();

        Map<String, String> ours = new HashMap<>();
        for (ResolveResult.Module m : res.buildList()) {
            if (!m.main) {
                ours.put(m.path, m.version);
            }
        }

        // Every module go selected, at the same version; and no extras.
        assertThat(ours).containsExactlyInAnyOrderEntriesOf(golden);
    }

    private static Map<String, String> listModVersions(Path dir) throws Exception {
        String out = runGo(dir, "list", "-m", "-f", "{{.Path}} {{.Version}}", "all");
        Map<String, String> m = new HashMap<>();
        for (String line : out.split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2 && !parts[1].isEmpty()) { // skip the main module (no version)
                m.put(parts[0], parts[1]);
            }
        }
        return m;
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

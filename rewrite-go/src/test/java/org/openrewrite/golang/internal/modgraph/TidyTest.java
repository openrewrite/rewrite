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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Validates the Java require-set computation against {@code go mod tidy} for the
 * scenarios that stress go1.17+ pruning-completeness: a test-transitive indirect
 * (kr/text via check.v1), gin's genuine promotions, and the conc depth-ordering
 * case where kr/pretty must be promoted but kr/text must NOT. The toolchain is
 * the golden; the resolver fetches all metadata from the proxy via HttpSender.
 */
class TidyTest {

    @Test
    void noExtras(@TempDir Path dir) throws Exception {
        assertMatchesGoModTidy(dir, "example.com/noextras",
          "module example.com/noextras\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n",
          "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n",
          null);
    }

    @Test
    void testTransitive(@TempDir Path dir) throws Exception {
        assertMatchesGoModTidy(dir, "example.com/testtrans",
          "module example.com/testtrans\n\ngo 1.25.0\n\nrequire github.com/stretchr/testify v1.9.0\n",
          "package main\n\nimport _ \"github.com/stretchr/testify/assert\"\n\nfunc main() {}\n",
          null);
    }

    @Test
    void ginPruningCompleteness(@TempDir Path dir) throws Exception {
        assertMatchesGoModTidy(dir, "example.com/ginapp",
          "module example.com/ginapp\n\ngo 1.25.0\n\nrequire github.com/gin-gonic/gin v1.10.0\n",
          "package main\n\nimport _ \"github.com/gin-gonic/gin\"\n\nfunc main() {}\n",
          null);
    }

    @Test
    void testInTestFileDoesNotOverPromote(@TempDir Path dir) throws Exception {
        assertMatchesGoModTidy(dir, "example.com/conctest",
          "module example.com/conctest\n\ngo 1.20\n\nrequire github.com/stretchr/testify v1.8.1\n",
          "package conctest\n",
          "package conctest\n\nimport (\n\t\"testing\"\n\n\t\"github.com/stretchr/testify/assert\"\n)\n\nfunc TestX(t *testing.T) { assert.Equal(t, 1, 1) }\n");
    }

    @Test
    void concDepthOrdering(@TempDir Path dir) throws Exception {
        assertMatchesGoModTidy(dir, "github.com/sourcegraph/conc",
          "module github.com/sourcegraph/conc\n\ngo 1.20\n\nrequire github.com/stretchr/testify v1.8.1\n\nrequire (\n\tgithub.com/davecgh/go-spew v1.1.1 // indirect\n\tgithub.com/kr/pretty v0.3.0 // indirect\n\tgithub.com/pmezard/go-difflib v1.0.0 // indirect\n\tgithub.com/rogpeppe/go-internal v1.9.0 // indirect\n\tgopkg.in/check.v1 v1.0.0-20190902080502-41f04d3bba15 // indirect\n\tgopkg.in/yaml.v3 v3.0.1 // indirect\n)\n",
          "package conc\n",
          "package conc\n\nimport (\n\t\"testing\"\n\n\t\"github.com/stretchr/testify/assert\"\n)\n\nfunc TestX(t *testing.T) { assert.Equal(t, 1, 1) }\n");
    }

    private void assertMatchesGoModTidy(Path dir, String modPath, String goMod, String mainGo,
                                        @Nullable String testGo) throws Exception {
        assumeTrue(hasGo(), "needs the go toolchain + network");
        write(dir, "go.mod", goMod);
        write(dir, "main.go", mainGo);
        if (testGo != null) {
            write(dir, "main_test.go", testGo);
        }
        runGo(dir, "mod", "tidy");

        byte[] tidied = Files.readAllBytes(dir.resolve("go.mod"));
        String tidiedStr = new String(tidied, StandardCharsets.UTF_8);
        GoModFile golden = GoModFile.parse(tidiedStr);
        Set<String> goldenDirect = new TreeSet<>();
        Set<String> goldenIndirect = new TreeSet<>();
        for (GoModFile.Require r : golden.requires()) {
            (r.indirect ? goldenIndirect : goldenDirect).add(r.path);
        }
        assumeTrue(!goldenDirect.isEmpty() || !goldenIndirect.isEmpty(), "go mod tidy produced nothing (offline?)");

        List<String> mainImports = scanMainImports(dir);
        String goproxy = runGo(dir, "env", "GOPROXY").trim();
        ModSource src = new ProxySource(goproxy, new HttpUrlConnectionSender());

        ResolveResult res = Resolver.resolve(tidied, src);
        RequireSet rs = Tidy.tidyRequireSet(mainImports, modPath, tidiedStr, res, src, true);

        assertThat(rs.complete()).as("expected a complete TidyRequireSet").isTrue();
        assertThat(new TreeSet<>(rs.direct().keySet())).as("direct").isEqualTo(goldenDirect);
        assertThat(new TreeSet<>(rs.indirect().keySet())).as("indirect").isEqualTo(goldenIndirect);
    }

    /** Imports across every .go file of the (flat) main module, tests included. */
    private static List<String> scanMainImports(Path dir) throws Exception {
        Set<String> imports = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.getFileName().toString().endsWith(".go"))::iterator) {
                imports.addAll(GoImports.parse(new String(Files.readAllBytes(f), StandardCharsets.UTF_8)));
            }
        }
        return new java.util.ArrayList<>(imports);
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

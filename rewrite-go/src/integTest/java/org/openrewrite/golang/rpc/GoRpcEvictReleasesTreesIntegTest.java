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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.internal.InMemoryLargeSourceSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 300, unit = TimeUnit.SECONDS)
class GoRpcEvictReleasesTreesIntegTest {

    private static final int FILES = 200;

    @TempDir
    Path tempDir;

    Path metrics;

    @BeforeEach
    void before() {
        metrics = tempDir.resolve("metrics.csv");
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(Paths.get("build/rewrite-go-rpc").toAbsolutePath())
          .log(tempDir.resolve("go-rpc.log"))
          .metricsCsv(metrics)
          .environment(Map.of("GOGC", "5")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void goHeapStaysFlatWhenEachFileIsEvictedAfterItsRecipeRun() throws IOException {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        List<SourceFile> files = GolangParser.builder().build()
          .parse(ctx, IntStream.range(0, FILES).mapToObj(GoRpcEvictReleasesTreesIntegTest::source).toArray(String[]::new))
          .collect(toList());
        assertThat(files).hasSize(FILES);

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        rpc.reset();
        RecipeRun run = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag")
          .run(new InMemoryLargeSourceSet(files), ctx);
        assertThat(run.getChangeset().getAllResults()).hasSize(FILES);
        GoRewriteRpc.shutdownCurrent();

        List<Row> evicts = readRows().stream().filter(r -> "Evict".equals(r.method)).collect(toList());
        assertThat(evicts).hasSize(FILES);

        Row early = evicts.get(FILES / 10);
        Row late = evicts.get(FILES - 1);
        String trend = String.format("Go after Evict #%d: heap=%,d B local=%d remote=%d; " +
                                     "after Evict #%d: heap=%,d B local=%d remote=%d",
          FILES / 10, early.heap, early.local, early.remote,
          FILES - 1, late.heap, late.local, late.remote);

        assertThat(evicts).as("control: Evict drops the file from local objects. " + trend)
          .allSatisfy(r -> assertThat(r.local).isEqualTo(early.local));
        assertThat(late.remote).as("Evict releases the tree Go received from Java. " + trend).isEqualTo(early.remote);
        assertThat(late.heap).as("Go heap stays flat across evicted files. " + trend).isLessThan(early.heap * 2);
    }

    private List<Row> readRows() throws IOException {
        List<String> lines = Files.readAllLines(metrics);
        List<String> header = List.of(lines.get(0).split(","));
        List<Row> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] c = line.split(",", -1);
            rows.add(new Row(
              c[header.indexOf("method")],
              Long.parseLong(c[header.indexOf("memory_used_bytes")]),
              Integer.parseInt(c[header.indexOf("local_objects")]),
              Integer.parseInt(c[header.indexOf("remote_objects")])));
        }
        return rows;
    }

    private record Row(String method, long heap, int local, int remote) {
    }

    private static String source(int i) {
        StringBuilder sb = new StringBuilder("package p").append(i).append("\n\nimport \"fmt\"\n\n");
        for (int f = 0; f < 20; f++) {
            sb.append("func f").append(f).append("(a int, b string) (int, error) {\n")
              .append("\tvar x = a * ").append(f).append("\n")
              .append("\tif x > 10 {\n")
              .append("\t\treturn 0, fmt.Errorf(\"too big %d %s\", x, b)\n")
              .append("\t}\n")
              .append("\tfor i := 0; i < x; i++ {\n")
              .append("\t\tfmt.Println(i, b)\n")
              .append("\t}\n")
              .append("\treturn x, nil\n")
              .append("}\n\n");
        }
        return sb.toString();
    }
}

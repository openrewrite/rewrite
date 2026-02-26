/*
 * Copyright 2024 the original author or authors.
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

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class PythonParserTest implements RewriteTest {

    @Test
    void parseString() {
        rewriteRun(
          python(
            """
              import sys
              print(sys.path)
              """,
            spec -> spec.afterRecipe(cu -> SoftAssertions.assertSoftly(softly -> {
                  softly.assertThat(cu).isInstanceOf(Py.CompilationUnit.class);
                  softly.assertThat(cu.getMarkers().getMarkers()).isEmpty();
              })
            )
          )
        );
    }

    @Test
    void parseAndPrint() {
        rewriteRun(
          python(
            """
              import sys # comment
              print(sys.path)
              """,
            spec -> spec.afterRecipe(cu -> SoftAssertions.assertSoftly(softly -> {
                  softly.assertThat(cu).isInstanceOf(Py.CompilationUnit.class);
                  softly.assertThat(cu.getMarkers().getMarkers()).isEmpty();
                  softly.assertThat(((SourceFile) new TreeVisitor<J, Integer>() {
                      @Override
                      public J preVisit(J tree, Integer integer) {
                          return tree.withId(Tree.randomId());
                      }
                  }.visitNonNull(cu, 0)).printAll()).isEqualTo("import sys # comment\nprint(sys.path)");
              })
            )
          )
        );
    }

    @Test
    void unicodeEscapes() {
        rewriteRun(
          python(
            """
              s = "\\uD83D\\uDE00"
              print(s)
              """,
            spec -> spec.afterRecipe(cu -> {
                var s = (J.Assignment) cu.getStatements().get(0);
                var str = (J.Literal) s.getAssignment();
                assertThat(str.getUnicodeEscapes()).satisfiesExactly(
                  esc -> assertThat(esc.getCodePoint()).isEqualTo("D83D"),
                  esc -> assertThat(esc.getCodePoint()).isEqualTo("DE00")
                );
            })
          )
        );
    }

    @Test
    @Timeout(600)
    void parseParamikoFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"),
          "moderne/working-set-python-static-analysis-2/paramiko/paramiko");
        if (!Files.isDirectory(dir)) {
            return;
        }

        List<Path> pyFiles;
        try (var stream = Files.walk(dir)) {
            pyFiles = stream
              .filter(p -> p.toString().endsWith(".py"))
              .filter(p -> !p.toString().contains("/build/"))
              .filter(p -> {
                  try {
                      return Files.size(p) > 0;
                  } catch (IOException e) {
                      return false;
                  }
              })
              .sorted()
              .collect(Collectors.toList());
        }

        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder().timeout(Duration.ofHours(1)));
        PythonParser parser = PythonParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        System.out.println("Parsing " + pyFiles.size() + " files as batch...");
        long t0 = System.nanoTime();
        List<SourceFile> allSourceFiles = parser.parse(pyFiles, dir, ctx)
          .peek(sf -> System.out.printf("  Parsed: %-50s (%.1fs elapsed)%n",
            sf.getSourcePath(), (System.nanoTime() - t0) / 1e9))
          .collect(Collectors.toList());
        System.out.printf("Batch parse complete in %.1fs. Shutting down RPC...%n",
          (System.nanoTime() - t0) / 1e9);
        PythonRewriteRpc.shutdownCurrent();

        for (SourceFile sf : allSourceFiles) {
            long pt = System.nanoTime();
            System.out.print("Printing: " + sf.getSourcePath() + "...");
            String printed = sf.printAll();
            System.out.printf(" %.1fs%n", (System.nanoTime() - pt) / 1e9);
            String expected = Files.readString(dir.resolve(sf.getSourcePath()));
            assertThat(printed).as("printAll() for " + sf.getSourcePath()).isEqualTo(expected);
        }
    }

    @Test
    @Timeout(600)
    void parseFabricProject() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"),
          "moderne/working-set-python-static-analysis-2/fabric/fabric");
        if (!Files.isDirectory(dir)) {
            return;
        }

        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder().timeout(Duration.ofHours(1)));
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        System.out.println("Parsing project: " + dir);
        long t0 = System.nanoTime();
        List<SourceFile> allSourceFiles = rpc.parseProject(dir, ctx)
          .peek(sf -> System.out.printf("  Parsed: %-50s (%.1fs elapsed)%n",
            sf.getSourcePath(), (System.nanoTime() - t0) / 1e9))
          .collect(Collectors.toList());
        System.out.printf("Parse complete in %.1fs (%d files). Shutting down RPC...%n",
          (System.nanoTime() - t0) / 1e9, allSourceFiles.size());
        PythonRewriteRpc.shutdownCurrent();

        for (SourceFile sf : allSourceFiles) {
            long pt = System.nanoTime();
            System.out.print("Printing: " + sf.getSourcePath() + "...");
            String printed = sf.printAll();
            System.out.printf(" %.1fs%n", (System.nanoTime() - pt) / 1e9);
            String expected = Files.readString(dir.resolve(sf.getSourcePath()));
            assertThat(printed).as("printAll() for " + sf.getSourcePath()).isEqualTo(expected);
        }
    }

    @Test
    @Timeout(600)
    void parseFabricProjectViaParserParse() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"),
          "moderne/working-set-python-static-analysis-2/fabric/fabric");
        if (!Files.isDirectory(dir)) {
            return;
        }

        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder().timeout(Duration.ofHours(1)));
        PythonParser parser = PythonParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        List<Path> pyFiles;
        try (var stream = Files.walk(dir)) {
            pyFiles = stream
              .filter(p -> p.toString().endsWith(".py"))
              .filter(p -> !p.toString().contains("/build/"))
              .filter(p -> {
                  try { return Files.size(p) > 0; } catch (IOException e) { return false; }
              })
              .sorted()
              .collect(Collectors.toList());
        }

        System.out.println("Parsing " + pyFiles.size() + " files via parser.parse...");
        long t0 = System.nanoTime();
        List<SourceFile> allSourceFiles = parser.parse(pyFiles, dir, ctx)
          .peek(sf -> System.out.printf("  Parsed: %-50s (%.1fs elapsed)%n",
            sf.getSourcePath(), (System.nanoTime() - t0) / 1e9))
          .collect(Collectors.toList());
        System.out.printf("Parse complete in %.1fs (%d files). Shutting down RPC...%n",
          (System.nanoTime() - t0) / 1e9, allSourceFiles.size());
        PythonRewriteRpc.shutdownCurrent();

        for (SourceFile sf : allSourceFiles) {
            long pt = System.nanoTime();
            System.out.print("Printing: " + sf.getSourcePath() + "...");
            String printed = sf.printAll();
            System.out.printf(" %.1fs%n", (System.nanoTime() - pt) / 1e9);
            String expected = Files.readString(dir.resolve(sf.getSourcePath()));
            assertThat(printed).as("printAll() for " + sf.getSourcePath()).isEqualTo(expected);
        }
    }

    @Test
    @Timeout(600)
    void parseParamikoProject() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"),
          "moderne/working-set-python-static-analysis-2/paramiko/paramiko");
        if (!Files.isDirectory(dir)) {
            return;
        }

        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder().timeout(Duration.ofHours(1)));
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        System.out.println("Parsing project: " + dir);
        long t0 = System.nanoTime();
        List<SourceFile> allSourceFiles = rpc.parseProject(dir, ctx)
          .peek(sf -> System.out.printf("  Parsed: %-50s (%.1fs elapsed)%n",
            sf.getSourcePath(), (System.nanoTime() - t0) / 1e9))
          .collect(Collectors.toList());
        System.out.printf("Parse complete in %.1fs (%d files). Shutting down RPC...%n",
          (System.nanoTime() - t0) / 1e9, allSourceFiles.size());
        PythonRewriteRpc.shutdownCurrent();

        for (SourceFile sf : allSourceFiles) {
            long pt = System.nanoTime();
            System.out.print("Printing: " + sf.getSourcePath() + "...");
            String printed = sf.printAll();
            System.out.printf(" %.1fs%n", (System.nanoTime() - pt) / 1e9);
            String expected = Files.readString(dir.resolve(sf.getSourcePath()));
            assertThat(printed).as("printAll() for " + sf.getSourcePath()).isEqualTo(expected);
        }
    }

    @Test
    @Timeout(60)
    void parseFabricSetupPyAlone() throws IOException {
        Path file = Path.of(System.getProperty("user.home"),
          "moderne/working-set-python-static-analysis-2/fabric/fabric/setup.py");
        if (!Files.isRegularFile(file)) {
            return;
        }

        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder().timeout(Duration.ofHours(1)));
        PythonParser parser = PythonParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        List<SourceFile> parsed = parser.parse(List.of(file), file.getParent(), ctx)
          .peek(sf -> System.out.println("Parsed: " + sf.getSourcePath()))
          .collect(Collectors.toList());
        PythonRewriteRpc.shutdownCurrent();

        assertThat(parsed).hasSize(1);
        System.out.println("Printing: " + parsed.get(0).getSourcePath());
        String printed = parsed.get(0).printAll();
        String expected = Files.readString(file);
        assertThat(printed).isEqualTo(expected);
        System.out.println("OK: " + printed.length() + " chars");
    }

    @Test
    void parseStringWithParser() {
        SourceFile sf = PythonParser.builder().build()
          .parse(
            //language=python
            """
              import sys
              print(sys.path)
              """)
          .findFirst()
          .get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sf).isInstanceOf(Py.CompilationUnit.class);
            softly.assertThat(sf.getMarkers().getMarkers()).isEmpty();
        });
    }
}

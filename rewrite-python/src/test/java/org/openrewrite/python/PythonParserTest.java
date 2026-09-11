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
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
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

    @Test
    void parsePython2WithLanguageLevel() {
        SourceFile sf = PythonParser.builder().languageLevel(PythonParser.PythonLanguageLevel.PYTHON_2_7).build()
          .parse("print \"hello\"\n")
          .findFirst()
          .get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sf).isInstanceOf(Py.CompilationUnit.class);
            softly.assertThat(sf.getMarkers().getMarkers()).isEmpty();
        });
    }

    @Test
    void parsePython2ComprehensiveRoundTrip() {
        String source =
          "import os\n" +
          "from os.path import join, dirname\n" +
          "\n" +
          "\n" +
          "def greet(name, greeting=\"hello\"):\n" +
          "    if name:\n" +
          "        print greeting, name\n" +
          "    else:\n" +
          "        print >> sys.stderr, \"missing name\"\n" +
          "    return greeting + name\n" +
          "\n" +
          "\n" +
          "class Worker(Base):\n" +
          "    def run(self, items):\n" +
          "        results = []\n" +
          "        for item in items:\n" +
          "            try:\n" +
          "                results.append(self.process(item))\n" +
          "            except ValueError, e:\n" +
          "                self.log(e)\n" +
          "        return results\n";

        SourceFile sf = PythonParser.builder().languageLevel(PythonParser.PythonLanguageLevel.PYTHON_2_7).build()
          .parse(source)
          .findFirst()
          .get();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sf).isInstanceOf(Py.CompilationUnit.class);
            softly.assertThat(sf.getMarkers().getMarkers()).isEmpty();
            softly.assertThat(sf.printAll()).isEqualTo(source);
        });
    }

    @Test
    void crlfFileOnDiskPrintsBackIdentically(@TempDir Path tempDir) throws IOException {
        // A file input reaches the RPC server as a path it reads itself, which
        // the source text of the tests above never exercises.
        String source = "import sys\r\n\r\n\r\ndef greet(name):\r\n    # a comment\r\n    print(name)\r\n";
        Path file = tempDir.resolve("crlf.py");
        Files.write(file, source.getBytes(UTF_8));

        SourceFile sf = PythonParser.builder().build()
          .parseInputs(singletonList(Parser.Input.fromFile(file)), tempDir,
            new InMemoryExecutionContext(Throwable::printStackTrace))
          .findFirst()
          .orElseThrow();

        assertThat(sf).isInstanceOf(Py.CompilationUnit.class);
        assertThat(sf.printAll()).isEqualTo(source);
    }
}

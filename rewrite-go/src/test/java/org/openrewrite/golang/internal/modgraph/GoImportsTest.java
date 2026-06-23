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

import static org.assertj.core.api.Assertions.assertThat;

class GoImportsTest {

    @Test
    void singleImport() {
        assertThat(GoImports.parse("package main\n\nimport \"fmt\"\n\nfunc main() {}\n"))
          .containsExactly("fmt");
    }

    @Test
    void blockImportsWithAliasBlankAndDot() {
        String src =
          "package p\n" +
          "\n" +
          "import (\n" +
          "\t\"fmt\"\n" +
          "\t_ \"github.com/lib/pq\"\n" +
          "\tm \"github.com/spf13/cobra\"\n" +
          "\t. \"errors\"\n" +
          ")\n" +
          "\n" +
          "func F() {}\n";
        assertThat(GoImports.parse(src))
          .containsExactlyInAnyOrder("fmt", "github.com/lib/pq", "github.com/spf13/cobra", "errors");
    }

    @Test
    void multipleImportDeclarations() {
        String src =
          "package p\n" +
          "import \"a\"\n" +
          "import (\n\t\"b\"\n\t\"c\"\n)\n" +
          "func g() {}\n";
        assertThat(GoImports.parse(src)).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void ignoresCommentsAndTokensInCodeBelow() {
        // The word `import` and quoted strings appear in comments and in code
        // after the import section; none must be treated as imports.
        String src =
          "// import \"not/an/import\"\n" +
          "package p\n" +
          "\n" +
          "/* import \"also/not\" */\n" +
          "import (\n" +
          "\t\"real/dep\" // trailing comment\n" +
          ")\n" +
          "\n" +
          "func main() {\n" +
          "\ts := \"import \\\"fake\\\"\"\n" +
          "\t_ = s\n" +
          "}\n";
        assertThat(GoImports.parse(src)).containsExactly("real/dep");
    }

    @Test
    void buildConstrainedFileStillYieldsImports() {
        // A //go:build windows file is parsed imports-only the same way.
        String src =
          "//go:build windows\n" +
          "\n" +
          "package p\n" +
          "\n" +
          "import _ \"github.com/inconshreveable/mousetrap\"\n";
        assertThat(GoImports.parse(src)).containsExactly("github.com/inconshreveable/mousetrap");
    }

    @Test
    void noImports() {
        assertThat(GoImports.parse("package p\n\nfunc main() {}\n")).isEmpty();
    }
}

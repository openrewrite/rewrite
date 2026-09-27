/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class OrderImportsCommentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports(false, null));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6143")
    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void preserveCommentsWhenOrderingImports(String lineEnding) {
        rewriteRun(
          java(
            """
              package com.example;

              /*
               * Copyright 2024 Example Inc.
               * Licensed under the Apache License, Version 2.0
               */
              import java.util.List;
              import java.io.File; // File I/O operations
              import java.util.ArrayList;
              """.replace("\n", lineEnding),
            """
              package com.example;

              /*
               * Copyright 2024 Example Inc.
               * Licensed under the Apache License, Version 2.0
               */
              import java.io.File; // File I/O operations
              import java.util.ArrayList;
              import java.util.List;
              """.replace("\n", lineEnding),
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports()).allSatisfy(anImport ->
              assertThat(anImport.getMarkers().getMarkers()).isEmpty()))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"012", "021", "102", "120", "201", "210"})
    void keepTrailingCommentsWithTheirImportInEveryOrder(String order) {
        String[] imports = {
          "import java.io.File; // Files",
          "import java.nio.file.Path; // Paths",
          "import java.util.List; // Lists"
        };
        String before = order.chars().mapToObj(index -> imports[index - '0']).collect(joining("\n"));
        String after = String.join("\n", imports);
        if (before.equals(after)) {
            rewriteRun(java(before));
        } else {
            rewriteRun(java(before, after));
        }
    }

    @ParameterizedTest
    @CsvSource({
      "java.util, ArrayList, List, false",
      "java.util, ArrayList, List, true",
      "static java.util.Collections, emptyList, singletonList, false",
      "static java.util.Collections, emptyList, singletonList, true"
    })
    void preserveCommentsWithoutChangingFoldingRules(String qualifier, String first, String second, boolean alwaysFold) {
        var style = ImportLayoutStyle.builder()
          .classCountToUseStarImport(alwaysFold ? 999 : 2)
          .nameCountToUseStarImport(alwaysFold ? 999 : 2)
          .importAllOthers()
          .importStaticAllOthers();
        if (alwaysFold) {
            style.packageToFold("java.util.*", false).staticPackageToFold("java.util.Collections.*", false);
        }
        rewriteRun(
          java(
            "import " + qualifier + "." + first + "; // First\n" +
            "import " + qualifier + "." + second + "; // Second",
            "// Second\nimport " + qualifier + ".*; // First",
            spec -> spec.markers(new NamedStyles(
              randomId(), "test", "Test", "Test", emptySet(), singletonList(style.build())))
          )
        );
    }

    @Test
    void preserveCommentsOnDuplicateImports() {
        rewriteRun(
          java(
            """
              import java.util.List; // First comment
              import java.util.List; // Second comment
              import java.io.File;
              """,
            """
              import java.io.File;
              import java.util.List; // First comment
              import java.util.List; // Second comment
              """
          )
        );
    }

    @Test
    void preserveLeadingCommentWhenImportMovesFirst() {
        rewriteRun(
          java(
            """
              import java.util.List;
              // File I/O operations
              import java.io.File;
              """,
            """
              // File I/O operations
              import java.io.File;
              import java.util.List;
              """
          )
        );
    }

    @Test
    void preserveTrailingCommentWhenImportMovesLast() {
        rewriteRun(
          java(
            """
              import java.util.List; // Lists
              import java.io.File;

              /** The application. */
              class A {}
              """,
            """
              import java.io.File;
              import java.util.List; // Lists

              /** The application. */
              class A {}
              """
          )
        );
    }

    @Test
    void distinguishTrailingAndLeadingComments() {
        rewriteRun(
          java(
            """
              import java.util.List; /* Lists */ // Collection types
              // File I/O operations
              import java.io.File;
              """,
            """
              // File I/O operations
              import java.io.File;
              import java.util.List; /* Lists */ // Collection types
              """
          )
        );
    }

    @Test
    void preserveCommentsOnAlreadyOrderedImports() {
        rewriteRun(
          java(
            """
              package com.example;

              /* Import header */
              import java.io.File; // Files
              // Collections
              import java.util.List; /* Lists */

              /** The application. */
              class A {}
              """
          )
        );
    }

    @Test
    void preserveCommentsWhenRemovingUnusedImports() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true, null)),
          java(
            """
              import java.util.List; // Lists
              import java.io.File; // Files
              import java.util.Set; // Unused sets

              class A {
                  List<File> files;
              }
              """,
            """
              import java.io.File; // Files
              import java.util.List; // Lists

              class A {
                  List<File> files;
              }
              """
          )
        );
    }

    @Test
    void preserveCommentsWhenUnfoldingWildcardImport() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true, null)),
          java(
            """
              import java.util.*; // Collections
              import java.io.File; // Files

              class A {
                  Map<String, List<File>> files;
              }
              """,
            """
              import java.io.File; // Files
              import java.util.List;
              import java.util.Map; // Collections

              class A {
                  Map<String, List<File>> files;
              }
              """
          )
        );
    }

    @Test
    void preserveCommentsWhenUnfoldingStaticWildcardImport() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true, null)),
          java(
            """
              import static java.util.Collections.*; // Collection helpers
              import java.util.List;

              class A {
                  List<String> empty = emptyList();
                  List<String> single = singletonList("a");
              }
              """,
            """
              import java.util.List;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.singletonList; // Collection helpers

              class A {
                  List<String> empty = emptyList();
                  List<String> single = singletonList("a");
              }
              """
          )
        );
    }

    @Test
    void preserveHeaderWhenRemovingAllImports() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(true, null)),
          java(
            """
              package com.example;

              /* Import header */
              import java.util.List; // Lists

              class A {}
              """,
            """
              package com.example;

              /* Import header */
              class A {}
              """
          )
        );
    }

    @Test
    void foldIntoCommentedStar() {
        rewriteRun(
          java(
            """
              import java.util.*; // Collections
              import java.util.List;
              """,
            """
              import java.util.*; // Collections
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void preserveFinalCommentWhenFolding(String lineEnding) {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.List; // Lists
              """.replace("\n", lineEnding),
            """
              // Lists
              import java.util.*;
              """.replace("\n", lineEnding)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void preserveCommentBeforeSemicolonWhenFolding(String lineEnding) {
        rewriteRun(
          java(
            """
              import java.util.List /* Lists */;
              import java.util.*; // Collections
              """.replace("\n", lineEnding),
            """
              /* Lists */
              import java.util.*; // Collections
              """.replace("\n", lineEnding)
          )
        );
    }
}

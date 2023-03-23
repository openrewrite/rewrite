/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;


class DataTableTest implements RewriteTest {

    @Test
    void resultsDataTable() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.fromRuntimeClasspath("org.openrewrite.java.cleanup.CommonStaticAnalysis"))
            .dataTable(SourcesFileResults.Row.class, rows -> {
                assertThat(rows)
                  .as("Running recipe CommonStaticAnalysis on two source files, if each file is changed " +
                      "by recipe ReplaceStringBuilderWithString only, so it should produce 4 rows in the SourcesFileResults " +
                      "table, and they are : " +
                      "row0 : file1 is changed by `CommonStaticAnalysis`" +
                      "row1 : file1 is changed by `ReplaceStringBuilderWithString`" +
                      "row2 : file2 is changed by `CommonStaticAnalysis`" +
                      "row3 : file2 is changed by `ReplaceStringBuilderWithString`" +
                      "file1,file2 order can be random in the results."
                      )
                  .hasSize(4);

                SourcesFileResults.Row row0 = rows.get(0);
                assertThat(row0.getRecipe()).isEqualTo("org.openrewrite.java.cleanup.CommonStaticAnalysis");
                SourcesFileResults.Row row1 = rows.get(1);
                assertThat(row1.getRecipe()).isEqualTo("org.openrewrite.java.cleanup.ReplaceStringBuilderWithString");
                SourcesFileResults.Row row2 = rows.get(2);
                assertThat(row2.getRecipe()).isEqualTo("org.openrewrite.java.cleanup.CommonStaticAnalysis");
                SourcesFileResults.Row row3 = rows.get(3);
                assertThat(row3.getRecipe()).isEqualTo("org.openrewrite.java.cleanup.ReplaceStringBuilderWithString");

                assertThat(row0.getSourcePath().equals("A.java") || row0.getSourcePath().equals("B.java")).isTrue();
                assertThat(row2.getSourcePath().equals("A.java") || row2.getSourcePath().equals("B.java")).isTrue();
                assertThat(row0.getSourcePath()).isEqualTo(row1.getSourcePath());
                assertThat(row2.getSourcePath()).isEqualTo(row3.getSourcePath());
                assertThat(row0.getSourcePath()).isNotEqualTo(row2.getSourcePath());
            }),
          java(
            """
              class A {
                  void foo() {
                      String s = new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      String s = "A" + "B";
                  }
              }
              """,
            spec -> spec.path("A.java")
          )
          ,
          java(
            """
              class B {
                  void foo() {
                      String s = new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class B {
                  void foo() {
                      String s = "A" + "B";
                  }
              }
              """,
            spec -> spec.path("B.java")
          )
        );
    }
}
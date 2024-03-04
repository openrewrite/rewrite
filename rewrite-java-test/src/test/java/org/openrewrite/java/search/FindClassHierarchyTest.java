/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.table.ClassHierarchy;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.PathUtils.separatorsToSystem;
import static org.openrewrite.java.Assertions.java;

class FindClassHierarchyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindClassHierarchy());
    }

    @Test
    void basic() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchy.Row.class, rows ->
            assertThat(rows).containsExactly(
              new ClassHierarchy.Row(separatorsToSystem("src/main/java/A.java"), "A", "java.lang.Object", "java.io.Serializable"))),

          java(
            //language=java
            """
              import java.io.Serializable;
              
              class A implements Serializable {
              }
              """,
            spec -> spec.path("src/main/java/A.java"))
        );
    }
}

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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.table.TypeMappings.*;

class FindTypeMappingsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindTypeMappings());
    }

    @Test
    void findTypeMappings() {
        rewriteRun(
          spec -> spec.dataTable(Row.class, table -> {
              assertThat(table).hasSize(14);
              boolean firstCount = false;
              boolean secondCount = false;
              for (Row row : table) {
                  if (row.getTreeName().equals("org.openrewrite.java.tree.J$ClassDeclaration") ||
                          row.getTreeName().equals("org.openrewrite.java.tree.J$Identifier") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Parameterized") ||
                          row.getTreeName().equals("org.openrewrite.java.tree.J$Identifier") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Variable") ||
                          row.getTreeName().equals("org.openrewrite.java.tree.J$NewClass") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Class") ||
                          row.getTreeName().equals("org.openrewrite.java.tree.J$NewClass") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Method") ||
                          row.getTreeName().equals("oorg.openrewrite.java.tree.J$VariableDeclarations$NamedVariable") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Parameterized") ||
                          row.getTreeName().equals("oorg.openrewrite.java.tree.J$VariableDeclarations$NamedVariable") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Variable") ||
                          row.getTreeName().equals("org.openrewrite.java.tree.J$VariableDeclarations")) {
                      assertThat(row.getCount()).isEqualTo(1);
                      firstCount = true;
                  } else if (row.getTreeName().equals("org.openrewrite.java.tree.J$Identifier") && row.getTypeName().equals("org.openrewrite.java.tree.JavaType$Class")) {
                      assertThat(row.getCount()).isEqualTo(6);
                      secondCount = true;
                  }
              }
              assertThat(firstCount).isTrue();
              assertThat(secondCount).isTrue();
            }),
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
              
              public class Test {
                  List<String> l = new ArrayList<>();
              }
              """
          )
        );
    }
}

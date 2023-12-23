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

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.table.TypeMappings.Row;

class FindTypeMappingsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindTypeMappings());
    }

    @Test
    void findTypeMappings() {
        rewriteRun(
          spec -> spec.dataTable(Row.class, table -> {
              assertThat(table.stream()
                .map(row -> "%-3s%-38s%-15s%d%s".formatted(row.getCompilationUnitName(), row.getTreeName(), row.getTypeName(), row.getCount(),
                  row.getNearestNonNullTreeName() == null ? "" : "  " + row.getNearestNonNullTreeName()))
                .sorted()
                .collect(Collectors.joining("\n", "", "\n")))
                .isEqualTo(
                  """
                    J  J$ClassDeclaration                    Class          1
                    J  J$FieldAccess                         Class          2
                    J  J$FieldAccess                         Unknown        2
                    J  J$Identifier                          Class          6
                    J  J$Identifier                          Parameterized  1
                    J  J$Identifier                          Unknown        4
                    J  J$Identifier                          Variable       1
                    J  J$Identifier                          null           10  J$Identifier
                    J  J$NewClass                            Method         1
                    J  J$ParameterizedType                   Parameterized  2
                    J  J$VariableDeclarations$NamedVariable  Variable       1
                    """
                );
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

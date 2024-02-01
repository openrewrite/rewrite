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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class JavaTemplateTest8Test implements RewriteTest {

    @Test
    void parameterizedMatch() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<String>)}")
          .doBeforeParseTemplate(System.out::println)
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  if (template.matches(getCursor())) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<String> s;
                  List<Integer> n;
              }
              """,
            """
              import java.util.List;
              class Test {
                  /*~~>*/List<String> s;
                  List<Integer> n;
              }
              """
          )
        );
    }
}

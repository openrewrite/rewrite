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
package org.openrewrite.kotlin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class KotlinTemplateTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceContextFreeStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return KotlinTemplate.builder("println(\"foo\")")
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val b1 = 1 == 2
                  }
              }
              """,
            """
              class Test {
                  fun foo() {
                      println("foo")
                  }
              }
              """
          ));
    }

    @Test
    void parserClasspath() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.toString().contains("ObjectMapper")) {
                      return multiVariable;
                  }
                  maybeAddImport(ObjectMapper.class.getName(), false);
                  Path path;
                  try {
                      path = Path.of(ObjectMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                  } catch (URISyntaxException e) {
                      throw new RuntimeException(e);
                  }
                  return KotlinTemplate.builder("val mapper = ObjectMapper()")
                    .parser(KotlinParser.builder().classpath(List.of(path)))
                    .imports(ObjectMapper.class.getName())
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          kotlin(
            """
              class Test {
                  fun foo() {
                      val b1 = 1 == 2
                  }
              }
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper
              
              class Test {
                  fun foo() {
                      val mapper = ObjectMapper()
                  }
              }
              """
          ));
    }
}

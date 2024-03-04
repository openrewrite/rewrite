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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateTest8Test implements RewriteTest {

    @Test
    void parameterizedMatch() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<String>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  /*~~>*/List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  /*~~>*/List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """
          )
        );
    }

    @Test
    void parameterizedMatchWithBounds() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<? extends java.lang.Number>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """,
            """
              import /*~~>*/java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  /*~~>*/List<Number> n;
                  /*~~>*/List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  /*~~>*/List<java.lang.Number> qn;
                  /*~~>*/List<java.lang.Integer> qi;
              }
              """
          )
        );
    }

    @Test
    void parameterizedArrayMatch() {
        JavaTemplate template = JavaTemplate.builder("#{anyArray(java.util.List<String>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object>[] o;
                  List<String>[] s;
                  List<Number>[] n;
                  List<Integer>[] i;
                  List<java.lang.Object>[] qo;
                  List<java.lang.String>[] qs;
                  List<java.lang.Number>[] qn;
                  List<java.lang.Integer>[] qi;
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<Object>[] o;
                  /*~~>*/List<String>[] s;
                  List<Number>[] n;
                  List<Integer>[] i;
                  List<java.lang.Object>[] qo;
                  /*~~>*/List<java.lang.String>[] qs;
                  List<java.lang.Number>[] qn;
                  List<java.lang.Integer>[] qi;
              }
              """
          )
        );
    }
}

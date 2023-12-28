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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ArrayTypeTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "String [] [ ] s;",
      """
      String [] [ ] method() {
          return null;
      }
      """
    })
    void arrayType(String input) {
        rewriteRun(
          java(
            String.format("""
              class Test {
                %s
              }
              """, input), spec -> spec.afterRecipe(cu -> {
                AtomicBoolean firstDimension = new AtomicBoolean(false);
                AtomicBoolean secondDimension = new AtomicBoolean(false);
                new JavaIsoVisitor<>() {
                    @Override
                    public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                        if (arrayType.getElementType() instanceof J.ArrayType) {
                            assertThat(arrayType.toString()).isEqualTo("String [] [ ]");
                            secondDimension.set(true);
                        } else {
                            assertThat(arrayType.toString()).isEqualTo("String []");
                            firstDimension.set(true);
                        }
                        return super.visitArrayType(arrayType, o);
                    }
                }.visit(cu, 0);
                assertThat(firstDimension.get()).isTrue();
                assertThat(secondDimension.get()).isTrue();
            })
          )
        );
    }

    @Test
    void javaTypesFromJsonCreatorConstructor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
                  if (arrayType.getElementType() instanceof J.ArrayType && arrayType.getMarkers().findFirst(SearchResult.class).isEmpty()) {
                      assert arrayType.getType() != null && "java.lang.Integer[][]".equals(arrayType.getType().toString());
                      // Construct a new J.ArrayType from an old LST model.
                      //noinspection deprecation
                      return new J.ArrayType(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY.addIfAbsent(new SearchResult(Tree.randomId(), "")),
                        ((J.ArrayType) arrayType.getElementType()).getElementType().withType(arrayType.getType()),
                        Arrays.asList(
                          JRightPadded.build(Space.EMPTY).withAfter(Space.build("", emptyList())),
                          JRightPadded.build(Space.EMPTY).withAfter(Space.build(" ", emptyList()))
                        ),
                        null,
                        null,
                        null
                      );
                  }
                  return super.visitArrayType(arrayType, ctx);
              }
          })),
          java(
            """
              class Test {
                  Integer[][ ] n = new Integer[0][0];
              }
              """,
            """
              class Test {
                  /*~~()~~>*/Integer[][ ] n = new Integer[0][0];
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                    assert arrayType.getType() != null;
                    if (arrayType.getElementType() instanceof J.ArrayType) {
                        assertThat(arrayType.getType().toString()).isEqualTo("java.lang.Integer[][]");
                        assertThat(arrayType.getDimension().getElement().getWhitespace()).isEqualTo(" ");
                    } else {
                        assertThat(arrayType.getType().toString()).isEqualTo("java.lang.Integer[]");
                        assert arrayType.getElementType().getType() != null;
                        assertThat(arrayType.getElementType().getType().toString()).isEqualTo("java.lang.Integer");
                    }
                    return super.visitArrayType(arrayType, o);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void singleDimensionalArrayFromJsonCreatorConstructor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
                  if (arrayType.getMarkers().findFirst(SearchResult.class).isEmpty()) {
                      assert arrayType.getType() == null || "java.lang.Integer[]".equals(arrayType.getType().toString());
                      // Construct a new J.ArrayType from an old LST model.
                      //noinspection deprecation
                      return new J.ArrayType(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY.addIfAbsent(new SearchResult(Tree.randomId(), "")),
                        arrayType.getElementType(),
                        singletonList(
                          JRightPadded.build(Space.EMPTY).withAfter(Space.build("", emptyList()))
                        ),
                        null,
                        null,
                        null
                      );
                  }
                  return super.visitArrayType(arrayType, ctx);
              }
          })),
          java(
            """
              class Test {
                  Integer[] n = new Integer[0];
              }
              """,
            """
              class Test {
                  /*~~()~~>*/Integer[] n = new Integer[0];
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                    assert arrayType.getType() != null;
                    assertThat(arrayType.getType().toString()).isEqualTo("java.lang.Integer");
                    return super.visitArrayType(arrayType, o);
                }
            }.visit(cu, 0))
          )
        );
    }
}

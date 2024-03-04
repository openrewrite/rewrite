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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
                            assertThat(arrayType.toString()).isEqualTo("String [ ]");
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
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public TypeTree visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
                  //noinspection SimplifyOptionalCallChains
                  if (!arrayType.getMarkers().findFirst(SearchResult.class).isPresent()) {
                      // Construct a new J.ArrayType from an old LST model.
                      List<JRightPadded<Space>> dimensions = new ArrayList<>();
                      assert arrayType.getDimension() != null;
                      dimensions.add(0, JRightPadded.build(arrayType.getDimension().getBefore()).withAfter(arrayType.getDimension().getElement()));
                      TypeTree elementType = arrayType.getElementType();
                      while (elementType instanceof J.ArrayType elementArrayType) {
                          assert elementArrayType.getDimension() != null;
                          dimensions.add(0, JRightPadded.build(elementArrayType.getDimension().getBefore()).withAfter(elementArrayType.getDimension().getElement()));
                          elementType = elementArrayType.getElementType();
                      }
                      //noinspection deprecation
                      return J.ArrayType.create(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY.addIfAbsent(new SearchResult(Tree.randomId(), "arr")),
                        elementType,
                        dimensions,
                        null,
                        null,
                        null
                      );
                  }
                  return arrayType;
              }
          })),
          java(
            """
              class Test {
                  Integer[ ] n1 = new Integer[0];
                  Integer[] [ ] n2 = new Integer[0][0];
                  Integer[][] [  ] n3 = new Integer[0][0][0];
              }
              """,
            """
              class Test {
                  /*~~(arr)~~>*/Integer[ ] n1 = new Integer[0];
                  /*~~(arr)~~>*/Integer[] [ ] n2 = new Integer[0][0];
                  /*~~(arr)~~>*/Integer[][] [  ] n3 = new Integer[0][0][0];
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.ArrayType visitArrayType(J.ArrayType arrayType, Integer p) {
                    assertThat(arrayType.getType()).isNotNull();
                    assertThat(arrayType.getType()).isInstanceOf(JavaType.Array.class);
                    assertThat(arrayType.getElementType().getType()).isEqualTo(((JavaType.Array) arrayType.getType()).getElemType());
                    return super.visitArrayType(arrayType, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void arrayTypeWithoutDimensions() {
        J.Identifier elementType = new J.Identifier(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          Collections.emptyList(),
          "String",
          JavaType.ShallowClass.build("java.lang.String"),
          null
        );
        //noinspection deprecation
        J.ArrayType migratedArrayType = J.ArrayType.create(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          elementType,
          Collections.emptyList(),
          null,
          null,
          null
        );
        assertThat(migratedArrayType.toString()).isEqualTo("String");
    }

    @Disabled("Fails print idempotency test")
    @Test
    void annotatedVarargsParameter() {
        rewriteRun(
          java(
            """
              class Test {
                  void m1(String @Deprecated... s) {}
              }
              """
          )
        );
    }

    @Disabled("Fails print idempotency test")
    @Test
    @SuppressWarnings("CStyleArrayDeclaration")
    void annotatedCStyleArrayParameter() {
        rewriteRun(
          java(
            """
              class Test {
                  void m2(String  s @Deprecated[]) {}
              }
              """
          )
        );
    }
}

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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SpacesTest implements RewriteTest {

    // This test ensures spaces stored in markers are visited and may be removed once the SpacesVisitor and tests are implemented.
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/192")
    @SuppressWarnings("RedundantNullableReturnType")
    @Test
    void visitsMarkerLocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, KSpace.Location loc, ExecutionContext executionContext) {
                  if (!space.getComments().isEmpty()) {
                      return space;
                  }
                  if (loc == KSpace.Location.TYPE_REFERENCE_PREFIX || loc == KSpace.Location.IS_NULLABLE_PREFIX || loc == KSpace.Location.CHECK_NOT_NULL_PREFIX) {
                      return space.withComments(ListUtils.concat(new TextComment(true, loc.name(), "", Markers.EMPTY), space.getComments()));
                  }
                  return super.visitSpace(space, loc, executionContext);
              }
          })),
          kotlin(
            """
              class A {
                  fun method ( ) : Int ? {
                      return 1
                  }
              }
              """,
            """
              class A {
                  fun method ( ) /*TYPE_REFERENCE_PREFIX*/: Int /*IS_NULLABLE_PREFIX*/? {
                      return 1
                  }
              }
              """
          ),
          kotlin(
            """
              val a = A ( )
              val b = a . method ( ) !!
              val c = b !!
              """,
            """
              val a = A ( )
              val b = a . method ( ) /*CHECK_NOT_NULL_PREFIX*/!!
              val c = b /*CHECK_NOT_NULL_PREFIX*/!!
              """
          )
        );
    }
}

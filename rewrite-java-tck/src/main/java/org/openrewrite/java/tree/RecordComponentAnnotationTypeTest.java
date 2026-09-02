/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

/**
 * A record component's annotations are propagated to whichever of the component, the field, the accessor
 * method and the constructor parameter the annotation is applicable to (JLS 9.7.4). Only those applicable
 * to the component itself appear in {@code Symbol.RecordComponent#getAnnotationMirrors()}, while
 * {@code #getOriginalAnnos()} holds every annotation written in the record header. The two lists therefore
 * do not line up, and the parser attributes one from the other by index.
 */
@MinimumJava17
class RecordComponentAnnotationTypeTest implements RewriteTest {

    private static final String ANNOTATIONS = """
      import java.lang.annotation.ElementType;
      import java.lang.annotation.Target;

      @Target(ElementType.METHOD)
      @interface OnMethod {
      }

      @Target(ElementType.RECORD_COMPONENT)
      @interface OnComponent {
      }

      @Target(ElementType.FIELD)
      @interface OnField {
      }
      """;

    private static Consumer<SourceSpec<J.CompilationUnit>> annotationTypes(String... expected) {
        return spec -> spec.afterRecipe(cu -> {
            List<String> actual = new ArrayList<>();
            new JavaIsoVisitor<Integer>() {
                @Override
                public J.Annotation visitAnnotation(J.Annotation annotation, Integer p) {
                    JavaType type = annotation.getAnnotationType().getType();
                    actual.add(annotation.getSimpleName() + " -> " + (type == null ? "null" : TypeUtils.toString(type)));
                    return super.visitAnnotation(annotation, p);
                }
            }.visit(cu, 0);
            assertThat(actual).containsExactly(expected);
        });
    }

    @Test
    void componentApplicableAnnotation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ANNOTATIONS),
          java(
            """
              record ComponentApplicable(@OnComponent String name) {
              }
              """,
            annotationTypes("OnComponent -> OnComponent")
          )
        );
    }

    @Disabled("Resolves to Unknown: the annotation reaches only the accessor, so it has no mirror on the component")
    @Test
    void accessorOnlyAnnotation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ANNOTATIONS),
          java(
            """
              record AccessorOnly(@OnMethod String name) {
              }
              """,
            annotationTypes("OnMethod -> OnMethod")
          )
        );
    }

    @Disabled("OnMethod resolves to OnComponent: the mirror for the second annotation is attributed to the first")
    @Test
    void accessorOnlyAnnotationBeforeComponentApplicableOne() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ANNOTATIONS),
          java(
            """
              record AccessorOnlyFirst(@OnMethod @OnComponent String name) {
              }
              """,
            annotationTypes("OnMethod -> OnMethod", "OnComponent -> OnComponent")
          )
        );
    }

    @Disabled("OnMethod resolves to Unknown: the mirror list is shorter, so the trailing annotation is never attributed")
    @Test
    void componentApplicableAnnotationBeforeAccessorOnlyOne() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ANNOTATIONS),
          java(
            """
              record AccessorOnlyLast(@OnComponent @OnMethod String name) {
              }
              """,
            annotationTypes("OnComponent -> OnComponent", "OnMethod -> OnMethod")
          )
        );
    }

    @Disabled("OnComponent resolves to Unknown: a field-only annotation shifts the mirror off its annotation")
    @Test
    void fieldOnlyAnnotationBeforeComponentApplicableOne() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ANNOTATIONS),
          java(
            """
              record FieldOnlyFirst(@OnField @OnComponent String name) {
              }
              """,
            annotationTypes("OnField -> OnField", "OnComponent -> OnComponent")
          )
        );
    }
}

package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/3453")
    @Test
    void annotatedArrayType() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;
              
              class TypeAnnotationTest {
                  Integer @A1 [] @A2 [ ] integers;
              
                  @Target(ElementType.TYPE_USE)
                  private @interface A1 {
                  }
              
                  @Target(ElementType.TYPE_USE)
                  private @interface A2 {
                  }
              }
              """, spec -> spec.afterRecipe(cu -> {
                AtomicBoolean firstDimension = new AtomicBoolean(false);
                AtomicBoolean secondDimension = new AtomicBoolean(false);
                new JavaIsoVisitor<>() {
                    @Override
                    public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                        if (arrayType.getElementType() instanceof J.ArrayType) {
                            if (arrayType.getAnnotations() != null && !arrayType.getAnnotations().isEmpty()) {
                                assertThat(arrayType.getAnnotations().get(0).getAnnotationType().toString()).isEqualTo("A1");
                                assertThat(arrayType.toString()).isEqualTo("Integer @A1 [] @A2 [ ]");
                                firstDimension.set(true);
                            }
                        } else {
                            if (arrayType.getAnnotations() != null && !arrayType.getAnnotations().isEmpty()) {
                                assertThat(arrayType.getAnnotations().get(0).getAnnotationType().toString()).isEqualTo("A2");
                                assertThat(arrayType.toString()).isEqualTo("Integer @A2 [ ]");
                                secondDimension.set(true);
                            }
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
}

/*
 * Copyright 2022 the original author or authors.
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
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

/**
 * @author Alex Boyko
 */
class JavaParserTest implements RewriteTest {

    @Test
    void incompleteAssignment() {
        rewriteRun(
          java(
            """
              @Deprecated(since=)
              public class A {}
               """
          )
        );
    }

    @SuppressWarnings("RedundantSuppression")
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    @Test
    void annotationCommentWithNoSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("serial")// fred
              @Deprecated
              public class PersistenceManagerImpl {
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(cu.getClasses().get(0).getLeadingAnnotations()).hasSize(2))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    @Test
    void annotationCommentWithSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("ALL") // fred
              @Deprecated
              public class PersistenceManagerImpl {
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(cu.getClasses().get(0).getLeadingAnnotations()).hasSize(2))
          )
        );
    }

    @Test
    void dependenciesFromResources(@TempDir Path temp) {
        JavaParserExecutionContextView ctx = JavaParserExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setParserClasspathDownloadTarget(temp.toFile());
        assertThat(JavaParser.dependenciesFromResources(ctx, "guava-31.0-jre")).isNotEmpty();
    }

    @Test
    void resetParserTest() throws Exception {
        JavaParser parser = JavaParser.fromJavaVersion().build();
        String source = """
          import java.util.List;
          import java.util.ArrayList;
          
          class Something {
              List<Integer> getList() {
                  System.out.println("hello");
                  return new ArrayList<>();
              }
          }
          """;
        List<J.CompilationUnit> cus = parser.parse(source).collect(Collectors.toList());

        J.CompilationUnit c = cus.get(0);
        J.MethodDeclaration m = c.getClasses().get(0).getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast).findFirst().orElseThrow();
        JavaType.Method methodType = m.getMethodType();
        assertThat(TypeUtils.asFullyQualified(methodType.getReturnType()).getFullyQualifiedName()).isEqualTo("java.util.List");

        parser.reset(cus.stream().map(cu -> cu.getSourcePath().toUri()).collect(Collectors.toList()));
//        parser.reset();

        cus = parser.parse(source).collect(Collectors.toList());
        assertThat(cus.size()).isEqualTo(1);
        assertThat(cus.get(0).getClasses().size()).isEqualTo(1);

        c = cus.get(0);
        m = c.getClasses().get(0).getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast).findFirst().orElseThrow();
        methodType = m.getMethodType();
        assertThat(TypeUtils.asFullyQualified(methodType.getReturnType()).getFullyQualifiedName()).isEqualTo("java.util.List");

    }

}

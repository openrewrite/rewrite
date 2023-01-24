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
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

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
}

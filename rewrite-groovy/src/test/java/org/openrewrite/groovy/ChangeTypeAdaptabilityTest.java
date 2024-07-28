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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

/**
 * Prove that {@link ChangeType}, written for Java, can adapt to working on Groovy code.
 */
class ChangeTypeAdaptabilityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("a.b.Original", "x.y.Target", true));
    }

    @SuppressWarnings("GrPackage")
    @Test
    void changeImport() {
        rewriteRun(
          groovy(
            """
              package a.b
              class Original {}
              """
          ),
          groovy(
            """
              import a.b.Original
                          
              class A {
                  Original type
              }
              """,
            """
              import x.y.Target
                          
              class A {
                  Target type
              }
              """
          )
        );
    }

    @SuppressWarnings("GrPackage")
    @Test
    void changeType() {
        rewriteRun(
          groovy(
            """
              package a.b
              class Original {}
              """
          ),
          groovy(
            """
              class A {
                  a.b.Original type
              }
              """,
            """
              class A {
                  x.y.Target type
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void changeDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("file", "newFile", false)),
          groovy(
            """
              class file {
              }
              """,
            """
              class newFile {
              }
              """,
            spec -> spec.path("file.groovy").afterRecipe(cu -> {
                assertThat("newFile.groovy").isEqualTo(cu.getSourcePath().toString());
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "newFile")).isTrue();
            })
          )
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @ExpectedToFail("fails because there's a reference change but no content diff but that's the point; would need to adjust RewriteTest")
    @Issue("https://github.com/openrewrite/rewrite/issues/3058")
    @Test
    void changeTypeAttributionImplicitUsage() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.List", "java.util.ArrayList", false)),
          groovy(
            """
              import java.util.Collections
                
              class Test {
                  int zero = Collections.emptyList().size()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations varDecl = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
                J.MethodInvocation sizeMi = (J.MethodInvocation) varDecl.getVariables().get(0).getInitializer();
                assertThat(TypeUtils.isOfClassType(sizeMi.getMethodType().getDeclaringType(),
                  "java.util.ArrayList")).isTrue();
                J.MethodInvocation emptyListMi = (J.MethodInvocation) sizeMi.getSelect();
                assertThat(TypeUtils.isOfClassType(emptyListMi.getMethodType().getReturnType(),
                  "java.util.ArrayList")).isTrue();
            })
          )
        );
    }
}

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

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

// This class may be removed after recipes with Assertions are added.
public class AssertionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                if ("a".equals(variable.getSimpleName())) {
                    return variable.withName(variable.getName().withSimpleName("b"));
                }
                return variable;
            }
        }));
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/30")
    @Test
    void isChanged() {
        rewriteRun(
          kotlin(
            """
            class A {
                val a = 1
            }
            """,
            """
            class A {
                val b = 1
            }
            """
          )
        );
    }

    /**
     * This test is expected to fail due to invalid syntax and exists to prevent a regression in the compiler configuration.
     * {@link AnalyzerWithCompilerReport} is used to analyze the syntax in each source to produce a ParserError if a syntax
     * error exists in the source code.
     */
    @ExpectedToFail
    @Test
    void invalidSyntax() {
        rewriteRun(
          kotlin(
            //language=none
            """
              a++
              """
          )
        );
    }
}

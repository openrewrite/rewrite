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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class GradleJavaTemplateTest implements RewriteTest {

    @Test
    @Disabled("work in progress")
    void useJavaTemplateInBuildGradle() {
        Recipe addDependency = toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                if (block.getStatements().isEmpty()) {
                    return block.withTemplate(
                      JavaTemplate
                        .builder(this::getCursor, "implementation(\"com.google.guava:guava:latest.release\")")
                        .doBeforeParseTemplate(System.out::println)
                        .build(),
                      block.getCoordinates().replace()
                    );
                }
                return super.visitBlock(block, ctx);
            }
        });

        rewriteRun(
          spec -> spec.recipe(addDependency),
          buildGradle(
            """
              dependencies {
              }
              """,
            """
              dependencies {
                  implementation("com.google.guava:guava:latest.release")
              }
              """
          )
        );
    }
}

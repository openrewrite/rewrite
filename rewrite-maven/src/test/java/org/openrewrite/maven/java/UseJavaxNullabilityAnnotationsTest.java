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
package org.openrewrite.maven.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * <Beschreibung>
 * <br>
 *
 * @author askrock
 */
class UseJavaxNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.maven.java")
          .build()
          .activateRecipes("org.openrewrite.maven.java.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void addsMavenDependencyIfNecessary() {
        rewriteRun(mavenProject("nullability",
          pomXml("""
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>rewrite</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>rewrite</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>javax.annotation</groupId>
                    <artifactId>javax.annotation-api</artifactId>
                    <version>1</version>
                  </dependency>
                </dependencies>
              </project>
              """),
          srcMainJava(java("""
            import org.openrewrite.internal.lang.NonNull;
                            
            class Test {
              @NonNull
              String nonNullVariable = "";
            }
            """, """
            import javax.annotation.Nonnull;
                            
            class Test {
              @Nonnull
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }

    @Test
    void doesNotAddMavenDependencyIfUnnecessary() {
        rewriteRun(mavenProject("nullability",
          pomXml("""
            <project>
              <groupId>org.openrewrite</groupId>
              <artifactId>rewrite</artifactId>
              <version>1</version>
            </project>
            """),
          srcMainJava(java(""" 
            class Test {
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }
}
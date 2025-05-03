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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

class AddPlatformDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api", "guava", "jackson-databind", "jackson-core", "lombok"));
    }

    @Language("java")
    private final String usingGuavaIntMath = """
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
            }
      """;

    @Test
    void onlyIfUsingTestScope() {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, null, null)),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                """,
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    testImplementation platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """
            )
          )
        );
    }

}

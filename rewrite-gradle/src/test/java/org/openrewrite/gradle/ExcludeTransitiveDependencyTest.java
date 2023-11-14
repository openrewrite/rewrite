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
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

@SuppressWarnings("FieldCanBeLocal")
class ExcludeTransitiveDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("commons-beanutils"));
    }

    @Language("java")
    private final String usingBeanutilsSetSimplePropertyOnly = """
      import org.apache.commons.beanutils.PropertyUtils;

      class Person {
      }

      public class A {
          public static void main(String[] args) throws Exception {
              Object person = new Person();
              PropertyUtils.setSimpleProperty(person, "name", "Bart Simpson");
              PropertyUtils.setSimpleProperty(person, "age", 38);
          }
      }
      """;

    @Test
    void regularExclusion() {
        ExcludeTransitiveDependency addDep = new ExcludeTransitiveDependency("commons-beanutils", "commons-beanutils", "1.9.4", null, null, "org.apache.commons.beanutils.PropertyUtils",
          null, null, null, "commons-collections", "commons-collections");
        rewriteRun(
          spec -> spec.recipe(addDep)
            .typeValidationOptions(TypeValidation.none()),
          mavenProject("project",
            srcTestJava(
              java(usingBeanutilsSetSimplePropertyOnly)
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                    id "com.netflix.nebula.facet" version "10.1.3"
                }

                repositories {
                    mavenCentral()
                }

                facets {
                    smokeTest {
                        parentSourceSet = "test"
                    }
                }
                """,
              """
                plugins {
                    id "java-library"
                    id "com.netflix.nebula.facet" version "10.1.3"
                }
                
                repositories {
                    mavenCentral()
                }
                
                facets {
                    smokeTest {
                        parentSourceSet = "test"
                    }
                }
                
                dependencies {
                    testImplementation("commons-beanutils:commons-beanutils:1.9.4") {
                        exclude group: "commons-collections", module: "commons-collections"
                    }
                }
                """
            )
          )
        );
    }



    // TODO: No exclusion. Assert does not add if unnecessary

    // TODO: No exclusion. Assert does not add is already present

    // TODO: Adds exclusion. Assert adds dependency if the dependency matches target.

    // TODO: Adds exclusion to all applicable. Assert adds dependency if a different dependency adds target transitively.


}

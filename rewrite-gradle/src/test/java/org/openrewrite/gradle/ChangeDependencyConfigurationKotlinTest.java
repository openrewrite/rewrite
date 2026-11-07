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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class ChangeDependencyConfigurationKotlinTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDependencyConfiguration("junit", "junit", "testImplementation", "testCompile"));
    }
    private static GradleProject originalProject() {
        return GradleProject.builder().nameToConfiguration(singletonMap("testCompile",
          GradleDependencyConfiguration.builder().name("testCompile").extendsFrom(emptyList())
            .requested(emptyList()).directResolved(emptyList()).build())).build();
    }
    @Test void changesRemovedKotlinConfiguration() {
        rewriteRun(buildGradleKts(
          "dependencies { testCompile(\"junit:junit:4.12\") }",
          "dependencies { testImplementation(\"junit:junit:4.12\") }", s -> s.markers(originalProject())));
    }
    @Test void retainsKotlinDependencyClosureInSubprojects() {
        rewriteRun(buildGradleKts("""
          subprojects {
              dependencies {
                  // Keep the exclusion.
                  testCompile("junit:junit:4.12") { exclude(group = "org.hamcrest") }
              }
          }
          """, """
          subprojects {
              dependencies {
                  // Keep the exclusion.
                  testImplementation("junit:junit:4.12") { exclude(group = "org.hamcrest") }
              }
          }
          """, s -> s.markers(originalProject())));
    }
    @Test void unrelatedCoordinateAndConfigurationAreUnchanged() {
        rewriteRun(buildGradleKts("""
          dependencies {
              testCompile("other:other:1.0")
              implementation("junit:junit:4.12")
          }
          """, s -> s.markers(originalProject())));
    }
    @Test void customConfigurationReceiverIsUnchanged() {
        rewriteRun(buildGradleKts("dependencies { custom.testCompile(\"junit:junit:4.12\") }",
          s -> s.markers(originalProject())));
    }
    @Test void customDependenciesReceiverIsUnchanged() {
        rewriteRun(buildGradleKts("custom.dependencies { testCompile(\"junit:junit:4.12\") }",
          s -> s.markers(originalProject())));
    }
    @Test void nestedCustomDslIsUnchanged() {
        rewriteRun(buildGradleKts("dependencies { custom { testCompile(\"junit:junit:4.12\") } }",
          s -> s.markers(originalProject())));
    }
    @Test void callOutsideDependenciesIsUnchanged() {
        rewriteRun(buildGradleKts("testCompile(\"junit:junit:4.12\")", s -> s.markers(originalProject())));
    }
    @Test void groovyLiteralAndMapControlsRemainSupported() {
        rewriteRun(buildGradle("""
          dependencies {
              testCompile 'junit:junit:4.12'
              testCompile group: 'junit', name: 'junit', version: '4.12'
          }
          """, """
          dependencies {
              testImplementation 'junit:junit:4.12'
              testImplementation group: 'junit', name: 'junit', version: '4.12'
          }
          """, s -> s.markers(originalProject())));
    }

    @Test void kotlinVersionTemplatePreservesExpression() {
        rewriteRun(buildGradleKts("""
          val junitVersion = "4.12"
          dependencies { testCompile("junit:junit:$junitVersion") }
          """, """
          val junitVersion = "4.12"
          dependencies { testImplementation("junit:junit:$junitVersion") }
          """, s -> s.markers(originalProject())));
    }
    @Test void kotlinMultiComponentNotationPreservesArguments() {
        rewriteRun(buildGradleKts("""
          dependencies { testCompile("junit", "junit", "4.12") }
          """, """
          dependencies { testImplementation("junit", "junit", "4.12") }
          """, s -> s.markers(originalProject())));
    }
    @Test void kotlinNamedNotationPreservesArguments() {
        rewriteRun(buildGradleKts("""
          dependencies { testCompile(group = "junit", name = "junit", version = "4.12") }
          """, """
          dependencies { testImplementation(group = "junit", name = "junit", version = "4.12") }
          """, s -> s.markers(originalProject())));
    }
    @Test void kotlinPlatformDeclarationRetainsWrapper() {
        rewriteRun(spec -> spec.recipe(new ChangeDependencyConfiguration("org.junit", "junit-bom", "testImplementation", "testCompile")),
          buildGradleKts("""
            dependencies { testCompile(platform("org.junit:junit-bom:5.7.1")) }
            """, """
            dependencies { testImplementation(platform("org.junit:junit-bom:5.7.1")) }
            """, s -> s.markers(originalProject())));
    }
    @Test void kotlinProjectDeclarationRetainsWrapper() {
        rewriteRun(spec -> spec.recipe(new ChangeDependencyConfiguration("*", "*", "testImplementation", "testCompile")),
          buildGradleKts("""
            dependencies { testCompile(project(":common")) }
            """, """
            dependencies { testImplementation(project(":common")) }
            """, s -> s.markers(originalProject())));
    }
    @Test void constraintsAreNotDependencies() {
        rewriteRun(buildGradleKts("""
          dependencies { constraints { testCompile("junit:junit:4.12") } }
          """, s -> s.markers(originalProject())));
    }
    @Test void unmodeledKotlinConfigurationIsNotAssumed() {
        rewriteRun(buildGradleKts("""
          dependencies { testCompile("junit:junit:4.12") }
          """));
    }

    @Test void groovyVersionTemplateRemainsSupported() {
        rewriteRun(buildGradle("""
          def version = '4.12'
          dependencies { testCompile "junit:junit:$version" }
          """, """
          def version = '4.12'
          dependencies { testImplementation "junit:junit:$version" }
          """, s -> s.markers(originalProject())));
    }
    @Test void groovyProjectAndPlatformRemainSupported() {
        rewriteRun(spec -> spec.recipe(new ChangeDependencyConfiguration("*", "*", "testImplementation", "testCompile")),
          buildGradle("""
            dependencies {
                testCompile project(':common')
                testCompile platform('org.junit:junit-bom:5.7.1')
            }
            """, """
            dependencies {
                testImplementation project(':common')
                testImplementation platform('org.junit:junit-bom:5.7.1')
            }
            """, s -> s.markers(originalProject())));
    }
}

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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;

class GradleParserTest implements RewriteTest {

    @Test
    void buildGradleAndSettingsGradle() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              gradleEnterprise {
                  server = 'https://enterprise-samples.gradle.com'
                  buildScan {
                      publishAlways()
                  }
              }
              """
          )
        );
    }

    @Test
    void allowImports() {
        rewriteRun(
          buildGradle(
            """
              import org.gradle.api.Project

              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.openrewrite:rewrite-java:latest.release"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(4);
                assertThat(cu.getStatements().getFirst()).isInstanceOf(J.Import.class);
                var i = (J.Import) cu.getStatements().getFirst();
                assertThat(i.getTypeName()).isEqualTo("org.gradle.api.Project");
                assertThat(cu.getStatements().get(3)).isInstanceOf(J.MethodInvocation.class);
                var m = (J.MethodInvocation) cu.getStatements().get(3);
                assertThat(m.getMethodType()).isNotNull();
                assertThat(m.getMethodType().getDeclaringType().getFullyQualifiedName()).isNotNull();
            })
          )
        );
    }

    @Test
    void allowMethodDeclaration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.openrewrite:rewrite-java:latest.release"
              }

              def greet() {
                  return "Hello, world!"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(4);
                assertThat(cu.getStatements().get(2)).isInstanceOf(J.MethodInvocation.class);
                var m = (J.MethodInvocation) cu.getStatements().get(2);
                assertThat(m.getMethodType()).isNotNull();
                assertThat(m.getMethodType().getDeclaringType().getFullyQualifiedName()).isNotNull();
                assertThat(cu.getStatements().get(3)).isInstanceOf(J.MethodDeclaration.class);
                var d = (J.MethodDeclaration) cu.getStatements().get(3);
                assertThat(d.getSimpleName()).isEqualTo("greet");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5270")
    @Test
    void develocityExtensionIsTypeAttributed() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.19.2'
              }
              develocity {
                  server = 'https://dev.example.com/'
                  buildScan {
                      publishing.onlyIf { false }
                  }
                  buildCache {
                      remote(develocity.buildCache) {
                          enabled = true
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var declaringTypes = new ArrayList<String>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        JavaType.Method type = method.getMethodType();
                        declaringTypes.add(method.getSimpleName() + ": " +
                                           (type == null ? null : type.getDeclaringType().getFullyQualifiedName()));
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                assertThat(declaringTypes).contains(
                  "develocity: RewriteSettings",
                  "buildScan: DevelocitySpec",
                  "onlyIf: com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration",
                  "buildCache: org.gradle.api.initialization.Settings"
                );
                assertThat(FindMethods.find(cu, "org.gradle.api.initialization.Settings plugins(..)", true)).isNotEmpty();
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "tasks.register('report')",
      "tasks.named('report')",
      "tasks.create('report')",
      "task('report')",
      "task(name: 'report')"
    })
    void taskConfigurationBlockIsTypeAttributed(String declaration) {
        rewriteRun(
          buildGradle(
            """
              %s {
                  dependsOn 'check'
                  doLast {
                      println 'done'
                  }
              }
              """.formatted(declaration),
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.find(cu, "org.gradle.api.Task dependsOn(..)", true)).isNotEmpty();
                assertThat(FindMethods.find(cu, "org.gradle.api.Task doLast(..)", true)).isNotEmpty();
            })
          )
        );
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
      tasks.register('copyDocs', Copy) { from 'docs' }               | org.gradle.api.tasks.AbstractCopyTask from(..)
      tasks.withType(Copy).configureEach { from 'docs' }             | org.gradle.api.tasks.AbstractCopyTask from(..)
      tasks.named('jar', Jar) { manifest { attributes('a': 'b') } }  | org.gradle.api.java.archives.Manifest attributes(..)
      task copyDocs(type: Copy) { from 'docs' }                      | org.gradle.api.tasks.AbstractCopyTask from(..)
      task report { dependsOn 'check' }                              | org.gradle.api.Task dependsOn(..)
      java { toolchain { } }                                         | org.gradle.api.plugins.JavaPluginExtension toolchain(..)
      sourceSets { main { java { srcDirs 'src' } } }                 | org.gradle.api.file.SourceDirectorySet srcDirs(..)
      publishing { publications { maven(MavenPublication) { pom { } } } } | org.gradle.api.publish.maven.MavenPublication pom(..)
      dependencies { errorProne('a:b:1') { exclude group: 'c' } }    | org.gradle.api.artifacts.ModuleDependency exclude(..)
      """)
    void closureDelegatesToTheObjectGradleConfiguresWithIt(String script, String method) {
        rewriteRun(
          buildGradle(script, spec -> spec.afterRecipe(cu ->
            assertThat(FindMethods.find(cu, method)).isNotEmpty()))
        );
    }

    @Test
    void configurationNotDeclaredByTheStubsIsADependencyDeclaration() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  testFixturesImplementation 'org.slf4j:slf4j-api:2.0.0'
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindMethods.find(cu, "DependencyHandlerSpec testFixturesImplementation(..)")).isNotEmpty())
          )
        );
    }

    @Test
    void dependenciesBlockOfAnUnknownPluginDeclaresNoDependencies() {
        rewriteRun(
          buildGradle(
            """
              dependencyManagement {
                  dependencies {
                      dependency 'org.slf4j:slf4j-api:2.0.0'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindMethods.find(cu, "DependencyHandlerSpec dependency(..)")).isEmpty())
          )
        );
    }

    @Test
    void apiOfTheSelectedGradleVersion() {
        // Gradle 9 removed JavaPluginConvention
        @Language("groovy") String script = """
          JavaPluginConvention convention = null
          convention.getSourceSets()
          """;
        rewriteRun(
          buildGradle(script, spec -> spec.afterRecipe(cu ->
            assertThat(FindMethods.find(cu, "org.gradle.api.plugins.JavaPluginConvention getSourceSets()")).isEmpty()))
        );
        rewriteRun(
          spec -> spec.parser(GradleParser.builder().gradleVersion("8.5")),
          buildGradle(script, spec -> spec.afterRecipe(cu ->
            assertThat(FindMethods.find(cu, "org.gradle.api.plugins.JavaPluginConvention getSourceSets()")).isNotEmpty()))
        );
    }

    @Test
    void gradleVersionOfTheProjectWrapper(@TempDir Path project) throws Exception {
        Path wrapper = Files.createDirectories(project.resolve("gradle/wrapper")).resolve("gradle-wrapper.properties");
        Files.writeString(wrapper, "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-rc-1-bin.zip\n");
        SourceFile buildGradle = GradleParser.builder().build().parseInputs(
            List.of(Parser.Input.fromString(project.resolve("build.gradle"), """
              JavaPluginConvention convention = null
              convention.getSourceSets()
              """)), project, new InMemoryExecutionContext(Throwable::printStackTrace))
          .findFirst().orElseThrow();
        assertThat(FindMethods.find((J) buildGradle, "org.gradle.api.plugins.JavaPluginConvention getSourceSets()")).isNotEmpty();
    }

    @Test
    void dontDropLeadingComments() {
        rewriteRun(
          buildGradle(
            """
              /*
               * LICENSE
               */
              import org.gradle.api.Project

              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void dontClobberExistingComments() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              // Some comment
              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void handleImportsThatArentTheFirstStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              // Deliberately not first, as per test
              import org.gradle.api.Project

              repositories {
                  mavenCentral()
              }

              dependencies {
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void dependencyNotations() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {

                  // String notation
                  implementation "org.openrewrite:rewrite-java:latest.release"
                  implementation ("org.openrewrite:rewrite-java:latest.release")
                  implementation ("org.openrewrite:rewrite-java:latest.release") { transitive = false }
                  implementation ("org.openrewrite:rewrite-java:latest.release") {
                      transitive = false
                  }
                  implementation ( "org.openrewrite:rewrite-java:latest.release" )
                  implementation ( "org.openrewrite:rewrite-java:latest.release" ) { transitive = false }
                  implementation ( "org.openrewrite:rewrite-java:latest.release" ) {
                      transitive = false
                  }

                  // Map notation
                  implementation group: "org.openrewrite", name: "rewrite-java", version: "latest.release"
                  implementation(group: "org.openrewrite", name: "rewrite-java", version: "latest.release")
                  implementation(group: "org.openrewrite", name: "rewrite-java", version: "latest.release") { transitive = false }
                  implementation(group: "org.openrewrite", name: "rewrite-java", version: "latest.release") {
                      transitive = false
                  }
                  implementation( group: "org.openrewrite", name: "rewrite-java", version: "latest.release" )
                  implementation( group: "org.openrewrite", name: "rewrite-java", version: "latest.release" ) { transitive = false }
                  implementation( group: "org.openrewrite", name: "rewrite-java", version: "latest.release" ) {
                      transitive = false
                  }

                  // Map literal notation
                  implementation([group: "org.openrewrite", name: "rewrite-java", version: "latest.release"])
                  implementation([group: "org.openrewrite", name: "rewrite-java", version: "latest.release"]) { transitive = false }
                  implementation([group: "org.openrewrite", name: "rewrite-java", version: "latest.release"]) {
                      transitive = false
                  }
                  implementation( [group: "org.openrewrite", name: "rewrite-java", version: "latest.release"] )
                  implementation( [group: "org.openrewrite", name: "rewrite-java", version: "latest.release"] ) { transitive = false }
                  implementation( [group: "org.openrewrite", name: "rewrite-java", version: "latest.release"] ) {
                      transitive = false
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void kotlinDslWithUnresolvedCallableReference() {
        rewriteRun(
          buildGradleKts(
            """
              kotlin { jvmToolchain { languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of)) } }
              """
          )
        );
    }

    @Test
    void escapedAndNonEscapedDollarSignsInSingleDoubleQuotes() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString("""
          plugins {
            id 'java-library'
          }

          task executeShellCommands {
              doLast {
                  exec {
                      commandLine 'bash', '-c', "RESPONSE=\\$(curl --location -s --request POST \\"https://localhost/$path\\")"
                  }
              }
          }
          """)), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void escapedAndNonEscapedDollarSignsInSingleSingleQuotes() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString("""
          plugins {
            id 'java-library'
          }

          task executeShellCommands {
              doLast {
                  exec {
                      commandLine 'bash', '-c', 'RESPONSE=\\$(curl --location -s --request POST "https://localhost/$path")'
                  }
              }
          }
          """)), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void escapedAndNonEscapedDollarSignsInTripleDoubleQuotes() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString("""
          plugins {
            id 'java-library'
          }

          task executeShellCommands {
              doLast {
                  exec {
                      commandLine 'bash', '-c', \"""
                          RESPONSE=\\$(curl --location -s --request POST "https://localhost")
                          echo "TEST" > "\\$(echo $someVar)"
                      \"""
                  }
              }
          }
          """)), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void escapedAndNonEscapedDollarSignsInTripleSingleQuotes() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString("""
          plugins {
            id 'java-library'
          }

          task executeShellCommands {
              doLast {
                  exec {
                      commandLine 'bash', '-c', '''
                          RESPONSE=\\$(curl --location -s --request POST "https://localhost")
                          echo "TEST" > "\\$(echo $someVar)"
                      '''
                  }
              }
          }
          """)), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4614")
    @Test
    void trailingComma() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              dependencies {
                  implementation platform("commons-lang:commons-lang:2.6", )
                  implementation platform("commons-lang:commons-lang3:3.0",)
              }
              """
          )
        );
    }

    @Test
    void trailingCommaWithClosures() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
            foo('bar',) {}
            foo('bar' , {} , )
            foo('bar' , {} , ) {}
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void noTrailingCommaWithClosures() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
            foo('bar') {}
            foo('bar' , {} )
            foo('bar' , {} ) {}
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void trailingCommaWithNamedParameters() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
          // lang=groovy
          """
              foo(a: "Hello World with no extra space",)
              foo(a: "Hello World with space before comma" ,)
              foo(a: "Hello World with space after comma", )
              foo(a: "Hello World with space before & after comma" , )
              foo(a: "Hello World with space before and new line after comma" ,
              )
          """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void noTrailingCommaWithNamedParameters() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
          // lang=groovy
          """
              foo(a: "Hello World with no extra space")
              foo(a: "Hello World with space after parameter" )
              foo(a: "Hello World with new line after parameter"
              )
          """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void trailingCommaWithNamedParametersAndClosures() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
              foo(a: 'bar',) {}
              foo(a: 'bar', {} , )
              foo(a: 'bar', b: {} , )
              foo('bar', b: {} , )
              foo(a: 'bar', {} , ) {}
              foo(a: 'bar', {} , {} , )
              foo(a: 'bar', b: {} , ) {}
              foo(a: 'bar', b: {} , {} , )
              foo('bar', b: {} , ) {}
              foo('bar', b: {} , {} , )
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void noTrailingCommaWithNamedParametersAndClosures() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
              foo(a: 'bar') {}
              foo(a: 'bar', {})
              foo(a: 'bar', b: {})
              foo('bar', b: {})
              foo(a: 'bar', {} )
              foo(a: 'bar', b: {} )
              foo('bar', b: {} )
              foo(a: 'bar', {} ) {}
              foo(a: 'bar', {} , {} )
              foo(a: 'bar', b: {} ) {}
              foo(a: 'bar', b: {} , {} )
              foo('bar', b: {} ) {}
              foo('bar', b: {} , {} )
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void trailingCommaWithNamedParametersComplicated() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
            task foo {
              doLast {
                bar.baz(
                  a: 'fizbuz',
                  b: 'buzfiz',
                ) {
                  c('d')
                }
              }
            }
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Test
    void noTrailingCommaWithNamedParametersComplicated() {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(Parser.Input.fromString(
            // lang=groovy
            """
            task foo {
              doLast {
                bar.baz(
                  a: 'fizbuz',
                  b: 'buzfiz'
                ) {
                  c('d')
                }
              }
            }
            """
        )), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/8301")
    @Test
    void cloneIsolatesGroovyAndKotlinTypeCaches() throws Exception {
        GradleParser.Builder shared = GradleParser.builder()
          .groovyParser(GroovyParser.builder())
          .kotlinParser(KotlinParser.builder());

        GradleParser.Builder cloneA = (GradleParser.Builder) shared.clone();
        GradleParser.Builder cloneB = (GradleParser.Builder) shared.clone();

        assertThat(cloneA.groovyParser)
          .isNotSameAs(shared.groovyParser)
          .isNotSameAs(cloneB.groovyParser);
        assertThat(cloneA.kotlinParser)
          .isNotSameAs(shared.kotlinParser)
          .isNotSameAs(cloneB.kotlinParser);

        assertThat(typeCacheOf(cloneA.groovyParser))
          .isNotSameAs(typeCacheOf(shared.groovyParser))
          .isNotSameAs(typeCacheOf(cloneB.groovyParser));
        assertThat(typeCacheOf(cloneA.kotlinParser))
          .isNotSameAs(typeCacheOf(shared.kotlinParser))
          .isNotSameAs(typeCacheOf(cloneB.kotlinParser));
    }

    private static JavaTypeCache typeCacheOf(Object parserBuilder) throws Exception {
        Field field = parserBuilder.getClass().getDeclaredField("typeCache");
        field.setAccessible(true);
        return (JavaTypeCache) field.get(parserBuilder);
    }

    @MethodSource("escapedBackslashesAndInterpolationInGStringParams")
    @ParameterizedTest
    void escapedBackslashesAndInterpolationInGString(@Language("groovy") String groovy) {
        var gradleParser = new GradleParser(new GradleParser.Builder());
        Stream<SourceFile> sourceFileStream = gradleParser.parseInputs(List.of(
          Parser.Input.fromString(groovy)
        ), null, new InMemoryExecutionContext());
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }

    /**
     * Produces a stream of test expressions like `def a = "\\${System.getProperty('user.name')}"`
     */
    static Stream<String> escapedBackslashesAndInterpolationInGStringParams() {
        return Stream.of(
            "1 + 1",
            "System.getProperty('user.name')"
        ).flatMap(exp ->
            """
            %s
            "%s"
            "${%s}"
            "\\${%s}"
            "\\\\${%s}"
            "\\\\\\${%s}"
            "${%s}\\\\"
            "\\t${%s}"
            "${%s}\\t"
            """.lines().map(s -> ("def a = " + s).formatted(exp))
        );
    }
}

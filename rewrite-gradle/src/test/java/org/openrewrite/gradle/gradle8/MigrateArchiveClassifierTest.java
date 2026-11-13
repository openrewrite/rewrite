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
package org.openrewrite.gradle.gradle8;

import org.junit.jupiter.api.Test;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class MigrateArchiveClassifierTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateArchiveClassifier());
    }

    @Test
    void interlokSixSites() {
        rewriteRun(buildGradle(
          """
            task javadocJar(type: Jar, dependsOn: javadoc) {
                classifier = 'javadoc'
                from javadoc.destinationDir
            }
            task sourcesJar(type: Jar, dependsOn: classes) {
                classifier = 'sources'
                from sourceSets.main.allSource
            }
            task examplesJar(type: Jar) {
                classifier = 'examples'
                from 'examples'
            }
            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        artifact javadocJar { classifier "javadoc" }
                        artifact sourcesJar { classifier "sources" }
                        artifact examplesJar { classifier "examples" }
                    }
                }
            }
            """,
          """
            task javadocJar(type: Jar, dependsOn: javadoc) {
                archiveClassifier = 'javadoc'
                from javadoc.destinationDir
            }
            task sourcesJar(type: Jar, dependsOn: classes) {
                archiveClassifier = 'sources'
                from sourceSets.main.allSource
            }
            task examplesJar(type: Jar) {
                archiveClassifier = 'examples'
                from 'examples'
            }
            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        artifact javadocJar { archiveClassifier = "javadoc" }
                        artifact sourcesJar { archiveClassifier = "sources" }
                        artifact examplesJar { archiveClassifier = "examples" }
                    }
                }
            }
            """
        ));
    }

    @Test
    void preserveMavenArtifactAndUnrelatedProperties() {
        rewriteRun(buildGradle(
          """
            task javadocJar(type: Jar)
            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        artifact(javadocJar) { classifier = 'javadoc' }
                        artifact(javadocJar) { classifier 'javadoc' }
                        artifact('other.jar') { classifier = 'other' }
                    }
                }
            }
            custom { classifier = 'custom'; classifier('custom'); setClassifier('custom') }
            unknownTask { classifier = 'unknown' }
            classifier = 'project'
            """
        ));
    }

    @Test
    void preserveCustomJarImport() {
        rewriteRun(buildGradle(
          """
            import example.Jar
            task customJar(type: Jar) { classifier = 'custom' }
            customJar { classifier 'custom' }
            """
        ));
    }

    @Test
    void setterFormsAndKnownTaskConfiguration() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar)
            docs {
                classifier 'one'
                classifier('two')
                setClassifier 'three'
                setClassifier('four')
                other.classifier = 'other'
                other.setClassifier('other')
                manifest { classifier = 'nested' }
            }
            """,
          """
            task docs(type: Jar)
            docs {
                archiveClassifier = 'one'
                archiveClassifier = 'two'
                archiveClassifier = 'three'
                archiveClassifier = 'four'
                other.classifier = 'other'
                other.setClassifier('other')
                manifest { classifier = 'nested' }
            }
            """
        ));
    }

    @Test
    void typedTaskContainerCalls() {
        rewriteRun(buildGradle(
          """
            tasks.register('docs', Jar) { classifier = 'docs' }
            tasks.create('sources', org.gradle.api.tasks.bundling.Jar) { setClassifier('sources') }
            tasks.withType(org.gradle.api.tasks.bundling.AbstractArchiveTask) { classifier('all') }
            tasks.docs { classifier = 'later' }
            """,
          """
            tasks.register('docs', Jar) { archiveClassifier = 'docs' }
            tasks.create('sources', org.gradle.api.tasks.bundling.Jar) { archiveClassifier = 'sources' }
            tasks.withType(org.gradle.api.tasks.bundling.AbstractArchiveTask) { archiveClassifier = 'all' }
            tasks.docs { archiveClassifier = 'later' }
            """
        ));
    }

    @Test
    void explicitGradleImport() {
        rewriteRun(buildGradle(
          """
            import org.gradle.api.tasks.bundling.Jar
            task docs(type: Jar) { classifier = 'docs' }
            """,
          """
            import org.gradle.api.tasks.bundling.Jar
            task docs(type: Jar) { archiveClassifier = 'docs' }
            """
        ));
    }

    @Test
    void fullyQualifiedGradleTypeWithCustomImport() {
        rewriteRun(buildGradle(
          """
            import example.Jar
            task docs(type: org.gradle.api.tasks.bundling.Jar) { classifier = 'docs' }
            task custom(type: Jar) { classifier = 'custom' }
            """,
          """
            import example.Jar
            task docs(type: org.gradle.api.tasks.bundling.Jar) { archiveClassifier = 'docs' }
            task custom(type: Jar) { classifier = 'custom' }
            """
        ));
    }

    @Test
    void preserveCustomJarClassAndQualifiedType() {
        rewriteRun(buildGradle(
          """
            class Jar { String classifier }
            task docs(type: Jar) { classifier = 'custom' }
            task other(type: example.Jar) { setClassifier('custom') }
            """
        ));
    }

    @Test
    void preserveWildcardImportAmbiguity() {
        rewriteRun(buildGradle(
          """
            import example.*
            task docs(type: Jar) { classifier = 'custom' }
            """,
          spec -> spec.mapBeforeRecipe(MigrateArchiveClassifierTest::withoutArchiveTypeAttribution)
        ));
    }

    @Test
    void preserveLocalClassifierAndExplicitParameter() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar) {
                def classifier = 'local'
                classifier = 'still local'
            }
            task other(type: Jar) { custom ->
                classifier = 'ambiguous'
            }
            """
        ));
    }

    @Test
    void preserveUnprovenAndNestedDelegates() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar)
            custom {
                docs { classifier = 'custom delegate' }
                task other(type: Jar) { classifier = 'unproven declaration' }
            }
            task plain { classifier = 'not an archive' }
            tasks.register('plain2', CustomTask) { classifier = 'custom task' }
            tasks.named('unknown') { classifier = 'unproven' }
            tasks.withType(CustomTask) { classifier = 'custom type' }
            """
        ));
    }

    @Test
    void preserveModernArchiveClassifier() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar) {
                archiveClassifier = 'docs'
                archiveClassifier.set('docs')
            }
            """
        ));
    }

    @Test
    void preserveArtifactDelegateWithinArchiveTask() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar) {
                publishing {
                    publications {
                        mavenJava(MavenPublication) {
                            artifact(docs) { classifier = 'artifact' }
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void knownNamedTask() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar)
            tasks.named('docs') { classifier = 'docs' }
            """,
          """
            task docs(type: Jar)
            tasks.named('docs') { archiveClassifier = 'docs' }
            """
        ));
    }

    @Test
    void preserveLexicalClassifier() {
        rewriteRun(buildGradle(
          """
            def classifier = 'script variable'
            task docs(type: Jar) { classifier = 'still script variable' }
            """
        ));
    }

    @Test
    void preserveCustomSetterAndExpressionCalls() {
        rewriteRun(buildGradle(
          """
            def setClassifier(String value) { value }
            task docs(type: Jar) {
                setClassifier('custom method')
                println(classifier('expression'))
                return classifier('explicit return')
            }
            """
        ));
    }

    @Test
    void preserveAliasedCustomJarAndTypeVariable() {
        rewriteRun(buildGradle(
          """
            import example.CustomArchive as Jar
            task docs(type: Jar) { classifier = 'custom' }
            """
          ),
          buildGradle(
            """
              def Jar = customTaskType
              task docs(type: Jar) { classifier = 'dynamic type' }
              """,
            spec -> spec.path("other.gradle")
          )
        );
    }

    @Test
    void preserveCustomTaskContainer() {
        rewriteRun(buildGradle(
          """
            def tasks = customTasks
            tasks.register('docs', Jar) { classifier = 'custom' }
            docs { classifier = 'custom' }
            """
        ));
    }

    @Test
    void preserveSetterCommentsAndSemicolons() {
        rewriteRun(buildGradle(
          """
            task docs(type: Jar) {
                // archive classifier
                classifier(/* keep */ 'docs'); from 'docs'
            }
            """,
          """
            task docs(type: Jar) {
                // archive classifier
                archiveClassifier = /* keep */ 'docs'; from 'docs'
            }
            """
        ));
    }
    @Test
    void composedInGradle8Migration() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.gradle.MigrateToGradle8"),
          buildGradle(
            """
              plugins { id 'java' }
              task sourcesJar(type: Jar) {
                  classifier = 'sources'
              }
              """,
            """
              plugins { id 'java' }
              task sourcesJar(type: Jar) {
                  archiveClassifier = 'sources'
              }
              """
          )
        );
    }

    @Test
    void allowGradleProvidedAntFilterWildcardImport() {
        rewriteRun(buildGradle(
          """
            import org.apache.tools.ant.filters.*
            task docs(type: Jar) {
                classifier = 'docs'
            }
            """,
          """
            import org.apache.tools.ant.filters.*
            task docs(type: Jar) {
                archiveClassifier = 'docs'
            }
            """,
          spec -> spec.mapBeforeRecipe(MigrateArchiveClassifierTest::withoutArchiveTypeAttribution)
        ));
    }

    private static G.CompilationUnit withoutArchiveTypeAttribution(G.CompilationUnit cu) {
        // Model older LSTs explicitly: current parsers can resolve Gradle's Jar despite an unrelated wildcard import.
        return (G.CompilationUnit) new GroovyIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                J.Identifier id = super.visitIdentifier(identifier, p);
                return "Jar".equals(id.getSimpleName()) ? id.withType(null).withFieldType(null) : id;
            }
        }.visitNonNull(cu, 0);
    }

}

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
package org.openrewrite.gradle.gradle9;

import org.junit.jupiter.api.Test;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseProjectForSourceSetsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseProjectForSourceSets());
    }

    @Test
    void neptusShape() {
        rewriteRun(buildGradle(
            """
            subprojects {
                dependencies {
                    compileOnly project(':core')
                    compileOnly project(':core').sourceSets.main.runtimeClasspath
                }
            }
            subprojects.each { project ->
                println project.name
            }
            """,
            """
            subprojects {
                dependencies {
                    compileOnly project(':core')
                    compileOnly project(path: ':core', configuration: 'runtimeElements')
                    compileOnly rootProject.project(':core').sourceSets.main.output
                }
            }
            subprojects.each { project ->
                println project.name
            }
            """));
    }

    @Test
    void parenthesesAndComments() {
        rewriteRun(buildGradle(
            """
            dependencies {
                compileOnly(project( /* keep */ ':core').sourceSets.main.runtimeClasspath)
                implementation project(':core').sourceSets.test.output
            }
            """,
            """
            dependencies {
                compileOnly(project(path: ':core', configuration: 'runtimeElements'))
                compileOnly(rootProject.project( /* keep */ ':core').sourceSets.main.output)
                implementation rootProject.project(':core').sourceSets.test.output
            }
            """));
    }

    @Test
    void outsideDependencies() {
        rewriteRun(buildGradle(
            """
            println project(':core').sourceSets
            subprojects { println project(':core').sourceSets }
            """));
    }

    @Test
    void ordinaryAndOtherProperties() {
        rewriteRun(buildGradle(
            """
            dependencies {
                compileOnly project(':core')
                println project(':core').name
                println project(':core').version
                println rootProject.project(':core').sourceSets
                println other.project(':core').sourceSets
                println project('core').sourceSets
                println project(path: ':core').sourceSets
                println project(":${name}").sourceSets
            }
            """));
    }

    @Test
    void customDsl() {
        rewriteRun(buildGradle(
            """
            def dependencies(Closure c) { c() }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void projectMethodShadow() {
        rewriteRun(buildGradle(
            """
            def project(String path) { [sourceSets: [:]] }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void variableShadow() {
        rewriteRun(buildGradle(
            """
            def project = { String path -> [sourceSets: [:]] }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void closureParameterShadow() {
        rewriteRun(buildGradle(
            """
            subprojects { project ->
                dependencies { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void rootProjectShadow() {
        rewriteRun(buildGradle(
            """
            def rootProject = new Object()
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void unrelatedClosure() {
        rewriteRun(buildGradle(
            """
            custom {
                dependencies { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void explicitCustomDependencies() {
        rewriteRun(buildGradle(
            """
            other.dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void nestedCustomDsl() {
        rewriteRun(buildGradle(
            """
            dependencies {
                custom { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void siblingVariableScope() {
        rewriteRun(buildGradle(
            """
            def unrelated() {
                def project = { path -> path }
                project(':other')
            }
            allprojects {
                dependencies { compileOnly project(':core').sourceSets.main.runtimeClasspath }
            }
            """,
            """
            def unrelated() {
                def project = { path -> path }
                project(':other')
            }
            allprojects {
                dependencies { compileOnly project(path: ':core', configuration: 'runtimeElements')
                    compileOnly rootProject.project(':core').sourceSets.main.output }
            }
            """));
    }

    @Test
    void kotlinUnchanged() {
        rewriteRun(buildGradleKts("""
            dependencies { compileOnly(project(":core").sourceSets.main.runtimeClasspath) }
            """));
    }
    @Test
    void customTypedMethods() {
        rewriteRun(buildGradle("""
            class Custom {
                Object project(String path) { [sourceSets: [:]] }
                void dependencies(Closure c) { c() }
            }
            Custom custom = new Custom()
            custom.dependencies { println custom.project(':core').sourceSets }
            """));
    }

    @Test
    void extraPropertyShadow() {
        rewriteRun(buildGradle("""
            ext.project = { path -> [sourceSets: [:]] }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void extraPropertiesBlockShadow() {
        rewriteRun(buildGradle("""
            ext { project = { path -> [sourceSets: [:]] } }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void dependenciesClosureVariableShadow() {
        rewriteRun(buildGradle("""
            def dependencies = { Closure c -> c() }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void wrapperMethodShadow() {
        rewriteRun(buildGradle("""
            def subprojects(Closure c) { c() }
            subprojects { dependencies { println project(':core').sourceSets } }
            """));
    }

    @Test
    void localRootProjectShadow() {
        rewriteRun(buildGradle("""
            subprojects {
                def rootProject = new Object()
                dependencies { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void neptusFilteredSubprojects() {
        rewriteRun(buildGradle(
            """
            configure(subprojects.findAll { it.name != 'core' }) {
                dependencies {
                    compileOnly project(':core')
                    compileOnly project(':core').sourceSets.main.runtimeClasspath
                }
            }
            subprojects.each { project -> println project.name }
            """,
            """
            configure(subprojects.findAll { it.name != 'core' }) {
                dependencies {
                    compileOnly project(':core')
                    compileOnly project(path: ':core', configuration: 'runtimeElements')
                    compileOnly rootProject.project(':core').sourceSets.main.output
                }
            }
            subprojects.each { project -> println project.name }
            """));
    }

    @Test
    void configureCustomCollection() {
        rewriteRun(buildGradle("""
            configure(custom.findAll { it.enabled }) {
                dependencies { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void configureMethodShadow() {
        rewriteRun(buildGradle("""
            def configure(Object objects, Closure c) { c() }
            configure(subprojects.findAll { it.name != 'core' }) {
                dependencies { println project(':core').sourceSets }
            }
            """));
    }

    @Test
    void sourceSetsWritesUnchanged() {
        rewriteRun(buildGradle("""
            dependencies {
                project(':core').sourceSets = [:]
                project(':core').sourceSets += [:]
            }
            """));
    }

    @Test
    void getterShadow() {
        rewriteRun(buildGradle("""
            def getRootProject() { new Object() }
            dependencies { println project(':core').sourceSets }
            """));
    }

    @Test
    void twoProjectFixture() throws IOException {
        try (var input = getClass().getResourceAsStream("/org/openrewrite/gradle/gradle9/project-source-sets/build.gradle")) {
            assert input != null;
            String before = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            rewriteRun(buildGradle(before, before.replace("compileOnly project(':core').sourceSets.main.runtimeClasspath",
                    "compileOnly project(path: ':core', configuration: 'runtimeElements')\n" +
                    "        compileOnly rootProject.project(':core').sourceSets.main.output")));
        }
    }

    @Test
    void sparseAttribution() {
        rewriteRun(
            buildGradle(
                """
                dependencies { compileOnly project(':core').sourceSets.main.runtimeClasspath }
                """,
                """
                dependencies { compileOnly project(path: ':core', configuration: 'runtimeElements')
                    compileOnly rootProject.project(':core').sourceSets.main.output }
                """,
                spec -> spec.beforeRecipe(cu -> new GroovyIsoVisitor<Void>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(
                            J.MethodInvocation method, Void unused) {
                        return super.visitMethodInvocation(method, unused).withMethodType(null);
                    }
                }.visitNonNull(cu, null))));
    }

    @Test
    void onlyDirectCompileOnlyMainRuntimeClasspathUsesVariant() {
        rewriteRun(buildGradle(
            """
            dependencies {
                compileOnly project(':core').sourceSets.main.runtimeClasspath
                compileOnly project(':core').sourceSets.test.runtimeClasspath
                compileOnly project(':core').sourceSets.main.compileClasspath
                println project(':core').sourceSets.main.runtimeClasspath
                implementation project(':core').sourceSets.main.runtimeClasspath
            }
            """,
            """
            dependencies {
                compileOnly project(path: ':core', configuration: 'runtimeElements')
                compileOnly rootProject.project(':core').sourceSets.main.output
                compileOnly project(':core').sourceSets.test.runtimeClasspath
                compileOnly project(':core').sourceSets.main.compileClasspath
                println project(':core').sourceSets.main.runtimeClasspath
                implementation project(':core').sourceSets.main.runtimeClasspath
            }
            """));
    }

    @Test
    void doesNotTreatCompositeFileCollectionAsProjectDependency() {
        rewriteRun(buildGradle(
            """
            dependencies {
                compileOnly files(project(':core').sourceSets.main.runtimeClasspath)
                compileOnly project(':core').sourceSets.main.runtimeClasspath.files
            }
            """));
    }

    @Test
    void explicitAndDeclarativeViewsUnchanged() {
        rewriteRun(buildGradle("""
            dependencies {
                compileOnly rootProject.project(':core').sourceSets.main.runtimeClasspath
                compileOnly project(path: ':core', configuration: 'runtimeElements')
                compileOnly other.project(':core').sourceSets.main.runtimeClasspath
            }
            """));
    }

    @Test
    void compileOnlyShadowNotReinterpretedAsDependency() {
        rewriteRun(buildGradle(
            """
            def compileOnly(Object value) { println value }
            dependencies { compileOnly project(':core').sourceSets.main.runtimeClasspath }
            """));
    }

    @Test
    void splitKeepsCommentsOnceAndTreeIdsUnique() {
        rewriteRun(buildGradle(
            """
            dependencies {
                // keep declaration
                compileOnly /* notation */ project( /* path */ ":core") /* project */ .sourceSets /* sets */ .main /* main */ .runtimeClasspath // trailing
            }
            """,
            """
            dependencies {
                // keep declaration
                compileOnly project(path: ":core", configuration: 'runtimeElements')
                compileOnly /* notation */ rootProject.project( /* path */ ":core") /* project */ .sourceSets /* sets */ .main /* main */ .output // trailing
            }
            """,
            spec -> spec.afterRecipe(cu -> new GroovyIsoVisitor<java.util.Set<java.util.UUID>>() {
                @Override
                public J preVisit(J tree, java.util.Set<java.util.UUID> ids) {
                    org.junit.jupiter.api.Assertions.assertTrue(ids.add(tree.getId()), "duplicate tree id: " + tree.getId());
                    return tree;
                }
            }.visit(cu, new java.util.HashSet<>()))));
    }

    @Test
    void unsupportedResolvingViewsRemainUnchanged() {
        rewriteRun(buildGradle("""
            dependencies {
                compileOnly((project(':core').sourceSets.main).runtimeClasspath)
                compileOnly project(':core').sourceSets.main.getRuntimeClasspath()
            }
            """));
    }

    @Test
    void variantDoesNotOverrideExtraCompileOnlyShadow() {
        rewriteRun(buildGradle("""
            ext.compileOnly = { value -> println value }
            dependencies { compileOnly project(':core').sourceSets.main.runtimeClasspath }
            """));
    }

}

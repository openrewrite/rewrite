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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseManifestAttributesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseManifestAttributes());
    }

    @Test
    void neptus() {
        rewriteRun(buildGradle(
            """
            allprojects {
                jar {
                    manifest {
                        attributes += ['Main-Class':'pt.lsts.neptus.loader.NeptusMain']
                    }
                }
            }
            """,
            """
            allprojects {
                jar {
                    manifest {
                        attributes(['Main-Class':'pt.lsts.neptus.loader.NeptusMain'])
                    }
                }
            }
            """));
    }

    @Test
    void commentsAndStatements() {
        rewriteRun(buildGradle(
            """
            jar {
                manifest {
                    // Keep this entry
                    attributes /* before */ += /* after */ ['Main-Class': 'Main'] // trailing
                    attributes += ['Other': 'value']
                    attributes = ['Replacement': 'value']
                }
            }
            """,
            """
            jar {
                manifest {
                    // Keep this entry
                    attributes( /* before */ /* after */ ['Main-Class': 'Main']) // trailing
                    attributes(['Other': 'value'])
                    attributes = ['Replacement': 'value']
                }
            }
            """));
    }

    @Test
    void explicitManifestReceiver() {
        rewriteRun(buildGradle(
            """
            import org.gradle.api.java.archives.Manifest
            Manifest mf = null
            mf.attributes += ['Main-Class': 'Main']
            """,
            """
            import org.gradle.api.java.archives.Manifest
            Manifest mf = null
            mf.attributes(['Main-Class': 'Main'])
            """));
    }

    @Test
    void multilineMapAndTypedMap() {
        rewriteRun(buildGradle(
            """
            allprojects {
                jar {
                    archiveBaseName = rootProject.name
                    manifest {
                        attributes += [
                            'Main-Class': 'pt.lsts.neptus.loader.NeptusMain',
                            ]
                        Map entries = ['Other': 'value']
                        attributes += entries
                    }
                    archiveVersion = ''
                }
            }
            """,
            """
            allprojects {
                jar {
                    archiveBaseName = rootProject.name
                    manifest {
                        attributes([
                            'Main-Class': 'pt.lsts.neptus.loader.NeptusMain',
                            ])
                        Map entries = ['Other': 'value']
                        attributes(entries)
                    }
                    archiveVersion = ''
                }
            }
            """));
    }

    @Test
    void neptusCoreProject() {
        rewriteRun(buildGradle(
            """
            subprojects.each { project -> println project.name }
            project(':core') {
                jar {
                    archiveBaseName = rootProject.name
                    manifest {
                        attributes += [
                            'Main-Class': 'pt.lsts.neptus.loader.NeptusMain',
                            ]
                    }
                    archiveVersion = ''
                }
                compileJava.mustRunAfter(generateRunScripts)
            }
            """,
            """
            subprojects.each { project -> println project.name }
            project(':core') {
                jar {
                    archiveBaseName = rootProject.name
                    manifest {
                        attributes([
                            'Main-Class': 'pt.lsts.neptus.loader.NeptusMain',
                            ])
                    }
                    archiveVersion = ''
                }
                compileJava.mustRunAfter(generateRunScripts)
            }
            """));
    }

    @Test
    void moreGuards() {
        rewriteRun(buildGradle(
            """
            def project = { path, closure -> closure() }
            project(':core') { jar { manifest { attributes += ['custom': 'value'] } } }
            """), buildGradle(
            """
            def project(String path, Closure c) { c() }
            project(':core') { jar { manifest { attributes += ['custom': 'value'] } } }
            """), buildGradle(
            """
            def jar(Closure c) { c() }
            jar { manifest { attributes += ['custom': 'value'] } }
            """), buildGradle(
            """
            def result = jar { manifest { attributes += ['observed': 'value'] } }
            """), buildGradle(
            """
            jar { manifest { attributes += 'not a map' } }
            """), buildGradle(
            """
            jar { manifest { other.attributes += ['untyped receiver': 'value'] } }
            """), buildGradle(
            """
            def work() {
                jar { manifest { attributes += ['method': 'value'] } }
            }
            """), buildGradle(
            """
            jar { manifest { def attributes = [:]; attributes += ['local': 'value'] } }
            """));
    }

    @Test
    void unrelatedAndObserved() {
        rewriteRun(buildGradle(
            """
            def attributes = [:]
            attributes += ['outside': 'value']
            jar { manifest { attributes += ['shadow': 'value'] } }
            """), buildGradle(
            """
            manifest { attributes += ['custom': 'value'] }
            jar {
                manifest {
                    foreign { attributes += ['nested': 'value'] }
                    def observed = (attributes += ['observed': 'value'])
                    return attributes += ['returned': 'value']
                }
            }
            """), buildGradle(
            """
            def manifest(Closure c) { c() }
            jar { manifest { attributes += ['custom': 'value'] } }
            """), buildGradle(
            """
            jar { manifest { attributes -> attributes += ['parameter': 'value'] } }
            """), buildGradleKts(
            """
            tasks.jar { manifest { attributes += mapOf("Main-Class" to "Main") } }
            """));
    }
}

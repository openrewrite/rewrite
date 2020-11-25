/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Scope

class MavenDependencyDownloadIntegTest {
    @Test
    fun springWebMvc() {
        val maven: Maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse(singleDependencyPom("org.springframework:spring-webmvc:4.3.6.RELEASE"))
                .first()

        val compileDependencies = maven.model.getDependencies(Scope.Compile)

        compileDependencies.forEach { dep ->
            println("${dep.coordinates} -> ${dep.artifactUri}")
        }
    }

    @Test
    fun rewriteCore() {
        val maven: Maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse(singleDependencyPom("org.openrewrite:rewrite-core:6.0.1"))
                .first()

        val compileDependencies = maven.model.getDependencies(Scope.Runtime)
                .sortedBy { d -> d.coordinates }

        compileDependencies.forEach { dep ->
            println("${dep.coordinates} -> ${dep.artifactUri}")
        }
    }
}

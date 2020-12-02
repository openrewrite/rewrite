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
import org.openrewrite.RefactorVisitor
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.maven.cache.InMemoryCache
import org.openrewrite.maven.tree.Maven

class RemoveDependencyTest : RefactorVisitorTestForParser<Maven> {
    companion object {
        private val mavenCache = InMemoryCache()
    }

    override val parser: MavenParser = MavenParser.builder()
            .resolveOptional(false)
            .cache(mavenCache)
            .build()

    override val visitors = listOf(RemoveDependency().apply {
        setGroupId("junit")
        setArtifactId("junit")
    })

    @Test
    fun removeDependency() = assertRefactored(
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>4.13.1</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun noDependencyToRemove() = assertUnchanged(
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )
}

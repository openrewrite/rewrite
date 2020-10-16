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
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.maven.tree.Maven

class ChangeDependencyScopeTest : RefactorVisitorTestForParser<Maven.Pom> {
    override val parser = MavenParser.builder()
            .resolveDependencies(false)
            .build()

    private val guavaToTest = ChangeDependencyScope().apply {
        setGroupId("com.google.guava")
        setArtifactId("guava")
        setToScope("test")
    }

    @Test
    fun noScopeToScope() = assertRefactored(
            visitors = listOf(guavaToTest),
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
                      <version>28.2-jre</version>
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
                      <version>28.2-jre</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun scopeToScope() = assertRefactored(
            visitors = listOf(guavaToTest),
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
                      <version>28.2-jre</version>
                      <scope>compile</scope>
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
                      <version>28.2-jre</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun scopeToNoScope() = assertRefactored(
            visitors = listOf(
                    guavaToTest.apply { setToScope(null) }
            ),
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
                      <version>28.2-jre</version>
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
                      <version>28.2-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )
}

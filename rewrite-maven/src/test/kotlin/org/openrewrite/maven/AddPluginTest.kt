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

class AddPluginTest : RefactorVisitorTestForParser<Maven> {
    override val parser: MavenParser = MavenParser.builder()
            .resolveOptional(false)
            .build()

    private val addPlugin = AddPlugin().apply {
        setGroupId("org.openrewrite.maven")
        setArtifactId("rewrite-maven-plugin")
        setVersion("100.0")
    }

    @Test
    fun addPlugin() = assertRefactored(
            visitors = listOf(addPlugin),
            before = """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """,
            after = """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>100.0</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """
    )

    @Test
    fun updatePluginVersion() = assertRefactored(
            visitors = listOf(addPlugin),
            before = """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>99.0</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """,
            after = """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>100.0</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """
    )
}

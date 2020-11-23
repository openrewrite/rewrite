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
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import java.net.URI

class RawMavenTest {
    @Test
    fun emptyContainers() {
        val maven = RawMaven.parse(Parser.Input(URI.create("pom.xml")) {
            """
                <project>
                    <dependencyManagement>
                        <!--  none, for now  -->
                    </dependencyManagement>
                    <dependencies>
                        <!--  none, for now  -->
                    </dependencies>
                    <repositories>
                        <!--  none, for now  -->
                    </repositories>
                    <licenses>
                        <!--  none, for now  -->
                    </licenses>
                    <profiles>
                        <!--  none, for now  -->
                    </profiles>
                </project>
            """.trimIndent().byteInputStream()
        }, null)

        assertThat(maven.pom.dependencyManagement?.dependencies?.dependencies).isEmpty()
        assertThat(maven.pom.repositories).isEmpty()
        assertThat(maven.pom.licenses).isEmpty()
        assertThat(maven.pom.profiles).isEmpty()
    }
}

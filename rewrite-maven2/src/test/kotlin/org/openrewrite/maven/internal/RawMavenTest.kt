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

        assertThat(maven.pom.dependencyManagement?.dependencies).isEmpty()
        assertThat(maven.pom.repositories).isEmpty()
        assertThat(maven.pom.licenses).isEmpty()
        assertThat(maven.pom.profiles).isEmpty()
    }
}

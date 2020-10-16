package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.maven.tree.Pom

class MavenParserTest {
    @Test
    fun parse() {
        val pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <developers>
                    <developer>
                        <name>Trygve Laugst&oslash;l</name>
                    </developer>
                </developers>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        val parser = MavenParser.builder().build()

        val maven = parser.parse(pom)[0]

        assertThat(maven.model.dependencies.first().maven.model.licenses.first()?.type)
                .isEqualTo(Pom.LicenseType.Eclipse2)
    }

    @Test
    fun milestoneParent() {
        val pom = MavenParser.builder().build()
                .parse("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0-M3</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                  
                  <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent()).first()

        assertThat(pom.model.parent?.model?.licenses).isNotNull()
    }

    @Test
    fun emptyArtifactPolicy() {
        // example from https://repo1.maven.org/maven2/org/openid4java/openid4java-parent/0.9.6/openid4java-parent-0.9.6.pom
        MavenParser.builder().build().parse("""
            <project>
                <repositories>
                    <repository>
                        <id>alchim.snapshots</id>
                        <name>Achim Repository Snapshots</name>
                        <url>http://alchim.sf.net/download/snapshots</url>
                        <snapshots/>
                    </repository>
                </repositories>
            </project>
        """.trimIndent())
    }
}

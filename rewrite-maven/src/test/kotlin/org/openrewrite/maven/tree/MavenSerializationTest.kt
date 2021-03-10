package org.openrewrite.maven.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.TreeSerializer
import org.openrewrite.maven.MavenParser

class MavenSerializationTest {

    @Test
    fun roundTripSerialization() {

        val source = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
        
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <type>pom</type>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
            </project>
        """

        val treeSerializer = TreeSerializer<Maven>()

        val m = MavenParser.builder().build().parse(source)[0]
        val serialized = treeSerializer.write(m)
        val deser = treeSerializer.read(serialized)
        assertEquals(m, deser)
    }
}
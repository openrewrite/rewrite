package org.openrewrite.xml.format

import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlRecipeTest
import org.openrewrite.xml.style.TabsAndIndentsStyle

class TabsAndIndentsTest : XmlRecipeTest {

    @Test
    fun indentsAndContinuationIndents() = assertChanged(
        recipe = toRecipe {
            TabsAndIndentsVisitor(
                TabsAndIndentsStyle.DEFAULT
                    .withIndentSize(2).withContinuationIndentSize(5)
            )
        },
        before = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <artifactId>quarkus-bootstrap-bom</artifactId>
            </project>
        """,
        after = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <artifactId>quarkus-bootstrap-bom</artifactId>
            </project>
        """
    )
}

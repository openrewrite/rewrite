package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.XmlParser;

import static org.openrewrite.xml.Assertions.xml;

class RemoveEmptyXmlTagsRecipeTest implements RewriteTest {

    @Test
    void removeEmptyXmlTags() {
        // Test input XML with empty <pluginRepositories> tag.
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTagsRecipe()),
          xml( """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>sample-app</artifactId>
                <version>1.0-SNAPSHOT</version>
                <pluginRepositories>
                </pluginRepositories>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.12</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>sample-app</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.12</version>
                    </dependency>
                </dependencies>
            </project>
            """)
        );
    }
}
package org.openrewrite.xml.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeTest;
import org.openrewrite.xml.XmlParser;

import static org.openrewrite.xml.Assertions.xml;

class RemoveEmptyXmlTagsRecipeTest extends RecipeTest {

    @Test
    void removeEmptyXmlTags() {
        // Test input XML with empty <pluginRepositories> tag.
        String inputXml = """
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
            """;

        // Expected output XML with the <pluginRepositories> tag removed.
        String expectedXml = """
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
            """;

        // Create the RemoveEmptyXmlTagsRecipe instance.
        Recipe recipe = new RemoveEmptyXmlTagsRecipe();

        // Apply the recipe to the input XML and assert the output.
        assertChanged(
          recipe,
          inputXml,
          expectedXml,
          "Remove empty XML tags without child elements"
        );
    }
}
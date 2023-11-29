/*
 * Copyright 2023 the original author or authors.
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
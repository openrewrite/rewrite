/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Recipe

class AddPropertyTest: MavenRecipeTest {
    override val recipe: Recipe
        get() = toRecipe { AddProperty("key", "value") }

    @Test
    fun addFirstProperty() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <dependencies>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <key>value</key>
              </properties>
            
              <dependencies>
              </dependencies>
            </project>
        """
    )

    @Test
    fun changeExistingProperty() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <key>v</key>
              </properties>
            
              <dependencies>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <key>value</key>
              </properties>
            
              <dependencies>
              </dependencies>
            </project>
        """
    )

    @Test
    fun addPropertyInOrder() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <abc>value</abc>
                <other>value</other>
              </properties>
            
              <dependencies>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <abc>value</abc>
                <key>value</key>
                <other>value</other>
              </properties>
            
              <dependencies>
              </dependencies>
            </project>
        """
    )
}

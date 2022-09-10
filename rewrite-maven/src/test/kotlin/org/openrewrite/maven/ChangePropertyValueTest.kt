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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChangePropertyValueTest : MavenRecipeTest {
    @Test
    fun property() = assertChanged(
        recipe = ChangePropertyValue("guava.version", "29.0-jre", false, false),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <guava.version>28.2-jre</guava.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <guava.version>29.0-jre</guava.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )
    @Test
    fun addFirstProperty() = assertChanged(
        recipe = ChangePropertyValue("key", "value", true, false),
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
        recipe = ChangePropertyValue("key", "value", true, false),
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
        recipe = ChangePropertyValue("key", "value", true, false),
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

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyValue(null, null, false, false)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("key")
        assertThat(valid.failures()[1].property).isEqualTo("newValue")

        recipe = ChangePropertyValue(null, "8", false, false)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("key")

        recipe = ChangePropertyValue("java.version", null, false, false)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newValue")

        recipe = ChangePropertyValue("java.version", "8", false, false)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}

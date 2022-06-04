/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author Alex Boyko
 */
class ChangeDependencyClassifierTest : MavenRecipeTest {
    override val recipe: ChangeDependencyClassifier
        get() = ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta")

    @Test
    fun noClassifierToClassifier() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
            
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
            
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                  <classifier>jakarta</classifier>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun classifierToClassifier() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                  <classifier>javax</classifier>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                  <classifier>jakarta</classifier>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun classifierToNoClassifier() = assertChanged(
        recipe = ChangeDependencyClassifier("org.ehcache", "ehcache", null as String?),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                  <classifier>jakarta</classifier>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                  <version>3.10.0</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeDependencyClassifier(null, null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(2)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("groupId")

        recipe = ChangeDependencyClassifier(null, "rewrite-maven", "test")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = ChangeDependencyClassifier("org.openrewrite", null, "test")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")

        recipe = ChangeDependencyClassifier("org.openrewrite", "rewrite-maven", "test")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue()
    }

}

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
import org.openrewrite.Issue

class UpgradeParentVersionTest : MavenRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1317")
    @Test
    fun doesNotDowngradeVersion() = assertUnchanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.4.12</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )

    @Test
    fun upgradeVersion() = assertChanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.5.12.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.5.22.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )

    @Test
    fun upgradeToExactVersion() = assertChanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "1.5.22.RELEASE",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.5.12.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.5.22.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UpgradeParentVersion(null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[2].property).isEqualTo("newVersion")

        recipe = UpgradeParentVersion(null, "rewrite-maven", "latest.release", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = UpgradeParentVersion("org.openrewrite", null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("newVersion")

        recipe = UpgradeParentVersion("org.openrewrite", "rewrite-maven", "latest.release", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()

        recipe = UpgradeParentVersion("org.openrewrite", "rewrite-maven", "latest.release", "123")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()

        recipe = UpgradeParentVersion("org.springframework.boot", "spring-boot-starter-parent", "1.5.22.RELEASE", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}

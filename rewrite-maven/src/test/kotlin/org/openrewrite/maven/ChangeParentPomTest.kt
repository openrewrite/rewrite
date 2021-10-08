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

class ChangeParentPomTest : MavenRecipeTest {

    @Test
    fun changeParent() = assertChanged(
        recipe = ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.5.12.RELEASE</version>
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
                <groupId>com.fasterxml.jackson</groupId>
                <artifactId>jackson-parent</artifactId>
                <version>2.12</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )

    @Test
    fun upgradeVersion() = assertChanged(
        recipe = ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
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
        recipe = ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
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
}

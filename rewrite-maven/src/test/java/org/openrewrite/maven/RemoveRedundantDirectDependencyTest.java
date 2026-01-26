/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveRedundantDirectDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveRedundantDirectDependency(null, null, null, null));
    }

    @DocumentExample
    @Test
    void removesDirectDependencyWhenAvailableTransitivelyWithSameOrNewerVersion() {
        rewriteRun(
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- This is the direct dependency that's also available transitively -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.0</version>
                    </dependency>
                    <!-- spring-boot-starter-tomcat transitively brings in tomcat-embed-core:10.1.28 -->
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- spring-boot-starter-tomcat transitively brings in tomcat-embed-core:10.1.28 -->
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void keepsDirectDependencyWhenVersionIsNewerThanTransitive() {
        // spring-boot-starter-tomcat:3.2.0 brings tomcat-embed-core:10.1.16 transitively
        // We declare 10.1.28 directly which is NEWER, so it should be kept
        rewriteRun(
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- This direct dependency version is NEWER than the transitive, keep it -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.28</version>
                    </dependency>
                    <!-- spring-boot-starter-tomcat:3.2.0 transitively brings in 10.1.16 -->
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void keepsDirectDependencyWhenNotAvailableTransitively() {
        rewriteRun(
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>32.1.3-jre</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void respectsGroupPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            "org.apache.tomcat.embed", null, null, null)),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void respectsExceptList() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, null, java.util.List.of("org.apache.tomcat.embed:tomcat-embed-core"))),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- Excepted from removal even though transitive is newer -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void removesWithComparatorAny() {
        // spring-boot-starter-tomcat:3.2.0 brings tomcat-embed-core:10.1.16 transitively
        // We declare 10.1.28 directly which is NEWER, but ANY mode removes it anyway
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.ANY, null)),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- Even though this is newer, ANY removes it -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.28</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void removesWithComparatorEq() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.EQ, null)),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- Versions must be exactly equal for removal with EQ -->
                    <!-- spring-boot-starter-tomcat:3.3.4 brings tomcat-embed-core:10.1.30 -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.30</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void keepsWhenComparatorEqAndVersionsDiffer() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.EQ, null)),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <!-- Version differs, EQ comparator should keep it -->
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-tomcat</artifactId>
                        <version>3.3.4</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }
}

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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ResolvedPomTest implements RewriteTest {

    @Test
    void resolveDependencyWithPlaceholderClassifier() {
        rewriteRun(
          pomXml(
            """
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo-parent</artifactId>
              <version>1</version>
              <properties>
                <netty.version>4.1.101.Final</netty.version>
                <netty-transport-native-epoll-classifier>linux-x86_64</netty-transport-native-epoll-classifier>
              </properties>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.netty</groupId>
                    <artifactId>netty-transport-native-epoll</artifactId>
                    <classifier>${netty-transport-native-epoll-classifier}</classifier>
                    <version>${netty.version}</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo</artifactId>
              <version>1</version>
              <parent>
                <groupId>org.example</groupId>
                <artifactId>foo-parent</artifactId>
                <version>1</version>
              </parent>
              <dependencies>
                <dependency>
                  <groupId>io.netty</groupId>
                  <artifactId>netty-transport-native-epoll</artifactId>
                  <classifier>${netty-transport-native-epoll-classifier}</classifier>
                </dependency>
              </dependencies>
            </project>
            """,
            spec -> spec.path("foo/pom.xml")
          ),
          pomXml(
            """
            <project>
              <groupId>org.example</groupId>
              <artifactId>bar</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1</version>
                </dependency>
              </dependencies>
            </project>
            """,
            spec -> spec.path("bar/pom.xml")
          )
        );
    }
}
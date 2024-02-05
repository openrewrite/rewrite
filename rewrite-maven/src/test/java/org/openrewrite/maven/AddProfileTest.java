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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;

class AddProfileTest implements RewriteTest {


    @DocumentExample
    @Test
    void addProfileToPom() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2).recipe(new AddProfile("myprofile", "<activation><foo>foo</foo></activation>",
            "<properties><bar>bar</bar></properties>", "<build><param>value</param></build>")),
          pomXml(
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>myprofile</id>
                    <activation>
                      <foo>foo</foo>
                    </activation>
                    <properties>
                      <bar>bar</bar>
                    </properties>
                    <build>
                      <param>value</param>
                    </build>
                  </profile>
                </profiles>
              </project>
              """

          )
        );
    }


    @Test
    void preExistingOtherProfile() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2).recipe(new AddProfile("myprofile", "<activation><foo>foo</foo></activation>",
            "<properties><bar>bar</bar></properties>", "<build><param>value</param></build>")),
          pomXml(
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>other.profile</id>
                    <activation>
                      <param>paramValue</param>
                    </activation>
                  </profile>
                </profiles>
              </project>
              """,
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>other.profile</id>
                    <activation>
                      <param>paramValue</param>
                    </activation>
                  </profile>
                  <profile>
                    <id>myprofile</id>
                    <activation>
                      <foo>foo</foo>
                    </activation>
                    <properties>
                      <bar>bar</bar>
                    </properties>
                    <build>
                      <param>value</param>
                    </build>
                  </profile>
                </profiles>
              </project>
              """

          )
        );
    }

    @Test
    void preExistingMatchingProfile() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2).recipe(new AddProfile("myprofile", "<activation><foo>foo</foo></activation>",
            "<properties><bar>bar</bar></properties>", "<build><param>value</param></build>")),
          pomXml(
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>myprofile</id>
                    <activation>
                      <param>paramValue</param>
                    </activation>
                  </profile>
                </profiles>
              </project>
              """,
            """
              <project>
                <groupId>group</groupId>
                <artifactId>artifact</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>myprofile</id>
                    <activation>
                      <foo>foo</foo>
                    </activation>
                    <properties>
                      <bar>bar</bar>
                    </properties>
                    <build>
                      <param>value</param>
                    </build>
                  </profile>
                </profiles>
              </project>
              """

          )
        );
    }


    @Test
    void notAPom() {
        rewriteRun(
          spec -> spec.recipe(new AddProfile("myprofile", "<activation></activation>",
            "<properties></properties>", "<build></build>")),
          xml(
            """
              <project>
              </project>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/beans.xml")

          )
        );
    }

}


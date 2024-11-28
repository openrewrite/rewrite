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
package org.openrewrite.xml;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.xml.Assertions.xml;

class ChangeTagValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void rewriteEmptyTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              null, "2.0", null)),
          xml(
            """
              <dependency>
                  <version/>
              </dependency>
              """,
            """
              <dependency>
                  <version>2.0</version>
              </dependency>
              """
            , spec -> spec.path("pom.xml"))
        );
    }

    @Test
    void rewriteTagValueSubstring() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "SNAPSHOT", "RELEASE", Boolean.TRUE)
          ),
          xml("""
                          <dependency>
                            <group>com.company.project</group>
                            <group>artifact</group>
                            <version>1.2.3-SNAPSHOT</version>
                          </dependency>
                    """,
            """
                  <dependency>
                    <group>com.company.project</group>
                    <group>artifact</group>
                    <version>1.2.3-RELEASE</version>
                  </dependency>
            """, spec -> spec.path("pom.xml")
          )
        );
    }


    @Test
    void appendTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "$", "-RELEASE", Boolean.TRUE)
          ),
          xml("""
                          <dependency>
                            <group>com.company.project</group>
                            <group>artifact</group>
                            <version>1.2.3</version>
                          </dependency>
                    """,
            """
                  <dependency>
                    <group>com.company.project</group>
                    <group>artifact</group>
                    <version>1.2.3-RELEASE</version>
                  </dependency>
            """, spec -> spec.path("pom.xml")
          )
        );
    }
}
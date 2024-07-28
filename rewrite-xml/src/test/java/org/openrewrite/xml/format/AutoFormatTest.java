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
package org.openrewrite.xml.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class AutoFormatTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/707")
    @Test
    void autoFormatAttributes() {
        rewriteRun(
          xml(
            """
              <databaseChangeLog
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog">
              </databaseChangeLog>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1503")
    @Test
    void autoFormatXmlDecl() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <tag>required tag</tag>
              """
          )
        );
    }

    @DocumentExample
    @Test
    void autoFormatTag() {
        rewriteRun(
          xml(
            """
              <project>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                    <exclusions>
                      <exclusion>
                <groupId>org.junit.vintage</groupId>
                <artifactId>junit-vintage-engine</artifactId>
              </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                    <exclusions>
                      <exclusion>
                        <groupId>org.junit.vintage</groupId>
                        <artifactId>junit-vintage-engine</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1189")
    void tagContentIndentation() {
        rewriteRun(
          xml(
            """
              <foo>
                bar
              </foo>
              """
          )
        );
    }
}

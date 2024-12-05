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
package org.openrewrite.maven;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ChangeParentPom2Test implements RewriteTest {

    @DocumentExample
    @Test
    void changeParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "com.bnppf.adm.sf4",
            "com.bnppf.adm.sf5",
            "adm-site-factory-4-pom",
            "adm-site-factory-5-pom",
            "1.0.6",
            null,
            null,
            null,
            false
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <parent>
                    <groupId>com.bnppf.adm.sf4</groupId>
                    <artifactId>adm-site-factory-4-pom</artifactId>
                    <version>1.7</version>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <parent>
                    <groupId>com.bnppf.adm.sf5</groupId>
                    <artifactId>adm-site-factory-5-pom</artifactId>
                    <version>1.0.6</version>
                </parent>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }
}

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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.style.TabsAndIndentsStyle;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class TabsAndIndentsTest implements RewriteTest {

    @Test
    void indentsAndContinuationIndents() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(
            TabsAndIndentsStyle.DEFAULT.withIndentSize(2).withContinuationIndentSize(5)
          ))),
          xml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <artifactId>quarkus-bootstrap-bom</artifactId>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <artifactId>quarkus-bootstrap-bom</artifactId>
              </project>
              """
          )
        );
    }
}

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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.style.Autodetect;
import org.openrewrite.xml.style.TabsAndIndentsStyle;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class AutodetectTest implements RewriteTest {

    @Test
    void autodetectSimple() {
        rewriteRun(
          hasIndentation(2, 4),
          xml(
            """
              <project>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <exclusions>
                      <exclusion>
                        <groupId>org.junit.vintage</groupId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1498")
    @Test
    void autodetectQuarkus() {
        rewriteRun(
          hasIndentation(4, 4),
          xml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-bootstrap-parent</artifactId>
                      <version>999-SNAPSHOT</version>
                  </parent>
                  <artifactId>quarkus-bootstrap-bom</artifactId>
              </project>
              """
          )
        );
    }

    @Test
    void continuationIndents() {
        rewriteRun(
          hasIndentation(4, 9),
          xml(
            """
              <project
                       xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <artifactId>quarkus-bootstrap-bom</artifactId>
              </project>
              """
          )
        );
    }

    private static Consumer<RecipeSpec> hasIndentation(int indentSize, int continuationIndentSize) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            TabsAndIndentsStyle tabsAndIndents = (TabsAndIndentsStyle) detector.build().getStyles().stream()
              .filter(TabsAndIndentsStyle.class::isInstance)
              .findAny().orElseThrow();
            assertThat(tabsAndIndents.getTabSize()).isEqualTo(1);
            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(indentSize);
            assertThat(tabsAndIndents.getContinuationIndentSize()).isEqualTo(continuationIndentSize);
            assertThat(tabsAndIndents.getUseTabCharacter()).isEqualTo(false);
        });
    }
}

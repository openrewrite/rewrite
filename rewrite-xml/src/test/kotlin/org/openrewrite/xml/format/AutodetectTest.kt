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
package org.openrewrite.xml.format

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.style.Autodetect
import org.openrewrite.xml.style.TabsAndIndentsStyle

class AutodetectTest {

    @Test
    fun autodetectSimple() = hasIndentation(
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
        """, indentSize = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1498")
    @Test
    fun autodetectQuarkus() = hasIndentation(
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
        """, indentSize = 4, continuationIndentSize = 4
    )

    @Test
    fun continuationIndents() = hasIndentation(
        """
            <project
                     xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <artifactId>quarkus-bootstrap-bom</artifactId>
            </project>
        """, indentSize = 4, continuationIndentSize = 9
    )

    private fun hasIndentation(
        @Language("xml") xml: String,
        indentSize: Int,
        continuationIndentSize: Int = 4,
        tabSize: Int = 1,
        useTabs: Boolean = false,
    ) {
        val autodetect = Autodetect.detect(XmlParser().parse(xml.trimIndent()))
        val tabsAndIndents: TabsAndIndentsStyle = autodetect.styles
            .filterIsInstance(TabsAndIndentsStyle::class.java).first()

        assertThat(tabsAndIndents.tabSize).isEqualTo(tabSize)
        assertThat(tabsAndIndents.indentSize).isEqualTo(indentSize)
        assertThat(tabsAndIndents.continuationIndentSize).isEqualTo(continuationIndentSize)
        assertThat(tabsAndIndents.useTabCharacter).isEqualTo(useTabs)
    }
}
